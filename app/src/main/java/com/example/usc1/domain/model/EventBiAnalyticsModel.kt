package com.example.usc1.domain.model

import kotlinx.serialization.json.JsonObject

/**
 * Saída do motor de métricas do BI de Eventos (M8.1b).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, o `return` do
 * `analytics` useMemo, linhas 6341-6618. Cada campo aqui é uma chave daquele objeto, no mesmo
 * nome e na mesma ordem, para que as cinco visões do M8.2 consumam sem tradução.
 */

/** `OperationalRecord` (124): ingresso e pedido normalizados na mesma forma. */
data class EventBiOperationalRecord(
    val id: String,
    val eventId: String,
    val eventName: String,
    val kind: EventBiRecordKind,
    val status: String,
    val statusFilter: EventBiStatementStatus,
    val typeLabel: String,
    val itemName: String,
    val category: String,
    val lotName: String,
    val quantity: Int,
    val value: Double,
    /** `Number.NaN` quando não há preço de lote/produto para comparar. */
    val expectedValue: Double,
    val discount: Double,
    val discountSource: String,
    val createdAtMillis: Long,
    val approvedAtMillis: Long,
    val completedAtMillis: Long,
    val approver: String,
    val approvalMethod: String,
    val source: String,
    val paymentSource: String,
    val createdBy: String,
    val completedBy: String,
    val completionMethod: String,
    val manual: Boolean,
    val manualAtDoor: Boolean,
    val hasCode: Boolean,
    val usedQuantity: Int,
    val approved: Boolean,
    val pending: Boolean,
    val rejected: Boolean,
    val cancelled: Boolean,
    val transferred: Boolean,
    val courtesy: Boolean,
) {
    /**
     * `approvedNearEvent.includes(record)` (4490) compara por identidade no JS. Aqui a chave
     * tipo+id cumpre o mesmo papel: dois registros distintos nunca a compartilham.
     */
    val identity: String get() = "${kind.remoteValue}:$id"
}

/** `GateScanRow` (4709): uma leitura de QR na portaria. */
data class EventBiGateScan(
    val eventId: String,
    val eventName: String,
    val orderId: String,
    val token: String,
    val holderName: String,
    val turma: String,
    val lotName: String,
    val ticketType: String,
    /** `"Manual"` ou `"QR code"`. */
    val source: String,
    val operator: String,
    val scannedAtMillis: Long,
    val value: Double,
    val href: String,
    val transferLabel: String,
)

/** `OPERATIONAL_ALERT_DESCRIPTIONS` (201-217): os 15 alertas de controle operacional. */
val EventBiOperationalAlertDescriptions: Map<String, String> = mapOf(
    "aprovado-sem-valor" to
        "Existe item aprovado sem valor registrado. Vale conferir se era cortesia real ou falha de cobrança.",
    "valor-zero-sem-cortesia" to
        "O item ficou aprovado por R$ 0,00, mas não aparece como cortesia. Pode indicar desconto indevido ou preço ausente.",
    "cortesia-com-valor" to
        "O item parece cortesia, mas tem valor cobrado. Confirme se a classificação ou o preço estão corretos.",
    "desconto-sem-origem" to
        "Há desconto aplicado sem cupom, plano ou justificativa registrada. Isso dificulta auditoria.",
    "valor-diferente-tabela" to
        "O valor aprovado não bate com o preço esperado do lote ou produto depois dos descontos registrados.",
    "manual-fora-padrao" to
        "Um pedido criado manualmente ficou com valor diferente do padrão do item.",
    "preco-incompativel" to
        "O preço do ingresso ou produto não combina com o lote/produto cadastrado.",
    "pagamento-sem-metodo" to
        "O pedido foi aprovado, mas o método de aprovação/pagamento não ficou identificado.",
    "aprovado-sem-fonte-pagamento" to
        "O pedido aprovado não mostra de onde veio o pagamento ou a confirmação.",
    "transferencia-valor-incompativel" to
        "Uma transferência aparece com valor associado. Transferência normalmente não deveria gerar cobrança nova.",
    "aprovado-sem-codigo" to
        "O item está aprovado, mas não tem QR ou código operacional para entrada/retirada.",
    "codigo-sem-uso" to
        "O item aprovado tem QR ou código gerado, mas ainda não foi usado na entrada ou retirada.",
    "uso-sem-aprovacao" to
        "O item foi usado na entrada ou retirada sem uma aprovação clara no extrato.",
    "status-incoerente" to
        "O status operacional não combina com o histórico do item, como cancelado com uso ou aprovado sem data.",
    "aprovado-perto-evento" to
        "O pedido foi aprovado muito perto do horário do evento, aumentando risco de fila ou erro operacional.",
)

