package com.example.usc1.ui.bi.charts

import com.example.usc1.domain.model.EventBiBubbleEntry
import com.example.usc1.domain.model.EventBiHeatmapEntry
import com.example.usc1.domain.model.EventBiMetricRow
import com.example.usc1.domain.model.EventBiNetworkEdge
import com.example.usc1.domain.model.EventBiTableRow
import com.example.usc1.domain.model.comparePtBr
import com.example.usc1.domain.model.eventBiHourSortValue
import com.example.usc1.domain.model.safeDivide
import kotlin.math.ceil

/**
 * Preparo de dados dos 26 componentes de gráfico do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 2139-3378.
 * Cada componente do web faz o filtro, a ordenação e o corte **dentro** do próprio corpo, antes
 * de entregar ao Recharts. Aqui essas regras ficam em funções puras: é o que os testes do módulo
 * verificam, e o `Canvas` só desenha o que sai delas.
 */

/**
 * `domain={[0, dataMax => Math.max(1, Math.ceil(dataMax * 1.18))]}` (2384): folga de 18% no topo
 * do eixo, com piso 1 para o eixo não colapsar quando tudo é zero.
 */
fun eventBiAxisTop(dataMax: Double): Double = maxOf(1.0, ceil(dataMax * 1.18))

/**
 * `ColumnBars` (2449): descarta o que é zero ou negativo e ordena decrescente pela própria chave.
 */
fun eventBiColumnRows(
    data: List<EventBiMetricRow>,
    dataKey: EventBiMetricKey,
): List<EventBiMetricRow> = data
    .filter { it.valueOf(dataKey) > 0 }
    .sortedByDescending { it.valueOf(dataKey) }

/**
 * `PieMetric` (2511): a rosca só considera a chave escolhida (`quantity` ou `value`).
 */
fun eventBiPieSlices(
    data: List<EventBiMetricRow>,
    dataKey: EventBiMetricKey,
): List<EventBiMetricRow> = data.filter { it.valueOf(dataKey) > 0 }

/**
 * `SimplePieMetric` (2563) e `SemiDonutMetric` (2602): o filtro é `quantity > 0 || value > 0`,
 * mais largo que o do `PieMetric` — uma fatia com quantidade zero e valor positivo aparece aqui
 * e some lá.
 */
fun eventBiWideSlices(data: List<EventBiMetricRow>): List<EventBiMetricRow> =
    data.filter { it.quantity > 0 || it.value > 0 }

/**
 * `ParetoMetric` (2638): top 10 por quantidade com acumulado em percentual.
 *
 * O total do acumulado vem de **todas** as linhas recebidas (2639), não das 10 exibidas; por isso
 * a última barra pode fechar abaixo de 100%.
 */
fun eventBiParetoRows(data: List<EventBiMetricRow>): List<Pair<EventBiMetricRow, Double>> {
    val total = data.sumOf { it.quantity }
    var running = 0.0
    return data
        .filter { it.quantity > 0 }
        .sortedByDescending { it.quantity }
        .take(10)
        .map { row ->
            running += safeDivide(row.quantity, total) * 100
            row to running
        }
}

/**
 * `ComboBarsLines` (2860): `none` ordena por `sortValue` crescente, `value` e `quantity` ordenam
 * decrescente com o outro campo como desempate.
 */
fun eventBiComboRows(
    data: List<EventBiMetricRow>,
    sortBy: EventBiComboSort,
): List<EventBiMetricRow> = when (sortBy) {
    EventBiComboSort.None -> data.sortedBy { it.sortValue }
    EventBiComboSort.Value -> data.sortedWith(
        compareByDescending<EventBiMetricRow> { it.value }.thenByDescending { it.quantity },
    )
    EventBiComboSort.Quantity -> data.sortedWith(
        compareByDescending<EventBiMetricRow> { it.quantity }.thenByDescending { it.value },
    )
}

/** `hasSecondary` (2865): a segunda linha só existe se alguma linha tiver `secondary > 0`. */
fun eventBiHasSecondary(rows: List<EventBiMetricRow>): Boolean = rows.any { it.secondary > 0 }

/** As quatro chaves fixas do `StackedPercentChart` (2917). */
val EventBiWithdrawalKeys: List<String> = listOf("retirado", "pendente", "parcial", "cancelado")

/**
 * `StackedPercentChart` (2923): cada chave vira percentual do total da própria linha. Linha com
 * total zero devolve zero em todas as chaves — `safeDivide` nunca produz `NaN`.
 */
