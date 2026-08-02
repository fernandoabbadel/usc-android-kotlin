package com.example.usc1.domain.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Acessores de linha do BI de Eventos (M8.1b).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 595-2137.
 * O web lê `Row = Record<string, unknown>` cru do Supabase; aqui o equivalente lê `JsonObject`,
 * mantendo cada cadeia de apelidos (`row.a || row.b || data.c`) exatamente na mesma ordem.
 */

// ------------------------------------------------------------------
// Leitura crua (`asString`, `asObject`, `asArray`, `parseNumber`)
// ------------------------------------------------------------------

/** `asString` do web: primitivo vira texto; objeto/array/null viram "". */
fun JsonElement?.asText(): String = when (this) {
    null, JsonNull -> ""
    is JsonPrimitive -> contentOrNull.orEmpty().trim()
    else -> ""
}

fun JsonObject?.str(key: String): String = this?.get(key).asText()

fun JsonObject?.obj(key: String): JsonObject? = this?.get(key) as? JsonObject

/** `asArray` (364): valor que não é array vira lista vazia. */
fun JsonObject?.arr(key: String): List<JsonElement> = (this?.get(key) as? JsonArray).orEmpty()

fun JsonObject?.objects(key: String): List<JsonObject> = arr(key).mapNotNull { it as? JsonObject }

fun JsonObject?.num(key: String, fallback: Double = 0.0): Double {
    val element = this?.get(key) as? JsonPrimitive ?: return fallback
    element.contentOrNull?.let { raw ->
        if (element.isString) return parseEventBiNumber(raw, fallback)
        return raw.toDoubleOrNull()?.takeIf { it.isFinite() } ?: fallback
    }
    return fallback
}

/** Verdade "JS-truthy" das checagens `Boolean(data.manualGateEntry)` do web. */
fun JsonObject?.truthy(key: String): Boolean = when (val element = this?.get(key)) {
    null, JsonNull -> false
    is JsonPrimitive -> element.booleanOrNull
        ?: element.contentOrNull?.let { it.isNotBlank() && it != "0" && it != "false" }
        ?: false
    is JsonArray -> element.isNotEmpty()
    is JsonObject -> element.isNotEmpty()
    else -> false
}

/** `asString(a || b || c)` do web: o primeiro apelido preenchido. */
fun firstText(vararg values: String?): String =
    values.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()

private fun JsonObject?.firstOf(vararg keys: String): String =
    keys.firstNotNullOfOrNull { key -> str(key).takeIf { it.isNotBlank() } }.orEmpty()

// ------------------------------------------------------------------
// Status (595-642)
// ------------------------------------------------------------------

/** `statusValue` (595): `status || situacao || state`, normalizado. */
fun JsonObject?.statusValue(): String =
    normalizeEventBiText(firstOf("status", "situacao", "state"))

/** `statementStatusFilterFromStatus` (635). */
fun statementStatusFromStatus(value: String?): EventBiStatementStatus {
    val status = EventBiStatus.classify(value)
    if (status == EventBiStatus.Approved) return EventBiStatementStatus.Approved
    if (status == EventBiStatus.Rejected || status == EventBiStatus.Cancelled) {
        return EventBiStatementStatus.All
    }
    val normalized = normalizeEventBiText(value)
    if (normalized.contains("analise") || normalized.contains("review")) {
        return EventBiStatementStatus.Review
    }
    if (normalized.contains("pend") || normalized.contains("pending") || normalized.contains("aguard")) {
        return EventBiStatementStatus.Pending
    }
    return EventBiStatementStatus.All
}

// ------------------------------------------------------------------
// Classificação de texto (876-897, 1600-1603, 1143-1152)
// ------------------------------------------------------------------

/** `hasStudentClass` (876): turma que realmente identifica aluno. */
fun hasStudentClass(value: String?): Boolean {
    val normalized = normalizeEventBiText(value)
    return normalized.isNotBlank() && normalized !in NonStudentClasses
}

private val NonStudentClasses = listOf(
    "-", "geral", "sem turma", "porta", "visitante", "visitor", "externo", "nao aluno",
)

/** `isManualUserId` (894). */
fun isManualUserId(value: String?): Boolean {
    val normalized = normalizeEventBiText(value)
    return normalized.startsWith("manual") || normalized.contains("porta")
}

/** `isCourtesyText` (1600). */
fun isCourtesyText(value: String?): Boolean {
    val normalized = normalizeEventBiText(value)
    return normalized.contains("cortesia") || normalized.contains("gratuito") ||
        normalized.contains("free")
}

/** `approvalMethodLabel` (1143). */
fun approvalMethodLabel(value: String?, fallback: String = "-"): String {
    val method = normalizeEventBiText(value)
    return when {
        method.contains("manual") -> "Manual"
        method.contains("pix") -> "Pix validado"
        method.contains("auto") -> "Automático"
        method.contains("import") -> "Importado"
        method.contains("cortesia") -> "Cortesia"
        method.contains("admin") -> "Admin"
        else -> fallback
    }
}

/** `isManagementEntityRole` (1663). */
fun isManagementEntityRole(value: String?): Boolean {
    val role = normalizeEventBiText(value)
    return role.startsWith("president") || role.startsWith("vice") || role.startsWith("secretar") ||
        role.startsWith("tesour") || role.startsWith("diretor")
}

// ------------------------------------------------------------------
// Entradas do ingresso (803-874, 963-1007)
// ------------------------------------------------------------------

/**
 * `readTicketEntries` (803): a primeira lista encontrada em
 * `payment_config|paymentConfig|data`.`ticketEntries|tickets|ingressos`, e depois nas mesmas
 * chaves da própria linha.
 */
fun JsonObject?.readTicketEntries(): List<JsonObject> {
    val config = obj("payment_config") ?: obj("paymentConfig") ?: obj("data")
    listOf("ticketEntries", "tickets", "ingressos").forEach { key ->
        (config?.get(key) as? JsonArray)?.let { array ->
            return array.mapNotNull { it as? JsonObject }
        }
    }
    listOf("ticketEntries", "tickets", "ingressos").forEach { key ->
        (this?.get(key) as? JsonArray)?.let { array ->
            return array.mapNotNull { it as? JsonObject }
        }
    }
    return emptyList()
}

/** `entryScannedAt` (821). */
fun JsonObject?.entryScannedAt(): Long = parseEventBiDate(
    firstOf("scannedAt", "scanAt", "checkedAt", "checkinAt", "lidoEm", "dataCheckin"),
)

