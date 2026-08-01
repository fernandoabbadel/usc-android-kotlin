package com.example.usc1.ui.company

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.data.repository.SupabasePartnersRepository
import com.example.usc1.domain.model.PartnerContactVisibility
import com.example.usc1.domain.model.PartnerCoupon
import com.example.usc1.domain.model.PartnerLeadForm
import com.example.usc1.domain.model.PartnerRecord
import com.example.usc1.domain.model.PartnerRegistrationRules
import com.example.usc1.domain.model.PartnerScanRecord
import com.example.usc1.domain.model.PartnerStatus
import com.example.usc1.domain.model.PartnerTier
import com.example.usc1.domain.model.PartnersCatalog
import com.example.usc1.domain.repository.PartnersRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompanyLoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val message: String? = null,
    /** Id do parceiro autenticado; a navegação abre o painel quando ele aparece. */
    val authenticatedPartnerId: String = "",
)

data class CompanyDashboardUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partner: PartnerRecord? = null,
    val recentScans: List<PartnerScanRecord> = emptyList(),
)

data class CompanyHistoryUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partnerName: String = "",
    val scans: List<PartnerScanRecord> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
) {
    val hasPrevious: Boolean get() = page > 1
}

data class CompanyEditUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val partnerName: String = "",
    val whatsApp: String = "",
    val instagram: String = "",
    val site: String = "",
    val contactVisibility: PartnerContactVisibility = PartnerContactVisibility(),
    val coupons: List<PartnerCoupon> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null,
)

data class CompanyRegisterUiState(
    val step: Int = 1,
    val form: PartnerLeadForm = PartnerLeadForm(),
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val createdPartnerId: String = "",
)

/** `/empresa` — login do painel do parceiro. */
class CompanyLoginViewModel(
    private val repository: PartnersRepository = SupabasePartnersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CompanyLoginUiState())
    val uiState: StateFlow<CompanyLoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, message = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, message = null) }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(message = "Informe e-mail e senha.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null) }
            runCatching { repository.loginPartner(state.email, state.password) }
                .onSuccess { result ->
                    when {
                        result == null -> _uiState.update {
                            it.copy(isSubmitting = false, message = "E-mail não encontrado.")
                        }
                        !result.passwordValid -> _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                message = if (result.hasPasswordResetCode) {
                                    "Senha incorreta. Um código de reset foi emitido pelo suporte; " +
                                        "a confirmação do código acontece no painel web."
                                } else {
                                    "Senha incorreta."
                                },
                            )
                        }
                        result.status == PartnerStatus.Pending -> _uiState.update {
                            it.copy(isSubmitting = false, message = "Cadastro em análise. Aguarde aprovação.")
                        }
                        result.status == PartnerStatus.Disabled -> _uiState.update {
                            it.copy(isSubmitting = false, message = "Acesso desativado. Contate a Atlética.")
                        }
                        else -> _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                message = "Bem-vindo, ${result.name}!",
                                authenticatedPartnerId = result.id,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            message = error.message.orEmpty().ifBlank { "Erro de conexão." },
                        )
                    }
                }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(authenticatedPartnerId = "") }
    }
}

/** `/empresa/{id}` — painel do parceiro. */
class CompanyDashboardViewModel(
    private val repository: PartnersRepository = SupabasePartnersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CompanyDashboardUiState())
    val uiState: StateFlow<CompanyDashboardUiState> = _uiState.asStateFlow()

    private var lastPartnerId: String = ""

    fun load(partnerId: String, forceRefresh: Boolean = false) {
        val cleanPartnerId = partnerId.trim()
        if (cleanPartnerId.isBlank()) {
            _uiState.value = CompanyDashboardUiState(
                isLoading = false,
                errorMessage = "Empresa não encontrada.",
            )
            return
        }
        if (!forceRefresh && cleanPartnerId == lastPartnerId) return
        lastPartnerId = cleanPartnerId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val partner = repository.getPartnerById(cleanPartnerId, forceRefresh = true)
                val scans = repository.getPartnerScansPage(
                    partnerId = cleanPartnerId,
                    page = 1,
                    pageSize = RecentScansSize,
                )
                partner to scans.scans
            }.onSuccess { (partner, scans) ->
                _uiState.value = CompanyDashboardUiState(
                    isLoading = false,
                    errorMessage = if (partner == null) "Empresa não encontrada." else null,
                    partner = partner,
                    recentScans = scans,
                )
            }.onFailure { error ->
                _uiState.value = CompanyDashboardUiState(
                    isLoading = false,
                    errorMessage = error.message.orEmpty().ifBlank {
                        "Não foi possível carregar a empresa agora."
                    },
                )
            }
        }
    }

    private companion object {
        const val RecentScansSize = 10
    }
}

