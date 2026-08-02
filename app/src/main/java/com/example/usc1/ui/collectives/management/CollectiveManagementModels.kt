package com.example.usc1.ui.collectives.management

import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.LeagueRoleCatalog

/**
 * Modelos do painel de gestão dos coletivos (M7).
 *
 * Fonte web: `app/ligas/LigasAdminPageContent.tsx` (hub, informações e membros),
 * `app/ligas/LeagueStoreAdminPage.tsx` (loja), `app/ligas/_components/LeagueFinanceDashboard.tsx`
 * (gestão e BI de produtos), `app/ligas/_components/LeagueFrequencyPage.tsx` (frequência),
 * `components/financeiro/FinancialStatementPage.tsx` (extrato) e os wrappers
 * `components/collectives/CommissionManagementPages.tsx` / `DirectoryManagementPages.tsx`.
 */

/** `entityLabel`/`entityArticle` dos wrappers de comissão e diretório. */
val CollectiveKind.entityLabel: String
    get() = when (this) {
        CollectiveKind.League -> "liga"
        CollectiveKind.Commission -> "comissão"
        CollectiveKind.Directory -> "diretório"
    }

/** `entityArticle` do web: comissão/liga usam "da"; diretório usa "do". */
val CollectiveKind.entityArticle: String
    get() = if (this == CollectiveKind.Directory) "do" else "da"

/** `showBoard` do web: só a liga expõe o Board Round. */
val CollectiveKind.showsBoardRound: Boolean
    get() = this == CollectiveKind.League

/** `defaultManagementBackLabel` do web. */
val CollectiveKind.managementBackLabel: String
    get() = when (this) {
        CollectiveKind.League -> "Voltar para ligas"
        CollectiveKind.Commission -> "Voltar para comissões"
        CollectiveKind.Directory -> "Voltar ao diretório"
    }

/** `LeagueAdminQuickNavKey` do web (`_components/LeagueAdminQuickNav.tsx`). */
enum class CollectiveManagementNav(val label: String) {
    Home("Início"),
    Info("Informações"),
    Members("Membros"),
    Agenda("Agenda"),
    Store("Loja"),
    Finance("Gestão"),
    Board("Board Round"),
}

/** `ManagedLeagueRecord` do web: liga/comissão/diretório com o cargo de gestão do usuário. */
data class ManagedCollective(
    val id: String,
    val name: String,
    val acronym: String = "",
    val turmaId: String = "",
    val logoUrl: String? = null,
    /** `managementRole`: cargo em `ligas_membros`, cargo embutido ou "Gestor da página". */
    val managementRole: String = "",
    val kind: CollectiveKind = CollectiveKind.League,
) {
    /** `league.sigla?.trim() || league.nome?.trim()` do cabeçalho do painel. */
    val headerTitle: String get() = acronym.trim().ifBlank { name.trim() }.ifBlank { kind.entityLabel }

    /** Segundo nome do cabeçalho, escondido quando repete o título. */
    val headerSubtitle: String
        get() = name.trim().takeIf { it.isNotBlank() && it != headerTitle }.orEmpty()

    /** `resolveCommissionRouteSegment`: comissão usa turma, diretório usa sigla, liga usa id. */
    val routeSegment: String
        get() = when (kind) {
            CollectiveKind.Commission -> turmaId.trim().ifBlank { acronym.trim() }.ifBlank { id }
            CollectiveKind.Directory -> acronym.trim().ifBlank { id }
            CollectiveKind.League -> id
        }

    /** Rótulo do card da tela de seleção (`commission.turmaId || commission.sigla`). */
    val selectionEyebrow: String
        get() = turmaId.trim().ifBlank { acronym.trim() }.ifBlank { kind.entityLabel.uppercase() }
}

// ------------------------------------------------------------------
// Hub e seleção
// ------------------------------------------------------------------

