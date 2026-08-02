package com.example.usc1.domain.model

import kotlinx.serialization.json.JsonObject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Motor de métricas do BI de Eventos (M8.1b).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, o `analytics`
 * useMemo das linhas 3843-6619 — 2.777 linhas e 211 chaves. É o mesmo motor para os quatro
 * players e para as cinco visões: quem muda é o `EventBiScope` que montou o `EventBiDataset`.
 *
 * Diferenças deliberadas, todas documentadas no `docs/ANDROID_PROGRESS.md`:
 * - `nowMillis` entra por parâmetro no lugar de `Date.now()`, para o teste ser determinístico;
 * - `links` é `EventBiLinkBuilder.Inert` enquanto o workspace de evento (M10) não existe: o
 *   indicador continua sendo calculado, só o `href` fica vazio;
 * - a recorrência histórica lê `dataset.scopeTickets`/`scopeOrders` (escopo inteiro, sem filtro
 *   de evento nem de período), que é o papel de `data.tickets`/`data.orders` no web.
 */
fun computeEventBiAnalytics(
    dataset: EventBiDataset,
    filter: EventBiFilter,
    links: EventBiLinkBuilder = EventBiLinkBuilder.Inert,
    nowMillis: Long = System.currentTimeMillis(),
): EventBiAnalytics = EventBiEngine(dataset, filter, links, nowMillis).run()

private const val MillisPerHour = 36e5
private const val MillisPerDay = 864e5
private const val MillisPerMinute = 60_000.0

/** `(right - left) / 36e5`, com `Number.NaN` quando falta uma das pontas. */
private fun hoursBetween(fromMillis: Long, toMillis: Long): Double =
    if (fromMillis > 0L && toMillis > 0L) (toMillis - fromMillis) / MillisPerHour else Double.NaN

private fun List<Double>.finiteNonNegative(): List<Double> = filter { it.isFinite() && it >= 0 }

private fun List<Double>.mean(): Double = safeDivide(sum(), size.toDouble())

