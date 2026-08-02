package com.example.usc1.domain.model

import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.management.CollectiveFrequencyEvent
import com.example.usc1.ui.collectives.management.CollectiveFrequencyFilter
import com.example.usc1.ui.collectives.management.CollectiveFrequencyStatus
import com.example.usc1.ui.collectives.management.CollectiveFrequencyUiState
import com.example.usc1.ui.collectives.management.CollectiveManagementNav
import com.example.usc1.ui.collectives.management.CollectiveManagementUiState
import com.example.usc1.ui.collectives.management.CollectiveMemberDraft
import com.example.usc1.ui.collectives.management.CollectiveMembersState
import com.example.usc1.ui.collectives.management.CollectiveStatementRow
import com.example.usc1.ui.collectives.management.CollectiveStatementStatus
import com.example.usc1.ui.collectives.management.CollectiveStatementType
import com.example.usc1.ui.collectives.management.CollectiveStatementUiState
import com.example.usc1.ui.collectives.management.CollectiveStoreAdminUiState
import com.example.usc1.ui.collectives.management.CollectiveAdminProduct
import com.example.usc1.ui.collectives.management.CollectiveUserOption
import com.example.usc1.ui.collectives.management.ManagedCollective
import com.example.usc1.ui.collectives.management.entityArticle
import com.example.usc1.ui.collectives.management.entityLabel
import com.example.usc1.ui.collectives.management.showsBoardRound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras do painel de gestão dos coletivos (M7), copiadas de
 * `app/ligas/LigasAdminPageContent.tsx`, `LeagueStoreAdminPage.tsx`,
 * `_components/LeagueFrequencyPage.tsx` e dos wrappers
 * `CommissionManagementPages.tsx` / `DirectoryManagementPages.tsx`.
 */
class CollectiveManagementM7RulesTest {

    @Test
    fun `rotulo e artigo de cada area seguem os wrappers do web`() {
        // `entityLabel`/`entityArticle` de CommissionManagementPages e DirectoryManagementPages.
        assertEquals("liga", CollectiveKind.League.entityLabel)
        assertEquals("da", CollectiveKind.League.entityArticle)
        assertEquals("comissão", CollectiveKind.Commission.entityLabel)
        assertEquals("da", CollectiveKind.Commission.entityArticle)
        assertEquals("diretório", CollectiveKind.Directory.entityLabel)
        assertEquals("do", CollectiveKind.Directory.entityArticle)
    }

    @Test
    fun `board round so aparece na liga`() {
        // `showBoard: false` nos wrappers de comissão e diretório.
        assertTrue(CollectiveKind.League.showsBoardRound)
        assertFalse(CollectiveKind.Commission.showsBoardRound)
        assertFalse(CollectiveKind.Directory.showsBoardRound)

        val leagueNav = CollectiveManagementUiState(kind = CollectiveKind.League).navItems
        val commissionNav = CollectiveManagementUiState(kind = CollectiveKind.Commission).navItems

        assertEquals(7, leagueNav.size)
        assertEquals(6, commissionNav.size)
        assertTrue(leagueNav.contains(CollectiveManagementNav.Board))
        assertFalse(commissionNav.contains(CollectiveManagementNav.Board))
    }

    @Test
    fun `segmento de rota usa turma na comissao e sigla no diretorio`() {
        // `resolveCommissionRouteSegment` do gate: turmaId, senão sigla, senão id.
        val commission = ManagedCollective(
            id = "abc-123",
            name = "Comissão Turma II",
            acronym = "T2",
            turmaId = "T2",
            kind = CollectiveKind.Commission,
        )
        assertEquals("T2", commission.routeSegment)

        val directory = ManagedCollective(
            id = "def-456",
            name = "Diretório Acadêmico Stella Zöllner",
            acronym = "DASZ",
            kind = CollectiveKind.Directory,
        )
        assertEquals("DASZ", directory.routeSegment)

        val league = ManagedCollective(
            id = "a153ef50",
            name = "Liga Acad Med Emergência",
            acronym = "LAMEI",
            kind = CollectiveKind.League,
        )
        assertEquals("a153ef50", league.routeSegment)
    }

    @Test
    fun `cabecalho esconde o nome quando ele repete a sigla`() {
        val withBoth = ManagedCollective(id = "1", name = "Liga Acad Med", acronym = "LAMEI")
        assertEquals("LAMEI", withBoth.headerTitle)
        assertEquals("Liga Acad Med", withBoth.headerSubtitle)

        val onlyName = ManagedCollective(id = "2", name = "Liga Acad Med")
        assertEquals("Liga Acad Med", onlyName.headerTitle)
        assertEquals("", onlyName.headerSubtitle)
    }

    @Test
    fun `busca de membros ignora quem ja esta na diretoria`() {
        // Filtro do modal "Adicionar Aluno" do web.
        val state = CollectiveMembersState(
            members = listOf(draft("u1", "Ana")),
            userOptions = listOf(
                CollectiveUserOption(id = "u1", name = "Ana", turma = "T2"),
                CollectiveUserOption(id = "u2", name = "Bruno", turma = "T3"),
                CollectiveUserOption(id = "u3", name = "Carla", turma = "T2"),
            ),
            searchTerm = "t2",
        )

        assertEquals(listOf("Carla"), state.filteredUserOptions.map { it.name })
    }

    @Test
    fun `membro novo do rascunho fica separado dos persistidos`() {
        // `newlyAddedMembers`/`persistedMembers` do web, via `savedMemberIds`.
        val state = CollectiveMembersState(
            members = listOf(
                draft("u1", "Ana", persisted = true),
                draft("u2", "Bruno", persisted = false),
            ),
        )

        assertEquals(listOf("Bruno"), state.newMembers.map { it.name })
        assertEquals(listOf("Ana"), state.persistedMembers.map { it.name })
    }