/** `CommissionManagementGate` / bloco `!isLoggedIn` de `LigasAdminPageContent`. */
data class CollectiveManagementUiState(
    val kind: CollectiveKind = CollectiveKind.League,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val managedCollectives: List<ManagedCollective> = emptyList(),
    val selected: ManagedCollective? = null,
    val activeNav: CollectiveManagementNav = CollectiveManagementNav.Home,
    val info: CollectiveInfoForm = CollectiveInfoForm(),
    val members: CollectiveMembersState = CollectiveMembersState(),
    val isSaving: Boolean = false,
) {
    /** Gate do web: sem coletivo gerenciável a tela vira "Acesso restrito". */
    val hasAccess: Boolean get() = managedCollectives.isNotEmpty()

    val showsBoardRound: Boolean get() = kind.showsBoardRound

    /** `navItems.filter(showBoard || key !== "board")`. */
    val navItems: List<CollectiveManagementNav>
        get() = CollectiveManagementNav.entries.filter {
            it != CollectiveManagementNav.Board || showsBoardRound
        }
}

// ------------------------------------------------------------------
// Informações (aba `visual`)
// ------------------------------------------------------------------

/** `LEAGUE_LINK_OPTIONS` do web. */
enum class CollectiveLinkType(val remoteValue: String, val label: String) {
    Instagram("instagram", "Instagram"),
    Whatsapp("whatsapp", "WhatsApp"),
    Site("site", "Site"),
    Formulario("formulario", "Formulário"),
    Outro("outro", "Outro");

    companion object {
        fun fromRemote(value: String?): CollectiveLinkType {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.remoteValue == normalized } ?: Outro
        }
    }
}

data class CollectiveLinkDraft(
    val id: String,
    val type: CollectiveLinkType = CollectiveLinkType.Outro,
    val label: String = "",
    val url: String = "",
)

/**
 * Aba "Informações" do painel. Limites vindos de `leaguesService.ts`
 * (`LEAGUE_SIGLA_MAX_LENGTH`, `LEAGUE_NAME_MAX_LENGTH`, `LEAGUE_DESCRIPTION_MAX_LENGTH`,
 * `LEAGUE_OVERVIEW_MAX_LENGTH`).
 */
data class CollectiveInfoForm(
    val acronym: String = "",
    val name: String = "",
    val description: String = "",
    val overview: String = "",
    val bizu: String = "",
    val logoUrl: String = "",
    val links: List<CollectiveLinkDraft> = emptyList(),
    val pixKey: String = "",
    val pixBank: String = "",
    val pixHolder: String = "",
    val whatsapp: String = "",
    /** `ativa`: status no tabuleiro do Board Round, só leitura no painel. */
    val activeOnBoard: Boolean = false,
    /** Toggle "Enviar Notificação?" do destaque da semana. */
    val sendNotification: Boolean = false,
) {
    companion object {
        const val AcronymMaxLength = 10
        const val NameMaxLength = 42
        const val DescriptionMaxLength = 180
        const val OverviewMaxLength = 500
        const val LinkLabelMaxLength = 40
        const val LinkUrlMaxLength = 400
        const val LinksMaxCount = 12
        const val PixFieldMaxLength = 140
        const val PhoneMaxLength = 20
    }
}

// ------------------------------------------------------------------
// Membros (aba `members`)
// ------------------------------------------------------------------

data class CollectiveMemberDraft(
    val id: String,
    val name: String,
    val role: String = LeagueRoleCatalog.DefaultRole,
    val photoUrl: String? = null,
    val profileLink: String = "",
    /** `savedMemberIds`: membro já persistido no último carregamento. */
    val persisted: Boolean = true,
)

data class CollectiveMemberRequestDraft(
    val id: String,
    val userId: String,
    val name: String,
    val photoUrl: String? = null,
    val turma: String = "",
    val requestedRole: String = LeagueRoleCatalog.DefaultRole,
    val createdAt: String = "",
)

/** `fetchLeagueUsers` do web, usado pelo modal "Adicionar Aluno". */
data class CollectiveUserOption(
    val id: String,
    val name: String,
    val turma: String = "",
    val photoUrl: String? = null,
)

