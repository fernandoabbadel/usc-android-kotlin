package com.example.usc1.domain.model

import com.example.usc1.ui.bi.charts.EventBiComboSort
import com.example.usc1.ui.bi.charts.EventBiHeatmapOrder
import com.example.usc1.ui.bi.charts.EventBiMetricKey
import com.example.usc1.ui.bi.charts.EventBiWithdrawalKeys
import com.example.usc1.ui.bi.charts.eventBiAxisTop
import com.example.usc1.ui.bi.charts.eventBiBubbleRows
import com.example.usc1.ui.bi.charts.eventBiColumnRows
import com.example.usc1.ui.bi.charts.eventBiComboRows
import com.example.usc1.ui.bi.charts.eventBiFunnelFraction
import com.example.usc1.ui.bi.charts.eventBiHasSecondary
import com.example.usc1.ui.bi.charts.eventBiHeatmapColumnNames
import com.example.usc1.ui.bi.charts.eventBiHeatmapRowNames
import com.example.usc1.ui.bi.charts.eventBiNetworkEdges
import com.example.usc1.ui.bi.charts.eventBiNetworkNodes
import com.example.usc1.ui.bi.charts.eventBiParetoRows
import com.example.usc1.ui.bi.charts.eventBiPieSlices
import com.example.usc1.ui.bi.charts.eventBiRadarRows
import com.example.usc1.ui.bi.charts.eventBiStackedShares
import com.example.usc1.ui.bi.charts.eventBiTablePage
import com.example.usc1.ui.bi.charts.eventBiTotalPages
import com.example.usc1.ui.bi.charts.eventBiTreemapRows
import com.example.usc1.ui.bi.charts.eventBiTreemapWeight
import com.example.usc1.ui.bi.charts.eventBiWaterfallColor
import com.example.usc1.ui.bi.charts.eventBiWaterfallRows
import com.example.usc1.ui.bi.charts.eventBiWideSlices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras das cinco visões analíticas e dos 26 componentes de gráfico do BI de Eventos (M8.2),
 * portadas de `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`:
 * os componentes (2139-3378) e os rótulos derivados do corpo do dashboard (6691-6746).
 */
class EventBiM82RulesTest {

    private fun row(
        name: String,
        quantity: Double = 0.0,
        value: Double = 0.0,
        secondary: Double = 0.0,
        sortValue: Double = 0.0,
    ) = EventBiMetricRow(
        name = name,
        quantity = quantity,
        value = value,
        secondary = secondary,
        sortValue = sortValue,
    )

    // ------------------------------------------------------------------
    // Eixos e barras (2359-2478)
    // ------------------------------------------------------------------

    @Test
    fun `axisTop aplica a folga de 18 por cento e nunca cai abaixo de um`() {
        // `domain={[0, dataMax => Math.max(1, Math.ceil(dataMax * 1.18))]}` (2384).
        assertEquals(118.0, eventBiAxisTop(100.0), 0.001)
        // `Math.ceil` arredonda para cima: 10 * 1.18 = 11.8 -> 12.
        assertEquals(12.0, eventBiAxisTop(10.0), 0.001)
        // Piso 1: com tudo zerado o eixo não colapsa.
        assertEquals(1.0, eventBiAxisTop(0.0), 0.001)
    }

    @Test
    fun `ColumnBars descarta linha zerada e ordena decrescente pela propria chave`() {
        // `ColumnBars` (2449): `filter(row => Number(row[dataKey] ?? 0) > 0).sort(...)`.
        val data = listOf(
            row("A", value = 10.0),
            row("B", value = 0.0),
            row("C", value = 30.0),
            row("D", value = 20.0),
        )
        assertEquals(listOf("C", "D", "A"), eventBiColumnRows(data, EventBiMetricKey.Value).map { it.name })
        // Trocar a chave troca o recorte: por `quantity` todas as linhas acima são zero.
        assertTrue(eventBiColumnRows(data, EventBiMetricKey.Quantity).isEmpty())
    }

    // ------------------------------------------------------------------
    // Pizza, rosca e meia-rosca (2500-2636)
    // ------------------------------------------------------------------

    @Test
    fun `PieMetric filtra so pela chave escolhida e SimplePie aceita quantidade ou valor`() {
        // `PieMetric` (2511) olha só `row[dataKey]`; `SimplePieMetric` (2563) e
        // `SemiDonutMetric` (2602) usam `quantity > 0 || value > 0`.
        val onlyValue = listOf(row("Só valor", quantity = 0.0, value = 90.0))
        assertTrue(eventBiPieSlices(onlyValue, EventBiMetricKey.Quantity).isEmpty())
        assertEquals(1, eventBiPieSlices(onlyValue, EventBiMetricKey.Value).size)
        // A mesma linha aparece na pizza simples, que é o filtro mais largo.
        assertEquals(1, eventBiWideSlices(onlyValue).size)
    }

