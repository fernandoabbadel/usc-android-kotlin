package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.repository.CollectivesRepository
import com.example.usc1.ui.collectives.CollectiveAreaUiConfig
import com.example.usc1.ui.collectives.CollectiveEvent
import com.example.usc1.ui.collectives.CollectiveEventVisibility
import com.example.usc1.ui.collectives.CollectiveGroup
import com.example.usc1.ui.collectives.CollectiveInteractionState
import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.CollectiveLikeResult
import com.example.usc1.ui.collectives.CollectiveLink
import com.example.usc1.ui.collectives.CollectiveMember
import com.example.usc1.ui.collectives.CollectiveMemberRequest
import com.example.usc1.ui.collectives.CollectivePaymentInfo
import com.example.usc1.ui.collectives.CollectiveSellerStats
import com.example.usc1.ui.collectives.CollectiveStoreProduct
import com.example.usc1.ui.collectives.CollectiveStoreState
import com.example.usc1.ui.collectives.LeagueRoleCatalog
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.NumberFormat
import java.text.Normalizer
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Área pública dos coletivos com Supabase direto.
 *
 * Espelha `web-reference/src/lib/leaguesService.ts` (`normalizeLeague`, `fetchLeagueSummaries`,
 * `fetchLeagueById`, `fetchPrimaryLeagueRecord`, `fetchUserLeagueInteractionState`,
 * `toggleUserLeagueLike`, `toggleUserLeagueFollow`, `addLeagueQuizHistory`),
 * `collectiveAreaUiService.ts`, `ligasUscUiService.ts`, `storePublicService.ts` e `turmasService.ts`.
 */
class SupabaseCollectivesRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : CollectivesRepository {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    override suspend fun getAreaUiConfig(
        tenantId: String,
        kind: CollectiveKind,
    ): CollectiveAreaUiConfig = withContext(Dispatchers.IO) {
        val fallback = CollectiveAreaUiConfig.default(kind)
        if (!SupabaseClientProvider.config.isConfigured) return@withContext fallback

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return@withContext fallback

        val docId = buildTenantScopedRowId(cleanTenantId, uiConfigDocId(kind))
        val row = runCatching {
            client.from(AppConfigTable)
                .select(columns = Columns.raw("id,titulo,subtitulo,data")) {
                    filter { eq("id", docId) }
                    limit(count = 1)
                }
                .decodeList<CollectiveAppConfigRow>()
                .firstOrNull()
        }.getOrNull() ?: return@withContext fallback

        val data = row.data ?: JsonObject(emptyMap())
        CollectiveAreaUiConfig(
            titulo = firstNotBlank(row.titulo, data.string("titulo"), fallback.titulo),
            subtitulo = firstNotBlank(row.subtitulo, data.string("subtitulo"), fallback.subtitulo),
            rotuloCard = firstNotBlank(data.string("rotuloCard"), fallback.rotuloCard),
            sidebarLabel = firstNotBlank(data.string("sidebarLabel"), fallback.sidebarLabel),
            managerUserIds = data.stringList("managerUserIds"),
        )
    }

