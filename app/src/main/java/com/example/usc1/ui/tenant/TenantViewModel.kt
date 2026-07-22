package com.example.usc1.ui.tenant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.data.repository.WebPublicTenantRepository
import com.example.usc1.domain.repository.PublicTenantRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TenantViewModel(
    private val repository: PublicTenantRepository = WebPublicTenantRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(TenantUiState())
    val uiState: StateFlow<TenantUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadDirectory()
    }

    fun loadDirectory() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectingTenantId = null,
                    errorMessage = null,
                )
            }
            try {
                val tenants = repository.getDirectory().map { tenant -> tenant.toTenantIdentity() }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tenants = tenants,
                        errorMessage = if (tenants.isEmpty()) {
                            "Nenhuma atlética pública está disponível agora."
                        } else {
                            null
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tenants = emptyList(),
                        errorMessage = error.message
                            ?: "Não foi possível carregar as atléticas agora.",
                    )
                }
            }
        }
    }

    fun selectTenant(
        tenant: TenantIdentity,
        onResolved: (TenantContext) -> Unit,
    ) {
        if (_uiState.value.isSelecting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectingTenantId = tenant.id,
                    errorMessage = null,
                )
            }
            try {
                val resolvedTenant = repository.resolveActiveTenant(
                    tenantId = tenant.id,
                    tenantSlug = tenant.slug,
                ) ?: error("A atlética selecionada não está mais disponível.")

                onResolved(resolvedTenant.toGuestTenantContext())
                _uiState.update { it.copy(selectingTenantId = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        selectingTenantId = null,
                        errorMessage = error.message
                            ?: "Não foi possível validar a atlética selecionada.",
                    )
                }
            }
        }
    }
}
