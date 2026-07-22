package com.example.usc1.data.repository

import com.example.usc1.BuildConfig
import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.domain.model.HomeDashboardBundle
import com.example.usc1.domain.model.HomeDashboardClassStat
import com.example.usc1.domain.model.HomeDashboardEvent
import com.example.usc1.domain.model.HomeDashboardLeague
import com.example.usc1.domain.model.HomeDashboardPartner
import com.example.usc1.domain.model.HomeDashboardPost
import com.example.usc1.domain.model.HomeDashboardProduct
import com.example.usc1.domain.model.HomeDashboardProfile
import com.example.usc1.domain.model.HomeDashboardSalesEvent
import com.example.usc1.domain.model.TenantAppModulesCatalog
import com.example.usc1.domain.repository.HomeDashboardRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

class SupabaseHomeDashboardRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : HomeDashboardRepository {
    override suspend fun getDashboard(
        tenantId: String,
        tenantSlug: String,
        userId: String?,
        forceRefresh: Boolean,
    ): HomeDashboardBundle = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        require(cleanTenantId.isNotBlank()) { "Selecione uma atlética antes de abrir o dashboard." }
        val cleanTenantSlug = tenantSlug.trim().lowercase()
        val cleanUserId = userId?.trim().orEmpty()
        val client = clientProvider()

