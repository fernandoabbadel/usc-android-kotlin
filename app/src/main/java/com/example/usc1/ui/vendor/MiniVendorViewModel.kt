package com.example.usc1.ui.vendor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseMiniVendorRepository
import com.example.usc1.domain.repository.MiniVendorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MiniVendorViewModel(
    private val repository: MiniVendorRepository = SupabaseMiniVendorRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(MiniVendorUiState())
    val uiState: StateFlow<MiniVendorUiState> = _uiState.asStateFlow()

    private var lastLoadKey: String = ""

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        val userId = session.user?.id.orEmpty().trim()
        val key = "$tenantId::$userId"
        if (tenantId.isBlank() || userId.isBlank()) {
            lastLoadKey = ""
            _uiState.value = MiniVendorUiState(
                statusLabel = "Sessão necessária para carregar o Mini Vendor.",
            )
            return
        }
        if (!forceRefresh && key == lastLoadKey && (_uiState.value.hasProfile || _uiState.value.errorMessage != null)) {
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
                repository.getDashboard(tenantId = tenantId, userId = userId)
            }.onSuccess { dashboard ->
                _uiState.value = dashboard.copy(isLoading = false, errorMessage = null)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar seu Mini Vendor agora."
                        },
                    )
                }
            }
        }
    }
}
