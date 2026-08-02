package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.repository.CollectiveManagementRepository
import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.LeagueRoleCatalog
import com.example.usc1.ui.collectives.management.CollectiveAdminProduct
import com.example.usc1.ui.collectives.management.CollectiveFinanceUiState
import com.example.usc1.ui.collectives.management.CollectiveFrequencyEvent
import com.example.usc1.ui.collectives.management.CollectiveFrequencyMember
import com.example.usc1.ui.collectives.management.CollectiveFrequencyStatus
import com.example.usc1.ui.collectives.management.CollectiveFrequencyUiState
import com.example.usc1.ui.collectives.management.CollectiveInfoForm
import com.example.usc1.ui.collectives.management.CollectiveLinkDraft
import com.example.usc1.ui.collectives.management.CollectiveLinkType
import com.example.usc1.ui.collectives.management.CollectiveMemberDraft
import com.example.usc1.ui.collectives.management.CollectiveMemberRequestDraft
import com.example.usc1.ui.collectives.management.CollectiveProductForm
import com.example.usc1.ui.collectives.management.CollectiveProductStatus
import com.example.usc1.ui.collectives.management.CollectiveStatementRow
import com.example.usc1.ui.collectives.management.CollectiveStatementStatus
import com.example.usc1.ui.collectives.management.CollectiveStatementType
import com.example.usc1.ui.collectives.management.CollectiveStatementUiState
import com.example.usc1.ui.collectives.management.CollectiveStoreAdminUiState
import com.example.usc1.ui.collectives.management.CollectiveStoreMode
import com.example.usc1.ui.collectives.management.CollectiveStoreOrder
import com.example.usc1.ui.collectives.management.CollectiveUserOption
import com.example.usc1.ui.collectives.management.ManagedCollective
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.Normalizer
import java.text.NumberFormat
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Painel de gestão dos coletivos com Supabase direto.
 *
 * Espelha `web-reference/src/lib/leaguesService.ts` (`fetchManagedLeagueSummaries`,
 * `fetchLeagueById`, `fetchLeagueUsers`, `updateLeagueConfigPatch`, `syncLeagueMembers`),
 * `web-reference/src/lib/storeService.ts` e os componentes
 * `app/ligas/LigasAdminPageContent.tsx`, `app/ligas/LeagueStoreAdminPage.tsx`,
 * `app/ligas/_components/LeagueFinanceDashboard.tsx`, `_components/LeagueFrequencyPage.tsx`
 * e `components/financeiro/FinancialStatementPage.tsx`.
 *
 * Economia: o web carrega a tabela inteira e filtra em memória. Aqui o filtro do vendedor,
 * do status e do evento vai para a consulta, como as demais telas nativas já fazem.
 */
class SupabaseCollectiveManagementRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : CollectiveManagementRepository {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    private val dateTimeFormatter = DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.systemDefault())

    // ------------------------------------------------------------------
    // Gate de acesso
    // ------------------------------------------------------------------

    override suspend fun getManagedCollectives(
        tenantId: String,
        userId: String,
        kind: CollectiveKind,
        isPlatformMaster: Boolean,
    ): List<ManagedCollective> = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) return@withContext emptyList()

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return@withContext emptyList()

        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank() && !isPlatformMaster) return@withContext emptyList()

        // `fetchLeagues({ orderByField: "nome" })` filtrado pela categoria da área.
        val rows = client.from(CollectivesTable)
            .select(columns = Columns.raw(ManagedCollectiveColumns)) {
                filter { eq("tenant_id", cleanTenantId) }
                order(column = "nome", order = Order.ASCENDING)
                limit(count = ManagedCollectivesLimit)
            }
            .decodeList<ManagementCollectiveRow>()
            .filter { resolveKind(it) == kind }

        if (isPlatformMaster) {
            // `isPlatformMaster` do web: todos os registros com o cargo "Master da Plataforma".
            return@withContext rows.map { it.toManaged(kind, "Master da Plataforma") }
                .sortedWith(managedOrder(kind))
        }

        // `ligas_membros` com cargo de gestão; o web ignora a tabela ausente e cai no membro embutido.
        val rolesByCollectiveId = runCatching {
            client.from(CollectiveMembersTable)
                .select(columns = Columns.raw("ligaId,cargo,userId")) {
                    filter {
                        eq("userId", cleanUserId)
                        eq("tenant_id", cleanTenantId)
                    }
                    limit(count = MembershipLimit)
                }
                .decodeList<CollectiveMembershipRow>()
                .mapNotNull { row ->
                    val collectiveId = row.ligaId?.trim().orEmpty()
                    val role = LeagueRoleCatalog.resolveRoleLabel(row.cargo)
                    if (collectiveId.isBlank() || !LeagueRoleCatalog.canManageRole(role)) return@mapNotNull null
                    collectiveId to role
                }
                .toMap()
        }.getOrDefault(emptyMap())

        rows.mapNotNull { row ->
            val embeddedRole = parseMembers(row.membros ?: row.data?.get("membros"))
                .firstOrNull { it.id == cleanUserId && LeagueRoleCatalog.canManageRole(it.role) }
                ?.role
            val hasManagerAccess = row.data.stringList("managerUserIds").contains(cleanUserId)
            val managementRole = rolesByCollectiveId[row.id.trim()]
                ?: embeddedRole
                ?: "Gestor da página".takeIf { hasManagerAccess }
                ?: return@mapNotNull null

            row.toManaged(kind, managementRole)
        }.sortedWith(managedOrder(kind))
    }

    /** `orderedRows` do gate: comissão ordena por turma e depois nome. */
    private fun managedOrder(kind: CollectiveKind): Comparator<ManagedCollective> =
        if (kind == CollectiveKind.Commission) {
            compareBy<ManagedCollective>({ it.turmaId }, { it.name })
        } else {
            compareBy { it.name }
        }

    // ------------------------------------------------------------------
    // Informações
    // ------------------------------------------------------------------

    override suspend fun getInfoForm(
        tenantId: String,
        collectiveId: String,
    ): CollectiveInfoForm = withContext(Dispatchers.IO) {
        val row = fetchCollectiveRow(collectiveId, tenantId) ?: return@withContext CollectiveInfoForm()
        val data = row.data ?: JsonObject(emptyMap())
        val payment = (row.paymentConfig ?: data["paymentConfig"])?.asObjectOrNull()

        CollectiveInfoForm(
            acronym = firstNotBlank(row.sigla, data.string("sigla")),
            name = firstNotBlank(row.nome, data.string("nome")),
            description = firstNotBlank(row.descricao, data.string("descricao")),
            overview = firstNotBlank(row.visaoGeral, data.string("visaoGeral")),
            bizu = firstNotBlank(row.bizu, data.string("bizu")),
            logoUrl = firstNotBlank(row.logoUrl, row.logo, row.foto, data.string("logoUrl")),
            links = parseLinks(data["links"] ?: row.links),
            pixKey = payment.string("chave"),
            pixBank = payment.string("banco"),
            pixHolder = payment.string("titular"),
            whatsapp = payment.string("whatsapp"),
            activeOnBoard = data.bool("ativa") ?: row.ativa ?: false,
        )
    }

    override suspend fun saveInfo(
        tenantId: String,
        collectiveId: String,
        form: CollectiveInfoForm,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanId = collectiveId.trim()
        require(cleanId.isNotBlank()) { "Coletivo inválido." }

        val whatsapp = normalizePhoneToBrE164(form.whatsapp)
        // `handleSaveVisualSection`: valida o WhatsApp antes de gravar.
        require(whatsapp.isBlank() || hasValidPhoneLength(whatsapp)) {
            "Informe um WhatsApp válido para as informações de pagamento."
        }

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val logo = form.logoUrl.trim()
        val links = form.links
            .filter { it.url.isNotBlank() }
            .take(CollectiveInfoForm.LinksMaxCount)

        val paymentConfig = buildJsonObject {
            put("chave", form.pixKey.trim().take(CollectiveInfoForm.PixFieldMaxLength))
            put("banco", form.pixBank.trim().take(CollectiveInfoForm.PixFieldMaxLength))
            put("titular", form.pixHolder.trim().take(CollectiveInfoForm.PixFieldMaxLength))
            put("whatsapp", whatsapp)
        }

        val patch = buildJsonObject {
            put("nome", form.name.trim().take(CollectiveInfoForm.NameMaxLength))
            put("sigla", form.acronym.trim().uppercase(Locale.ROOT).take(CollectiveInfoForm.AcronymMaxLength))
            put("descricao", form.description.trim().take(CollectiveInfoForm.DescriptionMaxLength))
            put("visaoGeral", form.overview.trim().take(CollectiveInfoForm.OverviewMaxLength))
            put("bizu", form.bizu.trim())
            put("links", links.toJsonArray())
            put("paymentConfig", paymentConfig)
            if (logo.isNotBlank()) {
                put("foto", logo)
                put("logoUrl", logo)
                put("logo", logo)
            }
        }

        updateCollectiveConfig(client, cleanId, cleanTenantId, patch)

        // `sendNotification && ligaData.bizu`: notificação global do destaque da semana.
        if (form.sendNotification && form.bizu.isNotBlank()) {
            runCatching {
                client.from(NotificationsTable).insert(
                    buildJsonObject {
                        put("title", "Novo destaque da ${form.acronym.trim()}!")
                        put("message", form.bizu.trim())
                        put("link", "/ligas_usc")
                        put("read", false)
                        put("createdAt", nowIso())
                        put("userId", "GLOBAL")
                    },
                )
            }.getOrElse { throw IllegalStateException("Informações salvas, mas a notificação falhou.") }
        }
    }

    // ------------------------------------------------------------------
    // Membros
    // ------------------------------------------------------------------

    override suspend fun getMembers(
        tenantId: String,
        collectiveId: String,
    ): Pair<List<CollectiveMemberDraft>, List<CollectiveMemberRequestDraft>> = withContext(Dispatchers.IO) {
        val row = fetchCollectiveRow(collectiveId, tenantId)
            ?: return@withContext emptyList<CollectiveMemberDraft>() to emptyList()
        val data = row.data ?: JsonObject(emptyMap())

        val members = parseMembers(row.membros ?: data["membros"])
            .sortedWith(memberOrder())
        val requests = parseMemberRequests(data["memberRequests"])
        members to requests
    }

    override suspend fun getUserOptions(tenantId: String): List<CollectiveUserOption> = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) return@withContext emptyList()

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return@withContext emptyList()

        // `fetchLeagueUsers({ maxResults: 120 })`.
        client.from(UsersTable)
            .select(columns = Columns.raw("uid,nome,turma,foto")) {
                filter { eq("tenant_id", cleanTenantId) }
                limit(count = UserOptionsLimit)
            }
            .decodeList<ManagementUserRow>()
            .mapNotNull { user ->
                val id = user.uid?.trim().orEmpty()
                if (id.isBlank()) return@mapNotNull null
                CollectiveUserOption(
                    id = id,
                    name = user.nome?.trim().orEmpty().ifBlank { "Sem nome" },
                    turma = user.turma?.trim().orEmpty(),
                    photoUrl = resolveRemoteImageUrl(user.foto),
                )
            }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    override suspend fun saveMembers(
        tenantId: String,
        collectiveId: String,
        members: List<CollectiveMemberDraft>,
        requests: List<CollectiveMemberRequestDraft>,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanId = collectiveId.trim()
        require(cleanId.isNotBlank()) { "Coletivo inválido." }

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)

        val ordered = members
            .map { it.copy(role = LeagueRoleCatalog.resolveRoleLabel(it.role)) }
            .sortedWith(memberOrder())
        val memberIds = ordered.map { it.id.trim() }.filter { it.isNotBlank() }.distinct()

        // `persistLeagueConfigPatch`: membros, solicitações e contagem no `ligas_config`.
        updateCollectiveConfig(
            client = client,
            collectiveId = cleanId,
            tenantId = cleanTenantId,
            patch = buildJsonObject {
                put("membros", ordered.toJsonArray())
                put("memberRequests", requests.toJsonArray())
                put("membersCount", memberIds.size)
                put("membrosIds", buildJsonArray { memberIds.forEach { add(JsonPrimitive(it)) } })
            },
        )

        // `syncLeagueMembers`: caminho direto em `ligas_membros` (o fallback do web sem service role).
        syncCollectiveMembers(client, cleanId, cleanTenantId, ordered, memberIds)
    }

    private suspend fun syncCollectiveMembers(
        client: SupabaseClient,
        collectiveId: String,
        tenantId: String,
        members: List<CollectiveMemberDraft>,
        memberIds: List<String>,
    ) {
        val existing = runCatching {
            client.from(CollectiveMembersTable)
                .select(columns = Columns.raw("id,userId,cargo")) {
                    filter {
                        eq("ligaId", collectiveId)
                        if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                    }
                    limit(count = MembershipLimit)
                }
                .decodeList<CollectiveMembershipRow>()
        }.getOrNull() ?: return

        val existingByUserId = existing.mapNotNull { row ->
            val userId = row.userId?.trim().orEmpty()
            if (userId.isBlank()) null else userId to row
        }.toMap()

        val toInsert = members.filter { it.id.trim().isNotBlank() && !existingByUserId.containsKey(it.id.trim()) }
        if (toInsert.isNotEmpty()) {
            client.from(CollectiveMembersTable).insert(
                toInsert.map { member ->
                    buildJsonObject {
                        put("ligaId", collectiveId)
                        put("userId", member.id.trim())
                        put("cargo", LeagueRoleCatalog.resolveRoleLabel(member.role).take(RoleMaxLength))
                        if (tenantId.isNotBlank()) put("tenant_id", tenantId)
                        put("joinedAt", nowIso())
                    }
                },
            )
        }

        members.forEach { member ->
            val userId = member.id.trim()
            val existingRow = existingByUserId[userId] ?: return@forEach
            val nextRole = LeagueRoleCatalog.resolveRoleLabel(member.role).take(RoleMaxLength)
            val currentRole = LeagueRoleCatalog.resolveRoleLabel(existingRow.cargo).take(RoleMaxLength)
            if (nextRole == currentRole) return@forEach

            client.from(CollectiveMembersTable).update(
                buildJsonObject {
                    put("cargo", nextRole)
                    if (tenantId.isNotBlank()) put("tenant_id", tenantId)
                },
            ) {
                filter {
                    eq("ligaId", collectiveId)
                    eq("userId", userId)
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
            }
        }

        val removed = existingByUserId.keys.filterNot { memberIds.contains(it) }
        if (removed.isNotEmpty()) {
            client.from(CollectiveMembersTable).delete {
                filter {
                    eq("ligaId", collectiveId)
                    isIn("userId", removed)
                    if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Loja
    // ------------------------------------------------------------------

    override suspend fun getStore(
        tenantId: String,
        collective: ManagedCollective,
        mode: CollectiveStoreMode,
    ): CollectiveStoreAdminUiState = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val collectiveId = collective.id.trim()
        val base = CollectiveStoreAdminUiState(
            kind = collective.kind,
            mode = mode,
            collective = collective,
            isLoading = false,
        )
        if (cleanTenantId.isBlank() || collectiveId.isBlank()) return@withContext base

        // Uma leitura só de `ligas_config` cobre PIX e WhatsApp da loja.
        val payment = fetchPaymentDefaults(collectiveId, cleanTenantId)

        val category = fetchStoreCategory(client, cleanTenantId, collectiveId)

        // `isLeagueSellerRow`: o web filtra em memória; aqui o vendedor vai na consulta.
        val products = runCatching {
            client.from(ProductsTable)
                .select(columns = Columns.raw(AdminProductColumns)) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("seller_id", collectiveId)
                    }
                    order(column = "nome", order = Order.ASCENDING)
                    limit(count = AdminProductsLimit)
                }
                .decodeList<ManagementProductRow>()
                .filter { isCollectiveSellerType(it.sellerType) }
                .map { it.toAdminProduct() }
        }.getOrDefault(emptyList())

        val orders = if (mode == CollectiveStoreMode.PendingOrders || mode == CollectiveStoreMode.ApprovedOrders) {
            val productIds = products.map { it.id }.filter { it.isNotBlank() }
            if (productIds.isEmpty()) {
                emptyList()
            } else {
                val status = if (mode == CollectiveStoreMode.ApprovedOrders) "approved" else "pendente"
                runCatching {
                    client.from(OrdersTable)
                        .select(columns = Columns.raw(OrderColumns)) {
                            filter {
                                eq("tenant_id", cleanTenantId)
                                eq("status", status)
                                isIn("productId", productIds)
                            }
                            order(column = "createdAt", order = Order.DESCENDING)
                            limit(count = OrdersPageSize)
                        }
                        .decodeList<ManagementOrderRow>()
                        .map { it.toOrder() }
                }.getOrDefault(emptyList())
            }
        } else {
            emptyList()
        }

        base.copy(
            categoryId = category?.id?.trim().orEmpty(),
            categoryVisible = category?.visible != false && category != null,
            storeCoverUrl = category?.coverImg?.trim().orEmpty(),
            storeColor = category?.buttonColor?.trim().orEmpty().ifBlank { DefaultStoreColor },
            products = products,
            orders = orders,
            collectivePixKey = payment.key,
            collectivePixBank = payment.bank,
            collectivePixHolder = payment.holder,
            collectiveWhatsapp = payment.whatsapp,
        )
    }

    override suspend fun saveStoreCategory(
        tenantId: String,
        collective: ManagedCollective,
        coverUrl: String,
        color: String,
        visible: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val collectiveId = collective.id.trim()
        require(collectiveId.isNotBlank() && cleanTenantId.isNotBlank()) { "Coletivo inválido." }

        val categoryName = collective.headerTitle.take(CategoryNameMaxLength)
        val existing = fetchStoreCategory(client, cleanTenantId, collectiveId)

        val payload = buildJsonObject {
            put("nome", categoryName)
            put("cover_img", coverUrl.trim().take(UrlMaxLength))
            put("button_color", color.trim().take(ButtonColorMaxLength))
            put("logo_url", collective.logoUrl?.trim().orEmpty().take(UrlMaxLength))
            // `normalizeStoreSellerTypeForWrite`: coletivo grava sempre `tenant`.
            put("seller_type", "tenant")
            put("seller_id", collectiveId)
            put("visible", visible)
        }

        if (existing != null) {
            client.from(CategoriesTable).update(payload) {
                filter {
                    eq("id", existing.id)
                    eq("tenant_id", cleanTenantId)
                }
            }
            // `renameStoreProductsCategory`: acompanha o nome do coletivo nos produtos.
            val previousName = existing.nome?.trim().orEmpty()
            if (previousName.isNotBlank() && previousName != categoryName) {
                client.from(ProductsTable).update(buildJsonObject { put("categoria", categoryName) }) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("seller_id", collectiveId)
                        eq("categoria", previousName)
                    }
                }
            }
            return@withContext
        }

        client.from(CategoriesTable).insert(
            buildJsonObject {
                payload.forEach { (key, value) -> put(key, value) }
                put("tenant_id", cleanTenantId)
                put("createdAt", nowIso())
            },
        )
    }

    override suspend fun setAllProductsActive(
        tenantId: String,
        collectiveId: String,
        active: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val cleanId = collectiveId.trim()
        require(cleanId.isNotBlank() && cleanTenantId.isNotBlank()) { "Coletivo inválido." }

        client.from(ProductsTable).update(
            buildJsonObject {
                put("active", active)
                put("aprovado", true)
                put("updatedAt", nowIso())
            },
        ) {
            filter {
                eq("tenant_id", cleanTenantId)
                eq("seller_id", cleanId)
            }
        }
    }

    override suspend fun saveProduct(
        tenantId: String,
        collective: ManagedCollective,
        form: CollectiveProductForm,
    ): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val collectiveId = collective.id.trim()
        require(collectiveId.isNotBlank() && cleanTenantId.isNotBlank()) { "Coletivo inválido." }

        val name = form.name.trim().take(CollectiveProductForm.NameMaxLength)
        require(name.isNotBlank()) { "Nome do produto obrigatório." }

        val price = parseMoney(form.price)
        require(price >= 0.0) { "Preço inválido." }

        val payment = fetchPaymentDefaults(collectiveId, cleanTenantId)
        val whatsapp = payment.whatsapp
        require(whatsapp.isNotBlank() && hasValidPhoneLength(whatsapp)) {
            "Configure um WhatsApp válido na seção de informações ${collective.kind.entityArticleForMessage()}."
        }

        if (form.useOwnPayment) {
            require(form.pixKey.isNotBlank() && form.pixBank.isNotBlank() && form.pixHolder.isNotBlank()) {
                "Preencha a chave PIX, o banco e o titular para usar dados próprios."
            }
        } else {
            require(payment.key.isNotBlank() && payment.bank.isNotBlank() && payment.holder.isNotBlank()) {
                "Configure os dados de pagamento na seção de informações ou use dados próprios."
            }
        }

        val paymentConfig = buildJsonObject {
            put("chave", if (form.useOwnPayment) form.pixKey.trim() else payment.key)
            put("banco", if (form.useOwnPayment) form.pixBank.trim() else payment.bank)
            put("titular", if (form.useOwnPayment) form.pixHolder.trim() else payment.holder)
            put("whatsapp", whatsapp)
        }

        val categoryName = collective.headerTitle.take(CategoryNameMaxLength)
        val logo = collective.logoUrl?.trim().orEmpty()
        val oldPrice = form.oldPrice.trim().takeIf { it.isNotBlank() }?.let { parseMoney(it) } ?: 0.0

        val payload = buildJsonObject {
            put("nome", name)
            put("categoria", categoryName)
            put("descricao", form.description.trim().take(CollectiveProductForm.DescriptionMaxLength))
            put("img", form.imageUrl.trim().ifBlank { logo })
            put("preco", price)
            put("estoque", parseIntSafe(form.stock))
            put("lote", form.lot.trim().take(CollectiveProductForm.LotMaxLength).ifBlank { "geral" })
            put("status", form.status.remoteValue)
            put("active", true)
            put("aprovado", true)
            put("payment_config", paymentConfig)
            // `normalizeStoreSellerTypeForWrite`: coletivo grava sempre `tenant`.
            put("seller_type", "tenant")
            put("seller_id", collectiveId)
            put("seller_name", categoryName)
            put("seller_logo_url", logo)
            put("precoAntigo", if (oldPrice > price) oldPrice else 0.0)
            put("tagLabel", form.tagLabel.trim().take(CollectiveProductForm.BadgeMaxLength))
            put("updatedAt", nowIso())
        }

        // `ensureCategory(true)` do web: a categoria precisa existir antes do produto,
        // preservando a capa e a cor já publicadas.
        val existingCategory = fetchStoreCategory(client, cleanTenantId, collectiveId)
        saveStoreCategory(
            tenantId = cleanTenantId,
            collective = collective,
            coverUrl = existingCategory?.coverImg?.trim().orEmpty(),
            color = existingCategory?.buttonColor?.trim().orEmpty().ifBlank { DefaultStoreColor },
            visible = true,
        )

        if (form.isEditing) {
            client.from(ProductsTable).update(payload) {
                filter {
                    eq("id", form.productId.trim())
                    eq("tenant_id", cleanTenantId)
                    eq("seller_id", collectiveId)
                }
            }
            return@withContext
        }

        client.from(ProductsTable).insert(
            buildJsonObject {
                payload.forEach { (key, value) -> put(key, value) }
                put("tenant_id", cleanTenantId)
                put("createdAt", nowIso())
            },
        )
    }

    override suspend fun setProductActive(
        tenantId: String,
        collectiveId: String,
        productId: String,
        active: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val cleanCollectiveId = collectiveId.trim()
        val cleanProductId = productId.trim()
        require(cleanProductId.isNotBlank() && cleanCollectiveId.isNotBlank()) { "Produto inválido." }

        client.from(ProductsTable).update(
            buildJsonObject {
                put("active", active)
                put("aprovado", true)
                put("updatedAt", nowIso())
            },
        ) {
            filter {
                eq("id", cleanProductId)
                eq("tenant_id", cleanTenantId)
                eq("seller_id", cleanCollectiveId)
            }
        }
    }

    override suspend fun approveOrder(
        tenantId: String,
        collectiveId: String,
        orderId: String,
        approvedBy: String,
    ): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val cleanOrderId = orderId.trim()
        require(cleanOrderId.isNotBlank()) { "Pedido inválido." }

        val order = fetchOrderForMutation(client, cleanTenantId, collectiveId.trim(), cleanOrderId)
        val now = nowIso()

        client.from(OrdersTable).update(
            buildJsonObject {
                put("status", "approved")
                put("approvedBy", approvedBy.trim().ifBlank { "gestao" })
                put("updatedAt", now)
            },
        ) {
            filter {
                eq("id", cleanOrderId)
                eq("tenant_id", cleanTenantId)
            }
        }

        // `approveStoreOrder`: baixa de estoque e contador de vendidos do produto.
        val productId = order.productId?.trim().orEmpty()
        if (productId.isNotBlank()) {
            val product = runCatching {
                client.from(ProductsTable)
                    .select(columns = Columns.raw("id,estoque,vendidos")) {
                        filter {
                            eq("id", productId)
                            eq("tenant_id", cleanTenantId)
                        }
                        limit(count = 1)
                    }
                    .decodeList<ManagementProductStockRow>()
                    .firstOrNull()
            }.getOrNull()

            if (product != null) {
                val quantity = maxOf(1, order.quantidade ?: order.itens ?: 1)
                client.from(ProductsTable).update(
                    buildJsonObject {
                        put("estoque", maxOf(0, (product.estoque ?: 0) - quantity))
                        put("vendidos", maxOf(0, (product.vendidos ?: 0) + quantity))
                        put("updatedAt", now)
                    },
                ) {
                    filter {
                        eq("id", productId)
                        eq("tenant_id", cleanTenantId)
                    }
                }
            }
        }

        // Notificação de aprovação, como o web faz ao liberar o pedido.
        runCatching {
            client.from(NotificationsTable).insert(
                buildJsonObject {
                    put("title", "Pedido aprovado")
                    put("message", "${order.productName?.trim().orEmpty().ifBlank { "Seu produto" }} foi liberado para retirada.")
                    put("link", "/pedidos")
                    put("read", false)
                    put("createdAt", now)
                    put("userId", order.userId?.trim().orEmpty())
                },
            )
        }
        Unit
    }

    override suspend fun setOrderStatus(
        tenantId: String,
        collectiveId: String,
        orderId: String,
        status: String,
    ): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val cleanOrderId = orderId.trim()
        require(cleanOrderId.isNotBlank()) { "Pedido inválido." }

        fetchOrderForMutation(client, cleanTenantId, collectiveId.trim(), cleanOrderId)

        client.from(OrdersTable).update(
            buildJsonObject {
                put("status", status.trim())
                put("updatedAt", nowIso())
            },
        ) {
            filter {
                eq("id", cleanOrderId)
                eq("tenant_id", cleanTenantId)
            }
        }
    }

    /** Guarda: o pedido precisa ser de um produto do coletivo antes de qualquer escrita. */
    private suspend fun fetchOrderForMutation(
        client: SupabaseClient,
        tenantId: String,
        collectiveId: String,
        orderId: String,
    ): ManagementOrderRow {
        val order = client.from(OrdersTable)
            .select(columns = Columns.raw(OrderColumns)) {
                filter {
                    eq("id", orderId)
                    eq("tenant_id", tenantId)
                }
                limit(count = 1)
            }
            .decodeList<ManagementOrderRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Pedido não encontrado nesta atlética.")

        val productId = order.productId?.trim().orEmpty()
        if (productId.isBlank()) throw IllegalStateException("Pedido sem produto vinculado.")

        val belongs = runCatching {
            client.from(ProductsTable)
                .select(columns = Columns.raw("id,seller_type,seller_id")) {
                    filter {
                        eq("id", productId)
                        eq("tenant_id", tenantId)
                        eq("seller_id", collectiveId)
                    }
                    limit(count = 1)
                }
                .decodeList<ManagementProductSellerRow>()
                .firstOrNull { isCollectiveSellerType(it.sellerType) }
        }.getOrNull()

        return belongs?.let { order }
            ?: throw IllegalStateException("Este pedido não é de um produto deste coletivo.")
    }

    // ------------------------------------------------------------------
    // Gestão / BI
    // ------------------------------------------------------------------

    /**
     * `view="hub"` do `LeagueFinanceDashboard`: só os quatro cartões de topo (860-895).
     *
     * O BI de produtos que o M7 devolvia aqui em versão reduzida saiu no M8.3 — virou o
     * `ProductBiEngine`, alimentado pelo `SupabaseProductBiRepository`, que atende os cinco
     * players com a mesma consulta escopada. Com ele saíram as três séries `*SalesBy*`: no web
     * elas só aparecem dentro do bloco `{false ? ... : null}` (774-1001).
     */
    override suspend fun getFinance(
        tenantId: String,
        collective: ManagedCollective,
    ): CollectiveFinanceUiState = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val collectiveId = collective.id.trim()
        val base = CollectiveFinanceUiState(
            kind = collective.kind,
            collective = collective,
            isLoading = false,
        )
        if (cleanTenantId.isBlank() || collectiveId.isBlank()) return@withContext base

        val products = fetchCollectiveProducts(client, cleanTenantId, collectiveId)
        val productIds = products.mapNotNull { it.id.trim().takeIf(String::isNotBlank) }

        // `approvedProductOrders`: o web baixa tudo e filtra; aqui o status vai na consulta.
        val orders = if (productIds.isEmpty()) {
            emptyList()
        } else {
            runCatching {
                client.from(OrdersTable)
                    .select(columns = Columns.raw(OrderColumns)) {
                        filter {
                            eq("tenant_id", cleanTenantId)
                            isIn("productId", productIds)
                            isIn("status", ApprovedStatuses)
                        }
                        order(column = "createdAt", order = Order.DESCENDING)
                        limit(count = FinanceOrdersLimit)
                    }
                    .decodeList<ManagementOrderRow>()
            }.getOrDefault(emptyList())
        }

        val eventKeys = fetchCollectiveEventKeys(collectiveId, cleanTenantId)
        val tickets = fetchApprovedTickets(client, cleanTenantId, eventKeys)

        var productRevenue = 0.0
        var productQuantity = 0
        orders.forEach { order ->
            val quantity = maxOf(1, order.quantidade ?: order.itens ?: 1)
            productRevenue += order.total ?: ((order.price ?: 0.0) * quantity)
            productQuantity += quantity
        }

        var eventRevenue = 0.0
        var eventQuantity = 0
        tickets.forEach { ticket ->
            eventRevenue += ticket.valorTotal ?: 0.0
            eventQuantity += maxOf(1, ticket.quantidade ?: 1)
        }

        base.copy(
            productRevenue = productRevenue,
            productQuantity = productQuantity,
            eventRevenue = eventRevenue,
            eventQuantity = eventQuantity,
            catalogCount = products.size,
        )
    }

    // ------------------------------------------------------------------
    // Frequência
    // ------------------------------------------------------------------

    override suspend fun getFrequency(
        tenantId: String,
        collective: ManagedCollective,
        memberScopeTurma: Boolean,
    ): CollectiveFrequencyUiState = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val collectiveId = collective.id.trim()
        val base = CollectiveFrequencyUiState(
            kind = collective.kind,
            collective = collective,
            isLoading = false,
        )
        if (cleanTenantId.isBlank() || collectiveId.isBlank()) return@withContext base

        val row = fetchCollectiveRow(collectiveId, cleanTenantId) ?: return@withContext base
        val data = row.data ?: JsonObject(emptyMap())
        val rawEvents = (row.eventos ?: data["eventos"]) as? JsonArray ?: JsonArray(emptyList())

        val events = rawEvents.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val title = obj.string("titulo")
            if (title.isBlank()) return@mapNotNull null
            val globalId = obj.string("globalEventId").ifBlank { obj.string("id") }
            CollectiveFrequencyEvent(
                key = globalId.ifBlank { title },
                title = title,
                isInternal = resolveInternalVisibility(obj),
            )
        }

        val memberDrafts = parseMembers(row.membros ?: data["membros"])
        val memberIds = memberDrafts.map { it.id }.filter { it.isNotBlank() }

        // `memberScope: "turma"` da comissão: a diretoria da turma vira a lista de presença.
        val turmaFilter = collective.turmaId.trim().takeIf { memberScopeTurma && it.isNotBlank() }
        val userById = fetchUsersByIds(client, cleanTenantId, memberIds, turmaFilter)

        val members = memberDrafts.mapNotNull { draft ->
            val id = draft.id.trim()
            if (id.isBlank()) return@mapNotNull null
            val user = userById[id]
            CollectiveFrequencyMember(
                id = id,
                name = draft.name,
                role = draft.role,
                turma = user?.turma?.trim().orEmpty().ifBlank { "Sem turma" },
                photoUrl = draft.photoUrl ?: resolveRemoteImageUrl(user?.foto),
            )
        }.sortedWith(compareBy({ it.turma }, { it.name }))

        val tickets = fetchApprovedTickets(client, cleanTenantId, events.map { it.key })
        val eventKeyByTicketKey = events.associate { it.key to it.key } +
            events.associate { it.title to it.key }

        val cells = mutableMapOf<String, CollectiveFrequencyStatus>()
        tickets.forEach { ticket ->
            val userId = ticket.userId?.trim().orEmpty()
            if (userId.isBlank()) return@forEach
            val eventKey = eventKeyByTicketKey[ticket.eventoId?.trim().orEmpty()]
                ?: eventKeyByTicketKey[ticket.eventoNome?.trim().orEmpty()]
                ?: return@forEach
            val scanned = ticketScannedCount(ticket.paymentConfig)
            val key = "$eventKey:$userId"
            val next = if (scanned > 0) CollectiveFrequencyStatus.Present else CollectiveFrequencyStatus.Approved
            if (cells[key] != CollectiveFrequencyStatus.Present) cells[key] = next
        }

        // `frequencyManualEntries`: leitura do mesmo campo que a rota admin do web serve.
        val manualEntries = (data["frequencyManualEntries"] as? JsonArray).orEmpty()
        manualEntries.forEach { element ->
            val obj = element.asObjectOrNull() ?: return@forEach
            val eventKey = obj.string("eventKey")
            val userId = obj.string("userId")
            if (eventKey.isBlank() || userId.isBlank()) return@forEach
            val status = when (obj.string("status").lowercase(Locale.ROOT)) {
                "presenca", "presença", "presente" -> CollectiveFrequencyStatus.Present
                "falta", "ausente" -> CollectiveFrequencyStatus.Absent
                "justificada", "justificativa", "justificado" -> CollectiveFrequencyStatus.Justified
                else -> return@forEach
            }
            cells["$eventKey:$userId"] = status
        }

        base.copy(
            events = events,
            members = members,
            cells = cells,
            manualEntryCount = manualEntries.size,
        )
    }

    // ------------------------------------------------------------------
    // Extrato
    // ------------------------------------------------------------------

    override suspend fun getStatement(
        tenantId: String,
        collective: ManagedCollective,
    ): CollectiveStatementUiState = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        val collectiveId = collective.id.trim()
        val base = CollectiveStatementUiState(
            kind = collective.kind,
            collective = collective,
            isLoading = false,
        )
        if (cleanTenantId.isBlank() || collectiveId.isBlank()) return@withContext base

        val products = fetchCollectiveProducts(client, cleanTenantId, collectiveId)
        val productById = products.associateBy { it.id.trim() }
        val productIds = products.mapNotNull { it.id.trim().takeIf(String::isNotBlank) }

        val orders = if (productIds.isEmpty()) {
            emptyList()
        } else {
            runCatching {
                client.from(OrdersTable)
                    .select(columns = Columns.raw(StatementOrderColumns)) {
                        filter {
                            eq("tenant_id", cleanTenantId)
                            isIn("productId", productIds)
                        }
                        order(column = "createdAt", order = Order.DESCENDING)
                        limit(count = StatementRowsLimit)
                    }
                    .decodeList<ManagementOrderRow>()
            }.getOrDefault(emptyList())
        }

        val eventKeys = fetchCollectiveEventKeys(collectiveId, cleanTenantId)
        val tickets = if (eventKeys.isEmpty()) {
            emptyList()
        } else {
            runCatching {
                client.from(TicketsTable)
                    .select(columns = Columns.raw(StatementTicketColumns)) {
                        filter {
                            eq("tenant_id", cleanTenantId)
                            isIn("eventoId", eventKeys)
                        }
                        order(column = "dataSolicitacao", order = Order.DESCENDING)
                        limit(count = StatementRowsLimit)
                    }
                    .decodeList<ManagementTicketRow>()
            }.getOrDefault(emptyList())
        }

        val rows = buildList {
            orders.forEach { order ->
                val product = productById[order.productId?.trim().orEmpty()]
                val quantity = maxOf(1, order.quantidade ?: order.itens ?: 1)
                val orderedAt = order.createdAt
                add(
                    CollectiveStatementRow(
                        id = order.id.trim(),
                        type = CollectiveStatementType.StoreProducts,
                        item = order.productName?.trim().orEmpty()
                            .ifBlank { product?.nome?.trim().orEmpty() }
                            .ifBlank { "Produto" },
                        lot = product?.lote?.trim().orEmpty().ifBlank { "Sem lote" },
                        category = product?.categoria?.trim().orEmpty().ifBlank { "Sem categoria" },
                        quantity = quantity,
                        client = order.userName?.trim().orEmpty().ifBlank { "Usuário" },
                        clientTurma = order.userTurma?.trim().orEmpty(),
                        orderedAtLabel = formatDateTime(orderedAt),
                        approvedBy = order.approvedBy?.trim().orEmpty(),
                        paymentSource = order.paymentSource?.trim().orEmpty()
                            .ifBlank { order.source?.trim().orEmpty() },
                        value = order.total ?: ((order.price ?: 0.0) * quantity),
                        status = order.status?.trim().orEmpty(),
                        statusGroup = statusGroup(order.status),
                        sortAt = parseEpochMillis(orderedAt),
                    ),
                )
            }

            tickets.forEach { ticket ->
                val quantity = maxOf(1, ticket.quantidade ?: 1)
                add(
                    CollectiveStatementRow(
                        id = ticket.id.trim(),
                        type = CollectiveStatementType.Tickets,
                        item = ticket.eventoNome?.trim().orEmpty().ifBlank { "Evento" },
                        lot = ticket.loteNome?.trim().orEmpty().ifBlank { "Sem lote" },
                        category = "Ingresso",
                        quantity = quantity,
                        client = ticket.userName?.trim().orEmpty().ifBlank { "Usuário" },
                        clientTurma = ticket.userTurma?.trim().orEmpty(),
                        orderedAtLabel = formatDateTime(ticket.dataSolicitacao),
                        approvedAtLabel = formatDateTime(ticket.dataAprovacao),
                        approvedBy = ticket.aprovadoPor?.trim().orEmpty(),
                        paymentSource = ticket.paymentSource?.trim().orEmpty()
                            .ifBlank { ticket.metodo?.trim().orEmpty() },
                        value = ticket.valorTotal ?: 0.0,
                        discount = ticket.discountValue ?: 0.0,
                        status = ticket.status?.trim().orEmpty(),
                        statusGroup = statusGroup(ticket.status),
                        sortAt = parseEpochMillis(ticket.dataSolicitacao),
                    ),
                )
            }
        }.sortedByDescending { it.sortAt }

        base.copy(rows = rows)
    }

    // ------------------------------------------------------------------
    // Consultas compartilhadas
    // ------------------------------------------------------------------

    private suspend fun fetchCollectiveRow(
        collectiveId: String,
        tenantId: String,
    ): ManagementCollectiveRow? {
        val cleanId = collectiveId.trim()
        if (cleanId.isBlank() || !SupabaseClientProvider.config.isConfigured) return null

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return null

        return runCatching {
            client.from(CollectivesTable)
                .select(columns = Columns.raw(CollectiveDetailColumns)) {
                    filter {
                        eq("id", cleanId)
                        eq("tenant_id", cleanTenantId)
                    }
                    limit(count = 1)
                }
                .decodeList<ManagementCollectiveRow>()
                .firstOrNull()
        }.getOrNull()
    }

    private suspend fun fetchCollectiveProducts(
        client: SupabaseClient,
        tenantId: String,
        collectiveId: String,
    ): List<ManagementProductRow> = runCatching {
        client.from(ProductsTable)
            .select(columns = Columns.raw(AdminProductColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("seller_id", collectiveId)
                }
                limit(count = AdminProductsLimit)
            }
            .decodeList<ManagementProductRow>()
            .filter { isCollectiveSellerType(it.sellerType) }
    }.getOrDefault(emptyList())

    /** `leagueEventIds` do web: id, `globalEventId` e o id extraído de `linkEvento`. */
    private suspend fun fetchCollectiveEventKeys(collectiveId: String, tenantId: String): List<String> {
        val row = fetchCollectiveRow(collectiveId, tenantId) ?: return emptyList()
        val data = row.data ?: JsonObject(emptyMap())
        val events = (row.eventos ?: data["eventos"]) as? JsonArray ?: return emptyList()

        return events.flatMap { element ->
            val obj = element.asObjectOrNull() ?: return@flatMap emptyList<String>()
            val link = obj.string("linkEvento")
            val fromLink = Regex("/eventos/([^/?#]+)", RegexOption.IGNORE_CASE)
                .find(link)
                ?.groupValues
                ?.getOrNull(1)
                .orEmpty()
            listOf(obj.string("id"), obj.string("globalEventId"), fromLink)
        }.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(EventKeysLimit)
    }

    private suspend fun fetchApprovedTickets(
        client: SupabaseClient,
        tenantId: String,
        eventKeys: List<String>,
    ): List<ManagementTicketRow> {
        val keys = eventKeys.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (keys.isEmpty()) return emptyList()

        return runCatching {
            client.from(TicketsTable)
                .select(columns = Columns.raw(TicketColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                        isIn("eventoId", keys)
                        isIn("status", ApprovedStatuses)
                    }
                    order(column = "dataSolicitacao", order = Order.DESCENDING)
                    limit(count = TicketsLimit)
                }
                .decodeList<ManagementTicketRow>()
        }.getOrDefault(emptyList())
    }

    private suspend fun fetchUsersByIds(
        client: SupabaseClient,
        tenantId: String,
        userIds: List<String>,
        turmaFilter: String?,
    ): Map<String, ManagementUserRow> {
        val ids = userIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return emptyMap()

        return runCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw("uid,nome,turma,foto")) {
                    filter {
                        eq("tenant_id", tenantId)
                        isIn("uid", ids.take(UserLookupLimit))
                    }
                    limit(count = UserLookupLimit.toLong())
                }
                .decodeList<ManagementUserRow>()
                .filter { user ->
                    turmaFilter == null ||
                        user.turma?.trim().orEmpty().equals(turmaFilter, ignoreCase = true)
                }
                .mapNotNull { user -> user.uid?.trim()?.takeIf(String::isNotBlank)?.let { it to user } }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    /** `isLeagueCategoryRow`: casa por seller_id aceitando league, tenant ou vazio. */
    private suspend fun fetchStoreCategory(
        client: SupabaseClient,
        tenantId: String,
        collectiveId: String,
    ): ManagementCategoryRow? = runCatching {
        client.from(CategoriesTable)
            .select(columns = Columns.raw(CategoryColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("seller_id", collectiveId)
                }
                limit(count = CategoryLookupLimit)
            }
            .decodeList<ManagementCategoryRow>()
            .firstOrNull { isCollectiveSellerType(it.sellerType) }
    }.getOrNull()

    private data class CollectivePaymentDefaults(
        val key: String = "",
        val bank: String = "",
        val holder: String = "",
        val whatsapp: String = "",
    )

    /**
     * `resolveLeaguePaymentConfig` do web: PIX e WhatsApp do coletivo, com fallback nos
     * eventos. Uma leitura só de `ligas_config` resolve os quatro campos.
     */
    private suspend fun fetchPaymentDefaults(
        collectiveId: String,
        tenantId: String,
    ): CollectivePaymentDefaults {
        val row = fetchCollectiveRow(collectiveId, tenantId) ?: return CollectivePaymentDefaults()
        val data = row.data ?: JsonObject(emptyMap())
        val payment = (row.paymentConfig ?: data["paymentConfig"])?.asObjectOrNull()

        val direct = CollectivePaymentDefaults(
            key = payment.string("chave"),
            bank = payment.string("banco"),
            holder = payment.string("titular"),
            whatsapp = normalizePhoneToBrE164(payment.string("whatsapp")),
        )

        val events = (row.eventos ?: data["eventos"]) as? JsonArray ?: return direct
        val eventCandidates = events.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val eventPayment = obj["paymentConfig"]?.asObjectOrNull()
            CollectivePaymentDefaults(
                key = eventPayment.string("chave").ifBlank { obj.string("pixChave") },
                bank = eventPayment.string("banco").ifBlank { obj.string("pixBanco") },
                holder = eventPayment.string("titular").ifBlank { obj.string("pixTitular") },
                whatsapp = normalizePhoneToBrE164(
                    eventPayment.string("whatsapp").ifBlank { obj.string("contatoComprovante") },
                ),
            )
        }

        val pixFallback = eventCandidates.firstOrNull {
            it.key.isNotBlank() || it.bank.isNotBlank() || it.holder.isNotBlank()
        }
        val whatsappFallback = eventCandidates.firstOrNull { it.whatsapp.isNotBlank() }

        val hasDirectPix = direct.key.isNotBlank() || direct.bank.isNotBlank() || direct.holder.isNotBlank()
        return CollectivePaymentDefaults(
            key = if (hasDirectPix) direct.key else pixFallback?.key.orEmpty(),
            bank = if (hasDirectPix) direct.bank else pixFallback?.bank.orEmpty(),
            holder = if (hasDirectPix) direct.holder else pixFallback?.holder.orEmpty(),
            whatsapp = direct.whatsapp.ifBlank { whatsappFallback?.whatsapp.orEmpty() },
        )
    }

    /**
     * `updateLeagueConfigRecordCompat`: grava as colunas planas e o mesmo conteúdo
     * dentro de `data`, que é o que as telas públicas leem.
     */
    private suspend fun updateCollectiveConfig(
        client: SupabaseClient,
        collectiveId: String,
        tenantId: String,
        patch: JsonObject,
    ) {
        val current = runCatching {
            client.from(CollectivesTable)
                .select(columns = Columns.raw("id,data")) {
                    filter {
                        eq("id", collectiveId)
                        if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
                    }
                    limit(count = 1)
                }
                .decodeList<ManagementCollectiveDataRow>()
                .firstOrNull()
                ?.data
        }.getOrNull() ?: JsonObject(emptyMap())

        val mergedData = buildJsonObject {
            current.forEach { (key, value) -> put(key, value) }
            patch.forEach { (key, value) -> put(key, value) }
        }

        client.from(CollectivesTable).update(
            buildJsonObject {
                patch.forEach { (key, value) -> put(key, value) }
                put("data", mergedData)
                put("updatedAt", nowIso())
            },
        ) {
            filter {
                eq("id", collectiveId)
                if (tenantId.isNotBlank()) eq("tenant_id", tenantId)
            }
        }
    }

    // ------------------------------------------------------------------
    // Normalização
    // ------------------------------------------------------------------

    private fun ManagementCollectiveRow.toManaged(
        kind: CollectiveKind,
        managementRole: String,
    ): ManagedCollective {
        val dataField = data ?: JsonObject(emptyMap())
        return ManagedCollective(
            id = id.trim(),
            name = firstNotBlank(nome, dataField.string("nome"), "Coletivo"),
            acronym = firstNotBlank(sigla, dataField.string("sigla")),
            turmaId = firstNotBlank(dataField.string("turmaId"), turmaId).uppercase(Locale.ROOT),
            logoUrl = resolveRemoteImageUrl(firstNotBlank(logoUrl, logo, foto)),
            managementRole = managementRole,
            kind = kind,
        )
    }

    /** `normalizeLeagueCategory` do web: o fallback é sempre `liga`. */
    private fun resolveKind(row: ManagementCollectiveRow): CollectiveKind {
        val dataField = row.data ?: JsonObject(emptyMap())
        val raw = firstNotBlank(dataField.string("category"), dataField.string("categoria"))
            .lowercase(Locale.ROOT)
            .stripAccents()
        return when (raw) {
            "comissao", "comissoes" -> CollectiveKind.Commission
            "diretorio" -> CollectiveKind.Directory
            else -> CollectiveKind.League
        }
    }

    private fun parseMembers(source: JsonElement?): List<CollectiveMemberDraft> {
        val entries = (source as? JsonArray)?.toList().orEmpty()
        return entries.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val id = obj.string("id")
            if (id.isBlank()) return@mapNotNull null
            CollectiveMemberDraft(
                id = id,
                name = obj.string("nome").ifBlank { "Sem nome" },
                role = LeagueRoleCatalog.resolveRoleLabel(obj.string("cargo")),
                photoUrl = resolveRemoteImageUrl(obj.string("foto")),
                profileLink = obj.string("linkPerfil"),
                persisted = true,
            )
        }
    }

    private fun parseMemberRequests(source: JsonElement?): List<CollectiveMemberRequestDraft> {
        val entries = (source as? JsonArray)?.toList().orEmpty()
        val seen = mutableSetOf<String>()
        return entries.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val userId = firstNotBlank(obj.string("userId"), obj.string("requesterUserId"))
            if (userId.isBlank() || !seen.add(userId)) return@mapNotNull null
            CollectiveMemberRequestDraft(
                id = obj.string("id").ifBlank { userId },
                userId = userId,
                name = obj.string("nome").ifBlank { "Atleta" },
                photoUrl = resolveRemoteImageUrl(obj.string("foto")),
                turma = obj.string("turma"),
                requestedRole = LeagueRoleCatalog.resolveRoleLabel(obj.string("requestedRole")),
                createdAt = obj.string("createdAt"),
            )
        }
    }

    private fun parseLinks(source: JsonElement?): List<CollectiveLinkDraft> {
        val entries = (source as? JsonArray)?.toList().orEmpty()
        return entries.mapIndexedNotNull { index, element ->
            val obj = element.asObjectOrNull() ?: return@mapIndexedNotNull null
            CollectiveLinkDraft(
                id = obj.string("id").ifBlank { "link-$index" },
                type = CollectiveLinkType.fromRemote(obj.string("type")),
                label = obj.string("label"),
                url = obj.string("url"),
            )
        }.take(CollectiveInfoForm.LinksMaxCount)
    }

    /** `normalizeLeagueEventVisibility` com os apelidos aceitos pelo web. */
    private fun resolveInternalVisibility(obj: JsonObject): Boolean {
        val stats = obj["stats"]?.asObjectOrNull()
        val raw = firstNotBlank(
            obj.string("visibility"),
            obj.string("visibilidade"),
            obj.string("leagueEventVisibility"),
            obj.string("eventVisibility"),
            stats.string("leagueEventVisibility"),
            stats.string("eventVisibility"),
        ).lowercase(Locale.ROOT)
        return raw == "internal" || raw == "interno"
    }

    private fun ManagementProductRow.toAdminProduct(): CollectiveAdminProduct = CollectiveAdminProduct(
        id = id.trim(),
        name = nome?.trim().orEmpty().ifBlank { "Produto sem nome" },
        priceLabel = currencyFormatter.format(preco ?: 0.0),
        price = preco ?: 0.0,
        oldPrice = precoAntigo ?: 0.0,
        stock = maxOf(0, estoque ?: 0),
        lot = lote?.trim().orEmpty(),
        imageUrl = resolveRemoteImageUrl(img),
        description = descricao?.trim().orEmpty(),
        status = CollectiveProductStatus.fromRemote(status),
        active = active != false,
        tagLabel = tagLabel?.trim().orEmpty(),
        soldCount = maxOf(0, vendidos ?: 0),
        category = categoria?.trim().orEmpty(),
    )

    private fun ManagementOrderRow.toOrder(): CollectiveStoreOrder {
        val quantity = maxOf(1, quantidade ?: itens ?: 1)
        val amount = total ?: ((price ?: 0.0) * quantity)
        return CollectiveStoreOrder(
            id = id.trim(),
            productId = productId?.trim().orEmpty(),
            productName = productName?.trim().orEmpty().ifBlank { "Produto" },
            userId = userId?.trim().orEmpty(),
            userName = userName?.trim().orEmpty().ifBlank { "Usuário" },
            quantity = quantity,
            total = amount,
            totalLabel = currencyFormatter.format(amount),
            createdAtLabel = formatDateTime(createdAt),
            status = status?.trim().orEmpty(),
        )
    }

    /** `ticketScannedCount` do web: entradas com status `lido` ou `scannedAt` preenchido. */
    private fun ticketScannedCount(paymentConfig: JsonElement?): Int {
        val config = paymentConfig?.asObjectOrNull() ?: return 0
        val entries = (config["ticketEntries"] ?: config["tickets"] ?: config["ingressos"]) as? JsonArray
            ?: return 0
        return entries.count { element ->
            val obj = element.asObjectOrNull() ?: return@count false
            obj.string("status").lowercase(Locale.ROOT) == "lido" || obj.string("scannedAt").isNotBlank()
        }
    }

    /** `statusIsApproved` + `StatusGroup` do extrato. */
    private fun statusGroup(status: String?): CollectiveStatementStatus {
        val normalized = status?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when {
            normalized in ApprovedStatuses -> CollectiveStatementStatus.Approved
            normalized == "pendente" || normalized == "pending" -> CollectiveStatementStatus.Pending
            normalized == "rejected" || normalized == "recusado" -> CollectiveStatementStatus.Rejected
            normalized == "cancelled" || normalized == "cancelado" -> CollectiveStatementStatus.Cancelled
            else -> CollectiveStatementStatus.Other
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

    private fun memberOrder(): Comparator<CollectiveMemberDraft> =
        compareBy<CollectiveMemberDraft> { LeagueRoleCatalog.roleImportance(it.role) }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { LeagueRoleCatalog.resolveRoleLabel(it.role) }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }

    /** `isLeagueSellerRow`/`isLeagueCategoryRow`: aceita league, tenant e vazio. */
    private fun isCollectiveSellerType(value: String?): Boolean {
        val normalized = value?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return normalized == "league" || normalized == "tenant" || normalized.isBlank()
    }

    private fun formatDateTime(value: String?): String {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return "Não informado"
        return runCatching { dateTimeFormatter.format(OffsetDateTime.parse(raw)) }
            .recoverCatching { dateTimeFormatter.format(Instant.parse(raw)) }
            .getOrDefault("Não informado")
    }

    private fun parseEpochMillis(value: String?): Long {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return 0L
        return runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
            .recoverCatching { Instant.parse(raw).toEpochMilli() }
            .getOrDefault(0L)
    }

    private fun parseMoney(value: String): Double {
        val normalized = value.replace(Regex("[^\\d,.-]"), "").replace(",", ".")
        return normalized.toDoubleOrNull() ?: 0.0
    }

    private fun parseIntSafe(value: String): Int =
        value.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(0) ?: 0

    /** `normalizePhoneToBrE164` de `utils/contactFields.ts`. */
    private fun normalizePhoneToBrE164(value: String): String {
        val digits = value.filter(Char::isDigit)
        if (digits.isBlank()) return ""
        val withCountry = if (digits.startsWith("55")) digits else "55$digits"
        return "+${withCountry.take(13)}"
    }

    /** `hasValidPhoneLength`: DDI + DDD + 8 ou 9 dígitos. */
    private fun hasValidPhoneLength(value: String): Boolean {
        val digits = value.filter(Char::isDigit)
        return digits.length in 12..13
    }

    private fun nowIso(): String = Instant.now().toString()

    private fun firstNotBlank(vararg values: String?): String {
        values.forEach { value ->
            val clean = value?.trim()
            if (!clean.isNullOrBlank()) return clean
        }
        return ""
    }

    private fun String.stripAccents(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    private fun CollectiveKind.entityArticleForMessage(): String = when (this) {
        CollectiveKind.League -> "da liga"
        CollectiveKind.Commission -> "da comissão"
        CollectiveKind.Directory -> "do diretório"
    }

    private companion object {
        const val CollectivesTable = "ligas_config"
        const val CollectiveMembersTable = "ligas_membros"
        const val ProductsTable = "produtos"
        const val CategoriesTable = "categorias"
        const val OrdersTable = "orders"
        const val TicketsTable = "solicitacoes_ingressos"
        const val UsersTable = "users"
        const val NotificationsTable = "notifications"

        const val ManagedCollectivesLimit = 120L
        const val MembershipLimit = 400L
        const val UserOptionsLimit = 120L
        const val UserLookupLimit = 200
        const val CategoryLookupLimit = 5L
        const val AdminProductsLimit = 300L
        const val OrdersPageSize = 50L
        const val FinanceOrdersLimit = 600L
        const val TicketsLimit = 600L
        const val StatementRowsLimit = 400L
        const val EventKeysLimit = 60
        const val RoleMaxLength = 80
        const val CategoryNameMaxLength = 80
        const val UrlMaxLength = 400
        const val ButtonColorMaxLength = 40
        const val DefaultStoreColor = "#10B981"

        /** `statusIsApproved` do web. */
        val ApprovedStatuses = listOf("approved", "aprovado", "aprovada", "delivered", "entregue", "validado")

        const val ManagedCollectiveColumns =
            "id,tenant_id,nome,sigla,foto,logoUrl,logo,membros,turmaId,data"
        const val CollectiveDetailColumns =
            "id,tenant_id,nome,sigla,descricao,foto,logoUrl,logo,visivel,ativa,membros," +
                "membrosIds,eventos,links,payment_config,bizu,likes,turmaId,data"
        const val CategoryColumns =
            "id,tenant_id,nome,cover_img,button_color,logo_url,visible,seller_type,seller_id"
        const val AdminProductColumns =
            "id,tenant_id,nome,descricao,preco,precoAntigo,img,categoria,lote,estoque,status," +
                "active,aprovado,vendidos,tagLabel,seller_type,seller_id"
        const val OrderColumns =
            "id,tenant_id,userId,userName,userTurma,productId,productName,price,total,quantidade," +
                "itens,status,approvedBy,createdAt"
        const val StatementOrderColumns =
            "id,tenant_id,userId,userName,userTurma,productId,productName,price,total,quantidade," +
                "itens,status,approvedBy,paymentSource,source,createdAt"
        const val TicketColumns =
            "id,tenant_id,eventoId,eventoNome,userId,userName,userTurma,status,loteNome," +
                "quantidade,valorTotal,dataSolicitacao,dataAprovacao,aprovadoPor,payment_config"
        const val StatementTicketColumns =
            "id,tenant_id,eventoId,eventoNome,userId,userName,userTurma,status,loteNome," +
                "quantidade,valorTotal,dataSolicitacao,dataAprovacao,aprovadoPor,paymentSource," +
                "metodo,discountValue"
    }
}

// ------------------------------------------------------------------
// Linhas do Supabase
// ------------------------------------------------------------------

@Serializable
private data class ManagementCollectiveRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val nome: String? = null,
    val sigla: String? = null,
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
    val turmaId: String? = null,
    val data: JsonObject? = null,
)

