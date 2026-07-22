package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminDashboardActivityLog
import com.example.usc1.domain.model.AdminDashboardBundle
import com.example.usc1.domain.model.AdminDashboardRecentUser
import com.example.usc1.domain.model.AdminDashboardStats
import com.example.usc1.domain.repository.AdminDashboardRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseAdminDashboardRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminDashboardRepository {
    override suspend fun getDashboardBundle(
        usersLimit: Int,
        logsLimit: Int,
        forceRefresh: Boolean,
    ): AdminDashboardBundle = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val safeUsersLimit = usersLimit.coerceIn(1, MaxRecentUsers)
        val safeLogsLimit = logsLimit.coerceIn(1, MaxRecentLogs)

        val usersCount = countTable(client, UsersTable, "uid", tenantId)
        val eventsCount = countTable(client, EventsTable, "id", tenantId)
        val salesCount = countTable(client, OrdersTable, "id", tenantId)
            ?: countTable(client, StoreOrdersTable, "id", tenantId)

        AdminDashboardBundle(
            stats = AdminDashboardStats(
                totalUsers = usersCount ?: 0,
                totalEvents = eventsCount ?: 0,
                totalSales = salesCount,
                activeChamps = null,
            ),
            recentUsers = fetchRecentUsers(client, tenantId, safeUsersLimit),
            recentActivity = fetchRecentActivity(client, tenantId, safeLogsLimit),
        )
    }

    private suspend fun countTable(
        client: SupabaseClient,
        tableName: String,
        countColumn: String,
        tenantId: String,
    ): Long? {
        return runCatching {
            client.from(tableName)
                .select(columns = Columns.raw(countColumn)) {
                    head = true
                    count(Count.PLANNED)
                    filter {
                        eq("tenant_id", tenantId)
                    }
                }
                .countOrNull()
        }.getOrNull()
    }

    private suspend fun fetchRecentUsers(
        client: SupabaseClient,
        tenantId: String,
        limit: Int,
    ): List<AdminDashboardRecentUser> {
        return runCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw(UserColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                    }
                    order(column = "data_adesao", order = Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList<AdminDashboardUserRow>()
        }.recoverCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw(UserColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                    }
                    order(column = "createdAt", order = Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList<AdminDashboardUserRow>()
        }.recoverCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw(UserColumnsWithoutDates)) {
                    filter {
                        eq("tenant_id", tenantId)
                    }
                    limit(count = limit.toLong())
                }
                .decodeList<AdminDashboardUserRow>()
        }.getOrDefault(emptyList())
            .mapNotNull { row -> row.toDomain() }
    }

    private suspend fun fetchRecentActivity(
        client: SupabaseClient,
        tenantId: String,
        limit: Int,
    ): List<AdminDashboardActivityLog> {
        return runCatching {
            client.from(ActivityLogsTable)
                .select(columns = Columns.raw(ActivityColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                    }
                    order(column = "timestamp", order = Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList<AdminDashboardActivityRow>()
        }.recoverCatching {
            client.from(ActivityLogsTable)
                .select(columns = Columns.raw(ActivityColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                    }
                    order(column = "createdAt", order = Order.DESCENDING)
                    limit(count = limit.toLong())
                }
                .decodeList<AdminDashboardActivityRow>()
        }.recoverCatching {
            client.from(ActivityLogsTable)
                .select(columns = Columns.raw(ActivityColumnsWithoutDates)) {
                    filter {
                        eq("tenant_id", tenantId)
                    }
                    limit(count = limit.toLong())
                }
                .decodeList<AdminDashboardActivityRow>()
        }.getOrDefault(emptyList())
            .mapNotNull { row -> row.toDomain() }
    }

    private fun AdminDashboardUserRow.toDomain(): AdminDashboardRecentUser? {
        val cleanId = uid.trim()
        if (cleanId.isBlank()) return null
        return AdminDashboardRecentUser(
            id = cleanId,
            name = nome.trim().ifBlank { "Sem Nome" },
            email = email?.trim().orEmpty().ifBlank { "---" },
            avatarUrl = foto?.trim()?.takeIf { it.isNotBlank() },
            className = turma?.trim().orEmpty().ifBlank { "---" },
            role = tenantRole?.trim()?.takeIf { it.isNotBlank() } ?: role?.trim().orEmpty(),
            createdAt = dataAdesao ?: createdAt,
        )
    }

    private fun AdminDashboardActivityRow.toDomain(): AdminDashboardActivityLog? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        return AdminDashboardActivityLog(
            id = cleanId,
            userName = userName?.trim().orEmpty().ifBlank { "Sistema" },
            action = action?.trim().orEmpty().ifBlank { "UPDATE" },
            resource = resource?.trim().orEmpty().ifBlank { "app" },
            timestamp = timestamp ?: createdAt,
        )
    }

    private companion object {
        const val MaxRecentUsers = 20
        const val MaxRecentLogs = 20
        const val UsersTable = "users"
        const val EventsTable = "eventos"
        const val OrdersTable = "orders"
        const val StoreOrdersTable = "store_orders"
        const val ActivityLogsTable = "activity_logs"
        const val UserColumns =
            "uid,nome,email,foto,turma,role,tenant_role,data_adesao,createdAt"
        const val UserColumnsWithoutDates =
            "uid,nome,email,foto,turma,role,tenant_role"
        const val ActivityColumns = "id,userName,action,resource,timestamp,createdAt"
        const val ActivityColumnsWithoutDates = "id,userName,action,resource"
    }
}

@Serializable
private data class AdminDashboardUserRow(
    val uid: String = "",
    val nome: String = "",
    val email: String? = null,
    val foto: String? = null,
    val turma: String? = null,
    val role: String? = null,
    @SerialName("tenant_role") val tenantRole: String? = null,
    @SerialName("data_adesao") val dataAdesao: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
private data class AdminDashboardActivityRow(
    val id: String = "",
    @SerialName("userName") val userName: String? = null,
    val action: String? = null,
    val resource: String? = null,
    val timestamp: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)
