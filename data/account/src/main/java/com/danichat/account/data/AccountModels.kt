package com.danichat.account.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.UUID

/** Only a public client key belongs in an APK. Secret/service-role keys are rejected. */
class SupabaseConfig internal constructor(internal val url: HttpUrl, internal val publishableKey: String) {
    companion object {
        fun fromBuildValues(url: String, publishableKey: String): SupabaseConfig? {
            if (url.isBlank() && publishableKey.isBlank()) return null
            val parsed = requireNotNull(url.toHttpUrlOrNull()) { "URL Supabase inválida" }
            require(parsed.isHttps && parsed.username.isEmpty() && parsed.password.isEmpty() &&
                parsed.encodedPath == "/" && parsed.query == null && parsed.fragment == null) {
                "Supabase exige uma origem HTTPS sem credenciais ou parâmetros"
            }
            require(publishableKey.matches(Regex("sb_publishable_[A-Za-z0-9_-]{10,}"))) {
                "Use somente uma chave publicável Supabase (sb_publishable_)"
            }
            return SupabaseConfig(parsed, publishableKey)
        }
    }
}

data class OnlineUser(val id: String)

data class OnlineProfile(val userId: String, val displayName: String = "", val bio: String = "") {
    fun validate() {
        require(UUID.fromString(userId).toString() == userId)
        require(displayName.isNotBlank() && displayName.codePointCount(0, displayName.length) <= 80)
        require(bio.codePointCount(0, bio.length) <= 500)
    }
}

enum class AccountFailure {
    NETWORK, INVALID_CREDENTIALS, EMAIL_NOT_CONFIRMED, RATE_LIMITED, INVALID_INPUT,
    SESSION_EXPIRED, SERVER_NOT_READY, SERVICE_UNAVAILABLE, LOCAL_STORAGE
}

class AccountException(val failure: AccountFailure) : Exception(failure.name)

/** Intentionally has no generated toString(): sessions must never enter logs or UI state. */
class AccountSession internal constructor(
    internal val origin: String,
    internal val userId: String,
    internal val accessToken: String,
    internal val refreshToken: String,
    internal val expiresAt: Long
)

interface AccountSessionStore {
    suspend fun load(): AccountSession?
    suspend fun save(session: AccountSession)
    suspend fun clear()
}

interface OnlineAccountRepository {
    suspend fun currentUser(): OnlineUser?
    suspend fun signIn(email: String, password: String): OnlineUser
    suspend fun signUp(email: String, password: String): OnlineUser?
    suspend fun loadProfile(): OnlineProfile
    suspend fun saveProfile(displayName: String, bio: String): OnlineProfile
    suspend fun signOut(): Boolean
    suspend fun clearLocalSession()
}