@Serializable
private data class ManagementCollectiveDataRow(
    val id: String = "",
    val data: JsonObject? = null,
)

@Serializable
private data class CollectiveMembershipRow(
    val id: String? = null,
    val ligaId: String? = null,
    val userId: String? = null,
    val cargo: String? = null,
)

@Serializable
private data class ManagementUserRow(
    val uid: String? = null,
    val nome: String? = null,
    val turma: String? = null,
    val foto: String? = null,
)

@Serializable
private data class ManagementCategoryRow(
    val id: String = "",
    val nome: String? = null,
    @SerialName("cover_img") val coverImg: String? = null,
    @SerialName("button_color") val buttonColor: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    val visible: Boolean? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
)

@Serializable
private data class ManagementProductRow(
    val id: String = "",
    val nome: String? = null,
    val descricao: String? = null,
    val preco: Double? = null,
    val precoAntigo: Double? = null,
    val img: String? = null,
    val categoria: String? = null,
    val lote: String? = null,
    val estoque: Int? = null,
    val status: String? = null,
    val active: Boolean? = null,
    val aprovado: Boolean? = null,
    val vendidos: Int? = null,
    val tagLabel: String? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
)

@Serializable
private data class ManagementProductStockRow(
    val id: String = "",
    val estoque: Int? = null,
    val vendidos: Int? = null,
)

