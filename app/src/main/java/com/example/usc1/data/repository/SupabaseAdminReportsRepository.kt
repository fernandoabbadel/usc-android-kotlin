package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminReportItem
import com.example.usc1.domain.model.AdminReportOrigin
import com.example.usc1.domain.model.AdminReportStatus
import com.example.usc1.domain.model.AdminReportsPage
import com.example.usc1.domain.model.AdminReportsSection
import com.example.usc1.domain.repository.AdminReportsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseAdminReportsRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminReportsRepository {
    override suspend fun getReports(
        section: AdminReportsSection,
        limit: Int,
        forceRefresh: Boolean,
    ): AdminReportsPage = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val safeLimit = limit.coerceIn(1, MaxReports)
        val rows = when (section) {
            AdminReportsSection.Banned -> fetchBannedAppeals(client, tenantId, safeLimit)
            AdminReportsSection.Support -> fetchSupportRequests(client, tenantId, safeLimit, onlyGymReports = false)
            AdminReportsSection.Community -> fetchCommunityReports(client, tenantId, safeLimit)
            AdminReportsSection.Gym -> fetchSupportRequests(client, tenantId, safeLimit, onlyGymReports = true)
        }.sortedByDescending(AdminReportItem::createdAtMs)

        AdminReportsPage(
            tenantId = tenantId,
            section = section,
            rows = rows,
        )
    }

    override suspend fun resolveReport(
        report: AdminReportItem,
        response: String,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanResponse = response.trim().take(MaxResponseLength)
        if (report.id.isBlank() || cleanResponse.isBlank()) return@withContext
        if (report.origin == AdminReportOrigin.CommunityReports) {
            throw IllegalStateException("Esta rota web não resolve denúncias de comunidade diretamente.")
        }
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val now = OffsetDateTime.now().toString()

        client.from(report.origin.tableName)
            .update(
                mapOf(
                    "response" to cleanResponse,
                    "status" to AdminReportStatus.Resolved.remoteValue,
                    "readByAdmin" to true,
                    "resolvedAt" to now,
                    "updatedAt" to now,
                ),
            ) {
                filter {
                    eq("id", report.id)
                    eq("tenant_id", tenantId)
                }
            }

        if (report.reporterId.isNotBlank()) {
            client.from(NotificationsTable)
                .insert(
                    mapOf(
                        "userId" to report.reporterId,
                        "tenant_id" to tenantId,
                        "title" to if (report.origin == AdminReportOrigin.BannedAppeals) {
                            "Apelação analisada"
                        } else {
                            "Chamado atualizado"
                        },
                        "message" to if (report.origin == AdminReportOrigin.BannedAppeals) {
                            "Sua apelação de bloqueio recebeu resposta da diretoria."
                        } else {
                            "O suporte respondeu seu chamado."
                        },
                        "link" to if (report.origin == AdminReportOrigin.BannedAppeals) {
                            "/banned"
                        } else {
                            "/configuracoes/suporte"
                        },
                        "read" to false,
                        "type" to if (report.origin == AdminReportOrigin.BannedAppeals) {
                            "appeal_response"
                        } else {
                            "support_response"
                        },
                        "createdAt" to now,
                    ),
                )
        }
    }

    override suspend fun deleteReport(report: AdminReportItem): Unit = withContext(Dispatchers.IO) {
        if (report.id.isBlank()) return@withContext
        if (report.origin == AdminReportOrigin.CommunityReports) {
            throw IllegalStateException("Esta rota web não exclui denúncias de comunidade diretamente.")
        }
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        client.from(report.origin.tableName)
            .delete {
                filter {
                    eq("id", report.id)
                    eq("tenant_id", tenantId)
                }
            }
    }

    private suspend fun fetchBannedAppeals(
        client: SupabaseClient,
        tenantId: String,
        limit: Int,
    ): List<AdminReportItem> {
        return client.from(BannedAppealsTable)
            .select(columns = Columns.raw("id,userName,userId,message,status,response,createdAt,createdAtMs,updatedAt,tenant_id")) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = limit.toLong())
            }
            .decodeList<BannedAppealRow>()
            .mapNotNull { it.toDomain() }
    }

    private suspend fun fetchSupportRequests(
        client: SupabaseClient,
        tenantId: String,
        limit: Int,
        onlyGymReports: Boolean,
    ): List<AdminReportItem> {
        val rows = client.from(SupportRequestsTable)
            .select(columns = Columns.raw("id,userId,userName,category,subject,message,module,status,response,createdAt,createdAtMs,updatedAt,tenant_id")) {
                filter {
                    eq("tenant_id", tenantId)
                    if (onlyGymReports) {
                        eq("category", "denuncia")
                    }
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = limit.toLong())
            }
            .decodeList<SupportRequestRow>()

        return rows.mapNotNull { row ->
            if (onlyGymReports) row.toGymDomain() else row.toSupportDomain()
        }
    }

    private suspend fun fetchCommunityReports(
        client: SupabaseClient,
        tenantId: String,
        limit: Int,
    ): List<AdminReportItem> {
        return client.from(CommunityReportsTable)
            .select(columns = Columns.raw("id,reporterId,reporterName,targetId,targetType,reason,content,status,timestamp,tenant_id")) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "timestamp", order = Order.DESCENDING)
                limit(count = limit.toLong())
            }
            .decodeList<CommunityReportRow>()
            .mapNotNull { it.toDomain() }
    }

    private companion object {
        const val BannedAppealsTable = "banned_appeals"
        const val SupportRequestsTable = "support_requests"
        const val CommunityReportsTable = "denuncias"
        const val NotificationsTable = "notifications"
        const val MaxReports = 240
        const val MaxResponseLength = 2_000
    }
}