    @Test
    fun `toggle da loja segue a contagem de produtos visiveis do web`() {
        // `visibleProducts.length === products.length` decide "Ocultar"/"Exibir produtos".
        val allVisible = CollectiveStoreAdminUiState(
            products = listOf(product("p1", active = true), product("p2", active = true)),
        )
        assertTrue(allVisible.allProductsVisible)
        assertEquals(2, allVisible.visibleProducts.size)

        val mixed = CollectiveStoreAdminUiState(
            products = listOf(product("p1", active = true), product("p2", active = false)),
        )
        assertFalse(mixed.allProductsVisible)

        // Catálogo vazio não pode contar como "todos visíveis": o botão fica desabilitado.
        assertFalse(CollectiveStoreAdminUiState().allProductsVisible)
    }

    @Test
    fun `venda so libera com pagamento completo do coletivo`() {
        // `hasCompletePaymentConfig` do LeagueStoreAdminPage.
        val complete = CollectiveStoreAdminUiState(
            collectivePixKey = "chave",
            collectivePixBank = "banco",
            collectivePixHolder = "titular",
            collectiveWhatsapp = "+5512999999999",
        )
        assertTrue(complete.hasCompletePayment)
        assertFalse(complete.copy(collectiveWhatsapp = "").hasCompletePayment)
    }

    @Test
    fun `filtro de frequencia separa evento interno e publico`() {
        // `eventFilter` do LeagueFrequencyPage.
        val state = CollectiveFrequencyUiState(
            events = listOf(
                CollectiveFrequencyEvent(key = "e1", title = "Aberto", isInternal = false),
                CollectiveFrequencyEvent(key = "e2", title = "Interno", isInternal = true),
            ),
        )

        assertEquals(2, state.filteredEvents.size)
        assertEquals(
            listOf("Aberto"),
            state.copy(filter = CollectiveFrequencyFilter.Public).filteredEvents.map { it.title },
        )
        assertEquals(
            listOf("Interno"),
            state.copy(filter = CollectiveFrequencyFilter.Internal).filteredEvents.map { it.title },
        )
        assertEquals("Publico", state.events.first().visibilityLabel)
        assertEquals("Interno", state.events.last().visibilityLabel)
    }

    @Test
    fun `presenca conta QR lido e ingresso aprovado como o web`() {
        // `presence` do LeagueFrequencyPage: presente exige QR lido; aprovado é ingresso liberado.
        val state = CollectiveFrequencyUiState(
            cells = mapOf(
                "e1:u1" to CollectiveFrequencyStatus.Present,
                "e1:u2" to CollectiveFrequencyStatus.Approved,
                "e2:u1" to CollectiveFrequencyStatus.Absent,
            ),
        )

        assertEquals(CollectiveFrequencyStatus.Present, state.statusFor("e1", "u1"))
        assertEquals(CollectiveFrequencyStatus.None, state.statusFor("e9", "u9"))
        assertEquals(1, state.presentCount)
        assertEquals(2, state.approvedCount)
    }

    @Test
    fun `extrato pagina de 20 em 20 e soma so o filtrado`() {
        // `PAGE_SIZE = 20` e os totais do FinancialStatementPage.
        val rows = (1..45).map { index ->
            row(
                id = "r$index",
                type = if (index % 2 == 0) CollectiveStatementType.Tickets else CollectiveStatementType.StoreProducts,
                value = 10.0,
                status = if (index <= 30) CollectiveStatementStatus.Approved else CollectiveStatementStatus.Pending,
            )
        }
        val state = CollectiveStatementUiState(rows = rows)

        assertEquals(20, state.pageRows.size)
        assertTrue(state.hasMore)
        assertEquals(40, state.copy(page = 2).pageRows.size)
        assertFalse(state.copy(page = 3).hasMore)

        assertEquals(450.0, state.totalValue, 0.001)
        assertEquals(300.0, state.approvedValue, 0.001)

        val onlyTickets = state.copy(typeFilter = CollectiveStatementType.Tickets)
        assertEquals(22, onlyTickets.filteredRows.size)
        assertEquals(220.0, onlyTickets.totalValue, 0.001)
    }

    @Test
    fun `busca do extrato casa cliente item e lote`() {
        val state = CollectiveStatementUiState(
            rows = listOf(
                row("r1", CollectiveStatementType.Tickets, 10.0, client = "Fernando", item = "Calourada"),
                row("r2", CollectiveStatementType.StoreProducts, 20.0, client = "Ana", item = "Camiseta"),
            ),
        )

        assertEquals(listOf("r1"), state.copy(searchTerm = "fernando").filteredRows.map { it.id })
        assertEquals(listOf("r2"), state.copy(searchTerm = "camis").filteredRows.map { it.id })
        assertEquals(2, state.copy(searchTerm = "").filteredRows.size)
    }

    private fun draft(id: String, name: String, persisted: Boolean = true) = CollectiveMemberDraft(
        id = id,
        name = name,
        persisted = persisted,
    )

    private fun product(id: String, active: Boolean) = CollectiveAdminProduct(
        id = id,
        name = "Produto $id",
        priceLabel = "R$ 10,00",
        active = active,
    )

    private fun row(
        id: String,
        type: CollectiveStatementType,
        value: Double,
        status: CollectiveStatementStatus = CollectiveStatementStatus.Approved,
        client: String = "Cliente",
        item: String = "Item",
    ) = CollectiveStatementRow(
        id = id,
        type = type,
        item = item,
        client = client,
        value = value,
        statusGroup = status,
    )
}