        supervisorScope {
            val dashboardDeferred = async {
                runCatching { fetchRpcBundle(client, cleanTenantId, cleanUserId) }
                    .getOrElse { fetchFallbackBundle(client, cleanTenantId, cleanUserId) }
            }
            val profileDeferred = async {
                runCatching { fetchProfile(client, cleanTenantId, cleanUserId) }
                    .getOrDefault(HomeDashboardProfile())
            }
            val salesEventDeferred = async {
                runCatching { fetchActiveSalesEvent(client, cleanTenantId) }.getOrNull()
            }
            val modulesDeferred = async {
                fetchEffectiveModuleVisibility(
                    client = client,
                    tenantId = cleanTenantId,
                    tenantSlug = cleanTenantSlug,
                    forceRefresh = forceRefresh,
                )
            }

            dashboardDeferred.await().copy(
                profile = profileDeferred.await(),
                activeSalesEvent = salesEventDeferred.await(),
                moduleVisibility = modulesDeferred.await(),
            )
        }
    }

    private suspend fun fetchRpcBundle(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): HomeDashboardBundle {
        val payload = client.postgrest.rpc(
            function = DashboardRpc,
            parameters = buildJsonObject {
                put("p_tenant_id", tenantId)
                if (userId.isBlank()) put("p_user_id", JsonNull) else put("p_user_id", userId)
            },
        ).decodeAs<JsonObject>()
        return DashboardBundleMapper.fromJson(payload)
    }

    private suspend fun fetchFallbackBundle(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): HomeDashboardBundle = supervisorScope {
        val eventsDeferred = async {
            runCatching { fetchFallbackEvents(client, tenantId) }.getOrDefault(emptyList())
        }
        val productsDeferred = async {
            runCatching { fetchFallbackProducts(client, tenantId, userId) }.getOrDefault(emptyList())
        }
        val partnersDeferred = async {
            runCatching { fetchFallbackPartners(client, tenantId) }.getOrDefault(emptyList())
        }
        val leaguesDeferred = async {
            runCatching { fetchFallbackLeagues(client, tenantId) }.getOrDefault(emptyList())
        }
        val postsDeferred = async {
            runCatching { fetchFallbackPosts(client, tenantId, userId) }.getOrDefault(emptyList())
        }
        val trainingDeferred = async {
            runCatching { fetchFallbackTrainingImages(client, tenantId) }.getOrDefault(emptyList())
        }
        val totalMembersDeferred = async {
            runCatching { countMembers(client, tenantId) }.getOrDefault(0)
        }
        val huntDeferred = async {
            runCatching { fetchCapturedFreshmen(client, tenantId, userId) }.getOrDefault(0)
        }

        HomeDashboardBundle(
            events = eventsDeferred.await(),
            products = productsDeferred.await(),
            partners = partnersDeferred.await(),
            leagues = leaguesDeferred.await(),
            posts = postsDeferred.await(),
            trainingImageUrls = trainingDeferred.await(),
            capturedFreshmen = huntDeferred.await(),
            totalMembers = totalMembersDeferred.await(),
        )
    }

    private suspend fun fetchFallbackEvents(
        client: SupabaseClient,
        tenantId: String,
    ): List<HomeDashboardEvent> {
        return client.from(EventsTable)
            .select(columns = Columns.raw(EventFallbackColumns)) {
                filter { eq("tenant_id", tenantId) }
                order(column = "data", order = Order.ASCENDING)
                limit(count = 24)
            }
            .decodeList<HomeEventFallbackRow>()
            .asSequence()
            .filter { row -> row.status.orEmpty().lowercase() !in ClosedEventStatuses }
            .filter { row -> isDashboardDateCurrent(row.data) }
            .filter { row -> row.id.isNotBlank() }
            .take(5)
            .map { row ->
                val stats = row.stats.asObject()
                HomeDashboardEvent(
                    id = row.id.trim(),
                    title = row.titulo.trim().ifBlank { "Evento" },
                    date = row.data?.trim().orEmpty(),
                    time = row.hora?.trim().orEmpty(),
                    location = row.local?.trim().orEmpty(),
                    imageUrl = row.imagem?.trim()?.takeIf(String::isNotBlank),
                    type = row.tipo?.trim().orEmpty(),
                    status = row.status?.trim().orEmpty().ifBlank { "ativo" },
                    likesCount = stats?.intValue("likes")?.coerceAtLeast(0) ?: 0,
                    interestedCount = (
                        (stats?.intValue("confirmados") ?: 0) +
                            (stats?.intValue("talvez") ?: 0)
                        ).coerceAtLeast(0),
                    viewerHasLiked = false,
                    viewerIsInterested = false,
                    imagePositionY = row.imagePositionY?.coerceIn(0.0, 100.0),
                )
            }
            .toList()
    }

    private suspend fun fetchFallbackProducts(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): List<HomeDashboardProduct> {
        return client.from(ProductsTable)
            .select(columns = Columns.raw(ProductFallbackColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("active", true)
                    eq("aprovado", true)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = 8)
            }
            .decodeList<HomeProductFallbackRow>()
            .mapNotNull { row ->
                val id = row.id.trim()
                if (id.isBlank()) return@mapNotNull null
                HomeDashboardProduct(
                    id = id,
                    name = row.nome.trim().ifBlank { "Produto" },
                    price = (row.preco ?: 0.0).coerceAtLeast(0.0),
                    imageUrl = row.img?.trim()?.takeIf(String::isNotBlank),
                    likesCount = row.likes.size,
                    viewerHasLiked = userId.isNotBlank() && userId in row.likes,
                )
            }
    }

    private suspend fun fetchFallbackPartners(
        client: SupabaseClient,
        tenantId: String,
    ): List<HomeDashboardPartner> {
        return client.from(PartnersTable)
            .select(columns = Columns.raw(PartnerFallbackColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("status", "active")
                }
                order(column = "nome", order = Order.ASCENDING)
                limit(count = 50)
            }
            .decodeList<HomePartnerFallbackRow>()
            .mapNotNull { row ->
                val id = row.id.trim()
                if (id.isBlank()) return@mapNotNull null
                HomeDashboardPartner(
                    id = id,
                    name = row.nome.trim().ifBlank { "Parceiro" },
                    logoUrl = row.imgLogo?.trim()?.takeIf(String::isNotBlank),
                    coverUrl = row.imgCapa?.trim()?.takeIf(String::isNotBlank),
                    category = row.categoria?.trim().orEmpty(),
                    tier = row.tier?.trim()?.lowercase().orEmpty().ifBlank { "standard" },
                )
            }
    }

    private suspend fun fetchProfile(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): HomeDashboardProfile {
        if (userId.isBlank() || userId.startsWith("guest_virtual_")) return HomeDashboardProfile()
        val row = client.from(UsersTable)
            .select(columns = Columns.raw(ProfileColumns)) {
                filter {
                    eq("uid", userId)
                    eq("tenant_id", tenantId)
                }
                limit(count = 1)
            }
            .decodeList<HomeProfileRow>()
            .firstOrNull()
            ?: return HomeDashboardProfile()

        return HomeDashboardProfile(
            avatarUrl = row.foto?.trim()?.takeIf(String::isNotBlank),
            className = row.turma?.trim().orEmpty(),
            planName = row.plano?.trim().orEmpty(),
            level = (row.level ?: 1).coerceAtLeast(1),
        )
    }

    private suspend fun fetchActiveSalesEvent(
        client: SupabaseClient,
        tenantId: String,
    ): HomeDashboardSalesEvent? {
        return client.from(EventsTable)
            .select(columns = Columns.raw(SalesEventColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("status", "ativo")
                }
                order(column = "data", order = Order.ASCENDING)
                limit(count = 30)
            }
            .decodeList<SalesEventRow>()
            .firstNotNullOfOrNull { row ->
                val eventParty = row.dataExtra.asObject()?.get("eventParty").asObject() ?: return@firstNotNullOfOrNull null
                if (!eventParty.booleanValue("enabled")) return@firstNotNullOfOrNull null
                HomeDashboardSalesEvent(
                    id = row.id,
                    title = row.titulo.ifBlank { "Evento" },
                    menuTitle = eventParty.stringValue("menuTitle")
                        .ifBlank { eventParty.stringValue("cardapioTitle") }
                        .ifBlank { "Menu do evento" },
                    imageUrl = row.imagem?.trim()?.takeIf(String::isNotBlank),
                )
            }
    }

    private suspend fun fetchEffectiveModuleVisibility(
        client: SupabaseClient,
        tenantId: String,
        tenantSlug: String,
        forceRefresh: Boolean,
    ): Map<String, Boolean> {
        try {
            val effectiveModules = fetchPublicEffectiveModuleVisibility(
                tenantId = tenantId,
                tenantSlug = tenantSlug,
                forceRefresh = forceRefresh,
            )
            LastEffectiveModuleVisibility[tenantId] = effectiveModules
            return effectiveModules
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // O endpoint é a única fonte cliente-segura dos bloqueios do perfil global.
        }

        val lastEffectiveModules = LastEffectiveModuleVisibility[tenantId]
        val tenantModules = try {
            fetchTenantModuleVisibility(client, tenantId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            null
        }

        // Limitação consciente: o documento da tenant não contém os bloqueios do perfil
        // global. Quando existe um mapa efetivo anterior, a interseção impede reabrir
        // silenciosamente um módulo que a plataforma havia proibido.
        return DashboardModulesMapper.fallbackAfterEndpointFailure(
            tenantModules = tenantModules,
            lastEffectiveModules = lastEffectiveModules,
        )
    }

    private suspend fun fetchPublicEffectiveModuleVisibility(
        tenantId: String,
        tenantSlug: String,
        forceRefresh: Boolean,
    ): Map<String, Boolean> {
        val response = PublicWebClient.get("${resolveWebAppBaseUrl()}/api/public/dashboard") {
            parameter("tenantId", tenantId)
            if (tenantSlug.isNotBlank()) parameter("tenant", tenantSlug)
            if (forceRefresh) parameter("refresh", "1")
        }
        check(response.status.value in 200..299) {
            "Falha ao carregar os módulos efetivos do dashboard (${response.status.value})."
        }

        val payload = DashboardJson.parseToJsonElement(response.bodyAsText()) as? JsonObject
            ?: error("Resposta inválida dos módulos efetivos do dashboard.")
        return DashboardModulesMapper.fromPublicDashboardResponse(payload)
            ?: error("A resposta pública não trouxe um mapa completo de módulos.")
    }

    private fun resolveWebAppBaseUrl(): String {
        val configuredUrl = BuildConfig.WEB_APP_URL.trim().trimEnd('/')
        val normalizedScheme = configuredUrl.lowercase()
        val isAllowed = normalizedScheme.startsWith("https://") ||
            (BuildConfig.DEBUG && normalizedScheme.startsWith("http://"))
        return configuredUrl.takeIf { it.isNotBlank() && isAllowed } ?: DefaultWebAppUrl
    }

    private suspend fun fetchTenantModuleVisibility(
        client: SupabaseClient,
        tenantId: String,
    ): Map<String, Boolean>? {
        val rowId = "tenant:$tenantId::app_modules"
        val row = client.from(AppConfigTable)
            .select(columns = Columns.raw("id,data")) {
                filter { eq("id", rowId) }
                limit(count = 1)
            }
            .decodeList<HomeAppConfigRow>()
            .firstOrNull()
            ?: return null
        val modules = row.data.asObject()?.get("modules").asObject() ?: return null
        return modules.mapNotNull { (key, value) ->
            (value as? JsonPrimitive)?.booleanOrNull?.let { key to it }
        }.toMap()
    }

    private suspend fun fetchFallbackLeagues(
        client: SupabaseClient,
        tenantId: String,
    ): List<HomeDashboardLeague> {
        return client.from(LeaguesTable)
            .select(columns = Columns.raw(LeagueColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("visivel", true)
                }
                order(column = "likes", order = Order.DESCENDING)
                limit(count = 6)
            }
            .decodeList<LeagueFallbackRow>()
            .filter { it.category.isNullOrBlank() || it.category.equals("liga", ignoreCase = true) }
            .take(2)
            .map { row ->
                HomeDashboardLeague(
                    id = row.id,
                    name = row.nome.ifBlank { "Liga" },
                    acronym = row.sigla,
                    logoUrl = listOfNotNull(row.logoUrl, row.logo, row.foto)
                        .firstOrNull { it.isNotBlank() },
                    description = row.descricao.orEmpty(),
                    weeklyTip = row.bizu.orEmpty(),
                )
            }
    }

    private suspend fun fetchFallbackPosts(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): List<HomeDashboardPost> {
        return client.from(PostsTable)
            .select(columns = Columns.raw(PostColumns)) {
                filter { eq("tenant_id", tenantId) }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = 2)
            }
            .decodeList<PostFallbackRow>()
            .filterNot { it.blocked == true }
            .map { row ->
                HomeDashboardPost(
                    id = row.id,
                    userName = row.userName.ifBlank { "Usuário" },
                    avatarUrl = row.avatar?.trim()?.takeIf(String::isNotBlank),
                    text = row.texto,
                    createdAt = row.createdAt.orEmpty(),
                    likesCount = row.likes.size,
                    viewerHasLiked = userId.isNotBlank() && userId in row.likes,
                )
            }
    }

    private suspend fun fetchFallbackTrainingImages(
        client: SupabaseClient,
        tenantId: String,
    ): List<String> {
        return client.from(TrainingTable)
            .select(columns = Columns.raw("id,imagem,dia,status,createdAt,tenant_id")) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("status", "ativo")
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = 24)
            }
            .decodeList<TrainingFallbackRow>()
            .filter { row -> isDashboardDateCurrent(row.dia, graceDays = 0) }
            .mapNotNull { it.imagem?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
            .take(4)
    }

    private suspend fun countMembers(client: SupabaseClient, tenantId: String): Int {
        return client.from(UsersTable)
            .select(columns = Columns.raw("uid")) {
                head = true
                count(Count.PLANNED)
                filter { eq("tenant_id", tenantId) }
            }
            .countOrNull()
            ?.coerceAtMost(Int.MAX_VALUE.toLong())
            ?.toInt()
            ?: 0
    }

    private suspend fun fetchCapturedFreshmen(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): Int {
        if (userId.isBlank() || userId.startsWith("guest_virtual_")) return 0
        return client.postgrest.rpc(
            function = HuntTotalRpc,
            parameters = buildJsonObject {
                put("p_tenant_id", tenantId)
                put("p_user_id", userId)
            },
        ).decodeAs<Int>().coerceAtLeast(0)
    }

    private companion object {
        const val DefaultWebAppUrl = "https://usc-atleticas.vercel.app"
        const val DashboardRpc = "dashboard_public_home_bundle"
        const val HuntTotalRpc = "dashboard_total_caca_calouros"
        const val UsersTable = "users"
        const val EventsTable = "eventos"
        const val ProductsTable = "produtos"
        const val PartnersTable = "parceiros"
        const val LeaguesTable = "ligas_config"
        const val PostsTable = "posts"
        const val TrainingTable = "treinos"
        const val AppConfigTable = "app_config"
        const val ProfileColumns = "uid,foto,turma,plano,level,tenant_id"
        const val SalesEventColumns = "id,titulo,data,imagem,status,tenant_id,data_extra"
        const val LeagueColumns =
            "id,nome,sigla,foto,logoUrl,logo,descricao,bizu,visivel,likes,category,tenant_id"
        const val PostColumns =
            "id,userId,userName,avatar,createdAt,texto,likes,blocked,tenant_id"
        const val EventFallbackColumns =
            "id,titulo,data,hora,local,imagem,imagePositionY,tipo,status,stats,tenant_id,createdAt"
        const val ProductFallbackColumns =
            "id,nome,preco,img,likes,active,aprovado,tenant_id,createdAt"
        const val PartnerFallbackColumns =
            "id,nome,imgLogo,imgCapa,categoria,tier,status,tenant_id"
        val ClosedEventStatuses = setOf("encerrado", "cancelado", "inativo")
        val LastEffectiveModuleVisibility = ConcurrentHashMap<String, Map<String, Boolean>>()
        val DashboardJson = Json { ignoreUnknownKeys = true }
        val PublicWebClient: HttpClient by lazy {
            HttpClient(Android) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 12_000
                    connectTimeoutMillis = 8_000
                    socketTimeoutMillis = 12_000
                }
            }
        }
    }
}

