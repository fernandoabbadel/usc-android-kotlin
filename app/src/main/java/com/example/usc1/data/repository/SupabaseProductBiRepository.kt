package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.ProductBiDataset
import com.example.usc1.domain.model.ProductBiOrder
import com.example.usc1.domain.model.ProductBiProduct
import com.example.usc1.domain.model.ProductBiScope
import com.example.usc1.domain.model.ProductBiUser
import com.example.usc1.domain.model.firstText
import com.example.usc1.domain.model.num
import com.example.usc1.domain.model.obj
import com.example.usc1.domain.model.parseEventBiDate
import com.example.usc1.domain.model.productBiTenantOwnsOrder
import com.example.usc1.domain.model.productBiTenantOwnsProduct
import com.example.usc1.domain.model.str
import com.example.usc1.domain.repository.ProductBiRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import java.time.Instant
import java.time.ZoneId
import kotlin.math.floor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * BI Loja com Supabase direto, nos cinco escopos (M8.3 + M8.4).
 *
 * ## Diferença de consulta em relação ao web
 *
 * `AdminBiDashboard.loadDashboardData` (linha 559) baixa `produtos` 1600, `orders` 5000 e
 * `users` 5000 do tenant inteiro e só depois aplica o recorte de vendedor em memória
 * (`tenantProducts`/`tenantOrders`, 1396-1413). O `LeagueFinanceDashboard` faz o equivalente
 * para o coletivo. Aqui o escopo vai para a consulta sempre que dá:
 *
 * - liga/comissão/diretório e mini-vendor: `produtos` filtrado por `seller_id`, e `orders`
 *   filtrado por `productId in (...)`. Nenhuma linha de outro vendedor chega ao aparelho;
 * - tenant: a exclusão de `mini_vendor`/`league`/`liga` também vai para a consulta, com um
 *   `or` que preserva a linha sem `seller_type`. A regra completa (`seller_id` igual ao tenant)
 *   continua sendo aplicada em memória, porque o `not.in` do PostgREST compara texto exato e
 *   deixaria passar um `Liga` com maiúscula. Consulta e memória juntas dão o mesmo resultado do
 *   web, com menos bytes.
 *
 * `users` é o único caso em que o app baixa mais do que precisaria por linha, porque o mapa
 * `userTurma` (381 do web) é montado antes de saber quais compradores aparecem: a consulta traz
 * só `uid,turma`, com teto, e nunca sem `tenant_id`.
 */
class SupabaseProductBiRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : ProductBiRepository {