    override suspend fun getCollectives(
        tenantId: String,
        kind: CollectiveKind,
    ): List<CollectiveGroup> = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) return@withContext emptyList()

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return@withContext emptyList()

        // `fetchLeagueSummaries`: ligas ordenam por likes desc (limite 60);
        // comissões e diretório ordenam por nome asc (limite 120).
        val orderByLikes = kind == CollectiveKind.League
        val maxResults = if (orderByLikes) LeagueCatalogLimit else CollectiveCatalogLimit

        val rows = client.from(CollectivesTable)
            .select(columns = Columns.raw(CollectiveSummaryColumns)) {
                filter { eq("tenant_id", cleanTenantId) }
                if (orderByLikes) {
                    order(column = "likes", order = Order.DESCENDING)
                } else {
                    order(column = "nome", order = Order.ASCENDING)
                }
                limit(count = maxResults)
            }
            .decodeList<CollectiveRow>()

        rows.mapNotNull { it.toGroup() }.filter { it.kind == kind }
    }

    override suspend fun getPrimaryCollective(
        tenantId: String,
        kind: CollectiveKind,
    ): CollectiveGroup? {
        // `fetchPrimaryLeagueRecord`: primeira página ativa, senão a primeira publicada.
        val collectives = getCollectives(tenantId, kind)
        return collectives.firstOrNull { it.active } ?: collectives.firstOrNull()
    }

    override suspend fun getCollective(
        tenantId: String,
        kind: CollectiveKind,
        collectiveId: String,
    ): CollectiveGroup? = withContext(Dispatchers.IO) {
        val cleanId = collectiveId.trim()
        if (cleanId.isBlank() || !SupabaseClientProvider.config.isConfigured) return@withContext null

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return@withContext null

        val group = client.from(CollectivesTable)
            .select(columns = Columns.raw(CollectiveDetailColumns)) {
                filter {
                    eq("id", cleanId)
                    eq("tenant_id", cleanTenantId)
                }
                limit(count = 1)
            }
            .decodeList<CollectiveRow>()
            .firstOrNull()
            ?.toGroup()
            ?: return@withContext null

        // `matchesRequestedArea` do web: comissão também aceita registro com turmaId.
        val matchesArea = group.kind == kind ||
            (kind == CollectiveKind.Commission && group.turmaId.isNotBlank())
        if (!matchesArea) return@withContext null

        hydrateEventsFromGlobalCatalog(client, cleanTenantId, group)
    }

    override suspend fun getInteractionState(
        tenantId: String,
        userId: String,
    ): CollectiveInteractionState = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank() || !SupabaseClientProvider.config.isConfigured) {
            return@withContext CollectiveInteractionState()
        }

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val extra = readUserExtra(client, cleanUserId)

        CollectiveInteractionState(
            likedIds = extra.interactionIds(LikedByTenantKey, LikedKey, cleanTenantId),
            followedIds = extra.interactionIds(FollowedByTenantKey, FollowedKey, cleanTenantId),
        )
    }

    override suspend fun toggleLike(
        tenantId: String,
        userId: String,
        collectiveId: String,
    ): CollectiveLikeResult = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        val cleanId = collectiveId.trim()
        require(cleanUserId.isNotBlank() && cleanId.isNotBlank()) { "Entre na sua conta para curtir." }

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val update = updateInteractionIds(
            client = client,
            userId = cleanUserId,
            tenantId = cleanTenantId,
            collectiveId = cleanId,
            byTenantKey = LikedByTenantKey,
            key = LikedKey,
        )

        val isLiked = !update.wasActive
        if (update.changed) {
            changeLikeCount(client, cleanTenantId, cleanId, if (isLiked) 1 else -1)
        }

        CollectiveLikeResult(likedIds = update.nextIds, isLiked = isLiked)
    }

    override suspend fun toggleFollow(
        tenantId: String,
        userId: String,
        collectiveId: String,
    ): List<String> = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        val cleanId = collectiveId.trim()
        require(cleanUserId.isNotBlank() && cleanId.isNotBlank()) { "Entre na sua conta para seguir." }

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        updateInteractionIds(
            client = client,
            userId = cleanUserId,
            tenantId = cleanTenantId,
            collectiveId = cleanId,
            byTenantKey = FollowedByTenantKey,
            key = FollowedKey,
        ).nextIds
    }

    override suspend fun getStore(
        tenantId: String,
        collectiveId: String,
        loadProducts: Boolean,
    ): CollectiveStoreState = withContext(Dispatchers.IO) {
        val cleanId = collectiveId.trim()
        if (cleanId.isBlank() || !SupabaseClientProvider.config.isConfigured) {
            return@withContext CollectiveStoreState()
        }

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return@withContext CollectiveStoreState()

        // `fetchStoreCategories` + `isLeagueStoreCategory`: casa por seller_id e aceita
        // seller_type tenant/league ou vazio.
        val category = runCatching {
            client.from(CategoriesTable)
                .select(columns = Columns.raw(StoreCategoryColumns)) {
                    filter { eq("tenant_id", cleanTenantId) }
                    limit(count = StoreCategoryLimit)
                }
                .decodeList<CollectiveStoreCategoryRow>()
                .firstOrNull { row ->
                    val sellerId = row.sellerId?.trim().orEmpty()
                    val sellerType = row.sellerType?.trim()?.lowercase(Locale.ROOT).orEmpty()
                    sellerId == cleanId && (sellerType == "tenant" || sellerType == "league" || sellerType.isBlank())
                }
        }.getOrNull()

        val enabled = category?.visible != false
        val coverImage = resolveRemoteImageUrl(
            firstNotBlank(category?.coverImg, category?.logoUrl),
        )

        if (!loadProducts || !enabled) {
            return@withContext CollectiveStoreState(enabled = enabled, coverImageUrl = coverImage)
        }

        // `fetchStoreProductsBySeller`: `league` consulta `seller_type = tenant`.
        val products = runCatching {
            client.from(ProductsTable)
                .select(columns = Columns.raw(StoreProductColumns)) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("active", true)
                        eq("aprovado", true)
                        eq("seller_type", "tenant")
                        eq("seller_id", cleanId)
                    }
                    order(column = "nome", order = Order.ASCENDING)
                    limit(count = StoreProductLimit)
                }
                .decodeList<CollectiveStoreProductRow>()
                .map { it.toStoreProduct() }
        }.getOrDefault(emptyList())

        CollectiveStoreState(enabled = true, coverImageUrl = coverImage, products = products)
    }

    override suspend fun getTurmaMemberCounts(
        tenantId: String,
        turmaIds: List<String>,
    ): Map<String, Int> = withContext(Dispatchers.IO) {
        val normalized = turmaIds.mapNotNull { normalizeTurmaId(it).takeIf(String::isNotBlank) }.distinct()
        if (normalized.isEmpty() || !SupabaseClientProvider.config.isConfigured) return@withContext emptyMap()

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return@withContext emptyMap()

        normalized.associateWith { turmaId ->
            runCatching {
                client.from(UsersTable)
                    .select(columns = Columns.raw("uid")) {
                        filter {
                            eq("tenant_id", cleanTenantId)
                            isIn("turma", listOf(turmaId, turmaId.lowercase(Locale.ROOT)))
                        }
                        limit(count = TurmaMembersLimit)
                    }
                    .decodeList<CollectiveUserIdRow>()
                    .size
            }.getOrDefault(0)
        }
    }

    override suspend fun getTurmaMemberIds(
        tenantId: String,
        turmaId: String,
    ): List<String> = withContext(Dispatchers.IO) {
        val normalized = normalizeTurmaId(turmaId)
        if (normalized.isBlank() || !SupabaseClientProvider.config.isConfigured) return@withContext emptyList()

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return@withContext emptyList()

        runCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw("uid")) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        isIn("turma", listOf(normalized, normalized.lowercase(Locale.ROOT)))
                    }
                    limit(count = TurmaMembersLimit)
                }
                .decodeList<CollectiveUserIdRow>()
                .mapNotNull { it.uid?.trim()?.takeIf(String::isNotBlank) }
        }.getOrDefault(emptyList())
    }

    override suspend fun getSellerStats(
        tenantId: String,
        collectiveIds: List<String>,
    ): Map<String, CollectiveSellerStats> = withContext(Dispatchers.IO) {
        val ids = collectiveIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty() || !SupabaseClientProvider.config.isConfigured) return@withContext emptyMap()

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return@withContext emptyMap()

        val rows = runCatching {
            client.from(ProductsTable)
                .select(columns = Columns.raw("seller_id,vendidos,likes")) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("active", true)
                        eq("aprovado", true)
                        eq("seller_type", "tenant")
                        isIn("seller_id", ids)
                    }
                    limit(count = SellerStatsLimit)
                }
                .decodeList<CollectiveSellerStatsRow>()
        }.getOrDefault(emptyList())

        val stats = ids.associateWith { CollectiveSellerStats(sellerId = it) }.toMutableMap()
        rows.forEach { row ->
            val sellerId = row.sellerId?.trim().orEmpty()
            val current = stats[sellerId] ?: return@forEach
            stats[sellerId] = current.copy(
                soldCount = current.soldCount + (row.vendidos ?: 0),
                exposedCount = current.exposedCount + 1,
                likesCount = current.likesCount + (row.likes as? JsonArray)?.size.orZero(),
            )
        }
        stats
    }

    override suspend fun addQuizHistory(
        userId: String,
        topMatch: String,
        keywords: List<String>,
    ) = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank() || !SupabaseClientProvider.config.isConfigured) return@withContext

        val client = clientProvider()
        val payload = buildJsonObject {
            put("userId", cleanUserId)
            put("date", Instant.now().toString())
            put("topMatch", topMatch.trim().take(QuizTopMatchMaxLength))
            put(
                "keywords",
                buildJsonArray { keywords.take(QuizKeywordsMaxCount).forEach { add(JsonPrimitive(it)) } },
            )
        }

        // O web ignora `42P01` (tabela ausente) e segue sem histórico.
        runCatching { client.from(QuizHistoryTable).insert(payload) }
        Unit
    }

    // ------------------------------------------------------------------
    // Normalização
    // ------------------------------------------------------------------

    private fun CollectiveRow.toGroup(): CollectiveGroup? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null

        val dataField = data ?: JsonObject(emptyMap())
        val kind = resolveKind(dataField.string("category").ifBlank { dataField.string("categoria") })
        val members = parseMembers(membros ?: dataField["membros"])
        val memberIds = membrosIds.orEmpty().mapNotNull { it.trim().takeIf(String::isNotBlank) }
        val logoUrl = resolveRemoteImageUrl(firstNotBlank(logoUrl, logo, foto))
        val resolvedImage = resolveRemoteImageUrl(foto) ?: logoUrl

        return CollectiveGroup(
            id = cleanId,
            name = firstNotBlank(nome, dataField.string("nome"), "Liga"),
            acronym = firstNotBlank(sigla, dataField.string("sigla")),
            turmaId = firstNotBlank(dataField.string("turmaId"), turmaId).uppercase(Locale.ROOT),
            president = firstNotBlank(presidente, dataField.string("presidente")),
            description = firstNotBlank(descricao, dataField.string("descricao")),
            overview = firstNotBlank(visaoGeral, dataField.string("visaoGeral")),
            bizu = firstNotBlank(bizu, dataField.string("bizu")),
            kind = kind,
            visible = dataField.bool("visivel") ?: visivel ?: false,
            active = dataField.bool("ativa") ?: ativa ?: false,
            likesCount = maxOf(0, likes ?: dataField.int("likes") ?: 0),
            membersCount = maxOf(
                0,
                dataField.int("membersCount")
                    ?: memberIds.size.takeIf { it > 0 }
                    ?: members.size,
            ),
            imageUrl = resolvedImage,
            members = members,
            memberRequests = parseMemberRequests(dataField["memberRequests"]),
            events = parseEvents(eventos ?: dataField["eventos"]),
            links = parseLinks(dataField["links"] ?: links),
            paymentInfo = parsePaymentInfo(paymentConfig ?: dataField["paymentConfig"]),
            managerUserIds = dataField.stringList("managerUserIds"),
        )
    }

    /** `normalizeLeagueCategory` do web: fallback é sempre `liga`. */
    private fun resolveKind(rawCategory: String): CollectiveKind {
        val raw = rawCategory.trim().lowercase(Locale.ROOT).stripAccents()
        return when (raw) {
            "comissao", "comissoes" -> CollectiveKind.Commission
            "diretorio" -> CollectiveKind.Directory
            else -> CollectiveKind.League
        }
    }

    private fun parseMembers(source: JsonElement?): List<CollectiveMember> {
        val entries = (source as? JsonArray)?.toList().orEmpty()
        return entries.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            CollectiveMember(
                id = obj.string("id"),
                name = obj.string("nome").ifBlank { "Sem nome" },
                role = LeagueRoleCatalog.resolveRoleLabel(obj.string("cargo")),
                photoUrl = resolveRemoteImageUrl(obj.string("foto")),
                profileLink = obj.string("linkPerfil"),
            )
        }
    }

    private fun parseMemberRequests(source: JsonElement?): List<CollectiveMemberRequest> {
        val entries = (source as? JsonArray)?.toList().orEmpty()
        val seen = mutableSetOf<String>()
        return entries.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val userId = firstNotBlank(obj.string("userId"), obj.string("requesterUserId"))
            if (userId.isBlank() || !seen.add(userId)) return@mapNotNull null
            CollectiveMemberRequest(
                id = obj.string("id"),
                userId = userId,
                name = obj.string("nome").ifBlank { "Atleta" },
                photoUrl = resolveRemoteImageUrl(obj.string("foto")),
                turma = obj.string("turma"),
                requestedRole = LeagueRoleCatalog.resolveRoleLabel(obj.string("requestedRole")),
                createdAt = obj.string("createdAt"),
            )
        }
    }

    private fun parseEvents(source: JsonElement?): List<CollectiveEvent> {
        val entries = (source as? JsonArray)?.toList().orEmpty()
        return entries.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val title = obj.string("titulo")
            if (title.isBlank()) return@mapNotNull null
            CollectiveEvent(
                id = obj.string("id"),
                title = title,
                date = obj.string("data"),
                time = obj.string("hora"),
                place = obj.string("local"),
                description = obj.string("descricao"),
                visibility = resolveEventVisibility(obj),
                imageUrl = resolveRemoteImageUrl(obj.string("imagem")),
                eventLink = obj.string("linkEvento"),
                globalEventId = obj.string("globalEventId"),
            )
        }
    }

    /** `normalizeLeagueEventVisibility`: aceita os apelidos usados pelo web. */
    private fun resolveEventVisibility(obj: JsonObject): CollectiveEventVisibility {
        val stats = obj["stats"]?.asObjectOrNull()
        val raw = firstNotBlank(
            obj.string("visibility"),
            obj.string("visibilidade"),
            obj.string("leagueEventVisibility"),
            obj.string("eventVisibility"),
            stats.string("leagueEventVisibility"),
            stats.string("eventVisibility"),
        ).trim().lowercase(Locale.ROOT)

        return if (raw == "internal" || raw == "interno") {
            CollectiveEventVisibility.Internal
        } else {
            CollectiveEventVisibility.Public
        }
    }

    private fun parseLinks(source: JsonElement?): List<CollectiveLink> {
        val entries = (source as? JsonArray)?.toList().orEmpty()
        return entries.mapIndexedNotNull { index, element ->
            val obj = element.asObjectOrNull() ?: return@mapIndexedNotNull null
            val url = obj.string("url")
            if (url.isBlank()) return@mapIndexedNotNull null
            val type = obj.string("type").trim().lowercase(Locale.ROOT).ifBlank { "outro" }
            CollectiveLink(
                id = obj.string("id").ifBlank { "link-$index" },
                label = obj.string("label"),
                type = type,
                url = url,
            )
        }.take(LinksMaxCount)
    }

    private fun parsePaymentInfo(source: JsonElement?): CollectivePaymentInfo {
        val obj = source?.asObjectOrNull() ?: return CollectivePaymentInfo()
        return CollectivePaymentInfo(
            pixKey = obj.string("chave"),
            bank = obj.string("banco"),
            holder = obj.string("titular"),
            whatsapp = obj.string("whatsapp"),
        )
    }

    private fun CollectiveStoreProductRow.toStoreProduct(): CollectiveStoreProduct = CollectiveStoreProduct(
        id = id.trim(),
        name = nome?.trim().orEmpty().ifBlank { "Produto sem nome" },
        priceLabel = currencyFormatter.format(preco ?: 0.0),
        imageUrl = resolveRemoteImageUrl(img),
        category = categoria?.trim().orEmpty(),
        tagLabel = tagLabel?.trim().orEmpty(),
    )

    /**
     * `hydrateLeagueEventsFromGlobalCatalog`: quando o evento da liga aponta para um
     * evento global, o web recarrega data/hora/local publicados do catálogo.
     */
    private suspend fun hydrateEventsFromGlobalCatalog(
        client: SupabaseClient,
        tenantId: String,
        group: CollectiveGroup,
    ): CollectiveGroup {
        val globalIds = group.events
            .mapNotNull { it.globalEventId.trim().takeIf(String::isNotBlank) }
            .distinct()
        if (globalIds.isEmpty()) return group

        val globalEvents = runCatching {
            client.from(EventsTable)
                .select(columns = Columns.raw(GlobalEventColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                        isIn("id", globalIds)
                    }
                    limit(count = globalIds.size.toLong())
                }
                .decodeList<CollectiveGlobalEventRow>()
                .associateBy { it.id.trim() }
        }.getOrDefault(emptyMap())

        if (globalEvents.isEmpty()) return group

        return group.copy(
            events = group.events.map { event ->
                val global = globalEvents[event.globalEventId.trim()] ?: return@map event
                event.copy(
                    title = firstNotBlank(global.titulo, event.title),
                    date = firstNotBlank(global.data, event.date),
                    time = firstNotBlank(global.hora, event.time),
                    place = firstNotBlank(global.local, event.place),
                    description = firstNotBlank(global.descricao, event.description),
                    imageUrl = resolveRemoteImageUrl(global.imagem) ?: event.imageUrl,
                )
            },
        )
    }

    // ------------------------------------------------------------------
    // users.extra
    // ------------------------------------------------------------------

    private data class InteractionUpdate(
        val nextIds: List<String>,
        val wasActive: Boolean,
        val changed: Boolean,
    )

    private suspend fun readUserExtra(client: SupabaseClient, userId: String): JsonObject {
        return runCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw("uid,extra")) {
                    filter { eq("uid", userId) }
                    limit(count = 1)
                }
                .decodeList<CollectiveUserExtraRow>()
                .firstOrNull()
                ?.extra
        }.getOrNull() ?: JsonObject(emptyMap())
    }

    /** `updateUserLeagueInteractionIds` do web: sempre grava por tenant quando há tenant. */
    private suspend fun updateInteractionIds(
        client: SupabaseClient,
        userId: String,
        tenantId: String,
        collectiveId: String,
        byTenantKey: String,
        key: String,
    ): InteractionUpdate {
        val currentExtra = readUserExtra(client, userId)
        val currentByTenant = currentExtra[byTenantKey]?.asObjectOrNull() ?: JsonObject(emptyMap())
        val currentIds = if (tenantId.isNotBlank()) {
            currentByTenant[tenantId].asStringList()
        } else {
            currentExtra[key].asStringList()
        }

        val wasActive = currentIds.contains(collectiveId)
        val nextIds = if (wasActive) {
            currentIds.filterNot { it == collectiveId }
        } else {
            (currentIds + collectiveId).distinct()
        }

        val nextExtra = buildJsonObject {
            currentExtra.forEach { (entryKey, value) ->
                if (entryKey != byTenantKey && entryKey != key) put(entryKey, value)
            }
            if (tenantId.isNotBlank()) {
                put(
                    byTenantKey,
                    buildJsonObject {
                        currentByTenant.forEach { (tenantKey, value) ->
                            if (tenantKey != tenantId) put(tenantKey, value)
                        }
                        put(tenantId, buildJsonArray { nextIds.forEach { add(JsonPrimitive(it)) } })
                    },
                )
                currentExtra[key]?.let { put(key, it) }
            } else {
                currentExtra[byTenantKey]?.let { put(byTenantKey, it) }
                put(key, buildJsonArray { nextIds.forEach { add(JsonPrimitive(it)) } })
            }
        }

        val changed = nextIds != currentIds
        if (changed) {
            client.from(UsersTable).update(
                buildJsonObject {
                    put("extra", nextExtra)
                    put("updatedAt", Instant.now().toString())
                },
            ) {
                filter { eq("uid", userId) }
            }
        }

        return InteractionUpdate(nextIds = nextIds, wasActive = wasActive, changed = changed)
    }

    /** `changeLeagueLikeCount` do web, sem callable: lê e regrava `ligas_config.likes`. */
    private suspend fun changeLikeCount(
        client: SupabaseClient,
        tenantId: String,
        collectiveId: String,
        delta: Int,
    ) {
        val current = runCatching {
            client.from(CollectivesTable)
                .select(columns = Columns.raw("id,likes")) {
                    filter {
                        eq("id", collectiveId)
                        if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                    }
                    limit(count = 1)
                }
                .decodeList<CollectiveLikesRow>()
                .firstOrNull()
                ?.likes
        }.getOrNull() ?: 0

        val next = maxOf(0, current + delta)
        client.from(CollectivesTable).update(
            buildJsonObject {
                put("likes", next)
                put("updatedAt", Instant.now().toString())
            },
        ) {
            filter {
                eq("id", collectiveId)
                if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private suspend fun resolveTenantId(client: SupabaseClient, tenantId: String): String {
        val clean = tenantId.trim()
        if (clean.isNotBlank()) return clean
        return runCatching { SupabaseTenantResolver.resolveActiveTenantId(client) }.getOrDefault("")
    }

    private fun uiConfigDocId(kind: CollectiveKind): String = when (kind) {
        CollectiveKind.League -> "ligas_usc_ui"
        CollectiveKind.Commission -> "comissoes_ui"
        CollectiveKind.Directory -> "diretorio_ui"
    }

    private fun buildTenantScopedRowId(tenantId: String, baseId: String): String =
        "tenant:${tenantId.trim()}::${baseId.trim()}"

    /** `normalizeTurmaId` de `turmasService.ts`. */
    private fun normalizeTurmaId(raw: String): String {
        val input = raw.trim().uppercase(Locale.ROOT)
        if (input.isBlank()) return ""
        if (Regex("""^T\d{1,3}$""").matches(input)) {
            return "T${input.drop(1).toInt()}"
        }
        val digits = input.filter(Char::isDigit)
        if (digits.isBlank()) return ""
        return "T${digits.toInt()}"
    }

    private fun JsonObject.interactionIds(
        byTenantKey: String,
        key: String,
        tenantId: String,
    ): List<String> {
        val byTenant = this[byTenantKey]?.asObjectOrNull()
        if (tenantId.isNotBlank() && byTenant != null) {
            return byTenant[tenantId].asStringList()
        }
        return this[key].asStringList()
    }

    private fun firstNotBlank(vararg values: String?): String {
        values.forEach { value ->
            val clean = value?.trim()
            if (!clean.isNullOrBlank()) return clean
        }
        return ""
    }

    private fun String.stripAccents(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    private fun Int?.orZero(): Int = this ?: 0

    private companion object {
        const val CollectivesTable = "ligas_config"
        const val ProductsTable = "produtos"
        const val CategoriesTable = "categorias"
        const val EventsTable = "eventos"
        const val UsersTable = "users"
        const val AppConfigTable = "app_config"
        const val QuizHistoryTable = "quiz_history"

        const val LeagueCatalogLimit = 60L
        const val CollectiveCatalogLimit = 120L
        const val StoreCategoryLimit = 300L
        const val StoreProductLimit = 12L
        const val SellerStatsLimit = 5000L
        const val TurmaMembersLimit = 200L
        const val LinksMaxCount = 12
        const val QuizTopMatchMaxLength = 120
        const val QuizKeywordsMaxCount = 60

        const val LikedByTenantKey = "likedLeagueIdsByTenant"
        const val LikedKey = "likedLeagueIds"
        const val FollowedByTenantKey = "followedLeagueIdsByTenant"
        const val FollowedKey = "followedLeagueIds"

        /** `LEAGUE_SUMMARY_SELECT_COLUMNS` do web. */
        const val CollectiveSummaryColumns =
            "id,tenant_id,nome,sigla,descricao,foto,logoUrl,logo,visivel,ativa,bizu,likes,status,membrosIds,data"

        /** `LEAGUES_SELECT_COLUMNS` do web. */
        const val CollectiveDetailColumns =
            "id,tenant_id,nome,sigla,presidente,descricao,foto,logoUrl,logo,visivel,ativa,membros," +
                "membrosIds,eventos,payment_config,bizu,likes,status,data"

        const val StoreCategoryColumns = "id,tenant_id,nome,cover_img,logo_url,visible,seller_type,seller_id"
        const val StoreProductColumns =
            "id,tenant_id,nome,preco,img,categoria,tagLabel,active,aprovado,seller_type,seller_id"
        const val GlobalEventColumns = "id,tenant_id,titulo,data,hora,local,descricao,imagem"
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
    val visaoGeral: String? = null,
    val foto: String? = null,
    val logoUrl: String? = null,
    val logo: String? = null,
    val visivel: Boolean? = null,
    val ativa: Boolean? = null,
    val membros: JsonElement? = null,
    val membrosIds: List<String>? = null,
    val eventos: JsonElement? = null,
    val links: JsonElement? = null,
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
    val bizu: String? = null,
    val likes: Int? = null,
    val status: String? = null,
    val turmaId: String? = null,
    val data: JsonObject? = null,
)

@Serializable
private data class CollectiveLikesRow(
    val id: String = "",
    val likes: Int? = null,
)

@Serializable
private data class CollectiveAppConfigRow(
    val id: String = "",
    val titulo: String? = null,
    val subtitulo: String? = null,
    val data: JsonObject? = null,
)

@Serializable
private data class CollectiveStoreCategoryRow(
    val id: String = "",
    val nome: String? = null,
    @SerialName("cover_img") val coverImg: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    val visible: Boolean? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
)

@Serializable
private data class CollectiveStoreProductRow(
    val id: String = "",
    val nome: String? = null,
    val preco: Double? = null,
    val img: String? = null,
    val categoria: String? = null,
    val tagLabel: String? = null,
)

@Serializable
private data class CollectiveSellerStatsRow(
    @SerialName("seller_id") val sellerId: String? = null,
    val vendidos: Int? = null,
    val likes: JsonElement? = null,
)

@Serializable
private data class CollectiveGlobalEventRow(
    val id: String = "",
    val titulo: String? = null,
    val data: String? = null,
    val hora: String? = null,
    val local: String? = null,
    val descricao: String? = null,
    val imagem: String? = null,
)

@Serializable
private data class CollectiveUserExtraRow(
    val uid: String? = null,
    val extra: JsonObject? = null,
)

@Serializable
private data class CollectiveUserIdRow(
    val uid: String? = null,
)

private fun JsonObject?.string(key: String): String {
    val primitive = this?.get(key) as? JsonPrimitive ?: return ""
    return primitive.contentOrNull?.trim().orEmpty()
}

private fun JsonObject?.int(key: String): Int? {
    val primitive = this?.get(key) as? JsonPrimitive ?: return null
    return primitive.intOrNull ?: primitive.contentOrNull?.toIntOrNull()
}

private fun JsonObject?.bool(key: String): Boolean? {
    val primitive = this?.get(key) as? JsonPrimitive ?: return null
    return primitive.booleanOrNull ?: primitive.contentOrNull?.toBooleanStrictOrNull()
}

private fun JsonObject?.stringList(key: String): List<String> = this?.get(key).asStringList()

private fun JsonElement?.asStringList(): List<String> {
    val array = this as? JsonArray ?: return emptyList()
    return array.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotBlank) }
        .distinct()
}

private fun JsonElement.asObjectOrNull(): JsonObject? {
    if (this is JsonNull) return null
    return runCatching { jsonObject }.getOrNull()
}
