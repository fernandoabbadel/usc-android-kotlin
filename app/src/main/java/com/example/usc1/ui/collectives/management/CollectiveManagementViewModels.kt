package com.example.usc1.ui.collectives.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseCollectiveManagementRepository
import com.example.usc1.domain.repository.CollectiveManagementRepository
import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.LeagueRoleCatalog
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Painel de gestão do coletivo: seleção, hub, informações e membros.
 *
 * Web: `CommissionManagementGate`/`DirectoryManagementGate` mais
 * `LigasAdminPageContent` nos modos `hub`, `lockedTab="visual"` e `lockedTab="members"`.
 */
class CollectiveManagementViewModel(
    private val repository: CollectiveManagementRepository = SupabaseCollectiveManagementRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectiveManagementUiState())
    val uiState: StateFlow<CollectiveManagementUiState> = _uiState.asStateFlow()

    /**
     * Gate do web: carrega os coletivos gerenciáveis e, quando a rota já traz um id
     * (ou só existe um registro), abre direto o painel dele.
     */
    fun load(
        session: UserSession,
        kind: CollectiveKind,
        collectiveId: String = "",
        nav: CollectiveManagementNav = CollectiveManagementNav.Home,
    ) {
        val tenantId = session.tenant?.id.orEmpty()
        val userId = session.user?.id.orEmpty()
        val isMaster = session.user?.role == UserRole.Master

        viewModelScope.launch {
            _uiState.update { it.copy(kind = kind, activeNav = nav, isLoading = true, errorMessage = null) }

            runCatching {
                repository.getManagedCollectives(
                    tenantId = tenantId,
                    userId = userId,
                    kind = kind,
                    isPlatformMaster = isMaster,
                )
            }.onSuccess { managed ->
                // `nextSelectedId` do gate: rota, senão o único registro disponível.
                val selected = managed.firstOrNull { matchesRouteSegment(it, collectiveId) }
                    ?: managed.singleOrNull()

                _uiState.update {
                    it.copy(
                        kind = kind,
                        managedCollectives = managed,
                        selected = selected,
                        activeNav = nav,
                        isLoading = selected != null,
                        errorMessage = null,
                    )
                }

                if (selected != null) loadSection(session, selected, nav)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        kind = kind,
                        isLoading = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Não foi possível carregar a gestão ${kind.entityArticle} ${kind.entityLabel}."
                        },
                    )
                }
            }
        }
    }

    /** `selectCommission` do gate: escolhe o coletivo na tela de seleção. */
    fun selectCollective(session: UserSession, collective: ManagedCollective) {
        _uiState.update { it.copy(selected = collective, isLoading = true) }
        viewModelScope.launch { loadSection(session, collective, _uiState.value.activeNav) }
    }

    /** Carrega apenas o que a seção precisa (o hub não busca membros nem informações). */
    private suspend fun loadSection(
        session: UserSession,
        collective: ManagedCollective,
        nav: CollectiveManagementNav,
    ) {
        val tenantId = session.tenant?.id.orEmpty()

        when (nav) {
            CollectiveManagementNav.Info -> {
                runCatching { repository.getInfoForm(tenantId, collective.id) }
                    .onSuccess { form -> _uiState.update { it.copy(info = form, isLoading = false) } }
                    .onFailure { error -> failSection(error, "Não foi possível carregar as informações.") }
            }

            CollectiveManagementNav.Members -> {
                runCatching { repository.getMembers(tenantId, collective.id) }
                    .onSuccess { (members, requests) ->
                        _uiState.update {
                            it.copy(
                                members = it.members.copy(members = members, requests = requests),
                                isLoading = false,
                            )
                        }
                    }
                    .onFailure { error -> failSection(error, "Não foi possível carregar os membros.") }
            }

            else -> _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun failSection(error: Throwable, fallback: String) {
        _uiState.update {
            it.copy(isLoading = false, errorMessage = error.message.orEmpty().ifBlank { fallback })
        }
    }

    // ------------------------------------------------------------------
    // Informações
    // ------------------------------------------------------------------

    fun updateInfo(transform: (CollectiveInfoForm) -> CollectiveInfoForm) {
        _uiState.update { it.copy(info = transform(it.info), actionMessage = null) }
    }

    /** "Adicionar link" do bloco de links públicos. */
    fun addLink() {
        _uiState.update { state ->
            if (state.info.links.size >= CollectiveInfoForm.LinksMaxCount) return@update state
            val next = state.info.links + CollectiveLinkDraft(id = "link-${System.currentTimeMillis()}")
            state.copy(info = state.info.copy(links = next))
        }
    }

    fun updateLink(linkId: String, transform: (CollectiveLinkDraft) -> CollectiveLinkDraft) {
        _uiState.update { state ->
            val next = state.info.links.map { if (it.id == linkId) transform(it) else it }
            state.copy(info = state.info.copy(links = next))
        }
    }

    fun removeLink(linkId: String) {
        _uiState.update { state ->
            state.copy(info = state.info.copy(links = state.info.links.filterNot { it.id == linkId }))
        }
    }

    /** `handleSaveVisualSection` do web. */
    fun saveInfo(session: UserSession) {
        val state = _uiState.value
        val collective = state.selected ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, actionMessage = null, errorMessage = null) }

            runCatching {
                repository.saveInfo(session.tenant?.id.orEmpty(), collective.id, state.info)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        actionMessage = "Informações salvas.",
                        info = it.info.copy(sendNotification = false),
                        // O cabeçalho acompanha o nome/sigla recém-salvos.
                        selected = collective.copy(
                            name = it.info.name.trim().ifBlank { collective.name },
                            acronym = it.info.acronym.trim().ifBlank { collective.acronym },
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message.orEmpty().ifBlank { "Erro ao salvar informações." },
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Membros
    // ------------------------------------------------------------------

    fun openUserSearch(session: UserSession) {
        _uiState.update { it.copy(members = it.members.copy(isSearchOpen = true, isLoadingUsers = true)) }

        viewModelScope.launch {
            runCatching { repository.getUserOptions(session.tenant?.id.orEmpty()) }
                .onSuccess { options ->
                    _uiState.update {
                        it.copy(members = it.members.copy(userOptions = options, isLoadingUsers = false))
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            members = it.members.copy(isLoadingUsers = false),
                            errorMessage = "Erro ao carregar usuários.",
                        )
                    }
                }
        }
    }

    fun closeUserSearch() {
        _uiState.update { it.copy(members = it.members.copy(isSearchOpen = false, searchTerm = "")) }
    }

    fun updateMemberSearch(term: String) {
        _uiState.update { it.copy(members = it.members.copy(searchTerm = term)) }
    }

    /** `addMember` do web: entra no rascunho com o cargo padrão. */
    fun addMember(option: CollectiveUserOption) {
        _uiState.update { state ->
            if (state.members.members.any { it.id == option.id }) return@update state
            val next = state.members.members + CollectiveMemberDraft(
                id = option.id,
                name = option.name,
                role = LeagueRoleCatalog.DefaultRole,
                photoUrl = option.photoUrl,
                profileLink = "/perfil/${option.id}",
                persisted = false,
            )
            state.copy(
                members = state.members.copy(members = next, isSearchOpen = false, searchTerm = ""),
                actionMessage = "Usuário adicionado! Defina o cargo.",
            )
        }
    }

    fun updateMemberRole(memberId: String, role: String) {
        _uiState.update { state ->
            val next = state.members.members.map {
                if (it.id == memberId) it.copy(role = LeagueRoleCatalog.resolveRoleLabel(role)) else it
            }
            state.copy(members = state.members.copy(members = next))
        }
    }

    fun removeMember(memberId: String) {
        _uiState.update { state ->
            state.copy(
                members = state.members.copy(members = state.members.members.filterNot { it.id == memberId }),
            )
        }
    }

    fun updateRequestRole(requestId: String, role: String) {
        _uiState.update { state ->
            val next = state.members.requests.map {
                if (it.id == requestId) it.copy(requestedRole = LeagueRoleCatalog.resolveRoleLabel(role)) else it
            }
            state.copy(members = state.members.copy(requests = next))
        }
    }

    /** `approveMemberRequest`: entra no rascunho; só o "salvar membros" persiste. */
    fun approveRequest(requestId: String) {
        _uiState.update { state ->
            val request = state.members.requests.firstOrNull { it.id == requestId } ?: return@update state
            val role = LeagueRoleCatalog.resolveRoleLabel(request.requestedRole)
            val existingIndex = state.members.members.indexOfFirst { it.id == request.userId }

            val nextMembers = if (existingIndex >= 0) {
                state.members.members.mapIndexed { index, member ->
                    if (index != existingIndex) member
                    else member.copy(
                        role = role,
                        name = member.name.ifBlank { request.name },
                        photoUrl = member.photoUrl ?: request.photoUrl,
                    )
                }
            } else {
                state.members.members + CollectiveMemberDraft(
                    id = request.userId,
                    name = request.name.ifBlank { "Sem Nome" },
                    role = role,
                    photoUrl = request.photoUrl,
                    profileLink = "/perfil/${request.userId}",
                    persisted = false,
                )
            }

            state.copy(
                members = state.members.copy(
                    members = nextMembers.sortedWith(memberOrder()),
                    requests = state.members.requests.filterNot { it.id == requestId },
                ),
                actionMessage = if (existingIndex >= 0) {
                    "Solicitação aceita e cargo atualizado no rascunho."
                } else {
                    "Solicitação aceita e membro adicionado ao rascunho."
                },
            )
        }
    }

    fun rejectRequest(requestId: String) {
        _uiState.update { state ->
            state.copy(
                members = state.members.copy(
                    requests = state.members.requests.filterNot { it.id == requestId },
                ),
                actionMessage = "Solicitação removida do rascunho.",
            )
        }
    }

    /** `handleSaveMembersSection` do web. */
    fun saveMembers(session: UserSession) {
        val state = _uiState.value
        val collective = state.selected ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, actionMessage = null, errorMessage = null) }

            runCatching {
                repository.saveMembers(
                    tenantId = session.tenant?.id.orEmpty(),
                    collectiveId = collective.id,
                    members = state.members.members,
                    requests = state.members.requests,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        actionMessage = "Membros sincronizados.",
                        members = it.members.copy(
                            members = it.members.members.map { member -> member.copy(persisted = true) },
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message.orEmpty().ifBlank { "Erro ao salvar membros." },
                    )
                }
            }
        }
    }

    fun consumeActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    private fun memberOrder(): Comparator<CollectiveMemberDraft> =
        compareBy<CollectiveMemberDraft> { LeagueRoleCatalog.roleImportance(it.role) }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }

    /** `matchesCommissionRouteSegment`: id, turma, sigla ou nome normalizados. */
    private fun matchesRouteSegment(collective: ManagedCollective, segment: String): Boolean {
        val normalizedSegment = normalizeToken(segment)
        if (normalizedSegment.isBlank()) return false
        return listOf(collective.id, collective.turmaId, collective.acronym, collective.name)
            .any { normalizeToken(it) == normalizedSegment }
    }

    private fun normalizeToken(value: String): String =
        value.trim().lowercase().replace(Regex("[^a-z0-9-]+"), "-").trim('-')
}

