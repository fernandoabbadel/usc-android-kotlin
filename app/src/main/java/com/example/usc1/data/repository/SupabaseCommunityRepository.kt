package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.repository.CommunityReactionField
import com.example.usc1.domain.repository.CommunityReactionResult
import com.example.usc1.domain.repository.CommunityRepository
import com.example.usc1.ui.community.CommunityComment
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
import java.util.UUID
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
        if (!SupabaseClientProvider.config.isConfigured) {
            return@withContext CommunityUiState(
                currentUserName = userName,
                currentUserAvatarUrl = userAvatarUrl,
                errorMessage = "Supabase não configurado para carregar a comunidade.",
            )
        }

        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            runCatching { SupabaseTenantResolver.resolveActiveTenantId(client) }.getOrDefault("")
        }
        if (cleanTenantId.isBlank()) {
            return@withContext CommunityUiState(
                currentUserId = userId.trim(),
                currentUserName = userName,
                currentUserAvatarUrl = userAvatarUrl,
                errorMessage = "Selecione uma atlética para carregar a comunidade.",
            )
        }
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

        val cleanUserId = userId.trim()
        val posts = rawPosts
            .asSequence()
            .filter { includeBlocked || it.blocked != true }
            .mapNotNull { row -> mapPost(row, cleanUserId) }
            .toList()

        CommunityUiState(
            title = config?.title.orEmpty().ifBlank { "Comunidade da Atlética" },
            subtitle = config?.subtitle.orEmpty().ifBlank { "Espaço oficial da atlética" },
            coverImageUrl = resolveRemoteImageUrl(config?.coverImageUrl),
            currentUserId = cleanUserId,
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

    override suspend fun createPost(
        tenantId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        category: String,
        text: String,
    ): String = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) {
            throw IllegalStateException("Supabase não configurado para publicar na comunidade.")
        }
        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            SupabaseTenantResolver.resolveActiveTenantId(client)
        }
        val cleanUserId = userId.trim()
        val cleanText = text.trim().take(280)
        if (cleanTenantId.isBlank() || cleanUserId.isBlank()) {
            throw IllegalStateException("Entre com sua conta e selecione uma atlética para publicar.")
        }
        if (cleanText.isBlank()) {
            throw IllegalStateException("Escreva uma mensagem para publicar no feed.")
        }

        val now = Instant.now().toString()
        val postId = UUID.randomUUID().toString()
        val cleanUserName = userName.trim().ifBlank { "Usuário USC" }
        val payload = communityJsonPayloadOf(
            "id" to postId,
            "tenant_id" to cleanTenantId,
            "userId" to cleanUserId,
            "userName" to cleanUserName,
            "avatar" to userAvatarUrl.orEmpty().trim(),
            "handle" to cleanUserName.toHandle(),
            "role" to "Membro ativo",
            "texto" to cleanText,
            "categoria" to normalizeCategory(category),
            "likes" to JsonArray(emptyList()),
            "hype" to JsonArray(emptyList()),
            "comentarios" to 0,
            "blocked" to false,
            "commentsDisabled" to false,
            "fixado" to false,
            "denunciasCount" to 0,
            "createdAt" to now,
            "updatedAt" to now,
        )
        insertCommunityPostWithOptionalColumnFallback(client, payload)
        postId
    }

    /** Espelha `updatePostArrayField` do web-reference: le o array, alterna o uid e regrava. */
    override suspend fun togglePostReaction(
        tenantId: String,
        postId: String,
        userId: String,
        field: CommunityReactionField,
    ): CommunityReactionResult = withContext(Dispatchers.IO) {
        val cleanPostId = postId.trim()
        val cleanUserId = userId.trim()
        if (cleanPostId.isBlank() || cleanUserId.isBlank()) {
            throw IllegalStateException("Entre com sua conta para reagir na comunidade.")
        }
        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            runCatching { SupabaseTenantResolver.resolveActiveTenantId(client) }.getOrDefault("")
        }

        val current = client.from(PostsTable)
            .select(columns = Columns.raw("id,likes,hype,userId")) {
                filter {
                    eq("id", cleanPostId)
                    if (cleanTenantId.isNotBlank()) eq("tenant_id", cleanTenantId)
                }
                limit(count = 1)
            }
            .decodeList<CommunityReactionRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Publicação não encontrada.")

        val values = when (field) {
            CommunityReactionField.Likes -> current.likes.orEmpty()
            CommunityReactionField.Hype -> current.hype.orEmpty()
        }.map(String::trim).filter(String::isNotBlank)

        val active = !values.contains(cleanUserId)
        val next = if (active) values + cleanUserId else values.filterNot { it == cleanUserId }

        val patch = LinkedHashMap<String, JsonElement>().apply {
            put(field.column, JsonArray(next.map(::JsonPrimitive)))
            put("updatedAt", JsonPrimitive(Instant.now().toString()))
        }
        client.from(PostsTable).update(JsonObject(patch)) {
            filter {
                eq("id", cleanPostId)
                if (cleanTenantId.isNotBlank()) eq("tenant_id", cleanTenantId)
            }
        }

        CommunityReactionResult(total = next.size, active = active)
    }

    override suspend fun getComments(
        tenantId: String,
        postId: String,
    ): List<CommunityComment> = withContext(Dispatchers.IO) {
        val cleanPostId = postId.trim()
        if (cleanPostId.isBlank()) return@withContext emptyList()
        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            runCatching { SupabaseTenantResolver.resolveActiveTenantId(client) }.getOrDefault("")
        }

        runCatching {
            client.from(CommentsTable)
                .select(columns = Columns.raw(CommentColumns)) {
                    filter {
                        eq("postId", cleanPostId)
                        if (cleanTenantId.isNotBlank()) eq("tenant_id", cleanTenantId)
                    }
                    order(column = "createdAt", order = Order.ASCENDING)
                    limit(count = MaxCommentResults)
                }
                .decodeList<CommunityCommentRow>()
                .mapNotNull { row ->
                    val id = row.id.orEmpty().trim()
                    val body = row.texto.orEmpty().trim()
                    if (id.isBlank() || body.isBlank()) return@mapNotNull null
                    CommunityComment(
                        id = id,
                        postId = cleanPostId,
                        userId = row.userId.orEmpty().trim(),
                        authorName = row.userName.orEmpty().trim().ifBlank { "Usuário USC" },
                        authorAvatarUrl = resolveRemoteImageUrl(row.avatar),
                        authorRole = firstNotBlank(row.role, row.plano, row.patente),
                        body = body,
                        timeLabel = formatRelativeTime(row.createdAt.orEmpty()),
                        likes = row.likes.orEmpty().size,
                    )
                }
        }.getOrDefault(emptyList())
    }

    override suspend fun createComment(
        tenantId: String,
        postId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        text: String,
    ): String = withContext(Dispatchers.IO) {
        val cleanPostId = postId.trim()
        val cleanUserId = userId.trim()
        val cleanText = text.trim().take(300)
        if (cleanPostId.isBlank() || cleanUserId.isBlank()) {
            throw IllegalStateException("Entre com sua conta para comentar.")
        }
        if (cleanText.isBlank()) {
            throw IllegalStateException("Escreva um comentário para publicar.")
        }

        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            runCatching { SupabaseTenantResolver.resolveActiveTenantId(client) }.getOrDefault("")
        }
        val now = Instant.now().toString()
        val commentId = UUID.randomUUID().toString()
        val cleanUserName = userName.trim().ifBlank { "Usuário USC" }

        val payload = communityJsonPayloadOf(
            "id" to commentId,
            "postId" to cleanPostId,
            "tenant_id" to cleanTenantId.takeIf(String::isNotBlank),
            "userId" to cleanUserId,
            "userName" to cleanUserName,
            "avatar" to userAvatarUrl.orEmpty().trim(),
            "role" to "Membro ativo",
            "texto" to cleanText,
            "likes" to JsonArray(emptyList()),
            "createdAt" to now,
            "updatedAt" to now,
        )
        insertCommunityRowWithOptionalColumnFallback(
            client = client,
            table = CommentsTable,
            nonRemovableColumns = setOf("id", "postId", "userId", "texto"),
            payload = payload,
        )

        runCatching { incrementCommentCount(client, cleanTenantId, cleanPostId) }
        commentId
    }

    private suspend fun incrementCommentCount(
        client: SupabaseClient,
        tenantId: String,
        postId: String,
    ) {
        val current = client.from(PostsTable)
            .select(columns = Columns.raw("id,comentarios")) {
                filter {
                    eq("id", postId)
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
                limit(count = 1)
            }
            .decodeList<CommunityCommentCountRow>()
            .firstOrNull()
            ?: return

        val patch = LinkedHashMap<String, JsonElement>().apply {
            put("comentarios", JsonPrimitive((current.comentarios ?: 0).coerceAtLeast(0) + 1))
            put("updatedAt", JsonPrimitive(Instant.now().toString()))
        }
        client.from(PostsTable).update(JsonObject(patch)) {
            filter {
                eq("id", postId)
                if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
            }
        }
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

    private fun mapPost(row: CommunityPostRow, viewerId: String): CommunityPost? {
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
            authorAvatarUrl = resolveRemoteImageUrl(row.avatar),
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
            imageUrl = resolveRemoteImageUrl(row.imagem),
            likedByMe = viewerId.isNotBlank() && row.likes.orEmpty().any { it.trim() == viewerId },
            hypedByMe = viewerId.isNotBlank() && row.hype.orEmpty().any { it.trim() == viewerId },
            planColorKey = row.planoCor.orEmpty().trim(),
            patente = row.patente.orEmpty().trim(),
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

    private fun communityJsonPayloadOf(
        vararg pairs: Pair<String, Any?>,
    ): LinkedHashMap<String, JsonElement> {
        val payload = LinkedHashMap<String, JsonElement>()
        pairs.forEach { (key, value) ->
            val element = value.toCommunityJsonElementOrNull() ?: return@forEach
            payload[key] = element
        }
        return payload
    }

    private fun Any?.toCommunityJsonElementOrNull(): JsonElement? {
        return when (this) {
            null -> null
            is JsonElement -> this.takeUnless { it is kotlinx.serialization.json.JsonNull }
            is String -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            is Int -> JsonPrimitive(this)
            is Long -> JsonPrimitive(this)
            is Float -> JsonPrimitive(this)
            is Double -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this.toDouble())
            else -> JsonPrimitive(toString())
        }
    }

    private suspend fun insertCommunityPostWithOptionalColumnFallback(
        client: SupabaseClient,
        payload: LinkedHashMap<String, JsonElement>,
    ) = insertCommunityRowWithOptionalColumnFallback(
        client = client,
        table = PostsTable,
        nonRemovableColumns = setOf("id", "tenant_id", "userId", "texto"),
        payload = payload,
    )

    private suspend fun insertCommunityRowWithOptionalColumnFallback(
        client: SupabaseClient,
        table: String,
        nonRemovableColumns: Set<String>,
        payload: LinkedHashMap<String, JsonElement>,
    ) {
        val attemptedRemovedColumns = mutableSetOf<String>()
        var mutablePayload = LinkedHashMap(payload)

        while (mutablePayload.isNotEmpty()) {
            try {
                client.from(table).insert(JsonObject(mutablePayload))
                return
            } catch (error: Throwable) {
                val missingColumn = extractCommunityProblematicColumn(error)
                if (
                    missingColumn.isNullOrBlank() ||
                    missingColumn in nonRemovableColumns ||
                    missingColumn in attemptedRemovedColumns ||
                    !mutablePayload.containsKey(missingColumn)
                ) {
                    throw error
                }
                attemptedRemovedColumns += missingColumn
                mutablePayload = LinkedHashMap(mutablePayload).also { it.remove(missingColumn) }
            }
        }
    }

    private fun extractCommunityProblematicColumn(error: Throwable): String? {
        val message = generateSequence(error) { it.cause }
            .joinToString("\n") { it.message.orEmpty() }
        val patterns = listOf(
            Regex("column\\s+[a-z0-9_]+\\.([a-zA-Z0-9_]+)\\s+does not exist", RegexOption.IGNORE_CASE),
            Regex("column\\s+[\"']?([a-zA-Z0-9_]+)[\"']?\\s+does not exist", RegexOption.IGNORE_CASE),
            Regex("could not find the [\"']?([a-zA-Z0-9_]+)[\"']? column", RegexOption.IGNORE_CASE),
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(message)?.groupValues?.getOrNull(1)
        }
    }

    private companion object {
        const val PostsTable = "posts"
        const val CommentsTable = "posts_comments"
        const val AppConfigTable = "app_config"
        const val CommunityConfigId = "comunidade"
        const val MaxFeedResults = 120
        const val MaxCommentResults = 200L
        const val MaxCategoryLength = 40
        const val CommentColumns =
            "id,postId,userId,userName,avatar,role,plano,plano_cor,plano_icon,patente," +
                "patente_icon,patente_cor,texto,likes,createdAt,updatedAt,tenant_id"
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

@Serializable
private data class CommunityReactionRow(
    val id: String = "",
    val likes: List<String>? = null,
    val hype: List<String>? = null,
    @SerialName("userId") val userId: String? = null,
)

@Serializable
private data class CommunityCommentCountRow(
    val id: String = "",
    val comentarios: Int? = null,
)

@Serializable
private data class CommunityCommentRow(
    val id: String? = null,
    @SerialName("postId") val postId: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("userId") val userId: String? = null,
    @SerialName("userName") val userName: String? = null,
    val avatar: String? = null,
    val role: String? = null,
    val plano: String? = null,
    @SerialName("plano_cor") val planoCor: String? = null,
    @SerialName("plano_icon") val planoIcon: String? = null,
    val patente: String? = null,
    @SerialName("patente_icon") val patenteIcon: String? = null,
    @SerialName("patente_cor") val patenteCor: String? = null,
    val texto: String? = null,
    val likes: List<String>? = null,
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
