package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.AdminStoreBundle
import com.example.usc1.domain.model.AdminStoreCatalog
import com.example.usc1.domain.model.AdminStoreCategoriesBundle
import com.example.usc1.domain.model.AdminStoreCategory
import com.example.usc1.domain.model.AdminStoreCategoryForm
import com.example.usc1.domain.model.AdminStoreFinanceConfig
import com.example.usc1.domain.model.AdminStoreOrder
import com.example.usc1.domain.model.AdminStoreOrderStatus
import com.example.usc1.domain.model.AdminStoreOrdersMode
import com.example.usc1.domain.model.AdminStoreOrdersPage
import com.example.usc1.domain.model.AdminStoreProduct
import com.example.usc1.domain.model.AdminStoreProductForm
import com.example.usc1.domain.model.AdminStoreProductStatus
import com.example.usc1.domain.model.AdminStoreProductsPage
import com.example.usc1.domain.model.AdminStoreReview
import com.example.usc1.domain.model.AdminStoreReviewStatus
import com.example.usc1.domain.model.AdminStoreSellerType
import com.example.usc1.domain.repository.AdminStoreRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.OffsetDateTime
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class SupabaseAdminStoreRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : AdminStoreRepository {
    override suspend fun getStoreBundle(forceRefresh: Boolean): AdminStoreBundle = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        AdminStoreBundle(
            tenantId = tenantId,
            finance = fetchFinanceConfig(client, tenantId),
        )
    }

    override suspend fun saveFinanceConfig(config: AdminStoreFinanceConfig): Unit = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        client.from(AppConfigTable).upsert(
            FinanceConfigUpsertRow(
                id = buildTenantScopedRowId(tenantId, FinanceDocId),
                tenantId = tenantId,
                chave = config.chave.trim(),
                banco = config.banco.trim(),
                titular = config.titular.trim(),
                whatsapp = config.whatsapp.trim(),
                updatedAt = OffsetDateTime.now().toString(),
            ),
        ) {
            onConflict = "id"
        }
    }

    override suspend fun getCategories(
        tenantLogoUrl: String?,
        defaultButtonColor: String,
        forceRefresh: Boolean,
    ): AdminStoreCategoriesBundle = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val categoryRows = fetchStoreCategoryRows(client, tenantId)
        val products = fetchStoreCategoryProducts(client, tenantId)
        val miniVendors = fetchStoreCategoryMiniVendors(client, tenantId)
        val displayCategories = buildDisplayCategories(
            tenantId = tenantId,
            tenantLogoUrl = tenantLogoUrl.orEmpty(),
            defaultButtonColor = defaultButtonColor.ifBlank { AdminStoreCatalog.CategoryColorDefault },
            categoryRows = categoryRows,
            products = products,
            miniVendors = miniVendors,
        )
        AdminStoreCategoriesBundle(
            tenantId = tenantId,
            categories = displayCategories,
            defaultButtonColor = defaultButtonColor.ifBlank { AdminStoreCatalog.CategoryColorDefault },
        )
    }

    override suspend fun saveCategory(
        form: AdminStoreCategoryForm,
        tenantLogoUrl: String?,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanName = form.nome.trim().take(AdminStoreCatalog.CategoryNameMaxLength)
        if (cleanName.isBlank()) {
            throw IllegalArgumentException("Nome da categoria obrigatório.")
        }
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val now = OffsetDateTime.now().toString()
        val categoryId = form.categoryId?.trim().orEmpty()
        val existingCategoryId = categoryId.ifBlank {
            client.from(CategoriesTable)
                .select(columns = Columns.raw("id")) {
                    filter {
                        eq("tenant_id", tenantId)
                        eq("nome", cleanName)
                        eq("seller_type", AdminStoreSellerType.Tenant.remoteValue)
                    }
                    limit(count = 1)
                }
                .decodeList<CategoryIdRow>()
                .firstOrNull()
                ?.id
                .orEmpty()
        }

        val payload = mapOf(
            "tenant_id" to tenantId,
            "nome" to cleanName,
            "cover_img" to form.coverImg.trim().take(AdminStoreCatalog.CategoryUrlMaxLength).ifBlank { null },
            "button_color" to form.buttonColor.trim().take(40).ifBlank { AdminStoreCatalog.CategoryColorDefault },
            "logo_url" to tenantLogoUrl.orEmpty().trim().take(AdminStoreCatalog.CategoryUrlMaxLength).ifBlank { null },
            "seller_type" to AdminStoreSellerType.Tenant.remoteValue,
            "seller_id" to tenantId,
            "visible" to form.visible,
            "updatedAt" to now,
        )

        if (existingCategoryId.isNotBlank()) {
            client.from(CategoriesTable)
                .update(payload) {
                    filter {
                        eq("id", existingCategoryId)
                        eq("tenant_id", tenantId)
                    }
                }
        } else {
            val nextDisplayOrder = resolveNextStoreCategoryDisplayOrder(client, tenantId)
            client.from(CategoriesTable)
                .insert(
                    payload + mapOf(
                        "display_order" to nextDisplayOrder,
                        "createdAt" to now,
                    ),
                )
        }

        val previousName = form.previousName.trim()
        if (previousName.isNotBlank() && previousName != cleanName) {
            renameTenantProductsCategory(client, tenantId, previousName, cleanName)
        }
    }

    override suspend fun saveCategoryDisplayOrder(categoryIds: List<String>): Unit = withContext(Dispatchers.IO) {
        val cleanCategoryIds = categoryIds.map(String::trim).filter(String::isNotBlank).distinct()
        if (cleanCategoryIds.isEmpty()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        cleanCategoryIds.forEachIndexed { index, categoryId ->
            client.from(CategoriesTable)
                .update(mapOf("display_order" to index)) {
                    filter {
                        eq("id", categoryId)
                        eq("tenant_id", tenantId)
                    }
                }
        }
    }

    override suspend fun setCategoryVisibility(
        categoryId: String,
        visible: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanCategoryId = categoryId.trim()
        if (cleanCategoryId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        client.from(CategoriesTable)
            .update(
                mapOf(
                    "visible" to visible,
                    "updatedAt" to OffsetDateTime.now().toString(),
                ),
            ) {
                filter {
                    eq("id", cleanCategoryId)
                    eq("tenant_id", tenantId)
                }
            }
    }

    override suspend fun setMiniVendorCategoryVisibility(
        miniVendorId: String,
        visible: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanMiniVendorId = miniVendorId.trim()
        if (cleanMiniVendorId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val now = OffsetDateTime.now().toString()
        client.from(MiniVendorsTable)
            .update(
                mapOf(
                    "category_visible" to visible,
                    "updated_at" to now,
                ),
            ) {
                filter {
                    eq("id", cleanMiniVendorId)
                    eq("tenant_id", tenantId)
                }
            }
        client.from(CategoriesTable)
            .update(
                mapOf(
                    "visible" to visible,
                    "updatedAt" to now,
                ),
            ) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("seller_type", AdminStoreSellerType.MiniVendor.remoteValue)
                    eq("seller_id", cleanMiniVendorId)
                }
            }
    }

    override suspend fun getProductsPage(
        categoryLabel: String?,
        inactiveOnly: Boolean,
        forceRefresh: Boolean,
    ): AdminStoreProductsPage = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val categoryNames = fetchTenantCategoryNames(
            client = client,
            tenantId = tenantId,
            products = fetchTenantProductLookup(client, tenantId),
        ).ifEmpty { listOf("Geral") }
        val selectedCategory = categoryLabel?.trim()
            ?.takeIf(String::isNotBlank)
            ?.take(AdminStoreCatalog.ProductCategoryMaxLength)
            ?: categoryNames.firstOrNull()
            ?: "Geral"

        val rows = client.from(ProductsTable)
            .select(columns = Columns.raw(ProductColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    if (inactiveOnly) {
                        eq("active", false)
                    } else {
                        eq("categoria", selectedCategory)
                    }
                }
                order(column = "nome", order = Order.ASCENDING)
                limit(count = MaxProductsPage.toLong())
            }
            .decodeList<AdminStoreProductRow>()
            .mapNotNull { it.toDomain(tenantId) }

        AdminStoreProductsPage(
            tenantId = tenantId,
            products = rows,
            categoryNames = categoryNames,
            selectedCategory = selectedCategory,
            inactiveOnly = inactiveOnly,
        )
    }

    override suspend fun saveProduct(
        form: AdminStoreProductForm,
        tenantName: String?,
        tenantLogoUrl: String?,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanName = form.nome.trim().take(AdminStoreCatalog.ProductNameMaxLength)
        val cleanCategory = form.categoria.trim().ifBlank { "Geral" }.take(AdminStoreCatalog.ProductCategoryMaxLength)
        val price = form.preco.parseMoneyOrNull()
        val oldPrice = form.precoAntigo.parseMoneyOrNull() ?: 0.0

        if (cleanName.isBlank()) {
            throw IllegalArgumentException("Nome do produto obrigatório.")
        }
        if (price == null || price < 0.0) {
            throw IllegalArgumentException("Preço inválido.")
        }

        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val productId = form.productId?.trim().orEmpty()
        val now = OffsetDateTime.now().toString()
        val sellerType = form.sellerType
        val sellerId = form.sellerId.trim().ifBlank {
            if (sellerType == AdminStoreSellerType.Tenant) tenantId else ""
        }
        val sellerName = form.sellerName.trim().ifBlank { tenantName.orEmpty().trim().ifBlank { "Atlética" } }
        val sellerLogoUrl = form.sellerLogoUrl.trim().ifBlank { tenantLogoUrl.orEmpty().trim().ifBlank { "/logo.png" } }
        val productImage = form.img.trim().take(AdminStoreCatalog.ProductImageUrlMaxLength).ifBlank { "/logo.png" }
        val payload = mutableMapOf<String, Any?>(
            "tenant_id" to tenantId,
            "nome" to cleanName,
            "categoria" to cleanCategory,
            "descricao" to form.descricao.trim().take(AdminStoreCatalog.ProductDescriptionMaxLength),
            "img" to productImage,
            "preco" to price,
            "status" to form.status.remoteValue,
            "estoque" to form.estoque.parseIntSafe(),
            "lote" to form.lote.trim().take(AdminStoreCatalog.ProductLotMaxLength).ifBlank { "geral" },
            "cores" to form.coresText.linesToCleanText(AdminStoreCatalog.ProductColorsTextMaxLength),
            "caracteristicas" to form.caracteristicasText.linesToJsonArray(AdminStoreCatalog.ProductFeaturesTextMaxLength),
            "seller_type" to sellerType.remoteValue,
            "seller_id" to sellerId,
            "seller_name" to sellerName,
            "seller_logo_url" to sellerLogoUrl,
            "updatedAt" to now,
        )
        payload["precoAntigo"] = if (oldPrice > price) oldPrice else 0.0
        val cleanTag = form.tagLabel.trim().take(AdminStoreCatalog.ProductBadgeMaxLength)
        if (cleanTag.isNotBlank()) {
            payload["tagLabel"] = cleanTag
            payload["tagColor"] = form.tagColor.trim().ifBlank { "zinc" }
            payload["tagEffect"] = form.tagEffect.trim().ifBlank { "none" }
        } else if (productId.isNotBlank()) {
            payload["tagLabel"] = ""
            payload["tagColor"] = "zinc"
            payload["tagEffect"] = "none"
        }

        if (productId.isNotBlank()) {
            client.from(ProductsTable)
                .update(payload) {
                    filter {
                        eq("id", productId)
                        eq("tenant_id", tenantId)
                    }
                }
        } else {
            client.from(ProductsTable)
                .insert(
                    payload + mapOf(
                        "createdAt" to now,
                        "vendidos" to 0,
                        "cliques" to 0,
                        "likes" to JsonArray(emptyList()),
                        "variantes" to JsonArray(emptyList()),
                        "plan_prices" to JsonArray(emptyList()),
                        "plan_visibility" to JsonArray(emptyList()),
                        "payment_config" to null,
                        "active" to true,
                        "aprovado" to true,
                    ),
                )
        }
    }

    override suspend fun setProductActive(
        productId: String,
        active: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanProductId = productId.trim()
        if (cleanProductId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val product = client.from(ProductsTable)
            .select(columns = Columns.raw("id,categoria")) {
                filter {
                    eq("id", cleanProductId)
                    eq("tenant_id", tenantId)
                }
                limit(count = 1)
            }
            .decodeList<ProductLookupRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Produto fora do tenant ativo.")

        if (active && product.categoria.trim().isBlank()) {
            throw IllegalStateException("Esse produto ficou sem categoria. Edite e escolha uma categoria antes de reativar.")
        }

        client.from(ProductsTable)
            .update(
                mapOf(
                    "active" to active,
                    "updatedAt" to OffsetDateTime.now().toString(),
                ),
            ) {
                filter {
                    eq("id", cleanProductId)
                    eq("tenant_id", tenantId)
                }
            }
    }

    override suspend fun getReviews(
        limit: Int,
        forceRefresh: Boolean,
    ): List<AdminStoreReview> = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val productIds = fetchTenantProductIds(client, tenantId)
        if (productIds.isEmpty()) return@withContext emptyList()

        client.from(ReviewsTable)
            .select(columns = Columns.raw("id,userName,productId,comment,rating,status,createdAt,updatedAt")) {
                filter {
                    isIn("productId", productIds)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = limit.coerceIn(1, MaxReviews).toLong())
            }
            .decodeList<ReviewRow>()
            .mapNotNull(ReviewRow::toDomain)
    }

    override suspend fun setReviewStatus(
        reviewId: String,
        status: AdminStoreReviewStatus,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanReviewId = reviewId.trim()
        if (cleanReviewId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val review = client.from(ReviewsTable)
            .select(columns = Columns.raw("id,productId")) {
                filter {
                    eq("id", cleanReviewId)
                }
                limit(count = 1)
            }
            .decodeList<ReviewProductRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Review não encontrada.")

        val cleanProductId = review.productId.trim()
        if (cleanProductId.isBlank()) {
            throw IllegalStateException("Review sem produto vinculado ao tenant ativo.")
        }
        ensureProductInTenant(client, cleanProductId, tenantId)

        client.from(ReviewsTable)
            .update(
                mapOf(
                    "status" to status.remoteValue,
                    "approved" to (status == AdminStoreReviewStatus.Approved),
                    "updatedAt" to OffsetDateTime.now().toString(),
                ),
            ) {
                filter {
                    eq("id", cleanReviewId)
                }
            }
    }

    override suspend fun getOrdersPage(
        mode: AdminStoreOrdersMode,
        page: Int,
        pageSize: Int,
        categoryLabel: String?,
    ): AdminStoreOrdersPage = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val safePage = page.coerceAtLeast(1)
        val safePageSize = pageSize.coerceIn(1, 50)
        val offset = (safePage - 1) * safePageSize
        val products = fetchTenantProductLookup(client, tenantId)
        val categoryNames = fetchTenantCategoryNames(client, tenantId, products)
        val normalizedCategory = categoryLabel?.trim().orEmpty()
        val categoryProductIds = if (normalizedCategory.isNotBlank()) {
            products.filter { it.categoria == normalizedCategory }.map { it.id }
        } else {
            emptyList()
        }

        if (normalizedCategory.isNotBlank() && categoryProductIds.isEmpty()) {
            return@withContext AdminStoreOrdersPage(
                rows = emptyList(),
                categoryNames = categoryNames,
                hasMore = false,
                page = safePage,
                categoryLabel = normalizedCategory,
                mode = mode,
            )
        }

        val productCategoryById = products.associate { it.id to it.categoria.ifBlank { "Sem categoria" } }
        val rows = client.from(OrdersTable)
            .select(columns = Columns.raw(OrderColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("status", mode.remoteStatus())
                    if (categoryProductIds.isNotEmpty()) {
                        isIn("productId", categoryProductIds)
                    }
                }
                order(column = "createdAt", order = Order.DESCENDING)
                range(offset.toLong()..(offset + safePageSize).toLong())
            }
            .decodeList<OrderRow>()

        val orders = rows.mapNotNull { row ->
            row.toDomain(productCategoryById[row.productId.trim()] ?: "Sem categoria")
        }
        AdminStoreOrdersPage(
            rows = orders.take(safePageSize),
            categoryNames = categoryNames,
            hasMore = orders.size > safePageSize,
            page = safePage,
            categoryLabel = normalizedCategory,
            mode = mode,
        )
    }

    override suspend fun approveOrder(
        orderId: String,
        approvedBy: String,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanOrderId = orderId.trim()
        if (cleanOrderId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val order = fetchOrderForMutation(client, tenantId, cleanOrderId)
        val now = OffsetDateTime.now().toString()
        val actor = approvedBy.trim().ifBlank { "admin" }

        client.from(OrdersTable)
            .update(
                mapOf(
                    "status" to AdminStoreOrderStatus.Approved.remoteValue,
                    "approvedBy" to actor,
                    "updatedAt" to now,
                ),
            ) {
                filter {
                    eq("id", cleanOrderId)
                    eq("tenant_id", tenantId)
                }
            }

        updateProductAfterApproval(client, tenantId, order)
        updateUserAfterApproval(client, tenantId, order)
        insertApprovalNotification(client, order, now)
    }

    override suspend fun setOrderStatus(
        orderId: String,
        status: AdminStoreOrderStatus,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanOrderId = orderId.trim()
        if (cleanOrderId.isBlank()) return@withContext
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        fetchOrderForMutation(client, tenantId, cleanOrderId)

        client.from(OrdersTable)
            .update(
                mapOf(
                    "status" to status.remoteValue,
                    "updatedAt" to OffsetDateTime.now().toString(),
                ),
            ) {
                filter {
                    eq("id", cleanOrderId)
                    eq("tenant_id", tenantId)
                }
            }
    }

    private suspend fun fetchFinanceConfig(
        client: SupabaseClient,
        tenantId: String,
    ): AdminStoreFinanceConfig {
        val row = client.from(AppConfigTable)
            .select(columns = Columns.raw("id,data,chave,banco,titular,whatsapp,updatedAt,createdAt")) {
                filter {
                    eq("id", buildTenantScopedRowId(tenantId, FinanceDocId))
                }
                limit(count = 1)
            }
            .decodeList<FinanceConfigRow>()
            .firstOrNull()

        return AdminStoreFinanceConfig(
            chave = row?.chave?.trim().orEmpty(),
            banco = row?.banco?.trim().orEmpty(),
            titular = row?.titular?.trim().orEmpty(),
            whatsapp = row?.whatsapp?.trim().orEmpty(),
        )
    }

    private suspend fun fetchTenantProductIds(
        client: SupabaseClient,
        tenantId: String,
    ): List<String> {
        return client.from(ProductsTable)
            .select(columns = Columns.raw("id")) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "nome", order = Order.ASCENDING)
                limit(count = MaxProducts.toLong())
            }
            .decodeList<ProductIdRow>()
            .mapNotNull { it.id.trim().takeIf(String::isNotBlank) }
            .distinct()
    }

    private suspend fun fetchTenantProductLookup(
        client: SupabaseClient,
        tenantId: String,
    ): List<ProductLookupRow> {
        return client.from(ProductsTable)
            .select(columns = Columns.raw("id,categoria")) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "nome", order = Order.ASCENDING)
                limit(count = MaxProducts.toLong())
            }
            .decodeList<ProductLookupRow>()
            .filter { it.id.isNotBlank() }
    }

    private suspend fun fetchTenantCategoryNames(
        client: SupabaseClient,
        tenantId: String,
        products: List<ProductLookupRow>,
    ): List<String> {
        val labels = linkedSetOf<String>()
        client.from(CategoriesTable)
            .select(columns = Columns.raw("nome")) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "display_order", order = Order.ASCENDING)
                limit(count = MaxCategories.toLong())
            }
            .decodeList<CategoryNameRow>()
            .forEach { row ->
                row.nome.trim().takeIf(String::isNotBlank)?.let(labels::add)
            }
        products.forEach { product ->
            product.categoria.trim().takeIf(String::isNotBlank)?.let(labels::add)
        }
        val collator = Collator.getInstance(Locale("pt", "BR"))
        return labels.sortedWith { left, right -> collator.compare(left, right) }
    }

    private suspend fun fetchStoreCategoryRows(
        client: SupabaseClient,
        tenantId: String,
    ): List<AdminStoreCategoryRow> {
        return client.from(CategoriesTable)
            .select(columns = Columns.raw(CategoryColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "display_order", order = Order.ASCENDING)
                limit(count = MaxCategories.toLong())
            }
            .decodeList<AdminStoreCategoryRow>()
    }

    private suspend fun fetchStoreCategoryProducts(
        client: SupabaseClient,
        tenantId: String,
    ): List<AdminStoreCategoryProductRow> {
        return client.from(ProductsTable)
            .select(columns = Columns.raw("id,categoria,seller_type,seller_id,seller_logo_url")) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "nome", order = Order.ASCENDING)
                limit(count = MaxProducts.toLong())
            }
            .decodeList<AdminStoreCategoryProductRow>()
    }

    private suspend fun fetchStoreCategoryMiniVendors(
        client: SupabaseClient,
        tenantId: String,
    ): List<AdminStoreCategoryMiniVendorRow> {
        return client.from(MiniVendorsTable)
            .select(columns = Columns.raw("id,store_name,logo_url,cover_url,category_visible,category_button_color")) {
                filter {
                    eq("tenant_id", tenantId)
                }
                limit(count = MaxMiniVendors.toLong())
            }
            .decodeList<AdminStoreCategoryMiniVendorRow>()
    }

    private fun buildDisplayCategories(
        tenantId: String,
        tenantLogoUrl: String,
        defaultButtonColor: String,
        categoryRows: List<AdminStoreCategoryRow>,
        products: List<AdminStoreCategoryProductRow>,
        miniVendors: List<AdminStoreCategoryMiniVendorRow>,
    ): List<AdminStoreCategory> {
        val miniVendorVisibility = miniVendors.associate { it.id.trim() to (it.categoryVisible ?: true) }
        val miniVendorById = miniVendors.associateBy { it.id.trim() }
        val productCounts = mutableMapOf<String, Int>()
        val rows = linkedMapOf<String, AdminStoreCategory>()

        fun sellerIdFor(type: AdminStoreSellerType, rawSellerId: String?): String {
            val cleanSellerId = rawSellerId?.trim().orEmpty()
            return if (type == AdminStoreSellerType.Tenant) cleanSellerId.ifBlank { tenantId } else cleanSellerId
        }

        fun categoryKey(name: String, type: AdminStoreSellerType, sellerId: String): String {
            return "${type.remoteValue}:${sellerId.ifBlank { "_" }}:${name.trim().lowercase()}"
        }

        products.forEach { product ->
            val name = product.categoria.trim()
            if (name.isBlank()) return@forEach
            val sellerType = AdminStoreSellerType.fromRemote(product.sellerType)
            val sellerId = sellerIdFor(sellerType, product.sellerId)
            val key = categoryKey(name, sellerType, sellerId)
            productCounts[key] = (productCounts[key] ?: 0) + 1
        }

        categoryRows.forEach { row ->
            val name = row.nome?.trim().orEmpty()
            if (name.isBlank()) return@forEach
            val sellerType = AdminStoreSellerType.fromRemote(row.sellerType)
            val sellerId = sellerIdFor(sellerType, row.sellerId)
            val key = categoryKey(name, sellerType, sellerId)
            val miniVendor = miniVendorById[sellerId]
            val rowLogo = when (sellerType) {
                AdminStoreSellerType.Tenant -> tenantLogoUrl.ifBlank { row.logoUrl.orEmpty().trim() }
                AdminStoreSellerType.MiniVendor -> row.logoUrl.orEmpty().trim().ifBlank { miniVendor?.logoUrl.orEmpty() }
                AdminStoreSellerType.League -> row.logoUrl.orEmpty().trim()
            }
            val visible = if (sellerType == AdminStoreSellerType.MiniVendor) {
                miniVendorVisibility[sellerId] ?: row.visible ?: true
            } else {
                row.visible ?: true
            }
            rows[key] = AdminStoreCategory(
                key = key,
                categoryId = row.id.trim().ifBlank { null },
                nome = name,
                coverImg = row.coverImg.orEmpty().trim().ifBlank { rowLogo },
                logoUrl = rowLogo,
                buttonColor = row.buttonColor.orEmpty().trim().ifBlank {
                    defaultColorForSeller(sellerType, defaultButtonColor)
                },
                displayOrder = row.displayOrder,
                sellerType = sellerType,
                sellerId = sellerId,
                derivedOnly = false,
                categoryVisible = visible,
                productCount = productCounts[key] ?: 0,
            )
        }

        products.forEach { product ->
            val name = product.categoria.trim()
            if (name.isBlank()) return@forEach
            val sellerType = AdminStoreSellerType.fromRemote(product.sellerType)
            val sellerId = sellerIdFor(sellerType, product.sellerId)
            val key = categoryKey(name, sellerType, sellerId)
            val current = rows[key]
            val miniVendor = miniVendorById[sellerId]
            val productLogo = when (sellerType) {
                AdminStoreSellerType.Tenant -> tenantLogoUrl
                AdminStoreSellerType.MiniVendor -> product.sellerLogoUrl.orEmpty().trim().ifBlank { miniVendor?.logoUrl.orEmpty() }
                AdminStoreSellerType.League -> product.sellerLogoUrl.orEmpty().trim()
            }
            if (current != null) {
                rows[key] = current.copy(
                    coverImg = current.coverImg.ifBlank { current.logoUrl.ifBlank { productLogo } },
                    logoUrl = current.logoUrl.ifBlank { productLogo },
                    productCount = productCounts[key] ?: current.productCount,
                )
            } else {
                rows[key] = AdminStoreCategory(
                    key = key,
                    categoryId = null,
                    nome = name,
                    coverImg = productLogo,
                    logoUrl = productLogo,
                    buttonColor = defaultColorForSeller(sellerType, defaultButtonColor),
                    displayOrder = null,
                    sellerType = sellerType,
                    sellerId = sellerId,
                    derivedOnly = true,
                    categoryVisible = if (sellerType == AdminStoreSellerType.MiniVendor) {
                        miniVendorVisibility[sellerId] ?: true
                    } else {
                        true
                    },
                    productCount = productCounts[key] ?: 0,
                )
            }
        }

        val collator = Collator.getInstance(Locale("pt", "BR"))
        return rows.values.sortedWith { left, right ->
            when {
                left.displayOrder != null && right.displayOrder != null && left.displayOrder != right.displayOrder ->
                    left.displayOrder.compareTo(right.displayOrder)
                left.displayOrder != null && right.displayOrder == null -> -1
                left.displayOrder == null && right.displayOrder != null -> 1
                sellerSortOrder(left.sellerType) != sellerSortOrder(right.sellerType) ->
                    sellerSortOrder(left.sellerType).compareTo(sellerSortOrder(right.sellerType))
                else -> collator.compare(left.nome, right.nome)
            }
        }
    }

    private suspend fun resolveNextStoreCategoryDisplayOrder(
        client: SupabaseClient,
        tenantId: String,
    ): Int {
        val latest = client.from(CategoriesTable)
            .select(columns = Columns.raw("display_order")) {
                filter {
                    eq("tenant_id", tenantId)
                }
                order(column = "display_order", order = Order.DESCENDING)
                limit(count = 1)
            }
            .decodeList<CategoryDisplayOrderRow>()
            .firstOrNull()
            ?.displayOrder
        return (latest ?: -1) + 1
    }

    private suspend fun renameTenantProductsCategory(
        client: SupabaseClient,
        tenantId: String,
        previousName: String,
        nextName: String,
    ) {
        client.from(ProductsTable)
            .update(
                mapOf(
                    "categoria" to nextName,
                    "updatedAt" to OffsetDateTime.now().toString(),
                ),
            ) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("categoria", previousName)
                    eq("seller_type", AdminStoreSellerType.Tenant.remoteValue)
                    eq("seller_id", tenantId)
                }
            }
    }

    private fun defaultColorForSeller(
        sellerType: AdminStoreSellerType,
        tenantDefault: String,
    ): String {
        return when (sellerType) {
            AdminStoreSellerType.Tenant -> tenantDefault.ifBlank { AdminStoreCatalog.CategoryColorDefault }
            AdminStoreSellerType.MiniVendor -> AdminStoreCatalog.MiniVendorColorDefault
            AdminStoreSellerType.League -> AdminStoreCatalog.LeagueColorDefault
        }
    }

    private fun sellerSortOrder(sellerType: AdminStoreSellerType): Int {
        return when (sellerType) {
            AdminStoreSellerType.Tenant -> 0
            AdminStoreSellerType.MiniVendor -> 1
            AdminStoreSellerType.League -> 2
        }
    }

    private suspend fun ensureProductInTenant(
        client: SupabaseClient,
        productId: String,
        tenantId: String,
    ) {
        val exists = client.from(ProductsTable)
            .select(columns = Columns.raw("id")) {
                filter {
                    eq("id", productId)
                    eq("tenant_id", tenantId)
                }
                limit(count = 1)
            }
            .decodeList<ProductIdRow>()
            .isNotEmpty()

        if (!exists) {
            throw IllegalStateException("Produto fora do tenant ativo.")
        }
    }

    private suspend fun fetchOrderForMutation(
        client: SupabaseClient,
        tenantId: String,
        orderId: String,
    ): OrderRow {
        return client.from(OrdersTable)
            .select(columns = Columns.raw(OrderColumns)) {
                filter {
                    eq("id", orderId)
                    eq("tenant_id", tenantId)
                }
                limit(count = 1)
            }
            .decodeList<OrderRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Pedido fora do tenant ativo.")
    }

    private suspend fun updateProductAfterApproval(
        client: SupabaseClient,
        tenantId: String,
        order: OrderRow,
    ) {
        val productId = order.productId.trim()
        if (productId.isBlank()) return
        runCatching {
            val product = client.from(ProductsTable)
                .select(columns = Columns.raw("id,estoque,vendidos")) {
                    filter {
                        eq("id", productId)
                        eq("tenant_id", tenantId)
                    }
                    limit(count = 1)
                }
                .decodeList<ProductInventoryRow>()
                .firstOrNull()
                ?: return
            val quantity = order.quantityForStock()
            client.from(ProductsTable)
                .update(
                    mapOf(
                        "estoque" to maxOf(0, (product.estoque ?: 0) - quantity),
                        "vendidos" to ((product.vendidos ?: 0) + quantity),
                        "updatedAt" to OffsetDateTime.now().toString(),
                    ),
                ) {
                    filter {
                        eq("id", productId)
                        eq("tenant_id", tenantId)
                    }
                }
        }
    }

    private suspend fun updateUserAfterApproval(
        client: SupabaseClient,
        tenantId: String,
        order: OrderRow,
    ) {
        val userId = order.userId.trim()
        if (userId.isBlank()) return
        runCatching {
            val isTenantMember = client.from(TenantMembershipsTable)
                .select(columns = Columns.raw("id")) {
                    filter {
                        eq("tenant_id", tenantId)
                        eq("user_id", userId)
                    }
                    limit(count = 1)
                }
                .decodeList<TenantMembershipIdRow>()
                .isNotEmpty()
            if (!isTenantMember) return

            val user = client.from(UsersTable)
                .select(columns = Columns.raw("uid,xp,selos")) {
                    filter {
                        eq("uid", userId)
                    }
                    limit(count = 1)
                }
                .decodeList<UserRewardRow>()
                .firstOrNull()
                ?: return
            val xpGain = kotlin.math.floor(maxOf(0.0, order.orderTotal()) * 10.0).toLong()
            client.from(UsersTable)
                .update(
                    mapOf(
                        "xp" to ((user.xp ?: 0) + xpGain),
                        "selos" to ((user.selos ?: 0) + 1),
                        "updatedAt" to OffsetDateTime.now().toString(),
                    ),
                ) {
                    filter {
                        eq("uid", userId)
                    }
                }
        }
    }

    private suspend fun insertApprovalNotification(
        client: SupabaseClient,
        order: OrderRow,
        now: String,
    ) {
        val userId = order.userId.trim()
        if (userId.isBlank()) return
        runCatching {
            val xpGain = kotlin.math.floor(maxOf(0.0, order.orderTotal()) * 10.0).toLong()
            client.from(NotificationsTable).insert(
                NotificationInsertRow(
                    userId = userId,
                    title = "Pagamento Aprovado!",
                    message = "Sua compra de ${(order.productName?.ifBlank { "Produto" }) ?: "Produto"} foi confirmada. Você ganhou $xpGain XP!",
                    read = false,
                    type = "order_approved",
                    createdAt = now,
                ),
            )
        }
    }

    private fun buildTenantScopedRowId(tenantId: String, baseId: String): String {
        val cleanTenantId = tenantId.trim()
        val cleanBaseId = baseId.trim()
        if (cleanTenantId.isBlank()) return cleanBaseId
        return "tenant:$cleanTenantId::$cleanBaseId"
    }

    private companion object {
        const val AppConfigTable = "app_config"
        const val ProductsTable = "produtos"
        const val CategoriesTable = "categorias"
        const val OrdersTable = "orders"
        const val ReviewsTable = "reviews"
        const val MiniVendorsTable = "mini_vendors"
        const val UsersTable = "users"
        const val NotificationsTable = "notifications"
        const val TenantMembershipsTable = "tenant_memberships"
        const val FinanceDocId = "financeiro"
        const val MaxProducts = 500
        const val MaxProductsPage = 120
        const val MaxCategories = 300
        const val MaxMiniVendors = 240
        const val MaxReviews = 300
        const val CategoryColumns =
            "id,tenant_id,nome,cover_img,button_color,logo_url,seller_type,seller_id,display_order,visible,createdAt"
        const val ProductColumns =
            "id,tenant_id,nome,descricao,preco,precoAntigo,img,categoria,estoque,lote,tagLabel,tagColor,tagEffect,active,aprovado,status,vendidos,cliques,cores,caracteristicas,variantes,seller_type,seller_id,seller_name,seller_logo_url,createdAt,updatedAt"
        const val OrderColumns =
            "id,tenant_id,userId,userName,productId,productName,price,total,quantidade,itens,data,status,approvedBy,payment_config,createdAt,updatedAt"
    }
}