/** `getLatestDateFromEntries` (825): a leitura mais recente entre as entradas. */
fun List<JsonObject>.latestEntryDate(): Long = this
    .map { parseEventBiDate(it.firstOf("scannedAt", "usedAt", "withdrawalAt", "checkinAt", "checkedAt")) }
    .filter { it > 0L }
    .maxOrNull() ?: 0L

/** `ticketRowCheckinAt` (832). */
fun JsonObject?.ticketRowCheckinAt(): Long =
    parseEventBiDate(firstOf("checkinAt", "checkedAt", "scannedAt", "dataCheckin"))

/** `isTicketEntryCheckedIn` (836). */
fun JsonObject?.isTicketEntryCheckedIn(): Boolean {
    if (entryScannedAt() > 0L) return true
    val status = normalizeEventBiText(firstOf("status", "scanStatus", "situacao"))
    return status.contains("lido") || status.contains("scan") || status.contains("check")
}

/** `entryScanSource` (841). */
fun JsonObject?.entryScanSource(): String {
    val source = normalizeEventBiText(firstOf("scanSource", "source", "scannerSource", "usedMethod"))
    return when {
        source.contains("manual") -> "Manual"
        source.contains("qr") || source.contains("scan") || source.contains("app") -> "QR code"
        entryScannedAt() > 0L -> "QR code"
        else -> "-"
    }
}

/** `isTransferredEntry` (862). */
fun JsonObject?.isTransferredEntry(): Boolean =
    normalizeEventBiText(firstOf("status", "situacao", "scanStatus")).contains("transfer")

/** `isCancelledEntry` (867). */
fun JsonObject?.isCancelledEntry(): Boolean {
    val status = normalizeEventBiText(firstOf("status", "situacao", "scanStatus"))
    return status.contains("cancel") || status.contains("rejeit") || status.contains("estorn") ||
        status.contains("refund")
}

/** `activeTicketEntries` (872): fora as transferidas e as canceladas. */
fun JsonObject?.activeTicketEntries(): List<JsonObject> =
    readTicketEntries().filter { !it.isTransferredEntry() && !it.isCancelledEntry() }

/** `ticketEntryToken` (963). */
fun JsonObject?.ticketEntryToken(): String =
    firstOf("token", "ticketToken", "id", "codigo", "qrCode", "code")

/** `ticketEntryUserId` (967). */
fun JsonObject?.ticketEntryUserId(): String =
    firstOf("holderUserId", "userId", "uid", "ownerUserId", "toUserId")

/** `rowCheckinOperator` (997). */
fun JsonObject?.rowCheckinOperator(): String = firstText(
    firstOf("checkinByUserName", "checkinBy"),
    obj("data").firstOf("checkinByUserName", "checkinBy"),
).ifBlank { "Sem operador" }

/** `entryScanOperator` (1002). */
fun JsonObject?.entryScanOperator(row: JsonObject?): String =
    firstOf("scannedByUserName", "usedByUserName", "checkinByUserName", "operatorName")
        .ifBlank { row.rowCheckinOperator() }

/** `checkinAuditRows` (1009): log da linha, do `data` e da entrada. */
fun JsonObject?.checkinAuditRows(entry: JsonObject?): List<JsonObject> =
    objects("checkinAuditLog") +
        obj("data").objects("checkinAuditLog") +
        entry.objects("checkinAuditLog") +
        entry.objects("auditLog")

/** `isDuplicateAuditEntry` (1021). */
fun JsonObject?.isDuplicateAuditEntry(): Boolean {
    val action = normalizeEventBiText(firstOf("action", "type", "status", "reason"))
    return action.contains("repeated") || action.contains("duplic") ||
        action.contains("ja utilizado") || action.contains("already")
}

/** `invalidReasonLabel` (1026). */
fun invalidReasonLabel(value: String?): String {
    val normalized = normalizeEventBiText(value)
    if (normalized.isBlank()) return ""
    return when {
        normalized.contains("outro evento") -> "QR (Quick Response) de outro evento"
        normalized.contains("ja utilizado") || normalized.contains("already") ||
            normalized.contains("duplic") -> "QR (Quick Response) já utilizado"
        normalized.contains("cancel") -> "QR (Quick Response) cancelado"
        normalized.contains("aprov") || normalized.contains("payment") ||
            normalized.contains("pagamento") -> "Ingresso não aprovado"
        normalized.contains("expir") -> "Código expirado"
        normalized.contains("mal format") || normalized.contains("invalid") ||
            normalized.contains("inval") -> "Código mal formatado"
        normalized.contains("produto") -> "Produto tentando entrar como ingresso"
        normalized.contains("permiss") -> "Usuário sem permissão"
        normalized.contains("scanner") || normalized.contains("tecnic") ||
            normalized.contains("erro") -> "Erro técnico do scanner"
        normalized.contains("nao encontrado") || normalized.contains("not found") ||
            normalized.contains("inexist") -> "QR (Quick Response) inexistente"
        else -> value?.trim().orEmpty().ifBlank { "Leitura inválida sem motivo informado" }
    }
}

/** `ticketEntryInvalidReason` (1046). */
fun JsonObject?.ticketEntryInvalidReason(row: JsonObject?): String {
    val direct = invalidReasonLabel(
        firstOf("invalidReason", "errorReason", "reason", "error", "message", "scanError"),
    )
    if (direct.isNotBlank()) return direct

    val status = normalizeEventBiText(firstOf("status", "scanStatus", "situacao"))
    return when {
        status.contains("duplic") -> "QR (Quick Response) já utilizado"
        status.contains("cancel") -> "QR (Quick Response) cancelado"
        status.contains("inval") -> "Leitura inválida sem motivo informado"
        row.checkinAuditRows(this).any { it.isDuplicateAuditEntry() } ->
            "QR (Quick Response) já utilizado"
        else -> ""
    }
}

/** `ticketHasQrCode` (797). */
fun JsonObject?.ticketHasQrCode(): Boolean = readTicketEntries().any { entry ->
    entry.firstOf("token", "id", "codigo", "qrCode", "code").isNotBlank()
}

/** `ticketQrStatus` (1060). */
fun JsonObject?.ticketQrStatus(): String {
    val entries = readTicketEntries()
    if (entries.isEmpty()) return if (ticketHasQrCode()) "QR disponível" else "Sem QR (Quick Response)"
    val checked = entries.count { it.isTicketEntryCheckedIn() }
    val invalid = entries.count { it.ticketEntryInvalidReason(this).isNotBlank() }
    return when {
        checked >= entries.size -> "Usado"
        checked > 0 -> "Parcialmente usado"
        invalid > 0 -> "Com tentativa inválida"
        else -> "Ativo sem uso"
    }
}