    // ------------------------------------------------------------------
    // Pareto (2638-2705)
    // ------------------------------------------------------------------

    @Test
    fun `Pareto acumula sobre o total de todas as linhas e nao apenas das dez exibidas`() {
        // `const total = data.reduce(...)` (2639) roda ANTES do `.slice(0, 10)` (2643).
        val data = (1..12).map { row("Item $it", quantity = 10.0) }
        val rows = eventBiParetoRows(data)
        assertEquals(10, rows.size)
        // 12 linhas de 10 somam 120; as 10 exibidas somam 100 -> 83,3%, não 100%.
        assertEquals(83.333, rows.last().second, 0.01)
    }

    @Test
    fun `Pareto descarta quantidade zero e ordena do maior para o menor`() {
        val data = listOf(
            row("Baixo", quantity = 1.0),
            row("Zero", quantity = 0.0),
            row("Alto", quantity = 9.0),
        )
        val rows = eventBiParetoRows(data)
        assertEquals(listOf("Alto", "Baixo"), rows.map { it.first.name })
        assertEquals(100.0, rows.last().second, 0.01)
    }

    // ------------------------------------------------------------------
    // ComboBarsLines (2842-2913)
    // ------------------------------------------------------------------

    @Test
    fun `ComboBarsLines com sortBy none ordena por sortValue crescente`() {
        // `if (sortBy === "none") return Number(left.sortValue ?? 0) - Number(right.sortValue ?? 0)`
        // (2861): é o que mantém a ordem cronológica do forecast e da antecedência.
        val data = listOf(
            row("Terceiro", quantity = 99.0, sortValue = 3.0),
            row("Primeiro", quantity = 1.0, sortValue = 1.0),
            row("Segundo", quantity = 50.0, sortValue = 2.0),
        )
        assertEquals(
            listOf("Primeiro", "Segundo", "Terceiro"),
            eventBiComboRows(data, EventBiComboSort.None).map { it.name },
        )
    }

    @Test
    fun `ComboBarsLines desempata quantidade por valor e valor por quantidade`() {
        // `right.quantity - left.quantity || right.value - left.value` (2863).
        val data = listOf(
            row("Empate menor", quantity = 5.0, value = 10.0),
            row("Empate maior", quantity = 5.0, value = 80.0),
        )
        assertEquals(
            listOf("Empate maior", "Empate menor"),
            eventBiComboRows(data, EventBiComboSort.Quantity).map { it.name },
        )
        // `right.value - left.value || right.quantity - left.quantity` (2862).
        val byValue = listOf(
            row("Valor alto", quantity = 1.0, value = 80.0),
            row("Valor baixo", quantity = 99.0, value = 10.0),
        )
        assertEquals(
            listOf("Valor alto", "Valor baixo"),
            eventBiComboRows(byValue, EventBiComboSort.Value).map { it.name },
        )
    }

    @Test
    fun `A segunda linha do combo so aparece quando algum secondary e positivo`() {
        // `hasSecondary` (2865): sem isso o gráfico desenharia uma linha constante em zero.
        assertFalse(eventBiHasSecondary(listOf(row("A", quantity = 5.0))))
        assertTrue(eventBiHasSecondary(listOf(row("A", quantity = 5.0), row("B", secondary = 0.5))))
    }

    // ------------------------------------------------------------------
    // StackedPercentChart (2915-2949)
    // ------------------------------------------------------------------

    @Test
    fun `StackedPercent normaliza cada linha para cem por cento`() {
        // `safeDivide(parseNumber(row[entry.key], 0), total) * 100` (2927).
        val stacked = EventBiTableRow(
            linkedMapOf(
                "name" to "Camiseta",
                "retirado" to 30.0,
                "pendente" to 10.0,
                "parcial" to 10.0,
                "cancelado" to 0.0,
            ),
        )
        val shares = eventBiStackedShares(stacked)
        assertEquals(listOf(60.0, 20.0, 20.0, 0.0), shares.map { kotlin.math.round(it) })
        assertEquals(100.0, shares.sum(), 0.001)
    }

    @Test
    fun `StackedPercent com linha zerada devolve zero em vez de NaN`() {
        // `safeDivide` (376) protege a divisão por zero que o `total` produziria.
        val empty = EventBiTableRow(linkedMapOf("name" to "Sem venda"))
        assertEquals(EventBiWithdrawalKeys.size, eventBiStackedShares(empty).size)
        assertTrue(eventBiStackedShares(empty).all { it == 0.0 })
    }

