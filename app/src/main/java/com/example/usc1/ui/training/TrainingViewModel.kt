package com.example.usc1.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseTrainingRepository
import com.example.usc1.domain.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrainingViewModel(
    private val repository: TrainingRepository = SupabaseTrainingRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private var lastLoadKey: String = ""

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val user = session.user
        val userId = user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) {
            lastLoadKey = ""
            _uiState.value = TrainingUiState(
                activeChallengeSubtitle = "Sessão necessária",
                activeChallengeDescription = "Entre com sua conta para carregar os treinos da atlética.",
            )
            return
        }
        if (!forceRefresh && key == lastLoadKey && (_uiState.value.sessions.isNotEmpty() || _uiState.value.errorMessage != null)) {
            return
        }
        lastLoadKey = key

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }
            runCatching {
                repository.getTrainingHub(
                    tenantId = tenantId,
                    userId = userId,
                    userName = user?.name.orEmpty(),
                )
            }.onSuccess { dashboard ->
                _uiState.value = dashboard.copy(isLoading = false, errorMessage = null)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar os treinos agora."
                        },
                    )
                }
            }
        }
    }
}
