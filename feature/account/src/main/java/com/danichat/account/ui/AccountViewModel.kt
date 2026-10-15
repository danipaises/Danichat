package com.danichat.account.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danichat.account.data.AccountException
import com.danichat.account.data.AccountFailure
import com.danichat.account.data.OnlineProfile
import com.danichat.account.data.OnlineUser
import com.danichat.account.data.OnlineAccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

data class AccountUiState(
    val configured: Boolean,
    val busy: Boolean = false,
    val user: OnlineUser? = null,
    val profile: OnlineProfile? = null,
    val failure: AccountFailure? = null,
    val notice: AccountNotice? = null,
    val clearCredentialsVersion: Int = 0
)

enum class AccountNotice { CONFIRM_EMAIL, PROFILE_SAVED, SIGNED_OUT, SIGNED_OUT_LOCALLY }

class AccountViewModel(private val repository: OnlineAccountRepository?) : ViewModel() {
    private val mutableState = MutableStateFlow(AccountUiState(configured = repository != null))
    val state = mutableState.asStateFlow()
    private var operation: Job? = null
    private var panicWipeInProgress = false

    init {
        if (repository != null) perform {
            val user = repository.currentUser()
            mutableState.update { it.copy(user = user) }
            if (user != null) reloadProfile()
        }
    }

    fun signIn(email: String, password: String) = perform {
        val user = requireNotNull(repository).signIn(email, password)
        mutableState.update {
            it.copy(user = user, profile = null, clearCredentialsVersion = it.clearCredentialsVersion + 1)
        }
        reloadProfile()
    }

    fun signUp(email: String, password: String) = perform {
        val user = requireNotNull(repository).signUp(email, password)
        mutableState.update {
            it.copy(user = user, notice = if (user == null) AccountNotice.CONFIRM_EMAIL else null,
                clearCredentialsVersion = it.clearCredentialsVersion + 1)
        }
        if (user != null) reloadProfile()
    }

    fun retry() = perform {
        val user = requireNotNull(repository).currentUser()
        mutableState.update { it.copy(user = user) }
        if (user != null) reloadProfile()
    }

    fun saveProfile(displayName: String, bio: String) = perform {
        val profile = requireNotNull(repository).saveProfile(displayName, bio)
        mutableState.update { it.copy(profile = profile, notice = AccountNotice.PROFILE_SAVED) }
    }

    fun signOut() = perform {
        val revoked = requireNotNull(repository).signOut()
        mutableState.update {
            it.copy(user = null, profile = null,
                notice = if (revoked) AccountNotice.SIGNED_OUT else AccountNotice.SIGNED_OUT_LOCALLY)
        }
    }

    suspend fun clearLocalForPanic(): Boolean = withContext(NonCancellable) {
        panicWipeInProgress = true
        operation?.cancelAndJoin()
        val clearedCredentials = mutableState.value.clearCredentialsVersion + 1
        try {
            repository?.clearLocalSession()
            mutableState.value = AccountUiState(configured = repository != null,
                clearCredentialsVersion = clearedCredentials)
            panicWipeInProgress = false
            true
        } catch (_: AccountException) {
            mutableState.value = AccountUiState(configured = repository != null,
                failure = AccountFailure.LOCAL_STORAGE, clearCredentialsVersion = clearedCredentials)
            false
        }
    }

    private suspend fun reloadProfile() {
        val profile = requireNotNull(repository).loadProfile()
        mutableState.update { it.copy(profile = profile) }
    }

    private fun perform(action: suspend () -> Unit) {
        if (repository == null || mutableState.value.busy || panicWipeInProgress) return
        mutableState.update { it.copy(busy = true, failure = null, notice = null) }
        operation = viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AccountException) {
                mutableState.update {
                    it.copy(failure = e.failure,
                        user = if (e.failure == AccountFailure.SESSION_EXPIRED) null else it.user,
                        profile = if (e.failure == AccountFailure.SESSION_EXPIRED) null else it.profile)
                }
            } catch (_: Exception) {
                mutableState.update { it.copy(failure = AccountFailure.SERVICE_UNAVAILABLE) }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }
}