@Suppress("LargeClass")
internal class EventBiEngine(
    private val dataset: EventBiDataset,
    private val filter: EventBiFilter,
    private val links: EventBiLinkBuilder,
    private val now: Long,
) {
    private val eventById = dataset.eventsById
    private val productsById = dataset.productsById
    private val userById = dataset.usersById

    // ------------------------------------------------------------------
    // Bases (3844-3897)
    // ------------------------------------------------------------------

    private fun EventBiTicket.classified() = EventBiStatus.classify(raw.statusValue())
    private fun EventBiOrder.classified() = EventBiStatus.classify(raw.statusValue())

    private val approvedTickets = dataset.tickets.filter { it.classified() == EventBiStatus.Approved }
    private val rejectedTickets = dataset.tickets.filter { it.classified() == EventBiStatus.Rejected }
    private val pendingTickets = dataset.tickets.filter {
        it.classified() !in ApprovedRejectedCancelled
    }
    private val approvedOrders = dataset.orders.filter { it.classified() == EventBiStatus.Approved }
    private val rejectedOrders = dataset.orders.filter { it.classified() == EventBiStatus.Rejected }
    private val pendingOrders = dataset.orders.filter { it.classified() !in ApprovedRejectedCancelled }
    private val cancelledOrders = dataset.orders.filter { it.classified() == EventBiStatus.Cancelled }
    private val refundedOrders = dataset.orders.filter { it.classified() == EventBiStatus.Refunded }

    private val approvedTicketQuantity = approvedTickets.sumOf { it.raw.ticketQuantity() }
    private val approvedProductQuantity = approvedOrders.sumOf { it.raw.orderQuantity() }
    private val ticketRevenue = approvedTickets.sumOf { it.raw.ticketValue() }
    private val productRevenue = approvedOrders.sumOf { it.raw.orderTotal() }
    private val ticketDiscounts = approvedTickets.sumOf { it.raw.ticketDiscount() }
    private val productDiscounts = approvedOrders.sumOf { it.raw.orderDiscount() }
    private val grossRevenue = ticketRevenue + productRevenue
    private val netRevenue = max(0.0, grossRevenue - ticketDiscounts - productDiscounts)
    private val allApprovedCount = approvedTickets.size + approvedOrders.size
    private val allCreatedCount = dataset.tickets.size + dataset.orders.size

    /** `paymentSent = allCreatedCount` (3867). */
    private val paymentSent = allCreatedCount

    private val eventCardClicks = dataset.events.sumOf { it.cardClicks }
    private val eventPurchaseClicks = dataset.events.sumOf { it.buyClicks }
    private val productPurchaseClicks = dataset.products.sumOf { it.clicks }
    private val purchaseClicks = max(eventPurchaseClicks, productPurchaseClicks)

    private val hasRsvpDateFilter = filter.hasPeriodFilter
    private val rsvpGoing = if (dataset.rsvps.isNotEmpty() || hasRsvpDateFilter) {
        dataset.rsvps.count { it.status == "going" }
    } else {
        dataset.events.sumOf { it.confirmedCount }
    }
    private val rsvpMaybe = if (dataset.rsvps.isNotEmpty() || hasRsvpDateFilter) {
        dataset.rsvps.count { it.status == "maybe" }
    } else {
        dataset.events.sumOf { it.maybeCount }
    }

    private val redeemedItems = approvedOrders.sumOf { it.raw.orderRedeemedQuantity() }
    private val redeemedValue = approvedOrders.sumOf { order ->
        order.raw.orderTotal() *
            safeDivide(order.raw.orderRedeemedQuantity().toDouble(), order.raw.orderQuantity().toDouble())
    }
    private val pendingRedeemItems = max(0, approvedProductQuantity - redeemedItems)
    private val pendingRedeemValue = max(
        0.0,
        productRevenue - redeemedValue -
            cancelledOrders.sumOf { it.raw.orderTotal() } -
            refundedOrders.sumOf { it.raw.orderTotal() },
    )

    /** `orderEventId(order) || productEventId(productsById.get(orderProductId(order)))` (4027). */
    private fun EventBiOrder.resolvedEventId(): String = raw.orderEventId()
        .ifBlank { productsById[raw.orderProductId()]?.raw.productEventId() }

    private fun eventStartOf(eventId: String): Long = eventById[eventId]?.startsAtMillis ?: 0L

    private fun eventLabelOf(eventId: String, fallback: String): String =
        eventById[eventId]?.name ?: fallback.ifBlank { "Evento" }

    // ------------------------------------------------------------------
    // Registros operacionais (3957-4079)
    // ------------------------------------------------------------------

    private val ticketRecords: List<EventBiOperationalRecord> = dataset.tickets.map { ticket ->
        val row = ticket.raw
        val relatedEventId = row.ticketEventId()
        val relatedEvent = eventById[relatedEventId]?.raw
        val status = row.statusValue()
        val data = row.obj("data")
        val method = row.ticketApprovalMethod()
        val source = row.ticketSource()
        val category = row.ticketItemCategory()
        val lotName = row.ticketLotName()
        val itemName = row.ticketItemName()
        val entries = row.readTicketEntries()
        val completedAt = row.ticketRowCheckinAt().takeIf { it > 0L } ?: entries.latestEntryDate()
        val completedBy = firstText(
            row.firstNotBlank("checkinByUserName", "checkinBy"),
            data.firstNotBlank("checkinByUserName"),
            entries.firstNotNullOfOrNull { entry ->
                entry.firstNotBlank("scannedByUserName", "usedByUserName", "checkinByUserName")
                    .takeIf { it.isNotBlank() }
            },
        ).ifBlank { "-" }
        val completionMethod = approvalMethodLabel(
            firstText(row.str("checkinMethod"), data.str("checkinMethod")),
            "",
        ).ifBlank {
            entries.map { it.entryScanSource() }.firstOrNull { it != "-" }
                ?: if (completedAt > 0L) "QR code" else "-"
        }
        val transferred = row.extractTicketTransfers().isNotEmpty() ||
            entries.any { it.isTransferredEntry() }
        val manual = row.isManualTicket()
        val courtesy = isCourtesyText("$category $lotName $itemName $method")
        val classified = EventBiStatus.classify(status)
        val approved = classified == EventBiStatus.Approved
        val rejected = classified == EventBiStatus.Rejected
        val cancelled = classified == EventBiStatus.Cancelled || classified == EventBiStatus.Refunded

        EventBiOperationalRecord(
            id = row.str("id"),
            eventId = relatedEventId,
            eventName = eventLabelOf(relatedEventId, row.str("eventoNome")),
            kind = EventBiRecordKind.Ticket,
            status = firstText(row.str("status"), status).ifBlank { "-" },
            statusFilter = statementStatusFromStatus(status),
            typeLabel = when {
                transferred -> "Transferência"
                manual -> "Cadastro manual"
                courtesy -> "Cortesia"
                else -> "Ingresso"
            },
            itemName = itemName,
            category = category,
            lotName = lotName,
            quantity = row.ticketQuantity(),
            value = row.ticketValue(),
            expectedValue = row.expectedTicketTotal(relatedEvent),
            discount = row.ticketDiscount(),
            discountSource = row.ticketDiscountSource(),
            createdAtMillis = row.ticketPurchaseDate(),
            approvedAtMillis = row.ticketApprovalDate(),
            completedAtMillis = completedAt,
            approver = row.ticketApproverName(),
            approvalMethod = method,
            source = source,
            paymentSource = row.ticketPaymentSource(),
            createdBy = data.firstNotBlank("createdByName", "createdBy", "operatorName").ifBlank { "-" },
            completionMethod = completionMethod,
            completedBy = completedBy,
            manual = manual,
            manualAtDoor = data.truthy("manualGateEntry") ||
                normalizeEventBiText("$source $method $lotName").contains("porta"),
            hasCode = row.ticketHasQrCode(),
            usedQuantity = row.ticketScannedCount(),
            approved = approved,
            pending = !approved && !rejected && !cancelled,
            rejected = rejected,
            cancelled = cancelled,
            transferred = transferred,
            courtesy = courtesy,
        )
    }

    private val orderRecords: List<EventBiOperationalRecord> = dataset.orders.map { order ->
        val row = order.raw
        val orderEvent = order.resolvedEventId()
        val status = row.statusValue()
        val eventParty = row.obj("data").obj("eventParty")
        val product = productsById[row.orderProductId()]?.raw
        val method = row.orderApprovalMethod()
        val source = row.orderSource()
        val itemName = row.orderItemName(product)
        val category = row.orderItemCategory(product)
        val manual = row.isManualOrder()
        val transferred = row.extractProductTransfers().isNotEmpty() ||
            row.readVoucherEntries().any { it.isTransferredEntry() }
        val courtesy = isCourtesyText("$category $itemName $method $source")
        val classified = EventBiStatus.classify(status)
        val approved = classified == EventBiStatus.Approved
        val rejected = classified == EventBiStatus.Rejected
        val cancelled = classified == EventBiStatus.Cancelled || classified == EventBiStatus.Refunded

        EventBiOperationalRecord(
            id = row.str("id"),
            eventId = orderEvent,
            eventName = eventById[orderEvent]?.name ?: "Evento",
            kind = EventBiRecordKind.Product,
            status = firstText(row.str("status"), status).ifBlank { "-" },
            statusFilter = statementStatusFromStatus(status),
            typeLabel = when {
                transferred -> "Transferência"
                manual -> "Cadastro manual"
                courtesy -> "Cortesia"
                else -> "Produto"
            },
            itemName = itemName,
            category = category,
            lotName = firstText(row.str("eventLoteNome"), eventParty.str("loteNome")).ifBlank { "-" },
            quantity = row.orderQuantity(),
            value = row.orderTotal(),
            expectedValue = row.expectedOrderTotal(product),
            discount = row.orderDiscount(),
            discountSource = row.orderDiscountSource(),
            createdAtMillis = row.orderCreatedAt(),
            approvedAtMillis = row.orderApprovalDate(),
            completedAtMillis = row.orderWithdrawalDate(),
            approver = row.orderApproverName(),
            approvalMethod = method,
            source = source,
            paymentSource = row.orderPaymentSource(),
            createdBy = firstText(
                row.str("eventCreatedByName"),
                eventParty.firstNotBlank("createdByName", "createdByUserName"),
            ).ifBlank { "-" },
            completedBy = row.orderWithdrawalOperator(),
            completionMethod = row.orderWithdrawalMethod(),
            manual = manual,
            manualAtDoor = normalizeEventBiText("$source $method $category").contains("porta"),
            hasCode = row.orderHasCode(),
            usedQuantity = row.orderRedeemedQuantity(),
            approved = approved,
            pending = !approved && !rejected && !cancelled,
            rejected = rejected,
            cancelled = cancelled,
            transferred = transferred,
            courtesy = courtesy,
        )
    }

    /** `operationalRecords` (4081): só o que tem evento resolvido. */
    private val operationalRecords = (ticketRecords + orderRecords).filter { it.eventId.isNotBlank() }
    private val approvedOperationalRecords = operationalRecords.filter { it.approved }
    private val pendingOperationalRecords = operationalRecords.filter { it.pending }

    private val operationalApprovalDurations = approvedOperationalRecords
        .map { hoursBetween(it.createdAtMillis, it.approvedAtMillis) }
        .finiteNonNegative()
    private val pendingWaitHours = pendingOperationalRecords
        .map { if (it.createdAtMillis > 0L) (now - it.createdAtMillis) / MillisPerHour else Double.NaN }
        .finiteNonNegative()

    /** `singleOperationalEventId` (4094). */
    private val singleOperationalEventId = when {
        filter.hasEventFilter -> filter.eventId
        dataset.events.size == 1 -> dataset.events.first().id
        else -> ""
    }

    private fun eventHref(eventId: String, options: EventBiStatementLink = EventBiStatementLink()) =
        links.statement(eventId, options)

    private fun checkinsHref(eventId: String, options: EventBiCheckinsLink = EventBiCheckinsLink()) =
        links.checkins(eventId, options)

    /** `toStatementHref` (3942): o link herda tipo/status/busca do próprio registro. */
    private fun EventBiOperationalRecord.statementHref(
        status: EventBiStatementStatus? = null,
        search: String? = null,
        alert: String = "",
    ) = eventHref(
        eventId,
        EventBiStatementLink(
            type = kind,
            status = status ?: statusFilter,
            search = search ?: itemName,
            alert = alert,
        ),
    )

    private companion object {
        val ApprovedRejectedCancelled = setOf(
            EventBiStatus.Approved, EventBiStatus.Rejected, EventBiStatus.Cancelled,
        )
    }

    // ------------------------------------------------------------------
    // Pendências (4105-4155)
    // ------------------------------------------------------------------

    private val pendingByEvent = EventBiMetricBucket()
    private val pendingByType = EventBiMetricBucket()
    private val pendingAgeBuckets = EventBiMetricBucket().apply {
        listOf("Menos de 15 min", "15 a 60 min", "1 a 6h", "6 a 24h", "Mais de 24h").forEach { seed(it) }
    }

    private fun buildPendingSection() {
        pendingOperationalRecords.forEach { record ->
            pendingByEvent.add(
                record.eventName, 1.0, record.value, 0.0,
                eventHref(record.eventId, EventBiStatementLink(status = EventBiStatementStatus.Pending)),
            )
            pendingByType.add(
                record.typeLabel, 1.0, record.value, 0.0,
                if (singleOperationalEventId.isNotBlank()) {
                    eventHref(
                        singleOperationalEventId,
                        EventBiStatementLink(
                            type = record.kind,
                            status = EventBiStatementStatus.Pending,
                            // Rótulo genérico não vira busca (4130).
                            search = if (record.typeLabel == "Produto" || record.typeLabel == "Ingresso") {
                                ""
                            } else {
                                record.typeLabel
                            },
                        ),
                    )
                } else {
                    ""
                },
            )
            val ageHours = if (record.createdAtMillis > 0L) {
                (now - record.createdAtMillis) / MillisPerHour
            } else {
                0.0
            }
            val ageBucket = when {
                ageHours < 0.25 -> "Menos de 15 min"
                ageHours < 1 -> "15 a 60 min"
                ageHours < 6 -> "1 a 6h"
                ageHours < 24 -> "6 a 24h"
                else -> "Mais de 24h"
            }
            pendingAgeBuckets.add(ageBucket, 1.0, record.value)
        }
    }

    /** `pendingNearEvent` (4148): pendente de evento que começa em até 24h (ou começou há 2h). */
    private val pendingNearEvent by lazy {
        pendingOperationalRecords.count { record ->
            val start = eventStartOf(record.eventId)
            if (start <= 0L) return@count false
            val hoursUntilEvent = (start - now) / MillisPerHour
            hoursUntilEvent in -2.0..24.0
        }
    }

    private val pendingAtDoor by lazy { pendingOperationalRecords.count { it.manualAtDoor } }

    // ------------------------------------------------------------------
    // SLA e qualidade de operador (4157-4364)
    // ------------------------------------------------------------------

    /** `showEventInOperationalGroups` (4169). */
    private val showEventInOperationalGroups = !filter.hasEventFilter && dataset.events.size > 1

    private class OperationalGroup(
        val name: String,
        val rawName: String,
        val eventId: String,
        val records: MutableList<EventBiOperationalRecord> = mutableListOf(),
    )

    /** `linkedOperationalGroups` (4170). */
    private fun linkedOperationalGroups(
        records: List<EventBiOperationalRecord>,
        getName: (EventBiOperationalRecord) -> String,
    ): List<OperationalGroup> {
        val groups = LinkedHashMap<String, OperationalGroup>()
        records.forEach { record ->
            val rawName = getName(record).trim().ifBlank { "Sem dado" }
            val key = if (showEventInOperationalGroups) "${record.eventId}:$rawName" else rawName
            groups.getOrPut(key) {
                OperationalGroup(
                    name = if (showEventInOperationalGroups) "${record.eventName} · $rawName" else rawName,
                    rawName = rawName,
                    eventId = record.eventId,
                )
            }.records += record
        }
        return groups.values.toList()
    }

    /** `groupRecords` (4157). */
    private fun groupRecords(
        records: List<EventBiOperationalRecord>,
        getKey: (EventBiOperationalRecord) -> String,
    ): Map<String, List<EventBiOperationalRecord>> {
        val groups = LinkedHashMap<String, MutableList<EventBiOperationalRecord>>()
        records.forEach { record ->
            groups.getOrPut(getKey(record).trim().ifBlank { "Sem dado" }) { mutableListOf() } += record
        }
        return groups
    }

    /** `buildSlaMetricRows` (4190): mediana no `value`, P90 no `secondary`, P90/P95 no `hint`. */
    private fun buildSlaMetricRows(
        groups: List<OperationalGroup>,
        hrefOptions: (String) -> EventBiStatementLink,
    ): List<EventBiMetricRow> = groups
        .map { group ->
            val durations = group.records
                .map { hoursBetween(it.createdAtMillis, it.approvedAtMillis) }
                .finiteNonNegative()
            val p90 = eventBiPercentile(durations, 0.9)
            EventBiMetricRow(
                name = group.name,
                quantity = group.records.size.toDouble(),
                value = eventBiMedian(durations),
                average = durations.mean(),
                secondary = p90,
                hint = "P90 ${formatEventBiHours(p90)} · P95 ${formatEventBiHours(eventBiPercentile(durations, 0.95))}",
                href = eventHref(
                    group.eventId,
                    hrefOptions(group.rawName).copy(status = EventBiStatementStatus.Approved),
                ),
            )
        }
        .sortedWith(compareByDescending<EventBiMetricRow> { it.quantity }.thenByDescending { it.value })
        .take(12)

    private val slaBySourceRows by lazy {
        buildSlaMetricRows(linkedOperationalGroups(approvedOperationalRecords) { it.source }) { source ->
            EventBiStatementLink(source = source)
        }
    }
    private val slaByApproverRows by lazy {
        buildSlaMetricRows(linkedOperationalGroups(approvedOperationalRecords) { it.approver }) { approver ->
            EventBiStatementLink(approver = approver)
        }
    }
    private val slaByEventRows by lazy {
        buildSlaMetricRows(
            groupRecords(approvedOperationalRecords) { it.eventName }.map { (name, records) ->
                OperationalGroup(name, name, records.firstOrNull()?.eventId.orEmpty(), records.toMutableList())
            },
        ) { EventBiStatementLink() }
    }

    private val approvalToEntryDurations by lazy {
        approvedOperationalRecords
            .filter { it.kind == EventBiRecordKind.Ticket }
            .map { hoursBetween(it.approvedAtMillis, it.completedAtMillis) }
            .finiteNonNegative()
    }
    private val approvalToWithdrawalDurations by lazy {
        approvedOperationalRecords
            .filter { it.kind == EventBiRecordKind.Product }
            .map { hoursBetween(it.approvedAtMillis, it.completedAtMillis) }
            .finiteNonNegative()
    }
    private val approvedWithoutCode by lazy { approvedOperationalRecords.filter { !it.hasCode } }
    private val codeWithoutUse by lazy {
        approvedOperationalRecords.filter { it.hasCode && it.usedQuantity <= 0 }
    }
    private val usedWithoutApproval by lazy {
        operationalRecords.filter { !it.approved && it.usedQuantity > 0 }
    }
    private val inconsistentStatus by lazy {
        operationalRecords.filter { record ->
            (record.approved && record.approvedAtMillis <= 0L) ||
                (!record.approved && record.usedQuantity > 0) ||
                (record.cancelled && record.usedQuantity > 0)
        }
    }

    /** `approvedNearEvent` (4259): aprovado entre 1h depois e 2h antes do início. */
    private val approvedNearEvent by lazy {
        approvedOperationalRecords.filter { record ->
            val start = eventStartOf(record.eventId)
            if (start <= 0L || record.approvedAtMillis <= 0L) return@filter false
            val hoursBeforeEvent = (start - record.approvedAtMillis) / MillisPerHour
            hoursBeforeEvent in -1.0..2.0
        }
    }
    private val approvedNearEventIds by lazy { approvedNearEvent.map { it.identity }.toSet() }

    /** `operatorQualityRows` (4266). */
    private val operatorQualityRows by lazy {
        linkedOperationalGroups(approvedOperationalRecords) { it.approver }
            .map { group ->
                val operator = group.rawName
                val records = group.records
                val durations = records
                    .map { hoursBetween(it.createdAtMillis, it.approvedAtMillis) }
                    .finiteNonNegative()
                val cancelledAfterApproval = operationalRecords.count { record ->
                    record.eventId == group.eventId && record.cancelled &&
                        record.approver == operator && record.approvedAtMillis > 0L
                }
                val approverLink = EventBiStatementLink(
                    status = EventBiStatementStatus.Approved, approver = operator,
                )
                tableRowOf(
                    "evento" to (records.firstOrNull()?.eventName ?: "-"),
                    "operador" to operator,
                    "aprovados" to records.size.toDouble(),
                    "valor" to records.sumOf { it.value },
                    "mediana" to eventBiMedian(durations),
                    "semValor" to records.count { it.value <= 0 }.toDouble(),
                    "manuais" to records.count {
                        it.manual || normalizeEventBiText(it.approvalMethod).contains("manual")
                    }.toDouble(),
                    "corrigidos" to cancelledAfterApproval.toDouble(),
                    "semUso" to records.count { it.usedQuantity <= 0 }.toDouble(),
                    "mesmoCriador" to records.count {
                        it.createdBy != "-" &&
                            normalizeEventBiText(it.createdBy) == normalizeEventBiText(it.approver)
                    }.toDouble(),
                    "mesmoBaixa" to records.count {
                        it.completedBy != "-" &&
                            normalizeEventBiText(it.completedBy) == normalizeEventBiText(it.approver)
                    }.toDouble(),
                    "href" to eventHref(group.eventId, approverLink),
                    "hrefAprovados" to eventHref(group.eventId, approverLink),
                    "hrefValor" to eventHref(group.eventId, approverLink),
                    "hrefSemValor" to eventHref(group.eventId, approverLink.copy(indicator = "sem-valor")),
                    "hrefManuais" to eventHref(group.eventId, approverLink.copy(flow = EventBiFlow.Approval)),
                    "hrefCorrigidos" to eventHref(
                        group.eventId,
                        EventBiStatementLink(approver = operator, indicator = "cancelado-pos-aprovacao"),
                    ),
                    "hrefSemUso" to eventHref(group.eventId, approverLink.copy(indicator = "sem-uso")),
                    "hrefMesmoCriador" to eventHref(group.eventId, approverLink.copy(indicator = "mesmo-criador")),
                    "hrefMesmoBaixa" to eventHref(group.eventId, approverLink.copy(indicator = "mesmo-baixa")),
                )
            }
            .sortedByDescending { it.number("aprovados") }
    }

    private val activeOperatorCount by lazy {
        approvedOperationalRecords
            .map { it.approver }
            .filter { it.isNotBlank() && it != "Sem aprovador" }
            .distinct()
            .size
    }

    private val operatorDistributionRows by lazy {
        val bucket = EventBiMetricBucket()
        approvedOperationalRecords.forEach { record ->
            bucket.add(
                if (showEventInOperationalGroups) "${record.eventName} · ${record.approver}" else record.approver,
                1.0,
                record.value,
                0.0,
                eventHref(
                    record.eventId,
                    EventBiStatementLink(status = EventBiStatementStatus.Approved, approver = record.approver),
                ),
            )
        }
        bucket.rows(12)
    }

    /** `demandWithoutCoverageRows` (4342): hora com pedido criado e nenhuma aprovação. */
    private val demandWithoutCoverageRows by lazy {
        val createdByHour = LinkedHashMap<String, Int>()
        val approvedByHour = LinkedHashMap<String, Int>()
        operationalRecords.forEach { record ->
            eventBiHourKey(record.createdAtMillis).takeIf { it.isNotBlank() }?.let { key ->
                createdByHour[key] = (createdByHour[key] ?: 0) + 1
            }
            eventBiHourKey(record.approvedAtMillis).takeIf { it.isNotBlank() }?.let { key ->
                approvedByHour[key] = (approvedByHour[key] ?: 0) + 1
            }
        }
        createdByHour.entries
            .filter { (approvedByHour[it.key] ?: 0) == 0 }
            .map { tableRowOf("horario" to it.key, "criados" to it.value.toDouble(), "aprovacoes" to 0.0) }
            .sortedByDescending { it.number("criados") }
            .take(10)
    }

    /** `outsideHoursApprovals` (4347): aprovação antes das 8h ou a partir das 23h. */
    private val outsideHoursApprovals by lazy {
        approvedOperationalRecords.count { record ->
            if (record.approvedAtMillis <= 0L) return@count false
            val hour = eventBiHourLabel(record.approvedAtMillis).removeSuffix("h").toIntOrNull()
                ?: return@count false
            hour < 8 || hour >= 23
        }
    }

    private val singleOperatorEventRows by lazy {
        groupRecords(approvedOperationalRecords) { it.eventName }
            .map { (eventLabel, records) ->
                val operators = records.map { it.approver }.filter { it != "Sem aprovador" }.distinct()
                tableRowOf(
                    "evento" to eventLabel,
                    "operador" to (operators.firstOrNull() ?: "Sem aprovador"),
                    "aprovacoes" to records.size.toDouble(),
                    "href" to eventHref(
                        records.firstOrNull()?.eventId.orEmpty(),
                        EventBiStatementLink(status = EventBiStatementStatus.Approved),
                    ),
                    "operadores" to operators.size.toDouble(),
                )
            }
            .filter { it.number("operadores") == 1.0 && it.number("aprovacoes") > 0 }
            .sortedByDescending { it.number("aprovacoes") }
            .take(10)
    }

    // ------------------------------------------------------------------
    // Manualidade por etapa (4366-4435)
    // ------------------------------------------------------------------

    private class ManualityStage(
        val evento: String,
        val tipo: String,
        val etapa: String,
        var quantidade: Double = 0.0,
        var valor: Double = 0.0,
        var percentual: Double = 0.0,
        val href: String,
    )

    private val manualityStageRowsRaw by lazy {
        val groups = LinkedHashMap<String, ManualityStage>()

        fun addStage(
            record: EventBiOperationalRecord,
            flow: EventBiFlow,
            label: String,
            indicator: String,
            status: EventBiStatementStatus? = null,
        ) {
            val typeLabel = if (record.kind == EventBiRecordKind.Product) {
                "Produto / modo vendas"
            } else {
                "Ingresso"
            }
            val key = "${record.eventId}:${record.kind.remoteValue}:${flow.remoteValue}:$indicator"
            val sameBase = operationalRecords.count {
                it.eventId == record.eventId && it.kind == record.kind
            }
            val current = groups.getOrPut(key) {
                ManualityStage(
                    evento = record.eventName,
                    tipo = typeLabel,
                    etapa = label,
                    href = eventHref(
                        record.eventId,
                        EventBiStatementLink(
                            type = record.kind, status = status, flow = flow, indicator = indicator,
                        ),
                    ),
                )
            }
            current.quantidade += 1
            current.valor += record.value
            current.percentual = safeDivide(current.quantidade, sameBase.toDouble()) * 100
        }

        operationalRecords.forEach { record ->
            val normalizedSource = normalizeEventBiText(record.source)
            val normalizedApproval = normalizeEventBiText(record.approvalMethod)
            val normalizedCompletion = normalizeEventBiText(record.completionMethod)
            val outsideCheckout = normalizedSource.isNotBlank() && normalizedSource != "app" &&
                normalizedSource != "app usc" && normalizedSource != "checkout publico"

            if (record.manual || outsideCheckout) {
                addStage(record, EventBiFlow.Order, "Pedido", "pedido-manual")
            }
            if (
                record.approved &&
                (record.manual || normalizedApproval.contains("manual") || normalizedApproval.contains("admin"))
            ) {
                addStage(
                    record, EventBiFlow.Approval, "Aprovação", "aprovacao-manual",
                    EventBiStatementStatus.Approved,
                )
            }
            if (
                record.kind == EventBiRecordKind.Ticket && record.usedQuantity > 0 &&
                normalizedCompletion.contains("manual")
            ) {
                addStage(record, EventBiFlow.CheckIn, "Check-in", "checkin-manual")
            }
            if (
                record.kind == EventBiRecordKind.Product && record.usedQuantity > 0 &&
                normalizedCompletion.contains("manual")
            ) {
                addStage(record, EventBiFlow.Withdrawal, "Retirada", "retirada-manual")
            }
        }

        groups.values.sortedWith(
            compareByDescending<ManualityStage> { it.quantidade }
                .thenComparator { left, right -> comparePtBr(left.evento, right.evento) },
        )
    }

    private val manualityStageRows by lazy {
        manualityStageRowsRaw.map { stage ->
            tableRowOf(
                "evento" to stage.evento,
                "tipo" to stage.tipo,
                "etapa" to stage.etapa,
                "quantidade" to stage.quantidade,
                "valor" to stage.valor,
                "percentual" to stage.percentual,
                "href" to stage.href,
            )
        }
    }

    private val manualityStageChartRows by lazy {
        manualityStageRowsRaw.take(12).map { stage ->
            EventBiMetricRow(
                name = "${stage.evento} · ${stage.tipo.replace(" / modo vendas", "")} · ${stage.etapa}",
                quantity = stage.quantidade,
                value = stage.percentual,
                href = stage.href,
            )
        }
    }

    // ------------------------------------------------------------------
    // Os 15 alertas de controle operacional (4437-4493)
    // ------------------------------------------------------------------

    private class AlertGroup(
        val alerta: String,
        val descricao: String,
        val evento: String,
        val item: String,
        val tipo: String,
        var quantidade: Double = 0.0,
        val href: String,
    )

    private val operationalAlertRows by lazy {
        val groups = LinkedHashMap<String, AlertGroup>()

        fun addAlert(
            alertKey: String,
            label: String,
            record: EventBiOperationalRecord,
            status: EventBiStatementStatus = record.statusFilter,
        ) {
            val key = "$alertKey:${record.eventId}:${record.kind.remoteValue}:${record.itemName}:${status.remoteValue}"
            groups.getOrPut(key) {
                AlertGroup(
                    alerta = label,
                    descricao = EventBiOperationalAlertDescriptions[alertKey]
                        ?: EventBiDefaultAlertDescription,
                    evento = record.eventName,
                    item = record.itemName,
                    tipo = record.typeLabel,
                    href = record.statementHref(status = status, search = record.itemName, alert = alertKey),
                )
            }.quantidade += 1
        }

        operationalRecords.forEach { record ->
            val expectedAfterDiscount = if (record.expectedValue.isFinite()) {
                max(0.0, record.expectedValue - record.discount)
            } else {
                Double.NaN
            }
            val hasValueMismatch = expectedAfterDiscount.isFinite() &&
                abs(expectedAfterDiscount - record.value) > 0.01

            if (record.approved && record.value <= 0) {
                addAlert("aprovado-sem-valor", "Aprovado sem valor", record, EventBiStatementStatus.Approved)
            }
            if (record.approved && record.value <= 0 && !record.courtesy) {
                addAlert(
                    "valor-zero-sem-cortesia", "Valor zero sem ser cortesia", record,
                    EventBiStatementStatus.Approved,
                )
            }
            if (record.approved && record.courtesy && record.value > 0) {
                addAlert(
                    "cortesia-com-valor", "Cortesia com valor maior que zero", record,
                    EventBiStatementStatus.Approved,
                )
            }
            if (record.discount > 0 && record.discountSource.isBlank()) {
                addAlert("desconto-sem-origem", "Desconto sem origem registrada", record)
            }
            if (record.approved && hasValueMismatch) {
                addAlert(
                    "valor-diferente-tabela", "Valor aprovado diferente do preço do lote/produto",
                    record, EventBiStatementStatus.Approved,
                )
            }
            if (record.manual && hasValueMismatch) {
                addAlert("manual-fora-padrao", "Pedido manual com valor fora do padrão", record)
            }
            if (hasValueMismatch && record.discount <= 0) {
                addAlert("preco-incompativel", "Ingresso/produto com preço incompatível com lote", record)
            }
            if (record.approved && record.approvalMethod == "-") {
                addAlert(
                    "pagamento-sem-metodo", "Pagamento aprovado sem método registrado", record,
                    EventBiStatementStatus.Approved,
                )
            }
            if (record.approved && record.paymentSource == "-" && !record.courtesy && !record.manual) {
                addAlert(
                    "aprovado-sem-fonte-pagamento", "Pedido aprovado sem fonte de pagamento", record,
                    EventBiStatementStatus.Approved,
                )
            }
            if (record.transferred && record.value > 0) {
                addAlert("transferencia-valor-incompativel", "Transferência com valor incompatível", record)
            }
            if (record.approved && !record.hasCode) {
                addAlert(
                    "aprovado-sem-codigo", "Pedido aprovado sem QR/código", record,
                    EventBiStatementStatus.Approved,
                )
            }
            if (record.approved && record.hasCode && record.usedQuantity <= 0) {
                addAlert(
                    "codigo-sem-uso", "Pedido com QR/código, mas sem uso", record,
                    EventBiStatementStatus.Approved,
                )
            }
            if (!record.approved && record.usedQuantity > 0) {
                addAlert("uso-sem-aprovacao", "Pedido usado sem aprovação clara", record)
            }
            if (
                (record.approved && record.approvedAtMillis <= 0L) ||
                (record.cancelled && record.usedQuantity > 0)
            ) {
                addAlert("status-incoerente", "Pedido com status incoerente", record)
            }
            if (record.identity in approvedNearEventIds) {
                addAlert(
                    "aprovado-perto-evento", "Pedido aprovado muito perto do horário do evento",
                    record, EventBiStatementStatus.Approved,
                )
            }
        }

        groups.values
            .sortedWith(
                compareByDescending<AlertGroup> { it.quantidade }
                    .thenComparator { left, right -> comparePtBr(left.alerta, right.alerta) },
            )
            .map { group ->
                tableRowOf(
                    "alerta" to group.alerta,
                    "descricao" to group.descricao,
                    "evento" to group.evento,
                    "item" to group.item,
                    "tipo" to group.tipo,
                    "quantidade" to group.quantidade,
                    "href" to group.href,
                )
            }
    }

    // ------------------------------------------------------------------
    // Acumuladores comerciais (3899-3940)
    // ------------------------------------------------------------------

    private val byLot = EventBiMetricBucket()
    private val byClass = EventBiMetricBucket()
    private val byAudience = EventBiMetricBucket()
    private val byWeekday = EventBiMetricBucket()
    private val byPeriod = EventBiMetricBucket()
    private val byPrice = EventBiMetricBucket()
    private val byApprover = EventBiMetricBucket()
    private val byApprovalMethod = EventBiMetricBucket()
    private val byTicketApprover = EventBiMetricBucket()
    private val byTicketApprovalMethod = EventBiMetricBucket()
    private val noShowByClass = EventBiMetricBucket()
    private val noShowByLot = EventBiMetricBucket()
    private val scanByHour = EventBiMetricBucket()
    private val byProductCategory = EventBiMetricBucket()
    private val byDiscountSource = EventBiMetricBucket()
    private val byOrderSource = EventBiMetricBucket()
    private val byWithdrawalMethod = EventBiMetricBucket()
    private val byWithdrawalOperator = EventBiMetricBucket()
    private val byTransferMode = EventBiMetricBucket()
    private val byTransferTarget = EventBiMetricBucket()
    private val byTransferActor = EventBiMetricBucket()
    private val eventSummary = EventBiMetricBucket()

    /** `leadBuckets` (3921): já vem com os seis baldes zerados, na ordem. */
    private val leadBuckets = EventBiMetricBucket().apply {
        listOf(
            "30 dias ou mais", "15 a 29 dias", "7 a 14 dias", "3 a 6 dias", "24 a 72h", "Menos de 24h",
        ).forEach { seed(it) }
    }

    private val productRows = LinkedHashMap<String, EventBiProductMetricRow>()
    private val buyerPurchases = LinkedHashMap<String, Int>()
    private val ticketBuyerPurchases = LinkedHashMap<String, Int>()
    private val checkedInTicketBuyerIds = LinkedHashSet<String>()
    private val productPurchaseBuyerIds = LinkedHashSet<String>()
    private val transferKeys = LinkedHashSet<String>()
    private val approvalDurations = mutableListOf<Double>()
    private val ticketApprovalDurations = mutableListOf<Double>()
    private val scanTokens = LinkedHashMap<String, Int>()

    private var pendingLess1 = 0
    private var pendingOneTo6 = 0
    private var pendingSixTo24 = 0
    private var pendingMore24 = 0
    private var ticketPendingLess1 = 0
    private var ticketPendingOneTo6 = 0
    private var ticketPendingSixTo24 = 0
    private var ticketPendingMore24 = 0
    private var invalidScans = 0
    private var appScans = 0
    private var manualScans = 0
    private var slowApprovals = 0

    /** `addTransfer` (4502): chave já vista não conta de novo, e o período filtra. */
    private fun addTransfer(transfer: EventBiTransfer) {
        if (transfer.key in transferKeys) return
        if (
            transfer.atMillis > 0L &&
            !eventBiDateInPeriod(transfer.atMillis, filter.startDate, filter.endDate)
        ) {
            return
        }
        transferKeys += transfer.key
        byTransferMode.add(transfer.mode, 1.0, 0.0)
        byTransferTarget.add(transfer.target, 1.0, 0.0)
        byTransferActor.add(transfer.actor.ifBlank { "Sem usuário" }, 1.0, 0.0)
    }

    private fun buildTransfers() {
        dataset.tickets.forEach { ticket -> ticket.raw.extractTicketTransfers().forEach(::addTransfer) }
        dataset.orders.forEach { order -> order.raw.extractProductTransfers().forEach(::addTransfer) }
    }

    // ------------------------------------------------------------------
    // Audiência (4514-4563)
    // ------------------------------------------------------------------

    private fun addTicketAudience(ticket: EventBiTicket, basis: EventBiAudienceBasis) {
        val row = ticket.raw
        val quantity = row.ticketQuantity()
        val value = row.ticketValue()
        val entries = row.readTicketEntries()
        val activeEntries = row.activeTicketEntries()
        val user = userById.userOf(row.str("userId"))

        if (basis == EventBiAudienceBasis.CheckIn) {
            val checkedEntries = activeEntries.filter { it.isTicketEntryCheckedIn() }
            when {
                checkedEntries.isNotEmpty() -> checkedEntries.forEach { entry ->
                    byAudience.add(
                        row.classifyTicketAudience(entry, user), 1.0,
                        safeDivide(value, quantity.toDouble()),
                    )
                }
                entries.isEmpty() && row.ticketRowCheckinAt() > 0L ->
                    byAudience.add(row.classifyTicketAudience(null, user), quantity.toDouble(), value)
            }
            return
        }

        when {
            activeEntries.isNotEmpty() -> activeEntries.forEach { entry ->
                byAudience.add(
                    row.classifyTicketAudience(entry, user), 1.0,
                    safeDivide(value, quantity.toDouble()),
                )
            }
            entries.isEmpty() ->
                byAudience.add(row.classifyTicketAudience(null, user), quantity.toDouble(), value)
        }
    }

    private fun addOrderAudience(order: EventBiOrder, basis: EventBiAudienceBasis) {
        val row = order.raw
        val quantity = row.orderQuantity()
        val value = row.orderTotal()
        val audienceQuantity = if (basis == EventBiAudienceBasis.CheckIn) {
            row.orderCheckedInAudienceQuantity()
        } else {
            row.orderAudienceQuantity()
        }
        if (audienceQuantity <= 0) return
        byAudience.add(
            row.classifyOrderAudience(userById.userOf(row.str("userId"))),
            audienceQuantity.toDouble(),
            value * safeDivide(audienceQuantity.toDouble(), quantity.toDouble()),
        )
    }

    /**
     * `audienceBasis` (4554): "pedidos" varre tudo; "aprovados" e "checkin" varrem só o aprovado.
     */
    private fun buildAudience() {
        if (filter.audienceBasis == EventBiAudienceBasis.Orders) {
            dataset.tickets.forEach { addTicketAudience(it, filter.audienceBasis) }
            dataset.orders.forEach { addOrderAudience(it, filter.audienceBasis) }
        } else {
            approvedTickets.forEach { addTicketAudience(it, filter.audienceBasis) }
            approvedOrders.forEach { addOrderAudience(it, filter.audienceBasis) }
        }
    }

    // ------------------------------------------------------------------
    // Varredura dos aprovados (4565-4700)
    // ------------------------------------------------------------------

    private fun buildApprovedTicketMetrics() {
        approvedTickets.forEach { ticket ->
            val row = ticket.raw
            val quantity = row.ticketQuantity()
            val value = row.ticketValue()
            val purchase = row.ticketPurchaseDate()
            val scanned = row.ticketScannedCount()
            val relatedEventId = row.ticketEventId()
            val eventLabel = eventLabelOf(relatedEventId, row.str("eventoNome"))
            val noShowCount = max(quantity - scanned, 0)
            val unit = safeDivide(value, quantity.toDouble())
            val lotName = row.ticketLotName()
            val className = row.ticketClassName()
            val approver = row.ticketApproverName()
            val buyerId = row.ticketBuyerId()

            byLot.add(lotName, quantity.toDouble(), value)
            byClass.add(className, quantity.toDouble(), value)
            byPrice.add(formatEventBiCurrency(unit), quantity.toDouble(), value)
            noShowByClass.add(className, noShowCount.toDouble(), 0.0)
            noShowByLot.add(lotName, noShowCount.toDouble(), 0.0)
            byApprover.add(approver, 1.0, value)
            byTicketApprover.add(approver, 1.0, value)
            val ticketMethod = row.ticketApprovalMethod()
            byApprovalMethod.add(ticketMethod, 1.0, value)
            byTicketApprovalMethod.add(ticketMethod, 1.0, value)
            eventSummary.add(eventLabel, quantity.toDouble(), value, scanned.toDouble())

            // `addPurchase` (4495): conta a compra e distribui em dia da semana e período.
            buyerPurchases[buyerId] = (buyerPurchases[buyerId] ?: 0) + 1
            byWeekday.add(eventBiWeekdayLabel(purchase), quantity.toDouble(), value)
            byPeriod.add(eventBiPeriodLabel(purchase), quantity.toDouble(), value)
            ticketBuyerPurchases[buyerId] = (ticketBuyerPurchases[buyerId] ?: 0) + 1

            val eventStart = eventStartOf(relatedEventId)
            if (purchase > 0L && eventStart > 0L) {
                leadBuckets.addToSeeded(
                    eventBiLeadBucketLabel(purchase, eventStart), quantity.toDouble(), value,
                )
            }

            val entries = row.readTicketEntries()
            entries.forEach { entry ->
                val scannedAt = entry.entryScannedAt()
                val token = entry.firstNotBlank("token", "id", "codigo", "qrCode")
                if (token.isNotBlank() && scannedAt > 0L) {
                    scanTokens[token] = (scanTokens[token] ?: 0) + 1
                }
                if (scannedAt > 0L) {
                    scanByHour.add(eventBiHourLabel(scannedAt), 1.0, safeDivide(value, quantity.toDouble()))
                    if (entry.entryScanSource() == "Manual") manualScans += 1 else appScans += 1
                    checkedInTicketBuyerIds += buyerId
                }
            }
            if (entries.isEmpty() && scanned > 0) checkedInTicketBuyerIds += buyerId
            invalidScans += row.ticketInvalidScanCount()

            val approval = row.ticketApprovalDate()
            val hours = hoursBetween(purchase, approval)
            if (hours.isFinite() && hours >= 0) {
                approvalDurations += hours
                ticketApprovalDurations += hours
                if (hours > 24) slowApprovals += 1
            }
        }
    }

    private fun buildApprovedOrderMetrics() {
        approvedOrders.forEach { order ->
            val row = order.raw
            val quantity = row.orderQuantity()
            val value = row.orderTotal()
            val redeemed = row.orderRedeemedQuantity()
            val pending = max(0, quantity - redeemed)
            val product = productsById[row.orderProductId()]?.raw
            val productLabel = row.orderItemName(product)
            val category = row.orderItemCategory(product)
            val buyerId = row.orderBuyerId()

            val existing = productRows[productLabel]
                ?: EventBiProductMetricRow(name = productLabel)
            val merged = existing.copy(
                quantity = existing.quantity + quantity,
                value = existing.value + value,
                redeemed = existing.redeemed + redeemed,
                pending = existing.pending + pending,
            )
            productRows[productLabel] = merged.copy(average = safeDivide(merged.value, merged.quantity))

            byProductCategory.add(category, quantity.toDouble(), value)
            byApprover.add(row.orderApproverName(), 1.0, value)
            byApprovalMethod.add(row.orderApprovalMethod(), 1.0, value)
            val discount = row.orderDiscount()
            byDiscountSource.add(row.orderDiscountSource(), if (discount > 0) 1.0 else 0.0, discount)
            byOrderSource.add(row.orderSource(), 1.0, value)
            if (redeemed > 0) {
                val share = value * safeDivide(redeemed.toDouble(), quantity.toDouble())
                byWithdrawalMethod.add(row.orderWithdrawalMethod(), redeemed.toDouble(), share)
                byWithdrawalOperator.add(row.orderWithdrawalOperator(), redeemed.toDouble(), share)
            }
            buyerPurchases[buyerId] = (buyerPurchases[buyerId] ?: 0) + 1
            if (value > 0) productPurchaseBuyerIds += buyerId

            val hours = hoursBetween(row.orderCreatedAt(), row.orderApprovalDate())
            if (hours.isFinite() && hours >= 0) {
                approvalDurations += hours
                if (hours > 24) slowApprovals += 1
            }
        }
    }

    /** `[...pendingTickets, ...pendingOrders]` (4686): envelhecimento do pendente. */
    private fun buildPendingAging() {
        pendingTickets.forEach { ticket ->
            val createdAt = ticket.raw.ticketPurchaseDate()
            if (createdAt <= 0L) return@forEach
            val hours = (now - createdAt) / MillisPerHour
            when {
                hours < 1 -> { pendingLess1 += 1; ticketPendingLess1 += 1 }
                hours < 6 -> { pendingOneTo6 += 1; ticketPendingOneTo6 += 1 }
                hours < 24 -> { pendingSixTo24 += 1; ticketPendingSixTo24 += 1 }
                else -> { pendingMore24 += 1; ticketPendingMore24 += 1 }
            }
        }
        pendingOrders.forEach { order ->
            val createdAt = order.raw.orderCreatedAt()
            if (createdAt <= 0L) return@forEach
            val hours = (now - createdAt) / MillisPerHour
            when {
                hours < 1 -> pendingLess1 += 1
                hours < 6 -> pendingOneTo6 += 1
                hours < 24 -> pendingSixTo24 += 1
                else -> pendingMore24 += 1
            }
        }
    }

    private val duplicateScans get() = scanTokens.values.count { it > 1 }
    private val ticketScanned by lazy { approvedTickets.sumOf { it.raw.ticketScannedCount() } }
    private val checkedInBuyersWithPurchase
        get() = checkedInTicketBuyerIds.count { it in productPurchaseBuyerIds }
    private val noShow get() = max(0, approvedTicketQuantity - ticketScanned)

    // ------------------------------------------------------------------
    // Portaria (4709-5250)
    // ------------------------------------------------------------------

    private class OperatorStats(
        var total: Int = 0,
        var qr: Int = 0,
        var manual: Int = 0,
        var invalid: Int = 0,
        var duplicate: Int = 0,
        val eventId: String,
        var href: String,
    )

    private val gateScans = mutableListOf<EventBiGateScan>()
    private val timingBuckets = EventBiMetricBucket().apply {
        listOf(
            "Antes do início", "Primeira hora", "Meio do evento", "Entrada muito tarde", "Não entraram",
        ).forEach { seed(it) }
    }
    private val presenceByType = EventBiCountRateBucket()
    private val presenceByLot = EventBiCountRateBucket()
    private val presenceByClass = EventBiCountRateBucket()
    private val presenceBySource = EventBiCountRateBucket()
    private val presenceByTransfer = EventBiCountRateBucket()
    private val presenceByOperationalCategory = EventBiCountRateBucket()
    private val invalidReasonGroups = EventBiMetricBucket()
    private val operatorStats = LinkedHashMap<String, OperatorStats>()
    private val duplicateContextRows = mutableListOf<EventBiTableRow>()
    private val absentRows = mutableListOf<EventBiTableRow>()
    private val unusedActiveRows = mutableListOf<EventBiTableRow>()

    /** `addTimingBucket` (4774): quantidade também vira valor. */
    private fun addTimingBucket(label: String, quantity: Int, href: String = "") {
        timingBuckets.addToSeeded(label, quantity.toDouble(), quantity.toDouble(), href)
    }

    /** `addOperatorStat` (4781). */
    private fun addOperatorStat(operator: String, eventId: String, source: String, href: String) {
        val cleanOperator = operator.trim().ifBlank { "Sem operador" }
        val current = operatorStats.getOrPut(cleanOperator) { OperatorStats(eventId = eventId, href = href) }
        when (source) {
            "QR code" -> { current.total += 1; current.qr += 1 }
            "Manual" -> { current.total += 1; current.manual += 1 }
            "Inválida" -> current.invalid += 1
            else -> current.duplicate += 1
        }
        if (current.href.isBlank() && href.isNotBlank()) current.href = href
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun buildGateSection() {
        approvedTickets.forEach { ticket ->
            val row = ticket.raw
            val eventIdValue = row.ticketEventId()
            val eventLabel = eventLabelOf(eventIdValue, row.str("eventoNome"))
            val eventStart = eventStartOf(eventIdValue)
            val quantity = row.ticketQuantity()
            val present = row.ticketScannedCount()
            val absent = max(0, quantity - present)
            val ticketValue = row.ticketValue()
            val unitValue = safeDivide(ticketValue, quantity.toDouble())
            val holderName = row.ticketHolderName()
            val ticketHref = eventHref(
                eventIdValue,
                EventBiStatementLink(
                    type = EventBiRecordKind.Ticket,
                    status = EventBiStatementStatus.Approved,
                    search = holderName,
                ),
            )
            val activeEntries = row.activeTicketEntries()
            val user = userById.userOf(row.str("userId"))
            val member = dataset.memberIndex[row.str("userId")]
            val typeLabel = row.classifyTicketOperationalCategory(
                activeEntries.firstOrNull(), user, member,
            )
            val transferLabel = row.ticketTransferLabel()
            val sourceLabel = row.ticketSource()
            val classLabel = row.ticketClassName()
            val lotLabel = row.ticketLotName()
            val ticketCheckinsHref = checkinsHref(
                eventIdValue,
                EventBiCheckinsLink(
                    search = holderName,
                    indicator = if (absent > 0) "ausente" else "",
                ),
            )

            presenceByType.add(typeLabel, quantity.toDouble(), present.toDouble(), ticketValue, ticketHref)
            presenceByLot.add(lotLabel, quantity.toDouble(), present.toDouble(), ticketValue, ticketHref)
            presenceByClass.add(classLabel, quantity.toDouble(), present.toDouble(), ticketValue, ticketHref)
            presenceBySource.add(sourceLabel, quantity.toDouble(), present.toDouble(), ticketValue, ticketHref)
            presenceByTransfer.add(
                transferLabel, quantity.toDouble(), present.toDouble(), ticketValue, ticketHref,
            )
            presenceByOperationalCategory.add(
                typeLabel, quantity.toDouble(), present.toDouble(), ticketValue, ticketHref,
            )

            // `classifyTiming` (4833).
            fun classifyTiming(scannedAt: Long): String {
                if (scannedAt <= 0L) return "Não entraram"
                if (eventStart <= 0L) return "Meio do evento"
                val hoursFromStart = (scannedAt - eventStart) / MillisPerHour
                return when {
                    hoursFromStart < 0 -> "Antes do início"
                    hoursFromStart <= 1 -> "Primeira hora"
                    hoursFromStart <= 4 -> "Meio do evento"
                    else -> "Entrada muito tarde"
                }
            }

            val entries = row.readTicketEntries()
            entries.forEach { entry ->
                val reason = entry.ticketEntryInvalidReason(row)
                val token = entry.ticketEntryToken()
                val entryHref = eventHref(
                    eventIdValue,
                    EventBiStatementLink(
                        type = EventBiRecordKind.Ticket,
                        status = EventBiStatementStatus.Approved,
                        search = token.ifBlank { row.ticketHolderName(entry) },
                    ),
                )
                if (reason.isNotBlank()) {
                    invalidReasonGroups.add(reason, 1.0, 0.0, 0.0, entryHref)
                    addOperatorStat(entry.entryScanOperator(row), eventIdValue, "Inválida", entryHref)
                }
                row.checkinAuditRows(entry).filter { it.isDuplicateAuditEntry() }.forEach { audit ->
                    val firstAt = entry.entryScannedAt().takeIf { it > 0L } ?: row.ticketRowCheckinAt()
                    val secondAt = parseEventBiDate(
                        audit.firstNotBlank("at", "createdAt", "timestamp"),
                    )
                    val operator = audit.firstNotBlank("byUserName", "operatorName")
                        .ifBlank { entry.entryScanOperator(row) }
                    val diffMinutes = if (firstAt > 0L && secondAt > 0L) {
                        max(0.0, (secondAt - firstAt) / MillisPerMinute)
                    } else {
                        0.0
                    }
                    addOperatorStat(operator, eventIdValue, "Duplicada", entryHref)
                    duplicateContextRows += tableRowOf(
                        "evento" to eventLabel,
                        "pessoa" to row.ticketHolderName(entry),
                        "ingresso" to token.ifBlank { row.str("id") },
                        "primeira" to formatEventBiDateTimeShort(firstAt),
                        "segunda" to formatEventBiDateTimeShort(secondAt),
                        "diferenca" to diffMinutes,
                        "operador" to operator,
                        "acao" to if (normalizeEventBiText(audit.str("action")).contains("manual")) {
                            "Liberado manualmente"
                        } else {
                            "Bloqueado"
                        },
                        "href" to entryHref,
                    )
                }
            }

            activeEntries.filter { it.isTicketEntryCheckedIn() }.forEach { entry ->
                val scannedAt = entry.entryScannedAt().takeIf { it > 0L } ?: row.ticketRowCheckinAt()
                if (scannedAt <= 0L) return@forEach
                val source = if (entry.entryScanSource() == "Manual") "Manual" else "QR code"
                val operator = entry.entryScanOperator(row)
                val entryType = row.classifyTicketOperationalCategory(entry, user, member)
                val entryHref = eventHref(
                    eventIdValue,
                    EventBiStatementLink(
                        type = EventBiRecordKind.Ticket,
                        status = EventBiStatementStatus.Approved,
                        search = entry.ticketEntryToken().ifBlank { row.ticketHolderName(entry) },
                    ),
                )
                gateScans += EventBiGateScan(
                    eventId = eventIdValue,
                    eventName = eventLabel,
                    orderId = row.str("id"),
                    token = entry.ticketEntryToken(),
                    holderName = row.ticketHolderName(entry),
                    turma = row.ticketHolderTurma(entry),
                    lotName = lotLabel,
                    ticketType = entryType,
                    source = source,
                    operator = operator,
                    scannedAtMillis = scannedAt,
                    value = unitValue,
                    href = entryHref,
                    transferLabel = transferLabel,
                )
                addTimingBucket(classifyTiming(scannedAt), 1, entryHref)
                addOperatorStat(operator, eventIdValue, source, entryHref)
            }

            // Ingresso sem entradas, mas com check-in na própria linha (4901).
            val rowCheckinAt = row.ticketRowCheckinAt()
            if (entries.isEmpty() && rowCheckinAt > 0L) {
                val source = if (normalizeEventBiText(row.str("checkinMethod")).contains("manual")) {
                    "Manual"
                } else {
                    "QR code"
                }
                repeat(quantity) {
                    gateScans += EventBiGateScan(
                        eventId = eventIdValue,
                        eventName = eventLabel,
                        orderId = row.str("id"),
                        token = row.str("id"),
                        holderName = holderName,
                        turma = row.ticketHolderTurma(),
                        lotName = lotLabel,
                        ticketType = typeLabel,
                        source = source,
                        operator = row.rowCheckinOperator(),
                        scannedAtMillis = rowCheckinAt,
                        value = unitValue,
                        href = ticketHref,
                        transferLabel = transferLabel,
                    )
                    addTimingBucket(classifyTiming(rowCheckinAt), 1, ticketHref)
                    addOperatorStat(row.rowCheckinOperator(), eventIdValue, source, ticketHref)
                }
            }

            if (absent > 0) {
                addTimingBucket("Não entraram", absent, ticketHref)
                absentRows += tableRowOf(
                    "nome" to holderName,
                    "turma" to row.ticketHolderTurma(),
                    "lote" to lotLabel,
                    "tipo" to typeLabel,
                    "quantidade" to absent.toDouble(),
                    "compra" to formatEventBiDateTimeShort(row.ticketPurchaseDate()),
                    "contato" to row.ticketContact(user),
                    "qr" to row.ticketQrStatus(),
                    "transferencia" to transferLabel,
                    "href" to ticketCheckinsHref.ifBlank { ticketHref },
                )
            }

            val firstEntryReason = entries.firstOrNull().ticketEntryInvalidReason(row)
            val manualCheckin = normalizeEventBiText(row.str("checkinMethod")).contains("manual")
            if (absent > 0 || !row.ticketHasQrCode() || firstEntryReason.isNotBlank() ||
                (present > 0 && manualCheckin)
            ) {
                val reason = when {
                    !row.ticketHasQrCode() -> "Aprovado sem QR (Quick Response)"
                    present > 0 && absent > 0 -> "Aprovado com entrada parcial"
                    present > 0 && manualCheckin -> "Aprovado com entrada manual"
                    firstEntryReason.isNotBlank() -> "Aprovado com tentativa inválida"
                    transferLabel != "Sem transferência" -> "Aprovado e transferido, mas não utilizado"
                    else -> "Aprovado e não utilizado"
                }
                unusedActiveRows += tableRowOf(
                    "situacao" to reason,
                    "nome" to holderName,
                    "turma" to row.ticketHolderTurma(),
                    "lote" to lotLabel,
                    "quantidade" to (if (absent > 0) absent else if (present > 0) present else quantity).toDouble(),
                    "qr" to row.ticketQrStatus(),
                    "href" to ticketCheckinsHref.ifBlank { ticketHref },
                )
            }
        }
    }

    private val gateScansSorted by lazy { gateScans.sortedBy { it.scannedAtMillis } }

    /** `entryCumulativeRows` (4972): acumulado por hora cheia. */
    private val entryCumulativeRows by lazy {
        val cumulativeByHour = EventBiMetricBucket()
        gateScansSorted.forEach { scan ->
            cumulativeByHour.add(eventBiHourKey(scan.scannedAtMillis), 1.0, 0.0, 0.0, scan.href)
        }
        var cumulativeTotal = 0.0
        cumulativeByHour.all()
            .sortedWith { left, right -> comparePtBr(left.name, right.name) }
            .map { row ->
                cumulativeTotal += row.quantity
                row.copy(quantity = cumulativeTotal, value = row.quantity)
            }
    }

    /** `scanModeByHourRows` (4987). */
    private val scanModeByHourRows by lazy {
        val byHour = LinkedHashMap<String, MutableMap<String, Any?>>()
        gateScansSorted.forEach { scan ->
            val label = eventBiHourLabel(scan.scannedAtMillis)
            val current = byHour.getOrPut(label) {
                linkedMapOf(
                    "name" to label, "qr" to 0.0, "manual" to 0.0, "total" to 0.0,
                    "manualRate" to 0.0, "href" to scan.href,
                )
            }
            val key = if (scan.source == "Manual") "manual" else "qr"
            current[key] = (current[key] as Double) + 1.0
            current["total"] = (current["total"] as Double) + 1.0
            current["manualRate"] = safeDivide(current["manual"] as Double, current["total"] as Double) * 100
            if ((current["href"] as String).isBlank()) current["href"] = scan.href
        }
        byHour.values
            .map { EventBiTableRow(it) }
            .sortedWith { left, right -> comparePtBr(left.text("name"), right.text("name")) }
    }

    /** `intervalRows` (5001): janelas de 15 minutos; `value` é a taxa por minuto. */
    private val intervalRows by lazy {
        val bucket = EventBiMetricBucket()
        gateScansSorted.forEach { scan ->
            bucket.add(
                eventBiMinuteBucketLabel(scan.scannedAtMillis, 15), 1.0, safeDivide(1.0, 15.0), 0.0, scan.href,
            )
        }
        bucket.all()
            .map { it.copy(value = safeDivide(it.quantity, 15.0)) }
            .sortedWith { left, right -> comparePtBr(left.name, right.name) }
    }

    private val peakInterval by lazy {
        intervalRows.fold(EventBiMetricRow(name = "-")) { current, row ->
            if (row.quantity > current.quantity) row else current
        }
    }

    private val scanDiffMinutes by lazy {
        gateScansSorted.zipWithNext { previous, scan ->
            (scan.scannedAtMillis - previous.scannedAtMillis) / MillisPerMinute
        }.finiteNonNegative()
    }

    private val averageMinutesBetweenScans get() = scanDiffMinutes.mean()
    private val longestIdleMinutes get() = eventBiMaxValue(scanDiffMinutes)

    /** `longestFastSequence` (5016): maior sequência com menos de 60s entre leituras. */
    private val longestFastSequence by lazy {
        var longest = 0
        var current = 0
        gateScansSorted.forEachIndexed { index, scan ->
            current = if (index == 0) {
                1
            } else {
                val diffSeconds =
                    (scan.scannedAtMillis - gateScansSorted[index - 1].scannedAtMillis) / 1000.0
                if (diffSeconds <= 60) current + 1 else 1
            }
            longest = max(longest, current)
        }
        longest
    }

    private val activeGateOperators by lazy {
        gateScans.map { it.operator }.filter { it != "Sem operador" }.distinct().size
    }

    /** `estimatedOperatorCapacityPer15` (5029): 4 leituras por minuto por operador, em 15 min. */
    private val estimatedOperatorCapacityPer15 get() = max(1, activeGateOperators) * 4 * 15
    private val queuePressure
        get() = safeDivide(peakInterval.quantity, estimatedOperatorCapacityPer15.toDouble()) * 100
    private val queueRisk
        get() = when {
            queuePressure >= 120 -> "Alto"
            queuePressure >= 75 -> "Médio"
            else -> "Baixo"
        }
    private val totalCapacity by lazy { dataset.events.sumOf { it.raw.eventCapacity() } }
    private val capacityRemaining
        get() = if (totalCapacity > 0) max(0, totalCapacity - ticketScanned) else 0
    private val occupancyRate get() = safeDivide(ticketScanned, totalCapacity) * 100
    private val manualityRate get() = safeDivide(manualScans, ticketScanned) * 100
    private val qrRate get() = safeDivide(appScans, ticketScanned) * 100

    private val portariaOperatorRows by lazy {
        operatorStats.entries
            .map { (operador, stats) ->
                tableRowOf(
                    "operador" to operador,
                    "entradas" to stats.total.toDouble(),
                    "qr" to stats.qr.toDouble(),
                    "manual" to stats.manual.toDouble(),
                    "invalidas" to stats.invalid.toDouble(),
                    "duplicadas" to stats.duplicate.toDouble(),
                    "erro" to safeDivide(
                        (stats.invalid + stats.duplicate).toDouble(),
                        (stats.total + stats.invalid + stats.duplicate).toDouble(),
                    ) * 100,
                    "manualidade" to safeDivide(stats.manual, stats.total) * 100,
                    "href" to stats.href,
                )
            }
            .sortedWith(
                compareByDescending<EventBiTableRow> { it.number("entradas") }
                    .thenByDescending { it.number("erro") },
            )
    }

    private val invalidReasonRows by lazy { invalidReasonGroups.rows(12) }

    private val portariaOperatorChartRows by lazy {
        portariaOperatorRows.take(12).map { row ->
            EventBiMetricRow(
                name = row.text("operador"),
                quantity = row.number("entradas"),
                value = row.number("manualidade"),
                href = row.href,
            )
        }
    }

    /** `operatorQualityRadarRows` (5057). */
    private val operatorQualityRadarRows by lazy {
        listOf(
            EventBiMetricRow("QR", appScans.toDouble(), qrRate),
            EventBiMetricRow("Manual", manualScans.toDouble(), max(0.0, 100 - manualityRate)),
            EventBiMetricRow(
                "Válidas", ticketScanned.toDouble(),
                max(
                    0.0,
                    100 - safeDivide(
                        (invalidScans + duplicateScans).toDouble(),
                        (ticketScanned + invalidScans + duplicateScans).toDouble(),
                    ) * 100,
                ),
            ),
            EventBiMetricRow(
                "Velocidade", peakInterval.quantity,
                min(100.0, safeDivide(peakInterval.quantity, estimatedOperatorCapacityPer15.toDouble()) * 100),
            ),
            EventBiMetricRow(
                "Presença", ticketScanned.toDouble(),
                safeDivide(ticketScanned, approvedTicketQuantity) * 100,
            ),
            EventBiMetricRow(
                "Rastreio", portariaOperatorRows.size.toDouble(),
                if (gateScans.isNotEmpty()) {
                    safeDivide(
                        gateScans.count { it.operator != "Sem operador" }.toDouble(),
                        gateScans.size.toDouble(),
                    ) * 100
                } else {
                    0.0
                },
            ),
        )
    }

    private val entryModeRows by lazy {
        listOf(
            EventBiMetricRow("QR (Quick Response)", appScans.toDouble(), 0.0),
            EventBiMetricRow("Manual", manualScans.toDouble(), 0.0),
        )
    }

    private val occupancyRows by lazy {
        if (totalCapacity > 0) {
            listOf(
                EventBiMetricRow("Ocupado", ticketScanned.toDouble(), ticketScanned.toDouble()),
                EventBiMetricRow("Restante", capacityRemaining.toDouble(), capacityRemaining.toDouble()),
            )
        } else {
            emptyList()
        }
    }

    /** `approvedWithoutReadRows` (5091): só as causas com quantidade maior que zero. */
    private val approvedWithoutReadRows by lazy {
        listOf(
            EventBiMetricRow("Não compareceu", noShow.toDouble(), 0.0),
            EventBiMetricRow(
                "Entrada manual",
                approvedTickets.count {
                    it.raw.ticketScannedCount() > 0 &&
                        normalizeEventBiText(it.raw.str("checkinMethod")).contains("manual")
                }.toDouble(),
                0.0,
            ),
            EventBiMetricRow(
                "Sem QR (Quick Response)",
                approvedTickets.count { !it.raw.ticketHasQrCode() }.toDouble(), 0.0,
            ),
            EventBiMetricRow(
                "Tentativa inválida",
                approvedTickets.count { ticket ->
                    ticket.raw.readTicketEntries().any {
                        it.ticketEntryInvalidReason(ticket.raw).isNotBlank()
                    }
                }.toDouble(),
                0.0,
            ),
            EventBiMetricRow(
                "Dados incompletos",
                approvedTickets.count {
                    it.raw.ticketHolderName().isBlank() || it.raw.ticketClassName().isBlank()
                }.toDouble(),
                0.0,
            ),
        ).filter { it.quantity > 0 }
    }

    /** `liveStatusRows` (5099). */
    private val liveStatusRows by lazy {
        dataset.events
            .map { event ->
                val targetEventId = event.id
                val eventScans = gateScans.filter { it.eventId == targetEventId }
                val eventApproved = approvedTickets
                    .filter { it.raw.ticketEventId() == targetEventId }
                    .sumOf { it.raw.ticketQuantity() }
                val eventPresent = eventScans.size
                val lastEntry = eventScans.maxByOrNull { it.scannedAtMillis }
                val eventInvalidRows = invalidReasonRows.filter { it.href.contains(targetEventId) }
                val elapsed = if (event.startsAtMillis > 0L) {
                    (now - event.startsAtMillis) / MillisPerHour
                } else {
                    0.0
                }
                tableRowOf(
                    "evento" to event.name,
                    "iniciado" to when {
                        event.startsAtMillis <= 0L -> "Sem data"
                        elapsed >= 0 -> "${formatEventBiDecimal(elapsed)}h"
                        else -> "Ainda não iniciou"
                    },
                    "ultimaEntrada" to formatEventBiDateTimeShort(lastEntry?.scannedAtMillis ?: 0L),
                    "ultimaInvalida" to (eventInvalidRows.firstOrNull()?.name ?: "-"),
                    "presentes" to eventPresent.toDouble(),
                    "ausentes" to max(0, eventApproved - eventPresent).toDouble(),
                    "entrada" to safeDivide(eventPresent, eventApproved) * 100,
                    "pico" to if (eventScans.isNotEmpty()) peakInterval.name else "-",
                    "href" to checkinsHref(targetEventId),
                )
            }
            .sortedByDescending { it.number("presentes") }
    }

    /** `portariaEventComparisonRows` (5125). */
    private val portariaEventComparisonRows by lazy {
        dataset.events
            .map { event ->
                val targetEventId = event.id
                val eventTickets = approvedTickets.filter { it.raw.ticketEventId() == targetEventId }
                val approved = eventTickets.sumOf { it.raw.ticketQuantity() }
                val present = eventTickets.sumOf { it.raw.ticketScannedCount() }
                val scans = gateScans.filter { it.eventId == targetEventId }
                val manual = scans.count { it.source == "Manual" }
                val peak = scans
                    .groupingBy { eventBiMinuteBucketLabel(it.scannedAtMillis, 15) }
                    .eachCount()
                    .maxByOrNull { it.value }
                tableRowOf(
                    "evento" to event.name,
                    "aprovados" to approved.toDouble(),
                    "presentes" to present.toDouble(),
                    "presenca" to safeDivide(present, approved) * 100,
                    "pico" to (peak?.let { "${it.key} (${it.value})" } ?: "-"),
                    "manualidade" to safeDivide(manual, present) * 100,
                    "invalidas" to eventTickets.sumOf { it.raw.ticketInvalidScanCount() }.toDouble(),
                    "href" to checkinsHref(targetEventId),
                )
            }
            .filter { it.number("aprovados") > 0 || it.number("presentes") > 0 }
            .sortedByDescending { it.number("presentes") }
    }

    private val portariaEventComparisonChartRows by lazy {
        portariaEventComparisonRows.take(12).map { row ->
            EventBiMetricRow(
                name = row.text("evento"),
                quantity = row.number("presentes"),
                value = row.number("manualidade"),
                href = row.href,
            )
        }
    }

    /** `portariaAlertRows` (5159): só entram os alertas com ocorrência. */
    @Suppress("LongMethod")
    private fun buildPortariaAlertRows(): List<EventBiTableRow> {
        val rows = mutableListOf<EventBiTableRow>()

        fun alert(alerta: String, descricao: String, impacto: String, quantidade: Int, href: String) {
            rows += tableRowOf(
                "alerta" to alerta,
                "descricao" to descricao,
                "impacto" to impacto,
                "quantidade" to quantidade.toDouble(),
                "href" to href,
            )
        }

        if (manualityRate > 30) {
            alert(
                "Entrada manual excessiva",
                "A portaria está registrando muita entrada sem leitura direta de QR (Quick Response).",
                "Alto", manualScans,
                if (singleOperationalEventId.isNotBlank()) {
                    eventHref(
                        singleOperationalEventId,
                        EventBiStatementLink(
                            type = EventBiRecordKind.Ticket,
                            status = EventBiStatementStatus.Approved,
                            flow = EventBiFlow.CheckIn,
                            indicator = "manual",
                        ),
                    )
                } else {
                    ""
                },
            )
        }
        if (usedWithoutApproval.isNotEmpty()) {
            alert(
                "Entrada sem aprovação",
                "Existe uso de ingresso sem aprovação clara no extrato.",
                "Alto", usedWithoutApproval.size,
                if (singleOperationalEventId.isNotBlank()) {
                    eventHref(
                        singleOperationalEventId,
                        EventBiStatementLink(
                            type = EventBiRecordKind.Ticket, alert = "uso-sem-aprovacao",
                        ),
                    )
                } else {
                    ""
                },
            )
        }
        if (duplicateContextRows.isNotEmpty()) {
            alert(
                "QR (Quick Response) duplicado",
                "Houve tentativa de reutilizar ingresso já lido.",
                "Alto", duplicateContextRows.size,
                duplicateContextRows.firstOrNull()?.href.orEmpty(),
            )
        }
        invalidReasonRows.forEach { row ->
            alert(
                row.name,
                "Motivo de leitura inválida identificado na auditoria da portaria.",
                if (row.name.contains("outro evento") || row.name.contains("cancelado")) "Alto" else "Médio",
                row.quantity.toInt(),
                row.href,
            )
        }
        val withoutQr = approvedTickets.count { !it.raw.ticketHasQrCode() }
        if (withoutQr > 0) {
            alert(
                "Ingresso aprovado sem QR (Quick Response)",
                "O ingresso foi aprovado, mas não tem código operacional para leitura.",
                "Médio", withoutQr,
                if (singleOperationalEventId.isNotBlank()) {
                    eventHref(
                        singleOperationalEventId,
                        EventBiStatementLink(
                            type = EventBiRecordKind.Ticket,
                            status = EventBiStatementStatus.Approved,
                            indicator = "sem-qr",
                        ),
                    )
                } else {
                    ""
                },
            )
        }
        val scansBeforeApproval = gateScans.count { scan ->
            val ticket = approvedTickets.firstOrNull { it.raw.str("id") == scan.orderId }
            val approvedAt = ticket?.raw?.ticketApprovalDate() ?: 0L
            approvedAt > 0L && scan.scannedAtMillis < approvedAt
        }
        if (scansBeforeApproval > 0) {
            alert(
                "Entrada registrada antes da aprovação",
                "A entrada aconteceu antes da aprovação do pedido.",
                "Alto", scansBeforeApproval,
                if (singleOperationalEventId.isNotBlank()) {
                    eventHref(
                        singleOperationalEventId,
                        EventBiStatementLink(
                            type = EventBiRecordKind.Ticket, indicator = "entrada-antes-aprovacao",
                        ),
                    )
                } else {
                    ""
                },
            )
        }
        val errorOperators = portariaOperatorRows.count {
            it.number("erro") >= 20 && it.number("invalidas") + it.number("duplicadas") >= 2
        }
        if (errorOperators > 0) {
            alert(
                "Muitas leituras inválidas pelo mesmo operador",
                "Um ou mais operadores concentram leituras inválidas ou duplicadas.",
                "Médio", errorOperators, portariaOperatorRows.firstOrNull()?.href.orEmpty(),
            )
        }
        val manualOperators = portariaOperatorRows.count {
            it.number("manualidade") >= 60 && it.number("manual") >= 3
        }
        if (manualOperators > 0) {
            alert(
                "Muitas entradas manuais pelo mesmo operador",
                "Um ou mais operadores usaram entrada manual em excesso.",
                "Médio", manualOperators, portariaOperatorRows.firstOrNull()?.href.orEmpty(),
            )
        }
        val startedWithAbsent = dataset.events.any {
            it.startsAtMillis > 0L && now > it.startsAtMillis && absentRows.isNotEmpty()
        }
        if (startedWithAbsent) {
            alert(
                "Aprovado sem entrada após início do evento",
                "Há ingressos aprovados que ainda não viraram presença depois do início.",
                "Baixo", absentRows.size,
                if (singleOperationalEventId.isNotBlank()) {
                    checkinsHref(singleOperationalEventId, EventBiCheckinsLink(indicator = "ausente"))
                } else {
                    ""
                },
            )
        }
        return rows
    }

    // ------------------------------------------------------------------
    // Recorrência e projeção (5252-5265)
    // ------------------------------------------------------------------

    private val recurringBuyers get() = buyerPurchases.values.count { it > 1 }

    private val recurrenceRows by lazy {
        listOf(
            EventBiMetricRow("Novos", max(0, buyerPurchases.size - recurringBuyers).toDouble(), 0.0),
            EventBiMetricRow("Recorrentes", recurringBuyers.toDouble(), 0.0),
        )
    }

    private val projectedRevenue by lazy {
        val firstPurchase = (
            approvedTickets.map { it.raw.ticketPurchaseDate() } +
                approvedOrders.map { it.raw.orderCreatedAt() }
            ).filter { it > 0L }.minOrNull() ?: 0L
        val selectedSingleEvent = if (filter.hasEventFilter) dataset.events.firstOrNull() else null
        val selectedEventDate = selectedSingleEvent?.startsAtMillis ?: 0L
        val daysElapsed = if (firstPurchase > 0L) max(1.0, (now - firstPurchase) / MillisPerDay) else 0.0
        val daysRemaining = if (selectedEventDate > 0L) {
            max(0.0, (selectedEventDate - now) / MillisPerDay)
        } else {
            0.0
        }
        if (selectedSingleEvent != null && daysElapsed > 0 && daysRemaining > 0) {
            grossRevenue + safeDivide(grossRevenue, daysElapsed) * daysRemaining
        } else {
            0.0
        }
    }

    // ------------------------------------------------------------------
    // Aprovadores e retirada (5267-5319)
    // ------------------------------------------------------------------

    private val approvalRows by lazy { byApprover.rows(12) }
    private val ticketApprovalRows by lazy { byTicketApprover.rows(12) }
    private val withdrawalDurations by lazy {
        approvedOrders
            .map { hoursBetween(it.raw.orderApprovalDate(), it.raw.orderWithdrawalDate()) }
            .finiteNonNegative()
    }

    private fun productAuditHref(options: EventBiStatementLink) =
        if (singleOperationalEventId.isNotBlank()) {
            eventHref(singleOperationalEventId, options.copy(type = EventBiRecordKind.Product))
        } else {
            ""
        }

    /** `auditRows` (5291): só linhas com quantidade maior que zero. */
    private val auditRows by lazy {
        listOf(
            tableRowOf(
                "alerta" to "Aprovado sem retirada",
                "quantidade" to approvedOrders.count { it.raw.orderRedeemedQuantity() == 0 }.toDouble(),
                "href" to productAuditHref(
                    EventBiStatementLink(
                        status = EventBiStatementStatus.Approved, indicator = "pendente-retirada",
                    ),
                ),
            ),
            tableRowOf(
                "alerta" to "Retirado sem aprovação",
                "quantidade" to dataset.orders.count {
                    it.classified() != EventBiStatus.Approved && it.raw.orderRedeemedQuantity() > 0
                }.toDouble(),
                "href" to productAuditHref(EventBiStatementLink(alert = "uso-sem-aprovacao")),
            ),
            tableRowOf(
                "alerta" to "Baixa manual",
                "quantidade" to approvedOrders.count { it.raw.orderWithdrawalMethod() == "Manual" }.toDouble(),
                "href" to productAuditHref(
                    EventBiStatementLink(
                        status = EventBiStatementStatus.Approved, indicator = "retirada-manual",
                    ),
                ),
            ),
            tableRowOf(
                "alerta" to "Pedido criado manualmente",
                "quantidade" to dataset.orders.count { it.raw.isManualOrder() }.toDouble(),
                "href" to productAuditHref(EventBiStatementLink(indicator = "pedido-manual")),
            ),
            tableRowOf(
                "alerta" to "Desconto 100%",
                "quantidade" to approvedOrders.count {
                    it.raw.orderDiscount() >= it.raw.orderTotal() && it.raw.orderTotal() > 0
                }.toDouble(),
                "href" to productAuditHref(
                    EventBiStatementLink(status = EventBiStatementStatus.Approved, search = "desconto"),
                ),
            ),
            tableRowOf(
                "alerta" to "Valor zerado",
                "quantidade" to approvedOrders.count { it.raw.orderTotal() <= 0 }.toDouble(),
                "href" to productAuditHref(
                    EventBiStatementLink(status = EventBiStatementStatus.Approved, indicator = "sem-valor"),
                ),
            ),
            tableRowOf(
                "alerta" to "Pedido cancelado após aprovação",
                "quantidade" to cancelledOrders.size.toDouble(),
                "href" to productAuditHref(EventBiStatementLink(indicator = "cancelado-pos-aprovacao")),
            ),
            tableRowOf(
                "alerta" to "Produto sem estoque",
                "quantidade" to dataset.products.count { it.stock <= 0 }.toDouble(),
            ),
            tableRowOf(
                "alerta" to "Pedido aprovado por quem criou",
                "quantidade" to approvedOrders.count {
                    it.raw.isManualOrder() && it.raw.orderApproverName() == it.raw.str("eventCreatedByName")
                }.toDouble(),
                "href" to productAuditHref(
                    EventBiStatementLink(
                        status = EventBiStatementStatus.Approved, indicator = "mesmo-criador",
                    ),
                ),
            ),
            tableRowOf(
                "alerta" to "Pedido baixado por quem aprovou",
                "quantidade" to approvedOrders.count {
                    it.raw.orderWithdrawalOperator() != "-" &&
                        it.raw.orderWithdrawalOperator() == it.raw.orderApproverName()
                }.toDouble(),
                "href" to productAuditHref(
                    EventBiStatementLink(status = EventBiStatementStatus.Approved, indicator = "mesmo-baixa"),
                ),
            ),
        ).filter { it.number("quantidade") > 0 }
    }

    private val operationalAlerts by lazy {
        listOf(
            tableRowOf(
                "alerta" to "Pedido aprovado sem valor",
                "quantidade" to (
                    approvedTickets.count { it.raw.ticketValue() <= 0 } +
                        approvedOrders.count { it.raw.orderTotal() <= 0 }
                    ).toDouble(),
            ),
            tableRowOf(
                "alerta" to "Pedido com desconto 100%",
                "quantidade" to approvedOrders.count {
                    it.raw.orderDiscount() >= it.raw.orderTotal() && it.raw.orderTotal() > 0
                }.toDouble(),
            ),
            tableRowOf(
                "alerta" to "Pedido manual criado por administrador",
                "quantidade" to dataset.orders.count { it.raw.isManualOrder() }.toDouble(),
            ),
            tableRowOf(
                "alerta" to "Pedido sem comprovante",
                "quantidade" to (
                    dataset.tickets.count { it.raw.obj("payment_config") == null } +
                        dataset.orders.count {
                            it.raw.obj("payment_config") == null && !it.raw.isManualOrder()
                        }
                    ).toDouble(),
            ),
            tableRowOf("alerta" to "Pedido pendente há mais de 24h", "quantidade" to pendingMore24.toDouble()),
            tableRowOf(
                "alerta" to "Pedido com retirada manual",
                "quantidade" to approvedOrders.count { it.raw.orderWithdrawalMethod() == "Manual" }.toDouble(),
            ),
            tableRowOf(
                "alerta" to "Pedido com divergência de valor",
                "quantidade" to (
                    approvedTickets.count { it.raw.ticketValue() < 0 } +
                        approvedOrders.count { it.raw.orderTotal() < 0 }
                    ).toDouble(),
            ),
        ).filter { it.number("quantidade") > 0 }
    }

    private val operationalTicketAlerts by lazy {
        listOf(
            tableRowOf(
                "alerta" to "Ingresso aprovado sem valor",
                "quantidade" to approvedTickets.count { it.raw.ticketValue() <= 0 }.toDouble(),
            ),
            tableRowOf(
                "alerta" to "Ingresso sem comprovante",
                "quantidade" to dataset.tickets.count { it.raw.obj("payment_config") == null }.toDouble(),
            ),
            tableRowOf(
                "alerta" to "Ingresso pendente há mais de 24h",
                "quantidade" to ticketPendingMore24.toDouble(),
            ),
            tableRowOf(
                "alerta" to "Ingresso com divergência de valor",
                "quantidade" to dataset.tickets.count { it.raw.ticketValue() < 0 }.toDouble(),
            ),
        ).filter { it.number("quantidade") > 0 }
    }

    /** `eventDecisionRows` (5321). */
    private val eventDecisionRows by lazy {
        eventSummary.rows(20).map { row ->
            val presenceRate = safeDivide(row.secondary, row.quantity) * 100
            tableRowOf(
                "evento" to row.name,
                "ingressos" to row.quantity,
                "receita" to row.value,
                "ticket" to safeDivide(row.value, row.quantity),
                "presença" to presenceRate,
                "decisão" to when {
                    row.value > 0 && presenceRate >= 70 -> "Repetir"
                    row.value > 0 && presenceRate >= 40 -> "Ajustar divulgação"
                    row.value > 0 -> "Ajustar formato"
                    else -> "Revisar proposta"
                },
            )
        }
    }

    private val productChartRows by lazy {
        productRows.values.sortedByDescending { it.value }.take(12)
    }

    private val productTableRows by lazy {
        productRows.values
            .sortedWith(
                compareByDescending<EventBiProductMetricRow> { it.value }
                    .thenByDescending { it.quantity },
            )
            .take(20)
            .map { row ->
                tableRowOf(
                    "produto" to row.name,
                    "itens" to row.quantity,
                    "receita" to row.value,
                    "ticket" to safeDivide(row.value, row.quantity),
                    "retirados" to row.redeemed,
                    "pendentes" to row.pending,
                )
            }
    }

    /** `singleEventIdForLinks` (5354). */
    private val singleEventIdForLinks get() = singleOperationalEventId

    // ------------------------------------------------------------------
    // Cruzamento ingresso x produto (5360-5384)
    // ------------------------------------------------------------------

    private val ticketBuyerIds by lazy {
        approvedTickets.map { it.raw.ticketBuyerId() }.filter { it.isNotBlank() }.toSet()
    }
    private val productBuyerIds by lazy {
        approvedOrders.map { it.raw.orderBuyerId() }.filter { it.isNotBlank() }.toSet()
    }
    private val productRedeemedBuyerIds by lazy {
        approvedOrders
            .filter { it.raw.orderRedeemedQuantity() > 0 }
            .map { it.raw.orderBuyerId() }
            .filter { it.isNotBlank() }
            .toSet()
    }
    private val checkedInProductBuyerIds by lazy {
        checkedInTicketBuyerIds.filter { it in productBuyerIds }.toSet()
    }
    private val checkedInProductRedeemedBuyerIds by lazy {
        checkedInProductBuyerIds.filter { it in productRedeemedBuyerIds }.toSet()
    }
    private val buyersWithTicketAndProduct by lazy { ticketBuyerIds.count { it in productBuyerIds } }
    private val ticketWithoutProduct
        get() = max(0, checkedInTicketBuyerIds.size - checkedInProductBuyerIds.size)
    private val productWithoutTicket by lazy { productBuyerIds.count { it !in ticketBuyerIds } }

    private val eventCostsTotal by lazy { dataset.events.sumOf { it.raw.eventCost() } }
    private val hasEventCostsField by lazy { dataset.events.any { it.raw.hasEventCostField() } }

    // ------------------------------------------------------------------
    // Recorrência histórica (5386-5472)
    // ------------------------------------------------------------------

    /** `isPreviousEvent` (5395): evento anterior ao atual, pela data de início. */
    private fun isPreviousEvent(previousEventId: String, currentEventId: String): Boolean {
        if (previousEventId.isBlank() || currentEventId.isBlank() || previousEventId == currentEventId) {
            return false
        }
        val previousAt = eventStartOf(previousEventId)
        val currentAt = eventStartOf(currentEventId)
        return previousAt > 0L && currentAt > 0L && previousAt < currentAt
    }

    private fun MutableMap<String, MutableSet<String>>.addMarker(buyerId: String, eventId: String) {
        val cleanBuyerId = buyerId.trim()
        val cleanEventId = eventId.trim()
        if (cleanBuyerId.isBlank() || cleanEventId.isBlank()) return
        getOrPut(cleanBuyerId) { linkedSetOf() } += cleanEventId
    }

    private fun Map<String, Set<String>>.hasHistoricalEvent(buyerId: String, currentEventId: String) =
        this[buyerId].orEmpty().any { isPreviousEvent(it, currentEventId) }

    private val historicalTicketCheckinsByBuyer = mutableMapOf<String, MutableSet<String>>()
    private val historicalProductPurchasesByBuyer = mutableMapOf<String, MutableSet<String>>()
    private val currentTicketCheckinsByBuyer = mutableMapOf<String, MutableSet<String>>()
    private val currentProductPurchasesByBuyer = mutableMapOf<String, MutableSet<String>>()

    /**
     * `data.tickets`/`data.orders` (5418-5441). No web é o tenant inteiro filtrado por
     * `matchesActiveScope`; aqui `scopeTickets`/`scopeOrders` já vêm do escopo, sem filtro de
     * evento nem de período — que é exatamente o recorte que essa recorrência precisa.
     */
    private fun buildRecurrence() {
        dataset.scopeTickets.forEach { ticket ->
            val targetEventId = ticket.raw.ticketEventId()
            if (targetEventId.isBlank() || targetEventId !in eventById) return@forEach
            if (
                EventBiStatus.classify(ticket.raw.statusValue()) != EventBiStatus.Approved ||
                ticket.raw.ticketScannedCount() <= 0
            ) {
                return@forEach
            }
            historicalTicketCheckinsByBuyer.addMarker(ticket.raw.ticketBuyerId(), targetEventId)
        }
        dataset.scopeOrders.forEach { order ->
            val targetEventId = order.resolvedEventId()
            if (targetEventId.isBlank() || targetEventId !in eventById) return@forEach
            if (EventBiStatus.classify(order.raw.statusValue()) != EventBiStatus.Approved) return@forEach
            historicalProductPurchasesByBuyer.addMarker(order.raw.orderBuyerId(), targetEventId)
        }
        approvedTickets.forEach { ticket ->
            if (ticket.raw.ticketScannedCount() <= 0) return@forEach
            currentTicketCheckinsByBuyer.addMarker(ticket.raw.ticketBuyerId(), ticket.raw.ticketEventId())
        }
        approvedOrders.forEach { order ->
            currentProductPurchasesByBuyer.addMarker(order.raw.orderBuyerId(), order.resolvedEventId())
        }
    }

    /** `classifyHistoricalRecurrence` (5403). */
    private fun classifyHistoricalRecurrence(
        currentMap: Map<String, Set<String>>,
        historicalMap: Map<String, Set<String>>,
    ): Pair<Int, Int> {
        var novos = 0
        var recorrentes = 0
        currentMap.forEach { (buyerId, markers) ->
            if (markers.any { historicalMap.hasHistoricalEvent(buyerId, it) }) recorrentes += 1 else novos += 1
        }
        return novos to recorrentes
    }

    private val recurrenceDetailRows by lazy {
        val (ticketNovos, ticketRecorrentes) =
            classifyHistoricalRecurrence(currentTicketCheckinsByBuyer, historicalTicketCheckinsByBuyer)
        val (productNovos, productRecorrentes) =
            classifyHistoricalRecurrence(currentProductPurchasesByBuyer, historicalProductPurchasesByBuyer)
        listOf(
            EventBiMetricRow("Novo em ingresso", ticketNovos.toDouble(), 0.0),
            EventBiMetricRow("Recorrente em ingresso", ticketRecorrentes.toDouble(), 0.0),
            EventBiMetricRow("Novo em produto", productNovos.toDouble(), 0.0),
            EventBiMetricRow("Recorrente em produto", productRecorrentes.toDouble(), 0.0),
        )
    }

    private val strategicRecurringBuyerIds by lazy {
        val ids = linkedSetOf<String>()
        currentTicketCheckinsByBuyer.forEach { (buyerId, markers) ->
            if (markers.any { historicalTicketCheckinsByBuyer.hasHistoricalEvent(buyerId, it) }) {
                ids += buyerId
            }
        }
        currentProductPurchasesByBuyer.forEach { (buyerId, markers) ->
            if (markers.any { historicalProductPurchasesByBuyer.hasHistoricalEvent(buyerId, it) }) {
                ids += buyerId
            }
        }
        ids
    }
    private val strategicRecurrenceBase by lazy {
        (currentTicketCheckinsByBuyer.keys + currentProductPurchasesByBuyer.keys).size
    }
    private val strategicRecurringRate
        get() = safeDivide(strategicRecurringBuyerIds.size, strategicRecurrenceBase) * 100

    private val tenantParticipationRate
        get() = safeDivide(checkedInTicketBuyerIds.size, dataset.tenantUserCount) * 100

    private val tenantParticipationRows by lazy {
        listOf(
            EventBiMetricRow("Participaram", checkedInTicketBuyerIds.size.toDouble(), tenantParticipationRate),
            EventBiMetricRow(
                "Não participaram",
                max(0, dataset.tenantUserCount - checkedInTicketBuyerIds.size).toDouble(),
                max(0.0, 100 - tenantParticipationRate),
            ),
        )
    }

    // ------------------------------------------------------------------
    // Score estratégico por evento (5474-5668)
    // ------------------------------------------------------------------

    private class StrategicEvent(
        val eventId: String,
        val evento: String,
        val href: String,
        var ticketQty: Int = 0,
        var present: Int = 0,
        var ticketRevenue: Double = 0.0,
        var productRevenue: Double = 0.0,
        var productQty: Int = 0,
        var productRedeemed: Int = 0,
        var productPending: Int = 0,
        val ticketBuyers: MutableSet<String> = linkedSetOf(),
        val checkedInTicketBuyers: MutableSet<String> = linkedSetOf(),
        val productBuyers: MutableSet<String> = linkedSetOf(),
        val productRedeemedBuyers: MutableSet<String> = linkedSetOf(),
        val buyers: MutableSet<String> = linkedSetOf(),
        val recurringBuyers: MutableSet<String> = linkedSetOf(),
        var manualCount: Int = 0,
        var operationalCount: Int = 0,
        val cost: Double = 0.0,
    )

    private val eventStrategicMap = LinkedHashMap<String, StrategicEvent>()

    private fun ensureStrategicEvent(eventId: String): StrategicEvent {
        val cleanEventId = eventId.trim()
        val relatedEvent = eventById[cleanEventId]
        return eventStrategicMap.getOrPut(cleanEventId) {
            StrategicEvent(
                eventId = cleanEventId,
                evento = relatedEvent?.name ?: cleanEventId.ifBlank { "Evento" },
                href = if (cleanEventId.isNotBlank()) {
                    eventHref(cleanEventId, EventBiStatementLink(status = EventBiStatementStatus.Approved))
                } else {
                    ""
                },
                cost = relatedEvent?.raw.eventCost(),
            )
        }
    }

    private fun buildStrategicEvents() {
        dataset.events.forEach { ensureStrategicEvent(it.id) }
        approvedTickets.forEach { ticket ->
            val targetEventId = ticket.raw.ticketEventId()
            val row = ensureStrategicEvent(targetEventId)
            val buyerId = ticket.raw.ticketBuyerId()
            val scanned = ticket.raw.ticketScannedCount()
            row.ticketQty += ticket.raw.ticketQuantity()
            row.present += scanned
            row.ticketRevenue += ticket.raw.ticketValue()
            row.ticketBuyers += buyerId
            if (scanned > 0) {
                row.checkedInTicketBuyers += buyerId
                if (historicalTicketCheckinsByBuyer.hasHistoricalEvent(buyerId, targetEventId)) {
                    row.recurringBuyers += buyerId
                }
            }
            row.buyers += buyerId
        }
        approvedOrders.forEach { order ->
            val targetEventId = order.resolvedEventId()
            if (targetEventId.isBlank()) return@forEach
            val row = ensureStrategicEvent(targetEventId)
            val buyerId = order.raw.orderBuyerId()
            val quantity = order.raw.orderQuantity()
            val redeemed = order.raw.orderRedeemedQuantity()
            row.productQty += quantity
            row.productRedeemed += redeemed
            row.productPending += max(0, quantity - redeemed)
            row.productRevenue += order.raw.orderTotal()
            row.productBuyers += buyerId
            if (redeemed > 0) row.productRedeemedBuyers += buyerId
            row.buyers += buyerId
            if (historicalProductPurchasesByBuyer.hasHistoricalEvent(buyerId, targetEventId)) {
                row.recurringBuyers += buyerId
            }
        }
        operationalRecords.forEach { record ->
            if (record.eventId.isBlank()) return@forEach
            val row = ensureStrategicEvent(record.eventId)
            row.operationalCount += 1
            if (
                record.manual ||
                normalizeEventBiText("${record.source} ${record.approvalMethod} ${record.completionMethod}")
                    .contains("manual")
            ) {
                row.manualCount += 1
            }
        }
    }

    private val strategicEventBaseRows by lazy {
        eventStrategicMap.values.filter {
            it.ticketQty > 0 || it.productQty > 0 || it.ticketRevenue > 0 || it.productRevenue > 0
        }
    }

    /** `strategicEventRows` (5574): a nota é a soma ponderada de oito fatores. */
    private val strategicEventRows by lazy {
        val maxEventRevenue = eventBiMaxValue(
            strategicEventBaseRows.map { it.ticketRevenue + it.productRevenue },
        )
        val maxEventPresence = eventBiMaxValue(strategicEventBaseRows.map { it.present.toDouble() })
        val maxProductPerPresent = eventBiMaxValue(
            strategicEventBaseRows.map { safeDivide(it.productRevenue, it.present.toDouble()) },
        )

        strategicEventBaseRows
            .map { row ->
                val totalRevenue = row.ticketRevenue + row.productRevenue
                val presenceRate = safeDivide(row.present, row.ticketQty) * 100
                val productConversion = safeDivide(
                    row.checkedInTicketBuyers.count { it in row.productBuyers }.toDouble(),
                    row.checkedInTicketBuyers.size.toDouble(),
                ) * 100
                val productPerPerson = safeDivide(row.productRevenue, row.present.toDouble())
                val withdrawalRateByEvent = safeDivide(row.productRedeemed, row.productQty) * 100
                val pendingRate = safeDivide(row.productPending, row.productQty) * 100
                val manualRate = safeDivide(row.manualCount, row.operationalCount) * 100
                val recurringRateByEvent = safeDivide(row.recurringBuyers.size, row.buyers.size) * 100
                val score = (
                    scoreFromRatio(totalRevenue, maxEventRevenue) * 0.2 +
                        scoreFromRatio(row.present.toDouble(), maxEventPresence) * 0.15 +
                        eventBiClamp(presenceRate) * 0.15 +
                        scoreFromRatio(productPerPerson, maxProductPerPresent) * 0.15 +
                        eventBiClamp(productConversion) * 0.15 +
                        eventBiClamp(recurringRateByEvent) * 0.08 +
                        scoreFromInverseRate(manualRate) * 0.06 +
                        scoreFromInverseRate(pendingRate) * 0.06
                    ).roundToInt()
                val decision = when {
                    score >= 85 -> "Repetir e escalar"
                    score >= 70 -> "Repetir"
                    presenceRate < 45 && totalRevenue > 0 -> "Ajustar divulgação"
                    productConversion < 25 && row.present > 0 -> "Ajustar produtos"
                    pendingRate > 30 -> "Ajustar portaria"
                    score < 40 -> "Evitar repetir"
                    else -> "Ajustar formato"
                }
                val reason = when {
                    productConversion < 25 && row.present > 0 -> "Presença não virou consumo"
                    pendingRate > 30 -> "Muita retirada pendente"
                    presenceRate < 45 -> "No-show alto"
                    productPerPerson > safeDivide(maxProductPerPresent, 2.0) -> "Consumo interno forte"
                    else -> "Equilíbrio geral"
                }
                tableRowOf(
                    "evento" to row.evento,
                    "ingressos" to row.ticketQty.toDouble(),
                    "checkins" to row.present.toDouble(),
                    "presentes" to row.present.toDouble(),
                    "presenca" to presenceRate,
                    "receitaIngressos" to row.ticketRevenue,
                    "receitaProdutos" to row.productRevenue,
                    "receitaTotal" to totalRevenue,
                    "produtoPorPresente" to productPerPerson,
                    "ticketTotalCliente" to safeDivide(totalRevenue, row.buyers.size.toDouble()),
                    "retirada" to withdrawalRateByEvent,
                    "pendencias" to row.productPending.toDouble(),
                    "score" to score.toDouble(),
                    "decisao" to decision,
                    "motivo" to reason,
                    "href" to row.href,
                    "hrefProdutos" to eventHref(
                        row.eventId,
                        EventBiStatementLink(
                            type = EventBiRecordKind.Product, status = EventBiStatementStatus.Approved,
                        ),
                    ),
                )
            }
            .sortedWith(
                compareByDescending<EventBiTableRow> { it.number("score") }
                    .thenByDescending { it.number("receitaTotal") },
            )
    }

    private val strategicScore by lazy {
        val hasBasis = strategicEventRows.isNotEmpty() &&
            (approvedTicketQuantity > 0 || approvedProductQuantity > 0 || grossRevenue > 0)
        if (!hasBasis) {
            null
        } else {
            safeDivide(
                strategicEventRows.sumOf { it.number("score") },
                strategicEventRows.size.toDouble(),
            ).roundToInt()
        }
    }

    private val strategicDecision
        get() = when (val score = strategicScore) {
            null -> "Sem dados suficientes"
            else -> when {
                score >= 85 -> "Repetir e escalar"
                score >= 70 -> "Repetir"
                score >= 40 -> "Ajustar"
                else -> "Repensar"
            }
        }

    private val strategicRadarRows by lazy {
        listOf(
            EventBiMetricRow(
                "Ingresso", approvedTicketQuantity.toDouble(),
                scoreFromRatio(ticketRevenue, max(grossRevenue, 1.0)),
            ),
            EventBiMetricRow(
                "Check-in", ticketScanned.toDouble(),
                eventBiClamp(safeDivide(ticketScanned, approvedTicketQuantity) * 100),
            ),
            EventBiMetricRow(
                "Produtos", approvedProductQuantity.toDouble(),
                scoreFromRatio(productRevenue, max(grossRevenue, 1.0)),
            ),
            EventBiMetricRow(
                "Recorrência", strategicRecurringBuyerIds.size.toDouble(),
                eventBiClamp(strategicRecurringRate),
            ),
            EventBiMetricRow(
                "Operação", approvedOperationalRecords.size.toDouble(),
                scoreFromInverseRate(
                    safeDivide(
                        operationalAlertRows.size.toDouble(),
                        max(approvedOperationalRecords.size, 1).toDouble(),
                    ) * 100,
                ),
            ),
            EventBiMetricRow(
                "Auditoria", operationalAlertRows.size.toDouble(),
                scoreFromInverseRate(
                    (duplicateScans + invalidScans + operationalAlertRows.size).toDouble() * 10, 100.0,
                ),
            ),
        )
    }

    private val strategicBubbleRows by lazy {
        strategicEventRows.map { row ->
            EventBiBubbleEntry(
                name = row.text("evento"),
                x = row.number("presenca"),
                y = row.number("receitaTotal"),
                z = max(1.0, row.number("receitaProdutos")),
                value = row.number("score"),
                decision = row.text("decisao"),
                href = row.href,
            )
        }
    }

    private fun ticketLink(search: String = "") = EventBiStatementLink(
        type = EventBiRecordKind.Ticket, status = EventBiStatementStatus.Approved, search = search,
    )

    private fun productLink(search: String = "") = EventBiStatementLink(
        type = EventBiRecordKind.Product, status = EventBiStatementStatus.Approved, search = search,
    )

    private val revenueOriginRows by lazy {
        listOf(
            EventBiMetricRow(
                "Ingressos", ticketRevenue, ticketRevenue,
                href = if (singleEventIdForLinks.isNotBlank()) {
                    eventHref(singleEventIdForLinks, ticketLink())
                } else {
                    ""
                },
            ),
            EventBiMetricRow(
                "Produtos (Modo Vendas)", productRevenue, productRevenue,
                href = if (singleEventIdForLinks.isNotBlank()) {
                    eventHref(singleEventIdForLinks, productLink())
                } else {
                    ""
                },
            ),
        )
    }

    /** `revenueDetailRows` (5683): os seis maiores lotes e os seis maiores produtos. */
    private val revenueDetailRows by lazy {
        (
            byLot.rows(6).map { it.copy(name = "Lote: ${it.name}", quantity = it.value) } +
                productChartRows.take(6).map {
                    EventBiMetricRow(name = "Produto: ${it.name}", quantity = it.value, value = it.value)
                }
            )
            .sortedByDescending { it.quantity }
            .take(12)
    }

    private val revenuePerPresentRows by lazy {
        strategicEventRows.map { row ->
            EventBiMetricRow(
                name = row.text("evento"),
                quantity = row.number("presentes"),
                value = if (row.number("receitaTotal") != 0.0) {
                    safeDivide(row.number("receitaTotal"), row.number("presentes"))
                } else {
                    0.0
                },
                secondary = safeDivide(row.number("receitaProdutos"), row.number("presentes")),
                href = row.href,
            )
        }
    }

    private val strategicFunnelRows by lazy {
        listOf(
            EventBiMetricRow(
                "Usuários com ingresso aprovado", ticketBuyerIds.size.toDouble(), 0.0,
                href = if (singleEventIdForLinks.isNotBlank()) eventHref(singleEventIdForLinks, ticketLink()) else "",
            ),
            EventBiMetricRow(
                "Desses, usuários com check-in", checkedInTicketBuyerIds.size.toDouble(), 0.0,
                href = if (singleEventIdForLinks.isNotBlank()) checkinsHref(singleEventIdForLinks) else "",
            ),
            EventBiMetricRow(
                "Desses, compraram produto", checkedInProductBuyerIds.size.toDouble(), 0.0,
                href = if (singleEventIdForLinks.isNotBlank()) eventHref(singleEventIdForLinks, productLink()) else "",
            ),
            EventBiMetricRow(
                "Desses, retiraram produto", checkedInProductRedeemedBuyerIds.size.toDouble(), 0.0,
                href = if (singleEventIdForLinks.isNotBlank()) {
                    eventHref(singleEventIdForLinks, productLink().copy(flow = EventBiFlow.Withdrawal))
                } else {
                    ""
                },
            ),
        )
    }

    /** `attachRateRows` (5700): receita e itens de produto por presente. */
    private val attachRateRows by lazy {
        productRows.values
            .map { row ->
                EventBiMetricRow(
                    name = row.name,
                    quantity = row.quantity,
                    value = safeDivide(row.value, max(ticketScanned, 1).toDouble()),
                    secondary = safeDivide(row.quantity, max(ticketScanned, 1).toDouble()),
                    href = if (singleEventIdForLinks.isNotBlank()) {
                        eventHref(singleEventIdForLinks, productLink(row.name))
                    } else {
                        ""
                    },
                )
            }
            .sortedWith(
                compareByDescending<EventBiMetricRow> { it.quantity }.thenByDescending { it.value },
            )
    }

    // ------------------------------------------------------------------
    // Heatmaps e composição por categoria (5709-5755)
    // ------------------------------------------------------------------

    private val eventProductHeatmapRows = mutableListOf<EventBiHeatmapEntry>()
    private val eventCategoryHeatmapRows = mutableListOf<EventBiHeatmapEntry>()
    private val categoryCompositionRows = LinkedHashMap<String, MutableMap<String, Any?>>()

    /** `addComposition` (5712): a categoria cai em ingresso/ficha/bar/cortesia/produto/outros. */
    private fun addComposition(eventLabel: String, key: String, value: Double, href: String) {
        val current = categoryCompositionRows.getOrPut(eventLabel) {
            linkedMapOf(
                "name" to eventLabel, "ingresso" to 0.0, "produto" to 0.0, "ficha" to 0.0,
                "bar" to 0.0, "cortesia" to 0.0, "outros" to 0.0, "href" to href,
            )
        }
        val normalized = normalizeEventBiText(key)
        val field = when {
            normalized.contains("ingresso") -> "ingresso"
            normalized.contains("ficha") -> "ficha"
            normalized.contains("bar") || normalized.contains("drink") ||
                normalized.contains("bebida") -> "bar"
            normalized.contains("cortesia") -> "cortesia"
            normalized.contains("produto") -> "produto"
            else -> "outros"
        }
        current[field] = (current[field] as Double) + value
    }

    private fun buildComposition() {
        approvedTickets.forEach { ticket ->
            val targetEventId = ticket.raw.ticketEventId()
            addComposition(
                eventLabelOf(targetEventId, ticket.raw.str("eventoNome")),
                "Ingresso",
                ticket.raw.ticketValue(),
                eventHref(targetEventId, ticketLink()),
            )
        }
        approvedOrders.forEach { order ->
            val targetEventId = order.resolvedEventId()
            val eventLabel = eventLabelOf(targetEventId, order.raw.str("eventoNome"))
            val product = productsById[order.raw.orderProductId()]?.raw
            val productLabel = order.raw.orderItemName(product)
            val category = order.raw.orderItemCategory(product)
            val total = order.raw.orderTotal()
            eventProductHeatmapRows += EventBiHeatmapEntry(
                row = eventLabel, column = productLabel, value = total,
                href = if (targetEventId.isNotBlank()) {
                    eventHref(targetEventId, productLink(productLabel))
                } else {
                    ""
                },
            )
            eventCategoryHeatmapRows += EventBiHeatmapEntry(
                row = eventLabel, column = category, value = total,
                href = if (targetEventId.isNotBlank()) {
                    eventHref(targetEventId, productLink(category))
                } else {
                    ""
                },
            )
            addComposition(
                eventLabel, category, total,
                if (targetEventId.isNotBlank()) eventHref(targetEventId, productLink()) else "",
            )
        }
    }

    private val categoryCompositionChartRows by lazy {
        categoryCompositionRows.values.take(12).map { EventBiTableRow(it) }
    }

    // ------------------------------------------------------------------
    // Antecedência por tipo (5757-5795)
    // ------------------------------------------------------------------

    private val leadBucketNames = listOf(
        "30 dias ou mais", "15 a 29 dias", "7 a 14 dias", "3 a 6 dias", "24 a 72h",
        "Menos de 24h", "Sem data",
    )

    private fun createLeadBucket() = EventBiMetricBucket().apply {
        leadBucketNames.forEachIndexed { index, name -> seed(name, index.toDouble()) }
    }

    private val ticketLeadBucket = createLeadBucket()
    private val productLeadBucket = createLeadBucket()

    private fun buildLeadRows() {
        approvedTickets.forEach { ticket ->
            val targetEventId = ticket.raw.ticketEventId()
            ticketLeadBucket.addToSeeded(
                eventBiLeadBucketLabel(ticket.raw.ticketPurchaseDate(), eventStartOf(targetEventId)),
                ticket.raw.ticketQuantity().toDouble(),
                ticket.raw.ticketValue(),
                eventHref(targetEventId, ticketLink()),
            )
        }
        approvedOrders.forEach { order ->
            val targetEventId = order.resolvedEventId()
            productLeadBucket.addToSeeded(
                eventBiLeadBucketLabel(order.raw.orderCreatedAt(), eventStartOf(targetEventId)),
                order.raw.orderQuantity().toDouble(),
                order.raw.orderTotal(),
                eventHref(targetEventId, productLink()),
            )
        }
    }

    private fun EventBiMetricBucket.leadRows(): List<EventBiMetricRow> {
        val order = leadBucketNames.withIndex().associate { (index, name) -> name to index }
        return all().sortedBy { order[it.name] ?: leadBucketNames.size }
    }

    // ------------------------------------------------------------------
    // Valor por cliente (5796-5875)
    // ------------------------------------------------------------------

    private class CustomerValue(
        var name: String,
        val event: String,
        var total: Double = 0.0,
        var ticket: Double = 0.0,
        var product: Double = 0.0,
        var items: Double = 0.0,
        var href: String = "",
    )

    private val customerValueMap = LinkedHashMap<String, CustomerValue>()
    private val customerByEventMap = LinkedHashMap<String, LinkedHashMap<String, CustomerValue>>()

    private fun buildCustomerValue() {
        fun ensureCustomer(buyerId: String, name: String, href: String): CustomerValue {
            val current = customerValueMap.getOrPut(buyerId) {
                CustomerValue(name = name.ifBlank { buyerId.ifBlank { "Cliente" } }, event = "Todos", href = href)
            }
            if (current.href.isBlank() && href.isNotBlank()) current.href = href
            return current
        }

        fun ensureEventCustomer(
            targetEventId: String,
            buyerId: String,
            name: String,
            href: String,
        ): CustomerValue {
            val cleanEventId = targetEventId.trim().ifBlank { "sem-evento" }
            val eventLabel = eventById[cleanEventId]?.name
                ?: if (cleanEventId == "sem-evento") "Sem evento vinculado" else cleanEventId
            val eventMap = customerByEventMap.getOrPut(cleanEventId) { LinkedHashMap() }
            val current = eventMap.getOrPut(buyerId) {
                CustomerValue(
                    name = name.ifBlank { buyerId.ifBlank { "Cliente" } }, event = eventLabel, href = href,
                )
            }
            if (current.href.isBlank() && href.isNotBlank()) current.href = href
            return current
        }

        // O parâmetro não pode se chamar `ticket`: sombrearia o campo `ticket` do acumulador.
        approvedTickets.forEach { approvedTicket ->
            val row = approvedTicket.raw
            val buyerId = row.ticketBuyerId()
            val buyerName = row.str("userName").ifBlank { buyerId }
            val targetEventId = row.ticketEventId()
            val href = eventHref(targetEventId, ticketLink(row.str("userName")))
            val value = row.ticketValue()
            val quantity = row.ticketQuantity().toDouble()
            ensureCustomer(buyerId, buyerName, href).apply {
                total += value
                ticket += value
                items += quantity
            }
            ensureEventCustomer(targetEventId, buyerId, buyerName, href).apply {
                total += value
                ticket += value
                items += quantity
            }
        }
        approvedOrders.forEach { order ->
            val row = order.raw
            val targetEventId = order.resolvedEventId()
            val buyerId = row.orderBuyerId()
            val buyerName = row.str("userName").ifBlank { buyerId }
            val href = if (targetEventId.isNotBlank()) {
                eventHref(targetEventId, productLink(row.str("userName")))
            } else {
                ""
            }
            val value = row.orderTotal()
            val quantity = row.orderQuantity().toDouble()
            ensureCustomer(buyerId, buyerName, href).apply {
                total += value
                product += value
                items += quantity
            }
            ensureEventCustomer(targetEventId, buyerId, buyerName, href).apply {
                total += value
                product += value
                items += quantity
            }
        }
    }

    private val customerTicketHistogramRows by lazy {
        val bucket = EventBiMetricBucket()
        customerValueMap.values.forEach { bucket.add(eventBiTicketBucket(it.total), 1.0, it.total) }
        bucket.all().sortedBy { eventBiTicketBucketSortValue(it.name) }
    }

    private val productTicketHistogramRows by lazy {
        val bucket = EventBiMetricBucket()
        customerValueMap.values.forEach { bucket.add(eventBiTicketBucket(it.product), 1.0, it.product) }
        bucket.all().sortedBy { eventBiTicketBucketSortValue(it.name) }
    }

    private val topCustomerRows by lazy {
        customerValueMap.values
            .sortedByDescending { it.total }
            .take(15)
            .map { customer ->
                tableRowOf(
                    "cliente" to customer.name,
                    "receitaTotal" to customer.total,
                    "ingresso" to customer.ticket,
                    "produto" to customer.product,
                    "itens" to customer.items,
                    "href" to customer.href,
                )
            }
    }

    private val topCustomersByEventRows by lazy {
        customerByEventMap.values
            .flatMap { eventCustomers ->
                eventCustomers.values
                    .sortedByDescending { it.total }
                    .take(5)
                    .mapIndexed { index, customer ->
                        tableRowOf(
                            "evento" to customer.event,
                            "posicao" to (index + 1).toDouble(),
                            "cliente" to customer.name,
                            "receitaTotal" to customer.total,
                            "ingresso" to customer.ticket,
                            "produto" to customer.product,
                            "itens" to customer.items,
                            "href" to customer.href,
                        )
                    }
            }
            .sortedWith(
                Comparator<EventBiTableRow> { left, right ->
                    comparePtBr(left.text("evento"), right.text("evento"))
                }.thenBy { it.number("posicao") },
            )
    }

    // ------------------------------------------------------------------
    // Consumo por turma e lote (5876-5910)
    // ------------------------------------------------------------------

    private class Consumption(
        var present: Double = 0.0,
        var productRevenue: Double = 0.0,
        var productQty: Double = 0.0,
        var noShow: Double = 0.0,
        var href: String = "",
    )

    private val classConsumptionMap = LinkedHashMap<String, Consumption>()
    private val lotConsumptionMap = LinkedHashMap<String, Consumption>()

    private fun addConsumption(
        map: MutableMap<String, Consumption>,
        key: String,
        present: Double = 0.0,
        productRevenue: Double = 0.0,
        productQty: Double = 0.0,
        noShow: Double = 0.0,
        href: String = "",
    ) {
        val current = map.getOrPut(key) { Consumption(href = href) }
        current.present += present
        current.productRevenue += productRevenue
        current.productQty += productQty
        current.noShow += noShow
        if (current.href.isBlank() && href.isNotBlank()) current.href = href
    }

    private fun buildConsumption() {
        approvedTickets.forEach { ticket ->
            val row = ticket.raw
            val present = row.ticketScannedCount().toDouble()
            val noShowCount = max(0.0, row.ticketQuantity() - present)
            val href = eventHref(row.ticketEventId(), ticketLink(row.ticketHolderName()))
            addConsumption(classConsumptionMap, row.ticketClassName(), present = present, noShow = noShowCount, href = href)
            addConsumption(lotConsumptionMap, row.ticketLotName(), present = present, noShow = noShowCount, href = href)
        }
        approvedOrders.forEach { order ->
            val row = order.raw
            val targetEventId = order.resolvedEventId()
            val product = productsById[row.orderProductId()]?.raw
            val href = if (targetEventId.isNotBlank()) {
                eventHref(targetEventId, productLink(row.orderItemName(product)))
            } else {
                ""
            }
            addConsumption(
                classConsumptionMap, row.orderClassName(userById.userOf(row.str("userId"))),
                productRevenue = row.orderTotal(), productQty = row.orderQuantity().toDouble(), href = href,
            )
            addConsumption(
                lotConsumptionMap,
                firstText(row.str("eventLoteNome"), row.orderItemCategory(product)).ifBlank { "Sem lote" },
                productRevenue = row.orderTotal(), productQty = row.orderQuantity().toDouble(), href = href,
            )
        }
    }

    private fun Map<String, Consumption>.consumptionRows(): List<EventBiMetricRow> = entries
        .map { (name, row) ->
            EventBiMetricRow(
                name = name,
                quantity = row.present,
                value = safeDivide(row.productRevenue, row.present),
                secondary = row.noShow,
                href = row.href,
            )
        }
        .sortedWith(compareByDescending<EventBiMetricRow> { it.value }.thenByDescending { it.quantity })
        .take(12)

    // ------------------------------------------------------------------
    // Fonte, desconto, preço e previsão (5911-6007)
    // ------------------------------------------------------------------

    private class SourceResult(
        var revenue: Double = 0.0,
        var present: Double = 0.0,
        var redeemed: Double = 0.0,
        var sold: Double = 0.0,
        var manual: Double = 0.0,
        var total: Double = 0.0,
        var href: String = "",
    )

    private val sourceResultMap = LinkedHashMap<String, SourceResult>()

    private fun buildSourceResults() {
        approvedTickets.forEach { ticket ->
            val row = ticket.raw
            val source = row.ticketSource()
            val current = sourceResultMap.getOrPut(source.trim().ifBlank { "Sem fonte" }) { SourceResult() }
            current.revenue += row.ticketValue()
            current.present += row.ticketScannedCount()
            current.sold += row.ticketQuantity()
            current.manual += if (row.isManualTicket()) 1.0 else 0.0
            current.total += 1
            val href = eventHref(row.ticketEventId(), ticketLink().copy(source = source))
            if (current.href.isBlank() && href.isNotBlank()) current.href = href
        }
        approvedOrders.forEach { order ->
            val row = order.raw
            val targetEventId = order.resolvedEventId()
            val source = row.orderSource()
            val current = sourceResultMap.getOrPut(source.trim().ifBlank { "Sem fonte" }) { SourceResult() }
            current.revenue += row.orderTotal()
            current.redeemed += row.orderRedeemedQuantity()
            current.sold += row.orderQuantity()
            current.manual += if (row.isManualOrder() || row.orderWithdrawalMethod() == "Manual") 1.0 else 0.0
            current.total += 1
            val href = if (targetEventId.isNotBlank()) {
                eventHref(targetEventId, productLink().copy(source = source))
            } else {
                ""
            }
            if (current.href.isBlank() && href.isNotBlank()) current.href = href
        }
    }

    private val sourceTreemapRows by lazy {
        sourceResultMap.entries.map { (name, row) ->
            EventBiMetricRow(
                name = name,
                quantity = row.sold,
                value = row.revenue,
                secondary = safeDivide(row.present + row.redeemed, row.sold) * 100,
                href = row.href,
            )
        }
    }

    private val discountImpactRows by lazy {
        val ticketsWith = approvedTickets.filter { it.raw.ticketDiscount() > 0 }
        val ticketsWithout = approvedTickets.filter { it.raw.ticketDiscount() <= 0 }
        val ordersWith = approvedOrders.filter { it.raw.orderDiscount() > 0 }
        val ordersWithout = approvedOrders.filter { it.raw.orderDiscount() <= 0 }
        listOf(
            EventBiMetricRow(
                name = "Com desconto",
                quantity = (
                    ticketsWith.sumOf { it.raw.ticketQuantity() } +
                        ordersWith.sumOf { it.raw.orderQuantity() }
                    ).toDouble(),
                value = ticketsWith.sumOf { it.raw.ticketValue() } + ordersWith.sumOf { it.raw.orderTotal() },
                secondary = (ticketsWith.size + ordersWith.size).toDouble(),
            ),
            EventBiMetricRow(
                name = "Sem desconto",
                quantity = (
                    ticketsWithout.sumOf { it.raw.ticketQuantity() } +
                        ordersWithout.sumOf { it.raw.orderQuantity() }
                    ).toDouble(),
                value = ticketsWithout.sumOf { it.raw.ticketValue() } +
                    ordersWithout.sumOf { it.raw.orderTotal() },
                secondary = (ticketsWithout.size + ordersWithout.size).toDouble(),
            ),
        )
    }

    /** `priceStrategyRows` (5960): faixa de preço do ingresso + produto. */
    private val priceStrategyRows by lazy {
        byPrice.rows(10).map { row ->
            EventBiBubbleEntry(
                name = row.name,
                x = parseEventBiNumber(row.name),
                // `row.secondary ?? row.quantity`: `byPrice` nunca soma `secondary`, então é 0.
                y = safeDivide(row.secondary, row.quantity) * 100,
                z = max(row.value, 1.0),
                value = scoreFromRatio(row.value, grossRevenue),
            )
        } + productChartRows.take(10).map { row ->
            EventBiBubbleEntry(
                name = row.name,
                x = safeDivide(row.value, row.quantity),
                y = safeDivide(row.redeemed, row.quantity) * 100,
                z = max(row.value, 1.0),
                value = scoreFromRatio(row.value, grossRevenue),
            )
        }
    }

    private val dailyRevenueRows by lazy {
        val byDay = LinkedHashMap<String, EventBiMetricRow>()
        fun accumulate(millis: Long, quantity: Double, value: Double) {
            val day = eventBiDateKey(millis)
            val daySortValue = eventBiDaySortValue(millis)
            val current = byDay[day] ?: EventBiMetricRow(name = day, sortValue = daySortValue)
            byDay[day] = current.copy(
                quantity = current.quantity + quantity,
                value = current.value + value,
                sortValue = min(current.sortValue, daySortValue),
            )
        }
        approvedTickets.forEach {
            accumulate(it.raw.ticketPurchaseDate(), it.raw.ticketQuantity().toDouble(), it.raw.ticketValue())
        }
        approvedOrders.forEach {
            accumulate(it.raw.orderCreatedAt(), it.raw.orderQuantity().toDouble(), it.raw.orderTotal())
        }
        byDay.values.sortedBy { it.sortValue }
    }

    /** `forecastRows` (5993): acumulado real no `value`, projetado no `secondary`. */
    private val forecastRows by lazy {
        var cumulativeRevenue = 0.0
        dailyRevenueRows.mapIndexed { index, row ->
            cumulativeRevenue += row.value
            val projected = if (projectedRevenue != 0.0 && dailyRevenueRows.isNotEmpty()) {
                cumulativeRevenue + safeDivide(
                    projectedRevenue - grossRevenue,
                    max(1, dailyRevenueRows.size - index).toDouble(),
                )
            } else {
                cumulativeRevenue
            }
            row.copy(value = cumulativeRevenue, secondary = max(projected, cumulativeRevenue))
        }
    }

    private val resultWaterfallRows by lazy {
        listOf(
            EventBiMetricRow("Ingressos", 1.0, ticketRevenue),
            EventBiMetricRow("Produtos", 1.0, productRevenue),
            EventBiMetricRow("Descontos", 1.0, -(ticketDiscounts + productDiscounts)),
            EventBiMetricRow("Custos", 1.0, -eventCostsTotal),
            EventBiMetricRow(
                "Resultado", 1.0,
                max(0.0, grossRevenue - ticketDiscounts - productDiscounts - eventCostsTotal),
            ),
        )
    }

    private val breakEvenTickets by lazy {
        if (eventCostsTotal > 0) {
            ceil(
                safeDivide(
                    eventCostsTotal,
                    max(safeDivide(ticketRevenue, approvedTicketQuantity.toDouble()), 1.0),
                ),
            ).toInt()
        } else {
            0
        }
    }

    // ------------------------------------------------------------------
    // Modo vendas: retirada (6009-6107)
    // ------------------------------------------------------------------

    private val pendingProductOrders by lazy {
        approvedOrders.filter { it.raw.orderRedeemedQuantity() < it.raw.orderQuantity() }
    }
    private val partialProductOrders by lazy {
        approvedOrders.filter {
            it.raw.orderRedeemedQuantity() > 0 && it.raw.orderRedeemedQuantity() < it.raw.orderQuantity()
        }
    }

    /** `orderApprovalDate(order) || orderCreatedAt(order)` (6013). */
    private fun EventBiOrder.startedAt(): Long =
        raw.orderApprovalDate().takeIf { it > 0L } ?: raw.orderCreatedAt()

    private fun EventBiOrder.pendingHours(): Double =
        startedAt().takeIf { it > 0L }?.let { (now - it) / MillisPerHour } ?: 0.0

    private val pendingWaitHoursProducts by lazy {
        pendingProductOrders.map { it.pendingHours() }.filter { it.isFinite() }
    }

    private val oldestPendingOrder by lazy {
        pendingProductOrders.filter { it.startedAt() > 0L }.minByOrNull { it.startedAt() }
    }

    /** `pendingRedeemAgingRows` (6021). */
    private val pendingRedeemAgingRows by lazy {
        listOf(
            Triple("Menos de 15 min", 0.0, 0.25),
            Triple("15–60 min", 0.25, 1.0),
            Triple("1–3h", 1.0, 3.0),
            Triple("3–12h", 3.0, 12.0),
            Triple("Mais de 12h", 12.0, 24.0),
            Triple("Mais de 24h", 24.0, Double.POSITIVE_INFINITY),
        ).map { (name, minHours, maxHours) ->
            val orders = pendingProductOrders.filter { it.pendingHours() in minHours..<maxHours }
            EventBiMetricRow(
                name = name,
                quantity = orders.sumOf {
                    max(0, it.raw.orderQuantity() - it.raw.orderRedeemedQuantity())
                }.toDouble(),
                value = orders.sumOf { order ->
                    val pending = max(0, order.raw.orderQuantity() - order.raw.orderRedeemedQuantity())
                    order.raw.orderTotal() *
                        safeDivide(pending.toDouble(), order.raw.orderQuantity().toDouble())
                },
                href = orders.firstOrNull()?.let {
                    eventHref(it.resolvedEventId(), productLink().copy(indicator = "pendente-retirada"))
                }.orEmpty(),
            )
        }
    }

    private val withdrawalStatusRows by lazy {
        listOf(
            EventBiMetricRow("Retirado", redeemedItems.toDouble(), redeemedValue),
            EventBiMetricRow("Pendente", pendingRedeemItems.toDouble(), pendingRedeemValue),
            EventBiMetricRow(
                "Retirada parcial", partialProductOrders.size.toDouble(),
                partialProductOrders.sumOf { it.raw.orderTotal() },
            ),
            EventBiMetricRow(
                "Cancelado/estornado", (cancelledOrders.size + refundedOrders.size).toDouble(),
                (cancelledOrders + refundedOrders).sumOf { it.raw.orderTotal() },
            ),
        )
    }

    /** `salesWithdrawalTimelineRows` (6047): vendido x retirado por hora. */
    private val salesWithdrawalTimelineRows by lazy {
        val timeline = LinkedHashMap<String, EventBiMetricRow>()
        approvedOrders.forEach { order ->
            val soldKey = eventBiHourLabel(order.startedAt())
            val sold = timeline[soldKey]
                ?: EventBiMetricRow(name = soldKey, sortValue = eventBiHourSortValue(soldKey))
            timeline[soldKey] = sold.copy(
                quantity = sold.quantity + order.raw.orderQuantity(),
                value = sold.value + order.raw.orderTotal(),
                sortValue = min(sold.sortValue, eventBiHourSortValue(soldKey)),
            )
            val withdrawalKey = eventBiHourLabel(order.raw.orderWithdrawalDate())
            if (withdrawalKey != "Sem horário") {
                val withdrawn = timeline[withdrawalKey]
                    ?: EventBiMetricRow(name = withdrawalKey, sortValue = eventBiHourSortValue(withdrawalKey))
                timeline[withdrawalKey] = withdrawn.copy(
                    secondary = withdrawn.secondary + order.raw.orderRedeemedQuantity(),
                    sortValue = min(withdrawn.sortValue, eventBiHourSortValue(withdrawalKey)),
                )
            }
        }
        timeline.values
            .sortedWith(
                compareBy<EventBiMetricRow> { it.sortValue }
                    .thenComparator { left, right -> comparePtBr(left.name, right.name) },
            )
            .map { it.copy(value = it.secondary, secondary = max(0.0, it.quantity - it.secondary)) }
    }

    private val productWithdrawalRows by lazy {
        productChartRows.map { row ->
            tableRowOf(
                "name" to row.name,
                "retirado" to row.redeemed,
                "pendente" to row.pending,
                "parcial" to min(row.redeemed, row.pending),
                "cancelado" to 0.0,
            )
        }
    }

    private val categoryWithdrawalRows by lazy {
        val byCategory = LinkedHashMap<String, MutableMap<String, Any?>>()
        approvedOrders.forEach { order ->
            val row = order.raw
            val category = row.orderItemCategory(productsById[row.orderProductId()]?.raw)
            val current = byCategory.getOrPut(category) {
                linkedMapOf(
                    "name" to category, "retirado" to 0.0, "pendente" to 0.0,
                    "parcial" to 0.0, "cancelado" to 0.0,
                )
            }
            val quantity = row.orderQuantity()
            val redeemed = row.orderRedeemedQuantity()
            current["retirado"] = (current["retirado"] as Double) + redeemed
            current["pendente"] = (current["pendente"] as Double) + max(0, quantity - redeemed)
            current["parcial"] = (current["parcial"] as Double) +
                if (redeemed > 0 && redeemed < quantity) 1.0 else 0.0
        }
        byCategory.values.map { EventBiTableRow(it) }
    }

    private val withdrawalRateValue get() = safeDivide(redeemedItems, approvedProductQuantity) * 100
    private val manualWithdrawalRateValue
        get() = safeDivide(
            approvedOrders.count { it.raw.orderWithdrawalMethod() == "Manual" }.toDouble(),
            approvedOrders.count { it.raw.orderRedeemedQuantity() > 0 }.toDouble(),
        ) * 100

    /** `salesHealthScore` (6090): seis fatores ponderados; `null` sem pedido aprovado. */
    private val salesHealthScore by lazy {
        if (approvedOrders.isEmpty()) {
            null
        } else {
            (
                eventBiClamp(withdrawalRateValue) * 0.25 +
                    scoreFromInverseRate(manualWithdrawalRateValue) * 0.2 +
                    scoreFromInverseRate(
                        safeDivide(pendingRedeemValue, max(productRevenue, 1.0)) * 100,
                    ) * 0.2 +
                    scoreFromInverseRate(
                        safeDivide(
                            pendingRedeemAgingRows.firstOrNull { it.name == "Mais de 24h" }?.quantity ?: 0.0,
                            max(approvedProductQuantity, 1).toDouble(),
                        ) * 100,
                    ) * 0.15 +
                    scoreFromInverseRate(
                        safeDivide(auditRows.size.toDouble(), max(approvedOrders.size, 1).toDouble()) * 100,
                    ) * 0.1 +
                    scoreFromInverseRate(
                        safeDivide(eventBiMedian(withdrawalDurations), 24.0) * 100,
                    ) * 0.1
                ).roundToInt()
        }
    }

    private val productRiskRadarRows by lazy {
        listOf(
            EventBiMetricRow(
                "Volume", approvedProductQuantity.toDouble(),
                scoreFromRatio(
                    approvedProductQuantity.toDouble(),
                    max(max(approvedTicketQuantity, approvedProductQuantity), 1).toDouble(),
                ),
            ),
            EventBiMetricRow(
                "Receita", approvedOrders.size.toDouble(),
                scoreFromRatio(productRevenue, max(grossRevenue, 1.0)),
            ),
            EventBiMetricRow(
                "Pendência", pendingRedeemItems.toDouble(),
                eventBiClamp(
                    safeDivide(pendingRedeemItems.toDouble(), max(approvedProductQuantity, 1).toDouble()) * 100,
                ),
            ),
            EventBiMetricRow(
                "Manualidade",
                approvedOrders.count { it.raw.orderWithdrawalMethod() == "Manual" }.toDouble(),
                eventBiClamp(manualWithdrawalRateValue),
            ),
            EventBiMetricRow(
                "Atraso", pendingProductOrders.size.toDouble(),
                eventBiClamp(safeDivide(eventBiMaxValue(pendingWaitHoursProducts), 24.0) * 100),
            ),
            EventBiMetricRow(
                "Auditoria", auditRows.size.toDouble(),
                eventBiClamp(
                    safeDivide(auditRows.size.toDouble(), max(approvedOrders.size, 1).toDouble()) * 100,
                ),
            ),
        )
    }

    private val operatorMethodHeatmapRows by lazy {
        approvedOrders
            .filter { it.raw.orderRedeemedQuantity() > 0 }
            .map { order ->
                EventBiHeatmapEntry(
                    row = order.raw.orderWithdrawalOperator(),
                    column = order.raw.orderWithdrawalMethod(),
                    value = order.raw.orderRedeemedQuantity().toDouble(),
                    href = eventHref(
                        order.resolvedEventId(),
                        productLink().copy(approver = order.raw.orderWithdrawalOperator()),
                    ),
                )
            }
    }

    private val withdrawalErrorRows by lazy {
        listOf(
            EventBiMetricRow(
                "QR ausente", approvedOrders.count { !it.raw.orderHasCode() }.toDouble(), 0.0,
            ),
            EventBiMetricRow(
                "Utilizado sem data",
                approvedOrders.count {
                    it.raw.orderQrStatus().lowercase().contains("util") &&
                        it.raw.orderWithdrawalDate() <= 0L
                }.toDouble(),
                0.0,
            ),
            EventBiMetricRow(
                "Ativo com baixa",
                approvedOrders.count {
                    normalizeEventBiText(it.raw.orderQrStatus()).contains("ativo") &&
                        it.raw.orderRedeemedQuantity() > 0
                }.toDouble(),
                0.0,
            ),
            EventBiMetricRow(
                "Duplicado",
                approvedOrders.flatMap { it.raw.orderCodes() }
                    .groupingBy { it }.eachCount()
                    .count { it.value > 1 }.toDouble(),
                0.0,
            ),
        ).filter { it.quantity > 0 }
    }

    /** `operatorSalesRows` (6122). */
    private val operatorSalesRows by lazy {
        val byOperator = LinkedHashMap<String, MutableMap<String, Any?>>()
        approvedOrders.forEach { order ->
            val row = order.raw
            val operator = row.orderWithdrawalOperator()
            val current = byOperator.getOrPut(operator) {
                linkedMapOf(
                    "operador" to operator, "baixas" to 0.0, "valor" to 0.0,
                    "manualidade" to 0.0, "mediana" to 0.0, "conflitos" to 0.0, "href" to "",
                )
            }
            val redeemed = row.orderRedeemedQuantity()
            current["baixas"] = (current["baixas"] as Double) + redeemed
            current["valor"] = (current["valor"] as Double) +
                row.orderTotal() * safeDivide(redeemed.toDouble(), row.orderQuantity().toDouble())
            current["manualidade"] = (current["manualidade"] as Double) +
                if (row.orderWithdrawalMethod() == "Manual") redeemed.toDouble() else 0.0
            current["conflitos"] = (current["conflitos"] as Double) +
                if (row.orderApproverName() == operator || row.orderCreatedByName() == operator) 1.0 else 0.0
            if ((current["href"] as String).isBlank()) {
                current["href"] = eventHref(order.resolvedEventId(), productLink().copy(approver = operator))
            }
        }
        byOperator.values
            .map { cells ->
                val operator = cells["operador"] as String
                val baixas = cells["baixas"] as Double
                cells["manualidade"] = safeDivide(cells["manualidade"] as Double, baixas) * 100
                cells["mediana"] = eventBiMedian(
                    approvedOrders
                        .filter { it.raw.orderWithdrawalOperator() == operator }
                        .map { hoursBetween(it.raw.orderApprovalDate(), it.raw.orderWithdrawalDate()) }
                        .finiteNonNegative(),
                )
                EventBiTableRow(cells)
            }
            .sortedByDescending { it.number("baixas") }
    }

    /** `conflictAuditRows` (6149): quem criou, aprovou e baixou é a mesma pessoa. */
    private val conflictAuditRows by lazy {
        approvedOrders.mapNotNull { order ->
            val row = order.raw
            val created = row.orderCreatedByName()
            val approved = row.orderApproverName()
            val withdrawn = row.orderWithdrawalOperator()
            val sameCreatedApproved = created != "-" &&
                normalizeEventBiText(created) == normalizeEventBiText(approved)
            val sameApprovedWithdrawn = withdrawn != "-" &&
                normalizeEventBiText(approved) == normalizeEventBiText(withdrawn)
            val sameCreatedWithdrawn = withdrawn != "-" && created != "-" &&
                normalizeEventBiText(created) == normalizeEventBiText(withdrawn)
            val allSame = sameCreatedApproved && sameApprovedWithdrawn
            val severity = when {
                allSame && row.orderTotal() >= 100 -> "Crítica"
                allSame -> "Alta"
                sameCreatedApproved || sameApprovedWithdrawn -> "Média"
                sameCreatedWithdrawn -> "Baixa"
                else -> ""
            }
            if (severity.isBlank()) return@mapNotNull null
            tableRowOf(
                "gravidade" to severity,
                "pedido" to row.str("id"),
                "cliente" to row.str("userName").ifBlank { row.orderBuyerId() },
                "produto" to row.orderItemName(productsById[row.orderProductId()]?.raw),
                "valor" to row.orderTotal(),
                "criado" to created,
                "aprovado" to approved,
                "baixado" to withdrawn,
                "href" to eventHref(
                    order.resolvedEventId(),
                    productLink(row.str("id")).copy(indicator = "conflito-funcao"),
                ),
            )
        }
    }

    private val partialWithdrawalRows by lazy {
        partialProductOrders.map { order ->
            val row = order.raw
            val quantity = row.orderQuantity()
            val redeemed = row.orderRedeemedQuantity()
            val pending = max(0, quantity - redeemed)
            tableRowOf(
                "pedido" to row.str("id"),
                "cliente" to row.str("userName").ifBlank { row.orderBuyerId() },
                "produto" to row.orderItemName(productsById[row.orderProductId()]?.raw),
                "vendido" to quantity.toDouble(),
                "retirado" to redeemed.toDouble(),
                "pendente" to pending.toDouble(),
                "saldo" to row.orderTotal() * safeDivide(pending.toDouble(), quantity.toDouble()),
                "href" to eventHref(
                    order.resolvedEventId(),
                    productLink(row.str("id")).copy(indicator = "retirada-parcial"),
                ),
            )
        }
    }

    private val pendingProductDetailRows by lazy {
        pendingProductOrders.map { order ->
            val row = order.raw
            val pendingQuantity = max(0, row.orderQuantity() - row.orderRedeemedQuantity())
            tableRowOf(
                "cliente" to row.str("userName").ifBlank { row.orderBuyerId() },
                "produto" to row.orderItemName(productsById[row.orderProductId()]?.raw),
                "quantidade" to pendingQuantity.toDouble(),
                "valor" to row.orderTotal() *
                    safeDivide(pendingQuantity.toDouble(), row.orderQuantity().toDouble()),
                "aprovadoEm" to formatEventBiDateTimeShort(row.orderApprovalDate()),
                "tempo" to order.pendingHours(),
                "origem" to row.orderSource(),
                "aprovador" to row.orderApproverName(),
                "qr" to row.orderQrStatus(),
                "href" to eventHref(
                    order.resolvedEventId(),
                    productLink(row.str("id")).copy(indicator = "pendente-retirada"),
                ),
            )
        }
    }

    private val salesWaterfallRows by lazy {
        listOf(
            EventBiMetricRow("Aprovada", 1.0, productRevenue),
            EventBiMetricRow("Entregue", 1.0, redeemedValue),
            EventBiMetricRow("Pendente", 1.0, -pendingRedeemValue),
            EventBiMetricRow("Cancelada", 1.0, -cancelledOrders.sumOf { it.raw.orderTotal() }),
        )
    }

    private val orderSourceQualityRows by lazy {
        val bySource = LinkedHashMap<String, MutableMap<String, Any?>>()
        approvedOrders.forEach { order ->
            val row = order.raw
            val source = row.orderSource()
            val current = bySource.getOrPut(source) {
                linkedMapOf(
                    "name" to source, "receita" to 0.0, "itens" to 0.0, "retirado" to 0.0,
                    "pendente" to 0.0, "parcial" to 0.0, "tempo" to 0.0, "manualidade" to 0.0,
                    "alertas" to 0.0, "href" to "",
                )
            }
            val quantity = row.orderQuantity()
            val redeemed = row.orderRedeemedQuantity()
            current["receita"] = (current["receita"] as Double) + row.orderTotal()
            current["itens"] = (current["itens"] as Double) + quantity
            current["retirado"] = (current["retirado"] as Double) + redeemed
            current["pendente"] = (current["pendente"] as Double) + max(0, quantity - redeemed)
            current["parcial"] = (current["parcial"] as Double) +
                if (redeemed > 0 && redeemed < quantity) 1.0 else 0.0
            current["manualidade"] = (current["manualidade"] as Double) +
                if (row.orderWithdrawalMethod() == "Manual") 1.0 else 0.0
            if ((current["href"] as String).isBlank()) {
                current["href"] = eventHref(order.resolvedEventId(), productLink().copy(source = source))
            }
        }
        bySource.values.map { cells ->
            val itens = cells["itens"] as Double
            cells["taxaRetirada"] = safeDivide(cells["retirado"] as Double, itens) * 100
            cells["manualidade"] = safeDivide(cells["manualidade"] as Double, itens) * 100
            EventBiTableRow(cells)
        }
    }

    private val paymentSourceRows by lazy {
        val bucket = EventBiMetricBucket()
        approvedOrders.forEach { order ->
            bucket.add(
                order.raw.orderPaymentSource(),
                order.raw.orderQuantity().toDouble(),
                order.raw.orderTotal(),
                0.0,
                eventHref(order.resolvedEventId(), productLink().copy(indicator = "fonte-pagamento")),
            )
        }
        bucket.rows(12)
    }

    private val paymentIssueRows by lazy {
        listOf(
            tableRowOf(
                "problema" to "Fonte pagamento ausente",
                "quantidade" to approvedOrders.count { it.raw.orderPaymentSource() == "-" }.toDouble(),
            ),
            tableRowOf(
                "problema" to "Pedido aprovado sem fonte",
                "quantidade" to approvedOrders.count {
                    it.raw.orderPaymentSource() == "-" && it.raw.orderTotal() > 0
                }.toDouble(),
            ),
            tableRowOf(
                "problema" to "Manual sem origem",
                "quantidade" to approvedOrders.count {
                    it.raw.isManualOrder() && it.raw.orderPaymentSource() == "-"
                }.toDouble(),
            ),
        ).filter { it.number("quantidade") > 0 }
    }

    private val discountDetailedRows by lazy {
        listOf(
            EventBiMetricRow(
                "Receita bruta", approvedOrders.size.toDouble(), productRevenue + productDiscounts,
            ),
            EventBiMetricRow("Receita líquida", approvedOrders.size.toDouble(), productRevenue),
            EventBiMetricRow(
                "Desconto",
                approvedOrders.count { it.raw.orderDiscount() > 0 }.toDouble(), productDiscounts,
            ),
        )
    }

    private val productHourHeatmapRows by lazy {
        approvedOrders.map { order ->
            val row = order.raw
            val itemName = row.orderItemName(productsById[row.orderProductId()]?.raw)
            EventBiHeatmapEntry(
                row = itemName,
                column = eventBiHourLabel(order.startedAt()),
                value = row.orderQuantity().toDouble(),
                href = eventHref(order.resolvedEventId(), productLink(itemName)),
            )
        }
    }

    /** `stockRows` (6250). */
    private val stockRows by lazy {
        dataset.products
            .map { product ->
                val productLabel = product.raw.productName()
                val metric = productRows[productLabel]
                val stock = product.stock.toDouble()
                val sold = metric?.quantity ?: 0.0
                tableRowOf(
                    "produto" to productLabel,
                    "estoque" to stock,
                    "vendido" to sold,
                    "retirado" to (metric?.redeemed ?: 0.0),
                    "pendente" to (metric?.pending ?: 0.0),
                    "disponivel" to max(0.0, stock - sold),
                    "ruptura" to if (stock > 0 && sold > stock) sold - stock else 0.0,
                    "href" to product.raw.productEventId().takeIf { it.isNotBlank() }
                        ?.let { eventHref(it, productLink(productLabel)) }.orEmpty(),
                )
            }
            .filter { it.number("estoque") > 0 || it.number("vendido") > 0 }
            .sortedByDescending { it.number("vendido") }
    }

    private val turnoverRows by lazy {
        stockRows.take(12).map { row ->
            EventBiMetricRow(
                name = row.text("produto"),
                quantity = row.number("vendido"),
                value = safeDivide(row.number("vendido"), max(row.number("estoque"), 1.0)) * 100,
                secondary = row.number("disponivel"),
                href = row.href,
            )
        }
    }

    /** `crossSellRows` (6278): pares de produto comprados pelo mesmo cliente. */
    private val crossSellRows by lazy {
        val productByBuyer = LinkedHashMap<String, MutableSet<String>>()
        approvedOrders.forEach { order ->
            productByBuyer.getOrPut(order.raw.orderBuyerId()) { linkedSetOf() } +=
                order.raw.orderItemName(productsById[order.raw.orderProductId()]?.raw)
        }
        val crossSell = LinkedHashMap<String, EventBiNetworkEdge>()
        productByBuyer.values.forEach { set ->
            val products = set.sortedWith { left, right -> comparePtBr(left, right) }
            products.forEachIndexed { index, from ->
                products.drop(index + 1).forEach { to ->
                    val key = "$from:$to"
                    val current = crossSell[key] ?: EventBiNetworkEdge(from, to, 0.0)
                    crossSell[key] = current.copy(value = current.value + 1)
                }
            }
        }
        crossSell.values.sortedByDescending { it.value }.take(12)
    }

    private val productTransferRows by lazy {
        approvedOrders.flatMap { order ->
            order.raw.extractProductTransfers().map { transfer ->
                EventBiHeatmapEntry(
                    row = transfer.actor.ifBlank { "Origem" },
                    column = "${transfer.target} · " +
                        if (order.raw.orderRedeemedQuantity() > 0) "retirado" else "pendente",
                    value = 1.0,
                    href = eventHref(
                        order.resolvedEventId(), productLink().copy(indicator = "transferencia"),
                    ),
                )
            }
        }
    }

    private val qrStatusRows by lazy {
        val bucket = EventBiMetricBucket()
        approvedOrders.forEach { order ->
            bucket.add(
                order.raw.orderQrStatus(),
                order.raw.orderQuantity().toDouble(),
                order.raw.orderTotal(),
                0.0,
                eventHref(order.resolvedEventId(), productLink().copy(indicator = "status-qr")),
            )
        }
        bucket.rows(10)
    }

    /** `improvedAuditRows` (6317): auditoria + conflitos + erros de baixa, com gravidade. */
    private val improvedAuditRows by lazy {
        (
            auditRows.map { row ->
                tableRowOf(
                    "gravidade" to when {
                        row.number("quantidade") >= 10 -> "Alta"
                        row.number("quantidade") >= 3 -> "Média"
                        else -> "Baixa"
                    },
                    "alerta" to row.text("alerta"),
                    "quantidade" to row.number("quantidade"),
                    "href" to row.href,
                    "acao" to if (row.href.isNotBlank()) "Abrir extrato" else "",
                )
            } + conflictAuditRows.map { row ->
                tableRowOf(
                    "gravidade" to row.text("gravidade"),
                    "alerta" to "Conflito de função · ${row.text("produto")}",
                    "quantidade" to 1.0,
                    "href" to row.href,
                    "acao" to if (row.href.isNotBlank()) "Abrir extrato" else "",
                )
            } + withdrawalErrorRows.map { row ->
                tableRowOf(
                    "gravidade" to if (row.name.contains("Duplicado")) "Crítica" else "Alta",
                    "alerta" to row.name,
                    "quantidade" to row.quantity,
                    "href" to row.href,
                    "acao" to if (row.href.isNotBlank()) "Abrir extrato" else "",
                )
            }
            ).filter { it.number("quantidade") > 0 }
    }

    // ------------------------------------------------------------------
    // `return` do analytics (6341-6618)
    // ------------------------------------------------------------------

    @Suppress("LongMethod")
    private fun buildResult(): EventBiAnalytics {
        // A ordem importa: os acumuladores precisam estar preenchidos antes das listas derivadas.
        buildPendingSection()
        buildTransfers()
        buildAudience()
        buildApprovedTicketMetrics()
        buildApprovedOrderMetrics()
        buildPendingAging()
        buildGateSection()
        buildRecurrence()
        buildStrategicEvents()
        buildComposition()
        buildLeadRows()
        buildCustomerValue()
        buildSourceResults()

        val topApproverCount = approvalRows.firstOrNull()?.quantity ?: 0.0
        val top3ApproverCount = approvalRows.take(3).sumOf { it.quantity }
        val ticketTopApproverCount = ticketApprovalRows.firstOrNull()?.quantity ?: 0.0
        val ticketTop3ApproverCount = ticketApprovalRows.take(3).sumOf { it.quantity }

        return EventBiAnalytics(
            totals = buildTotals(),
            commercial = buildCommercial(),
            operational = buildOperational(
                topApproverCount, top3ApproverCount, ticketTopApproverCount, ticketTop3ApproverCount,
            ),
            gate = buildGate(),
            strategic = buildStrategic(),
            sales = buildSales(),
            operationalRecords = operationalRecords,
            gateScans = gateScans,
        )
    }

    private fun buildTotals(): EventBiTotals {
        return EventBiTotals(
        approvedTickets = approvedTickets,
            approvedOrders = approvedOrders,
            rejectedTickets = rejectedTickets,
            rejectedOrders = rejectedOrders,
            pendingTickets = pendingTickets,
            pendingOrders = pendingOrders,

            grossRevenue = grossRevenue,
            netRevenue = netRevenue,
            ticketRevenue = ticketRevenue,
            ticketNetRevenue = max(0.0, ticketRevenue - ticketDiscounts),
            productRevenue = productRevenue,
            approvedTicketQuantity = approvedTicketQuantity,
            approvedProductQuantity = approvedProductQuantity,
            ticketCreatedCount = dataset.tickets.size,
            ticketApprovedCount = approvedTickets.size,
            allApprovedCount = allApprovedCount,
            allCreatedCount = allCreatedCount,
            approvalRate = safeDivide(allApprovedCount, paymentSent) * 100,
            rejectionRate = safeDivide(rejectedTickets.size + rejectedOrders.size, paymentSent) * 100,
            ticketApprovalRate = safeDivide(approvedTickets.size, dataset.tickets.size) * 100,
            ticketRejectionRate = safeDivide(rejectedTickets.size, dataset.tickets.size) * 100,
            ticketAverageByOrder = safeDivide(ticketRevenue, approvedTickets.size.toDouble()),
            ticketAverageByItem = safeDivide(ticketRevenue, approvedTicketQuantity.toDouble()),
            ticketAverageByCustomer = safeDivide(ticketRevenue, ticketBuyerPurchases.size.toDouble()),
            averageByItem = safeDivide(
                grossRevenue, (approvedTicketQuantity + approvedProductQuantity).toDouble(),
            ),
            averageByCustomer = safeDivide(grossRevenue, buyerPurchases.size.toDouble()),

            ticketFunnelRows = listOf(
                EventBiMetricRow("Clique no card", eventCardClicks.toDouble(), 0.0),
                EventBiMetricRow("Clique em comprar", eventPurchaseClicks.toDouble(), 0.0),
                EventBiMetricRow("Pedido criado", dataset.tickets.size.toDouble(), 0.0),
                EventBiMetricRow("RSVP Eu vou", rsvpGoing.toDouble(), 0.0),
                EventBiMetricRow("RSVP Talvez", rsvpMaybe.toDouble(), 0.0),
                EventBiMetricRow("Pedido aprovado", approvedTickets.size.toDouble(), 0.0),
                EventBiMetricRow("Check-in", ticketScanned.toDouble(), 0.0),
                EventBiMetricRow("Check-in com compra", checkedInBuyersWithPurchase.toDouble(), 0.0),
            ),
            funnelRows = listOf(
                EventBiMetricRow("Clique no card", eventCardClicks.toDouble(), 0.0),
                EventBiMetricRow("Clique em comprar", purchaseClicks.toDouble(), 0.0),
                EventBiMetricRow("Pedido criado", allCreatedCount.toDouble(), 0.0),
                EventBiMetricRow("RSVP Eu vou", rsvpGoing.toDouble(), 0.0),
                EventBiMetricRow("RSVP Talvez", rsvpMaybe.toDouble(), 0.0),
                EventBiMetricRow("Pedido aprovado", allApprovedCount.toDouble(), 0.0),
                EventBiMetricRow("Check-in", ticketScanned.toDouble(), 0.0),
                EventBiMetricRow("Check-in com compra", checkedInBuyersWithPurchase.toDouble(), 0.0),
            ),
        )
    }

    private fun buildCommercial(): EventBiCommercial {
        return EventBiCommercial(
        lotRows = byLot.rows(12),
            classRows = byClass.rows(12),
            audienceRows = byAudience.rows(8),
            audienceTotal = byAudience.totalQuantity,
            // `WEEKDAYS.map(...)`/`PERIODS.map(...)`: dia/período sem venda entra zerado.
            weekdayRows = EventBiWeekdays.map { byWeekday.valueOf(it) ?: EventBiMetricRow(it) },
            periodRows = EventBiPeriods.map { byPeriod.valueOf(it) ?: EventBiMetricRow(it) },
            priceRows = byPrice.rows(12),
            approvalRows = approvalRows,
            ticketApprovalRows = ticketApprovalRows,
            approvalMethodRows = byApprovalMethod.rows(10),
            ticketApprovalMethodRows = byTicketApprovalMethod.rows(10),
            approvalAverage = approvalDurations.mean(),
            ticketApprovalAverage = ticketApprovalDurations.mean(),
            approvalMedian = eventBiMedian(approvalDurations),
            ticketApprovalMedian = eventBiMedian(ticketApprovalDurations),
        )
    }

    private fun buildOperational(
        topApproverCount: Double,
        top3ApproverCount: Double,
        ticketTopApproverCount: Double,
        ticketTop3ApproverCount: Double,
    ): EventBiOperational {
        return EventBiOperational(
        pendingAgingRows = listOf(
                EventBiMetricRow("Menos de 1h", pendingLess1.toDouble(), 0.0),
                EventBiMetricRow("1 a 6h", pendingOneTo6.toDouble(), 0.0),
                EventBiMetricRow("6 a 24h", pendingSixTo24.toDouble(), 0.0),
                EventBiMetricRow("Mais de 24h", pendingMore24.toDouble(), 0.0),
            ),
            ticketPendingAgingRows = listOf(
                EventBiMetricRow("Menos de 1h", ticketPendingLess1.toDouble(), 0.0),
                EventBiMetricRow("1 a 6h", ticketPendingOneTo6.toDouble(), 0.0),
                EventBiMetricRow("6 a 24h", ticketPendingSixTo24.toDouble(), 0.0),
                EventBiMetricRow("Mais de 24h", ticketPendingMore24.toDouble(), 0.0),
            ),
            operationalPendingCount = pendingOperationalRecords.size,
            operationalPendingNearEvent = pendingNearEvent,
            operationalPendingAtDoor = pendingAtDoor,
            operationalPendingByEventRows = pendingByEvent.rows(12),
            operationalPendingByTypeRows = pendingByType.rows(10),
            operationalPendingAgeRows = pendingAgeBuckets.all(),
            operationalApprovalAverage = operationalApprovalDurations.mean(),
            operationalApprovalMedian = eventBiMedian(operationalApprovalDurations),
            operationalApprovalP90 = eventBiPercentile(operationalApprovalDurations, 0.9),
            operationalApprovalP95 = eventBiPercentile(operationalApprovalDurations, 0.95),
            operationalMaxPendingHours = eventBiMaxValue(pendingWaitHours),
            operationalApprovedWithin5m = safeDivide(
                operationalApprovalDurations.count { it <= 5.0 / 60 }.toDouble(),
                operationalApprovalDurations.size.toDouble(),
            ) * 100,
            operationalApprovedWithin15m = safeDivide(
                operationalApprovalDurations.count { it <= 0.25 }.toDouble(),
                operationalApprovalDurations.size.toDouble(),
            ) * 100,
            operationalApprovedWithin1h = safeDivide(
                operationalApprovalDurations.count { it <= 1 }.toDouble(),
                operationalApprovalDurations.size.toDouble(),
            ) * 100,
            operationalApprovedWithin24h = safeDivide(
                operationalApprovalDurations.count { it <= 24 }.toDouble(),
                operationalApprovalDurations.size.toDouble(),
            ) * 100,
            slaBySourceRows = slaBySourceRows,
            slaByApproverRows = slaByApproverRows,
            slaByEventRows = slaByEventRows,
            approvalToEntryMedian = eventBiMedian(approvalToEntryDurations),
            approvalToWithdrawalMedian = eventBiMedian(approvalToWithdrawalDurations),
            approvedWithoutCodeCount = approvedWithoutCode.size,
            codeWithoutUseCount = codeWithoutUse.size,
            usedWithoutApprovalCount = usedWithoutApproval.size,
            inconsistentStatusCount = inconsistentStatus.size,
            approvedNearEventCount = approvedNearEvent.size,
            operatorQualityRows = operatorQualityRows,
            activeOperatorCount = activeOperatorCount,
            operatorDistributionRows = operatorDistributionRows,
            demandWithoutCoverageRows = demandWithoutCoverageRows,
            outsideHoursApprovals = outsideHoursApprovals,
            singleOperatorEventRows = singleOperatorEventRows,
            manualityStageRows = manualityStageRows,
            manualityStageChartRows = manualityStageChartRows,
            operationalControlAlertRows = operationalAlertRows,
            topApproverDependency = safeDivide(topApproverCount, allApprovedCount.toDouble()) * 100,
            top3ApproverDependency = safeDivide(top3ApproverCount, allApprovedCount.toDouble()) * 100,
            ticketTopApproverDependency = safeDivide(
                ticketTopApproverCount, approvedTickets.size.toDouble(),
            ) * 100,
            ticketTop3ApproverDependency = safeDivide(
                ticketTop3ApproverCount, approvedTickets.size.toDouble(),
            ) * 100,
            slowApprovals = slowApprovals,
            operationalAlerts = operationalAlerts,
            operationalTicketAlerts = operationalTicketAlerts,
        )
    }

    private fun buildGate(): EventBiGate {
        return EventBiGate(
        ticketScanned = ticketScanned,
            noShow = noShow,
            showRate = safeDivide(ticketScanned, approvedTicketQuantity) * 100,
            noShowRate = safeDivide(noShow, approvedTicketQuantity) * 100,
            revenuePerPresent = safeDivide(ticketRevenue, ticketScanned.toDouble()),
            duplicateScans = duplicateScans,
            invalidScans = invalidScans,
            appScans = appScans,
            manualScans = manualScans,
            manualityRate = manualityRate,
            qrRate = qrRate,
            totalCapacity = totalCapacity,
            capacityRemaining = capacityRemaining,
            occupancyRate = occupancyRate,
            queueRisk = queueRisk,
            queuePressure = queuePressure,
            activeGateOperators = activeGateOperators,
            peakInterval = peakInterval,
            averageMinutesBetweenScans = averageMinutesBetweenScans,
            longestFastSequence = longestFastSequence,
            longestIdleMinutes = longestIdleMinutes,
            entryCumulativeRows = entryCumulativeRows,
            entryTimingRows = timingBuckets.all(),
            presenceByTypeRows = presenceByType.rows(12),
            presenceByLotRows = presenceByLot.rows(12),
            // `noShowRateByLotRows` (5069): a mesma base, virada para o ausente.
            noShowRateByLotRows = presenceByLot.rows(12).map { row ->
                row.copy(
                    quantity = row.secondary,
                    value = safeDivide(row.secondary, row.secondary + row.quantity) * 100,
                )
            },
            scanModeByHourRows = scanModeByHourRows,
            entryModeRows = entryModeRows,
            portariaOperatorRows = portariaOperatorRows,
            portariaOperatorChartRows = portariaOperatorChartRows,
            operatorQualityRadarRows = operatorQualityRadarRows,
            invalidReasonRows = invalidReasonRows,
            duplicateContextRows = duplicateContextRows,
            approvedWithoutReadRows = approvedWithoutReadRows,
            presentByClassRows = presenceByClass.rows(12),
            presenceBySourceRows = presenceBySource.rows(10),
            presenceByTransferRows = presenceByTransfer.rows(10),
            operationalCategoryRows = presenceByOperationalCategory.rows(10),
            occupancyRows = occupancyRows,
            intervalRows = intervalRows,
            liveStatusRows = liveStatusRows,
            absentRows = absentRows,
            unusedActiveRows = unusedActiveRows,
            portariaAlertRows = buildPortariaAlertRows(),
            portariaEventComparisonRows = portariaEventComparisonRows,
            portariaEventComparisonChartRows = portariaEventComparisonChartRows,
            scanByHourRows = scanByHour.rows(24).sortedBy { it.name },
            noShowByClassRows = noShowByClass.rows(12),
            noShowByLotRows = noShowByLot.rows(12),
        )
    }

    private fun buildStrategic(): EventBiStrategic {
        return EventBiStrategic(
        uniqueBuyers = buyerPurchases.size,
            recurringBuyers = recurringBuyers,
            recurringRate = safeDivide(recurringBuyers, buyerPurchases.size) * 100,
            leadRows = leadBuckets.all(),
            recurrenceRows = recurrenceRows,
            projectedRevenue = projectedRevenue,
            resultWithoutCosts = netRevenue,
            eventDecisionRows = eventDecisionRows,
            revenueOriginRows = revenueOriginRows,
            revenueDetailRows = revenueDetailRows,
            totalRevenuePerBuyer = safeDivide(grossRevenue, buyerPurchases.size.toDouble()),
            totalRevenuePerPresent = safeDivide(grossRevenue, ticketScanned.toDouble()),
            ticketRevenuePerPresent = safeDivide(ticketRevenue, ticketScanned.toDouble()),
            productRevenuePerPresent = safeDivide(productRevenue, ticketScanned.toDouble()),
            productPerPresent = safeDivide(approvedProductQuantity, ticketScanned),
            productRevenueShare = safeDivide(productRevenue, grossRevenue) * 100,
            ticketRevenueShare = safeDivide(ticketRevenue, grossRevenue) * 100,
            ticketBuyerCount = ticketBuyerIds.size,
            checkedInTicketBuyerCount = checkedInTicketBuyerIds.size,
            productBuyerCount = productBuyerIds.size,
            productRedeemedBuyerCount = productRedeemedBuyerIds.size,
            buyersWithTicketAndProduct = buyersWithTicketAndProduct,
            ticketWithoutProduct = ticketWithoutProduct,
            productWithoutTicket = productWithoutTicket,
            productPresentBuyerIds = checkedInProductBuyerIds.size,
            strategicFunnelRows = strategicFunnelRows,
            attachRateRows = attachRateRows,
            strategicEventRows = strategicEventRows,
            strategicScore = strategicScore,
            strategicDecision = strategicDecision,
            strategicRadarRows = strategicRadarRows,
            strategicBubbleRows = strategicBubbleRows,
            revenuePerPresentRows = revenuePerPresentRows,
            eventProductHeatmapRows = eventProductHeatmapRows,
            eventCategoryHeatmapRows = eventCategoryHeatmapRows,
            categoryCompositionChartRows = categoryCompositionChartRows,
            ticketLeadRows = ticketLeadBucket.leadRows(),
            productLeadRows = productLeadBucket.leadRows(),
            recurrenceDetailRows = recurrenceDetailRows,
            strategicRecurringBuyers = strategicRecurringBuyerIds.size,
            strategicRecurringRate = strategicRecurringRate,
            tenantParticipationRows = tenantParticipationRows,
            customerTicketHistogramRows = customerTicketHistogramRows,
            topCustomerRows = topCustomerRows,
            topCustomersByEventRows = topCustomersByEventRows,
            classConsumptionRows = classConsumptionMap.consumptionRows(),
            lotConsumptionRows = lotConsumptionMap.consumptionRows(),
            sourceTreemapRows = sourceTreemapRows,
            discountImpactRows = discountImpactRows,
            priceStrategyRows = priceStrategyRows,
            forecastRows = forecastRows,
            resultWaterfallRows = resultWaterfallRows,
            eventCostsTotal = eventCostsTotal,
            hasEventCostsField = hasEventCostsField,
            breakEvenTickets = breakEvenTickets,
        )
    }

    private fun buildSales(): EventBiSales {
        return EventBiSales(
        redeemedItems = redeemedItems,
            redeemedValue = redeemedValue,
            pendingRedeemItems = pendingRedeemItems,
            pendingRedeemValue = pendingRedeemValue,
            withdrawalRate = withdrawalRateValue,
            pendingRedeemOrders = pendingProductOrders.size,
            partialRedeemOrders = partialProductOrders.size,
            oldestPendingOrderName = oldestPendingOrder?.let { order ->
                order.raw.orderItemName(productsById[order.raw.orderProductId()]?.raw)
            } ?: "-",
            maxPendingRedeemHours = eventBiMaxValue(pendingWaitHoursProducts),
            averageWithdrawalHours = eventBiMedian(withdrawalDurations),
            manualWithdrawalRate = manualWithdrawalRateValue,
            productRows = productTableRows,
            productChartRows = productChartRows,
            categoryRows = byProductCategory.rows(12),
            discountRows = byDiscountSource.rows(10),
            orderSourceRows = byOrderSource.rows(10),
            withdrawalMethodRows = byWithdrawalMethod.rows(10),
            withdrawalOperatorRows = byWithdrawalOperator.rows(12),
            transferModeRows = byTransferMode.rows(6),
            transferTargetRows = byTransferTarget.rows(6),
            transferActorRows = byTransferActor.rows(12),
            auditRows = auditRows,
            pendingRedeemAgingRows = pendingRedeemAgingRows,
            withdrawalStatusRows = withdrawalStatusRows,
            salesWithdrawalTimelineRows = salesWithdrawalTimelineRows,
            productWithdrawalRows = productWithdrawalRows,
            categoryWithdrawalRows = categoryWithdrawalRows,
            salesHealthScore = salesHealthScore,
            productRiskRadarRows = productRiskRadarRows,
            operatorMethodHeatmapRows = operatorMethodHeatmapRows,
            withdrawalErrorRows = withdrawalErrorRows,
            operatorSalesRows = operatorSalesRows,
            conflictAuditRows = conflictAuditRows,
            partialWithdrawalRows = partialWithdrawalRows,
            pendingProductDetailRows = pendingProductDetailRows,
            salesWaterfallRows = salesWaterfallRows,
            orderSourceQualityRows = orderSourceQualityRows,
            paymentSourceRows = paymentSourceRows,
            paymentIssueRows = paymentIssueRows,
            discountDetailedRows = discountDetailedRows,
            productHourHeatmapRows = productHourHeatmapRows,
            stockRows = stockRows,
            turnoverRows = turnoverRows,
            crossSellRows = crossSellRows,
            productTicketHistogramRows = productTicketHistogramRows,
            productTransferRows = productTransferRows,
            qrStatusRows = qrStatusRows,
            improvedAuditRows = improvedAuditRows,
        )
    }

    fun run(): EventBiAnalytics = buildResult()
}

private fun JsonObject?.firstNotBlank(vararg keys: String): String =
    keys.firstNotNullOfOrNull { key -> str(key).takeIf { it.isNotBlank() } }.orEmpty()