data class CollectiveMembersState(
    val members: List<CollectiveMemberDraft> = emptyList(),
    val requests: List<CollectiveMemberRequestDraft> = emptyList(),
    val userOptions: List<CollectiveUserOption> = emptyList(),
    val isLoadingUsers: Boolean = false,
    val searchTerm: String = "",
    val isSearchOpen: Boolean = false,
) {
    val roleOptions: List<String> get() = LeagueRoleCatalog.roleOptions

    /** `newlyAddedMembers` do web. */
    val newMembers: List<CollectiveMemberDraft> get() = members.filterNot { it.persisted }

    val persistedMembers: List<CollectiveMemberDraft> get() = members.filter { it.persisted }

    /** Filtro do modal de busca (`allUsers.filter(...)` do web). */
    val filteredUserOptions: List<CollectiveUserOption>
        get() {
            val term = searchTerm.trim().lowercase()
            val alreadyIn = members.map { it.id }.toSet()
            return userOptions
                .filterNot { alreadyIn.contains(it.id) }
                .filter {
                    term.isBlank() ||
                        it.name.lowercase().contains(term) ||
                        it.turma.lowercase().contains(term)
                }
                .take(60)
        }
}

// ------------------------------------------------------------------
// Loja do coletivo (`LeagueStoreAdminPage`)
// ------------------------------------------------------------------

/** `LeagueStoreMode` do web. */
enum class CollectiveStoreMode(val title: String) {
    Overview("Loja"),
    Products("Produtos"),
    PendingOrders("Pedidos pendentes"),
    ApprovedOrders("Pedidos aprovados"),
}

/** `ProductStatus` do web. */
enum class CollectiveProductStatus(val remoteValue: String, val label: String) {
    Ativo("ativo", "Ativo"),
    EmBreve("em_breve", "Em breve"),
    Esgotado("esgotado", "Esgotado");

    companion object {
        fun fromRemote(value: String?): CollectiveProductStatus {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.remoteValue == normalized } ?: Ativo
        }
    }
}

data class CollectiveAdminProduct(
    val id: String,
    val name: String,
    val priceLabel: String,
    val price: Double = 0.0,
    val oldPrice: Double = 0.0,
    val stock: Int = 0,
    val lot: String = "",
    val imageUrl: String? = null,
    val description: String = "",
    val status: CollectiveProductStatus = CollectiveProductStatus.Ativo,
    val active: Boolean = true,
    val tagLabel: String = "",
    val soldCount: Int = 0,
    val category: String = "",
) {
    /** `Estoque {n} - {Oculto|Visível}` da lista do web. */
    val visibilityLabel: String get() = if (active) "Visível" else "Oculto"
}

/** `ProductForm` do web, sem as variações (fora do escopo do M7). */
data class CollectiveProductForm(
    val productId: String = "",
    val name: String = "",
    val price: String = "",
    val oldPrice: String = "",
    val status: CollectiveProductStatus = CollectiveProductStatus.Ativo,
    val stock: String = "",
    val lot: String = "geral",
    val imageUrl: String = "",
    val description: String = "",
    val useOwnPayment: Boolean = false,
    val pixKey: String = "",
    val pixBank: String = "",
    val pixHolder: String = "",
    val tagLabel: String = "",
) {
    val isEditing: Boolean get() = productId.isNotBlank()

    companion object {
        const val NameMaxLength = 120
        const val DescriptionMaxLength = 1200
        const val LotMaxLength = 80
        const val BadgeMaxLength = 30
    }
}

/** `StoreOrder` normalizado (`fetchStoreOrdersPage` do web). */
data class CollectiveStoreOrder(
    val id: String,
    val productId: String = "",
    val productName: String = "Produto",
    val userId: String = "",
    val userName: String = "Usuário",
    val quantity: Int = 1,
    val total: Double = 0.0,
    val totalLabel: String = "R$ 0,00",
    val createdAtLabel: String = "Não informado",
    val status: String = "pendente",
)