/** `ticketInvalidScanCount` (855). */
fun JsonObject?.ticketInvalidScanCount(): Int = readTicketEntries().count { entry ->
    val status = normalizeEventBiText(entry.firstOf("status", "scanStatus", "situacao"))
    status.contains("invalid") || status.contains("inval") || status.contains("duplic")
}

// ------------------------------------------------------------------
// Ingresso (686-801, 971-995)
// ------------------------------------------------------------------

/** `ticketEventId` (686). */
fun JsonObject?.ticketEventId(): String =
    firstOf("eventoId", "eventId", "event_id", "linkEvento", "globalEventId")

/** `ticketQuantity` (705): quantidade explícita ou o número de entradas (mínimo 1). */
fun JsonObject?.ticketQuantity(): Int {
    val explicit = listOf("quantidade", "quantity", "qtd")
        .firstNotNullOfOrNull { key -> num(key, 0.0).takeIf { it > 0 } } ?: 0.0
    if (explicit > 0) return explicit.toInt()
    return max(readTicketEntries().size, 1)
}

/** `ticketValue` (711). */
fun JsonObject?.ticketValue(): Double = listOf("valorTotal", "total", "valor", "amount", "preco")
    .firstNotNullOfOrNull { key -> this?.get(key)?.takeIf { it != JsonNull }?.let { num(key) } } ?: 0.0

/** `ticketDiscount` (715). */
fun JsonObject?.ticketDiscount(): Double {
    val data = obj("data")
    return listOf("discountValue", "desconto").firstNotNullOfOrNull { key ->
        this?.get(key)?.takeIf { it != JsonNull }?.let { num(key) }
    } ?: listOf("discountValue", "desconto").firstNotNullOfOrNull { key ->
        data?.get(key)?.takeIf { it != JsonNull }?.let { data.num(key) }
    } ?: 0.0
}

/** `ticketPurchaseDate` (720). */
fun JsonObject?.ticketPurchaseDate(): Long =
    parseEventBiDate(firstOf("dataSolicitacao", "createdAt", "created_at", "insertedAt"))

/** `ticketApprovalDate` (724). */
fun JsonObject?.ticketApprovalDate(): Long =
    parseEventBiDate(firstOf("dataAprovacao", "approvedAt", "aprovadoEm", "updatedAt"))

/** `ticketLotName` (728). */
fun JsonObject?.ticketLotName(): String =
    firstOf("loteNome", "lote", "ticketName", "tipoIngresso", "categoria").ifBlank { "Sem lote" }

/** `ticketClassName` (732). */
fun JsonObject?.ticketClassName(): String = firstText(
    firstOf("userTurma", "turma"),
    obj("data").firstOf("turma", "userTurma"),
).ifBlank { "Sem turma" }

/** `ticketBuyerId` (737). */
fun JsonObject?.ticketBuyerId(): String =
    firstOf("userId", "user_id", "compradorId", "email", "userEmail", "userName")
        .ifBlank { "pedido-${str("id")}" }

/** `ticketApproverName` (741). */
fun JsonObject?.ticketApproverName(): String =
    firstOf("aprovadoPor", "approvedBy", "aprovador", "approverName").ifBlank { "Sem aprovador" }

/** `ticketItemName` (745). */
fun JsonObject?.ticketItemName(): String =
    firstOf("itemName", "ticketName", "loteNome", "lote", "tipoIngresso").ifBlank { "Ingresso" }

/** `ticketItemCategory` (749). */
fun JsonObject?.ticketItemCategory(): String = firstText(
    str("itemCategory"),
    obj("data").firstOf("itemCategory", "categoria", "loteCategoria"),
    ticketLotName(),
).ifBlank { "Ingresso" }

/** `ticketApprovalMethod` (754). */
fun JsonObject?.ticketApprovalMethod(): String = approvalMethodLabel(
    firstOf("approvalMethod", "metodo", "aprovacaoMetodo", "approvalSource"),
    if (ticketApproverName() != "Sem aprovador") "Manual" else "-",
)

/** `isManualTicket` (761). */
fun JsonObject?.isManualTicket(): Boolean {
    val data = obj("data")
    val method = normalizeEventBiText(
        firstText(firstOf("approvalMethod", "metodo"), data.str("approvalMethod")),
    )
    return data.truthy("manualGateEntry") || data.truthy("createdManually") ||
        data.truthy("manualOrder") || method.contains("manual") || method.contains("porta") ||
        isManualUserId(str("userId"))
}

/** `ticketSource` (774). */
fun JsonObject?.ticketSource(): String {
    val data = obj("data")
    val source = firstText(data.firstOf("source", "origem"), firstOf("source", "canal_origem"))
    if (source.isNotBlank()) return source
    if (data.truthy("manualGateEntry")) return "Cadastro manual"
    if (isManualTicket()) return "Manual/admin"
    return "App"
}

/** `ticketPaymentSource` (783). */
fun JsonObject?.ticketPaymentSource(): String {
    val config = obj("payment_config") ?: obj("paymentConfig")
    val data = obj("data")
    val direct = firstText(
        firstOf("paymentSource", "paymentMethod"),
        data.firstOf("paymentSource", "paymentMethod"),
        config.firstOf("method", "provider"),
    )
    if (direct.isNotBlank()) return direct
    return if (this?.get("payment_config") != null && this["payment_config"] != JsonNull) {
        "Configuração de pagamento"
    } else {
        "-"
    }
}

/** `ticketDiscountSource` (792). */
fun JsonObject?.ticketDiscountSource(): String = firstText(
    str("discountSource"),
    obj("data").str("discountSource"),
    str("discountKind"),
    obj("data").str("discountKind"),
)

/** `ticketHolderName` (971). */
fun JsonObject?.ticketHolderName(entry: JsonObject? = null): String = firstText(
    entry.firstOf("holderName", "userName", "nome"),
    firstOf("userName", "nome"),
).ifBlank { "Participante" }

/** `ticketHolderTurma` (979). */
fun JsonObject?.ticketHolderTurma(entry: JsonObject? = null): String = firstText(
    entry.firstOf("holderTurma", "userTurma", "turma"),
    ticketClassName(),
).ifBlank { "Sem turma" }

/** `ticketContact` (987). */
fun JsonObject?.ticketContact(user: JsonObject?): String {
    val data = obj("data")
    val phone = firstText(
        data.firstOf("telefone", "phone", "whatsapp"),
        firstOf("telefone", "phone"),
        user.firstOf("telefone", "phone"),
    )
    if (phone.isNotBlank()) return phone
    return firstText(data.str("email"), str("email"), user.str("email")).ifBlank { "-" }
}

