package com.danichat.account.ui

import com.danichat.account.data.AccountException
import com.danichat.account.data.AccountFailure
import com.danichat.account.data.OnlineAccountRepository
import com.danichat.account.data.OnlineProfile
import com.danichat.account.data.OnlineUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `panic cancels an in-flight login before clearing the account`() = runTest(dispatcher) {
        val repository = FakeAccountRepository()
        val viewModel = AccountViewModel(repository)
        runCurrent()
        viewModel.signIn("test@example.invalid", "synthetic-password")
        runCurrent()
        assertTrue(repository.loginStarted)
        assertTrue(viewModel.clearLocalForPanic())
        repository.loginResult.complete(OnlineUser(USER))
        runCurrent()
        assertTrue(repository.cleared)
        assertFalse(repository.loginFinished)
        assertNull(viewModel.state.value.user)
        assertFalse(viewModel.state.value.busy)
    }

    @Test fun `failed emergency deletion is reported and blocks new online operations`() = runTest(dispatcher) {
        val repository = FakeAccountRepository().apply { failClear = true }
        val viewModel = AccountViewModel(repository)
        runCurrent()
        assertFalse(viewModel.clearLocalForPanic())
        assertEquals(AccountFailure.LOCAL_STORAGE, viewModel.state.value.failure)
        viewModel.signIn("test@example.invalid", "synthetic-password")
        runCurrent()
        assertFalse(repository.loginStarted)
    }

    @Test fun `unconfigured account never starts network operations`() = runTest(dispatcher) {
        val viewModel = AccountViewModel(null)
        viewModel.signIn("test@example.invalid", "synthetic-password")
        runCurrent()
        assertFalse(viewModel.state.value.configured)
        assertNull(viewModel.state.value.user)
        assertTrue(viewModel.clearLocalForPanic())
    }

    private class FakeAccountRepository : OnlineAccountRepository {
        val loginResult = CompletableDeferred<OnlineUser>()
        var loginStarted = false
        var loginFinished = false
        var cleared = false
        var failClear = false
        override suspend fun currentUser(): OnlineUser? = null
        override suspend fun signIn(email: String, password: String): OnlineUser {
            loginStarted = true
            return loginResult.await().also { loginFinished = true }
        }
        override suspend fun signUp(email: String, password: String): OnlineUser? = error("Unexpected registration")
        override suspend fun loadProfile(): OnlineProfile = error("Unexpected profile request")
        override suspend fun saveProfile(displayName: String, bio: String): OnlineProfile = error("Unexpected save")
        override suspend fun signOut(): Boolean = error("Unexpected remote logout")
        override suspend fun clearLocalSession() {
            if (failClear) throw AccountException(AccountFailure.LOCAL_STORAGE)
            cleared = true
        }
    }

    companion object { private const val USER = "00000000-0000-4000-8000-000000000001" }
}