data class CollectiveStoreAdminUiState(
    val kind: CollectiveKind = CollectiveKind.League,
    val mode: CollectiveStoreMode = CollectiveStoreMode.Overview,
    val collective: ManagedCollective? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val busyOrderId: String = "",
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val categoryId: String = "",
    val categoryVisible: Boolean = false,
    val storeCoverUrl: String = "",
    val storeColor: String = "#10B981",
    val products: List<CollectiveAdminProduct> = emptyList(),
    val orders: List<CollectiveStoreOrder> = emptyList(),
    val form: CollectiveProductForm? = null,
    /** `resolveLeaguePaymentConfig`: PIX do coletivo usado como padrão do produto. */
    val collectivePixKey: String = "",
    val collectivePixBank: String = "",
    val collectivePixHolder: String = "",
    val collectiveWhatsapp: String = "",
) {
    val visibleProducts: List<CollectiveAdminProduct> get() = products.filter { it.active }

    /** `visibleProducts.length === products.length` do botão de exibir/ocultar. */
    val allProductsVisible: Boolean get() = products.isNotEmpty() && visibleProducts.size == products.size

    /** `hasCompletePaymentConfig` do web. */
    val hasCompletePayment: Boolean
        get() = collectivePixKey.isNotBlank() && collectivePixBank.isNotBlank() &&
            collectivePixHolder.isNotBlank() && collectiveWhatsapp.isNotBlank()
}

// ------------------------------------------------------------------
// Gestão / BI (`LeagueFinanceDashboard`)
// ------------------------------------------------------------------

// `CollectiveMetricRow` (o `MetricRow` do `LeagueFinanceDashboard`) saiu no M8.3 junto com a
// versão reduzida do BI de produtos. O motor único usa `ProductBiMetricRow`, que tem os quatro
// campos do web (`qtd`, `valor`, `medio`, `extra`) em vez de dois.

/**
 * `view="hub"` do `LeagueFinanceDashboard`: os quatro cartões de topo (860-895) e os atalhos.
 *
 * O `view="produtos"` — `ProductManagementAnalytics` — saiu deste estado no M8.3. Ele agora é
 * um motor único (`domain/model/ProductBiEngine.kt`) servido a liga, comissão, diretório,
 * mini-vendor e tenant pela mesma tela. Com ele saíram os campos que só a versão reduzida do
 * M7 usava: `uniqueBuyers`, `averageTicket`, `stockTotal`, `repurchaseBuyers`, `abcCurve`,
 * `productSalesByName`, `productSalesByLot` e `eventSalesByName` — estes três últimos porque no
 * web só aparecem dentro do bloco `{false ? ... : null}` (774-1001), que nunca renderiza.
 */
data class CollectiveFinanceUiState(
    val kind: CollectiveKind = CollectiveKind.League,
    val collective: ManagedCollective? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val productRevenue: Double = 0.0,
    val productQuantity: Int = 0,
    val eventRevenue: Double = 0.0,
    val eventQuantity: Int = 0,
    val catalogCount: Int = 0,
) {
    val totalRevenue: Double get() = productRevenue + eventRevenue
    val totalQuantity: Int get() = productQuantity + eventQuantity
}

// ------------------------------------------------------------------
// Frequência (`LeagueFrequencyPage`)
// ------------------------------------------------------------------

/** `CellSelectStatus` do web. */
enum class CollectiveFrequencyStatus(val label: String) {
    Present("Presente"),
    Approved("Aprovado"),
    Absent("Falta"),
    Justified("Justificado"),
    None("-"),
}

data class CollectiveFrequencyEvent(
    val key: String,
    val title: String,
    val isInternal: Boolean = false,
) {
    val visibilityLabel: String get() = if (isInternal) "Interno" else "Publico"
}

data class CollectiveFrequencyMember(
    val id: String,
    val name: String,
    val role: String = "",
    val turma: String = "Sem turma",
    val photoUrl: String? = null,
)

/** `eventFilter` do web. */
enum class CollectiveFrequencyFilter(val label: String) {
    All("Todos"),
    Public("Público"),
    Internal("Interno"),
}