/** `ticketScannedCount` (848). */
fun JsonObject?.ticketScannedCount(): Int {
    val scanned = readTicketEntries().count { it.isTicketEntryCheckedIn() }
    if (scanned > 0) return scanned
    return if (ticketRowCheckinAt() > 0L) ticketQuantity() else 0
}

/** `classifyTicketAudience` (899). */
fun JsonObject?.classifyTicketAudience(entry: JsonObject?, user: JsonObject?): String {
    val data = obj("data")
    val category = normalizeEventBiText(
        firstText(str("itemCategory"), data.str("itemCategory"), ticketLotName()),
    )
    val lot = normalizeEventBiText(ticketLotName())
    val userId = str("userId")
    val turma = firstText(
        user.str("turma"),
        str("userTurma"),
        entry.str("holderTurma"),
        entry.str("userTurma"),
    )
    val manualGateEntry = data.truthy("manualGateEntry")

    if (category.contains("convid") || lot.contains("convid")) return "Convidado"
    if (
        manualGateEntry || isManualUserId(userId) || category.contains("porta") ||
        category.contains("extern") || category.contains("nao aluno") ||
        lot.contains("extern") || lot.contains("nao aluno")
    ) {
        return if (hasStudentClass(turma) && !manualGateEntry) "Aluno" else "Não aluno"
    }
    if (hasStudentClass(turma)) return "Aluno"
    return "Não classificado"
}

/** `classifyTicketOperationalCategory` (1719). */
fun JsonObject?.classifyTicketOperationalCategory(
    entry: JsonObject?,
    user: JsonObject?,
    member: EventBiMemberMeta?,
): String {
    val data = obj("data")
    val category = normalizeEventBiText(
        firstText(str("itemCategory"), data.str("itemCategory"), ticketLotName()),
    )
    val lot = normalizeEventBiText(ticketLotName())
    val userId = entry.ticketEntryUserId().ifBlank { str("userId") }
    val turma = firstText(
        user.str("turma"),
        str("userTurma"),
        entry.str("holderTurma"),
        entry.str("userTurma"),
    )
    val manualGateEntry = data.truthy("manualGateEntry") || isManualUserId(userId)

    if (manualGateEntry || category.contains("porta") || lot.contains("porta")) return "Entrada/porta"
    if (isCourtesyText("$category $lot ${str("itemName")}")) return "Cortesia"
    if (category.contains("convid") || lot.contains("convid")) return "Convidado"
    if (member?.management == true) return "Diretoria"
    if (member != null) return "Membro"
    if (category.contains("extern") || lot.contains("extern")) return "Externo"
    if (category.contains("nao aluno") || lot.contains("nao aluno")) return "Não aluno"
    if (hasStudentClass(turma)) return "Aluno"
    return "Não aluno"
}

// ------------------------------------------------------------------
// Transferências (1270-1388, 1071-1079)
// ------------------------------------------------------------------

/** `TransferMetricEvent` (1270). */
data class EventBiTransfer(
    val key: String,
    /** `"Manual" | "App"`. */
    val mode: String,
    /** `"Usuário da faculdade" | "Cadastro manual/externo"`. */
    val target: String,
    val actor: String,
    val atMillis: Long,
)

/** `transferModeFromAudit` (1278). */
private fun transferModeFromAudit(audit: JsonObject?, fallback: String): String {
    val manual = if (audit.truthy("manual")) "manual" else ""
    val text = normalizeEventBiText(
        "$manual ${audit.firstOf("mode", "method", "source").ifBlank { fallback }}",
    )
    return if (text.contains("manual")) "Manual" else "App"
}

/** `transferTargetFromUserId` (1285). */
private fun transferTargetFromUserId(value: String?): String =
    if (isManualUserId(value)) "Cadastro manual/externo" else "Usuário da faculdade"

/** `transferActorName` (1289). */
private fun transferActorName(audit: JsonObject?, fallback: String): String =
    audit.firstOf("byUserName", "transferByUserName", "fromUserName", "transferredFromUserName")
        .ifBlank { fallback }

/** `extractTicketTransfers` (1299). */
fun JsonObject?.extractTicketTransfers(): List<EventBiTransfer> {
    val row = this
    val data = obj("data")
    val audits = mutableListOf<JsonObject>()

    audits += objects("transferHistory")
    obj("data").obj("transferAudit")?.takeIf { it.isNotEmpty() }?.let { audits += it }

    readTicketEntries().forEach { entry ->
        audits += entry.objects("transferHistory")
        if (
            entry.str("transferredAt").isNotBlank() ||
            entry.str("transferredToUserId").isNotBlank() ||
            entry.str("transferredFromUserId").isNotBlank()
        ) {
            audits += JsonObject(
                buildMap {
                    entry["transferredAt"]?.let { put("at", it) }
                    entry["transferredFromUserId"]?.let { put("fromUserId", it) }
                    entry["transferredFromUserName"]?.let { put("fromUserName", it) }
                    (entry["transferredToUserId"] ?: row?.get("transferToUserId") ?: row?.get("userId"))
                        ?.let { put("toUserId", it) }
                    (entry["transferredToUserName"] ?: row?.get("transferToUserName") ?: row?.get("userName"))
                        ?.let { put("toUserName", it) }
                    (entry["transferByUserName"] ?: row?.get("transferByUserName"))
                        ?.let { put("byUserName", it) }
                    data?.get("manualTransfer")?.let { put("manual", it) }
                    (entry["token"] ?: entry["id"])?.let { put("ticketToken", it) }
                },
            )
        }
    }

    if (audits.isEmpty() && str("transferAt").isNotBlank()) {
        audits += JsonObject(
            buildMap {
                row?.get("transferAt")?.let { put("at", it) }
                row?.get("transferFromUserId")?.let { put("fromUserId", it) }
                row?.get("transferFromUserName")?.let { put("fromUserName", it) }
                row?.get("transferToUserId")?.let { put("toUserId", it) }
                row?.get("transferToUserName")?.let { put("toUserName", it) }
                row?.get("transferByUserName")?.let { put("byUserName", it) }
                data?.get("manualTransfer")?.let { put("manual", it) }
            },
        )
    }

    return audits.mapIndexed { index, audit ->
        val toUserId = firstText(
            audit.str("toUserId"),
            audit.str("transferredToUserId"),
            str("transferToUserId"),
            str("userId"),
        )
        val at = parseEventBiDate(
            firstText(audit.str("at"), audit.str("transferredAt"), str("transferAt")),
        )
        EventBiTransfer(
            key = audit.str("id").ifBlank {
                listOf(
                    "ticket",
                    audit.str("fromOrderId").ifBlank { str("id") },
                    audit.str("toOrderId").ifBlank { str("id") },
                    firstText(audit.str("ticketToken"), audit.str("token")).ifBlank { index.toString() },
                    firstText(audit.str("at"), audit.str("transferredAt"), str("transferAt")),
                ).joinToString(":")
            },
            mode = transferModeFromAudit(audit, str("approvalMethod")),
            target = transferTargetFromUserId(toUserId),
            actor = transferActorName(
                audit,
                firstText(str("transferByUserName"), str("userName")).ifBlank { "Sem usuário" },
            ),
            atMillis = at,
        )
    }.filter { it.actor.isNotBlank() || it.atMillis > 0L }
}

