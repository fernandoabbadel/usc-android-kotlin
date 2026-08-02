package com.example.usc1.domain.model

import com.example.usc1.ui.collectives.CollectiveKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras do motor do BI de Eventos (M8.1), portadas de
 * `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx` (linhas 1-6770) e dos
 * quatro wrappers de escopo (`app/admin/bi/page.tsx`, `app/admin/gestao/eventos/page.tsx`,
 * `app/ligas/_components/LeagueEventBiDashboard.tsx`, `CommissionManagementEventBiPage` e
 * `DirectoryManagementEventBiPage`).
 */
class EventBiM8RulesTest {

    @Test
    fun `hub lista os cinco modulos com titulo e subtitulo do array MODULES`() {
        // `MODULES` (~linha 219): ordem, títulos e subtítulos exatos.
        val modules = EventBiView.modules

        assertEquals(5, modules.size)
        assertEquals(
            listOf("comercial", "operacional", "portaria", "estrategico", "vendas"),
            modules.map { it.routeValue },
        )
        assertEquals(
            listOf(
                "BI Comercial",
                "BI Operacional",
                "BI Portaria",
                "BI Estratégico",
                "BI Modo Vendas",
            ),
            modules.map { it.title },
        )
        assertEquals(
            listOf(
                "Venda, receita, lote, preço, turma e funil.",
                "Aprovação, comprovante, fila, gargalo e aprovadores.",
                "Entrada, presença, ausência e leitura de QR code.",
                "Recorrência, previsão, comportamento e repetição.",
                "Produto, ficha, bar, retirada, baixa e auditoria.",
            ),
            modules.map { it.subtitle },
        )
    }

    @Test
    fun `filtro de produto so aparece na visao de vendas`() {
        // `showProduct={view === "vendas"}` do `Filters`.
        assertTrue(EventBiView.Sales.showsProductFilter)
        EventBiView.entries.filter { it != EventBiView.Sales }.forEach {
            assertFalse(it.showsProductFilter)
        }
    }

    @Test
    fun `cabecalho do tenant usa o default do DashboardShell`() {
        // `app/admin/bi/page.tsx` não passa contexto: eyebrow "BI Administrativo",
        // título "BI de Eventos" e subtítulo sem sufixo de escopo.
        val header = EventBiContext(
            scope = EventBiScopeRef(EventBiScope.Tenant, EventBiScopeRef.All),
            contextEyebrow = EventBiScope.Tenant.defaultEyebrow,
        ).headerFor(EventBiView.Home)

        assertEquals("BI Administrativo", header.eyebrow)
        assertEquals("BI de Eventos", header.title)
        assertEquals("Escolha a visão analítica.", header.subtitle)
    }

    @Test
    fun `cabecalho do coletivo troca titulo e concatena o subtitulo`() {
        // `titleLabel = contextTitle || title` e
        // `subtitleLabel = contextTitle ? "{title}. {subtitle}" : subtitle`.
        val league = EventBiContext(
            scope = EventBiScopeRef(EventBiScope.League, "a153ef50"),
            scopeLabel = EventBiScope.League.defaultScopeLabel,
            contextTitle = "LAMEI",
            contextEyebrow = EventBiScope.League.defaultEyebrow,
        )
        val header = league.headerFor(EventBiView.Home)

        assertEquals("BI da liga", header.eyebrow)
        assertEquals("LAMEI", header.title)
        assertEquals("BI de Eventos. Escolha a visão analítica da liga.", header.subtitle)

        // Nas visões o título vem do `MODULES` e entra no subtítulo.
        val gate = league.headerFor(EventBiView.Gate)
        assertEquals("LAMEI", gate.title)
        assertEquals(
            "BI Portaria. Entrada, presença, ausência e leitura de QR code.",
            gate.subtitle,
        )
    }

    @Test
    fun `rotulo de escopo de cada player segue os wrappers do web`() {
        assertEquals("", EventBiScope.Tenant.defaultScopeLabel)
        assertEquals("da liga", EventBiScope.League.defaultScopeLabel)
        assertEquals("da comissão", EventBiScope.Commission.defaultScopeLabel)
        assertEquals("do diretório", EventBiScope.Directory.defaultScopeLabel)

        // Bloco travado do `Filters`: sem `scopeLabel`, tenant vira "Atlética".
        assertEquals("Atlética", EventBiContext().lockedScopeLabel)
        assertEquals(
            "da comissão",
            EventBiContext(
                scope = EventBiScopeRef(EventBiScope.Commission, "T2"),
                scopeLabel = EventBiScope.Commission.defaultScopeLabel,
            ).lockedScopeLabel,
        )
        assertEquals(
            "Entidade",
            EventBiContext(scope = EventBiScopeRef(EventBiScope.Directory, "DASZ")).lockedScopeLabel,
        )
    }

    @Test
    fun `seletor de escopo fica travado em todos os players`() {
        // `scopeLocked={Boolean(lockedScopeType)}` com default `"tenant"` é sempre verdadeiro:
        // nem o player tenant mostra o `<select>` de escopo no web.
        assertTrue(EventBiScopeRef.SelectorLocked)
    }

    @Test
    fun `escopo de coletivo exige id resolvido`() {
        // `matchesActiveScope`: com `scopeId === "todos"` e escopo de entidade nada casa.
        val resolved = EventBiScopeRef(EventBiScope.League, "a153ef50")
        assertTrue(resolved.isCollective)
        assertFalse(resolved.isAllEntities)

        val unresolved = EventBiScopeRef(EventBiScope.League, EventBiScopeRef.All)
        assertFalse(unresolved.isCollective)
        assertTrue(unresolved.isAllEntities)

        val tenant = EventBiScopeRef()
        assertFalse(tenant.isCollective)
        assertTrue(tenant.isAllEntities)
    }