/** `/empresa/{id}/historico` — 20 scans por página. */
class CompanyHistoryViewModel(
    private val repository: PartnersRepository = SupabasePartnersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CompanyHistoryUiState())
    val uiState: StateFlow<CompanyHistoryUiState> = _uiState.asStateFlow()

    private var partnerId: String = ""

    fun load(partnerId: String, forceRefresh: Boolean = false) {
        val cleanPartnerId = partnerId.trim()
        if (cleanPartnerId.isBlank()) {
            _uiState.value = CompanyHistoryUiState(
                isLoading = false,
                errorMessage = "Empresa não encontrada.",
            )
            return
        }
        if (!forceRefresh && cleanPartnerId == this.partnerId) return
        this.partnerId = cleanPartnerId
        loadPage(1)
    }

    fun previousPage() {
        val page = _uiState.value.page
        if (page > 1) loadPage(page - 1)
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.hasMore) loadPage(state.page + 1)
    }

    private fun loadPage(page: Int) {
        val cleanPartnerId = partnerId
        if (cleanPartnerId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val partner = repository.getPartnerById(cleanPartnerId)
                val result = repository.getPartnerScansPage(
                    partnerId = cleanPartnerId,
                    page = page,
                    pageSize = PartnersCatalog.PageSize,
                )
                Triple(partner?.name.orEmpty(), result.scans, result.hasMore)
            }.onSuccess { (name, scans, hasMore) ->
                _uiState.value = CompanyHistoryUiState(
                    isLoading = false,
                    partnerName = name.ifBlank { "Empresa" },
                    scans = scans,
                    page = page,
                    hasMore = hasMore,
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar o histórico agora."
                        },
                    )
                }
            }
        }
    }
}