@Serializable
private data class ManagementProductSellerRow(
    val id: String = "",
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
)

@Serializable
private data class ManagementOrderRow(
    val id: String = "",
    val userId: String? = null,
    val userName: String? = null,
    val userTurma: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val price: Double? = null,
    val total: Double? = null,
    val quantidade: Int? = null,
    val itens: Int? = null,
    val status: String? = null,
    val approvedBy: String? = null,
    val paymentSource: String? = null,
    val source: String? = null,
    val createdAt: String? = null,
)

@Serializable
private data class ManagementTicketRow(
    val id: String = "",
    val eventoId: String? = null,
    val eventoNome: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val userTurma: String? = null,
    val status: String? = null,
    val loteNome: String? = null,
    val quantidade: Int? = null,
    val valorTotal: Double? = null,
    val dataSolicitacao: String? = null,
    val dataAprovacao: String? = null,
    val aprovadoPor: String? = null,
    val paymentSource: String? = null,
    val metodo: String? = null,
    val discountValue: Double? = null,
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
)

// ------------------------------------------------------------------
// JSON helpers
// ------------------------------------------------------------------

private fun List<CollectiveLinkDraft>.toJsonArray(): JsonArray = buildJsonArray {
    forEach { link ->
        add(
            buildJsonObject {
                put("id", link.id)
                put("type", link.type.remoteValue)
                put("label", link.label.trim().take(CollectiveInfoForm.LinkLabelMaxLength))
                put("url", link.url.trim().take(CollectiveInfoForm.LinkUrlMaxLength))
            },
        )
    }
}

