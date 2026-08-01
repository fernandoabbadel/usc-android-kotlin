package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.repository.MiniVendorRepository
import com.example.usc1.ui.vendor.MiniVendorOrder
import com.example.usc1.ui.vendor.MiniVendorOrderStatus
import com.example.usc1.ui.vendor.MiniVendorProfileForm
import com.example.usc1.ui.vendor.MiniVendorProduct
import com.example.usc1.ui.vendor.MiniVendorProductForm
import com.example.usc1.ui.vendor.MiniVendorUiState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.NumberFormat
import java.text.Normalizer
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class SupabaseMiniVendorRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : MiniVendorRepository {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    override suspend fun getDashboard(
        tenantId: String,
        userId: String,
    ): MiniVendorUiState = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) {
            return@withContext MiniVendorUiState(
                statusLabel = "Configure o Supabase público para carregar seu Mini Vendor.",
            )
        }

        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            SupabaseTenantResolver.resolveActiveTenantId(client)
        }
        val cleanUserId = userId.trim().ifBlank {
            client.auth.currentSessionOrNull()?.user?.id.orEmpty()
        }

        if (cleanTenantId.isBlank() || cleanUserId.isBlank()) {
            return@withContext MiniVendorUiState(
                statusLabel = "Entre com uma sessão válida para carregar seu Mini Vendor.",
            )
        }

        val profile = client.from(MiniVendorsTable)
            .select(columns = Columns.raw(MiniVendorColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("user_id", cleanUserId)
                }
                limit(count = 1)
            }
            .decodeList<MiniVendorProfileRow>()
            .firstOrNull()

        if (profile == null || profile.id.trim().isBlank()) {
            return@withContext MiniVendorUiState(
                statusLabel = "Você ainda não cadastrou sua lojinha.",
            )
        }

        val sellerId = profile.id.trim()
        val products = client.from(ProductsTable)
            .select(columns = Columns.raw(ProductColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", sellerId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = MaxProducts.toLong())
            }
            .decodeList<MiniVendorProductRow>()
            .map(::mapProduct)

        val orders = client.from(OrdersTable)
            .select(columns = Columns.raw(OrderColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", sellerId)
                }
                order(column = "createdAt", order = Order.DESCENDING)
                limit(count = MaxOrders.toLong())
            }
            .decodeList<MiniVendorOrderRow>()

        // Nomes de quem aprovou: mesma resolução de `fetchCanonicalUserVisuals` no web.
        val approverIds = orders
            .mapNotNull { it.approvedBy?.trim()?.takeIf { id -> id.isNotBlank() && id != "admin" } }
            .distinct()
        val approverNames = if (approverIds.isEmpty()) {
            emptyMap()
        } else {
            runCatching {
                client.from(UsersTable)
                    .select(columns = Columns.raw("uid,nome")) {
                        filter { isIn("uid", approverIds) }
                    }
                    .decodeList<MiniVendorApproverRow>()
                    .associate { it.uid to it.nome.trim() }
            }.getOrElse { emptyMap() }
        }

        val approvedOrders = orders
            .filter { it.status.toOrderStatus() != MiniVendorOrderStatus.Pending }
            .map { row -> mapOrder(row, approverNames) }
        val pendingOrders = orders
            .filter { it.status.toOrderStatus() == MiniVendorOrderStatus.Pending }
            .map { row -> mapOrder(row, approverNames) }

        MiniVendorUiState(
            profileId = sellerId,
            storeName = profile.storeName.orEmpty().trim().ifBlank { "Mini Vendor" },
            profileStatus = profile.status.orEmpty().trim().lowercase(Locale.ROOT),
            statusLabel = profile.status.toProfileStatusLabel(),
            slug = profile.slug.orEmpty().trim(),
            description = profile.description.orEmpty().trim(),
            logoUrl = resolveRemoteImageUrl(profile.logoUrl),
            coverUrl = resolveRemoteImageUrl(profile.coverUrl),
            pixKey = profile.pixKey.orEmpty().trim(),
            pixBank = profile.pixBank.orEmpty().trim(),
            pixHolder = profile.pixHolder.orEmpty().trim(),
            pixWhatsapp = profile.pixWhatsapp.orEmpty().trim(),
            instagram = profile.instagram.orEmpty().trim(),
            instagramEnabled = profile.instagramEnabled == true,
            whatsapp = profile.whatsapp.orEmpty().trim(),
            whatsappEnabled = profile.whatsappEnabled == true,
            profileVisible = profile.profileVisible != false,
            categoryVisible = profile.categoryVisible != false,
            productsVisible = profile.productsVisible != false,
            categoryButtonColor = profile.categoryButtonColor.orEmpty().trim().ifBlank { "#2563EB" },
            approvedAtLabel = formatDate(profile.approvedAt.orEmpty()),
            totalRevenueLabel = formatCurrency(
                orders
                    .filter { it.status.toOrderStatus() == MiniVendorOrderStatus.Approved }
                    .sumOf(MiniVendorOrderRow::amount),
            ),
            pendingAmountLabel = formatCurrency(
                orders
                    .filter { it.status.toOrderStatus() == MiniVendorOrderStatus.Pending }
                    .sumOf(MiniVendorOrderRow::amount),
            ),
            products = products,
            pendingOrders = pendingOrders,
            approvedOrders = approvedOrders,
        )
    }

    override suspend fun saveProfile(
        tenantId: String,
        userId: String,
        form: MiniVendorProfileForm,
    ): MiniVendorUiState = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) {
            throw IllegalStateException("Configure o Supabase público para salvar seu Mini Vendor.")
        }

        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            SupabaseTenantResolver.resolveActiveTenantId(client)
        }
        val cleanUserId = userId.trim().ifBlank {
            client.auth.currentSessionOrNull()?.user?.id.orEmpty()
        }
        val cleanStoreName = form.storeName.trim().take(StoreNameMaxLength)
        if (cleanTenantId.isBlank() || cleanUserId.isBlank()) {
            throw IllegalArgumentException("Entre com uma sessão válida para salvar seu Mini Vendor.")
        }
        if (cleanStoreName.isBlank()) {
            throw IllegalArgumentException("Nome da loja obrigatório.")
        }
        if (form.pixKey.trim().isBlank() || form.pixBank.trim().isBlank() || form.pixHolder.trim().isBlank()) {
            throw IllegalArgumentException("Preencha chave PIX, banco e titular.")
        }

        val existing = fetchProfileRow(client, cleanTenantId, cleanUserId)
        val nextStatus = when (existing?.status.orEmpty().trim().lowercase(Locale.ROOT)) {
            "approved" -> "approved"
            "disabled" -> "disabled"
            else -> "pending"
        }
        val now = OffsetDateTime.now().toString()
        val payload = mapOf(
            "status" to nextStatus,
            "store_name" to cleanStoreName,
            "slug" to cleanStoreName.toMiniVendorSlug(),
            "description" to form.description.trim().take(DescriptionMaxLength),
            "logo_url" to form.logoUrl.trim().take(UrlMaxLength).ifBlank { null },
            "cover_url" to form.coverUrl.trim().take(UrlMaxLength).ifBlank { null },
            "pix_key" to form.pixKey.trim().take(PixKeyMaxLength),
            "pix_bank" to form.pixBank.trim().take(PixBankMaxLength),
            "pix_holder" to form.pixHolder.trim().take(PixHolderMaxLength),
            "pix_whatsapp" to form.pixWhatsapp.trim().take(PhoneMaxLength),
            "instagram" to form.instagram.trim().take(ContactMaxLength),
            "instagram_enabled" to form.instagramEnabled,
            "whatsapp" to form.whatsapp.trim().take(PhoneMaxLength),
            "whatsapp_enabled" to form.whatsappEnabled,
            "profile_visible" to form.profileVisible,
            "category_visible" to form.categoryVisible,
            "products_visible" to form.productsVisible,
            "category_button_color" to form.categoryButtonColor.trim().take(ColorMaxLength).ifBlank { "#2563eb" },
            "approved_by" to if (nextStatus == "approved") existing?.approvedBy?.trim()?.ifBlank { null } else null,
            "approved_at" to if (nextStatus == "approved") existing?.approvedAt?.trim()?.ifBlank { null } else null,
            "updated_at" to now,
        )

        val saved = if (existing?.id?.trim().isNullOrBlank()) {
            client.from(MiniVendorsTable)
                .insert(
                    payload + mapOf(
                        "tenant_id" to cleanTenantId,
                        "user_id" to cleanUserId,
                        "created_at" to now,
                    ),
                ) {
                    select(columns = Columns.raw(MiniVendorColumns))
                }
                .decodeList<MiniVendorProfileRow>()
                .firstOrNull()
        } else {
            client.from(MiniVendorsTable)
                .update(payload) {
                    filter {
                        eq("id", existing.id)
                        eq("tenant_id", cleanTenantId)
                        eq("user_id", cleanUserId)
                    }
                    select(columns = Columns.raw(MiniVendorColumns))
                }
                .decodeList<MiniVendorProfileRow>()
                .firstOrNull()
        } ?: throw IllegalStateException("Não foi possível salvar a loja mini vendor.")

        if (saved.status.orEmpty().trim().lowercase(Locale.ROOT) == "approved") {
            syncMiniVendorStoreCategory(client, saved)
        }

        getDashboard(tenantId = cleanTenantId, userId = cleanUserId).copy(
            actionMessage = if (saved.status.orEmpty().trim().lowercase(Locale.ROOT) == "approved") {
                "Loja mini vendor atualizada."
            } else {
                "Cadastro salvo. A loja segue em aprovação, mas os produtos continuam liberados."
            },
        )
    }

    override suspend fun saveProduct(
        tenantId: String,
        userId: String,
        form: MiniVendorProductForm,
    ): MiniVendorUiState = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) {
            throw IllegalStateException("Configure o Supabase público para salvar produtos do Mini Vendor.")
        }

        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            SupabaseTenantResolver.resolveActiveTenantId(client)
        }
        val cleanUserId = userId.trim().ifBlank {
            client.auth.currentSessionOrNull()?.user?.id.orEmpty()
        }
        if (cleanTenantId.isBlank() || cleanUserId.isBlank()) {
            throw IllegalArgumentException("Entre com uma sessão válida para salvar produtos.")
        }

        val profile = fetchProfileRow(client, cleanTenantId, cleanUserId)
            ?: throw IllegalStateException("Cadastre sua loja mini vendor antes de criar produtos.")
        val sellerId = profile.id.trim()
        if (sellerId.isBlank()) {
            throw IllegalStateException("Cadastro mini vendor sem identificador válido.")
        }

        val cleanName = form.name.trim().take(ProductNameMaxLength)
        val price = form.price.parseMoneyOrNull()
        val oldPrice = form.oldPrice.parseMoneyOrNull() ?: 0.0
        if (cleanName.isBlank()) {
            throw IllegalArgumentException("Nome do produto obrigatório.")
        }
        if (price == null || price < 0.0) {
            throw IllegalArgumentException("Preço inválido.")
        }

        val now = OffsetDateTime.now().toString()
        val productId = form.productId?.trim().orEmpty()
        val storeName = profile.storeName.orEmpty().trim().ifBlank { "Mini Vendor" }
        val productImage = form.imageUrl
            .trim()
            .take(UrlMaxLength)
            .ifBlank { profile.logoUrl.orEmpty().trim().ifBlank { "/logo.png" } }
        val sellerLogo = profile.logoUrl.orEmpty().trim().ifBlank { "/logo.png" }
        val tagLabel = form.tagLabel.trim().take(ProductBadgeMaxLength)

        val payload = mutableMapOf<String, Any?>(
            "tenant_id" to cleanTenantId,
            "nome" to cleanName,
            "categoria" to storeName.take(StoreNameMaxLength),
            "descricao" to form.description.trim().take(ProductDescriptionMaxLength),
            "img" to productImage,
            "preco" to price,
            "precoAntigo" to if (oldPrice > price) oldPrice else 0.0,
            "estoque" to form.stock.parseIntSafe(),
            "lote" to form.lot.trim().take(ProductLotMaxLength).ifBlank { "geral" },
            "status" to form.remoteStatus.trim().take(ProductStatusMaxLength).ifBlank { "ativo" },
            "active" to form.active,
            "aprovado" to true,
            "seller_type" to "mini_vendor",
            "seller_id" to sellerId,
            "seller_name" to storeName,
            "seller_logo_url" to sellerLogo,
            "payment_config" to profile.toProductPaymentConfig(),
            "updatedAt" to now,
        )
        if (tagLabel.isNotBlank()) {
            payload["tagLabel"] = tagLabel
            payload["tagColor"] = form.tagColor.trim().take(ProductTagColorMaxLength).ifBlank { "zinc" }
            payload["tagEffect"] = form.tagEffect.trim().take(ProductTagEffectMaxLength).ifBlank { "none" }
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
                        eq("tenant_id", cleanTenantId)
                        eq("seller_type", "mini_vendor")
                        eq("seller_id", sellerId)
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
                    ),
                )
        }

        getDashboard(tenantId = cleanTenantId, userId = cleanUserId).copy(
            actionMessage = if (productId.isBlank()) "Produto criado." else "Produto atualizado.",
        )
    }

    override suspend fun setProductActive(
        tenantId: String,
        userId: String,
        productId: String,
        active: Boolean,
    ): MiniVendorUiState = withContext(Dispatchers.IO) {
        val cleanProductId = productId.trim()
        if (cleanProductId.isBlank()) {
            return@withContext getDashboard(tenantId, userId)
        }
        if (!SupabaseClientProvider.config.isConfigured) {
            throw IllegalStateException("Configure o Supabase público para alterar produtos.")
        }

        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            SupabaseTenantResolver.resolveActiveTenantId(client)
        }
        val cleanUserId = userId.trim().ifBlank {
            client.auth.currentSessionOrNull()?.user?.id.orEmpty()
        }
        val profile = fetchProfileRow(client, cleanTenantId, cleanUserId)
            ?: throw IllegalStateException("Mini Vendor não encontrado para a sessão atual.")
        val sellerId = profile.id.trim()
        if (cleanTenantId.isBlank() || cleanUserId.isBlank() || sellerId.isBlank()) {
            throw IllegalArgumentException("Sessão inválida para alterar produtos.")
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
                    eq("tenant_id", cleanTenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", sellerId)
                }
            }

        getDashboard(tenantId = cleanTenantId, userId = cleanUserId).copy(
            actionMessage = if (active) "Produto reativado." else "Produto desativado.",
        )
    }

    override suspend fun setOrderStatus(
        tenantId: String,
        userId: String,
        orderId: String,
        status: MiniVendorOrderStatus,
        approvedBy: String,
    ): MiniVendorUiState = withContext(Dispatchers.IO) {
        val cleanOrderId = orderId.trim()
        if (cleanOrderId.isBlank()) {
            return@withContext getDashboard(tenantId, userId)
        }
        if (!SupabaseClientProvider.config.isConfigured) {
            throw IllegalStateException("Configure o Supabase público para alterar pedidos.")
        }

        val client = clientProvider()
        val cleanTenantId = tenantId.trim().ifBlank {
            SupabaseTenantResolver.resolveActiveTenantId(client)
        }
        val cleanUserId = userId.trim().ifBlank {
            client.auth.currentSessionOrNull()?.user?.id.orEmpty()
        }
        val profile = fetchProfileRow(client, cleanTenantId, cleanUserId)
            ?: throw IllegalStateException("Mini Vendor não encontrado para a sessão atual.")
        val sellerId = profile.id.trim()
        if (cleanTenantId.isBlank() || cleanUserId.isBlank() || sellerId.isBlank()) {
            throw IllegalArgumentException("Sessão inválida para alterar pedidos.")
        }

        val currentOrder = client.from(OrdersTable)
            .select(columns = Columns.raw(OrderColumns)) {
                filter {
                    eq("id", cleanOrderId)
                    eq("tenant_id", cleanTenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", sellerId)
                }
                limit(count = 1)
            }
            .decodeList<MiniVendorOrderRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Pedido fora do Mini Vendor atual.")

        val wasApproved = currentOrder.status.toOrderStatus() == MiniVendorOrderStatus.Approved
        val now = OffsetDateTime.now().toString()
        val patch = mutableMapOf<String, Any?>(
            "status" to status.remoteValue,
            "updatedAt" to now,
        )
        if (status == MiniVendorOrderStatus.Approved || status == MiniVendorOrderStatus.Rejected) {
            patch["approvedBy"] = approvedBy.trim().ifBlank { cleanUserId }
        }

        client.from(OrdersTable)
            .update(patch) {
                filter {
                    eq("id", cleanOrderId)
                    eq("tenant_id", cleanTenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", sellerId)
                }
            }

        if (status == MiniVendorOrderStatus.Approved && !wasApproved) {
            decrementProductStockAfterApproval(
                client = client,
                tenantId = cleanTenantId,
                sellerId = sellerId,
                productId = currentOrder.productId.orEmpty(),
                quantity = currentOrder.quantity(),
                now = now,
            )
        }

        getDashboard(tenantId = cleanTenantId, userId = cleanUserId).copy(
            actionMessage = when (status) {
                MiniVendorOrderStatus.Approved -> "Pedido aprovado."
                MiniVendorOrderStatus.Rejected -> "Pedido rejeitado."
                MiniVendorOrderStatus.Delivered -> "Pedido marcado como entregue."
                MiniVendorOrderStatus.Pending -> "Pedido voltou para pendente."
            },
        )
    }

    private fun mapProduct(row: MiniVendorProductRow): MiniVendorProduct {
        val stock = row.estoque ?: 0
        return MiniVendorProduct(
            id = row.id.trim(),
            name = row.nome.orEmpty().trim().ifBlank { "Produto" },
            priceLabel = formatCurrency(row.preco ?: 0.0),
            oldPriceLabel = row.precoAntigo?.takeIf { it > 0.0 }?.let(::formatCurrency),
            category = row.categoria.orEmpty().trim(),
            description = row.descricao.orEmpty().trim(),
            stockCount = stock,
            soldCount = row.vendidos ?: 0,
            clicksCount = row.cliques ?: 0,
            stockLabel = when {
                stock <= 0 -> "Sem estoque"
                stock == 1 -> "1 disponível"
                else -> "$stock disponíveis"
            },
            status = productStatusLabel(row),
            soldLabel = when (row.vendidos ?: 0) {
                0 -> ""
                1 -> "1 vendido"
                else -> "${row.vendidos} vendidos"
            },
            clicksLabel = when (row.cliques ?: 0) {
                0 -> ""
                1 -> "1 clique"
                else -> "${row.cliques} cliques"
            },
            tagLabel = row.tagLabel.orEmpty().trim(),
            tagColor = row.tagColor.orEmpty().trim().ifBlank { "zinc" },
            tagEffect = row.tagEffect.orEmpty().trim().ifBlank { "none" },
            lot = row.lote.orEmpty().trim(),
            imageUrl = resolveRemoteImageUrl(row.img),
            remoteStatus = row.status.orEmpty().trim().ifBlank { "ativo" },
            active = row.active != false,
        )
    }

    private fun mapOrder(
        row: MiniVendorOrderRow,
        approverNames: Map<String, String> = emptyMap(),
    ): MiniVendorOrder {
        val approvedBy = row.approvedBy.orEmpty().trim()
        return MiniVendorOrder(
            id = row.id.trim().ifBlank { "Pedido" },
            productId = row.productId.orEmpty().trim(),
            userId = row.userId.orEmpty().trim(),
            customerName = row.userName.orEmpty().trim().ifBlank { "Cliente USC" },
            productName = row.productName.orEmpty().trim().ifBlank { "Produto" },
            amountLabel = formatCurrency(row.amount()),
            createdAtLabel = formatDate(row.createdAt.orEmpty()),
            approvedBy = approvedBy,
            approvedByName = when {
                approvedBy.isBlank() -> ""
                approvedBy == "admin" -> "Admin"
                else -> approverNames[approvedBy].orEmpty().ifBlank { approvedBy.compactUserId() }
            },
            // O web usa `updatedAt || createdAt` como data da aprovação.
            approvedAtLabel = formatDate(row.updatedAt.orEmpty().ifBlank { row.createdAt.orEmpty() }),
            quantity = row.quantity(),
            status = row.status.toOrderStatus(),
        )
    }

    private fun productStatusLabel(row: MiniVendorProductRow): String {
        val cleanStatus = row.status.orEmpty().trim().lowercase(Locale.ROOT)
        return when {
            row.active == false -> "Inativo"
            row.aprovado == false -> "Em análise"
            cleanStatus == "em_breve" || cleanStatus == "agendado" -> "Em breve"
            cleanStatus == "esgotado" || cleanStatus == "encerrado" -> "Esgotado"
            (row.vendidos ?: 0) > 0 -> "${row.vendidos} vendidos"
            else -> "Publicado"
        }
    }

    private fun String?.toProfileStatusLabel(): String {
        return when (orEmpty().trim().lowercase(Locale.ROOT)) {
            "approved", "aprovado" -> "Aprovado para vender"
            "rejected", "recusado", "rejeitado" -> "Cadastro recusado"
            "disabled", "desativado" -> "Mini Vendor desativado"
            else -> "Aguardando aprovação"
        }
    }

    private fun String?.toOrderStatus(): MiniVendorOrderStatus {
        return when (orEmpty().trim().lowercase(Locale.ROOT)) {
            "approved",
            "aprovado",
            "confirmado",
            "confirmed",
            "paid",
            "pago" -> MiniVendorOrderStatus.Approved
            "delivered",
            "entregue",
            "retirado" -> MiniVendorOrderStatus.Delivered
            "rejected",
            "recusado",
            "rejeitado",
            "cancelado",
            "canceled" -> MiniVendorOrderStatus.Rejected
            else -> MiniVendorOrderStatus.Pending
        }
    }

    private fun formatCurrency(value: Double): String = currencyFormatter.format(value)

    private fun formatDate(value: String): String {
        val clean = value.trim()
        if (clean.isBlank()) return ""
        val instant = runCatching { OffsetDateTime.parse(clean).toInstant() }
            .getOrElse {
                runCatching { Instant.parse(clean) }.getOrNull()
            } ?: return clean.take(16)
        return MiniVendorDateFormatter.format(instant)
    }

    private suspend fun fetchProfileRow(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): MiniVendorProfileRow? {
        return client.from(MiniVendorsTable)
            .select(columns = Columns.raw(MiniVendorColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("user_id", userId)
                }
                limit(count = 1)
            }
            .decodeList<MiniVendorProfileRow>()
            .firstOrNull()
    }

    private suspend fun syncMiniVendorStoreCategory(
        client: SupabaseClient,
        profile: MiniVendorProfileRow,
    ) {
        val cleanTenantId = profile.tenantId.orEmpty().trim()
        val cleanSellerId = profile.id.trim()
        val cleanName = profile.storeName.orEmpty().trim()
        if (cleanTenantId.isBlank() || cleanSellerId.isBlank() || cleanName.isBlank()) return
        val now = OffsetDateTime.now().toString()
        val existingCategory = client.from(CategoriesTable)
            .select(columns = Columns.raw("id,cover_img,logo_url")) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", cleanSellerId)
                }
                limit(count = 1)
            }
            .decodeList<MiniVendorStoreCategoryRow>()
            .firstOrNull()

        val categoryPayload = mapOf(
            "tenant_id" to cleanTenantId,
            "nome" to cleanName.take(StoreNameMaxLength),
            "cover_img" to profile.coverUrl.orEmpty().trim().ifBlank { existingCategory?.coverImg.orEmpty() }.ifBlank { null },
            "button_color" to profile.categoryButtonColor.orEmpty().trim().ifBlank { "#2563eb" },
            "logo_url" to profile.logoUrl.orEmpty().trim().ifBlank { existingCategory?.logoUrl.orEmpty() }.ifBlank { null },
            "seller_type" to "mini_vendor",
            "seller_id" to cleanSellerId,
            "visible" to (profile.categoryVisible != false),
            "updatedAt" to now,
        )

        if (existingCategory?.id?.isNotBlank() == true) {
            client.from(CategoriesTable)
                .update(categoryPayload) {
                    filter {
                        eq("id", existingCategory.id)
                        eq("tenant_id", cleanTenantId)
                    }
                }
        } else {
            client.from(CategoriesTable)
                .insert(categoryPayload + mapOf("createdAt" to now))
        }

        val productPatch = mutableMapOf<String, Any?>(
            "categoria" to cleanName,
            "seller_name" to cleanName,
            "updatedAt" to now,
        )
        profile.logoUrl.orEmpty().trim().takeIf(String::isNotBlank)?.let {
            productPatch["seller_logo_url"] = it
        }
        client.from(ProductsTable)
            .update(productPatch) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", cleanSellerId)
                }
            }
    }

    private suspend fun decrementProductStockAfterApproval(
        client: SupabaseClient,
        tenantId: String,
        sellerId: String,
        productId: String,
        quantity: Int,
        now: String,
    ) {
        val cleanProductId = productId.trim()
        if (cleanProductId.isBlank()) return
        val product = client.from(ProductsTable)
            .select(columns = Columns.raw("id,estoque,vendidos")) {
                filter {
                    eq("id", cleanProductId)
                    eq("tenant_id", tenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", sellerId)
                }
                limit(count = 1)
            }
            .decodeList<MiniVendorProductStockRow>()
            .firstOrNull()
            ?: return

        val safeQuantity = quantity.coerceAtLeast(1)
        client.from(ProductsTable)
            .update(
                mapOf(
                    "estoque" to (product.estoque ?: 0).minus(safeQuantity).coerceAtLeast(0),
                    "vendidos" to (product.vendidos ?: 0) + safeQuantity,
                    "updatedAt" to now,
                ),
            ) {
                filter {
                    eq("id", cleanProductId)
                    eq("tenant_id", tenantId)
                    eq("seller_type", "mini_vendor")
                    eq("seller_id", sellerId)
                }
            }
    }

    private fun MiniVendorProfileRow.toProductPaymentConfig(): JsonObject? {
        val chave = pixKey.orEmpty().trim().take(PixKeyMaxLength)
        val banco = pixBank.orEmpty().trim().take(PixBankMaxLength)
        val titular = pixHolder.orEmpty().trim().take(PixHolderMaxLength)
        if (chave.isBlank() || banco.isBlank() || titular.isBlank()) return null
        val whatsapp = pixWhatsapp.orEmpty().trim().take(PhoneMaxLength)
        val values = mutableMapOf(
            "chave" to JsonPrimitive(chave),
            "banco" to JsonPrimitive(banco),
            "titular" to JsonPrimitive(titular),
        )
        if (whatsapp.isNotBlank()) {
            values["whatsapp"] = JsonPrimitive(whatsapp)
        }
        return JsonObject(values)
    }

    private companion object {
        const val MiniVendorsTable = "mini_vendors"
        const val ProductsTable = "produtos"
        const val CategoriesTable = "categorias"
        const val OrdersTable = "orders"
        const val MaxProducts = 120
        const val MaxOrders = 160
        const val StoreNameMaxLength = 80
        const val DescriptionMaxLength = 1200
        const val UrlMaxLength = 400
        const val PixKeyMaxLength = 180
        const val PixBankMaxLength = 120
        const val PixHolderMaxLength = 180
        const val PhoneMaxLength = 60
        const val ContactMaxLength = 160
        const val ColorMaxLength = 32
        const val ProductNameMaxLength = 120
        const val ProductDescriptionMaxLength = 1200
        const val ProductLotMaxLength = 80
        const val ProductBadgeMaxLength = 30
        const val ProductTagColorMaxLength = 30
        const val ProductTagEffectMaxLength = 30
        const val ProductStatusMaxLength = 30
        const val MiniVendorColumns =
            "id,tenant_id,user_id,status,store_name,slug,description,logo_url,cover_url,pix_key,pix_bank,pix_holder,pix_whatsapp,instagram,instagram_enabled,whatsapp,whatsapp_enabled,profile_visible,category_visible,products_visible,category_button_color,approved_by,approved_at,created_at,updated_at"
        const val ProductColumns =
            "id,tenant_id,nome,categoria,descricao,img,preco,precoAntigo,estoque,lote,tagLabel,tagColor,tagEffect,active,aprovado,vendidos,cliques,status,seller_type,seller_id,seller_name,seller_logo_url,createdAt,updatedAt"
        const val OrderColumns =
            "id,tenant_id,userId,userName,productId,productName,price,total,quantidade,itens,data,status,approvedBy,seller_type,seller_id,seller_name,seller_logo_url,payment_config,createdAt,updatedAt"
        const val UsersTable = "users"
    }
}