/**
 * Loja do coletivo.
 *
 * Web: `app/ligas/LeagueStoreAdminPage.tsx` nos modos `overview`, `products`,
 * `pending` e `approved`.
 */
class CollectiveStoreAdminViewModel(
    private val repository: CollectiveManagementRepository = SupabaseCollectiveManagementRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectiveStoreAdminUiState())
    val uiState: StateFlow<CollectiveStoreAdminUiState> = _uiState.asStateFlow()

    fun load(session: UserSession, collective: ManagedCollective, mode: CollectiveStoreMode) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    kind = collective.kind,
                    mode = mode,
                    collective = collective,
                    isLoading = true,
                    errorMessage = null,
                )
            }
            reload(session, collective, mode)
        }
    }

    private suspend fun reload(
        session: UserSession,
        collective: ManagedCollective,
        mode: CollectiveStoreMode,
    ) {
        runCatching { repository.getStore(session.tenant?.id.orEmpty(), collective, mode) }
            .onSuccess { state -> _uiState.value = state }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message.orEmpty().ifBlank {
                            "Erro ao carregar loja ${collective.kind.entityArticle} ${collective.kind.entityLabel}."
                        },
                    )
                }
            }
    }

    fun updateCover(url: String) {
        _uiState.update { it.copy(storeCoverUrl = url.take(400)) }
    }

    fun updateColor(color: String) {
        _uiState.update { it.copy(storeColor = color.take(40)) }
    }

    /** `handleSaveStore` do web. */
    fun saveStore(session: UserSession, visible: Boolean? = null) {
        val state = _uiState.value
        val collective = state.collective ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, actionMessage = null, errorMessage = null) }
            runCatching {
                repository.saveStoreCategory(
                    tenantId = session.tenant?.id.orEmpty(),
                    collective = collective,
                    coverUrl = state.storeCoverUrl,
                    color = state.storeColor,
                    visible = visible ?: state.categoryVisible,
                )
            }.onSuccess {
                reload(session, collective, state.mode)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        actionMessage = "Loja ${collective.kind.entityArticle} ${collective.kind.entityLabel} atualizada.",
                    )
                }
            }.onFailure { error -> failAction(error, "Erro ao salvar loja.") }
        }
    }

    /** `handleToggleProducts` do web. */
    fun toggleAllProducts(session: UserSession, visible: Boolean) {
        val state = _uiState.value
        val collective = state.collective ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, actionMessage = null, errorMessage = null) }
            runCatching {
                repository.setAllProductsActive(session.tenant?.id.orEmpty(), collective.id, visible)
            }.onSuccess {
                reload(session, collective, state.mode)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        actionMessage = if (visible) "Produtos exibidos." else "Produtos ocultados.",
                    )
                }
            }.onFailure { error -> failAction(error, "Erro ao atualizar produtos.") }
        }
    }

    /** `openProductForm` do web. */
    fun openProductForm(product: CollectiveAdminProduct? = null) {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                form = if (product == null) {
                    CollectiveProductForm()
                } else {
                    CollectiveProductForm(
                        productId = product.id,
                        name = product.name,
                        price = formatAmountInput(product.price),
                        oldPrice = product.oldPrice.takeIf { value -> value > 0.0 }?.let(::formatAmountInput).orEmpty(),
                        status = product.status,
                        stock = product.stock.takeIf { value -> value > 0 }?.toString().orEmpty(),
                        lot = product.lot.ifBlank { "geral" },
                        imageUrl = product.imageUrl.orEmpty(),
                        description = product.description,
                        useOwnPayment = false,
                        pixKey = state.collectivePixKey,
                        pixBank = state.collectivePixBank,
                        pixHolder = state.collectivePixHolder,
                        tagLabel = product.tagLabel,
                    )
                },
            )
        }
    }

    fun closeProductForm() {
        _uiState.update { it.copy(form = null) }
    }

    fun updateProductForm(transform: (CollectiveProductForm) -> CollectiveProductForm) {
        _uiState.update { state ->
            val form = state.form ?: return@update state
            state.copy(form = transform(form))
        }
    }

    /** `handleSaveProduct` do web. */
    fun saveProduct(session: UserSession) {
        val state = _uiState.value
        val collective = state.collective ?: return
        val form = state.form ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, actionMessage = null, errorMessage = null) }
            runCatching {
                repository.saveProduct(session.tenant?.id.orEmpty(), collective, form)
            }.onSuccess {
                reload(session, collective, state.mode)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        form = null,
                        actionMessage = if (form.isEditing) "Produto atualizado." else "Produto criado.",
                    )
                }
            }.onFailure { error -> failAction(error, "Erro ao salvar produto.") }
        }
    }

    fun setProductActive(session: UserSession, product: CollectiveAdminProduct) {
        val state = _uiState.value
        val collective = state.collective ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                repository.setProductActive(
                    tenantId = session.tenant?.id.orEmpty(),
                    collectiveId = collective.id,
                    productId = product.id,
                    active = !product.active,
                )
            }.onSuccess {
                reload(session, collective, state.mode)
                _uiState.update { it.copy(isSaving = false) }
            }.onFailure { error -> failAction(error, "Erro ao atualizar produto.") }
        }
    }

    /** `handleApprove` do web. */
    fun approveOrder(session: UserSession, order: CollectiveStoreOrder) {
        val state = _uiState.value
        val collective = state.collective ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(busyOrderId = order.id, errorMessage = null, actionMessage = null) }
            runCatching {
                repository.approveOrder(
                    tenantId = session.tenant?.id.orEmpty(),
                    collectiveId = collective.id,
                    orderId = order.id,
                    approvedBy = session.user?.id.orEmpty(),
                )
            }.onSuccess {
                reload(session, collective, state.mode)
                _uiState.update { it.copy(busyOrderId = "", actionMessage = "Pedido aprovado.") }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        busyOrderId = "",
                        errorMessage = error.message.orEmpty().ifBlank { "Erro ao aprovar pedido." },
                    )
                }
            }
        }
    }

    /** `handleOrderStatus` do web (`pendente`, `rejected`, `delivered`). */
    fun setOrderStatus(session: UserSession, order: CollectiveStoreOrder, status: String) {
        val state = _uiState.value
        val collective = state.collective ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(busyOrderId = order.id, errorMessage = null, actionMessage = null) }
            runCatching {
                repository.setOrderStatus(
                    tenantId = session.tenant?.id.orEmpty(),
                    collectiveId = collective.id,
                    orderId = order.id,
                    status = status,
                )
            }.onSuccess {
                reload(session, collective, state.mode)
                _uiState.update { it.copy(busyOrderId = "", actionMessage = "Pedido atualizado.") }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        busyOrderId = "",
                        errorMessage = error.message.orEmpty().ifBlank { "Erro ao atualizar pedido." },
                    )
                }
            }
        }
    }

    fun consumeActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    private fun failAction(error: Throwable, fallback: String) {
        _uiState.update {
            it.copy(
                isSaving = false,
                errorMessage = error.message.orEmpty().ifBlank { fallback },
            )
        }
    }

    private fun formatAmountInput(value: Double): String =
        if (value <= 0.0) "" else String.format(Locale.ROOT, "%.2f", value)
}

