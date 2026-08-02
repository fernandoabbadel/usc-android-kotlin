package com.example.usc1.domain.model

import com.example.usc1.ui.bi.store.ProductBiLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras de escopo do BI Loja do tenant (M8.4), portadas de
 * `web-reference/src/app/admin/gestao/_components/AdminBiDashboard.tsx`, o `ProductsBi`
 * (1393-1424) — em especial as duas exclusões de vendedor das linhas 1399 e 1409.
 *
 * O motor em si é o mesmo do M8.3 e está coberto por [ProductBiM83RulesTest]: aqui só entra o
 * que muda entre os cinco players.
 */
class ProductBiM84RulesTest {

    private val tenantId = "tenant-1"

    // ------------------------------------------------------------------
    // Exclusão de vendedor (1396-1402)
    // ------------------------------------------------------------------

    /** 1399: `["mini_vendor","league","liga"].includes(sellerType)` recusa a linha. */
    @Test
    fun `produto de mini vendor liga e league fica fora do BI Loja do tenant`() {
        listOf("mini_vendor", "league", "liga").forEach { type ->
            assertFalse(type, productBiTenantOwnsProduct(type, tenantId, tenantId))
        }
        assertFalse(productBiTenantOwnsProduct("  MINI_VENDOR  ", "", tenantId))
    }

    /**
     * A lista do web tem `liga` **e** `league`, mas não tem `commission` nem `directory`:
     * comissão e diretório continuam entrando no BI Loja do tenant. É o comportamento do web,
     * não um esquecimento do port — a exclusão da comissão/diretório nunca existiu lá.
     */
    @Test
    fun `comissao e diretorio continuam no BI Loja do tenant`() {
        assertEquals(listOf("mini_vendor", "league", "liga"), ProductBiExcludedSellerTypes)
        assertTrue(productBiTenantOwnsProduct("commission", tenantId, tenantId))
        assertTrue(productBiTenantOwnsProduct("directory", tenantId, tenantId))
    }

    /** 1400: linha sem `seller_id` é aceita, seja qual for o tenant ativo. */
    @Test
    fun `produto sem seller_id entra`() {
        assertTrue(productBiTenantOwnsProduct("tenant", "", tenantId))
        assertTrue(productBiTenantOwnsProduct(null, null, tenantId))
        assertTrue(productBiTenantOwnsProduct("tenant", "   ", tenantId))
    }

    /** 1401: com `seller_id` preenchido, ele precisa ser o do tenant ativo. */
    @Test
    fun `produto de outro seller_id fica fora`() {
        assertFalse(productBiTenantOwnsProduct("tenant", "outro-tenant", tenantId))
        assertTrue(productBiTenantOwnsProduct("tenant", tenantId, tenantId))
    }

    /** 1401: `!cleanTenantId ||` — sem tenant resolvido, qualquer `seller_id` passa. */
    @Test
    fun `sem tenant ativo qualquer seller_id passa`() {
        assertTrue(productBiTenantOwnsProduct("tenant", "outro-tenant", ""))
        assertTrue(productBiTenantOwnsProduct("tenant", "outro-tenant", "   "))
    }

    // ------------------------------------------------------------------
    // Exclusão de pedido (1406-1413)
    // ------------------------------------------------------------------

    /**
     * 1408: pedido de um produto que **já** passou no recorte entra mesmo com `seller_type` de
     * mini vendor. Sem essa primeira linha, um pedido com vendedor desatualizado sumiria da
     * receita de um produto que o painel mostra.
     */
    @Test
    fun `pedido de produto do tenant entra mesmo marcado como de outro vendedor`() {
        assertTrue(
            productBiTenantOwnsOrder(
                sellerType = "mini_vendor",
                sellerId = "outro",
                productId = "p1",
                tenantProductIds = setOf("p1"),
                tenantId = tenantId,
            ),
        )
    }

    /** 1409-1412: fora dessa exceção o pedido responde à mesma regra do produto. */
    @Test
    fun `pedido de produto de fora segue a regra de vendedor`() {
        assertFalse(
            productBiTenantOwnsOrder("mini_vendor", "outro", "p9", setOf("p1"), tenantId),
        )
        assertTrue(
            productBiTenantOwnsOrder("tenant", "", "p9", setOf("p1"), tenantId),
        )
        assertFalse(
            productBiTenantOwnsOrder("tenant", "outro-tenant", "p9", setOf("p1"), tenantId),
        )
    }

    /** `productId` vazio não pode casar com um conjunto que contenha string vazia. */
    @Test
    fun `pedido sem productId nao casa por acaso`() {
        assertFalse(
            productBiTenantOwnsOrder("liga", "outro", "", setOf(""), tenantId),
        )
    }

