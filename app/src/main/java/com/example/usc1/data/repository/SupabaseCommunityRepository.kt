package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.repository.CommunityRepository
import com.example.usc1.ui.community.CommunityFeedFilter
import com.example.usc1.ui.community.CommunityPost
import com.example.usc1.ui.community.CommunityPostStatus
import com.example.usc1.ui.community.CommunityUiState
import com.example.usc1.ui.community.DefaultCommunityCategories
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

class SupabaseCommunityRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : CommunityRepository {
    override suspend fun getCommunityFeed(
        tenantId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        includeBlocked: Boolean,
    ): CommunityUiState = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        if (!SupabaseClientProvider.config.isConfigured || cleanTenantId.isBlank()) {
            return@withContext CommunityUiState(
                currentUserName = userName,
                currentUserAvatarUrl = userAvatarUrl,
                errorMessage = "Supabase não configurado para carregar a comunidade.",
            )
        }

        val client = clientProvider()
        val config = runCatching { fetchConfig(client, cleanTenantId) }.getOrNull()
        val tabs = normalizeCategories(config?.categories)
        val activeTab = tabs.firstOrNull().orEmpty().ifBlank { DefaultCommunityCategories.first() }
        val rawPosts = client.from(PostsTable)
            .select(columns = Columns.raw(PostColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = MaxFeedResults.toLong())
            }
            .decodeList<CommunityPostRow>()

        val posts = rawPosts
            .asSequence()
            .filter { includeBlocked || it.blocked != true }
            .mapNotNull(::mapPost)
            .toList()

        CommunityUiState(
            title = config?.title.orEmpty().ifBlank { "Comunidade da Atlética" },
            subtitle = config?.subtitle.orEmpty().ifBlank { "Espaço oficial da atlética" },
            coverImageUrl = config?.coverImageUrl,
            currentUserName = userName,
            currentUserAvatarUrl = userAvatarUrl,
            isLoading = false,
            errorMessage = null,
            activeTab = activeTab,
            activeFilter = CommunityFeedFilter.Recent,
            maxVisiblePosts = if (config?.limitMessages == false) 100 else 20,
            tabs = tabs,
            allPosts = posts,
            posts = posts.filter { it.category.equals(activeTab, ignoreCase = true) }.take(if (config?.limitMessages == false) 100 else 20),
        )
    }

    private suspend fun fetchConfig(
        client: SupabaseClient,
        tenantId: String,
    ): CommunityConfig {
        val configIds = listOf(buildTenantScopedRowId(tenantId, CommunityConfigId), CommunityConfigId)
            .filter(String::isNotBlank)
        val row = client.from(AppConfigTable)
            .select(columns = Columns.raw(ConfigColumns)) {
                filter {
                    isIn("id", configIds)
                }
                limit(count = 2)
            }
            .decodeList<CommunityConfigRow>()
            .sortedBy { if (it.id == configIds.firstOrNull()) 0 else 1 }
            .firstOrNull()
            ?: return CommunityConfig()

        val data = row.data.asObject()
        return CommunityConfig(
            title = row.titulo.orEmpty().ifBlank { data.stringValue("titulo") },
            subtitle = row.subtitulo.orEmpty().ifBlank { data.stringValue("subtitulo") },
            coverImageUrl = firstNotBlank(
                row.capaUrl,
                row.capa,
                data.stringValue("capaUrl"),
                data.stringValue("capa"),
            ).takeIf(String::isNotBlank),
            limitMessages = row.limitMessages ?: data.booleanValueOrNull("limitMessages"),
            categories = normalizeCategories(
                data.stringArrayValue("categorias")
                    .ifEmpty { data.stringArrayValue("categories") },
            ),
        )
    }

    private fun mapPost(row: CommunityPostRow): CommunityPost? {
        val id = row.id.trim()
        if (id.isBlank()) return null
        val body = row.texto.orEmpty().trim()
        if (body.isBlank() && row.imagem.isNullOrBlank()) return null
        val category = normalizeCategory(row.categoria)
        val status = when {
            row.blocked == true -> CommunityPostStatus.Blocked
            row.fixado == true -> CommunityPostStatus.Pinned
            else -> CommunityPostStatus.Published
        }
        val authorName = row.userName.orEmpty().trim().ifBlank { "Usuário USC" }
        val title = when {
            row.fixado == true -> "Post fixado"
            row.commentsDisabled == true -> "Comentários trancados"
            else -> category
        }
        return CommunityPost(
            id = id,
            userId = row.userId.orEmpty().trim(),
            authorName = authorName,
            authorAvatarUrl = row.avatar?.trim()?.takeIf(String::isNotBlank),
            handle = row.handle.orEmpty().trim().ifBlank { authorName.toHandle() },
            authorRole = firstNotBlank(row.role, row.plano, row.patente, "Membro"),
            category = category,
            title = title,
            body = body,
            timeLabel = formatRelativeTime(row.createdAt.orEmpty()),
            status = status,
            likes = row.likes.orEmpty().size,
            hype = row.hype.orEmpty().size,
            comments = (row.comentarios ?: 0).coerceAtLeast(0),
            reports = (row.denunciasCount ?: 0).coerceAtLeast(0),
            commentsDisabled = row.commentsDisabled == true,
            imageUrl = row.imagem?.trim()?.takeIf(String::isNotBlank),
        )
    }

    private fun normalizeCategory(value: String?): String {
        val clean = value.orEmpty().trim().replace(Regex("\\s+"), " ").take(MaxCategoryLength)
        return clean.ifBlank { DefaultCommunityCategories.first() }
    }

    private fun normalizeCategories(value: List<String>?): List<String> {
        val source = value?.takeIf(List<String>::isNotEmpty) ?: DefaultCommunityCategories
        val seen = linkedSetOf<String>()
        source.forEach { item ->
            val clean = normalizeCategory(item)
            if (clean.isBlank()) return@forEach
            val exists = seen.any { it.equals(clean, ignoreCase = true) }
            if (!exists) seen.add(clean)
        }
        return seen.toList().ifEmpty { DefaultCommunityCategories }
    }

    private fun formatRelativeTime(value: String): String {
        val instant = parseInstant(value) ?: return value.take(12).ifBlank { "Agora" }
        val now = Instant.now()
        val duration = Duration.between(instant, now).takeIf { !it.isNegative } ?: Duration.ZERO
        val minutes = duration.toMinutes()
        return when {
            minutes < 1 -> "Agora"
            minutes < 60 -> "$minutes min"
            minutes < 24 * 60 -> "${duration.toHours()} h"
            minutes < 7 * 24 * 60 -> "${duration.toDays()} dias"
            else -> CommunityDateFormatter.format(instant)
        }
    }

    private fun parseInstant(value: String): Instant? {
        val clean = value.trim()
        if (clean.isBlank()) return null
        return runCatching { OffsetDateTime.parse(clean).toInstant() }
            .getOrElse { runCatching { Instant.parse(clean) }.getOrNull() }
    }

    private fun firstNotBlank(vararg values: String?): String {
        return values.firstNotNullOfOrNull { it?.trim()?.takeIf(String::isNotBlank) }.orEmpty()
    }

    private fun String.toHandle(): String {
        val compact = lowercase(Locale.ROOT)
            .normalizeAscii()
            .replace(Regex("[^a-z0-9]+"), "")
            .take(18)
        return "@${compact.ifBlank { "usc" }}"
    }

    private fun String.normalizeAscii(): String {
        val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{Mn}+"), "")
    }

    private fun buildTenantScopedRowId(tenantId: String, baseId: String): String {
        val cleanTenantId = tenantId.trim()
        val cleanBaseId = baseId.trim()
        return if (cleanTenantId.isBlank()) cleanBaseId else "tenant:$cleanTenantId::$cleanBaseId"
    }

    private companion object {
        const val PostsTable = "posts"
        const val AppConfigTable = "app_config"
        const val CommunityConfigId = "comunidade"
        const val MaxFeedResults = 120
        const val MaxCategoryLength = 40
        const val PostColumns =
            "id,userId,userName,avatar,handle,role,plano,plano_cor,plano_icon,patente,patente_icon,patente_cor,texto,imagem,categoria,likes,hype,comentarios,blocked,commentsDisabled,fixado,denunciasCount,createdAt,updatedAt,tenant_id"
        const val ConfigColumns =
            "id,tenant_id,data,titulo,subtitulo,capa,capaUrl,limitMessages,updatedAt"
    }
}