/**
 * Hub da gestão financeira do coletivo.
 *
 * Web: `app/ligas/_components/LeagueFinanceDashboard.tsx` com `view="hub"`.
 *
 * O `view="produtos"` saiu daqui no M8.3: ele é `ProductManagementAnalytics`, hoje portado como
 * motor único em `ui/bi/store/` e servido pelo `ProductBiViewModel` nos cinco players. Esta
 * ViewModel ficou só com os quatro cartões de topo e os atalhos do hub.
 */
class CollectiveFinanceViewModel(
    private val repository: CollectiveManagementRepository = SupabaseCollectiveManagementRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectiveFinanceUiState())
    val uiState: StateFlow<CollectiveFinanceUiState> = _uiState.asStateFlow()

    fun load(session: UserSession, collective: ManagedCollective) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    kind = collective.kind,
                    collective = collective,
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching { repository.getFinance(session.tenant?.id.orEmpty(), collective) }
                .onSuccess { state -> _uiState.value = state }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message.orEmpty().ifBlank {
                                "Não foi possível carregar a gestão financeira agora."
                            },
                        )
                    }
                }
        }
    }
}

/**
 * Frequência do coletivo.
 *
 * Web: `app/ligas/_components/LeagueFrequencyPage.tsx`.
 */
