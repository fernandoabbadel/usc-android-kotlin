package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseAdminDashboardModulesRepository
import com.example.usc1.domain.model.TenantAppModulesCatalog
import com.example.usc1.domain.repository.AdminDashboardModulesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminDashboardModulesViewModel(
    private val repository: AdminDashboardModulesRepository = SupabaseAdminDashboardModulesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminDashboardModulesUiState())
    val uiState: StateFlow<AdminDashboardModulesUiState> = _uiState.asStateFlow()

    fun load(session: UserSession, forceRefresh: Boolean = false) {
        val tenant = session.tenant
        if (tenant == null) {
            _uiState.value = AdminDashboardModulesUiState(
                isLoading = false,
                groups = TenantAppModulesCatalog.groupDefinitions(
                    modules = TenantAppModulesCatalog.defaultConfig.modules,
                    profileAppModules = emptyMap(),
                ),
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
                _uiState.value = repository.getModulesBundle(
                    tenantName = tenant.name,
                    tenantSlug = tenant.slug,
                    forceRefresh = forceRefresh,
                ).toUiState()
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar modulos da tenant.",
                    )
                }
            }
        }
    }

    fun toggleModule(key: String) {
        _uiState.update { state ->
            state.copy(
                saveMessage = null,
                modules = state.modules.toMutableMap().also { modules ->
                    modules[key] = modules[key] == false
                },
            )
        }
    }

    fun save() {
        val current = _uiState.value
        if (!current.hasActiveTenant) {
            _uiState.update {
                it.copy(saveMessage = "Selecione uma tenant valida antes de salvar.")
            }
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
                repository.saveModulesConfig(current.toConfig())
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveMessage = "Modulos visiveis atualizados para esta tenant.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveMessage = "Erro ao salvar modulos da tenant.",
                        errorMessage = error.message,
                    )
                }
            }
        }
    }
}