@JvmName("membersToJsonArray")
private fun List<CollectiveMemberDraft>.toJsonArray(): JsonArray = buildJsonArray {
    forEach { member ->
        add(
            buildJsonObject {
                put("id", member.id.trim())
                put("nome", member.name.trim())
                put("cargo", LeagueRoleCatalog.resolveRoleLabel(member.role))
                put("foto", member.photoUrl.orEmpty())
                put("linkPerfil", member.profileLink.ifBlank { "/perfil/${member.id.trim()}" })
            },
        )
    }
}

@JvmName("requestsToJsonArray")
private fun List<CollectiveMemberRequestDraft>.toJsonArray(): JsonArray = buildJsonArray {
    forEach { request ->
        add(
            buildJsonObject {
                put("id", request.id)
                put("userId", request.userId)
                put("nome", request.name)
                put("foto", request.photoUrl.orEmpty())
                put("turma", request.turma)
                put("requestedRole", LeagueRoleCatalog.resolveRoleLabel(request.requestedRole))
                put("createdAt", request.createdAt)
            },
        )
    }
}

private fun JsonArray?.orEmpty(): List<JsonElement> = this?.toList().orEmpty()

private fun JsonObject?.string(key: String): String {
    val primitive = this?.get(key) as? JsonPrimitive ?: return ""
    return primitive.contentOrNull?.trim().orEmpty()
}

private fun JsonObject?.bool(key: String): Boolean? {
    val primitive = this?.get(key) as? JsonPrimitive ?: return null
    return primitive.booleanOrNull ?: primitive.contentOrNull?.toBooleanStrictOrNull()
}

private fun JsonObject?.stringList(key: String): List<String> {
    val array = this?.get(key) as? JsonArray ?: return emptyList()
    return array.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotBlank) }
        .distinct()
}

private fun JsonElement.asObjectOrNull(): JsonObject? {
    if (this is JsonNull) return null
    return runCatching { jsonObject }.getOrNull()
}