class CollectiveFrequencyViewModel(
    private val repository: CollectiveManagementRepository = SupabaseCollectiveManagementRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectiveFrequencyUiState())
    val uiState: StateFlow<CollectiveFrequencyUiState> = _uiState.asStateFlow()

    fun load(session: UserSession, collective: ManagedCollective) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    kind = collective.kind,
                    collective = collective,
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                repository.getFrequency(
                    tenantId = session.tenant?.id.orEmpty(),
                    collective = collective,
                    // `memberScope: "turma"` só na comissão.
                    memberScopeTurma = collective.kind == CollectiveKind.Commission,
                )
            }.onSuccess { state -> _uiState.value = state }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message.orEmpty().ifBlank {
                                "Não foi possível carregar a frequência agora."
                            },
                        )
                    }
                }
        }
    }

    fun setFilter(filter: CollectiveFrequencyFilter) {
        _uiState.update { it.copy(filter = filter) }
    }
}

/**
 * Extrato financeiro do coletivo.
 *
 * Web: `components/financeiro/FinancialStatementPage.tsx` com `scopeType`
 * `league`/`commission`/`directory`.
 */
class CollectiveStatementViewModel(
    private val repository: CollectiveManagementRepository = SupabaseCollectiveManagementRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectiveStatementUiState())
    val uiState: StateFlow<CollectiveStatementUiState> = _uiState.asStateFlow()

    fun load(session: UserSession, collective: ManagedCollective) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    kind = collective.kind,
                    collective = collective,
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching { repository.getStatement(session.tenant?.id.orEmpty(), collective) }
                .onSuccess { state -> _uiState.value = state }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message.orEmpty().ifBlank {
                                "Não foi possível carregar o extrato agora."
                            },
                        )
                    }
                }
        }
    }

    fun setTypeFilter(type: CollectiveStatementType?) {
        _uiState.update { it.copy(typeFilter = type, page = 1) }
    }

    fun setStatusFilter(status: CollectiveStatementStatus?) {
        _uiState.update { it.copy(statusFilter = status, page = 1) }
    }

    fun updateSearch(term: String) {
        _uiState.update { it.copy(searchTerm = term, page = 1) }
    }

    fun loadMore() {
        _uiState.update { if (it.hasMore) it.copy(page = it.page + 1) else it }
    }
}
