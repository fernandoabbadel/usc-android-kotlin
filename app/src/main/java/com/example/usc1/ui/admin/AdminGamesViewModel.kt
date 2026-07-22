package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabaseAdminGamesRepository
import com.example.usc1.domain.model.AdminArenaUser
import com.example.usc1.domain.repository.AdminGamesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminGamesViewModel(
    private val repository: AdminGamesRepository = SupabaseAdminGamesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminGamesUiState())
    val uiState: StateFlow<AdminGamesUiState> = _uiState.asStateFlow()

    fun load(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val users = repository.getArenaUsers(forceRefresh)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        users = users,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao buscar usuários.",
                    )
                }
            }
        }
    }

    fun setSearchTerm(value: String) {
        _uiState.update { it.copy(searchTerm = value) }
    }

    fun selectUser(user: AdminArenaUser?) {
        _uiState.update { it.copy(selectedUser = user) }
    }
}
