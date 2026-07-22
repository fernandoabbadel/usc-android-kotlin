package com.example.usc1.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.AuthStatus
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.data.repository.SupabaseAuthRepository
import com.example.usc1.data.supabase.AuthDeepLinkEvent
import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = SupabaseAuthRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.session.collect { session ->
                _uiState.update { it.copy(session = session, isLoading = false) }
            }
        }
        viewModelScope.launch {
            SupabaseClientProvider.authDeepLinkEvents.collect { event ->
                when (event) {
                    AuthDeepLinkEvent.SessionImported -> refreshAfterOAuthReturn()
                    is AuthDeepLinkEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isWaitingForOAuthRedirect = false,
                                errorMessage = event.userMessage,
                                statusMessage = null,
                            )
                        }
                    }
                }
            }
        }
        refreshApproval()
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, statusMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null, statusMessage = null) }
    }

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, errorMessage = null, statusMessage = null) }
    }

    fun onInviteCodeChange(value: String) {
        _uiState.update { it.copy(inviteCode = value, errorMessage = null, statusMessage = null) }
    }

    fun signIn() {
        runAuthAction { authRepository.signIn(email = _uiState.value.email, password = _uiState.value.password) }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isWaitingForOAuthRedirect = true,
                    errorMessage = null,
                    statusMessage = "Abrindo login Google. O retorno precisa acontecer no app por usc1://auth.",
                )
            }
            try {
                val nextSession = authRepository.signInWithGoogle()
                val hasNativeSession = nextSession.status != AuthStatus.Unauthenticated
                Log.d(Tag, "sessão criada imediatamente após OAuth: ${if (hasNativeSession) "sim" else "não"}")
                _uiState.update {
                    it.copy(
                        session = if (hasNativeSession) nextSession else it.session,
                        isLoading = false,
                        isWaitingForOAuthRedirect = !hasNativeSession,
                        errorMessage = null,
                        statusMessage = if (hasNativeSession) {
                            null
                        } else {
                            "Login iniciado no navegador. Aguardando retorno automático para o app por usc1://auth."
                        },
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isWaitingForOAuthRedirect = false,
                        errorMessage = error.message ?: "Não foi possível iniciar o login Google.",
                        statusMessage = null,
                    )
                }
            }
        }
    }

    fun loginAsGuest() {
        runAuthAction { authRepository.loginAsGuest() }
    }

    fun selectGuestTenant(tenant: TenantContext) {
        runAuthAction { authRepository.selectGuestTenant(tenant) }
    }

    fun register() {
        val current = _uiState.value
        if (!current.canSubmitRegister) {
            _uiState.update {
                it.copy(
                    errorMessage = "Informe o código de convite enviado pela atlética.",
                    statusMessage = null,
                )
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
        signIn()
    }

    fun simulateWaitingApproval() {
        runAuthAction { authRepository.requireInvite() }
    }

    fun simulateInviteRequired() {
        runAuthAction { authRepository.requireInvite() }
    }

    fun simulateBanned() {
        runAuthAction { authRepository.markBanned() }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, statusMessage = null) }
    }

    private fun refreshAfterOAuthReturn() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    statusMessage = "Retorno do Google recebido. Validando sessão e tenant ativo.",
                )
            }
            try {
                val nextSession = authRepository.refreshSession()
                val hasAuthUser = nextSession.user != null
                val hasTenant = nextSession.tenant != null
                Log.d(Tag, "usuário autenticado após deep link: ${if (hasAuthUser) "sim" else "não"}")
                Log.d(Tag, "tenant ativo após deep link: ${if (hasTenant) "sim" else "não"}")
                _uiState.update {
                    it.copy(
                        session = nextSession,
                        isLoading = false,
                        isWaitingForOAuthRedirect = false,
                        errorMessage = null,
                        statusMessage = oauthStatusMessage(nextSession),
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isWaitingForOAuthRedirect = false,
                        errorMessage = error.message ?: "O app recebeu o retorno do Google, mas não conseguiu validar a sessão.",
                        statusMessage = null,
                    )
                }
            }
        }
    }

    private fun oauthStatusMessage(session: UserSession): String? {
        return when (session.status) {
            AuthStatus.Authenticated -> null
            AuthStatus.InviteRequired -> "Login recebido, mas nenhum tenant ativo foi resolvido. Informe um convite da atlética."
            AuthStatus.WaitingApproval -> "Login recebido. Sua entrada na atlética ainda está aguardando aprovação."
            AuthStatus.Banned -> "Login recebido, mas o usuário está bloqueado."
            AuthStatus.Unauthenticated -> "O retorno do Google chegou, mas a sessão Supabase não foi criada."
            AuthStatus.Loading -> null
        }
    }

    private fun runAuthAction(action: suspend () -> UserSession) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    statusMessage = null,
                )
            }
            try {
                val nextSession = action()
                _uiState.update {
                    it.copy(
                        session = nextSession,
                        isLoading = false,
                        isWaitingForOAuthRedirect = false,
                        password = if (nextSession.status == AuthStatus.Authenticated) "" else it.password,
                        statusMessage = null,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isWaitingForOAuthRedirect = false,
                        errorMessage = error.message ?: "Não foi possível concluir a ação.",
                        statusMessage = null,
                    )
                }
            }
        }
    }

    private companion object {
        const val Tag = "USCAuth"
    }
}
