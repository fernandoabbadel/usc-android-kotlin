package com.example.usc1.domain.model

/**
 * Motor do BI Loja (M8.3).
 *
 * Fonte: `web-reference/src/components/ProductManagementAnalytics.tsx`, o `useMemo` de
 * `analytics` (371-543) e os agregadores `addMetric`/`metricRows` (166-180).
 *
 * Um motor só para os cinco players — tenant, liga, comissão, diretório e mini-vendor. Nenhum
 * deles tem métrica própria: o web usa o mesmo componente em todos, mudando apenas `products`,
 * `orders`, `users` e os três textos do cabeçalho. O escopo já chega resolvido no
 * [ProductBiDataset]; o motor não sabe de qual player veio.
 */

/** `MetricRow` (41-47). Nomes do web: `qtd`, `valor`, `medio`, `extra`. */
data class ProductBiMetricRow(
    val name: String,
    val qtd: Double = 0.0,
    val valor: Double = 0.0,
    val medio: Double = 0.0,
    val extra: Double = 0.0,
)

/** `ProductMetric` (49-61) — o acumulador por produto. */
private data class ProductBiProductMetric(
    val id: String,
    val name: String,
    val category: String,
    val lot: String,
    val stock: Double,
    var sold: Double,
    val clicks: Double,
    val likes: Double,
    var revenue: Double = 0.0,
    var orders: Double = 0.0,
    var discountedRevenue: Double = 0.0,
)

/** O retorno do `useMemo` (509-542), campo a campo. */
data class ProductBiAnalytics(
    val revenue: Double = 0.0,
    val itemQtd: Double = 0.0,
    val approvedOrderCount: Int = 0,
    val selectedOrderCount: Int = 0,
    val uniqueBuyers: Int = 0,
    val averageOrder: Double = 0.0,
    val clickConversion: Double = 0.0,
    val sellThrough: Double = 0.0,
    val stalledCount: Int = 0,
    val repeatRate: Double = 0.0,
    val topFiveDependency: Double = 0.0,
    val byProduct: List<ProductBiMetricRow> = emptyList(),
    val byVariant: List<ProductBiMetricRow> = emptyList(),
    val byLot: List<ProductBiMetricRow> = emptyList(),
    val byCategory: List<ProductBiMetricRow> = emptyList(),
    val byWeekday: List<ProductBiMetricRow> = emptyList(),
    val byClass: List<ProductBiMetricRow> = emptyList(),
    val byUser: List<ProductBiMetricRow> = emptyList(),
    val engagementRows: List<ProductBiMetricRow> = emptyList(),
    val stockRows: List<ProductBiMetricRow> = emptyList(),
    val stalledRows: List<ProductBiMetricRow> = emptyList(),
    val discountRows: List<ProductBiMetricRow> = emptyList(),
    val recurrenceRows: List<ProductBiMetricRow> = emptyList(),
    val abcRows: List<ProductBiMetricRow> = emptyList(),
)

/** `safeDivide` (113-115): denominador zero devolve zero, não `NaN`. */
internal fun productBiSafeDivide(numerator: Double, denominator: Double): Double =
    if (denominator == 0.0) 0.0 else numerator / denominator

/**
 * `addMetric` (166-174). `medio` é recalculado a cada soma, então ele é sempre
 * `valor acumulado / qtd acumulada` — não a média das médias.
 */
internal fun MutableMap<String, ProductBiMetricRow>.addProductBiMetric(
    name: String,
    qtd: Double,
    valor: Double,
    extra: Double = 0.0,
) {
    // `name.trim() || "Sem nome"` (167).
    val cleanName = name.trim().ifBlank { "Sem nome" }
    val current = this[cleanName] ?: ProductBiMetricRow(cleanName)
    val nextQtd = current.qtd + qtd
    val nextValor = current.valor + valor
    this[cleanName] = current.copy(
        qtd = nextQtd,
        valor = nextValor,
        extra = current.extra + extra,
        medio = productBiSafeDivide(nextValor, nextQtd),
    )
}

/**
 * `metricRows` (176-180): ordena por `valor`, depois `qtd`, depois `extra` — todos decrescentes
 * — e corta em `limit` (12 por padrão).
 */
internal fun Map<String, ProductBiMetricRow>.productBiMetricRows(limit: Int = 12): List<ProductBiMetricRow> =
    values
        .sortedWith(
            compareByDescending<ProductBiMetricRow> { it.valor }
                .thenByDescending { it.qtd }
                .thenByDescending { it.extra },
        )
        .take(limit)

/**
 * O `useMemo` de `analytics` (371-543).
 *
 * @param productFilter `"todos"` ou o id do produto escolhido no `<select>` (361).
 */