    override suspend fun getDataset(
        tenantId: String,
        scope: ProductBiScope,
        sellerId: String,
        userId: String,
    ): ProductBiDataset = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) return@withContext ProductBiDataset(scope = scope)

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return@withContext ProductBiDataset(scope = scope)

        // `fetchCurrentMiniVendorProfile`: sem perfil não há lojinha para analisar, e a página
        // do web para em "Cadastre a lojinha antes de abrir a gestão." (`!profile?.id`).
        val profile = if (scope == ProductBiScope.MiniVendor && sellerId.isBlank()) {
            fetchMiniVendorProfile(client, cleanTenantId, userId)
        } else {
            MiniVendorProfile(id = sellerId.trim())
        }
        val resolvedSellerId = profile.id
        if (scope != ProductBiScope.Tenant && resolvedSellerId.isBlank()) {
            return@withContext ProductBiDataset(
                scope = scope,
                hasSellerProfile = scope != ProductBiScope.MiniVendor,
            )
        }

        val productRows = fetchProducts(client, cleanTenantId, scope, resolvedSellerId)
        val products = productRows.map { it.toProduct() }
        val productIds = products.map { it.id }.filter { it.isNotBlank() }.distinct()

        val orderRows = fetchOrders(client, cleanTenantId, scope, resolvedSellerId, productIds)
        val orders = orderRows
            .filter { row ->
                // `tenantOrders` (1406-1413): só o escopo tenant reavalia o vendedor da linha.
                scope != ProductBiScope.Tenant || productBiTenantOwnsOrder(
                    sellerType = row.str("seller_type"),
                    sellerId = row.str("seller_id"),
                    productId = row.str("productId"),
                    tenantProductIds = productIds.toSet(),
                    tenantId = cleanTenantId,
                )
            }
            .map { it.toOrder() }

        ProductBiDataset(
            scope = scope,
            products = products,
            orders = orders,
            users = fetchUsers(client, cleanTenantId, scope),
            sellerName = profile.storeName,
            hasSellerProfile = true,
        )
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    private suspend fun fetchProducts(
        client: SupabaseClient,
        tenantId: String,
        scope: ProductBiScope,
        sellerId: String,
    ): List<JsonObject> {
        val rows = queryRows(
            client, ProductsTable, ProductColumns, ProductFallbackColumns, ProductsLimit,
            orderColumn = "createdAt",
        ) {
            eq("tenant_id", tenantId)
            if (scope != ProductBiScope.Tenant) eq("seller_id", sellerId)
        }
        if (scope != ProductBiScope.Tenant) return rows
        // A exclusão do tenant fica em memória, e é o único filtro do M8 que não desceu para a
        // consulta: a regra (1396-1402) lê duas colunas e compara `seller_type` em minúsculas,
        // enquanto o `not.in` do PostgREST compara texto exato — um `Liga` gravado com maiúscula
        // passaria pela consulta e teria de ser recusado aqui de qualquer jeito. Como o teto já
        // é `tenant_id` + 400 linhas, o ganho não pagaria o risco de divergir do web.
        return rows.filter {
            productBiTenantOwnsProduct(it.str("seller_type"), it.str("seller_id"), tenantId)
        }
    }

    private suspend fun fetchOrders(
        client: SupabaseClient,
        tenantId: String,
        scope: ProductBiScope,
        sellerId: String,
        productIds: List<String>,
    ): List<JsonObject> {
        // Fora do tenant o pedido só interessa se apontar para um produto do escopo: catálogo
        // vazio significa BI vazio, e a consulta nem chega a sair.
        if (scope != ProductBiScope.Tenant && productIds.isEmpty()) return emptyList()

        return queryRows(
            client, OrdersTable, OrderColumns, OrderFallbackColumns, OrdersLimit,
            orderColumn = "createdAt",
        ) {
            eq("tenant_id", tenantId)
            when (scope) {
                ProductBiScope.Tenant -> Unit
                ProductBiScope.MiniVendor -> or {
                    isIn("productId", productIds)
                    eq("seller_id", sellerId)
                }
                else -> isIn("productId", productIds)
            }
        }
    }

    /**
     * `users` (`uid,turma`). O mini-vendor é o único player que **não** passa a prop `users`
     * (a página de gestão da lojinha chama `<ProductManagementAnalytics products orders />` sem
     * ela), então lá o mapa de turma fica vazio de propósito e a turma sai do próprio pedido.
     */
    private suspend fun fetchUsers(
        client: SupabaseClient,
        tenantId: String,
        scope: ProductBiScope,
    ): List<ProductBiUser> {
        if (scope == ProductBiScope.MiniVendor) return emptyList()
        return queryRows(client, UsersTable, UserColumns, UserColumns, UsersLimit) {
            eq("tenant_id", tenantId)
        }.map { row ->
            ProductBiUser(
                uid = firstText(row.str("uid"), row.str("id")),
                turma = firstText(row.str("turma"), row.str("userTurma")).ifBlank { "Sem turma" },
            )
        }.filter { it.uid.isNotBlank() }
    }

    /** `fetchCurrentMiniVendorProfile({ tenantId, userId })` do web. */
    private suspend fun fetchMiniVendorProfile(
        client: SupabaseClient,
        tenantId: String,
        userId: String,
    ): MiniVendorProfile {
        val clean = userId.trim()
        if (clean.isBlank()) return MiniVendorProfile()
        val row = queryRows(
            client, MiniVendorsTable, "id,tenant_id,user_id,store_name", "id,user_id", 1L,
        ) {
            eq("tenant_id", tenantId)
            eq("user_id", clean)
        }.firstOrNull() ?: return MiniVendorProfile()
        return MiniVendorProfile(id = row.str("id"), storeName = row.str("store_name"))
    }

    private data class MiniVendorProfile(val id: String = "", val storeName: String = "")

    /** Mesma degradação de coluna de `SupabaseEventBiRepository`. */
    private suspend fun queryRows(
        client: SupabaseClient,
        table: String,
        columns: String,
        fallbackColumns: String,
        limit: Long,
        orderColumn: String? = null,
        extraFilter: PostgrestFilterBuilder.() -> Unit = {},
    ): List<JsonObject> {
        suspend fun query(selected: String, ordered: Boolean): List<JsonObject> =
            client.from(table)
                .select(columns = Columns.raw(selected)) {
                    filter { extraFilter() }
                    if (ordered) orderColumn?.let { order(column = it, order = Order.DESCENDING) }
                    limit(count = limit)
                }
                .decodeList<JsonObject>()

        return runCatching { query(columns, ordered = true) }
            .recoverCatching { query(columns, ordered = false) }
            .recoverCatching { query(fallbackColumns, ordered = false) }
            .getOrDefault(emptyList())
    }

    private suspend fun resolveTenantId(client: SupabaseClient, tenantId: String): String {
        val clean = tenantId.trim()
        if (clean.isNotBlank()) return clean
        return runCatching { SupabaseTenantResolver.resolveActiveTenantId(client) }.getOrDefault("")
    }

    // ------------------------------------------------------------------
    // Mapeamento das linhas (os acessores 122-164 do web)
    // ------------------------------------------------------------------

    /** `productId` (126) e `productName` (122). */
    private fun JsonObject.toProduct(): ProductBiProduct {
        val likesElement = this["likes"]
        return ProductBiProduct(
            id = firstText(str("id"), str("productId"), str("produto_id")),
            name = firstText(str("nome"), str("productName"), str("name")).ifBlank { "Produto" },
            category = str("categoria").trim().ifBlank { "Sem categoria" },
            lot = str("lote").trim().ifBlank { "Sem lote" },
            stock = num("estoque"),
            sold = num("vendidos"),
            clicks = num("cliques"),
            // 387: array de likes conta o tamanho; número conta o próprio valor.
            likes = if (likesElement is JsonArray) likesElement.size.toDouble() else num("likes"),
            price = num("preco"),
            oldPrice = num("precoAntigo"),
        )
    }

    private fun JsonObject.toOrder(): ProductBiOrder {
        // `orderQuantity` (134): mínimo 1, sempre inteiro.
        val quantity = maxOf(
            1.0,
            floor(firstNumber("quantidade", "itens", "qtd", fallback = 1.0)),
        )
        // `orderTotal` (138): `total ?? valorTotal`; zero cai para `preço × quantidade`.
        val declared = firstNumber("total", "valorTotal")
        val total = if (declared != 0.0) declared else firstNumber("price", "preco") * quantity
        val data = obj("data")

        return ProductBiOrder(
            id = str("id"),
            // `orderProductId` (130)
            productId = firstText(str("productId"), str("produtoId"), str("product_id"), str("produto_id")),
            productName = str("productName"),
            quantity = quantity,
            total = total,
            status = str("status"),
            // `buyer` (427): id, nome, e-mail e, por último, o próprio pedido.
            buyerKey = firstText(str("userId"), str("userName"), str("email")).ifBlank { str("id") },
            buyerName = str("userName").trim().ifBlank { "Comprador" },
            userId = str("userId"),
            turmaFromData = firstText(data.str("userTurma"), data.str("turma")),
            variantLabel = variantLabel(data),
            weekdayIndex = weekdayIndexOf(str("createdAt")),
        )
    }

    /** `orderVariantLabel` (143-153). */
    private fun variantLabel(data: JsonObject?): String {
        val explicit = firstText(data.str("varianteLabel"), data.str("variantLabel")).trim()
        if (explicit.isNotBlank()) return explicit
        val size = firstText(data.str("tamanhoSelecionado"), data.str("variantSize")).trim()
        val color = firstText(data.str("corVariante"), data.str("variantColor")).trim()
        return listOf(
            if (size.isNotBlank()) "Tamanho $size" else "",
            if (color.isNotBlank()) "Cor $color" else "",
        ).filter { it.isNotBlank() }.joinToString(" • ")
    }

    /**
     * `weekdayLabel` (161-164) = `WEEKDAYS[date.getDay()]`. Sem data o web devolve "Sem data",
     * que some do gráfico (o eixo só percorre os sete dias); aqui isso é `null`.
     */
    private fun weekdayIndexOf(value: String): Int? {
        val millis = parseEventBiDate(value)
        if (millis <= 0L) return null
        // `Date.getDay()`: 0 = domingo. `DayOfWeek.getValue()`: 1 = segunda, 7 = domingo.
        val day = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).dayOfWeek.value
        return day % 7
    }

    private fun JsonObject.firstNumber(vararg keys: String, fallback: Double = 0.0): Double {
        keys.forEach { key ->
            if (this[key] != null) {
                val parsed = num(key, Double.NaN)
                if (!parsed.isNaN()) return parsed
            }
        }
        return fallback
    }

    private companion object {
        const val ProductsTable = "produtos"
        const val OrdersTable = "orders"
        const val UsersTable = "users"
        const val MiniVendorsTable = "mini_vendors"

        /** Tetos do web: `produtos` 1600, `orders` 5000, `users` 5000 (`loadDashboardData`). */
        const val ProductsLimit = 400L
        const val OrdersLimit = 1200L
        const val UsersLimit = 600L

        const val ProductColumns =
            "id,tenant_id,nome,lote,categoria,preco,precoAntigo,estoque,likes,cliques,vendidos," +
                "active,aprovado,status,seller_type,seller_id,seller_name,createdAt"
        const val ProductFallbackColumns =
            "id,nome,lote,categoria,preco,estoque,vendidos,seller_type,seller_id"
        const val OrderColumns =
            "id,tenant_id,userId,userName,productId,productName,quantidade,itens,total,price," +
                "status,createdAt,seller_type,seller_id,seller_name,data"
        const val OrderFallbackColumns =
            "id,userId,userName,productId,productName,quantidade,total,price,status,createdAt"
        const val UserColumns = "uid,turma,tenant_id"
    }
}