/** `extractProductTransfers` (1365): só as solicitações aceitas. */
fun JsonObject?.extractProductTransfers(): List<EventBiTransfer> {
    val eventParty = obj("data").obj("eventParty")
    return eventParty.objects("transferRequests")
        .filter { normalizeEventBiText(it.str("status")) in listOf("aceito", "accepted") }
        .mapIndexed { index, entry ->
            EventBiTransfer(
                key = listOf(
                    "product",
                    str("id"),
                    entry.str("id").ifBlank { index.toString() },
                    entry.str("voucherId"),
                    firstText(entry.str("acceptedAt"), entry.str("requestedAt")),
                ).joinToString(":"),
                mode = "App",
                target = transferTargetFromUserId(entry.str("toUserId")),
                actor = firstText(entry.str("fromUserName"), str("userName")).ifBlank { "Sem usuário" },
                atMillis = parseEventBiDate(
                    firstText(
                        entry.str("acceptedAt"),
                        entry.str("requestedAt"),
                        str("updatedAt"),
                        str("createdAt"),
                    ),
                ),
            )
        }
}

/** `ticketTransferLabel` (1071). */
fun JsonObject?.ticketTransferLabel(): String {
    val transfers = extractTicketTransfers()
    if (transfers.isNotEmpty() || str("transferAt").isNotBlank()) {
        val latest = transfers.maxByOrNull { it.atMillis }
        val target = firstText(latest?.actor?.takeIf { transfers.isNotEmpty() }, str("transferToUserName"))
        return if (target.isNotBlank()) "Transferido para $target" else "Transferido"
    }
    return "Sem transferência"
}

// ------------------------------------------------------------------
// Vouchers e pedido (1097-1521)
// ------------------------------------------------------------------

/** `productEventId` (1097). */
fun JsonObject?.productEventId(): String = firstText(
    obj("data").obj("eventParty").str("eventId"),
    firstOf("eventId", "eventoId"),
)

/** `productName` (1103). */
fun JsonObject?.productName(): String = firstOf("nome", "productName", "name").ifBlank { "Produto" }

/** `orderEventId` (1107). */
fun JsonObject?.orderEventId(): String = firstText(
    firstOf("eventId", "eventoId"),
    obj("data").obj("eventParty").str("eventId"),
)

/** `orderProductId` (1113). */
fun JsonObject?.orderProductId(): String =
    firstOf("productId", "produtoId", "product_id", "produto_id")

/** `orderQuantity` (1117): mínimo 1, sempre inteiro. */
fun JsonObject?.orderQuantity(): Int {
    val raw = listOf("quantidade", "quantity", "qtd")
        .firstNotNullOfOrNull { key -> this?.get(key)?.takeIf { it != JsonNull }?.let { num(key, 1.0) } }
        ?: 1.0
    return max(1, floor(raw).toInt())
}

/** `orderTotal` (1121): sem total explícito, pedido de evento usa o preço cheio. */
fun JsonObject?.orderTotal(): Double {
    listOf("total", "valorTotal").forEach { key ->
        this?.get(key)?.takeIf { it != JsonNull }?.let { return num(key) }
    }
    val price = listOf("price", "preco", "valor")
        .firstNotNullOfOrNull { key -> this?.get(key)?.takeIf { it != JsonNull }?.let { num(key) } }
        ?: 0.0
    return if (orderEventId().isNotBlank()) price else price * orderQuantity()
}

/** `orderDiscount` (1128). */
fun JsonObject?.orderDiscount(): Double {
    val eventParty = obj("data").obj("eventParty")
    this?.get("eventDiscountValue")?.takeIf { it != JsonNull }?.let { return num("eventDiscountValue") }
    listOf("discountValue", "desconto").forEach { key ->
        eventParty?.get(key)?.takeIf { it != JsonNull }?.let { return eventParty.num(key) }
    }
    return 0.0
}

/** `orderDiscountSource` (1134). */
fun JsonObject?.orderDiscountSource(): String {
    val eventParty = obj("data").obj("eventParty")
    val direct = firstText(
        str("eventDiscountSource"),
        eventParty.firstOf("discountSource", "discountKind"),
    )
    if (direct.isNotBlank()) return direct
    return if (orderDiscount() > 0) "Manual" else "Sem desconto"
}

/** `orderCreatedAt` (1154). */
fun JsonObject?.orderCreatedAt(): Long =
    parseEventBiDate(firstOf("createdAt", "data", "created_at"))

/** `orderApprovalDate` (1158). */
fun JsonObject?.orderApprovalDate(): Long = parseEventBiDate(
    firstText(
        str("eventApprovalAt"),
        obj("data").obj("eventParty").str("approvedAt"),
        str("updatedAt"),
    ),
)

/** `orderApproverName` (1164). */
fun JsonObject?.orderApproverName(): String {
    val eventParty = obj("data").obj("eventParty")
    return firstText(
        firstOf("approvedBy", "eventCreatedByName"),
        eventParty.firstOf("approvedByName", "createdByName"),
    ).ifBlank { "Sem aprovador" }
}

/** `orderApprovalMethod` (1170). */
fun JsonObject?.orderApprovalMethod(): String = approvalMethodLabel(
    firstText(str("eventApprovalMethod"), obj("data").obj("eventParty").str("approvalMethod")),
    if (orderApproverName() != "Sem aprovador") "Manual" else "-",
)

/** `isManualOrder` (1179). */
fun JsonObject?.isManualOrder(): Boolean {
    val eventParty = obj("data").obj("eventParty")
    return truthy("eventCreatedManually") || eventParty.truthy("manualOrder") ||
        eventParty.truthy("createdManually")
}