/** Descrição padrão do `addOperationalAlert` (4456) quando a chave não está no mapa. */
const val EventBiDefaultAlertDescription =
    "Confira os itens deste alerta no extrato para entender o impacto operacional."

/**
 * As 211 chaves devolvidas pelo `analytics`, agrupadas por visão.
 *
 * O web devolve um objeto único com as 211 chaves. Na JVM isso não é possível: um construtor
 * tem no máximo 255 slots e cada `Double` ocupa dois, então uma data class única estoura o
 * limite com `ClassFormatError`. O agrupamento por visão é a saída — e é o mesmo recorte que
 * as cinco telas do M8.2 consomem. Cada campo mantém o nome exato do web.
 */
data class EventBiAnalytics(
    val totals: EventBiTotals = EventBiTotals(),
    val commercial: EventBiCommercial = EventBiCommercial(),
    val operational: EventBiOperational = EventBiOperational(),
    val gate: EventBiGate = EventBiGate(),
    val strategic: EventBiStrategic = EventBiStrategic(),
    val sales: EventBiSales = EventBiSales(),
    /** Registros brutos, para as tabelas do M8.2. */
    val operationalRecords: List<EventBiOperationalRecord> = emptyList(),
    val gateScans: List<EventBiGateScan> = emptyList(),
)

/** Bases, receita, contagem e funis (6342-6387). */
data class EventBiTotals(
    // --- Bases (6342-6347) ---
    val approvedTickets: List<EventBiTicket> = emptyList(),
    val approvedOrders: List<EventBiOrder> = emptyList(),
    val rejectedTickets: List<EventBiTicket> = emptyList(),
    val rejectedOrders: List<EventBiOrder> = emptyList(),
    val pendingTickets: List<EventBiTicket> = emptyList(),
    val pendingOrders: List<EventBiOrder> = emptyList(),

    // --- Receita e volume (6348-6367) ---
    val grossRevenue: Double = 0.0,
    val netRevenue: Double = 0.0,
    val ticketRevenue: Double = 0.0,
    val ticketNetRevenue: Double = 0.0,
    val productRevenue: Double = 0.0,
    val approvedTicketQuantity: Int = 0,
    val approvedProductQuantity: Int = 0,
    val ticketCreatedCount: Int = 0,
    val ticketApprovedCount: Int = 0,
    val allApprovedCount: Int = 0,
    val allCreatedCount: Int = 0,
    val approvalRate: Double = 0.0,
    val rejectionRate: Double = 0.0,
    val ticketApprovalRate: Double = 0.0,
    val ticketRejectionRate: Double = 0.0,
    val ticketAverageByOrder: Double = 0.0,
    val ticketAverageByItem: Double = 0.0,
    val ticketAverageByCustomer: Double = 0.0,
    val averageByItem: Double = 0.0,
    val averageByCustomer: Double = 0.0,

    // --- Funis (6368-6387) ---
    val ticketFunnelRows: List<EventBiMetricRow> = emptyList(),
    val funnelRows: List<EventBiMetricRow> = emptyList(),
)

/** Cortes comerciais: lote, turma, audiência, preço e aprovação (6388-6402). */
data class EventBiCommercial(
    val lotRows: List<EventBiMetricRow> = emptyList(),
    val classRows: List<EventBiMetricRow> = emptyList(),
    val audienceRows: List<EventBiMetricRow> = emptyList(),
    val audienceTotal: Double = 0.0,
    val weekdayRows: List<EventBiMetricRow> = emptyList(),
    val periodRows: List<EventBiMetricRow> = emptyList(),
    val priceRows: List<EventBiMetricRow> = emptyList(),
    val approvalRows: List<EventBiMetricRow> = emptyList(),
    val ticketApprovalRows: List<EventBiMetricRow> = emptyList(),
    val approvalMethodRows: List<EventBiMetricRow> = emptyList(),
    val ticketApprovalMethodRows: List<EventBiMetricRow> = emptyList(),
    val approvalAverage: Double = 0.0,
    val ticketApprovalAverage: Double = 0.0,
    val approvalMedian: Double = 0.0,
    val ticketApprovalMedian: Double = 0.0,
)