/** `/empresa/{id}/editar` — contatos públicos e cupons. */
class CompanyEditViewModel(
    private val repository: PartnersRepository = SupabasePartnersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CompanyEditUiState())
    val uiState: StateFlow<CompanyEditUiState> = _uiState.asStateFlow()

    private var partnerId: String = ""

    fun load(partnerId: String, forceRefresh: Boolean = false) {
        val cleanPartnerId = partnerId.trim()
        if (cleanPartnerId.isBlank()) {
            _uiState.value = CompanyEditUiState(
                isLoading = false,
                errorMessage = "Parceiro não encontrado.",
            )
            return
        }
        if (!forceRefresh && cleanPartnerId == this.partnerId) return
        this.partnerId = cleanPartnerId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val partner = repository.getPartnerById(cleanPartnerId, forceRefresh = true)
                val visibility = repository.getPartnerContactVisibility(cleanPartnerId)
                partner to visibility
            }.onSuccess { (partner, visibility) ->
                if (partner == null) {
                    _uiState.value = CompanyEditUiState(
                        isLoading = false,
                        errorMessage = "Parceiro não encontrado.",
                    )
                    return@onSuccess
                }
                _uiState.value = CompanyEditUiState(
                    isLoading = false,
                    partnerName = partner.name,
                    whatsApp = partner.whatsApp.ifBlank { partner.phone },
                    instagram = partner.instagram,
                    site = partner.site,
                    contactVisibility = visibility,
                    coupons = partner.coupons.ifEmpty { listOf(emptyCoupon()) },
                )
            }.onFailure { error ->
                _uiState.value = CompanyEditUiState(
                    isLoading = false,
                    errorMessage = error.message.orEmpty().ifBlank {
                        "Erro ao carregar parceiro."
                    },
                )
            }
        }
    }

    fun onWhatsAppChange(value: String) = _uiState.update { it.copy(whatsApp = value) }

    fun onInstagramChange(value: String) = _uiState.update { it.copy(instagram = value) }

    fun onSiteChange(value: String) = _uiState.update { it.copy(site = value) }

    fun toggleVisibility(field: CompanyContactField) {
        _uiState.update { state ->
            val current = state.contactVisibility
            state.copy(
                contactVisibility = when (field) {
                    CompanyContactField.WhatsApp -> current.copy(whatsApp = !current.whatsApp)
                    CompanyContactField.Instagram -> current.copy(instagram = !current.instagram)
                    CompanyContactField.Site -> current.copy(site = !current.site)
                },
            )
        }
    }

    fun addCoupon() {
        _uiState.update { it.copy(coupons = it.coupons + emptyCoupon()) }
    }

    fun removeCoupon(couponId: String) {
        _uiState.update { state -> state.copy(coupons = state.coupons.filterNot { it.id == couponId }) }
    }

    fun updateCoupon(couponId: String, transform: (PartnerCoupon) -> PartnerCoupon) {
        _uiState.update { state ->
            state.copy(
                coupons = state.coupons.map { coupon ->
                    if (coupon.id == couponId) transform(coupon) else coupon
                },
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        // Mesmas guardas do web: contato preenchido exige o aceite de visibilidade.
        if (state.whatsApp.isNotBlank() && !state.contactVisibility.whatsApp) {
            _uiState.update { it.copy(message = "Confirme o aviso de visibilidade do WhatsApp.") }
            return
        }
        if (state.instagram.isNotBlank() && !state.contactVisibility.instagram) {
            _uiState.update { it.copy(message = "Confirme o aviso de visibilidade do Instagram.") }
            return
        }
        if (state.site.isNotBlank() && !state.contactVisibility.site) {
            _uiState.update { it.copy(message = "Confirme o aviso de visibilidade do site.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            runCatching {
                repository.updatePartnerPublicProfile(
                    partnerId = partnerId,
                    whatsApp = state.whatsApp,
                    instagram = state.instagram,
                    site = state.site,
                    coupons = state.coupons,
                    contactVisibility = state.contactVisibility,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(isSaving = false, message = "Página pública do parceiro atualizada.")
                }
                load(partnerId, forceRefresh = true)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = error.message.orEmpty().ifBlank {
                            "Erro ao salvar configurações."
                        },
                    )
                }
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    private fun emptyCoupon(): PartnerCoupon = PartnerCoupon(
        id = UUID.randomUUID().toString(),
        title = "",
        rule = "",
        valueLabel = "",
        type = "percentual",
        active = true,
    )
}

enum class CompanyContactField { WhatsApp, Instagram, Site }

/** `/empresa/cadastro`. */
class CompanyRegisterViewModel(
    private val repository: PartnersRepository = SupabasePartnersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CompanyRegisterUiState())
    val uiState: StateFlow<CompanyRegisterUiState> = _uiState.asStateFlow()

    fun selectTier(tier: PartnerTier) {
        _uiState.update { it.copy(form = it.form.copy(tier = tier), step = 2, message = null) }
    }

    fun backToPlans() {
        _uiState.update { it.copy(step = 1, message = null) }
    }

    fun updateForm(transform: (PartnerLeadForm) -> PartnerLeadForm) {
        _uiState.update { it.copy(form = transform(it.form), message = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        PartnerRegistrationRules.validate(state.form)?.let { error ->
            _uiState.update { it.copy(message = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null) }
            runCatching { repository.createPartnerLead(state.form) }
                .onSuccess { partnerId ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            createdPartnerId = partnerId,
                            step = 3,
                            message = "Cadastro enviado. Aguarde a aprovação da atlética.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            message = error.message.orEmpty().ifBlank {
                                "Não foi possível enviar o cadastro."
                            },
                        )
                    }
                }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}