private val CommunityDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.of("America/Sao_Paulo"))

private data class CommunityConfig(
    val title: String = "",
    val subtitle: String = "",
    val coverImageUrl: String? = null,
    val limitMessages: Boolean? = null,
    val categories: List<String> = DefaultCommunityCategories,
)

@Serializable
private data class CommunityConfigRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val data: JsonElement? = null,
    val titulo: String? = null,
    val subtitulo: String? = null,
    val capa: String? = null,
    val capaUrl: String? = null,
    val limitMessages: Boolean? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
private data class CommunityPostRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("userId") val userId: String? = null,
    @SerialName("userName") val userName: String? = null,
    val avatar: String? = null,
    val handle: String? = null,
    val role: String? = null,
    val plano: String? = null,
    @SerialName("plano_cor") val planoCor: String? = null,
    @SerialName("plano_icon") val planoIcon: String? = null,
    val patente: String? = null,
    @SerialName("patente_cor") val patenteCor: String? = null,
    @SerialName("patente_icon") val patenteIcon: String? = null,
    val texto: String? = null,
    val imagem: String? = null,
    val categoria: String? = null,
    val likes: List<String>? = null,
    val hype: List<String>? = null,
    val comentarios: Int? = null,
    val blocked: Boolean? = null,
    val commentsDisabled: Boolean? = null,
    val fixado: Boolean? = null,
    val denunciasCount: Int? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

private fun JsonObject?.stringValue(key: String): String {
    return (this?.get(key) as? JsonPrimitive)
        ?.contentOrNull
        ?.trim()
        .orEmpty()
}

private fun JsonObject?.booleanValueOrNull(key: String): Boolean? {
    return (this?.get(key) as? JsonPrimitive)?.booleanOrNull
}

private fun JsonObject?.stringArrayValue(key: String): List<String> {
    val array = this?.get(key) as? JsonArray ?: return emptyList()
    return array.mapNotNull { element ->
        (element as? JsonPrimitive)
            ?.contentOrNull
            ?.trim()
            ?.takeIf(String::isNotBlank)
    }
}