/** Visão operacional: pendência, SLA, operadores, manualidade e alertas (6403-6458). */
data class EventBiOperational(
    val pendingAgingRows: List<EventBiMetricRow> = emptyList(),
    val ticketPendingAgingRows: List<EventBiMetricRow> = emptyList(),
    val operationalPendingCount: Int = 0,
    val operationalPendingNearEvent: Int = 0,
    val operationalPendingAtDoor: Int = 0,
    val operationalPendingByEventRows: List<EventBiMetricRow> = emptyList(),
    val operationalPendingByTypeRows: List<EventBiMetricRow> = emptyList(),
    val operationalPendingAgeRows: List<EventBiMetricRow> = emptyList(),
    val operationalApprovalAverage: Double = 0.0,
    val operationalApprovalMedian: Double = 0.0,
    val operationalApprovalP90: Double = 0.0,
    val operationalApprovalP95: Double = 0.0,
    val operationalMaxPendingHours: Double = 0.0,
    val operationalApprovedWithin5m: Double = 0.0,
    val operationalApprovedWithin15m: Double = 0.0,
    val operationalApprovedWithin1h: Double = 0.0,
    val operationalApprovedWithin24h: Double = 0.0,
    val slaBySourceRows: List<EventBiMetricRow> = emptyList(),
    val slaByApproverRows: List<EventBiMetricRow> = emptyList(),
    val slaByEventRows: List<EventBiMetricRow> = emptyList(),
    val approvalToEntryMedian: Double = 0.0,
    val approvalToWithdrawalMedian: Double = 0.0,
    val approvedWithoutCodeCount: Int = 0,
    val codeWithoutUseCount: Int = 0,
    val usedWithoutApprovalCount: Int = 0,
    val inconsistentStatusCount: Int = 0,
    val approvedNearEventCount: Int = 0,
    val operatorQualityRows: List<EventBiTableRow> = emptyList(),
    val activeOperatorCount: Int = 0,
    val operatorDistributionRows: List<EventBiMetricRow> = emptyList(),
    val demandWithoutCoverageRows: List<EventBiTableRow> = emptyList(),
    val outsideHoursApprovals: Int = 0,
    val singleOperatorEventRows: List<EventBiTableRow> = emptyList(),
    val manualityStageRows: List<EventBiTableRow> = emptyList(),
    val manualityStageChartRows: List<EventBiMetricRow> = emptyList(),
    val operationalControlAlertRows: List<EventBiTableRow> = emptyList(),
    val topApproverDependency: Double = 0.0,
    val top3ApproverDependency: Double = 0.0,
    val ticketTopApproverDependency: Double = 0.0,
    val ticketTop3ApproverDependency: Double = 0.0,
    val slowApprovals: Int = 0,
    val operationalAlerts: List<EventBiTableRow> = emptyList(),
    val operationalTicketAlerts: List<EventBiTableRow> = emptyList(),
)

/** Visão de portaria: entrada, presença, leitura de QR e fila (6459-6507). */
data class EventBiGate(
    val ticketScanned: Int = 0,
    val noShow: Int = 0,
    val showRate: Double = 0.0,
    val noShowRate: Double = 0.0,
    val revenuePerPresent: Double = 0.0,
    val duplicateScans: Int = 0,
    val invalidScans: Int = 0,
    val appScans: Int = 0,
    val manualScans: Int = 0,
    val manualityRate: Double = 0.0,
    val qrRate: Double = 0.0,
    val totalCapacity: Int = 0,
    val capacityRemaining: Int = 0,
    val occupancyRate: Double = 0.0,
    val queueRisk: String = "Baixo",
    val queuePressure: Double = 0.0,
    val activeGateOperators: Int = 0,
    val peakInterval: EventBiMetricRow = EventBiMetricRow(name = "-"),
    val averageMinutesBetweenScans: Double = 0.0,
    val longestFastSequence: Int = 0,
    val longestIdleMinutes: Double = 0.0,
    val entryCumulativeRows: List<EventBiMetricRow> = emptyList(),
    val entryTimingRows: List<EventBiMetricRow> = emptyList(),
    val presenceByTypeRows: List<EventBiMetricRow> = emptyList(),
    val presenceByLotRows: List<EventBiMetricRow> = emptyList(),
    val noShowRateByLotRows: List<EventBiMetricRow> = emptyList(),
    val scanModeByHourRows: List<EventBiTableRow> = emptyList(),
    val entryModeRows: List<EventBiMetricRow> = emptyList(),
    val portariaOperatorRows: List<EventBiTableRow> = emptyList(),
    val portariaOperatorChartRows: List<EventBiMetricRow> = emptyList(),
    val operatorQualityRadarRows: List<EventBiMetricRow> = emptyList(),
    val invalidReasonRows: List<EventBiMetricRow> = emptyList(),
    val duplicateContextRows: List<EventBiTableRow> = emptyList(),
    val approvedWithoutReadRows: List<EventBiMetricRow> = emptyList(),
    val presentByClassRows: List<EventBiMetricRow> = emptyList(),
    val presenceBySourceRows: List<EventBiMetricRow> = emptyList(),
    val presenceByTransferRows: List<EventBiMetricRow> = emptyList(),
    val operationalCategoryRows: List<EventBiMetricRow> = emptyList(),
    val occupancyRows: List<EventBiMetricRow> = emptyList(),
    val intervalRows: List<EventBiMetricRow> = emptyList(),
    val liveStatusRows: List<EventBiTableRow> = emptyList(),
    val absentRows: List<EventBiTableRow> = emptyList(),
    val unusedActiveRows: List<EventBiTableRow> = emptyList(),
    val portariaAlertRows: List<EventBiTableRow> = emptyList(),
    val portariaEventComparisonRows: List<EventBiTableRow> = emptyList(),
    val portariaEventComparisonChartRows: List<EventBiMetricRow> = emptyList(),
    val scanByHourRows: List<EventBiMetricRow> = emptyList(),
    val noShowByClassRows: List<EventBiMetricRow> = emptyList(),
    val noShowByLotRows: List<EventBiMetricRow> = emptyList(),
)

