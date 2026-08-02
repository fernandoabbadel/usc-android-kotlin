package com.example.usc1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras do motor do BI Loja (M8.3), portadas de
 * `web-reference/src/components/ProductManagementAnalytics.tsx` — o `useMemo` de `analytics`
 * (371-543) e os agregadores `addMetric`/`metricRows` (166-180).
 *
 * As regras de escopo do tenant (`AdminBiDashboard` 1393-1413) estão em
 * [ProductBiM84RulesTest].
 */
class ProductBiM83RulesTest {

    private fun product(
        id: String,
        name: String = id,
        stock: Double = 0.0,
        sold: Double = 0.0,
        clicks: Double = 0.0,
        likes: Double = 0.0,
        price: Double = 0.0,
        oldPrice: Double = 0.0,
        lot: String = "Sem lote",
        category: String = "Sem categoria",
    ) = ProductBiProduct(
        id = id, name = name, category = category, lot = lot, stock = stock, sold = sold,
        clicks = clicks, likes = likes, price = price, oldPrice = oldPrice,
    )

    private fun order(
        id: String,
        productId: String,
        quantity: Double = 1.0,
        total: Double = 0.0,
        status: String = "aprovado",
        buyerKey: String = "comprador",
        buyerName: String = "Comprador",
        userId: String = "",
        turmaFromData: String = "",
        variantLabel: String = "",
        weekdayIndex: Int? = null,
        productName: String = "",
    ) = ProductBiOrder(
        id = id, productId = productId, productName = productName, quantity = quantity,
        total = total, status = status, buyerKey = buyerKey, buyerName = buyerName,
        userId = userId, turmaFromData = turmaFromData, variantLabel = variantLabel,
        weekdayIndex = weekdayIndex,
    )

    private fun dataset(
        products: List<ProductBiProduct> = emptyList(),
        orders: List<ProductBiOrder> = emptyList(),
        users: List<ProductBiUser> = emptyList(),
    ) = ProductBiDataset(products = products, orders = orders, users = users)

    // ------------------------------------------------------------------
    // Status e agregadores
    // ------------------------------------------------------------------

    /**
     * `statusIsApproved` (117-120). A lista do BI Loja é menor que a do BI de Eventos: não tem
     * `validado` nem `redeemed`, que o `EventBiStatus` aceita.
     */
    @Test
    fun `statusIsApproved usa a lista propria do BI Loja`() {
        listOf("approved", "aprovado", "aprovada", "pago", "paid", "confirmado", "confirmada", "entregue")
            .forEach { assertTrue(it, productBiStatusIsApproved(it)) }
        assertTrue(productBiStatusIsApproved("  APROVADO  "))
        assertFalse(productBiStatusIsApproved("validado"))
        assertFalse(productBiStatusIsApproved("redeemed"))
        assertFalse(productBiStatusIsApproved("pendente"))
    }

    /** `addMetric` (166-174): `medio` é `valor / qtd` acumulado, não a média das médias. */
    @Test
    fun `addMetric recalcula medio sobre o acumulado`() {
        val map = linkedMapOf<String, ProductBiMetricRow>()
        map.addProductBiMetric("Camisa", qtd = 1.0, valor = 100.0)
        map.addProductBiMetric("Camisa", qtd = 3.0, valor = 60.0)
        val row = map.getValue("Camisa")
        assertEquals(4.0, row.qtd, 0.0)
        assertEquals(160.0, row.valor, 0.0)
        // Média das médias daria (100 + 20) / 2 = 60.
        assertEquals(40.0, row.medio, 0.0001)
    }

    /** `name.trim() || "Sem nome"` (167). */
    @Test
    fun `addMetric troca nome vazio por Sem nome`() {
        val map = linkedMapOf<String, ProductBiMetricRow>()
        map.addProductBiMetric("   ", qtd = 1.0, valor = 5.0)
        assertEquals(listOf("Sem nome"), map.keys.toList())
    }

    /** `metricRows` (176-180): valor, depois qtd, depois extra — todos decrescentes. */
    @Test
    fun `metricRows desempata por qtd e depois por extra`() {
        val map = linkedMapOf(
            "a" to ProductBiMetricRow("a", qtd = 2.0, valor = 10.0, extra = 1.0),
            "b" to ProductBiMetricRow("b", qtd = 5.0, valor = 10.0, extra = 0.0),
            "c" to ProductBiMetricRow("c", qtd = 2.0, valor = 10.0, extra = 9.0),
            "d" to ProductBiMetricRow("d", qtd = 99.0, valor = 1.0),
        )
        assertEquals(listOf("b", "c", "a", "d"), map.productBiMetricRows().map { it.name })
    }