@Serializable
private data class FinanceConfigRow(
    val id: String = "",
    val chave: String? = null,
    val banco: String? = null,
    val titular: String? = null,
    val whatsapp: String? = null,
)

@Serializable
private data class FinanceConfigUpsertRow(
    val id: String,
    @SerialName("tenant_id") val tenantId: String,
    val chave: String,
    val banco: String,
    val titular: String,
    val whatsapp: String,
    @SerialName("updatedAt") val updatedAt: String,
)

@Serializable
private data class ProductIdRow(
    val id: String = "",
)

@Serializable
private data class ProductLookupRow(
    val id: String = "",
    val categoria: String = "",
)

@Serializable
private data class AdminStoreProductRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val nome: String? = null,
    val descricao: String? = null,
    val preco: Double? = null,
    @SerialName("precoAntigo") val precoAntigo: Double? = null,
    val img: String? = null,
    val categoria: String? = null,
    val estoque: Int? = null,
    val lote: String? = null,
    @SerialName("tagLabel") val tagLabel: String? = null,
    @SerialName("tagColor") val tagColor: String? = null,
    @SerialName("tagEffect") val tagEffect: String? = null,
    val active: Boolean? = null,
    val aprovado: Boolean? = null,
    val status: String? = null,
    val vendidos: Int? = null,
    val cliques: Int? = null,
    val cores: String? = null,
    val caracteristicas: JsonElement? = null,
    val variantes: JsonElement? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
) {
    fun toDomain(activeTenantId: String): AdminStoreProduct? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val seller = AdminStoreSellerType.fromRemote(sellerType)
        return AdminStoreProduct(
            id = cleanId,
            nome = nome?.trim().orEmpty(),
            descricao = descricao?.trim().orEmpty(),
            preco = preco ?: 0.0,
            precoAntigo = precoAntigo ?: 0.0,
            img = img?.trim().orEmpty(),
            categoria = categoria?.trim().orEmpty(),
            estoque = estoque ?: 0,
            lote = lote?.trim().orEmpty(),
            tagLabel = tagLabel?.trim().orEmpty(),
            tagColor = tagColor?.trim().orEmpty(),
            tagEffect = tagEffect?.trim().orEmpty(),
            active = active != false,
            aprovado = aprovado != false,
            status = AdminStoreProductStatus.fromRemote(status),
            vendidos = vendidos ?: 0,
            cliques = cliques ?: 0,
            cores = cores?.trim().orEmpty(),
            caracteristicas = caracteristicas.toStringList(),
            variantCount = variantes.arraySize(),
            sellerType = seller,
            sellerId = sellerId?.trim().orEmpty().ifBlank {
                if (seller == AdminStoreSellerType.Tenant) activeTenantId else ""
            },
            sellerName = sellerName?.trim().orEmpty(),
            sellerLogoUrl = sellerLogoUrl?.trim().orEmpty(),
        )
    }
}