/** Visão estratégica: recorrência, score, previsão e resultado (6508-6564). */
data class EventBiStrategic(
    val uniqueBuyers: Int = 0,
    val recurringBuyers: Int = 0,
    val recurringRate: Double = 0.0,
    val leadRows: List<EventBiMetricRow> = emptyList(),
    val recurrenceRows: List<EventBiMetricRow> = emptyList(),
    val projectedRevenue: Double = 0.0,
    val resultWithoutCosts: Double = 0.0,
    val eventDecisionRows: List<EventBiTableRow> = emptyList(),
    val revenueOriginRows: List<EventBiMetricRow> = emptyList(),
    val revenueDetailRows: List<EventBiMetricRow> = emptyList(),
    val totalRevenuePerBuyer: Double = 0.0,
    val totalRevenuePerPresent: Double = 0.0,
    val ticketRevenuePerPresent: Double = 0.0,
    val productRevenuePerPresent: Double = 0.0,
    val productPerPresent: Double = 0.0,
    val productRevenueShare: Double = 0.0,
    val ticketRevenueShare: Double = 0.0,
    val ticketBuyerCount: Int = 0,
    val checkedInTicketBuyerCount: Int = 0,
    val productBuyerCount: Int = 0,
    val productRedeemedBuyerCount: Int = 0,
    val buyersWithTicketAndProduct: Int = 0,
    val ticketWithoutProduct: Int = 0,
    val productWithoutTicket: Int = 0,
    val productPresentBuyerIds: Int = 0,
    val strategicFunnelRows: List<EventBiMetricRow> = emptyList(),
    val attachRateRows: List<EventBiMetricRow> = emptyList(),
    val strategicEventRows: List<EventBiTableRow> = emptyList(),
    /** `null` quando não há base para pontuar (`hasStrategicScoreBasis`, 5642). */
    val strategicScore: Int? = null,
    val strategicDecision: String = "Sem dados suficientes",
    val strategicRadarRows: List<EventBiMetricRow> = emptyList(),
    val strategicBubbleRows: List<EventBiBubbleEntry> = emptyList(),
    val revenuePerPresentRows: List<EventBiMetricRow> = emptyList(),
    val eventProductHeatmapRows: List<EventBiHeatmapEntry> = emptyList(),
    val eventCategoryHeatmapRows: List<EventBiHeatmapEntry> = emptyList(),
    val categoryCompositionChartRows: List<EventBiTableRow> = emptyList(),
    val ticketLeadRows: List<EventBiMetricRow> = emptyList(),
    val productLeadRows: List<EventBiMetricRow> = emptyList(),
    val recurrenceDetailRows: List<EventBiMetricRow> = emptyList(),
    val strategicRecurringBuyers: Int = 0,
    val strategicRecurringRate: Double = 0.0,
    val tenantParticipationRows: List<EventBiMetricRow> = emptyList(),
    val customerTicketHistogramRows: List<EventBiMetricRow> = emptyList(),
    val topCustomerRows: List<EventBiTableRow> = emptyList(),
    val topCustomersByEventRows: List<EventBiTableRow> = emptyList(),
    val classConsumptionRows: List<EventBiMetricRow> = emptyList(),
    val lotConsumptionRows: List<EventBiMetricRow> = emptyList(),
    val sourceTreemapRows: List<EventBiMetricRow> = emptyList(),
    val discountImpactRows: List<EventBiMetricRow> = emptyList(),
    val priceStrategyRows: List<EventBiBubbleEntry> = emptyList(),
    val forecastRows: List<EventBiMetricRow> = emptyList(),
    val resultWaterfallRows: List<EventBiMetricRow> = emptyList(),
    val eventCostsTotal: Double = 0.0,
    val hasEventCostsField: Boolean = false,
    val breakEvenTickets: Int = 0,
)