    // ------------------------------------------------------------------
    // Escopo por player: cada um vê só o seu
    // ------------------------------------------------------------------

    /**
     * O motor é o mesmo nos cinco players; o que separa um do outro é o recorte que chega no
     * [ProductBiDataset]. Este teste fixa que dois escopos com o mesmo motor e catálogos
     * diferentes não se enxergam.
     */
    @Test
    fun `escopos diferentes nao compartilham receita`() {
        val liga = ProductBiDataset(
            scope = ProductBiScope.League,
            products = listOf(ProductBiProduct("liga-1", "Camisa da liga")),
            orders = listOf(
                ProductBiOrder("o1", productId = "liga-1", total = 100.0, status = "aprovado"),
            ),
        )
        val lojinha = ProductBiDataset(
            scope = ProductBiScope.MiniVendor,
            products = listOf(ProductBiProduct("mv-1", "Caneca da lojinha")),
            orders = listOf(
                ProductBiOrder("o2", productId = "mv-1", total = 40.0, status = "aprovado"),
            ),
        )

        assertEquals(100.0, buildProductBiAnalytics(liga).revenue, 0.0)
        assertEquals(40.0, buildProductBiAnalytics(lojinha).revenue, 0.0)
        assertEquals(listOf("Camisa da liga"), buildProductBiAnalytics(liga).byProduct.map { it.name })
        assertEquals(listOf("Caneca da lojinha"), buildProductBiAnalytics(lojinha).byProduct.map { it.name })
    }

    /** `productOptions` (363-369): dedup por id, sem linha de id vazio. */
    @Test
    fun `opcoes de produto deduplicam por id e descartam id vazio`() {
        val dataset = ProductBiDataset(
            products = listOf(
                ProductBiProduct("p1", "Camisa"),
                ProductBiProduct("p1", "Camisa repetida"),
                ProductBiProduct("", "Sem id"),
                ProductBiProduct("p2", "Caneca"),
            ),
        )
        assertEquals(listOf("p1", "p2"), dataset.productOptions.map { it.id })
        assertEquals("Camisa", dataset.productOptions.first().title)
    }

    // ------------------------------------------------------------------
    // Textos de cada player
    // ------------------------------------------------------------------

    /** `ProductsBi` (1417-1423): os três textos do BI Loja do tenant. */
    @Test
    fun `textos do tenant sao os do AdminBiDashboard`() {
        assertEquals("Produtos oficiais da loja", ProductBiLabels.Tenant.title)
        assertEquals("Todos os produtos oficiais", ProductBiLabels.Tenant.allLabel)
        assertTrue(ProductBiLabels.Tenant.subtitle.endsWith("apenas da loja oficial da atlética."))
    }

    /** `MiniVendorGestaoPage`: `title = profile.storeName || "Minha lojinha"`. */
    @Test
    fun `titulo da lojinha cai para Minha lojinha quando nao ha nome`() {
        assertEquals("Loja da Ana", ProductBiLabels.ofMiniVendor("Loja da Ana").title)
        assertEquals("Minha lojinha", ProductBiLabels.ofMiniVendor("   ").title)
        assertEquals(
            "Análises privadas da sua lojinha: sem comparar com atlética, ligas ou outros vendedores.",
            ProductBiLabels.ofMiniVendor("Loja da Ana").subtitle,
        )
        assertEquals("Todos os produtos da lojinha", ProductBiLabels.MiniVendor.allLabel)
    }

    /**
     * `LeagueFinanceDashboard` (769-777): `title` e `allLabel` interpolam a entidade, mas o
     * subtítulo é literal e termina em "apenas desta liga." também na comissão e no diretório.
     */
    @Test
    fun `subtitulo do coletivo diz desta liga em todos os tres`() {
        val comissao = ProductBiLabels.ofCollective("da", "comissão")
        val diretorio = ProductBiLabels.ofCollective("do", "diretório")
        assertEquals("Produtos da comissão", comissao.title)
        assertEquals("Todos os produtos do diretório", diretorio.allLabel)
        assertTrue(comissao.subtitle.endsWith("apenas desta liga."))
        assertTrue(diretorio.subtitle.endsWith("apenas desta liga."))
    }

    /** `ProductBiLabels.of` cobre os cinco players sem ramo perdido. */
    @Test
    fun `todos os cinco escopos tem rotulo`() {
        ProductBiScope.entries.forEach { scope ->
            val labels = ProductBiLabels.of(scope)
            assertTrue(scope.name, labels.title.isNotBlank())
            assertTrue(scope.name, labels.allLabel.isNotBlank())
        }
        assertEquals("mini_vendor", ProductBiScope.MiniVendor.remoteValue)
    }
}