/** `orderSource` (1185). */
fun JsonObject?.orderSource(): String {
    val eventParty = obj("data").obj("eventParty")
    val source = firstText(eventParty.str("source"), firstOf("source", "canal_origem"))
    if (source.isNotBlank()) return source
    if (isManualOrder()) return "Criado manualmente"
    val sellerType = normalizeEventBiText(str("seller_type"))
    if (sellerType.contains("pdv")) return "PDV/bar"
    if (sellerType.contains("admin")) return "Admin"
    return "Checkout público"
}

/** `orderBuyerId` (1197). */
fun JsonObject?.orderBuyerId(): String =
    firstOf("userId", "user_id", "email", "userName", "id").ifBlank { "pedido-${str("id")}" }

/** `orderItemName` (1201). */
fun JsonObject?.orderItemName(product: JsonObject?): String =
    firstText(firstOf("eventItemName", "productName"), product.str("nome")).ifBlank { "Produto" }

/** `orderItemCategory` (1206). */
fun JsonObject?.orderItemCategory(product: JsonObject?): String = firstText(
    str("eventItemCategory"),
    obj("data").obj("eventParty").str("section"),
    product.str("categoria"),
).ifBlank { "Sem categoria" }

/** `readVoucherEntries` (1213). */
fun JsonObject?.readVoucherEntries(): List<JsonObject> {
    val eventParty = obj("data").obj("eventParty") ?: return emptyList()
    (eventParty["voucherEntries"] as? JsonArray)?.let { return it.mapNotNull { entry -> entry as? JsonObject } }
    (eventParty["vouchers"] as? JsonArray)?.let { return it.mapNotNull { entry -> entry as? JsonObject } }
    return emptyList()
}

/** `activeVoucherEntries` (1224). */
fun JsonObject?.activeVoucherEntries(): List<JsonObject> =
    readVoucherEntries().filter { !it.isTransferredEntry() && !it.isCancelledEntry() }

/** `isRedeemedEntry` (1390). */
fun JsonObject?.isRedeemedEntry(): Boolean {
    val status = normalizeEventBiText(firstOf("status", "situacao"))
    return str("usedAt").isNotBlank() || str("withdrawalAt").isNotBlank() ||
        str("scannedAt").isNotBlank() || status.contains("utilizado") ||
        status.contains("inativo") || status.contains("retirado") ||
        status.contains("redeemed") || status.contains("used")
}

/** `orderRedeemedQuantity` (1404). */
fun JsonObject?.orderRedeemedQuantity(): Int {
    val redeemed = readVoucherEntries().count { it.isRedeemedEntry() }
    if (redeemed > 0) return min(orderQuantity(), redeemed)
    if (str("eventCheckinAt").isNotBlank() || normalizeEventBiText(str("status")) == "redeemed") {
        return orderQuantity()
    }
    return 0
}

/** `orderAudienceQuantity` (1228). */
fun JsonObject?.orderAudienceQuantity(): Int {
    val entries = readVoucherEntries()
    if (entries.isNotEmpty()) return activeVoucherEntries().size
    val eventParty = obj("data").obj("eventParty")
    if (eventParty.truthy("transferOrder") && orderTotal() <= 0) return max(1, orderQuantity())
    return orderQuantity()
}

/** `orderCheckedInAudienceQuantity` (1237). */
fun JsonObject?.orderCheckedInAudienceQuantity(): Int {
    val entries = activeVoucherEntries()
    if (entries.isNotEmpty()) return entries.count { it.isRedeemedEntry() }
    return orderRedeemedQuantity()
}

/** `classifyOrderAudience` (1243). */
fun JsonObject?.classifyOrderAudience(user: JsonObject?): String {
    val eventParty = obj("data").obj("eventParty")
    val manualCustomer = eventParty.obj("manualCustomer")
    val userId = str("userId")
    val turma = firstText(
        user.str("turma"),
        str("userTurma"),
        eventParty.str("userTurma"),
        manualCustomer.str("turma"),
    )
    val category = normalizeEventBiText(
        firstText(
            str("eventItemCategory"),
            eventParty.str("section"),
            str("productName"),
            str("productname"),
        ),
    )

    if (category.contains("convid")) return "Convidado"
    if (hasStudentClass(turma)) return "Aluno"
    if (
        isManualUserId(userId) || isManualOrder() || eventParty.truthy("manualOrder") ||
        eventParty.truthy("createdManually") || eventParty.truthy("externalNumber") ||
        manualCustomer.truthy("externalNumber") || manualCustomer.truthy("cpf") ||
        manualCustomer.truthy("telefone") || manualCustomer.truthy("email")
    ) {
        return "Não aluno"
    }
    return "Não classificado"
}

/** `orderWithdrawalDate` (1412). */
fun JsonObject?.orderWithdrawalDate(): Long {
    val dates = readVoucherEntries()
        .map { parseEventBiDate(it.firstOf("usedAt", "withdrawalAt", "scannedAt")) }
        .filter { it > 0L }
    if (dates.isNotEmpty()) return dates.max()
    return parseEventBiDate(
        firstText(str("eventCheckinAt"), obj("data").obj("eventParty").str("usedAt")),
    )
}

/** `orderWithdrawalMethod` (1424). */
fun JsonObject?.orderWithdrawalMethod(): String {
    val eventParty = obj("data").obj("eventParty")
    val entryMethod = readVoucherEntries()
        .firstNotNullOfOrNull { entry ->
            entry.firstOf("usedMethod", "withdrawalMethod", "scanSource").takeIf { it.isNotBlank() }
        }
        .orEmpty()
    val method = normalizeEventBiText(
        firstText(str("eventCheckinMethod"), entryMethod, eventParty.str("usedMethod")),
    )
    return when {
        method.contains("manual") -> "Manual"
        method.contains("codigo") || method.contains("code") -> "Código curto"
        method.contains("document") -> "Documento"
        method.contains("lista") -> "Lista nominal"
        method.contains("transfer") -> "Transferido"
        method.contains("qr") || method.contains("scan") -> "QR code"
        orderRedeemedQuantity() > 0 -> "QR code"
        else -> "-"
    }
}

/** `orderWithdrawalOperator` (1440). */
fun JsonObject?.orderWithdrawalOperator(): String {
    val eventParty = obj("data").obj("eventParty")
    val direct = firstText(str("eventCheckinByUserName"), eventParty.str("usedByUserName"))
    if (direct.isNotBlank()) return direct
    return readVoucherEntries()
        .firstNotNullOfOrNull { entry ->
            entry.firstOf("usedByUserName", "withdrawalByUserName", "scannedByUserName")
                .takeIf { it.isNotBlank() }
        }
        ?: "-"
}

/** `orderHasCode` (1452). */
fun JsonObject?.orderHasCode(): Boolean = readVoucherEntries().any { entry ->
    entry.firstOf("code", "manualNumber", "token", "id").isNotBlank()
}