/** Visão modo vendas: produto, retirada, baixa e auditoria (6565-6617). */
data class EventBiSales(
    val redeemedItems: Int = 0,
    val redeemedValue: Double = 0.0,
    val pendingRedeemItems: Int = 0,
    val pendingRedeemValue: Double = 0.0,
    val withdrawalRate: Double = 0.0,
    val pendingRedeemOrders: Int = 0,
    val partialRedeemOrders: Int = 0,
    val oldestPendingOrderName: String = "-",
    val maxPendingRedeemHours: Double = 0.0,
    val averageWithdrawalHours: Double = 0.0,
    val manualWithdrawalRate: Double = 0.0,
    val productRows: List<EventBiTableRow> = emptyList(),
    val productChartRows: List<EventBiProductMetricRow> = emptyList(),
    val categoryRows: List<EventBiMetricRow> = emptyList(),
    val discountRows: List<EventBiMetricRow> = emptyList(),
    val orderSourceRows: List<EventBiMetricRow> = emptyList(),
    val withdrawalMethodRows: List<EventBiMetricRow> = emptyList(),
    val withdrawalOperatorRows: List<EventBiMetricRow> = emptyList(),
    val transferModeRows: List<EventBiMetricRow> = emptyList(),
    val transferTargetRows: List<EventBiMetricRow> = emptyList(),
    val transferActorRows: List<EventBiMetricRow> = emptyList(),
    val auditRows: List<EventBiTableRow> = emptyList(),
    val pendingRedeemAgingRows: List<EventBiMetricRow> = emptyList(),
    val withdrawalStatusRows: List<EventBiMetricRow> = emptyList(),
    val salesWithdrawalTimelineRows: List<EventBiMetricRow> = emptyList(),
    val productWithdrawalRows: List<EventBiTableRow> = emptyList(),
    val categoryWithdrawalRows: List<EventBiTableRow> = emptyList(),
    /** `null` quando não há pedido aprovado (5090). */
    val salesHealthScore: Int? = null,
    val productRiskRadarRows: List<EventBiMetricRow> = emptyList(),
    val operatorMethodHeatmapRows: List<EventBiHeatmapEntry> = emptyList(),
    val withdrawalErrorRows: List<EventBiMetricRow> = emptyList(),
    val operatorSalesRows: List<EventBiTableRow> = emptyList(),
    val conflictAuditRows: List<EventBiTableRow> = emptyList(),
    val partialWithdrawalRows: List<EventBiTableRow> = emptyList(),
    val pendingProductDetailRows: List<EventBiTableRow> = emptyList(),
    val salesWaterfallRows: List<EventBiMetricRow> = emptyList(),
    val orderSourceQualityRows: List<EventBiTableRow> = emptyList(),
    val paymentSourceRows: List<EventBiMetricRow> = emptyList(),
    val paymentIssueRows: List<EventBiTableRow> = emptyList(),
    val discountDetailedRows: List<EventBiMetricRow> = emptyList(),
    val productHourHeatmapRows: List<EventBiHeatmapEntry> = emptyList(),
    val stockRows: List<EventBiTableRow> = emptyList(),
    val turnoverRows: List<EventBiMetricRow> = emptyList(),
    val crossSellRows: List<EventBiNetworkEdge> = emptyList(),
    val productTicketHistogramRows: List<EventBiMetricRow> = emptyList(),
    val productTransferRows: List<EventBiHeatmapEntry> = emptyList(),
    val qrStatusRows: List<EventBiMetricRow> = emptyList(),
    val improvedAuditRows: List<EventBiTableRow> = emptyList(),
)

/** Usuário do índice `userById`; vazio quando `users` não foi lida. */
internal fun Map<String, JsonObject>.userOf(id: String): JsonObject? = this[id.trim()]
