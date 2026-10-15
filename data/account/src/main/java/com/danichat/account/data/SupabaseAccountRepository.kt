package com.danichat.account.data

import com.google.gson.JsonObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.UUID
import okhttp3.OkHttpClient

/** Auth/profile API only. No messages, contacts, Noise keys or Nostr keys are uploaded. */
class SupabaseAccountRepository internal constructor(
    private val api: SupabaseAccountApi,
    private val store: AccountSessionStore,
    private val epochSeconds: () -> Long = { System.currentTimeMillis() / 1000 }
) : OnlineAccountRepository {
    constructor(config: SupabaseConfig, store: AccountSessionStore, httpClient: () -> OkHttpClient) :
        this(SupabaseAccountApi(config, httpClient), store)

    private val mutex = Mutex()
    private var session: AccountSession? = null
    private var loaded = false

    override suspend fun currentUser(): OnlineUser? = mutex.withLock {
        loadSession()?.let { OnlineUser(it.userId) }
    }

    override suspend fun signIn(email: String, password: String): OnlineUser = mutex.withLock {
        validateCredentials(email, password)
        val json = api.request("auth/v1/token", "POST", credentials(email, password),
            query = mapOf("grant_type" to "password")).asJsonObject
        val next = parseSession(json)
        store.save(next)
        session = next
        loaded = true
        OnlineUser(next.userId)
    }

    /** Null means the server requires email confirmation; it never fabricates a session. */
    override suspend fun signUp(email: String, password: String): OnlineUser? = mutex.withLock {
        validateCredentials(email, password)
        if (password.length < 12) throw AccountException(AccountFailure.INVALID_INPUT)
        val json = api.request("auth/v1/signup", "POST", credentials(email, password)).asJsonObject
        if (!json.has("access_token") || json["access_token"].isJsonNull) return@withLock null
        val next = parseSession(json)
        store.save(next)
        session = next
        loaded = true
        OnlineUser(next.userId)
    }

    override suspend fun loadProfile(): OnlineProfile = mutex.withLock {
        val current = validSession()
        val rows = authenticatedRequest {
            api.request("rest/v1/danichat_profiles", token = current.accessToken,
                query = mapOf("user_id" to "eq.${current.userId}", "select" to "user_id,display_name,bio"))
        }.asJsonArray
        if (rows.size() == 0) OnlineProfile(current.userId) else parseProfile(rows.single().asJsonObject, current.userId)
    }

    override suspend fun saveProfile(displayName: String, bio: String): OnlineProfile = mutex.withLock {
        val current = validSession()
        val profile = OnlineProfile(current.userId, displayName.trim(), bio.trim())
        try { profile.validate() } catch (_: IllegalArgumentException) {
            throw AccountException(AccountFailure.INVALID_INPUT)
        }
        val json = JsonObject().apply {
            addProperty("user_id", profile.userId)
            addProperty("display_name", profile.displayName)
            addProperty("bio", profile.bio)
        }
        val rows = authenticatedRequest {
            api.request("rest/v1/danichat_profiles", "POST", json, current.accessToken,
                query = mapOf("on_conflict" to "user_id", "select" to "user_id,display_name,bio"),
                prefer = "resolution=merge-duplicates,return=representation")
        }.asJsonArray
        parseProfile(rows.single().asJsonObject, current.userId)
    }

    /** Returns whether revocation reached the server. Always removes the local session. */
    override suspend fun signOut(): Boolean = mutex.withLock {
        val old = loadSession()
        try {
            if (old == null) true else {
                try {
                    api.request("auth/v1/logout", "POST", token = old.accessToken,
                        query = mapOf("scope" to "local"))
                    true
                } catch (_: AccountException) { false }
            }
        } finally {
            withContext(NonCancellable) {
                try { store.clear() } finally {
                    session = null
                    loaded = true
                }
            }
        }
    }

    /** Offline emergency wipe: never wait for a remote logout or touch mesh identities. */
    override suspend fun clearLocalSession() = mutex.withLock {
        try { store.clear() } finally {
            session = null
            loaded = true
        }
    }

    private suspend fun loadSession(): AccountSession? {
        if (!loaded) {
            val saved = store.load()
            if (saved != null && saved.origin != api.config.url.toString()) {
                store.clear()
                session = null
            } else session = saved
            loaded = true
        }
        return session
    }

    private suspend fun <T> authenticatedRequest(action: suspend () -> T): T = try {
        action()
    } catch (e: AccountException) {
        if (e.failure == AccountFailure.SESSION_EXPIRED) {
            session = null
            store.clear()
        }
        throw e
    }

    private suspend fun validSession(): AccountSession {
        val current = loadSession() ?: throw AccountException(AccountFailure.SESSION_EXPIRED)
        if (current.expiresAt > epochSeconds() + 60) return current
        val json = try {
            api.request("auth/v1/token", "POST", JsonObject().apply {
                addProperty("refresh_token", current.refreshToken)
            }, query = mapOf("grant_type" to "refresh_token")).asJsonObject
        } catch (e: AccountException) {
            if (e.failure in setOf(AccountFailure.SESSION_EXPIRED, AccountFailure.INVALID_INPUT)) {
                session = null
                store.clear()
                throw AccountException(AccountFailure.SESSION_EXPIRED)
            }
            throw e
        }
        val next = parseSession(json)
        if (next.userId != current.userId) {
            session = null
            store.clear()
            throw AccountException(AccountFailure.SESSION_EXPIRED)
        }
        store.save(next)
        session = next
        return next
    }

    private fun parseSession(json: JsonObject): AccountSession = try {
        val id = json["user"].asJsonObject["id"].asString
        require(UUID.fromString(id).toString() == id)
        val access = json["access_token"].asString.also { require(it.isNotBlank()) }
        val refresh = json["refresh_token"].asString.also { require(it.isNotBlank()) }
        val expiry = json["expires_at"]?.asLong ?: (epochSeconds() + json["expires_in"].asLong)
        AccountSession(api.config.url.toString(), id, access, refresh, expiry)
    } catch (_: Exception) {
        throw AccountException(AccountFailure.SERVICE_UNAVAILABLE)
    }

    private fun parseProfile(json: JsonObject, expectedUser: String): OnlineProfile {
        val profile = OnlineProfile(json["user_id"].asString, json["display_name"].asString, json["bio"].asString)
        if (profile.userId != expectedUser) throw AccountException(AccountFailure.SERVICE_UNAVAILABLE)
        return profile
    }

    private fun validateCredentials(email: String, password: String) {
        if (email.length > 254 || !email.contains('@') || password.isEmpty()) {
            throw AccountException(AccountFailure.INVALID_INPUT)
        }
    }

    private fun credentials(email: String, password: String) = JsonObject().apply {
        addProperty("email", email.trim())
        addProperty("password", password)
    }
}