/** `orderPaymentSource` (1458). */
fun JsonObject?.orderPaymentSource(): String {
    val eventParty = obj("data").obj("eventParty")
    val config = obj("payment_config") ?: obj("paymentConfig")
    val direct = firstText(
        firstOf("paymentSource", "paymentMethod"),
        eventParty.firstOf("paymentSource", "paymentMethod"),
        config.firstOf("method", "provider"),
    )
    if (direct.isNotBlank()) return direct
    return if (this?.get("payment_config") != null && this["payment_config"] != JsonNull) {
        "Configuração de pagamento"
    } else {
        "-"
    }
}

/** `orderQrStatus` (1468). */
fun JsonObject?.orderQrStatus(): String {
    val entries = readVoucherEntries()
    val eventParty = obj("data").obj("eventParty")
    val direct = firstText(
        eventParty.firstOf("voucherStatus", "qrStatus"),
        firstOf("qrStatus", "statusQr"),
    )
    if (direct.isNotBlank()) return direct
    if (entries.isEmpty()) return if (orderHasCode()) "Ativo" else "Sem QR (Quick Response)"
    val redeemed = entries.count { it.isRedeemedEntry() }
    val cancelled = entries.count { it.isCancelledEntry() }
    val invalid = entries.count {
        normalizeEventBiText(it.firstOf("status", "situacao")).contains("inval")
    }
    return when {
        redeemed >= entries.size -> "Utilizado"
        redeemed > 0 -> "Parcial"
        cancelled >= entries.size -> "Cancelado"
        invalid > 0 -> "Inválido"
        else -> "Ativo"
    }
}

/** `orderCodes` (1485). */
fun JsonObject?.orderCodes(): List<String> {
    val eventParty = obj("data").obj("eventParty")
    val direct = listOf(
        eventParty.str("orderCode"),
        eventParty.str("orderNumber"),
        eventParty.str("manualCode"),
        eventParty.str("manualNumber"),
        eventParty.str("fichaNumero"),
        str("code"),
        str("codigo"),
    ).filter { it.isNotBlank() }
    val entryCodes = readVoucherEntries().flatMap { entry ->
        listOf(entry.str("code"), entry.str("manualNumber"), entry.str("token"), entry.str("id"))
    }.filter { it.isNotBlank() }
    return (direct + entryCodes).distinct()
}

/** `orderCreatedByName` (1506). */
fun JsonObject?.orderCreatedByName(): String = firstText(
    str("eventCreatedByName"),
    obj("data").obj("eventParty").str("createdByName"),
    obj("data").str("createdByName"),
).ifBlank { "-" }

/** `orderClassName` (1512). */
fun JsonObject?.orderClassName(user: JsonObject?): String {
    val eventParty = obj("data").obj("eventParty")
    return firstText(
        str("userTurma"),
        user.str("turma"),
        eventParty.str("userTurma"),
        eventParty.obj("manualCustomer").str("turma"),
    ).ifBlank { "Sem turma" }
}

// ------------------------------------------------------------------
// Evento: capacidade, custo, lotes e preço esperado (924-961, 1523-1639)
// ------------------------------------------------------------------

/** `eventCapacity` (924): capacidade explícita ou a soma dos lotes. */
fun JsonObject?.eventCapacity(): Int {
    val dataExtra = obj("data_extra")
    val eventParty = dataExtra.obj("eventParty")
    val stats = obj("stats")
    val explicit = listOf(
        num("capacidade"), num("capacity"), num("vagas"),
        dataExtra.num("capacidade"), dataExtra.num("capacity"), dataExtra.num("capacidadeTotal"),
        dataExtra.num("totalCapacity"), dataExtra.num("eventCapacity"),
        eventParty.num("capacidade"), eventParty.num("capacity"),
        eventParty.num("capacidadeTotal"), eventParty.num("totalCapacity"),
        stats.num("capacidade"), stats.num("capacity"), stats.num("vagas"),
    ).firstOrNull { it > 0 }
    if (explicit != null) return explicit.toInt()

    val lots = listOf("lotes", "batches", "tickets")
        .firstNotNullOfOrNull { key -> objects(key).takeIf { it.isNotEmpty() } }
        .orEmpty()
    return lots.sumOf { lot ->
        listOf("quantidade", "capacidade", "capacity", "limite", "total", "estoque", "vagas")
            .firstNotNullOfOrNull { key -> lot.num(key, 0.0).takeIf { it != 0.0 } } ?: 0.0
    }.toInt()
}

/** `eventLotRows` (1605): `lotes || lots || data_extra.lotes || data_extra.lots || stats.lotes`. */
fun JsonObject?.eventLotRows(): List<JsonObject> {
    val dataExtra = obj("data_extra")
    val stats = obj("stats")
    (this?.get("lotes") as? JsonArray)?.let { return it.mapNotNull { entry -> entry as? JsonObject } }
    (this?.get("lots") as? JsonArray)?.let { return it.mapNotNull { entry -> entry as? JsonObject } }
    (dataExtra?.get("lotes") as? JsonArray)?.let { return it.mapNotNull { entry -> entry as? JsonObject } }
    (dataExtra?.get("lots") as? JsonArray)?.let { return it.mapNotNull { entry -> entry as? JsonObject } }
    (stats?.get("lotes") as? JsonArray)?.let { return it.mapNotNull { entry -> entry as? JsonObject } }
    return emptyList()
}

/** `eventLotUnitPrice` (1618): casa por id ou por nome normalizado; `NaN` quando não acha. */
fun JsonObject?.eventLotUnitPrice(lotId: String?, lotName: String?): Double {
    val cleanLotId = lotId?.trim().orEmpty()
    val cleanLotName = normalizeEventBiText(lotName)
    val lot = eventLotRows().firstOrNull { entry ->
        val idMatches = cleanLotId.isNotBlank() && entry.str("id") == cleanLotId
        val nameMatches = cleanLotName.isNotBlank() &&
            normalizeEventBiText(entry.firstOf("nome", "name", "loteNome")) == cleanLotName
        idMatches || nameMatches
    } ?: return Double.NaN
    return listOf("preco", "price", "valor", "value")
        .firstNotNullOfOrNull { key -> lot[key]?.takeIf { it != JsonNull }?.let { lot.num(key) } }
        ?: Double.NaN
}