@kotlinx.serialization.Serializable
private data class MiniVendorApproverRow(
    val uid: String = "",
    val nome: String = "",
)

/** `compactUserId` do web: encurta o id quando o nome não resolve. */
private fun String.compactUserId(): String =
    if (length > 18) "${take(8)}...${takeLast(4)}" else this

private fun String.toMiniVendorSlug(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
        .replace("[^a-z0-9]+".toRegex(), "-")
        .trim('-')
    return normalized.take(80).ifBlank { "mini-vendor" }
}

private fun String.parseMoneyOrNull(): Double? {
    val clean = trim()
        .replace("R$", "", ignoreCase = true)
        .replace(" ", "")
        .replace(".", "")
        .replace(",", ".")
    if (clean.isBlank()) return null
    return clean.toDoubleOrNull()
}

private fun String.parseIntSafe(): Int {
    val digits = filter(Char::isDigit)
    return digits.toIntOrNull() ?: 0
}

private val MiniVendorDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM • HH:mm", Locale.forLanguageTag("pt-BR"))
        .withZone(ZoneId.of("America/Sao_Paulo"))

@Serializable
private data class MiniVendorProfileRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val status: String? = null,
    @SerialName("store_name") val storeName: String? = null,
    val slug: String? = null,
    val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("pix_key") val pixKey: String? = null,
    @SerialName("pix_bank") val pixBank: String? = null,
    @SerialName("pix_holder") val pixHolder: String? = null,
    @SerialName("pix_whatsapp") val pixWhatsapp: String? = null,
    val instagram: String? = null,
    @SerialName("instagram_enabled") val instagramEnabled: Boolean? = null,
    val whatsapp: String? = null,
    @SerialName("whatsapp_enabled") val whatsappEnabled: Boolean? = null,
    @SerialName("profile_visible") val profileVisible: Boolean? = null,
    @SerialName("category_visible") val categoryVisible: Boolean? = null,
    @SerialName("products_visible") val productsVisible: Boolean? = null,
    @SerialName("category_button_color") val categoryButtonColor: String? = null,
    @SerialName("approved_by") val approvedBy: String? = null,
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
private data class MiniVendorStoreCategoryRow(
    val id: String = "",
    @SerialName("cover_img") val coverImg: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
)