@Serializable
private data class AdminStoreCategoryRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val nome: String? = null,
    @SerialName("cover_img") val coverImg: String? = null,
    @SerialName("button_color") val buttonColor: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("display_order") val displayOrder: Int? = null,
    val visible: Boolean? = null,
)

@Serializable
private data class AdminStoreCategoryProductRow(
    val id: String = "",
    val categoria: String = "",
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
)

@Serializable
private data class AdminStoreCategoryMiniVendorRow(
    val id: String = "",
    @SerialName("store_name") val storeName: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("category_visible") val categoryVisible: Boolean? = null,
    @SerialName("category_button_color") val categoryButtonColor: String? = null,
)

@Serializable
private data class CategoryIdRow(
    val id: String = "",
)

@Serializable
private data class CategoryDisplayOrderRow(
    @SerialName("display_order") val displayOrder: Int? = null,
)

@Serializable
private data class CategoryNameRow(
    val nome: String = "",
)

@Serializable
private data class ProductInventoryRow(
    val id: String = "",
    val estoque: Int? = null,
    val vendidos: Int? = null,
)

@Serializable
private data class TenantMembershipIdRow(
    val id: String = "",
)

@Serializable
private data class UserRewardRow(
    val uid: String = "",
    val xp: Long? = null,
    val selos: Long? = null,
)