/** `expectedTicketTotal` (1629). */
fun JsonObject?.expectedTicketTotal(relatedEvent: JsonObject?): Double {
    val lotPrice = relatedEvent.eventLotUnitPrice(str("loteId"), ticketLotName())
    val unit = if (lotPrice.isFinite()) {
        lotPrice
    } else {
        listOf("valorUnitario", "unitPrice")
            .firstNotNullOfOrNull { key -> this?.get(key)?.takeIf { it != JsonNull }?.let { num(key) } }
            ?: Double.NaN
    }
    return if (unit.isFinite()) unit * ticketQuantity() else Double.NaN
}

/** `expectedOrderTotal` (1635). */
fun JsonObject?.expectedOrderTotal(product: JsonObject?): Double {
    val unit = listOf("preco" to product, "price" to product, "price" to this, "preco" to this)
        .firstNotNullOfOrNull { (key, source) ->
            source?.get(key)?.takeIf { it != JsonNull }?.let { source.num(key) }
        } ?: Double.NaN
    return if (unit.isFinite()) unit * orderQuantity() else Double.NaN
}

/** Chaves de custo lidas por `eventCost` (1523) e `hasEventCostField` (1566). */
private val CostKeys = listOf("custo", "custos", "cost", "totalCost", "custoTotal", "valorCusto")

private fun JsonObject?.costCandidates(): List<JsonElement?> {
    val dataExtra = obj("data_extra")
    val eventParty = dataExtra.obj("eventParty")
    val stats = obj("stats")
    return CostKeys.map { this?.get(it) } +
        listOf("custo", "custos", "cost", "totalCost").flatMap { key ->
            listOf(dataExtra?.get(key), eventParty?.get(key), stats?.get(key))
        }
}

/** `eventCost` (1523): soma número, lista de itens e objeto de rubricas. */
fun JsonObject?.eventCost(): Double = costCandidates().sumOf { candidate ->
    when (candidate) {
        is JsonArray -> candidate.sumOf { entry ->
            val row = entry as? JsonObject
            listOf("valor", "value", "total", "custo", "cost")
                .firstNotNullOfOrNull { key -> row?.get(key)?.let { row.num(key) } }
                ?: parseEventBiNumber(entry.asText())
        }
        is JsonObject -> candidate.values.sumOf { parseEventBiNumber(it.asText()) }
        is JsonPrimitive -> if (candidate.isString) {
            parseEventBiNumber(candidate.contentOrNull)
        } else {
            candidate.contentOrNull?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
        }
        else -> 0.0
    }
}

/** `hasEventCostField` (1566): o campo existe, mesmo que zerado. */
fun JsonObject?.hasEventCostField(): Boolean = costCandidates().any { candidate ->
    when (candidate) {
        null, JsonNull -> false
        is JsonArray -> candidate.isNotEmpty()
        is JsonObject -> candidate.isNotEmpty()
        is JsonPrimitive -> if (candidate.isString) candidate.content.isNotBlank() else true
        else -> true
    }
}

// ------------------------------------------------------------------
// Entidades e membros (1674-1717)
// ------------------------------------------------------------------

/** `EntityMemberMeta` (1652). */
data class EventBiMemberMeta(
    val roles: List<String> = emptyList(),
    val scopeTypes: List<EventBiScope> = emptyList(),
    val management: Boolean = false,
)

/** `entityMemberRows` (1674): membros com cargo + ids soltos. */
fun JsonObject?.entityMemberRows(): List<Pair<String, String>> {
    val data = obj("data")
    val members = (objects("membros") + data.objects("membros") + data.objects("members"))
        .map { member ->
            val id = member.firstOf("id", "uid", "userId", "user_id")
            val role = member.firstOf("cargo", "role", "funcao", "position").ifBlank { "Membro" }
            id to role
        }
        .filter { it.first.isNotBlank() }

    val memberIds = (arr("membrosIds") + arr("memberIds") + data.arr("membrosIds") + data.arr("memberIds"))
        .map { it.asText() }
        .filter { it.isNotBlank() }
        .map { it to "Membro" }

    return (members + memberIds).distinct()
}

/** `buildEntityMemberIndex` (1701): id de usuário -> cargos, escopos e se é diretoria. */
fun buildEventBiMemberIndex(rows: List<Pair<JsonObject, EventBiScope>>): Map<String, EventBiMemberMeta> {
    val index = mutableMapOf<String, EventBiMemberMeta>()
    rows.forEach { (row, scopeType) ->
        row.entityMemberRows().forEach { (memberId, role) ->
            val current = index[memberId] ?: EventBiMemberMeta()
            index[memberId] = current.copy(
                roles = if (role in current.roles) current.roles else current.roles + role,
                scopeTypes = if (scopeType in current.scopeTypes) {
                    current.scopeTypes
                } else {
                    current.scopeTypes + scopeType
                },
                management = current.management || isManagementEntityRole(role),
            )
        }
    }
    return index
}

// ------------------------------------------------------------------
// Rótulos de tempo (1081-1095)
// ------------------------------------------------------------------

/** `formatDateTimeShort` (1081): `dd/MM HH:mm`, ou "-". */
fun formatEventBiDateTimeShort(millis: Long): String {
    if (millis <= 0L) return "-"
    val local = java.time.LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(millis),
        java.time.ZoneId.systemDefault(),
    )
    return "%02d/%02d %02d:%02d".format(
        local.dayOfMonth, local.monthValue, local.hour, local.minute,
    )
}

/** `minuteBucketLabel` (1087): janela de N minutos, `08h00-08h15`. */
fun eventBiMinuteBucketLabel(millis: Long, intervalMinutes: Int): String {
    val local = java.time.LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(millis),
        java.time.ZoneId.systemDefault(),
    )
    val startMinute = (local.minute / intervalMinutes) * intervalMinutes
    val start = local.withMinute(startMinute).withSecond(0).withNano(0)
    val end = start.plusMinutes(intervalMinutes.toLong())
    return "%02dh%02d-%02dh%02d".format(start.hour, start.minute, end.hour, end.minute)
}

/** `hourKey` (4328): `dd/MM HHh`, chave de hora cheia usada na cobertura de demanda. */
fun eventBiHourKey(millis: Long): String {
    if (millis <= 0L) return ""
    val local = java.time.LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(millis),
        java.time.ZoneId.systemDefault(),
    )
    return "%02d/%02d %02dh".format(local.dayOfMonth, local.monthValue, local.hour)
}

/** Início do dia local, usado como `sortValue` das linhas diárias (5981). */
fun eventBiDaySortValue(millis: Long): Double {
    if (millis <= 0L) return Long.MAX_VALUE.toDouble()
    val local = java.time.LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(millis),
        java.time.ZoneId.systemDefault(),
    )
    return local.toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant().toEpochMilli().toDouble()
}
