package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.HistoricEvent
import com.example.usc1.domain.model.HistoryPageConfig
import com.example.usc1.domain.model.OrganogramConfig
import com.example.usc1.domain.model.OrganogramDisplayMember
import com.example.usc1.domain.model.OrganogramMember
import com.example.usc1.domain.model.OrganogramMemberStatus
import com.example.usc1.domain.model.TenantHistoryCatalog
import com.example.usc1.domain.model.TenantHistoryData
import com.example.usc1.domain.repository.TenantHistoryRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Porta de `historyService.ts` e `organogramService.ts` para `/historico`
 * e `/historico/organograma`.
 */
class SupabaseTenantHistoryRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : TenantHistoryRepository {

    override suspend fun getHistory(tenantId: String): TenantHistoryData = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext TenantHistoryData()
        }

        val client = clientProvider()
        val config = runCatching { fetchHistoryConfig(client, cleanTenantId) }
            .getOrElse { HistoryPageConfig() }

        val events = client.from(TenantHistoryCatalog.HistoricEventsTable)
            .select(columns = Columns.raw(HistoricEventColumns)) {
                filter { eq("tenant_id", cleanTenantId) }
                // A página usa `order: "asc"` para montar a linha do tempo.
                order(column = "data", order = Order.ASCENDING)
                limit(count = TenantHistoryCatalog.MaxHistoryEvents.toLong())
            }
            .decodeList<HistoricEventRow>()
            .map { it.toEvent() }

        TenantHistoryData(config = config, events = events)
    }

    override suspend fun getOrganogramConfig(
        tenantId: String,
    ): OrganogramConfig = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext OrganogramConfig()
        }

        val row = clientProvider().from(TenantHistoryCatalog.AppConfigTable)
            .select(columns = Columns.raw("id,data")) {
                filter {
                    eq("id", buildTenantScopedRowId(cleanTenantId, TenantHistoryCatalog.OrganogramConfigDocId))
                }
                limit(count = 1)
            }
            .decodeList<JsonObject>()
            .firstOrNull()
            ?: return@withContext OrganogramConfig()

        parseOrganogramConfig(row)
    }

    override suspend fun resolveOrganogramMembers(
        tenantId: String,
        config: OrganogramConfig,
        fallbackPhotoUrl: String?,
    ): List<OrganogramDisplayMember> = withContext(Dispatchers.IO) {
        val published = config.members.filter { it.isPublished }
        if (published.isEmpty()) return@withContext emptyList()

        val linkedIds = published.mapNotNull { it.userId.trim().takeIf(String::isNotBlank) }.distinct()
        val visuals = if (linkedIds.isEmpty() || !SupabaseClientProvider.config.isConfigured) {
            emptyMap()
        } else {
            runCatching { fetchUserVisuals(clientProvider(), tenantId.trim(), linkedIds) }
                .getOrElse { emptyMap() }
        }

        published.map { member ->
            val visual = visuals[member.userId]
            OrganogramDisplayMember(
                member = member,
                displayName = visual?.name?.takeIf { it.isNotBlank() }
                    ?: member.name.ifBlank { "Membro a definir" },
                displayPhotoUrl = visual?.photoUrl ?: member.photoUrl ?: fallbackPhotoUrl,
                displayDetail = visual?.className?.takeIf { it.isNotBlank() }
                    ?: if (member.userId.isNotBlank()) "Membro vinculado" else "Vinculação pendente",
                hasCanonicalVisual = visual != null,
            )
        }
    }

    /** `fetchHistoryPageConfig`: lê `app_config` e aceita payload no topo ou em `data`. */
    private suspend fun fetchHistoryConfig(
        client: SupabaseClient,
        tenantId: String,
    ): HistoryPageConfig {
        val row = client.from(TenantHistoryCatalog.AppConfigTable)
            .select(columns = Columns.raw("id,data")) {
                filter {
                    eq("id", buildTenantScopedRowId(tenantId, TenantHistoryCatalog.HistoryConfigDocId))
                }
                limit(count = 1)
            }
            .decodeList<JsonObject>()
            .firstOrNull()
            ?: return HistoryPageConfig()

        val payload = (row["data"] as? JsonObject) ?: row
        return HistoryPageConfig(
            title = payload.stringValue("tituloPagina").ifBlank {
                TenantHistoryCatalog.DefaultHistoryTitle
            }.take(120),
            subtitle = payload.stringValue("subtituloPagina").ifBlank {
                TenantHistoryCatalog.DefaultHistorySubtitle
            }.take(240),
            coverPhotoUrl = resolveRemoteImageUrl(payload.stringValue("fotoCapa")),
        )
    }

    /** `normalizeConfig` do `organogramService`. */
    private fun parseOrganogramConfig(row: JsonObject): OrganogramConfig {
        val nested = (row["data"] as? JsonObject) ?: JsonObject(emptyMap())
        val title = row.stringValue("tituloPagina").ifBlank { nested.stringValue("tituloPagina") }
        val subtitle = row.stringValue("subtituloPagina").ifBlank { nested.stringValue("subtituloPagina") }
        val membersSource = (row["membros"] as? JsonArray) ?: (nested["membros"] as? JsonArray)

        val members = membersSource.orEmpty().mapIndexedNotNull { index, element ->
            val entry = element as? JsonObject ?: return@mapIndexedNotNull null
            val role = entry.stringValue("cargo").take(80)
            if (role.isBlank()) return@mapIndexedNotNull null
            val section = TenantHistoryCatalog.normalizeSectionName(entry.stringValue("secao"))
            OrganogramMember(
                id = entry.stringValue("id").take(120).ifBlank { "organograma:$section:$role:$index" },
                section = section,
                role = role,
                order = (entry["ordem"] as? JsonPrimitive)?.intOrNull ?: index,
                status = OrganogramMemberStatus.fromRemote(entry.stringValue("status")),
                userId = entry.stringValue("userId").take(120),
                name = entry.stringValue("nome").take(120),
                photoUrl = resolveRemoteImageUrl(entry.stringValue("foto")),
            )
        }

        val configuredOrder = ((row["ordemSecoes"] as? JsonArray) ?: (nested["ordemSecoes"] as? JsonArray))
            .orEmpty()
            .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .map(TenantHistoryCatalog::normalizeSectionName)
            .filter { it.isNotBlank() }
            .distinct()
        val missingSections = members.map { it.section }.distinct().filterNot { it in configuredOrder }
        val sectionOrder = configuredOrder + missingSections
        val sectionIndex = sectionOrder.withIndex().associate { (index, name) -> name to index }

        return OrganogramConfig(
            title = title.ifBlank { TenantHistoryCatalog.DefaultOrganogramTitle }.take(120),
            subtitle = subtitle.ifBlank { TenantHistoryCatalog.DefaultOrganogramSubtitle }.take(240),
            members = members.sortedWith(
                compareBy(
                    { sectionIndex[it.section] ?: Int.MAX_VALUE },
                    { it.order },
                    { it.role.lowercase() },
                ),
            ),
            sectionOrder = sectionOrder,
        )
    }

    private suspend fun fetchUserVisuals(
        client: SupabaseClient,
        tenantId: String,
        userIds: List<String>,
    ): Map<String, UserVisual> {
        if (userIds.isEmpty()) return emptyMap()
        return client.from(UsersTable)
            .select(columns = Columns.raw("uid,nome,foto,turma")) {
                filter {
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                    isIn("uid", userIds)
                }
                limit(count = userIds.size.toLong())
            }
            .decodeList<UserVisualRow>()
            .mapNotNull { row ->
                val uid = row.uid.trim()
                if (uid.isBlank()) return@mapNotNull null
                uid to UserVisual(
                    name = row.nome.orEmpty().trim(),
                    photoUrl = resolveRemoteImageUrl(row.foto),
                    className = row.turma.orEmpty().trim(),
                )
            }
            .toMap()
    }

    private fun HistoricEventRow.toEvent(): HistoricEvent {
        val eventDate = data.orEmpty().trim().take(10)
        return HistoricEvent(
            id = id.trim(),
            title = titulo.orEmpty().trim().take(120).ifBlank { "Evento" },
            date = eventDate,
            year = ano.orEmpty().trim().take(4).ifBlank { eventDate.take(4) },
            description = descricao.orEmpty().trim().take(2_000),
            location = local.orEmpty().trim().take(120),
            photoUrl = resolveRemoteImageUrl(foto),
        )
    }

    private fun buildTenantScopedRowId(tenantId: String, baseId: String): String {
        return "tenant:${tenantId.trim()}::${baseId.trim()}"
    }

    private data class UserVisual(
        val name: String,
        val photoUrl: String?,
        val className: String,
    )

    private companion object {
        const val UsersTable = "users"
        const val HistoricEventColumns = "id,titulo,data,ano,descricao,local,foto"
    }
}

private fun JsonObject.stringValue(key: String): String {
    val element = this[key] ?: return ""
    if (element is JsonNull) return ""
    return (element as? JsonPrimitive)?.contentOrNull.orEmpty().trim()
}

@Serializable
private data class HistoricEventRow(
    val id: String = "",
    val titulo: String? = null,
    val data: String? = null,
    val ano: String? = null,
    val descricao: String? = null,
    val local: String? = null,
    val foto: String? = null,
)

@Serializable
private data class UserVisualRow(
    val uid: String = "",
    val nome: String? = null,
    val foto: String? = null,
    val turma: String? = null,
)