    // ------------------------------------------------------------------
    // Radar, heatmap, treemap, bolha, rede e funil (2707-3265)
    // ------------------------------------------------------------------

    @Test
    fun `Radar filtra antes de cortar em seis eixos`() {
        // `data.filter(...).slice(0, 6)` (2708): o filtro vem primeiro, então uma linha zerada
        // no meio da lista não "gasta" uma das seis vagas.
        val data = listOf(
            row("Zero 1"),
            row("Eixo 1", value = 10.0),
            row("Zero 2"),
            row("Eixo 2", value = 20.0),
            row("Eixo 3", quantity = 1.0),
        )
        assertEquals(listOf("Eixo 1", "Eixo 2", "Eixo 3"), eventBiRadarRows(data).map { it.name })
    }

    @Test
    fun `Heatmap por hora ordena pelo numero da hora e nao pelo texto`() {
        // `hourSortValue(left) - hourSortValue(right)` (2963): em ordem alfabética "10h" viria
        // antes de "09h"; por hora não.
        val data = listOf(
            EventBiHeatmapEntry("Cerveja", "10h", 5.0),
            EventBiHeatmapEntry("Cerveja", "09h", 3.0),
            EventBiHeatmapEntry("Cerveja", "22h", 9.0),
        )
        assertEquals(
            listOf("09h", "10h", "22h"),
            eventBiHeatmapColumnNames(data, EventBiHeatmapOrder.Hour),
        )
        // `none` mantém a ordem de chegada, que é o default do web (2954).
        assertEquals(
            listOf("10h", "09h", "22h"),
            eventBiHeatmapColumnNames(data, EventBiHeatmapOrder.None),
        )
    }

    @Test
    fun `Heatmap corta em dez linhas e oito colunas`() {
        // `.slice(0, 10)` nas linhas (2960) e `.slice(0, maxColumns)` nas colunas (2967).
        val data = (1..14).flatMap { rowIndex ->
            (1..12).map { columnIndex ->
                EventBiHeatmapEntry("Linha $rowIndex", "Col $columnIndex", 1.0)
            }
        }
        assertEquals(10, eventBiHeatmapRowNames(data).size)
        assertEquals(8, eventBiHeatmapColumnNames(data, EventBiHeatmapOrder.None).size)
    }

    @Test
    fun `Treemap dimensiona pelo maior entre valor e quantidade`() {
        // `Math.max(row.value, row.quantity)` (3024): o bloco cresce pelo que for maior.
        assertEquals(80.0, eventBiTreemapWeight(row("A", quantity = 80.0, value = 12.0)), 0.001)
        assertEquals(90.0, eventBiTreemapWeight(row("B", quantity = 5.0, value = 90.0)), 0.001)
        // `.slice(0, 12)` (3018).
        assertEquals(12, eventBiTreemapRows((1..20).map { row("Item $it", value = 1.0) }).size)
    }

    @Test
    fun `Bolha aparece quando qualquer um dos tres eixos e positivo`() {
        // `row.x > 0 || row.y > 0 || row.value > 0` (3094): `z` sozinho não basta.
        val onlyZ = EventBiBubbleEntry(name = "Só bolha", x = 0.0, y = 0.0, z = 500.0, value = 0.0)
        val onlyScore = EventBiBubbleEntry(name = "Só score", x = 0.0, y = 0.0, z = 0.0, value = 70.0)
        assertTrue(eventBiBubbleRows(listOf(onlyZ)).isEmpty())
        assertEquals(1, eventBiBubbleRows(listOf(onlyScore)).size)
    }

    @Test
    fun `Waterfall mantem valor negativo e pinta de rosa`() {
        // `filter(row => row.quantity !== 0 || row.value !== 0)` (3193) e a cor de 3209.
        val data = listOf(
            row("Receita", value = 100.0),
            row("Descontos", value = -20.0),
            row("Zerado", value = 0.0, quantity = 0.0),
        )
        assertEquals(listOf("Receita", "Descontos"), eventBiWaterfallRows(data).map { it.name })
        assertEquals("#22c55e", eventBiWaterfallColor(100.0))
        assertEquals("#fb7185", eventBiWaterfallColor(-20.0))
        // Zero é positivo na regra do web (`value >= 0`).
        assertEquals("#22c55e", eventBiWaterfallColor(0.0))
    }

    @Test
    fun `Rede corta em doze arestas e dez nos derivados dessas arestas`() {
        // `.slice(0, 12)` nas arestas (3219) e `.slice(0, 10)` nos nós (3221) — os nós saem das
        // arestas JÁ cortadas, então uma aresta descartada não traz nó nenhum.
        val edges = (1..20).map { EventBiNetworkEdge(from = "A$it", to = "B$it", value = 1.0) }
        val visible = eventBiNetworkEdges(edges)
        assertEquals(12, visible.size)
        assertEquals(10, eventBiNetworkNodes(visible).size)
        // Aresta com valor zero sai antes da contagem.
        assertTrue(eventBiNetworkEdges(listOf(EventBiNetworkEdge("A", "B", 0.0))).isEmpty())
    }

