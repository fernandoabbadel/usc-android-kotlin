package com.example.usc1.domain.model

/**
 * Rótulos derivados que o web calcula no corpo do componente, entre os filtros e o `return`
 * das visões (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 6691-6746.
 * São regras de negócio, não formatação de tela: por isso ficam no domínio, junto do motor, e
 * não dentro do Composable.
 */

/** `strategicScoreLabel` (6727): sem base para pontuar, o card mostra texto em vez de número. */
fun eventBiStrategicScoreLabel(score: Int?): String =
    score?.let { formatEventBiNumber(it.toDouble()) } ?: "Sem dados"

/**
 * `strategicCostHint` (6729): distingue custo cadastrado com valor, custo cadastrado zerado e
 * campo de custo ausente — três estados diferentes, não dois.
 */
fun eventBiStrategicCostHint(eventCostsTotal: Double, hasEventCostsField: Boolean): String = when {
    eventCostsTotal > 0 -> "Custos: ${formatEventBiCurrency(eventCostsTotal)}"
    hasEventCostsField -> "Custo cadastrado como R$ 0,00"
    else -> "Campo de custo opcional vazio"
}

/** `salesHealthLabel` (6735): as quatro faixas do score do modo vendas. */
fun eventBiSalesHealthLabel(score: Int?): String = when {
    score == null -> "Sem dados suficientes"
    score >= 85 -> "Excelente"
    score >= 70 -> "Boa"
    score >= 40 -> "Atenção"
    else -> "Crítica"
}

/** `salesHealthValue` (6745). */
fun eventBiSalesHealthValue(score: Int?): String =
    score?.let { formatEventBiNumber(it.toDouble()) } ?: "Sem dados"

/**
 * `selectedStatementEventId` (6691): o evento do filtro; em "todos", só resolve quando o recorte
 * tem exatamente um evento — com dois ou mais o web devolve string vazia e os atalhos somem.
 */
fun eventBiSelectedStatementEventId(eventFilterId: String, scopeEventIds: List<String>): String =
    when {
        eventFilterId != EventBiScopeRef.All -> eventFilterId
        scopeEventIds.size == 1 -> scopeEventIds.first()
        else -> ""
    }