    /** `metricRows(map, limit)` corta em 12 por padrão. */
    @Test
    fun `metricRows corta em 12 por padrao`() {
        val map = (1..20).associate { index ->
            "p$index" to ProductBiMetricRow("p$index", valor = index.toDouble())
        }.toMutableMap()
        assertEquals(12, map.productBiMetricRows().size)
        assertEquals(14, map.productBiMetricRows(14).size)
    }

    // ------------------------------------------------------------------
    // Recorte e filtro
    // ------------------------------------------------------------------

    /**
     * 375-378: com catálogo carregado, um pedido de produto de fora do recorte é descartado
     * mesmo estando aprovado.
     */
    @Test
    fun `pedido de produto fora do catalogo nao entra na receita`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(product("p1")),
                orders = listOf(
                    order("o1", "p1", total = 100.0),
                    order("o2", "outro", total = 999.0),
                ),
            ),
        )
        assertEquals(100.0, analytics.revenue, 0.0)
        assertEquals(1, analytics.approvedOrderCount)
    }

    /**
     * 376: `selectedProductIds.size > 0` — com catálogo **vazio** o primeiro teste é pulado e o
     * pedido só precisa passar no filtro de produto. É o que segura o BI de um escopo cujo
     * `produtos` não veio, mas cujos pedidos vieram.
     */
    @Test
    fun `catalogo vazio deixa o pedido passar`() {
        val analytics = buildProductBiAnalytics(
            dataset(orders = listOf(order("o1", "p1", total = 50.0))),
        )
        assertEquals(50.0, analytics.revenue, 0.0)
    }

    /** `productFilter` (373-378) estreita catálogo e pedidos ao mesmo tempo. */
    @Test
    fun `filtro de produto estreita catalogo e pedidos`() {
        val data = dataset(
            products = listOf(product("p1", stock = 10.0), product("p2", stock = 4.0)),
            orders = listOf(order("o1", "p1", total = 30.0), order("o2", "p2", total = 70.0)),
        )
        val filtered = buildProductBiAnalytics(data, productFilter = "p2")
        assertEquals(70.0, filtered.revenue, 0.0)
        assertEquals(listOf("p2"), filtered.byProduct.map { it.name })
        assertEquals(100.0, buildProductBiAnalytics(data).revenue, 0.0)
    }

    /** 421: o nome do pedido tem prioridade sobre o nome do catálogo. */
    @Test
    fun `nome do pedido vence o nome do catalogo`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(product("p1", name = "Nome antigo")),
                orders = listOf(order("o1", "p1", total = 10.0, productName = "Nome do pedido")),
            ),
        )
        assertEquals(listOf("Nome do pedido"), analytics.byProduct.map { it.name })
    }

    /** 435: variação só vira linha quando o pedido declara tamanho ou cor. */
    @Test
    fun `variacao so entra quando o pedido tem rotulo`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(product("p1", name = "Camisa")),
                orders = listOf(
                    order("o1", "p1", total = 10.0, variantLabel = "Tamanho M"),
                    order("o2", "p1", total = 10.0),
                ),
            ),
        )
        assertEquals(listOf("Camisa • Tamanho M"), analytics.byVariant.map { it.name })
    }

    /** 426: `data.userTurma` vence o mapa de usuários, que vence "Sem turma". */
    @Test
    fun `turma sai do pedido antes do mapa de usuarios`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(product("p1")),
                orders = listOf(
                    order("o1", "p1", total = 10.0, userId = "u1", turmaFromData = "ENG 22"),
                    order("o2", "p1", total = 10.0, userId = "u2", buyerKey = "b2"),
                    order("o3", "p1", total = 10.0, userId = "u3", buyerKey = "b3"),
                ),
                users = listOf(ProductBiUser("u2", "MED 21")),
            ),
        )
        assertEquals(setOf("ENG 22", "MED 21", "Sem turma"), analytics.byClass.map { it.name }.toSet())
    }

    /**
     * 438 + 533: pedido sem data cai em "Sem data" no web, e o gráfico só percorre os sete
     * dias — a linha some. O eixo continua com os sete dias, mesmo zerados.
     */
    @Test
    fun `pedido sem data nao aparece no grafico de dias`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(product("p1")),
                orders = listOf(
                    order("o1", "p1", total = 10.0, weekdayIndex = 2),
                    order("o2", "p1", total = 90.0, weekdayIndex = null),
                ),
            ),
        )
        assertEquals(ProductBiWeekdays, analytics.byWeekday.map { it.name })
        assertEquals(10.0, analytics.byWeekday[2].valor, 0.0)
        assertEquals(10.0, analytics.byWeekday.sumOf { it.valor }, 0.0)
    }

    // ------------------------------------------------------------------
    // Indicadores
    // ------------------------------------------------------------------

    /**
     * 516-520: `clickConversion` divide os **pedidos do acumulador por produto** pelos cliques,
     * não `approvedOrders.length`. Um pedido de produto fora do catálogo não conta em cima.
     */
    @Test
    fun `conversao de clique usa o acumulador por produto`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(product("p1", clicks = 10.0), product("p2", clicks = 10.0)),
                orders = listOf(order("o1", "p1", total = 10.0), order("o2", "p1", total = 10.0)),
            ),
        )
        // 2 pedidos / 20 cliques.
        assertEquals(10.0, analytics.clickConversion, 0.0001)
    }

    /** 521-525: `sellThrough` = vendidos / (vendidos + estoque), somando o catálogo inteiro. */
    @Test
    fun `venda do estoque soma vendidos do catalogo mais os pedidos`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(product("p1", stock = 6.0, sold = 2.0)),
                orders = listOf(order("o1", "p1", quantity = 2.0, total = 10.0)),
            ),
        )
        // `sold` sobe de 2 para 4 (443); base = 4 + 6 = 10.
        assertEquals(40.0, analytics.sellThrough, 0.0001)
    }

    /**
     * 473-482: parado é estoque positivo **com venda zero**, ou 5+ cliques sem nenhum pedido.
     * Um produto com 4 cliques e sem venda entra pela primeira condição; um com venda e 9
     * cliques sem pedido novo não entra.
     */
    @Test
    fun `produto parado tem as duas condicoes do web`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(
                    product("sem-venda", name = "Sem venda", stock = 5.0, clicks = 4.0),
                    product("clique-sem-compra", name = "Clique sem compra", stock = 5.0, sold = 3.0, clicks = 9.0),
                    product("vendido", name = "Vendido", stock = 5.0, sold = 3.0, clicks = 9.0),
                    product("sem-estoque", name = "Sem estoque", stock = 0.0, clicks = 20.0),
                ),
                orders = listOf(order("o1", "vendido", total = 10.0)),
            ),
        )
        assertEquals(
            setOf("Sem venda", "Clique sem compra"),
            analytics.stalledRows.map { it.name }.toSet(),
        )
        assertEquals(2, analytics.stalledCount)
    }

    /**
     * 484-487: a fatia "Com desconto" carrega `qtd` 1 ou 0 — é um sinalizador, não a contagem
     * de pedidos. "Sem desconto" desconta esse mesmo 1 do total de pedidos aprovados.
     */
    @Test
    fun `dependencia de desconto usa 1 como sinalizador`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(
                    product("promo", price = 50.0, oldPrice = 80.0),
                    product("cheio", price = 50.0, oldPrice = 50.0),
                ),
                orders = listOf(
                    order("o1", "promo", total = 50.0),
                    order("o2", "cheio", total = 50.0, buyerKey = "b2"),
                    order("o3", "cheio", total = 50.0, buyerKey = "b3"),
                ),
            ),
        )
        val comDesconto = analytics.discountRows.first { it.name == "Com desconto" }
        val semDesconto = analytics.discountRows.first { it.name == "Sem desconto" }
        assertEquals(1.0, comDesconto.qtd, 0.0)
        assertEquals(50.0, comDesconto.valor, 0.0)
        assertEquals(2.0, semDesconto.qtd, 0.0)
        assertEquals(100.0, semDesconto.valor, 0.0)
    }

    /** 487: linha zerada nos dois campos some da pizza. */
    @Test
    fun `desconto zerado nao vira fatia`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(product("cheio", price = 50.0, oldPrice = 50.0)),
                orders = listOf(order("o1", "cheio", total = 50.0)),
            ),
        )
        assertEquals(listOf("Sem desconto"), analytics.discountRows.map { it.name })
    }

    /** `hasDiscount` (428): só `precoAntigo > preco`; igual não conta. */
    @Test
    fun `desconto exige preco antigo maior`() {
        assertTrue(ProductBiProduct("p", price = 10.0, oldPrice = 12.0).hasDiscount)
        assertFalse(ProductBiProduct("p", price = 10.0, oldPrice = 10.0).hasDiscount)
        assertFalse(ProductBiProduct("p", price = 10.0, oldPrice = 0.0).hasDiscount)
    }

    /** 489-492 e 527: recompra conta **comprador**, não pedido. */
    @Test
    fun `recompra conta comprador e nao pedido`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(product("p1")),
                orders = listOf(
                    order("o1", "p1", total = 10.0, buyerKey = "ana"),
                    order("o2", "p1", total = 10.0, buyerKey = "ana"),
                    order("o3", "p1", total = 10.0, buyerKey = "bia"),
                ),
            ),
        )
        assertEquals(2, analytics.uniqueBuyers)
        assertEquals(1.0, analytics.recurrenceRows.first { it.name == "Primeira compra" }.qtd, 0.0)
        assertEquals(1.0, analytics.recurrenceRows.first { it.name == "Recompra" }.qtd, 0.0)
        assertEquals(50.0, analytics.repeatRate, 0.0001)
    }

    /**
     * 497-505: a faixa ABC sai do acumulado sobre a receita **inteira** do recorte, não sobre a
     * soma das 14 linhas de `byProduct`. Com 15 produtos de receita igual, o 15º fica fora do
     * corte e o acumulado das 14 primeiras para em 93,3% — nenhuma linha chega a "C".
     */
    @Test
    fun `curva ABC acumula sobre a receita inteira`() {
        val products = (1..15).map { product("p$it", name = "p$it") }
        val orders = (1..15).map { order("o$it", "p$it", total = 100.0, buyerKey = "b$it") }
        val analytics = buildProductBiAnalytics(dataset(products = products, orders = orders))

        assertEquals(14, analytics.byProduct.size)
        assertEquals(1500.0, analytics.revenue, 0.0)
        // 80% de 1500 = 1200 -> 12 linhas em A; 95% = 1425 -> as duas seguintes em B.
        val abc = analytics.abcRows.associate { it.name to it.qtd }
        assertEquals(12.0, abc.getValue("A"), 0.0)
        assertEquals(2.0, abc.getValue("B"), 0.0)
        assertFalse(abc.containsKey("C"))
    }

    /** 528: `topFiveDependency` é a receita das 5 primeiras sobre a receita total. */
    @Test
    fun `dependencia do top 5 usa a receita total`() {
        val products = (1..10).map { product("p$it", name = "p$it") }
        val orders = (1..10).map { order("o$it", "p$it", total = 100.0, buyerKey = "b$it") }
        val analytics = buildProductBiAnalytics(dataset(products = products, orders = orders))
        assertEquals(50.0, analytics.topFiveDependency, 0.0001)
    }

    /** `safeDivide` (113-115): sem pedido aprovado nada vira `NaN`. */
    @Test
    fun `recorte vazio devolve zero e nao NaN`() {
        val analytics = buildProductBiAnalytics(dataset(products = listOf(product("p1"))))
        assertEquals(0.0, analytics.revenue, 0.0)
        assertEquals(0.0, analytics.averageOrder, 0.0)
        assertEquals(0.0, analytics.clickConversion, 0.0)
        assertEquals(0.0, analytics.sellThrough, 0.0)
        assertEquals(0.0, analytics.repeatRate, 0.0)
        assertEquals(0.0, analytics.topFiveDependency, 0.0)
    }

    /** 450-458: o painel de engajamento ordena por cliques (`valor`), não por likes. */
    @Test
    fun `engajamento ordena por cliques`() {
        val analytics = buildProductBiAnalytics(
            dataset(
                products = listOf(
                    product("muitos-likes", name = "Muitos likes", likes = 90.0, clicks = 1.0),
                    product("muitos-cliques", name = "Muitos cliques", likes = 1.0, clicks = 90.0),
                ),
            ),
        )
        assertEquals(
            listOf("Muitos cliques", "Muitos likes"),
            analytics.engagementRows.map { it.name },
        )
    }
}
