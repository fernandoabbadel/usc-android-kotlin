package com.example.usc1.domain.repository

import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.management.CollectiveFinanceUiState
import com.example.usc1.ui.collectives.management.CollectiveFrequencyUiState
import com.example.usc1.ui.collectives.management.CollectiveInfoForm
import com.example.usc1.ui.collectives.management.CollectiveMemberDraft
import com.example.usc1.ui.collectives.management.CollectiveMemberRequestDraft
import com.example.usc1.ui.collectives.management.CollectiveProductForm
import com.example.usc1.ui.collectives.management.CollectiveStatementUiState
import com.example.usc1.ui.collectives.management.CollectiveStoreAdminUiState
import com.example.usc1.ui.collectives.management.CollectiveStoreMode
import com.example.usc1.ui.collectives.management.CollectiveUserOption
import com.example.usc1.ui.collectives.management.ManagedCollective

/**
 * Painel de gestão dos coletivos (M7).
 *
 * Espelha `fetchManagedLeagueSummaries`, `fetchLeagueById`, `fetchLeagueUsers`,
 * `updateLeagueConfigPatch` e `syncLeagueMembers` de `leaguesService.ts`, mais
 * `storeService.ts` (`fetchStoreCategories`, `fetchStoreProducts`, `fetchStoreOrdersPage`,
 * `upsertStoreCategory`, `upsertStoreProduct`, `setStoreOrderStatus`, `approveStoreOrder`).
 */
interface CollectiveManagementRepository {
    /** `fetchManagedLeagueSummaries({ category })`: coletivos que o usuário pode gerir. */
    suspend fun getManagedCollectives(
        tenantId: String,
        userId: String,
        kind: CollectiveKind,
        isPlatformMaster: Boolean,
    ): List<ManagedCollective>

    /** `fetchLeagueById` mapeado para o formulário da aba "Informações". */
    suspend fun getInfoForm(tenantId: String, collectiveId: String): CollectiveInfoForm

    /**
     * `handleSaveVisualSection`: `updateLeagueConfigPatch` com nome, sigla, descrição,
     * visão geral, links, `payment_config`, bizu e logo, mais a notificação global opcional.
     */
    suspend fun saveInfo(
        tenantId: String,
        collectiveId: String,
        form: CollectiveInfoForm,
    )

    /** `fetchLeagueById` mapeado para membros + solicitações pendentes. */
    suspend fun getMembers(
        tenantId: String,
        collectiveId: String,
    ): Pair<List<CollectiveMemberDraft>, List<CollectiveMemberRequestDraft>>

    /** `fetchLeagueUsers({ maxResults: 120 })` do modal "Adicionar Aluno". */
    suspend fun getUserOptions(tenantId: String): List<CollectiveUserOption>

    /**
     * `handleSaveMembersSection`: grava `membros`/`memberRequests`/`membersCount` em
     * `ligas_config` e sincroniza `ligas_membros` como o fallback direto de `syncLeagueMembers`.
     */
    suspend fun saveMembers(
        tenantId: String,
        collectiveId: String,
        members: List<CollectiveMemberDraft>,
        requests: List<CollectiveMemberRequestDraft>,
    )

    /** `LeagueStoreAdminPage.load`: categoria, produtos e (nos modos de pedido) os pedidos. */
    suspend fun getStore(
        tenantId: String,
        collective: ManagedCollective,
        mode: CollectiveStoreMode,
    ): CollectiveStoreAdminUiState

    /** `handleSaveStore`: `upsertStoreCategory` com capa, cor e visibilidade. */
    suspend fun saveStoreCategory(
        tenantId: String,
        collective: ManagedCollective,
        coverUrl: String,
        color: String,
        visible: Boolean,
    )

    /** `handleToggleProducts`: `upsertStoreProduct({ active })` para todos os produtos. */
    suspend fun setAllProductsActive(
        tenantId: String,
        collectiveId: String,
        active: Boolean,
    )

    /** `handleSaveProduct`: `upsertStoreProduct` com `seller_type`/`seller_id` do coletivo. */
    suspend fun saveProduct(
        tenantId: String,
        collective: ManagedCollective,
        form: CollectiveProductForm,
    )

    /** Botão "Exibir"/"Ocultar" da lista de produtos. */
    suspend fun setProductActive(
        tenantId: String,
        collectiveId: String,
        productId: String,
        active: Boolean,
    )

    /** `handleApprove`: `approveStoreOrder` sem a parte de vouchers de evento. */
    suspend fun approveOrder(
        tenantId: String,
        collectiveId: String,
        orderId: String,
        approvedBy: String,
    )

    /** `handleOrderStatus`: `setStoreOrderStatus` (`pendente`, `rejected`, `delivered`). */
    suspend fun setOrderStatus(
        tenantId: String,
        collectiveId: String,
        orderId: String,
        status: String,
    )

    /**
     * `loadLeagueFinanceData` + `analytics` do `LeagueFinanceDashboard`, no recorte do
     * `view="hub"`. O `view="produtos"` é o `ProductBiRepository` desde o M8.3.
     */
    suspend fun getFinance(
        tenantId: String,
        collective: ManagedCollective,
    ): CollectiveFinanceUiState

    /** `loadLeagueFrequencyData` + `presenceData` do `LeagueFrequencyPage`. */
    suspend fun getFrequency(
        tenantId: String,
        collective: ManagedCollective,
        memberScopeTurma: Boolean,
    ): CollectiveFrequencyUiState

    /** `FinancialStatementPage` no escopo do coletivo. */
    suspend fun getStatement(
        tenantId: String,
        collective: ManagedCollective,
    ): CollectiveStatementUiState
}