internal object DashboardModulesMapper {
    fun fromPublicDashboardResponse(payload: JsonObject): Map<String, Boolean>? {
        val modules = payload["modulesConfig"].asObject()
            ?.get("modules").asObject()
            ?: return null
        val parsedModules = linkedMapOf<String, Boolean>()

        TenantAppModulesCatalog.definitions.forEach { definition ->
            val value = modules[definition.key] as? JsonPrimitive ?: return null
            if (value.isString) return null
            parsedModules[definition.key] = value.booleanOrNull ?: return null
        }

        return TenantAppModulesCatalog.normalizeModules(parsedModules)
    }

    fun conservativeFallback(
        tenantModules: Map<String, Boolean>,
        lastEffectiveModules: Map<String, Boolean>?,
    ): Map<String, Boolean> {
        val normalizedTenantModules = TenantAppModulesCatalog.normalizeModules(tenantModules)
        val normalizedEffectiveModules = lastEffectiveModules
            ?.let(TenantAppModulesCatalog::normalizeModules)
            ?: return normalizedTenantModules

        return TenantAppModulesCatalog.definitions.associate { definition ->
            definition.key to (
                normalizedTenantModules[definition.key] != false &&
                    normalizedEffectiveModules[definition.key] != false
                )
        }
    }

    fun fallbackAfterEndpointFailure(
        tenantModules: Map<String, Boolean>?,
        lastEffectiveModules: Map<String, Boolean>?,
    ): Map<String, Boolean> {
        return when {
            tenantModules != null -> conservativeFallback(tenantModules, lastEffectiveModules)
            lastEffectiveModules != null -> TenantAppModulesCatalog.normalizeModules(lastEffectiveModules)
            else -> denyAllKnownModules()
        }
    }