@Serializable
private data class BannedAppealRow(
    val id: String = "",
    @SerialName("userName") val userName: String? = null,
    @SerialName("userId") val userId: String? = null,
    val message: String? = null,
    val status: String? = null,
    val response: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("createdAtMs") val createdAtMs: Long? = null,
) {
    fun toDomain(): AdminReportItem? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val ms = createdAtMs ?: createdAt.toMillis()
        return AdminReportItem(
            id = cleanId,
            section = AdminReportsSection.Banned,
            origin = AdminReportOrigin.BannedAppeals,
            author = userName?.trim().orEmpty().ifBlank { "Usuário Desconhecido" },
            reason = "Solicitação de Desbloqueio",
            description = message?.trim().orEmpty().take(5_000),
            dateLabel = ms.toDateLabel(createdAt),
            createdAtMs = ms,
            status = AdminReportStatus.fromRemote(status),
            adminResponse = response?.trim().orEmpty(),
            reporterId = userId?.trim().orEmpty(),
            targetId = "",
            targetType = "",
        )
    }
}

@Serializable
private data class SupportRequestRow(
    val id: String = "",
    @SerialName("userId") val userId: String? = null,
    @SerialName("userName") val userName: String? = null,
    val category: String? = null,
    val subject: String? = null,
    val message: String? = null,
    val module: String? = null,
    val status: String? = null,
    val response: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("createdAtMs") val createdAtMs: Long? = null,
) {
    fun toSupportDomain(): AdminReportItem? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val ms = createdAtMs ?: createdAt.toMillis()
        return AdminReportItem(
            id = cleanId,
            section = AdminReportsSection.Support,
            origin = AdminReportOrigin.SupportRequests,
            author = userName?.trim().orEmpty().ifBlank { "Usuário" },
            reason = subject?.trim().orEmpty().ifBlank { "Suporte" },
            description = message?.trim().orEmpty().take(5_000),
            dateLabel = ms.toDateLabel(createdAt),
            createdAtMs = ms,
            status = AdminReportStatus.fromRemote(status),
            adminResponse = response?.trim().orEmpty(),
            reporterId = userId?.trim().orEmpty(),
            targetId = "",
            targetType = "",
        )
    }

    fun toGymDomain(): AdminReportItem? {
        val cleanId = id.trim()
        if (cleanId.isBlank() || !isGymRelated()) return null
        val ms = createdAtMs ?: createdAt.toMillis()
        val subjectLabel = subject?.trim().orEmpty().ifBlank { "Denúncia Gym" }
        val messageText = message?.trim().orEmpty()
        return AdminReportItem(
            id = cleanId,
            section = AdminReportsSection.Gym,
            origin = AdminReportOrigin.SupportRequests,
            author = userName?.trim().orEmpty().ifBlank { "Usuário" },
            reason = subjectLabel,
            description = listOf(subjectLabel, messageText).filter(String::isNotBlank).joinToString(" - ").take(5_000),
            dateLabel = ms.toDateLabel(createdAt),
            createdAtMs = ms,
            status = AdminReportStatus.fromRemote(status),
            adminResponse = response?.trim().orEmpty(),
            reporterId = userId?.trim().orEmpty(),
            targetId = "",
            targetType = "",
        )
    }

    private fun isGymRelated(): Boolean {
        val moduleHint = module?.lowercase().orEmpty()
        if ("gym" in moduleHint || "treino" in moduleHint) return true
        val joined = "${subject.orEmpty()} ${message.orEmpty()}".lowercase()
        return listOf("gym", "academia", "treino", "checkin", "check-in", "qr").any(joined::contains)
    }
}

@Serializable
private data class CommunityReportRow(
    val id: String = "",
    @SerialName("reporterId") val reporterId: String? = null,
    @SerialName("reporterName") val reporterName: String? = null,
    @SerialName("targetId") val targetId: String? = null,
    @SerialName("targetType") val targetType: String? = null,
    val reason: String? = null,
    val content: String? = null,
    val status: String? = null,
    val timestamp: String? = null,
) {
    fun toDomain(): AdminReportItem? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val ms = timestamp.toMillis()
        val reasonText = reason?.trim().orEmpty()
        val contentText = content?.trim().orEmpty()
        return AdminReportItem(
            id = cleanId,
            section = AdminReportsSection.Community,
            origin = AdminReportOrigin.CommunityReports,
            author = reporterName?.trim().orEmpty().ifBlank { "Usuário" },
            reason = reasonText,
            description = listOf(reasonText, contentText).filter(String::isNotBlank).joinToString(" - ").take(5_000)
                .ifBlank { "Conteúdo denunciado na comunidade." },
            dateLabel = ms.toDateLabel(timestamp),
            createdAtMs = ms,
            status = AdminReportStatus.fromRemote(status),
            adminResponse = "",
            reporterId = reporterId?.trim().orEmpty(),
            targetId = targetId?.trim().orEmpty(),
            targetType = targetType?.trim().orEmpty(),
        )
    }
}

private fun String?.toMillis(): Long {
    val value = this?.trim().orEmpty()
    if (value.isBlank()) return 0L
    return runCatching { Instant.parse(value).toEpochMilli() }
        .getOrElse {
            runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrDefault(0L)
        }
}

private fun Long.toDateLabel(rawFallback: String?): String {
    if (this <= 0L) return rawFallback?.trim().orEmpty().ifBlank { "Data desconhecida" }
    return runCatching {
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale("pt", "BR")))
    }.getOrElse { "Data desconhecida" }
}
