package com.example.usc1.domain.model

import java.util.Locale

/**
 * Entrada normalizada do BI Loja (M8.3/M8.4).
 *
 * Fonte: `web-reference/src/components/ProductManagementAnalytics.tsx`, o bloco de acessores
 * (63-180) e as props do componente (32-39).
 *
 * O web recebe `Row[]` cru do Supabase e resolve cada campo por cadeia de apelidos
 * (`row.nome ?? row.productName ?? row.name`). Aqui a cadeia é resolvida na borda — no
 * repositório — e o motor recebe registro já tipado. É a mesma escolha do M8.1 para o BI de
 * Eventos: o escopo e a normalização ficam na consulta, não no motor.
 *
 * Um só conjunto de tipos atende os cinco players: tenant, liga, comissão, diretório e
 * mini-vendor. O que muda entre eles é o recorte da consulta, nunca a métrica.
 */

/** Player que abre o BI Loja. `seller_type` do web, mais o tenant. */
enum class ProductBiScope(val remoteValue: String) {
    Tenant("tenant"),
    League("league"),
    Commission("commission"),
    Directory("directory"),
    MiniVendor("mini_vendor"),
}

/**
 * Vendedores que o BI Loja do tenant recusa (`AdminBiDashboard` 1399 e 1409):
 * `["mini_vendor", "league", "liga"].includes(sellerType)`.
 *
 * "Produtos oficiais da loja da atlética, sem misturar mini vendors, ligas ou outros players"
 * é o subtítulo do web. Note que a lista tem `liga` **e** `league`, mas não tem `commission`
 * nem `directory`: comissão e diretório continuam entrando no BI Loja do tenant. É o que o web
 * faz; não é engano do port.
 */
val ProductBiExcludedSellerTypes: List<String> = listOf("mini_vendor", "league", "liga")

/**
 * `tenantProducts` (1396-1402): recusa mini vendor/liga; aceita linha sem `seller_id`; e,
 * quando há `seller_id`, exige que ele seja o do tenant ativo.
 */
fun productBiTenantOwnsProduct(sellerType: String?, sellerId: String?, tenantId: String): Boolean {
    val type = sellerType?.trim()?.lowercase(Locale.ROOT).orEmpty()
    if (type in ProductBiExcludedSellerTypes) return false
    val id = sellerId?.trim().orEmpty()
    if (id.isEmpty()) return true
    val tenant = tenantId.trim()
    return tenant.isEmpty() || id == tenant
}

/**
 * `tenantOrders` (1406-1413). A diferença para o produto está na primeira linha: pedido de um
 * produto que já passou no recorte entra **mesmo que o próprio pedido esteja marcado como de
 * outro vendedor**. Sem isso, um pedido com `seller_type` desatualizado sumiria da receita de
 * um produto que o painel mostra.
 */
fun productBiTenantOwnsOrder(
    sellerType: String?,
    sellerId: String?,
    productId: String?,
    tenantProductIds: Set<String>,
    tenantId: String,
): Boolean {
    if (productId?.trim().orEmpty() in tenantProductIds && productId?.trim().orEmpty().isNotEmpty()) return true
    return productBiTenantOwnsProduct(sellerType, sellerId, tenantId)
}

/** `statusIsApproved` (117-120). Lista própria, menor que a do BI de Eventos. */
fun productBiStatusIsApproved(status: String?): Boolean {
    val normalized = status?.trim()?.lowercase(Locale.ROOT).orEmpty()
    return normalized in ProductBiApprovedStatuses
}

internal val ProductBiApprovedStatuses = listOf(
    "approved", "aprovado", "aprovada", "pago", "paid", "confirmado", "confirmada", "entregue",
)

/** `COLORS` (63). */
val ProductBiColors: List<String> = listOf(
    "#12d18e", "#38bdf8", "#facc15", "#f97316", "#f472b6", "#a78bfa", "#fb7185", "#22c55e",
)

/** `WEEKDAYS` (64), começando no domingo como `Date.getDay()`. */
val ProductBiWeekdays: List<String> = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

