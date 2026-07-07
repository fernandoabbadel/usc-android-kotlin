package com.example.usc1.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.AuthStatus
import com.example.usc1.data.repository.MockAuthRepository
import com.example.usc1.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = MockAuthRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, errorMessage = null) }
    }

    fun onInviteCodeChange(value: String) {
        _uiState.update { it.copy(inviteCode = value, errorMessage = null) }
    }

    fun signIn() {
        val current = _uiState.value
        if (!current.canSubmitLogin) {
            _uiState.update {
                it.copy(errorMessage = "Informe e-mail e senha para continuar.")
            }
            return
        }

        runAuthAction {
            authRepository.signIn(
                email = current.email,
                password = current.password,
            )
        }
    }

    fun register() {
        val current = _uiState.value
        if (!current.canSubmitRegister) {
            _uiState.update {
                it.copy(errorMessage = "Preencha nome e e-mail para criar a conta.")
            }
            return
        }

        runAuthAction {
            authRepository.register(
                fullName = current.fullName,
                email = current.email,
                inviteCode = current.inviteCode.ifBlank { null },
            )
        }
    }

    fun refreshApproval() {
        runAuthAction { authRepository.refreshSession() }
    }

    fun signOut() {
        runAuthAction { authRepository.signOut() }
    }

    fun simulateAuthenticated() {
        _uiState.update {
            it.copy(
                email = "membro@usc.app",
                password = "123456",
            )
        }
        signIn()
    }

    fun simulateWaitingApproval() {
        _uiState.update {
            it.copy(
                email = "pendente@usc.app",
                password = "123456",
            )
        }
        signIn()
    }

    fun simulateInviteRequired() {
        runAuthAction { authRepository.requireInvite() }
    }

    fun simulateBanned() {
        runAuthAction { authRepository.markBanned() }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun runAuthAction(action: suspend () -> com.example.usc1.core.session.UserSession) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }
            try {
                val nextSession = action()
                _uiState.update {
                    it.copy(
                        session = nextSession,
                        isLoading = false,
                        password = if (nextSession.status == AuthStatus.Authenticated) "" else it.password,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Não foi possível concluir a ação.",
                    )
                }
            }
        }
    }
}
