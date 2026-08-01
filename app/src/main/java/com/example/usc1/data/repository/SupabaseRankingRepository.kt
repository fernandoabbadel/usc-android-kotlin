package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.RankingCatalog
import com.example.usc1.domain.model.RankingUser
import com.example.usc1.domain.repository.RankingRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** Porta de `web-reference/src/lib/rankingService.ts` para `/ranking` e `/ranking/[turmaId]`. */
class SupabaseRankingRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : RankingRepository {

    override suspend fun getGlobalRanking(
        tenantId: String,
        limit: Int,
    ): List<RankingUser> = fetchRanking(tenantId, className = null, limit = limit)

    override suspend fun getClassRanking(
        tenantId: String,
        className: String,
        limit: Int,
    ): List<RankingUser> {
        val cleanClass = className.trim()
        if (cleanClass.isBlank()) return emptyList()
        return fetchRanking(tenantId, className = cleanClass, limit = limit)
    }

    private suspend fun fetchRanking(
        tenantId: String,
        className: String?,
        limit: Int,
    ): List<RankingUser> = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext emptyList()
        }

        val safeLimit = limit.coerceIn(1, RankingCatalog.MaxGlobalResults)
        clientProvider().from(UsersTable)
            .select(columns = Columns.raw(RankingColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    if (!className.isNullOrBlank()) {
                        eq("turma", className)
                    }
                }
                order(column = "xp", order = Order.DESCENDING)
                limit(count = safeLimit.toLong())
            }
            .decodeList<RankingRow>()
            .mapNotNull { it.toRankingUser() }
            .sortedByDescending { it.xp }
            .take(safeLimit)
    }

    private fun RankingRow.toRankingUser(): RankingUser? {
        // `normalizeUser` aceita tanto `id` quanto `uid`.
        val resolvedId = id.orEmpty().trim().ifBlank { uid.orEmpty().trim() }
        if (resolvedId.isBlank()) return null
        return RankingUser(
            id = resolvedId,
            name = nome.orEmpty().trim().ifBlank { "Atleta Anonimo" },
            nickname = apelido.orEmpty().trim(),
            photoUrl = resolveRemoteImageUrl(foto),
            className = turma.orEmpty().trim().ifBlank { "GERAL" },
            xp = xp.coerceAtLeast(0),
        )
    }

    private companion object {
        const val UsersTable = "users"
        const val RankingColumns = "id,uid,nome,apelido,foto,turma,xp"
    }
}

@Serializable
private data class RankingRow(
    val id: String? = null,
    val uid: String? = null,
    val nome: String? = null,
    val apelido: String? = null,
    val foto: String? = null,
    val turma: String? = null,
    val xp: Int = 0,
)
