package com.danichat.account.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/** Separate from the BitChat key store. No plaintext fallback and no cloud key backup. */
@Suppress("DEPRECATION")
class EncryptedAccountSessionStore(context: Context, config: SupabaseConfig) : AccountSessionStore {
    private val appContext = context.applicationContext
    private val project = MessageDigest.getInstance("SHA-256")
        .digest(config.url.toString().toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }.take(24)

    private val preferences by lazy {
        EncryptedSharedPreferences.create(
            appContext, "danichat_online_session_$project",
            MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun load(): AccountSession? = withContext(Dispatchers.IO) {
        protectStorage {
            preferences.getString("session", null)?.let {
                val value = JsonParser.parseString(it).asJsonObject
                AccountSession(
                    value["origin"].asString, value["user_id"].asString,
                    value["access_token"].asString, value["refresh_token"].asString,
                    value["expires_at"].asLong
                )
            }
        }
    }

    override suspend fun save(session: AccountSession): Unit = withContext(Dispatchers.IO) {
        protectStorage {
            val json = JsonObject().apply {
                addProperty("origin", session.origin)
                addProperty("user_id", session.userId)
                addProperty("access_token", session.accessToken)
                addProperty("refresh_token", session.refreshToken)
                addProperty("expires_at", session.expiresAt)
            }
            check(preferences.edit().putString("session", json.toString()).commit())
        }
    }

    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        protectStorage { check(preferences.edit().clear().commit()) }
    }

    private inline fun <T> protectStorage(action: () -> T): T = try {
        action()
    } catch (_: Exception) {
        throw AccountException(AccountFailure.LOCAL_STORAGE)
    }
}