@Serializable
private data class MiniVendorProductRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val nome: String? = null,
    val categoria: String? = null,
    val descricao: String? = null,
    val img: String? = null,
    val preco: Double? = null,
    @SerialName("precoAntigo") val precoAntigo: Double? = null,
    val estoque: Int? = null,
    val lote: String? = null,
    @SerialName("tagLabel") val tagLabel: String? = null,
    @SerialName("tagColor") val tagColor: String? = null,
    @SerialName("tagEffect") val tagEffect: String? = null,
    val active: Boolean? = null,
    val aprovado: Boolean? = null,
    val vendidos: Int? = null,
    val cliques: Int? = null,
    val status: String? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
private data class MiniVendorProductStockRow(
    val id: String = "",
    val estoque: Int? = null,
    val vendidos: Int? = null,
)

@Serializable
private data class MiniVendorOrderRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("userId") val userId: String? = null,
    @SerialName("userName") val userName: String? = null,
    @SerialName("productId") val productId: String? = null,
    @SerialName("productName") val productName: String? = null,
    val price: Double? = null,
    val total: Double? = null,
    val quantidade: Int? = null,
    val itens: Int? = null,
    val status: String? = null,
    @SerialName("approvedBy") val approvedBy: String? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
) {
    fun amount(): Double {
        val cleanTotal = total ?: 0.0
        if (cleanTotal > 0.0) return cleanTotal
        val cleanPrice = price ?: 0.0
        val cleanQuantity = quantidade ?: itens ?: 1
        return cleanPrice * cleanQuantity.coerceAtLeast(1)
    }

    fun quantity(): Int {
        return (quantidade ?: itens ?: 1).coerceAtLeast(1)
    }
}
