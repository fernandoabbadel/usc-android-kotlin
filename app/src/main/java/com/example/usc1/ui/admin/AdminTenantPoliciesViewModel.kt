package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseAdminTenantPoliciesRepository
import com.example.usc1.domain.model.TenantPolicyCatalog
import com.example.usc1.domain.model.TenantPolicyDocument
import com.example.usc1.domain.repository.AdminTenantPoliciesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminTenantPoliciesViewModel(
    private val repository: AdminTenantPoliciesRepository = SupabaseAdminTenantPoliciesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminTenantPoliciesUiState())
    val uiState: StateFlow<AdminTenantPoliciesUiState> = _uiState.asStateFlow()

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenant = session.tenant
        if (tenant == null) {
            _uiState.value = AdminTenantPoliciesUiState(
                isLoading = false,
                policies = TenantPolicyCatalog.templates,
            )
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    saveMessage = null,
                    tenantName = tenant.name,
                    tenantSlug = tenant.slug,
                )
            }
            try {
                _uiState.value = repository.getPoliciesBundle(
                    tenantName = tenant.name,
                    tenantSlug = tenant.slug,
                    forceRefresh = forceRefresh,
                ).toUiState()
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar políticas.",
                    )
                }
            }
        }
    }

    fun updatePolicyContent(module: String, content: String) {
        val nextContent = content.take(TenantPolicyCatalog.MaxContentLength)
        updatePolicy(module) { policy ->
            val canRemainVisible = nextContent.trim().isNotBlank()
            policy.copy(
                content = nextContent,
                visible = policy.visible && canRemainVisible,
            )
        }
    }

    fun togglePolicyVisibility(module: String) {
        updatePolicy(module) { policy ->
            val canBeVisible = policy.content.trim().isNotBlank()
            policy.copy(visible = if (canBeVisible) !policy.visible else false)
        }
    }

    fun save() {
        val current = _uiState.value
        if (!current.hasActiveTenant) {
            _uiState.update { it.copy(saveMessage = "Tenant não identificado.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null,
                    saveMessage = null,
                )
            }
            try {
                val saved = repository.savePolicies(current.policies)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        policies = saved,
                        saveMessage = "Políticas salvas.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Erro ao salvar políticas.",
                        saveMessage = error.message ?: "Erro ao salvar políticas.",
                    )
                }
            }
        }
    }

    private fun updatePolicy(
        module: String,
        transform: (TenantPolicyDocument) -> TenantPolicyDocument,
    ) {
        _uiState.update { state ->
            state.copy(
                saveMessage = null,
                policies = state.policies.map { policy ->
                    if (policy.module == module) transform(policy) else policy
                },
            )
        }
    }
}
