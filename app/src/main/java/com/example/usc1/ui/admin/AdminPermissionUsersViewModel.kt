package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminUsersRepository
import com.example.usc1.domain.model.AdminPermissionRole
import com.example.usc1.domain.model.AdminUserListItem
import com.example.usc1.domain.model.AdminUserRoleUpdate
import com.example.usc1.domain.model.AdminUserTurmaLeaderUpdate
import com.example.usc1.domain.model.AdminUsersLetterGroup
import com.example.usc1.domain.repository.AdminUsersRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminPermissionUsersViewModel(
    private val repository: AdminUsersRepository = SupabaseAdminUsersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminPermissionUsersUiState())
    val uiState: StateFlow<AdminPermissionUsersUiState> = _uiState.asStateFlow()
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
            it.copy(filters = it.filters.copy(search = value.take(80)), actionMessage = null)
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SearchDebounceMillis)
            loadUsers(reset = true)
        }
    }

    fun onLetterGroupChange(group: AdminUsersLetterGroup) {
        _uiState.update {
            it.copy(
                filters = it.filters.copy(letterGroup = group, search = ""),
                actionMessage = null,
            )
        }
        loadUsers(reset = true)
    }

    fun selectRole(user: AdminUserListItem, nextRole: String) {
        val normalizedNextRole = AdminPermissionRole.normalize(nextRole)
        val currentRole = AdminPermissionRole.normalize(user.role)
        _uiState.update { state ->
            val nextPending = state.pendingRoles.toMutableMap()
            if (normalizedNextRole == currentRole) {
                nextPending.remove(user.id)
            } else {
                nextPending[user.id] = normalizedNextRole
            }
            state.copy(pendingRoles = nextPending, actionMessage = null, errorMessage = null)
        }
    }

    fun saveRole(
        user: AdminUserListItem,
        actorUserId: String,
        actorName: String,
        roleOverride: String? = null,
    ) {
        val role = AdminPermissionRole.normalize(roleOverride ?: _uiState.value.pendingRoles[user.id])
        val hasPendingRole = _uiState.value.pendingRoles[user.id] != null || roleOverride != null
        if (!hasPendingRole) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    savingUserIds = it.savingUserIds + user.id,
                    actionMessage = null,
                    errorMessage = null,
                )
            }
            try {
                repository.updateUserRole(
                    AdminUserRoleUpdate(
                        targetUserId = user.id,
                        role = role,
                        actorUserId = actorUserId,
                        actorName = actorName,
                    ),
                )
                _uiState.update { state ->
                    state.copy(
                        users = state.users.map { row ->
                            if (row.id == user.id) row.copy(role = role) else row
                        },
                        pendingRoles = state.pendingRoles - user.id,
                        savingUserIds = state.savingUserIds - user.id,
                        actionMessage = "Cargo salvo como ${AdminPermissionRole.labelFor(role)}.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        savingUserIds = it.savingUserIds - user.id,
                        errorMessage = error.message ?: "Erro ao atualizar cargo.",
                    )
                }
            }
        }
    }

    fun saveAllRoles(actorUserId: String, actorName: String) {
        val state = _uiState.value
        val pending = state.pendingRoles.toList()
        if (pending.isEmpty()) return
        viewModelScope.launch {
            pending.forEach { (targetUserId, role) ->
                val user = _uiState.value.users.firstOrNull { it.id == targetUserId } ?: return@forEach
                saveRole(user = user, actorUserId = actorUserId, actorName = actorName, roleOverride = role)
            }
        }
    }

    fun toggleTurmaLeader(user: AdminUserListItem) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(mutatingLeaderUserId = user.id, actionMessage = null, errorMessage = null)
            }
            try {
                repository.setUserTurmaLeader(
                    AdminUserTurmaLeaderUpdate(
                        targetUserId = user.id,
                        enabled = !user.isTurmaLeader,
                    ),
                )
                _uiState.update { state ->
                    state.copy(
                        users = state.users.map { row ->
                            if (row.id == user.id) row.copy(isTurmaLeader = !user.isTurmaLeader) else row
                        },
                        mutatingLeaderUserId = null,
                        actionMessage = if (!user.isTurmaLeader) {
                            "Usuário marcado como líder de turma."
                        } else {
                            "Liderança de turma removida."
                        },
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        mutatingLeaderUserId = null,
                        errorMessage = error.message ?: "Erro ao atualizar liderança de turma.",
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
                _uiState.update { page.toPermissionUsersUiState(current = it, append = !reset) }
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
        const val SearchDebounceMillis = 350L
    }
}