    @Test
    fun `Funil tem piso de oito por cento para a etapa menor continuar visivel`() {
        // `Math.max(8, safeDivide(row.quantity, max) * 100)` (2815).
        assertEquals(0.08, eventBiFunnelFraction(1.0, 1000.0), 0.0001)
        assertEquals(0.5, eventBiFunnelFraction(50.0, 100.0), 0.0001)
        assertEquals(1.0, eventBiFunnelFraction(100.0, 100.0), 0.0001)
    }

    // ------------------------------------------------------------------
    // DataTable (3267-3377)
    // ------------------------------------------------------------------

    @Test
    fun `DataTable pagina em blocos e nunca reporta menos de uma pagina`() {
        // `Math.max(1, Math.ceil(rows.length / pageSize))` (3284).
        assertEquals(1, eventBiTotalPages(0, 20))
        assertEquals(1, eventBiTotalPages(20, 20))
        assertEquals(2, eventBiTotalPages(21, 20))
        // `pageSize` ausente é uma página só (3284).
        assertEquals(1, eventBiTotalPages(500, 0))
    }

    @Test
    fun `DataTable prende a pagina no intervalo valido antes de fatiar`() {
        // `Math.min(totalPages, Math.max(1, page))` (3285).
        val rows = (1..25).map { EventBiTableRow(linkedMapOf("evento" to "Evento $it")) }
        assertEquals(5, eventBiTablePage(rows, 20, 2).size)
        // Página acima do total volta para a última.
        assertEquals(5, eventBiTablePage(rows, 20, 99).size)
        // Página zero ou negativa volta para a primeira.
        assertEquals(20, eventBiTablePage(rows, 20, 0).size)
        assertEquals("Evento 1", eventBiTablePage(rows, 20, 0).first().text("evento"))
    }

    // ------------------------------------------------------------------
    // Rótulos derivados do corpo do dashboard (6691-6746)
    // ------------------------------------------------------------------

    @Test
    fun `strategicScoreLabel mostra texto quando nao ha base para pontuar`() {
        // `analytics.strategicScore === null ? "Sem dados" : formatNumber(...)` (6727).
        assertEquals("Sem dados", eventBiStrategicScoreLabel(null))
        assertEquals("72", eventBiStrategicScoreLabel(72))
        // Zero é um score válido, não ausência de dado.
        assertEquals("0", eventBiStrategicScoreLabel(0))
    }

    @Test
    fun `strategicCostHint separa custo zerado de campo de custo ausente`() {
        // `analytics.hasEventCostsField` (6732) distingue os dois estados que a soma zero mistura.
        assertEquals("Custos: R$ 1.500,00", eventBiStrategicCostHint(1500.0, true))
        assertEquals("Custo cadastrado como R$ 0,00", eventBiStrategicCostHint(0.0, true))
        assertEquals("Campo de custo opcional vazio", eventBiStrategicCostHint(0.0, false))
    }

    @Test
    fun `salesHealthLabel usa as quatro faixas do modo vendas`() {
        // `>= 85 / >= 70 / >= 40` (6735-6744).
        assertEquals("Sem dados suficientes", eventBiSalesHealthLabel(null))
        assertEquals("Excelente", eventBiSalesHealthLabel(85))
        assertEquals("Boa", eventBiSalesHealthLabel(70))
        assertEquals("Atenção", eventBiSalesHealthLabel(40))
        assertEquals("Crítica", eventBiSalesHealthLabel(39))
        assertEquals("Sem dados", eventBiSalesHealthValue(null))
        assertEquals("70", eventBiSalesHealthValue(70))
    }

    @Test
    fun `selectedStatementEventId so resolve o filtro todos quando ha um unico evento`() {
        // `selectedData.events.length === 1 ? eventId(...) : ""` (6694): com dois eventos os
        // atalhos de extrato somem, porque não daria para saber a qual evento apontar.
        assertEquals("evt-9", eventBiSelectedStatementEventId("evt-9", listOf("evt-1", "evt-2")))
        assertEquals(
            "evt-1",
            eventBiSelectedStatementEventId(EventBiScopeRef.All, listOf("evt-1")),
        )
        assertEquals(
            "",
            eventBiSelectedStatementEventId(EventBiScopeRef.All, listOf("evt-1", "evt-2")),
        )
        assertEquals("", eventBiSelectedStatementEventId(EventBiScopeRef.All, emptyList()))
    }
}