    fun denyAllKnownModules(): Map<String, Boolean> =
        TenantAppModulesCatalog.definitions.associate { definition -> definition.key to false }
}

internal object DashboardBundleMapper {
    fun fromJson(payload: JsonObject): HomeDashboardBundle {
        return HomeDashboardBundle(
            events = payload.objectArray("events").mapNotNull(::toEvent),
            products = payload.objectArray("produtos").mapNotNull(::toProduct),
            partners = payload.objectArray("parceiros").mapNotNull(::toPartner),
            leagues = payload.objectArray("ligas").mapNotNull(::toLeague),
            posts = payload.objectArray("mensagens").mapNotNull(::toPost),
            trainingImageUrls = payload.stringArray("treinos").distinct().take(4),
            capturedFreshmen = payload.intValue("totalCaca").coerceAtLeast(0),
            totalMembers = payload.intValue("totalAlunos").coerceAtLeast(0),
        )
    }

    private fun toEvent(row: JsonObject): HomeDashboardEvent? {
        val id = row.stringValue("id")
        if (id.isBlank()) return null
        return HomeDashboardEvent(
            id = id,
            title = row.stringValue("titulo").ifBlank { "Evento" },
            date = row.stringValue("data"),
            time = row.stringValue("hora"),
            location = row.stringValue("local"),
            imageUrl = row.stringValue("imagem").takeIf(String::isNotBlank),
            type = row.stringValue("tipo"),
            status = row.stringValue("status").ifBlank { "ativo" },
            likesCount = row.intValue("likesCount").coerceAtLeast(0),
            interestedCount = row.intValue("interessadosCount").coerceAtLeast(0),
            viewerHasLiked = row.booleanValue("viewerHasLiked"),
            viewerIsInterested = row.booleanValue("viewerIsInterested"),
            imagePositionY = row.nullableDoubleValue("imagePositionY")?.coerceIn(0.0, 100.0),
        )
    }