@Serializable
private data class NotificationInsertRow(
    @SerialName("userId") val userId: String,
    val title: String,
    val message: String,
    val read: Boolean,
    val type: String,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
private data class ReviewProductRow(
    val id: String = "",
    @SerialName("productId") val productId: String = "",
)

@Serializable
private data class ReviewRow(
    val id: String = "",
    @SerialName("userName") val userName: String? = null,
    @SerialName("productId") val productId: String? = null,
    val comment: String? = null,
    val rating: Int? = null,
    val status: String? = null,
) {
    fun toDomain(): AdminStoreReview? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        return AdminStoreReview(
            id = cleanId,
            userName = userName?.trim().orEmpty(),
            productId = productId?.trim().orEmpty(),
            comment = comment?.trim().orEmpty(),
            rating = (rating ?: 0).coerceIn(0, 5),
            status = AdminStoreReviewStatus.fromRemote(status),
        )
    }
}

@Serializable
private data class OrderRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("userId") val userId: String = "",
    @SerialName("userName") val userName: String? = null,
    @SerialName("productId") val productId: String = "",
    @SerialName("productName") val productName: String? = null,
    val price: Double? = null,
    val total: Double? = null,
    val quantidade: Int? = null,
    val itens: Int? = null,
    val data: JsonElement? = null,
    val status: String? = null,
    @SerialName("approvedBy") val approvedBy: String? = null,
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
) {
    fun toDomain(productCategory: String): AdminStoreOrder? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        return AdminStoreOrder(
            id = cleanId,
            userId = userId.trim(),
            userName = userName?.trim().orEmpty(),
            productId = productId.trim(),
            productName = productName?.trim().orEmpty(),
            productCategory = productCategory,
            price = price ?: 0.0,
            total = total ?: price ?: 0.0,
            quantidade = quantityForStock(),
            itens = itens ?: quantidade ?: 1,
            status = AdminStoreOrderStatus.fromRemote(status),
            approvedBy = approvedBy?.trim().orEmpty(),
            receiverLabel = paymentConfig.receiverLabel(),
            variantLabel = data.variantLabel(),
            createdAt = createdAt?.trim().orEmpty(),
            updatedAt = updatedAt?.trim().orEmpty(),
        )
    }

    fun quantityForStock(): Int {
        return maxOf(1, quantidade ?: itens ?: 1)
    }

    fun orderTotal(): Double {
        return total ?: price ?: 0.0
    }
}

