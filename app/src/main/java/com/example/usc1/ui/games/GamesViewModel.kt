package com.example.usc1.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseGamesRepository
import com.example.usc1.domain.repository.GamesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GamesViewModel(
    private val repository: GamesRepository = SupabaseGamesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(GamesUiState())
    val uiState: StateFlow<GamesUiState> = _uiState.asStateFlow()
    private var lastLoadedKey: String? = null
    private var loadJob: Job? = null

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val userName = session.user?.name.orEmpty().trim()
        val key = "$tenantId:$userId"

        if (tenantId.isBlank()) {
            loadJob?.cancel()
            lastLoadedKey = null
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Selecione uma atlética para carregar a arena.",
                )
            }
            return
        }
        if (!forceRefresh && lastLoadedKey == key && _uiState.value.errorMessage == null) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val next = repository.getGamesHub(
                    tenantId = tenantId,
                    userId = userId,
                    userName = userName,
                )
                lastLoadedKey = key
                _uiState.value = next.copy(isLoading = false)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastLoadedKey = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Não foi possível carregar a arena.",
                    )
                }
            }
        }
    }
}