    private fun toProduct(row: JsonObject): HomeDashboardProduct? {
        val id = row.stringValue("id")
        if (id.isBlank()) return null
        return HomeDashboardProduct(
            id = id,
            name = row.stringValue("nome").ifBlank { "Produto" },
            price = row.doubleValue("preco").coerceAtLeast(0.0),
            imageUrl = row.stringValue("img").takeIf(String::isNotBlank),
            likesCount = row.intValue("likesCount").coerceAtLeast(0),
            viewerHasLiked = row.booleanValue("viewerHasLiked"),
            topClasses = row.objectArray("topTurmas").mapNotNull { stat ->
                val className = stat.stringValue("turma")
                val count = stat.intValue("count").coerceAtLeast(0)
                if (className.isBlank() || count == 0) null else {
                    HomeDashboardClassStat(className = className, count = count)
                }
            }.take(3),
        )
    }

    private fun toPartner(row: JsonObject): HomeDashboardPartner? {
        val id = row.stringValue("id")
        if (id.isBlank()) return null
        return HomeDashboardPartner(
            id = id,
            name = row.stringValue("nome").ifBlank { "Parceiro" },
            logoUrl = row.stringValue("imgLogo").takeIf(String::isNotBlank),
            coverUrl = row.stringValue("imgCapa").takeIf(String::isNotBlank),
            category = row.stringValue("categoria"),
            tier = row.stringValue("plano")
                .ifBlank { row.stringValue("tier") }
                .ifBlank { row.stringValue("categoria") }
                .ifBlank { "standard" }
                .lowercase(),
        )
    }