private fun AdminStoreOrdersMode.remoteStatus(): String {
    return when (this) {
        AdminStoreOrdersMode.Pending -> "pendente"
        AdminStoreOrdersMode.Approved -> "approved"
    }
}

private fun String.parseMoneyOrNull(): Double? {
    val clean = trim().replace(",", ".")
    if (clean.isBlank()) return null
    return clean.toDoubleOrNull()
}

private fun String.parseIntSafe(): Int {
    val digits = filter(Char::isDigit)
    return digits.toIntOrNull() ?: 0
}

private fun String.linesToCleanText(maxLength: Int): String {
    return lines()
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString("\n")
        .take(maxLength)
}

private fun String.linesToJsonArray(maxLength: Int): JsonArray {
    val remaining = take(maxLength)
    val rows = remaining
        .lines()
        .map(String::trim)
        .filter(String::isNotBlank)
        .map { JsonPrimitive(it.take(120)) }
        .take(24)
    return JsonArray(rows)
}

private fun JsonElement?.toStringList(): List<String> {
    return when (val value = this) {
        is JsonArray -> value.mapNotNull { element ->
            runCatching { element.jsonPrimitive.content.trim() }.getOrNull()?.takeIf(String::isNotBlank)
        }
        is JsonPrimitive -> value.content.lines().map(String::trim).filter(String::isNotBlank)
        else -> emptyList()
    }
}