data class CollectiveFrequencyUiState(
    val kind: CollectiveKind = CollectiveKind.League,
    val collective: ManagedCollective? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val events: List<CollectiveFrequencyEvent> = emptyList(),
    val members: List<CollectiveFrequencyMember> = emptyList(),
    /** Chave `"{eventKey}:{userId}"`, como o `presence` do web. */
    val cells: Map<String, CollectiveFrequencyStatus> = emptyMap(),
    val filter: CollectiveFrequencyFilter = CollectiveFrequencyFilter.All,
    val manualEntryCount: Int = 0,
) {
    val filteredEvents: List<CollectiveFrequencyEvent>
        get() = when (filter) {
            CollectiveFrequencyFilter.All -> events
            CollectiveFrequencyFilter.Public -> events.filterNot { it.isInternal }
            CollectiveFrequencyFilter.Internal -> events.filter { it.isInternal }
        }

    fun statusFor(eventKey: String, memberId: String): CollectiveFrequencyStatus =
        cells["$eventKey:$memberId"] ?: CollectiveFrequencyStatus.None

    val presentCount: Int get() = cells.values.count { it == CollectiveFrequencyStatus.Present }

    val approvedCount: Int
        get() = cells.values.count {
            it == CollectiveFrequencyStatus.Present || it == CollectiveFrequencyStatus.Approved
        }
}

// ------------------------------------------------------------------
// Extrato financeiro (`FinancialStatementPage` no escopo do coletivo)
// ------------------------------------------------------------------

/** `FinancialType` do web, sem `planos` (só existe no escopo tenant). */
enum class CollectiveStatementType(val label: String) {
    Tickets("Ingressos"),
    StoreProducts("Produtos loja"),
}

/** `StatusGroup` do web. */
enum class CollectiveStatementStatus(val label: String) {
    Approved("Aprovado"),
    Pending("Pendente"),
    Rejected("Recusado"),
    Cancelled("Cancelado"),
    Other("Outro"),
}

/** `StatementRow` do web, com as colunas que o app mostra. */
data class CollectiveStatementRow(
    val id: String,
    val type: CollectiveStatementType,
    val item: String,
    val lot: String = "",
    val category: String = "",
    val quantity: Int = 1,
    val client: String = "",
    val clientTurma: String = "",
    val orderedAtLabel: String = "",
    val approvedAtLabel: String = "",
    val approvedBy: String = "",
    val paymentSource: String = "",
    val value: Double = 0.0,
    val discount: Double = 0.0,
    val status: String = "",
    val statusGroup: CollectiveStatementStatus = CollectiveStatementStatus.Other,
    val sortAt: Long = 0L,
)

data class CollectiveStatementUiState(
    val kind: CollectiveKind = CollectiveKind.League,
    val collective: ManagedCollective? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val rows: List<CollectiveStatementRow> = emptyList(),
    val typeFilter: CollectiveStatementType? = null,
    val statusFilter: CollectiveStatementStatus? = null,
    val searchTerm: String = "",
    val page: Int = 1,
) {
    /** `PAGE_SIZE` do web. */
    val pageSize: Int get() = PageSize

    val filteredRows: List<CollectiveStatementRow>
        get() {
            val term = searchTerm.trim().lowercase()
            return rows.filter { row ->
                (typeFilter == null || row.type == typeFilter) &&
                    (statusFilter == null || row.statusGroup == statusFilter) &&
                    (
                        term.isBlank() ||
                            row.client.lowercase().contains(term) ||
                            row.item.lowercase().contains(term) ||
                            row.lot.lowercase().contains(term)
                        )
            }
        }

    val pageRows: List<CollectiveStatementRow>
        get() = filteredRows.take(page * PageSize)

    val hasMore: Boolean get() = filteredRows.size > page * PageSize

    val totalValue: Double get() = filteredRows.sumOf { it.value }

    val totalDiscount: Double get() = filteredRows.sumOf { it.discount }

    val approvedValue: Double
        get() = filteredRows
            .filter { it.statusGroup == CollectiveStatementStatus.Approved }
            .sumOf { it.value }

    private companion object {
        const val PageSize = 20
    }
}
