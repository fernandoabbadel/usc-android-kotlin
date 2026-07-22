package com.example.usc1.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabasePartnersRepository
import com.example.usc1.domain.model.PartnerForm
import com.example.usc1.domain.model.PartnerRecord
import com.example.usc1.domain.model.PartnerStatus
import com.example.usc1.domain.model.PartnersCatalog
import com.example.usc1.domain.repository.PartnersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminPartnersViewModel(
    private val repository: PartnersRepository = SupabasePartnersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminPartnersUiState())
    val uiState: StateFlow<AdminPartnersUiState> = _uiState.asStateFlow()

    fun loadActive(page: Int = 1, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    mode = AdminPartnersMode.Active,
                    isLoading = page == 1,
                    isLoadingMore = page > 1,
                    errorMessage = null,
                    actionMessage = null,
                    page = page.coerceAtLeast(1),
                )
            }
            try {
                val counts = repository.getAdminPartnersTierCounts(forceRefresh)
                val result = repository.getAdminPartnersPage(
                    status = PartnerStatus.Active,
                    page = page,
                    pageSize = PartnersCatalog.PageSize,
                    forceRefresh = forceRefresh,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        tenantId = result.tenantId,
                        counts = counts,
                        partners = result.partners,
                        hasMore = result.hasMore,
                        page = result.page,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = error.message ?: "Erro ao carregar parceiros ativos.",
                    )
                }
            }
        }
    }

    fun loadCompanies(page: Int = 1, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val status = _uiState.value.statusFilter
            _uiState.update {
                it.copy(
                    mode = AdminPartnersMode.Companies,
                    isLoading = true,
                    errorMessage = null,
                    actionMessage = null,
                    page = page.coerceAtLeast(1),
                )
            }
            try {
                val result = repository.getAdminPartnersPage(
                    status = status,
                    page = page,
                    pageSize = PartnersCatalog.PageSize,
                    forceRefresh = forceRefresh,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tenantId = result.tenantId,
                        partners = result.partners,
                        hasMore = result.hasMore,
                        page = result.page,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar empresas.",
                    )
                }
            }
        }
    }

    fun loadBi(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    mode = AdminPartnersMode.Bi,
                    isLoading = true,
                    errorMessage = null,
                    actionMessage = null,
                )
            }
            try {
                val bundle = repository.getAdminPartnersBundle(forceRefresh)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tenantId = bundle.tenantId,
                        partners = bundle.partners,
                        scans = bundle.scans,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar BI de parceiros.",
                    )
                }
            }
        }
    }

    fun loadHistory(page: Int = 1, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    mode = AdminPartnersMode.History,
                    isLoading = true,
                    errorMessage = null,
                    actionMessage = null,
                    page = page.coerceAtLeast(1),
                )
            }
            try {
                val result = repository.getAdminPartnerScansPage(
                    page = page,
                    pageSize = PartnersCatalog.PageSize,
                    forceRefresh = forceRefresh,
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tenantId = result.tenantId,
                        scans = result.scans,
                        hasMore = result.hasMore,
                        page = result.page,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Erro ao carregar histórico.",
                    )
                }
            }
        }
    }

    fun setSearch(value: String) {
        _uiState.update { it.copy(search = value) }
    }

    fun setStatusFilter(status: PartnerStatus?) {
        _uiState.update { it.copy(statusFilter = status, page = 1) }
        loadCompanies(page = 1, forceRefresh = true)
    }

    fun toggleBiMetric() {
        _uiState.update {
            it.copy(
                biMetric = if (it.biMetric == AdminPartnersBiMetric.Quantity) {
                    AdminPartnersBiMetric.Value
                } else {
                    AdminPartnersBiMetric.Quantity
                },
            )
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.page <= 1) return
        loadForCurrentMode(state.page - 1)
    }

    fun nextPage() {
        val state = _uiState.value
        if (!state.hasMore) return
        loadForCurrentMode(state.page + 1)
    }

    fun openCreateForm() {
        _uiState.update {
            it.copy(
                form = PartnerForm(),
                errorMessage = null,
                actionMessage = null,
            )
        }
    }

    fun openEditForm(partner: PartnerRecord) {
        _uiState.update {
            it.copy(
                form = PartnerForm(
                    partnerId = partner.id,
                    name = partner.name,
                    category = partner.category,
                    tier = partner.tier,
                    status = partner.status,
                    cnpj = partner.cnpj,
                    responsible = partner.responsible,
                    email = partner.email,
                    phone = partner.phone,
                    description = partner.description,
                    address = partner.address,
                    businessHours = partner.businessHours,
                    instagram = partner.instagram,
                    whatsApp = partner.whatsApp,
                    site = partner.site,
                    logoUrl = partner.logoUrl,
                    coverUrl = partner.coverUrl,
                ),
                errorMessage = null,
                actionMessage = null,
            )
        }
    }

    fun closeForm() {
        _uiState.update { it.copy(form = null) }
    }

    fun updateForm(transform: (PartnerForm) -> PartnerForm) {
        _uiState.update { state ->
            state.copy(form = state.form?.let(transform))
        }
    }

    fun saveForm() {
        val form = _uiState.value.form ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(mutatingId = "form", errorMessage = null, actionMessage = null) }
            try {
                repository.savePartner(form)
                _uiState.update {
                    it.copy(
                        mutatingId = "",
                        form = null,
                        actionMessage = if (form.partnerId.isBlank()) "Parceiro criado." else "Parceiro salvo.",
                    )
                }
                loadCompanies(page = 1, forceRefresh = true)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        mutatingId = "",
                        errorMessage = error.message ?: "Erro ao salvar parceiro.",
                    )
                }
            }
        }
    }

    fun toggleStatus(partner: PartnerRecord) {
        val next = if (partner.status == PartnerStatus.Active) PartnerStatus.Disabled else PartnerStatus.Active
        viewModelScope.launch {
            _uiState.update { it.copy(mutatingId = partner.id, errorMessage = null, actionMessage = null) }
            try {
                repository.setPartnerStatus(partner.id, next)
                _uiState.update { it.copy(mutatingId = "", actionMessage = "Status atualizado.") }
                loadCompanies(page = _uiState.value.page, forceRefresh = true)
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        mutatingId = "",
                        errorMessage = error.message ?: "Erro ao atualizar status.",
                    )
                }
            }
        }
    }

    fun requestPasswordReset(partner: PartnerRecord) {
        viewModelScope.launch {
            _uiState.update { it.copy(mutatingId = "reset:${partner.id}", errorMessage = null, actionMessage = null) }
            try {
                val reset = repository.requestPasswordReset(partner.id)
                _uiState.update {
                    it.copy(
                        mutatingId = "",
                        actionMessage = "Código de reset gerado: ${reset.code}. Ele expira em 30 minutos.",
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        mutatingId = "",
                        errorMessage = error.message ?: "Erro ao gerar código de reset.",
                    )
                }
            }
        }
    }

    private fun loadForCurrentMode(page: Int) {
        when (_uiState.value.mode) {
            AdminPartnersMode.Active -> loadActive(page = page)
            AdminPartnersMode.Companies -> loadCompanies(page = page)
            AdminPartnersMode.Bi -> loadBi(forceRefresh = true)
            AdminPartnersMode.History -> loadHistory(page = page)
        }
    }
}
