package com.example.usc1.ui.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseGuideRepository
import com.example.usc1.domain.repository.GuideRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GuideViewModel(
    private val repository: GuideRepository = SupabaseGuideRepository(),
) : ViewModel() {
    private val _guideState = MutableStateFlow(GuideUiState())
    val guideState: StateFlow<GuideUiState> = _guideState.asStateFlow()

    private val _legalState = MutableStateFlow(LegalUiState())
    val legalState: StateFlow<LegalUiState> = _legalState.asStateFlow()

    private var lastLoadedTenantId: String? = null
    private var loadJob: Job? = null

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenantId = session.tenant?.id.orEmpty().trim()
        if (tenantId.isBlank()) {
            loadJob?.cancel()
            lastLoadedTenantId = null
            _guideState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Selecione uma atlética para carregar o guia.",
                )
            }
            _legalState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Selecione uma atlética para carregar documentos legais.",
                )
            }
            return
        }
        if (!forceRefresh && lastLoadedTenantId == tenantId && _guideState.value.errorMessage == null && _legalState.value.errorMessage == null) {
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _guideState.update { it.copy(isLoading = true, errorMessage = null) }
            _legalState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val guideDeferred = async { repository.getGuide(tenantId) }
                val legalDeferred = async { repository.getLegalDocs(tenantId) }
                _guideState.value = guideDeferred.await().copy(isLoading = false)
                _legalState.value = legalDeferred.await().copy(isLoading = false)
                lastLoadedTenantId = tenantId
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                lastLoadedTenantId = null
                val message = error.message ?: "Não foi possível carregar a central de informações."
                _guideState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = message,
                    )
                }
                _legalState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = message,
                    )
                }
            }
        }
    }
}
