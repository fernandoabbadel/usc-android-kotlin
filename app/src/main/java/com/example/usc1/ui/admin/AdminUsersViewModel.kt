package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminUsersRepository
import com.example.usc1.domain.model.AdminUserListItem
import com.example.usc1.domain.model.AdminUserPlan
import com.example.usc1.domain.model.AdminUserStatus
import com.example.usc1.domain.model.AdminUsersLetterGroup
import com.example.usc1.domain.repository.AdminUsersRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminUsersViewModel(
    private val repository: AdminUsersRepository = SupabaseAdminUsersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadUsers(reset = true)
    }

    fun refresh() {
        loadUsers(reset = true, forceRefresh = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasMore || state.nextCursor == null || state.isLoadingMore) return
        loadUsers(reset = false, cursorId = state.nextCursor)
    }

    fun onSearchChange(value: String) {
        _uiState.update {
            it.copy(filters = it.filters.copy(search = value.take(80)))
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadUsers(reset = true)
        }
    }

    fun onPlanFilterChange(plan: AdminUserPlan) {
        _uiState.update {
            it.copy(filters = it.filters.copy(plan = plan))
        }
    }

    fun onLetterGroupChange(group: AdminUsersLetterGroup) {
        _uiState.update {
            it.copy(filters = it.filters.copy(letterGroup = group, search = ""))
        }
        loadUsers(reset = true)
    }

    fun toggleStatus(user: AdminUserListItem) {
        val nextStatus = if (user.status == AdminUserStatus.Bloqueado) {
            AdminUserStatus.Ativo
        } else {
            AdminUserStatus.Bloqueado
        }
        viewModelScope.launch {
            _uiState.update { it.copy(mutatingUserId = user.id, actionMessage = null, errorMessage = null) }
            try {
                repository.setUserStatus(user.id, nextStatus)
                _uiState.update { state ->
                    state.copy(
                        mutatingUserId = null,
                        users = state.users.map {
                            if (it.id == user.id) it.copy(status = nextStatus) else it
                        },
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
                        mutatingUserId = null,
                        errorMessage = error.message ?: "Erro ao atualizar status.",
                    )
                }
            }
        }
    }

    fun requestDelete(user: AdminUserListItem) {
        _uiState.update { it.copy(pendingDeleteUser = user, actionMessage = null) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDeleteUser = null) }
    }

    fun confirmDelete() {
        val user = _uiState.value.pendingDeleteUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(mutatingUserId = user.id, errorMessage = null, actionMessage = null) }
            try {
                repository.deleteUser(user.id)
                _uiState.update { state ->
                    state.copy(
                        mutatingUserId = null,
                        pendingDeleteUser = null,
                        users = state.users.filterNot { it.id == user.id },
                        actionMessage = "Usuário removido.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        mutatingUserId = null,
                        pendingDeleteUser = null,
                        errorMessage = error.message ?: "Erro ao remover usuário.",
                    )
                }
            }
        }
    }

    private fun loadUsers(
        reset: Boolean,
        cursorId: String? = null,
        forceRefresh: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.update {
                if (reset) {
                    it.copy(isLoading = true, errorMessage = null, actionMessage = null)
                } else {
                    it.copy(isLoadingMore = true, errorMessage = null, actionMessage = null)
                }
            }
            try {
                val page = repository.getUsersPage(
                    pageSize = PageSize,
                    cursorId = if (reset) null else cursorId,
                    filters = _uiState.value.filters,
                    forceRefresh = forceRefresh,
                )
                _uiState.update { page.toUiState(current = it, append = !reset) }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Erro ao carregar usuários.",
                    )
                }
            }
        }
    }

    private companion object {
        const val PageSize = 20
    }
}