    @Test
    fun `escopo do player vem do tipo de coletivo do M7`() {
        assertEquals(EventBiScope.League, EventBiScope.fromCollectiveKind(CollectiveKind.League))
        assertEquals(EventBiScope.Commission, EventBiScope.fromCollectiveKind(CollectiveKind.Commission))
        assertEquals(EventBiScope.Directory, EventBiScope.fromCollectiveKind(CollectiveKind.Directory))
    }

    @Test
    fun `categoria do ligas_config classifica a entidade como no web`() {
        // `entityScopeType`: diretório, comissão e o default liga.
        assertEquals(EventBiScope.Directory, EventBiScope.fromRemote("diretorio"))
        assertEquals(EventBiScope.Directory, EventBiScope.fromRemote("Directory"))
        assertEquals(EventBiScope.Commission, EventBiScope.fromRemote("comissao"))
        assertEquals(EventBiScope.Commission, EventBiScope.fromRemote("comissões"))
        assertEquals(EventBiScope.League, EventBiScope.fromRemote("liga"))
        assertEquals(EventBiScope.League, EventBiScope.fromRemote("league"))
        // Sem categoria reconhecida não há escopo externo declarado.
        assertEquals(EventBiScope.Tenant, EventBiScope.fromRemote(null))
        assertEquals(EventBiScope.Tenant, EventBiScope.fromRemote("festa"))
    }

    @Test
    fun `status aprovado do BI e mais amplo que o do M7`() {
        // `isApprovedStatus` do BI inclui paid/pago/confirmado/redeemed, que `statusIsApproved`
        // do M7 não trata. Por isso o BI tem a própria classificação.
        listOf("approved", "aprovado", "paid", "pago", "confirmado", "entregue", "redeemed")
            .forEach { assertEquals(EventBiStatus.Approved, EventBiStatus.classify(it)) }

        assertEquals(EventBiStatus.Rejected, EventBiStatus.classify("recusado"))
        assertEquals(EventBiStatus.Cancelled, EventBiStatus.classify("expirado"))
        assertEquals(EventBiStatus.Refunded, EventBiStatus.classify("estornado"))
        // `pendingTickets`: o que não é aprovado, recusado nem cancelado.
        assertEquals(EventBiStatus.Pending, EventBiStatus.classify("pendente"))
        assertEquals(EventBiStatus.Pending, EventBiStatus.classify(null))
    }

    @Test
    fun `periodo segue o dateInPeriod do web`() {
        val zone = java.time.ZoneId.systemDefault()
        val day = java.time.LocalDate.of(2026, 3, 10)
        val noon = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val lastSecond = day.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        val dayBefore = day.minusDays(1).atTime(23, 0).atZone(zone).toInstant().toEpochMilli()

        // Sem filtro tudo passa.
        assertTrue(eventBiDateInPeriod(noon, "", ""))
        // `startDate` corta em T00:00:00 e `endDate` em T23:59:59 — a borda entra.
        assertTrue(eventBiDateInPeriod(noon, "2026-03-10", "2026-03-10"))
        assertTrue(eventBiDateInPeriod(lastSecond, "2026-03-10", "2026-03-10"))
        assertFalse(eventBiDateInPeriod(dayBefore, "2026-03-10", ""))
        assertFalse(eventBiDateInPeriod(noon, "", "2026-03-09"))
        // `if (!date) return true`: registro sem data nunca é descartado pelo período.
        assertTrue(eventBiDateInPeriod(0L, "2026-03-10", "2026-03-10"))
    }

    @Test
    fun `filtros do dashboard comecam em todos e aprovados`() {
        // `useState("todos")` para evento e produto; `useState<AudienceBasis>("aprovados")`.
        val filter = EventBiFilter()

        assertEquals(EventBiScopeRef.All, filter.eventId)
        assertEquals(EventBiScopeRef.All, filter.productId)
        assertEquals(EventBiAudienceBasis.Approved, filter.audienceBasis)
        assertFalse(filter.hasEventFilter)
        assertFalse(filter.hasProductFilter)
        assertFalse(filter.hasPeriodFilter)

        val narrowed = filter.copy(eventId = "evt-1", startDate = "2026-03-01")
        assertTrue(narrowed.hasEventFilter)
        assertTrue(narrowed.hasPeriodFilter)
    }

    @Test
    fun `visao vem da rota e cai no hub quando desconhecida`() {
        assertEquals(EventBiView.Commercial, EventBiView.fromRoute("comercial"))
        assertEquals(EventBiView.Strategic, EventBiView.fromRoute("ESTRATEGICO"))
        assertEquals(EventBiView.Home, EventBiView.fromRoute("inicio"))
        assertEquals(EventBiView.Home, EventBiView.fromRoute("qualquer-coisa"))
        assertEquals(EventBiView.Home, EventBiView.fromRoute(null))
    }

    @Test
    fun `dataset do hub nao traz transacoes`() {
        // `includeTransactions = false`: o hub só precisa das opções de filtro.
        val hub = EventBiDataset(
            eventOptions = listOf(EventBiOption("evt-1", "Calourada")),
            events = listOf(EventBiEvent(id = "evt-1", name = "Calourada")),
        )

        assertFalse(hub.hasTransactions)
        assertFalse(hub.isEmpty)
        assertTrue(hub.tickets.isEmpty())
        assertTrue(hub.orders.isEmpty())
        assertTrue(EventBiDataset().isEmpty)
        assertNull(hub.eventOptions.firstOrNull { it.id == "evt-2" })
    }
}
