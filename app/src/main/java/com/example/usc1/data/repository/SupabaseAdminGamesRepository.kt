package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminArenaUser
import com.example.usc1.domain.model.AdminGamesCatalog
import com.example.usc1.domain.repository.AdminGamesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

class SupabaseAdminGamesRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminGamesRepository {
    override suspend fun getArenaUsers(forceRefresh: Boolean): List<AdminArenaUser> = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        client.from(UsersTable)
            .select(columns = Columns.raw("uid,nome,apelido,turma,foto,xp,sharkCoins,stats,plano_badge")) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "xp", order = Order.DESCENDING)
                limit(count = AdminGamesCatalog.MaxUsers.toLong())
            }
            .decodeList<ArenaUserRow>()
            .mapNotNull { it.toDomain() }
            .sortedByDescending { it.xp }
    }

    private companion object {
        const val UsersTable = "users"
    }
}

@Serializable
private data class ArenaUserRow(
    val uid: String? = null,
    val nome: String? = null,
    val apelido: String? = null,
    val turma: String? = null,
    val foto: String? = null,
    val xp: Int? = null,
    @SerialName("sharkCoins") val sharkCoins: Int? = null,
    @SerialName("plano_badge") val planBadge: String? = null,
    val stats: JsonObject? = null,
) {
    fun toDomain(): AdminArenaUser? {
        val cleanId = uid?.trim().orEmpty()
        if (cleanId.isBlank()) return null
        return AdminArenaUser(
            id = cleanId,
            name = nome?.trim().orEmpty().ifBlank { "Atleta" },
            nickname = apelido?.trim().orEmpty(),
            className = turma?.trim().orEmpty().ifBlank { "Geral" },
            photoUrl = foto?.trim().orEmpty(),
            xp = xp ?: 0,
            sharkCoins = sharkCoins ?: 0,
            planBadge = planBadge?.trim().orEmpty(),
            stats = stats.toNumberMap(),
        )
    }
}

private fun JsonObject?.toNumberMap(): Map<String, Double> {
    if (this == null) return emptyMap()
    return entries.mapNotNull { (key, value) ->
        val number = value.jsonPrimitive.doubleOrNull ?: return@mapNotNull null
        key to number
    }.toMap()
}
