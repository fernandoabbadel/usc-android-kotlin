package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminActivityLogRecord
import com.example.usc1.domain.model.AdminActivityLogsCatalog
import com.example.usc1.domain.model.AdminActivityLogsPage
import com.example.usc1.domain.repository.AdminActivityLogsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseAdminActivityLogsRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminActivityLogsRepository {
    override suspend fun fetchAdminActivityLogsPage(
        pageSize: Int,
        cursorId: String?,
        forceRefresh: Boolean,
    ): AdminActivityLogsPage = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val safePageSize = pageSize.coerceIn(1, AdminActivityLogsCatalog.MaxActivityLogResults)
        val offset = cursorId?.toIntOrNull()?.takeIf { it >= 0 } ?: 0
        val rows = runCatching {
            fetchRows(client = client, tenantId = tenantId, offset = offset, pageSize = safePageSize, withOrder = true)
        }.recoverCatching { error ->
            val missingColumn = extractMissingColumn(error)
            if (missingColumn.isBlank()) throw error
            fetchRows(client = client, tenantId = tenantId, offset = offset, pageSize = safePageSize, withOrder = false)
                .sortedByDescending { it.timestamp.toMillis() }
        }.getOrThrow()

        val hasMore = rows.size > safePageSize
        AdminActivityLogsPage(
            logs = rows.take(safePageSize),
            page = offset / safePageSize + 1,
            pageSize = safePageSize,
            nextCursor = if (hasMore) (offset + safePageSize).toString() else null,
            hasMore = hasMore,
        )
    }

    private suspend fun fetchRows(
        client: SupabaseClient,
        tenantId: String,
        offset: Int,
        pageSize: Int,
        withOrder: Boolean,
    ): List<AdminActivityLogRecord> {
        val query = client.from(ActivityLogsTable)
            .select(columns = Columns.raw(ActivityLogColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                if (withOrder) {
                    order(column = "timestamp", order = Order.DESCENDING)
                }
                range(offset.toLong()..(offset + pageSize).toLong())
            }
        return query.decodeList<ActivityLogRow>().mapNotNull { it.toDomain() }
    }

    private fun extractMissingColumn(error: Throwable): String {
        val message = error.message.orEmpty()
        val patterns = listOf(
            Regex("column\\s+[a-z0-9_]+\\.([a-z0-9_]+)\\s+does not exist", RegexOption.IGNORE_CASE),
            Regex("column\\s+([a-z0-9_]+)\\s+does not exist", RegexOption.IGNORE_CASE),
            Regex("could not find the [\"']?([a-z0-9_]+)[\"']? column", RegexOption.IGNORE_CASE),
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(message)?.groupValues?.getOrNull(1)
        }.orEmpty()
    }

    private companion object {
        const val ActivityLogsTable = "activity_logs"
        const val ActivityLogColumns = "id,userId,userName,action,resource,details,timestamp"
    }
}

@Serializable
private data class ActivityLogRow(
    val id: String = "",
    @SerialName("userId") val userId: String? = null,
    @SerialName("userName") val userName: String? = null,
    val action: String? = null,
    val resource: String? = null,
    val details: String? = null,
    val timestamp: String? = null,
) {
    fun toDomain(): AdminActivityLogRecord? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        return AdminActivityLogRecord(
            id = cleanId,
            userId = userId?.trim().orEmpty(),
            userName = userName?.trim().orEmpty().ifBlank { "Sistema" },
            action = action?.trim().orEmpty().ifBlank { "UNKNOWN" },
            resource = resource?.trim().orEmpty().ifBlank { "Sistema" },
            details = details?.trim().orEmpty(),
            timestamp = timestamp?.trim().orEmpty(),
        )
    }
}

private fun String.toMillis(): Long {
    val value = trim()
    if (value.isBlank()) return 0L
    return runCatching { Instant.parse(value).toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .getOrDefault(0L)
}
