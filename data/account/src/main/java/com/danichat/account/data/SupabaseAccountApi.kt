package com.danichat.account.data

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class SupabaseAccountApi(
    internal val config: SupabaseConfig,
    private val client: () -> OkHttpClient = { OkHttpClient() }
) {
    suspend fun request(
        path: String,
        method: String = "GET",
        body: JsonObject? = null,
        token: String? = null,
        query: Map<String, String> = emptyMap(),
        prefer: String? = null
    ): JsonElement = suspendCancellableCoroutine { continuation ->
        val url = config.url.newBuilder().addPathSegments(path).apply {
            query.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        val request = Request.Builder().url(url).header("apikey", config.publishableKey).apply {
            if (token != null) header("Authorization", "Bearer $token")
            if (prefer != null) header("Prefer", prefer)
            header("Accept", "application/json")
            method(method, if (method in setOf("GET", "HEAD")) null else
                (body?.toString() ?: "{}").toRequestBody("application/json".toMediaType()))
        }.build()
        // Resolve the current route for every request, including after Tor preferences change.
        // Never follow redirects with API keys or bearer credentials attached.
        val http = client().newBuilder().followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).callTimeout(30, TimeUnit.SECONDS).build()
        val call = http.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(AccountException(AccountFailure.NETWORK))
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        val raw = it.body.string()
                        val json = if (raw.isBlank()) JsonObject() else try {
                            JsonParser.parseString(raw)
                        } catch (_: Exception) {
                            throw AccountException(AccountFailure.SERVICE_UNAVAILABLE)
                        }
                        if (!it.isSuccessful) {
                            val code = if (json.isJsonObject) json.asJsonObject["error_code"]?.asString else null
                            val failure = when {
                                it.code == 429 -> AccountFailure.RATE_LIMITED
                                code == "email_not_confirmed" -> AccountFailure.EMAIL_NOT_CONFIRMED
                                code == "invalid_credentials" -> AccountFailure.INVALID_CREDENTIALS
                                it.code == 401 || it.code == 403 -> AccountFailure.SESSION_EXPIRED
                                it.code == 404 -> AccountFailure.SERVER_NOT_READY
                                it.code == 400 || it.code == 422 -> AccountFailure.INVALID_INPUT
                                else -> AccountFailure.SERVICE_UNAVAILABLE
                            }
                            throw AccountException(failure)
                        }
                        json
                    }
                }
                if (continuation.isActive) result.fold(continuation::resume, continuation::resumeWithException)
            }
        })
    }
}