    private fun toLeague(row: JsonObject): HomeDashboardLeague? {
        val id = row.stringValue("id")
        if (id.isBlank()) return null
        return HomeDashboardLeague(
            id = id,
            name = row.stringValue("nome").ifBlank { "Liga" },
            acronym = row.stringValue("sigla"),
            logoUrl = listOf(
                row.stringValue("logoUrl"),
                row.stringValue("logo"),
                row.stringValue("foto"),
            ).firstOrNull(String::isNotBlank),
            description = row.stringValue("descricao"),
            weeklyTip = row.stringValue("bizu"),
        )
    }

    private fun toPost(row: JsonObject): HomeDashboardPost? {
        val id = row.stringValue("id")
        if (id.isBlank()) return null
        return HomeDashboardPost(
            id = id,
            userName = row.stringValue("userName").ifBlank { "Usuário" },
            avatarUrl = row.stringValue("avatar").takeIf(String::isNotBlank),
            text = row.stringValue("texto").ifBlank { row.stringValue("text") },
            createdAt = row.stringValue("createdAt"),
            likesCount = row.intValue("likesCount").coerceAtLeast(0),
            viewerHasLiked = row.booleanValue("viewerHasLiked"),
        )
    }
}

@Serializable
private data class HomeEventFallbackRow(
    val id: String = "",
    val titulo: String = "",
    val data: String? = null,
    val hora: String? = null,
    val local: String? = null,
    val imagem: String? = null,
    val imagePositionY: Double? = null,
    val tipo: String? = null,
    val status: String? = null,
    val stats: JsonElement? = null,
)

