package com.example.usc1.data.repository

import com.example.usc1.R
import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.repository.CollectivesRepository
import com.example.usc1.ui.collectives.CollectiveAgendaItem
import com.example.usc1.ui.collectives.CollectiveEvent
import com.example.usc1.ui.collectives.CollectiveGroup
import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.CollectiveMember
import com.example.usc1.ui.collectives.CollectiveStoreItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

class SupabaseCollectivesRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : CollectivesRepository {
    override suspend fun getCollectives(
        tenantId: String,
        kind: CollectiveKind,
    ): List<CollectiveGroup> = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext emptyList()
        }

        clientProvider().from(CollectivesTable)
            .select(columns = Columns.raw(CollectiveColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                }
                order(column = "nome", order = Order.ASCENDING)
                limit(count = MaxCollectives.toLong())
            }
            .decodeList<CollectiveRow>()
            .mapNotNull { row -> row.toGroup() }
            .filter { group -> group.kind == kind }
            .sortedWith(compareBy<CollectiveGroup> { it.accentName }.thenBy { it.name.lowercase(Locale.ROOT) })
    }

    private fun CollectiveRow.toGroup(): CollectiveGroup? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null

        val kind = inferKind(cleanId, nome, sigla, data)
        val turmaId = firstNotBlank(jsonString(data, "turmaId"), jsonString(data, "turma"), sigla)
            .uppercase(Locale.ROOT)
            .takeIf { kind == CollectiveKind.Commission && it.isNotBlank() && it.startsWith("T") }
        val cleanName = firstNotBlank(
            nome,
            jsonString(data, "nome"),
            jsonString(data, "name"),
            turmaId?.let { "Comissão $it" },
            fallbackName(kind),
        )
        val cleanSigla = firstNotBlank(sigla, jsonString(data, "sigla"), turmaId).uppercase(Locale.ROOT)
        val president = firstNotBlank(presidente, jsonString(data, "presidente"), jsonString(data, "coordenador"))
        val members = parseMembers(membros, data?.get("membros"))
            .ifEmpty {
                president.takeIf(String::isNotBlank)?.let {
                    listOf(CollectiveMember(name = it, role = "Presidência", status = "Ativo"))
                }.orEmpty()
            }
        val description = firstNotBlank(
            descricao,
            jsonString(data, "descricao"),
            jsonString(data, "description"),
            jsonString(data, "visaoGeral"),
            bizu,
            jsonString(data, "bizu"),
        ).ifBlank {
            when (kind) {
                CollectiveKind.League -> "Liga acadêmica oficial com membros, agenda, eventos e loja própria."
                CollectiveKind.Directory -> "Diretório acadêmico oficial com gestão, agenda, eventos e loja institucional."
                CollectiveKind.Commission -> "Página oficial da comissão de formatura${turmaId?.let { " da $it" }.orEmpty()}."
            }
        }
        val imageUrl = safeImageUrl(firstNotBlank(logoUrl, logo, foto, jsonString(data, "logoUrl"), jsonString(data, "foto")))
        val parsedEvents = parseEvents(eventos, data?.get("eventos"))
        val parsedStore = parseStore(data?.get("loja"), data?.get("store"), data?.get("produtos"))
        val parsedAgenda = parseAgenda(data?.get("agenda"), data?.get("calendario"), data?.get("tarefas"))
        val memberCount = listOf(
            membrosIds?.size ?: 0,
            members.size,
            jsonInt(data, "membersCount") ?: 0,
            jsonInt(data, "memberCount") ?: 0,
            jsonInt(data, "totalMembros") ?: 0,
        ).maxOrNull() ?: 0

        return CollectiveGroup(
            id = cleanId,
            name = cleanName,
            subtitle = buildSubtitle(kind, cleanSigla, turmaId, president),
            description = description,
            kind = kind,
            status = statusLabel(status, ativa, visivel),
            memberCount = memberCount,
            imageRes = fallbackImage(kind),
            imageUrl = imageUrl,
            accentName = firstNotBlank(turmaId, cleanSigla, kind.label),
            members = members,
            agenda = parsedAgenda,
            store = parsedStore,
            events = parsedEvents,
        )
    }

    private fun inferKind(
        id: String,
        name: String?,
        acronym: String?,
        data: JsonObject?,
    ): CollectiveKind {
        val category = normalizeCategory(
            firstNotBlank(
                jsonString(data, "category"),
                jsonString(data, "categoria"),
                jsonString(data, "area"),
                jsonString(data, "type"),
            ),
        )
        if (category == "comissao") return CollectiveKind.Commission
        if (category == "diretorio") return CollectiveKind.Directory
        val haystack = listOf(id, name.orEmpty(), acronym.orEmpty(), jsonString(data, "nome"))
            .joinToString(" ")
            .lowercase(Locale.ROOT)
            .normalizeAscii()
        return when {
            haystack.contains("comissao") || haystack.contains("comissoes") || haystack.contains("commission") -> CollectiveKind.Commission
            haystack.contains("diretorio") || haystack.contains("directory") -> CollectiveKind.Directory
            else -> CollectiveKind.League
        }
    }

    private fun buildSubtitle(
        kind: CollectiveKind,
        acronym: String,
        turmaId: String?,
        president: String,
    ): String {
        return when {
            kind == CollectiveKind.Commission && !turmaId.isNullOrBlank() && president.isNotBlank() -> "$turmaId • Presidência: $president"
            kind == CollectiveKind.Commission && !turmaId.isNullOrBlank() -> "Turma $turmaId"
            acronym.isNotBlank() && president.isNotBlank() -> "$acronym • Presidência: $president"
            acronym.isNotBlank() -> acronym
            president.isNotBlank() -> "Presidência: $president"
            else -> "Página oficial"
        }
    }

    private fun normalizeCategory(value: String): String {
        val raw = value.trim().lowercase(Locale.ROOT).normalizeAscii()
        return when (raw) {
            "comissao", "comissoes", "commission", "commissions" -> "comissao"
            "diretorio", "directory", "da" -> "diretorio"
            else -> "liga"
        }
    }

    private fun fallbackName(kind: CollectiveKind): String {
        return when (kind) {
            CollectiveKind.League -> "Liga"
            CollectiveKind.Directory -> "Diretório"
            CollectiveKind.Commission -> "Comissão"
        }
    }

    private fun fallbackImage(kind: CollectiveKind): Int {
        return when (kind) {
            CollectiveKind.League -> R.drawable.battle_forest
            CollectiveKind.Directory -> R.drawable.logo_usc_wide
            CollectiveKind.Commission -> R.drawable.carteirinha_bg
        }
    }

    private fun statusLabel(
        rawStatus: String?,
        active: Boolean?,
        visible: Boolean?,
    ): String {
        val cleanStatus = firstNotBlank(rawStatus).lowercase(Locale.ROOT)
        return when {
            visible == false -> "Oculta"
            active == false -> "Inativa"
            cleanStatus in setOf("approved", "aprovado", "active", "ativo", "publicado") -> "Ativa"
            cleanStatus in setOf("pending", "pendente", "pending_approval", "em_analise") -> "Em análise"
            cleanStatus in setOf("rejected", "recusado", "rejeitado") -> "Recusada"
            cleanStatus.isNotBlank() -> cleanStatus.replace('_', ' ').replaceFirstChar { it.titlecase(Locale.forLanguageTag("pt-BR")) }
            else -> "Ativa"
        }
    }

    private fun parseMembers(vararg sources: JsonElement?): List<CollectiveMember> {
        return firstArray(*sources).mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val name = firstNotBlank(
                obj.string("nome"),
                obj.string("name"),
                obj.string("userName"),
                obj.string("usuarioNome"),
            )
            if (name.isBlank()) return@mapNotNull null
            CollectiveMember(
                name = name,
                role = firstNotBlank(obj.string("cargo"), obj.string("role"), obj.string("funcao"), "Membro"),
                status = firstNotBlank(obj.string("status"), "Ativo"),
            )
        }
    }

    private fun parseAgenda(vararg sources: JsonElement?): List<CollectiveAgendaItem> {
        return firstArray(*sources).mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val title = firstNotBlank(obj.string("titulo"), obj.string("title"), obj.string("nome"))
            if (title.isBlank()) return@mapNotNull null
            CollectiveAgendaItem(
                title = title,
                dateLabel = firstNotBlank(obj.string("data"), obj.string("date"), obj.string("quando"), "Data a definir"),
                place = firstNotBlank(obj.string("local"), obj.string("place"), obj.string("onde"), "Local a definir"),
            )
        }
    }

    private fun parseStore(vararg sources: JsonElement?): List<CollectiveStoreItem> {
        return firstArray(*sources).mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val name = firstNotBlank(obj.string("nome"), obj.string("name"), obj.string("produto"), obj.string("title"))
            if (name.isBlank()) return@mapNotNull null
            CollectiveStoreItem(
                name = name,
                priceLabel = firstNotBlank(obj.string("preco"), obj.string("price"), obj.string("priceLabel"), "Preço sob consulta"),
                status = firstNotBlank(obj.string("status"), obj.string("badge"), "Disponível"),
            )
        }
    }

    private fun parseEvents(vararg sources: JsonElement?): List<CollectiveEvent> {
        return firstArray(*sources).mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val title = firstNotBlank(obj.string("titulo"), obj.string("title"), obj.string("nome"), obj.string("eventoNome"))
            if (title.isBlank()) return@mapNotNull null
            CollectiveEvent(
                title = title,
                dateLabel = firstNotBlank(obj.string("data"), obj.string("date"), obj.string("dateLabel"), "Data a definir"),
                status = firstNotBlank(obj.string("status"), obj.string("badge"), "Publicado"),
            )
        }
    }

    private fun firstArray(vararg sources: JsonElement?): List<JsonElement> {
        return sources.firstNotNullOfOrNull { source ->
            (source as? JsonArray)?.takeIf { it.isNotEmpty() }?.toList()
        }.orEmpty()
    }

    private fun safeImageUrl(value: String): String? {
        val clean = value.trim()
        if (clean.startsWith("http://", ignoreCase = true)) return clean
        if (clean.startsWith("https://", ignoreCase = true)) return clean
        if (clean.startsWith("data:image/", ignoreCase = true)) return clean
        return null
    }

    private fun firstNotBlank(vararg values: String?): String {
        for (value in values) {
            val clean = value?.trim()
            if (!clean.isNullOrBlank()) return clean
        }
        return ""
    }

    private fun String.normalizeAscii(): String {
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{Mn}+"), "")
    }

    private companion object {
        const val CollectivesTable = "ligas_config"
        const val MaxCollectives = 120
        const val CollectiveColumns =
            "id,tenant_id,nome,sigla,presidente,descricao,foto,logoUrl,logo,visivel,ativa,membros,membrosIds,eventos,perguntas,bizu,status,data"
    }
}

@Serializable
private data class CollectiveRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val nome: String? = null,
    val sigla: String? = null,
    val presidente: String? = null,
    val descricao: String? = null,
    val foto: String? = null,
    val logoUrl: String? = null,
    val logo: String? = null,
    val visivel: Boolean? = null,
    val ativa: Boolean? = null,
    val membros: JsonElement? = null,
    val membrosIds: List<String>? = null,
    val eventos: JsonElement? = null,
    val perguntas: JsonElement? = null,
    val bizu: String? = null,
    val status: String? = null,
    val data: JsonObject? = null,
)

private fun jsonString(data: JsonObject?, key: String): String {
    val primitive = data?.get(key) as? JsonPrimitive ?: return ""
    return primitive.contentOrNull?.trim().orEmpty()
}

private fun jsonInt(data: JsonObject?, key: String): Int? {
    val primitive = data?.get(key) as? JsonPrimitive ?: return null
    return primitive.intOrNull
}

private fun JsonElement.asObjectOrNull(): JsonObject? {
    return runCatching { jsonObject }.getOrNull()
}

private fun JsonObject.string(key: String): String {
    val primitive = get(key) as? JsonPrimitive ?: return ""
    return primitive.contentOrNull?.trim().orEmpty()
}