private fun JsonElement?.arraySize(): Int {
    return (this as? JsonArray)?.size ?: 0
}

private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

private fun JsonObject.stringValue(key: String): String {
    return this[key]?.jsonPrimitive?.content?.trim().orEmpty()
}

private fun JsonElement?.receiverLabel(): String {
    val payment = asObject() ?: return "Não informado"
    val recipient = payment["recipient"].asObject() ?: return "Não informado"
    return listOf(
        recipient.stringValue("name"),
        recipient.stringValue("turma"),
        recipient.stringValue("phone"),
    ).filter(String::isNotBlank).joinToString(" - ").ifBlank { "Não informado" }
}

private fun JsonElement?.variantLabel(): String {
    val data = asObject() ?: return ""
    val explicitLabel = data.stringValue("varianteLabel").ifBlank { data.stringValue("variantLabel") }
    if (explicitLabel.isNotBlank()) return explicitLabel
    val size = data.stringValue("tamanhoSelecionado").ifBlank { data.stringValue("variantSize") }
    val color = data.stringValue("corVariante").ifBlank { data.stringValue("variantColor") }
    return listOf(
        if (size.isNotBlank()) "Tamanho $size" else "",
        if (color.isNotBlank()) "Cor $color" else "",
    ).filter(String::isNotBlank).joinToString(" • ")
}
