package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminUsersRepository
import com.example.usc1.domain.model.AdminUserPlan
import com.example.usc1.domain.model.AdminUserStatus
import com.example.usc1.domain.repository.AdminUsersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminUserDetailViewModel(
    private val repository: AdminUsersRepository = SupabaseAdminUsersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminUserDetailUiState())
    val uiState: StateFlow<AdminUserDetailUiState> = _uiState.asStateFlow()

    fun load(userId: String, forceRefresh: Boolean = false) {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) {
            _uiState.update {
                it.copy(
                    userId = "",
                    isLoading = false,
                    errorMessage = "Usuário não encontrado.",
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userId = cleanUserId,
                    isLoading = true,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            try {
                val profile = repository.getUserProfile(cleanUserId, forceRefresh)
                if (profile == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = null,
                            form = null,
                            errorMessage = "Usuário não encontrado.",
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        form = profile.toDetailForm(),
                        errorMessage = null,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar usuário.",
                    )
                }
            }
        }
    }

    fun updateNome(value: String) {
        updateForm { it.copy(nome = value.take(120)) }
    }

    fun updateTelefone(value: String) {
        updateForm { it.copy(telefone = value.onlyDigits().take(PhoneMaxLength)) }
    }

    fun updateMatricula(value: String) {
        updateForm { it.copy(matricula = value.take(40)) }
    }

    fun updateTurma(value: String) {
        updateForm { it.copy(turma = value.take(30)) }
    }

    fun updatePlano(value: AdminUserPlan) {
        if (value == AdminUserPlan.Todos) return
        updateForm { it.copy(plano = value) }
    }

    fun updateStatus(value: AdminUserStatus) {
        updateForm { it.copy(status = value) }
    }

    fun save() {
        val state = _uiState.value
        val form = state.form ?: return
        if (form.telefone.isNotBlank() && !form.telefone.hasValidPhoneLength()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Informe um telefone valido com DDI e somente numeros.",
                    actionMessage = null,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, actionMessage = null) }
            try {
                repository.updateUser(form.toUpdate(state.userId))
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        profile = it.profile?.copy(
                            nome = form.nome,
                            telefone = form.telefone,
                            matricula = form.matricula,
                            turma = form.turma,
                            status = form.status,
                            tier = form.plano,
                        ),
                        actionMessage = "Usuário atualizado.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Erro ao salvar.",
                    )
                }
            }
        }
    }

    fun toggleStatus() {
        val state = _uiState.value
        val form = state.form ?: return
        val nextStatus = if (form.status == AdminUserStatus.Bloqueado) {
            AdminUserStatus.Ativo
        } else {
            AdminUserStatus.Bloqueado
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isChangingStatus = true, errorMessage = null, actionMessage = null) }
            try {
                repository.setUserStatus(state.userId, nextStatus)
                _uiState.update {
                    it.copy(
                        isChangingStatus = false,
                        form = it.form?.copy(status = nextStatus),
                        profile = it.profile?.copy(status = nextStatus),
                        actionMessage = if (nextStatus == AdminUserStatus.Bloqueado) {
                            "Usuário bloqueado."
                        } else {
                            "Usuário desbloqueado."
                        },
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isChangingStatus = false,
                        errorMessage = error.message ?: "Erro ao alterar status.",
                    )
                }
            }
        }
    }

    fun requestDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = true, actionMessage = null, errorMessage = null) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun confirmDelete() {
        val userId = _uiState.value.userId
        if (userId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, showDeleteConfirmation = false, errorMessage = null) }
            try {
                repository.deleteUser(userId)
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        actionMessage = "Usuário excluído.",
                        shouldNavigateBack = true,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Erro ao excluir usuário.",
                    )
                }
            }
        }
    }

    fun consumeNavigateBack() {
        _uiState.update { it.copy(shouldNavigateBack = false) }
    }

    private fun updateForm(transform: (AdminUserDetailForm) -> AdminUserDetailForm) {
        _uiState.update { state ->
            val form = state.form ?: return@update state
            state.copy(form = transform(form), actionMessage = null, errorMessage = null)
        }
    }

    private fun String.onlyDigits(): String = filter(Char::isDigit)

    private fun String.hasValidPhoneLength(): Boolean {
        val digits = onlyDigits().take(PhoneMaxLength)
        return digits.length in PhoneMinLength..PhoneMaxLength
    }

    private companion object {
        const val PhoneMinLength = 10
        const val PhoneMaxLength = 15
    }
}