fun buildProductBiAnalytics(
    dataset: ProductBiDataset,
    productFilter: String = ProductBiDataset.AllProducts,
): ProductBiAnalytics {
    val all = ProductBiDataset.AllProducts
    val products = dataset.products
    val orders = dataset.orders

    // 372-373
    val selectedProducts = if (productFilter == all) {
        products
    } else {
        products.filter { it.id == productFilter }
    }
    val selectedProductIds = selectedProducts.map { it.id }.filter { it.isNotBlank() }.toSet()

    // 375-378. Com catálogo vazio (`selectedProductIds.size === 0`) o primeiro teste é pulado e
    // o pedido só precisa passar no filtro de produto — é como o web se comporta.
    val selectedOrders = orders.filter { order ->
        if (selectedProductIds.isNotEmpty() && order.productId !in selectedProductIds) return@filter false
        productFilter == all || order.productId == productFilter
    }
    val approvedOrders = selectedOrders.filter { productBiStatusIsApproved(it.status) }

    val productMap = selectedProducts.associateBy { it.id }
    // `userTurma` (381)
    val userTurma = dataset.users.associate { it.uid to it.turma.ifBlank { "Sem turma" } }

    // 383-401
    val productMetrics = LinkedHashMap<String, ProductBiProductMetric>()
    selectedProducts.forEach { product ->
        if (product.id.isBlank()) return@forEach
        productMetrics[product.id] = ProductBiProductMetric(
            id = product.id,
            name = product.name,
            category = product.category,
            lot = product.lot,
            stock = product.stock,
            sold = product.sold,
            clicks = product.clicks,
            likes = product.likes,
        )
    }

    val byProduct = LinkedHashMap<String, ProductBiMetricRow>()
    val byVariant = LinkedHashMap<String, ProductBiMetricRow>()
    val byLot = LinkedHashMap<String, ProductBiMetricRow>()
    val byCategory = LinkedHashMap<String, ProductBiMetricRow>()
    val byWeekday = LinkedHashMap<String, ProductBiMetricRow>()
    val byClass = LinkedHashMap<String, ProductBiMetricRow>()
    val byUser = LinkedHashMap<String, ProductBiMetricRow>()
    val buyers = LinkedHashMap<String, Int>()
    var revenue = 0.0
    var itemQtd = 0.0
    var discountRevenue = 0.0

    // 415-448
    approvedOrders.forEach { order ->
        val product = productMap[order.productId]
        val qtd = order.quantity
        val value = order.total
        val metric = productMetrics[order.productId]
        // 421: o nome do pedido tem prioridade sobre o do catálogo.
        val productLabel = order.productName.trim().ifBlank { product?.name ?: "Produto" }
        val variantLabel = order.variantLabel
        val lot = product?.lot?.trim().orEmpty().ifBlank { "Sem lote" }
        val category = product?.category?.trim().orEmpty().ifBlank { "Sem categoria" }
        // 426: `data.userTurma`, depois o mapa de usuários, depois "Sem turma".
        val turma = order.turmaFromData.trim()
            .ifBlank { userTurma[order.userId].orEmpty() }
            .ifBlank { "Sem turma" }
        val hasDiscount = product?.hasDiscount == true

        revenue += value
        itemQtd += qtd
        if (hasDiscount) discountRevenue += value
        buyers[order.buyerKey] = (buyers[order.buyerKey] ?: 0) + 1
        byProduct.addProductBiMetric(productLabel, qtd, value)
        // 435: variação só entra quando o pedido tem tamanho/cor.
        if (variantLabel.isNotBlank()) {
            byVariant.addProductBiMetric("$productLabel • $variantLabel", qtd, value)
        }
        byLot.addProductBiMetric(lot, qtd, value)
        byCategory.addProductBiMetric(category, qtd, value)
        order.weekdayIndex?.let { byWeekday.addProductBiMetric(ProductBiWeekdays[it], qtd, value) }
        byClass.addProductBiMetric(turma, qtd, value)
        byUser.addProductBiMetric(order.buyerName, qtd, value)

        if (metric != null) {
            metric.sold += qtd
            metric.revenue += value
            metric.orders += 1.0
            if (hasDiscount) metric.discountedRevenue += value
        }
    }

    val metricList = productMetrics.values.toList()

    // 450-458: ordena por cliques (`valor`), não por likes.
    val engagementRows = metricList
        .map { metric ->
            ProductBiMetricRow(
                name = metric.name,
                qtd = metric.likes,
                valor = metric.clicks,
                medio = productBiSafeDivide(metric.orders, metric.clicks) * 100,
            )
        }
        .sortedWith(compareByDescending<ProductBiMetricRow> { it.valor }.thenByDescending { it.qtd })
        .take(12)

    // 460-471: `base = estoque + vendidos`, e a ordenação é pela taxa de saída.
    val stockRows = metricList
        .map { metric ->
            val base = metric.stock + metric.sold
            ProductBiMetricRow(
                name = metric.name,
                qtd = metric.sold,
                valor = metric.stock,
                medio = productBiSafeDivide(metric.sold, base) * 100,
            )
        }
        .sortedByDescending { it.medio }
        .take(12)

    // 473-482: parado é estoque positivo com venda zero, **ou** 5+ cliques sem nenhum pedido.
    val stalledRows = metricList
        .filter { metric -> metric.stock > 0 && (metric.sold == 0.0 || (metric.clicks >= 5 && metric.orders == 0.0)) }
        .map { metric ->
            ProductBiMetricRow(
                name = metric.name,
                qtd = metric.stock,
                valor = metric.clicks,
                medio = productBiSafeDivide(metric.orders, metric.clicks) * 100,
            )
        }
        .sortedWith(compareByDescending<ProductBiMetricRow> { it.qtd }.thenByDescending { it.valor })
        .take(12)

    // 484-487. `qtd` de "Com desconto" é 1 ou 0, não a contagem de pedidos: o web usa a fatia só
    // para dizer se existe receita com desconto. "Sem desconto" desconta esse mesmo 1.
    val discountRows = listOf(
        ProductBiMetricRow(
            name = "Com desconto",
            qtd = if (discountRevenue > 0) 1.0 else 0.0,
            valor = discountRevenue,
        ),
        ProductBiMetricRow(
            name = "Sem desconto",
            qtd = maxOf(0.0, approvedOrders.size - (if (discountRevenue > 0) 1.0 else 0.0)),
            valor = maxOf(0.0, revenue - discountRevenue),
        ),
    ).filter { it.valor > 0 || it.qtd > 0 }

    // 489-492: conta comprador, não pedido, e `valor` fica zerado de propósito.
    val recurrenceRows = listOf(
        ProductBiMetricRow(name = "Primeira compra", qtd = buyers.values.count { it == 1 }.toDouble()),
        ProductBiMetricRow(name = "Recompra", qtd = buyers.values.count { it > 1 }.toDouble()),
    )

    // 494-507
    val byProductRows = byProduct.productBiMetricRows(14)
    val topFiveRevenue = byProductRows.take(5).sumOf { it.valor }
    var cumulative = 0.0
    val abcMap = LinkedHashMap<String, ProductBiMetricRow>()
    byProductRows.forEach { row ->
        cumulative += row.valor
        // A faixa sai do acumulado sobre a receita **inteira** do recorte (499), não sobre a
        // soma das 14 linhas: com receita fora do top 14, nenhuma linha chega a 100%.
        val share = productBiSafeDivide(cumulative, revenue) * 100
        val bucket = if (share <= 80) "A" else if (share <= 95) "B" else "C"
        abcMap.addProductBiMetric(bucket, 1.0, row.valor)
    }

    val totalOrders = metricList.sumOf { it.orders }
    val totalClicks = metricList.sumOf { it.clicks }
    val totalSold = metricList.sumOf { it.sold }
    val totalBase = metricList.sumOf { it.sold + it.stock }

    return ProductBiAnalytics(
        revenue = revenue,
        itemQtd = itemQtd,
        approvedOrderCount = approvedOrders.size,
        selectedOrderCount = selectedOrders.size,
        uniqueBuyers = buyers.size,
        averageOrder = productBiSafeDivide(revenue, approvedOrders.size.toDouble()),
        // 516-520: pedidos **do acumulador por produto**, não `approvedOrders.length`.
        clickConversion = productBiSafeDivide(totalOrders, totalClicks) * 100,
        sellThrough = productBiSafeDivide(totalSold, totalBase) * 100,
        stalledCount = stalledRows.size,
        repeatRate = productBiSafeDivide(
            buyers.values.count { it > 1 }.toDouble(),
            buyers.size.toDouble(),
        ) * 100,
        topFiveDependency = productBiSafeDivide(topFiveRevenue, revenue) * 100,
        byProduct = byProductRows,
        byVariant = byVariant.productBiMetricRows(14),
        byLot = byLot.productBiMetricRows(),
        byCategory = byCategory.productBiMetricRows(),
        // 533: percorre os sete dias fixos, então "Sem data" nunca aparece no gráfico.
        byWeekday = ProductBiWeekdays.map { day -> byWeekday[day] ?: ProductBiMetricRow(day) },
        byClass = byClass.productBiMetricRows(),
        byUser = byUser.productBiMetricRows(12),
        engagementRows = engagementRows,
        stockRows = stockRows,
        stalledRows = stalledRows,
        discountRows = discountRows,
        recurrenceRows = recurrenceRows,
        abcRows = abcMap.productBiMetricRows(3),
    )
}