fun eventBiStackedShares(row: EventBiTableRow): List<Double> {
    val total = EventBiWithdrawalKeys.sumOf { row.number(it) }
    return EventBiWithdrawalKeys.map { safeDivide(row.number(it), total) * 100 }
}

/** `RadarMetric` (2708): o filtro vem **antes** do corte em 6 eixos. */
fun eventBiRadarRows(data: List<EventBiMetricRow>): List<EventBiMetricRow> =
    data.filter { it.value > 0 || it.quantity > 0 }.take(6)

/** `HeatmapMetric` (2960): no máximo 10 linhas, na ordem de chegada. */
fun eventBiHeatmapRowNames(data: List<EventBiHeatmapEntry>): List<String> =
    data.map { it.row }.distinct().take(10)

/**
 * `HeatmapMetric` (2961): `hour` ordena por `hourSortValue` com desempate em pt-BR, `alpha`
 * ordena só em pt-BR e `none` mantém a ordem de chegada.
 */
fun eventBiHeatmapColumnNames(
    data: List<EventBiHeatmapEntry>,
    columnOrder: EventBiHeatmapOrder,
    maxColumns: Int = 8,
): List<String> = data.map { it.column }.distinct()
    .let { list ->
        when (columnOrder) {
            EventBiHeatmapOrder.Hour -> list.sortedWith(
                compareBy<String> { eventBiHourSortValue(it) }
                    .thenComparator { left, right -> comparePtBr(left, right) },
            )
            EventBiHeatmapOrder.Alpha -> list.sortedWith { left, right -> comparePtBr(left, right) }
            EventBiHeatmapOrder.None -> list
        }
    }
    .take(maxColumns)

/** `TreemapMetric` (3018): até 12 blocos, dimensionados por `Math.max(value, quantity)` (3024). */
fun eventBiTreemapRows(data: List<EventBiMetricRow>): List<EventBiMetricRow> =
    data.filter { it.value > 0 || it.quantity > 0 }.take(12)

fun eventBiTreemapWeight(row: EventBiMetricRow): Double = maxOf(row.value, row.quantity)

/** `BubbleMetric` (3094): basta um dos três eixos ser positivo para a bolha aparecer. */
fun eventBiBubbleRows(data: List<EventBiBubbleEntry>): List<EventBiBubbleEntry> =
    data.filter { it.x > 0 || it.y > 0 || it.value > 0 }

/**
 * `WaterfallMetric` (3193): etapa com quantidade e valor zerados sai da cascata; o resto fica,
 * inclusive valor negativo.
 */
fun eventBiWaterfallRows(data: List<EventBiMetricRow>): List<EventBiMetricRow> =
    data.filter { it.quantity != 0.0 || it.value != 0.0 }

/** `<Cell fill={entry.value >= 0 ? "#22c55e" : "#fb7185"} />` (3209). */
fun eventBiWaterfallColor(value: Double): String = if (value >= 0) "#22c55e" else "#fb7185"

/** `NetworkMetric` (3219): até 12 arestas com valor positivo. */
fun eventBiNetworkEdges(data: List<EventBiNetworkEdge>): List<EventBiNetworkEdge> =
    data.filter { it.value > 0 }.take(12)

/** `NetworkMetric` (3221): os nós saem das arestas já cortadas, no máximo 10. */
fun eventBiNetworkNodes(edges: List<EventBiNetworkEdge>): List<String> =
    edges.flatMap { listOf(it.from, it.to) }.distinct().take(10)

/** `FunnelMetric` (2815): largura proporcional, com piso de 8% para a etapa menor ficar visível. */
fun eventBiFunnelFraction(quantity: Double, max: Double): Double =
    maxOf(0.08, safeDivide(quantity, max))

/** `DataTable` (3284): número de páginas, nunca menor que 1. */
fun eventBiTotalPages(rowCount: Int, pageSize: Int): Int =
    if (pageSize > 0) maxOf(1, (rowCount + pageSize - 1) / pageSize) else 1

/** `DataTable` (3286): a fatia visível da página, já com a página presa ao intervalo válido. */
fun eventBiTablePage(
    rows: List<EventBiTableRow>,
    pageSize: Int,
    page: Int,
): List<EventBiTableRow> {
    if (pageSize <= 0) return rows
    val safePage = page.coerceIn(1, eventBiTotalPages(rows.size, pageSize))
    return rows.drop((safePage - 1) * pageSize).take(pageSize)
}