/** Uma linha de `produtos` já normalizada. */
data class ProductBiProduct(
    val id: String,
    /** `productName` (122): `nome ?? productName ?? name`, com "Produto" de reserva. */
    val name: String = "Produto",
    /** `asString(product.categoria) || "Sem categoria"` (391). */
    val category: String = "Sem categoria",
    /** `asString(product.lote) || "Sem lote"` (392). */
    val lot: String = "Sem lote",
    val stock: Double = 0.0,
    /** `vendidos` da própria linha — o motor soma os pedidos aprovados em cima (443). */
    val sold: Double = 0.0,
    val clicks: Double = 0.0,
    /** `Array.isArray(product.likes) ? product.likes.length : parseNumber(product.likes)` (387). */
    val likes: Double = 0.0,
    val price: Double = 0.0,
    val oldPrice: Double = 0.0,
) {
    /** `hasDiscount` (428): `precoAntigo > preco`. */
    val hasDiscount: Boolean get() = oldPrice > price
}

/** Uma linha de `orders` já normalizada. */
data class ProductBiOrder(
    val id: String,
    val productId: String = "",
    /** `asString(order.productName) || productName(product ?? order)` (421). */
    val productName: String = "",
    /** `orderQuantity` (134): `max(1, floor(quantidade ?? itens ?? qtd, 1))`. */
    val quantity: Double = 1.0,
    /** `orderTotal` (138): `total ?? valorTotal`, ou `preço × quantidade` quando aquele é zero. */
    val total: Double = 0.0,
    val status: String = "",
    /** `buyer` (427): `userId ?? userName ?? email`, com `id` de reserva. */
    val buyerKey: String = "",
    /** `asString(order.userName) || "Comprador"` (440). */
    val buyerName: String = "Comprador",
    val userId: String = "",
    /** `orderData.userTurma ?? orderData.turma` (426), antes de cair no mapa de usuários. */
    val turmaFromData: String = "",
    /** `orderVariantLabel` (143-153). Vazio quando o pedido não tem tamanho nem cor. */
    val variantLabel: String = "",
    /**
     * `weekdayLabel(order.createdAt ?? order.data)` (438). `null` quando a linha não tem data:
     * o web devolve "Sem data", e como o gráfico só percorre os sete dias (533) a linha some.
     */
    val weekdayIndex: Int? = null,
)

/** Uma linha de `users` já normalizada — só o que `userTurma` (381) usa. */
data class ProductBiUser(
    val uid: String,
    /** `asString(user.turma ?? user.userTurma) || "Sem turma"`. */
    val turma: String = "Sem turma",
)

/** `id`/`title` do `<select>` de produto (363-369). */
data class ProductBiOption(val id: String, val title: String)

/**
 * O recorte já escopado que o motor consome, mais os três textos que cada player troca
 * (`title`, `subtitle`, `allLabel` — props 36-38).
 */
data class ProductBiDataset(
    val scope: ProductBiScope = ProductBiScope.Tenant,
    val products: List<ProductBiProduct> = emptyList(),
    val orders: List<ProductBiOrder> = emptyList(),
    val users: List<ProductBiUser> = emptyList(),
    val title: String = "Gestão de produtos",
    val subtitle: String = "Vendas, conversão, estoque e recompra no catálogo deste escopo.",
    val allLabel: String = "Todos os produtos",
    /** Nome do vendedor dono do recorte; só o mini-vendor usa, como `title` (`profile.storeName`). */
    val sellerName: String = "",
    /**
     * `Boolean(profile?.id)` da página do mini-vendor. Distingue "lojinha não cadastrada"
     * (o web mostra "Cadastre a lojinha antes de abrir a gestão.") de "cadastrada e sem venda".
     */
    val hasSellerProfile: Boolean = true,
) {
    val isEmpty: Boolean get() = products.isEmpty() && orders.isEmpty()

    /** `productOptions` (363-369): dedup por id, na ordem em que o catálogo veio. */
    val productOptions: List<ProductBiOption>
        get() = products
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }
            .map { ProductBiOption(it.id, it.name) }

    companion object {
        /** `productFilter` inicial (361). */
        const val AllProducts = "todos"
    }
}