@Serializable
private data class HomeProductFallbackRow(
    val id: String = "",
    val nome: String = "",
    val preco: Double? = null,
    val img: String? = null,
    val likes: List<String> = emptyList(),
)

@Serializable
private data class HomePartnerFallbackRow(
    val id: String = "",
    val nome: String = "",
    @SerialName("imgLogo") val imgLogo: String? = null,
    @SerialName("imgCapa") val imgCapa: String? = null,
    val categoria: String? = null,
    val tier: String? = null,
)

@Serializable
private data class HomeProfileRow(
    val uid: String = "",
    val foto: String? = null,
    val turma: String? = null,
    val plano: String? = null,
    val level: Int? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
)

@Serializable
private data class SalesEventRow(
    val id: String = "",
    val titulo: String = "",
    val data: String? = null,
    val imagem: String? = null,
    val status: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("data_extra") val dataExtra: JsonElement? = null,
)

@Serializable
private data class HomeAppConfigRow(
    val id: String = "",
    val data: JsonElement? = null,
)

@Serializable
private data class LeagueFallbackRow(
    val id: String = "",
    val nome: String = "",
    val sigla: String = "",
    val foto: String? = null,
    val logoUrl: String? = null,
    val logo: String? = null,
    val descricao: String? = null,
    val bizu: String? = null,
    val category: String? = null,
)

@Serializable
private data class PostFallbackRow(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val avatar: String? = null,
    val createdAt: String? = null,
    val texto: String = "",
    val likes: List<String> = emptyList(),
    val blocked: Boolean? = null,
)

@Serializable
private data class TrainingFallbackRow(
    val id: String = "",
    val imagem: String? = null,
    val dia: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
)

private fun isDashboardDateCurrent(rawDate: String?, graceDays: Long = 1): Boolean {
    val value = rawDate?.trim().orEmpty()
    if (value.isBlank()) return true
    val normalized = value.take(10)
    val parsed = DashboardDateFormats.firstNotNullOfOrNull { formatter ->
        runCatching { LocalDate.parse(normalized, formatter) }.getOrNull()
    } ?: return true
    val earliestVisibleDate = LocalDate.now(DashboardZone).minusDays(graceDays)
    return !parsed.isBefore(earliestVisibleDate)
}

private val DashboardZone = ZoneId.of("America/Sao_Paulo")
private val DashboardDateFormats = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
)

private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

private fun JsonObject.objectArray(key: String): List<JsonObject> =
    (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

private fun JsonObject.stringArray(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
        ?.filter(String::isNotBlank)
        .orEmpty()

private fun JsonObject.stringValue(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

private fun JsonObject.intValue(key: String): Int {
    val value = this[key] as? JsonPrimitive ?: return 0
    return value.intOrNull ?: value.doubleOrNull?.toInt() ?: value.contentOrNull?.toDoubleOrNull()?.toInt() ?: 0
}

private fun JsonObject.doubleValue(key: String): Double {
    val value = this[key] as? JsonPrimitive ?: return 0.0
    return value.doubleOrNull ?: value.contentOrNull?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
}

private fun JsonObject.nullableDoubleValue(key: String): Double? {
    val value = this[key] as? JsonPrimitive ?: return null
    return value.doubleOrNull ?: value.contentOrNull?.replace(',', '.')?.toDoubleOrNull()
}

private fun JsonObject.booleanValue(key: String): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull ?: false
