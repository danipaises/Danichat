package com.danichat.account.data

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupabaseAccountRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var config: SupabaseConfig
    private lateinit var store: MemoryStore
    private lateinit var repository: SupabaseAccountRepository

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        // Internal test construction allows loopback HTTP; production parsing requires HTTPS.
        config = SupabaseConfig(server.url("/"), "sb_publishable_synthetic_test")
        store = MemoryStore()
        val client = OkHttpClient()
        repository = SupabaseAccountRepository(SupabaseAccountApi(config) { client }, store) { NOW }
    }

    @After fun tearDown() { server.close() }

    @Test fun `email confirmation does not create a local session`() = runTest {
        server.enqueue(json("""{"user":{"id":"$USER_A"}}"""))
        assertNull(repository.signUp("test@example.invalid", "synthetic-password"))
        assertNull(repository.currentUser())
        assertNull(store.value)
        val request = server.takeRequest()
        assertEquals("/auth/v1/signup", request.url.encodedPath)
        assertNull(request.headers["Authorization"])
    }

    @Test fun `password login stores the real session and profile requests use its token`() = runTest {
        server.enqueue(json(sessionJson()))
        server.enqueue(json(profileJson()))
        assertEquals(USER_A, repository.signIn("test@example.invalid", "synthetic-password").id)
        assertEquals("Perfil sintético", repository.loadProfile().displayName)
        assertEquals(USER_A, store.value?.userId)
        val login = server.takeRequest()
        assertEquals("password", login.url.queryParameter("grant_type"))
        assertEquals("sb_publishable_synthetic_test", login.headers["apikey"])
        val profile = server.takeRequest()
        assertEquals("Bearer synthetic-access", profile.headers["Authorization"])
        assertEquals("eq.$USER_A", profile.url.queryParameter("user_id"))
    }

    @Test fun `invalid credentials never become an authenticated user`() = runTest {
        server.enqueue(json("""{"error_code":"invalid_credentials"}""", 400))
        assertFailure(AccountFailure.INVALID_CREDENTIALS) {
            repository.signIn("test@example.invalid", "synthetic-password")
        }
        assertNull(store.value)
        assertNull(repository.currentUser())
    }

    @Test fun `a stored session from a different project is not sent`() = runTest {
        store.value = storedSession(origin = "https://other.example.invalid/")
        assertNull(repository.currentUser())
        assertNull(store.value)
        assertEquals(0, server.requestCount)
    }

    @Test fun `concurrent loads rotate an expired refresh token only once`() = runTest {
        store.value = storedSession(expiry = NOW - 1)
        server.enqueue(json(sessionJson(access = "rotated-access", refresh = "rotated-refresh")))
        server.enqueue(json(profileJson()))
        server.enqueue(json(profileJson()))
        val first = async { repository.loadProfile() }
        val second = async { repository.loadProfile() }
        assertEquals(USER_A, first.await().userId)
        assertEquals(USER_A, second.await().userId)
        assertEquals(3, server.requestCount)
        assertEquals("refresh_token", server.takeRequest().url.queryParameter("grant_type"))
        repeat(2) { assertEquals("Bearer rotated-access", server.takeRequest().headers["Authorization"]) }
        assertEquals("rotated-refresh", store.value?.refreshToken)
    }

    @Test fun `refresh cannot silently switch to another account`() = runTest {
        store.value = storedSession(expiry = NOW - 1)
        server.enqueue(json(sessionJson(user = USER_B)))
        assertFailure(AccountFailure.SESSION_EXPIRED) { repository.loadProfile() }
        assertNull(store.value)
        assertNull(repository.currentUser())
        assertEquals(1, server.requestCount)
    }

    @Test fun `revoked refresh token clears the local account without retry loops`() = runTest {
        store.value = storedSession(expiry = NOW - 1)
        server.enqueue(json("{}", 401))
        assertFailure(AccountFailure.SESSION_EXPIRED) { repository.loadProfile() }
        assertNull(store.value)
        assertFailure(AccountFailure.SESSION_EXPIRED) { repository.loadProfile() }
        assertEquals(1, server.requestCount)
    }

    @Test fun `an authorization rejection on profile access clears the session`() = runTest {
        store.value = storedSession()
        server.enqueue(json("{}", 403))
        assertFailure(AccountFailure.SESSION_EXPIRED) { repository.loadProfile() }
        assertNull(store.value)
        assertNull(repository.currentUser())
    }

    @Test fun `a foreign profile response is rejected`() = runTest {
        store.value = storedSession()
        server.enqueue(json(profileJson(USER_B)))
        assertFailure(AccountFailure.SERVICE_UNAVAILABLE) { repository.loadProfile() }
    }

    @Test fun `redirects never forward the bearer token to another origin`() = runTest {
        MockWebServer().use { destination ->
            destination.start()
            store.value = storedSession()
            server.enqueue(MockResponse.Builder().code(302)
                .addHeader("Location", destination.url("/capture")).build())
            assertFailure(AccountFailure.SERVICE_UNAVAILABLE) { repository.loadProfile() }
            assertEquals(0, destination.requestCount)
        }
    }

    @Test fun `logout removes local session even when revocation is unavailable`() = runTest {
        store.value = storedSession()
        server.enqueue(json("{}", 503))
        assertFalse(repository.signOut())
        assertNull(store.value)
        assertNull(repository.currentUser())
    }

    @Test fun `emergency wipe clears the stored account without any network request`() = runTest {
        store.value = storedSession()
        assertEquals(USER_A, repository.currentUser()?.id)
        repository.clearLocalSession()
        assertNull(store.value)
        assertNull(repository.currentUser())
        assertEquals(0, server.requestCount)
    }

    @Test fun `profile upsert uses authenticated owner and accepts Unicode code point limits`() = runTest {
        store.value = storedSession()
        val name = "😀".repeat(80)
        server.enqueue(json("""[{"user_id":"$USER_A","display_name":"$name","bio":""}]"""))
        assertEquals(name, repository.saveProfile(name, "").displayName)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("resolution=merge-duplicates,return=representation", request.headers["Prefer"])
        assertTrue(request.body!!.utf8().contains(USER_A))
        assertFailure(AccountFailure.INVALID_INPUT) { repository.saveProfile(name + "a", "") }
        assertEquals(1, server.requestCount)
    }

    @Test fun `session persistence failure does not report a successful login`() = runTest {
        store.failSaves = true
        server.enqueue(json(sessionJson()))
        assertFailure(AccountFailure.LOCAL_STORAGE) {
            repository.signIn("test@example.invalid", "synthetic-password")
        }
        assertNull(repository.currentUser())
    }

    private fun storedSession(origin: String = config.url.toString(), expiry: Long = NOW + 3600) =
        AccountSession(origin, USER_A, "synthetic-access", "synthetic-refresh", expiry)

    private fun sessionJson(user: String = USER_A, access: String = "synthetic-access",
        refresh: String = "synthetic-refresh") =
        """{"user":{"id":"$user"},"access_token":"$access","refresh_token":"$refresh","expires_at":${NOW + 3600}}"""

    private fun profileJson(user: String = USER_A) =
        """[{"user_id":"$user","display_name":"Perfil sintético","bio":""}]"""

    private fun json(body: String, code: Int = 200) = MockResponse.Builder().code(code)
        .addHeader("Content-Type", "application/json").body(body).build()

    private suspend fun assertFailure(expected: AccountFailure, action: suspend () -> Unit) {
        try {
            action()
            throw AssertionError("Expected $expected")
        } catch (e: AccountException) { assertEquals(expected, e.failure) }
    }

    private class MemoryStore : AccountSessionStore {
        var value: AccountSession? = null
        var failSaves = false
        override suspend fun load() = value
        override suspend fun save(session: AccountSession) {
            if (failSaves) throw AccountException(AccountFailure.LOCAL_STORAGE)
            value = session
        }
        override suspend fun clear() { value = null }
    }

    companion object {
        private const val NOW = 1_700_000_000L
        private const val USER_A = "00000000-0000-4000-8000-000000000001"
        private const val USER_B = "00000000-0000-4000-8000-000000000002"
    }
}
