package com.example.usc1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regras do M9 (Admin: Loja), portadas de:
 * - `web-reference/src/lib/upload.ts` (upload e guardas de custo);
 * - `web-reference/src/lib/imageCompression.ts` (plano de compressão);
 * - `web-reference/src/lib/storeService.ts` (`approveStoreOrder` 1109-1345 e
 *   `syncApprovedOrderVariantStock` 1017-1107);
 * - `web-reference/src/app/admin/loja/produtos/page.tsx` (planos e recebedores);
 * - `web-reference/src/lib/paymentRecipients.ts`.
 */
class AdminStoreM9RulesTest {

    // ---------------------------------------------------------------- upload.ts

    @Test
    fun `recusa formato fora de jpg png webp`() {
        // upload.ts 72-74
        assertEquals(
            "Formato inválido. Use JPG, PNG ou WEBP.",
            StoreImageUpload.validateImageFile(1_000, "image/gif"),
        )
        assertNull(StoreImageUpload.validateImageFile(1_000, "image/png"))
        assertNull(StoreImageUpload.validateImageFile(1_000, "image/webp"))
    }

    @Test
    fun `mensagem de tamanho muda de MB para KB abaixo de um mega`() {
        // upload.ts 75-82: o teto de 200KB da compressão cai no ramo de KB.
        assertEquals(
            "A imagem excede 2MB.",
            StoreImageUpload.validateImageFile(3L * 1024 * 1024, "image/jpeg"),
        )
        assertEquals(
            "A imagem excede 200KB.",
            StoreImageUpload.validateImageFile(
                sizeBytes = 300L * 1024,
                mimeType = "image/jpeg",
                maxBytes = StoreImageUpload.DefaultCompressedUploadMaxBytes,
            ),
        )
    }

    @Test
    fun `recusa resolucao acima de 2400 e area acima do teto`() {
        // upload.ts 215-220
        assertEquals(
            "Resolucao maxima: 2400x2400.",
            StoreImageUpload.validateImageDimensions(2401, 100),
        )
        assertNull(StoreImageUpload.validateImageDimensions(2400, 2400))
        assertEquals(
            "Imagem muito grande. Reduza a resolucao.",
            StoreImageUpload.validateImageDimensions(
                width = 2000,
                height = 2000,
                maxPixels = 1_000_000,
            ),
        )
    }

    @Test
    fun `segmento vazio do caminho vira file, nao some`() {
        // upload.ts 87-99: o `|| "file"` vale para a cadeia inteira, então o `filter(Boolean)`
        // seguinte nunca corta nada — `a//b` tem três segmentos, não dois.
        assertEquals("file", StoreImageUpload.sanitizeStoragePathSegment("///"))
        assertEquals("a/file/b", StoreImageUpload.normalizeStoragePath("a//b"))
        assertEquals("store/tenant_1/produtos", StoreImageUpload.normalizeStoragePath("store/Tenant 1/produtos"))
    }

    @Test
    fun `nome de saida so ganha extensao quando o hint nao tem uma`() {
        // upload.ts 150-161
        assertEquals(
            "produto.jpg",
            StoreImageUpload.resolveOutputFileName("foto.jpg", "image/jpeg", "produto", 1_000L),
        )
        assertEquals(
            "cover.png",
            StoreImageUpload.resolveOutputFileName("foto.png", "image/png", "cover.png", 1_000L),
        )
        assertEquals(
            "1000-foto.jpg",
            StoreImageUpload.resolveOutputFileName("foto.jpg", "image/jpeg", null, 1_000L),
        )
    }

    @Test
    fun `token de versao usa base 36 de tamanho e data`() {
        // upload.ts 110-126
        assertEquals("rs-sc", StoreImageUpload.buildFileMetadataVersionToken(1_000, 1_020))
        assertEquals("0", StoreImageUpload.buildFileMetadataVersionToken(0, 0))
        assertEquals(null, StoreImageUpload.buildFileMetadataVersionToken(-1, 0))
    }

    @Test
    fun `query de versao respeita url que ja tem interrogacao`() {
        // upload.ts 128-139
        assertEquals("https://x/a.webp?v=abc", StoreImageUpload.appendAssetVersionQuery("https://x/a.webp", "abc"))
        assertEquals("https://x/a.webp?t=1&v=abc", StoreImageUpload.appendAssetVersionQuery("https://x/a.webp?t=1", "abc"))
        assertEquals("https://x/a.webp", StoreImageUpload.appendAssetVersionQuery("https://x/a.webp", ""))
        assertNull(StoreImageUpload.appendAssetVersionQuery("", "abc"))
    }

    @Test
    fun `guarda bloqueia upload em andamento, intervalo curto, teto por minuto e repetido`() {
        // upload.ts 248-286
        val guard = StoreUploadGuard()
        val scope = "store:product:t1:draft"
        // `Date.now()` do web nunca é 0; com base zero o `?? 0` de 266 faria o primeiro envio
        // cair no intervalo mínimo. O relógio real do app parte de um instante grande.
        val base = 1_700_000_000_000L

        assertNull(guard.reserve(scope, "fp-1", base))
        assertEquals("Upload ja em andamento. Aguarde terminar.", guard.reserve(scope, "fp-2", base + 100))
        guard.release(scope)

        // 267-269: menos de 1200ms desde o último.
        assertEquals(
            "Aguarde alguns segundos antes de novo upload.",
            guard.reserve(scope, "fp-2", base + 500),
        )

        var now = base + 2_000
        repeat(5) {
            assertNull(guard.reserve(scope, "fp-$it-a", now))
            guard.release(scope)
            now += 1_500L
        }
        // 271-274: com 6 envios na janela de 60s, o próximo é recusado.
        assertEquals("Limite de uploads por minuto atingido.", guard.reserve(scope, "fp-x", now))
    }

    @Test
    fun `dedupe so vale depois do upload aceito pelo storage`() {
        // upload.ts 276-281 e 395-399: reservar não registra a impressão digital.
        val guard = StoreUploadGuard()
        val scope = "store:category:t1:capa"
        val base = 1_700_000_000_000L

        assertNull(guard.reserve(scope, "mesmo-arquivo", base))
        guard.release(scope)
        // Sem `registerSuccess`, o mesmo arquivo não é considerado repetido.
        assertNull(guard.reserve(scope, "mesmo-arquivo", base + 2_000))
        guard.registerSuccess(scope, "mesmo-arquivo", base + 2_000)
        guard.release(scope)

        assertEquals(
            "Arquivo repetido detectado. Evite uploads duplicados.",
            guard.reserve(scope, "mesmo-arquivo", base + 4_000),
        )
        // 45s depois já não é repetido.
        assertNull(guard.reserve(scope, "mesmo-arquivo", base + 50_000))
    }

    // ------------------------------------------------------- imageCompression.ts

    @Test
    fun `plano de compressao encolhe para caber em 1600 e nunca aumenta`() {
        // imageCompression.ts 66-68: a escala trava em 1, então imagem pequena não é ampliada.
        assertEquals(1600 to 900, StoreImageCompressionPlan.initialSize(3200, 1800, 1600, 1600))
        assertEquals(800 to 600, StoreImageCompressionPlan.initialSize(800, 600, 1600, 1600))
    }

    @Test
    fun `varredura de qualidade para em 50, nao chega ao minimo de 45`() {
        // imageCompression.ts 83-101: partindo de 0,82 e descendo 0,08, o passo seguinte a 0,50
        // é 0,42, que já é menor que o mínimo de 0,45 — então 0,45 nunca é tentado.
        assertEquals(listOf(82, 74, 66, 58, 50), StoreImageCompressionPlan.qualitySteps())
    }

    @Test
    fun `encolhimento tem piso de 320 e para quando chega nele`() {
        // imageCompression.ts 107-112
        assertEquals(850 to 425, StoreImageCompressionPlan.shrink(1000, 500))
        assertEquals(340 to 320, StoreImageCompressionPlan.shrink(400, 340))
        assertNull(StoreImageCompressionPlan.shrink(320, 800))
    }

    @Test
    fun `arquivo comprimido sai sempre como webp`() {
        // imageCompression.ts 45-52
        assertEquals("foto.webp", StoreImageCompressionPlan.toWebpFileName("foto.JPG"))
        assertEquals("minha_foto.webp", StoreImageCompressionPlan.toWebpFileName("minha foto.png"))
        assertEquals("image.webp", StoreImageCompressionPlan.toWebpFileName(".png"))
    }

    @Test
    fun `alvo estavel usa upsert e versao, rascunho usa nome unico`() {
        // `produtos/page.tsx` 737-749 e 774-786
        val stable = StoreUploadTargets.categoryCover("tenant-1", "Camisetas", 1_000L)
        assertEquals("store/tenant-1/categorias/camisetas", stable.path)
        assertEquals("cover", stable.options.fileName)
        assertTrue(stable.options.upsert)
        assertEquals(StoreUploadVersionStrategy.FileMetadata, stable.options.versionStrategy)

        val draft = StoreUploadTargets.productImage("tenant-1", "", 1_000L)
        assertEquals("store/tenant-1/produtos/drafts", draft.path)
        assertEquals("produto-1000", draft.options.fileName)
        assertFalse(draft.options.upsert)
        assertEquals(StoreUploadVersionStrategy.None, draft.options.versionStrategy)
        assertEquals("store:product:tenant-1:draft", draft.options.scopeKey)
    }

    // ---------------------------------------------------------- approveStoreOrder

    @Test
    fun `total zerado cai para o preco, como o operador ou do javascript`() {
        // `AdminStoreOrdersStatusPage.tsx` 376: `Number(row.total || row.price || 0)`.
        assertEquals(30.0, StoreOrderApproval.approvalPrice(total = 0.0, price = 30.0), 0.0001)
        assertEquals(50.0, StoreOrderApproval.approvalPrice(total = 50.0, price = 30.0), 0.0001)
        assertEquals(30.0, StoreOrderApproval.approvalPrice(total = null, price = 30.0), 0.0001)
        assertEquals(0.0, StoreOrderApproval.approvalPrice(total = null, price = null), 0.0001)
    }

    @Test
    fun `xp e o valor pago vezes dez, truncado`() {
        // storeService.ts 1147
        assertEquals(199L, StoreOrderApproval.xpGain(19.99))
        assertEquals(0L, StoreOrderApproval.xpGain(-5.0))
    }

    @Test
    fun `quantidade tem piso de um e cai de quantidade para itens`() {
        // storeService.ts 1148-1153
        assertEquals(3, StoreOrderApproval.quantity(quantidade = 3.0, itens = 9.0))
        assertEquals(9, StoreOrderApproval.quantity(quantidade = null, itens = 9.0))
        assertEquals(1, StoreOrderApproval.quantity(quantidade = 0.0, itens = null))
        assertEquals(2, StoreOrderApproval.quantity(quantidade = 2.7, itens = null))
    }

    @Test
    fun `fichas ja gravadas sao cortadas na quantidade do pedido`() {
        // storeService.ts 1274-1284: o corte é destrutivo — 3 fichas com quantidade 1 sai com 1.
        val existing = listOf(
            StoreVoucherEntry(id = "a", label = "Ficha A", status = "INATIVO"),
            StoreVoucherEntry(id = "b", label = "Ficha B"),
            StoreVoucherEntry(id = "c", label = "Ficha C"),
        )
        val result = StoreOrderApproval.buildVoucherEntries(existing, quantity = 1)
        assertEquals(1, result.size)
        // 1282: só o literal "inativo" desativa, e a comparação é em minúsculas.
        assertEquals("inativo", result.first().status)
    }

    @Test
    fun `ficha sem id nem rotulo ganha o padrao pela posicao`() {
        // storeService.ts 1280-1281
        val result = StoreOrderApproval.buildVoucherEntries(
            existing = listOf(StoreVoucherEntry(), StoreVoucherEntry(voucherId = "v2")),
            quantity = 2,
        )
        assertEquals("item-1", result[0].id)
        assertEquals("Ficha 1", result[0].label)
        assertEquals("v2", result[1].id)
        // Status ausente vira "ativo", não "inativo".
        assertEquals("ativo", result[1].status)
    }

    @Test
    fun `pedido sem ficha gravada gera uma por unidade`() {
        // storeService.ts 1285-1293
        val result = StoreOrderApproval.buildVoucherEntries(emptyList(), quantity = 3)
        assertEquals(listOf("item-1", "item-2", "item-3"), result.map { it.id })
        assertEquals(listOf("Ficha 1", "Ficha 2", "Ficha 3"), result.map { it.label })
        assertTrue(result.all { it.status == "ativo" && it.usedAt.isEmpty() })
    }

    @Test
    fun `variacao casa por chave ou por rotulo, e sem casar nada nao grava`() {
        // storeService.ts 1060-1079
        val variants = listOf(
            StoreProductVariantStock(id = "v1", tamanho = "M", cor = "Preto", estoque = 5, vendidos = 1),
            StoreProductVariantStock(tamanho = "G", cor = "Azul", estoque = 2, vendidos = 0),
        )

        val byKey = StoreOrderApproval.applyVariantStock(variants, "V1", "", 2)
        assertNotNull(byKey)
        assertEquals(3, byKey!![0].estoque)
        assertEquals(3, byKey[0].vendidos)

        // 1064-1065: sem id explícito, a chave é `tamanho-cor-índice`; o rótulo também casa.
        val byLabel = StoreOrderApproval.applyVariantStock(variants, "inexistente", "Tamanho G • Cor Azul", 1)
        assertNotNull(byLabel)
        assertEquals(1, byLabel!![1].estoque)

        // 1079: nenhuma variação casou — o web não grava nada.
        assertNull(StoreOrderApproval.applyVariantStock(variants, "nao-existe", "", 1))
        // Sem id de variação no pedido não há o que casar.
        assertNull(StoreOrderApproval.applyVariantStock(variants, "  ", "", 1))
    }

    @Test
    fun `estoque do produto passa a ser a soma das variacoes`() {
        // storeService.ts 1081
        val variants = listOf(
            StoreProductVariantStock(estoque = 4),
            StoreProductVariantStock(estoque = 0),
            StoreProductVariantStock(estoque = 7),
        )
        assertEquals(11, StoreOrderApproval.sumVariantStock(variants))
    }

    @Test
    fun `marcador do web impede baixar o estoque da variacao duas vezes`() {
        // storeService.ts 1032
        assertTrue(StoreOrderApproval.hasVariantStockApplied("2026-08-02T10:00:00Z"))
        assertFalse(StoreOrderApproval.hasVariantStockApplied(" "))
        assertFalse(StoreOrderApproval.hasVariantStockApplied(null))
    }

    @Test
    fun `pedido ja aprovado nao e reaprovado`() {
        // Guarda **adicionada no Android**, sem equivalente no web: `approveStoreOrder` repete
        // estoque, XP e selo a cada clique. Ver StoreOrderApproval.shouldSkipApproval.
        assertTrue(StoreOrderApproval.shouldSkipApproval("approved"))
        assertTrue(StoreOrderApproval.shouldSkipApproval(" APPROVED "))
        assertFalse(StoreOrderApproval.shouldSkipApproval("pendente"))
        assertFalse(StoreOrderApproval.shouldSkipApproval(null))
    }

    @Test
    fun `falha parcial nao desfaz a aprovacao, mas e relatada`() {
        // storeService.ts 1191-1193 e 1330-1332: o web só faz `console.warn`.
        val ok = StoreOrderApproval.summarize(emptyList())
        assertTrue(ok.approved)
        assertFalse(ok.hasPartialFailure)
        assertNull(ok.partialFailureMessage())

        val partial = StoreOrderApproval.summarize(
            listOf(StoreApprovalStep.ProductStock, StoreApprovalStep.EventVoucher),
        )
        assertTrue(partial.approved)
        assertEquals(
            "Pedido aprovado, mas falhou ao atualizar: estoque do produto, fichas do modo vendas.",
            partial.partialFailureMessage(),
        )
    }

    // -------------------------------------------------- plano e recebedores

    @Test
    fun `preco ausente fica em branco e visibilidade ausente fica visivel`() {
        // `produtos/page.tsx` 262-285: os dois mapas têm fallback diferente.
        val rows = StoreProductPlanScope.buildRows(
            plans = listOf(StorePlanOption("p1", "Atleta"), StorePlanOption("p2", "Lenda")),
            planPrices = listOf(StorePlanPrice("p1", "Atleta", 25.0)),
            planVisibility = listOf(StorePlanVisibility("p2", "Lenda", visible = false)),
        )
        assertEquals("25", rows[0].price)
        assertTrue(rows[0].visible)
        assertEquals("", rows[1].price)
        assertFalse(rows[1].visible)
    }

    @Test
    fun `chave do plano usa o id e cai para o nome`() {
        // `produtos/page.tsx` 264 e 278
        assertEquals("p1", StoreProductPlanScope.planKey("P1", "Atleta"))
        assertEquals("atleta", StoreProductPlanScope.planKey("  ", "Atleta"))
        assertEquals("", StoreProductPlanScope.planKey(null, null))
    }

    @Test
    fun `so linha com preco entra em plan_prices, mas visibilidade grava todas`() {
        // `produtos/page.tsx` 877-889
        val rows = listOf(
            StorePlanScopeRow("p1", "Atleta", price = "19,90", visible = true),
            StorePlanScopeRow("p2", "Lenda", price = "", visible = false),
            StorePlanScopeRow("p3", "Bicho", price = "abc", visible = true),
        )
        val prices = StoreProductPlanScope.toPlanPrices(rows)
        // 882: a vírgula do usuário vira ponto antes do parse.
        assertEquals(listOf("p1"), prices.map { it.planId })
        assertEquals(19.90, prices.first().price!!, 0.0001)

        assertEquals(listOf("p1", "p2", "p3"), StoreProductPlanScope.toPlanVisibility(rows).map { it.planId })
    }

    @Test
    fun `recebedor vazio some e os demais ganham os literais do web`() {
        // paymentRecipients.ts 84-92
        assertNull(StorePaymentRecipients.normalize("", "", "", "", ""))
        val normalized = StorePaymentRecipients.normalize("u1", "", "", "", "")
        assertEquals("Usuário", normalized?.name)
        assertEquals("Sem turma", normalized?.turma)
        assertEquals(StorePaymentRecipients.DefaultAvatarUrl, normalized?.avatarUrl)
    }

    @Test
    fun `selecao vazia de recebedor devolve lista vazia, nao a lista inteira`() {
        // paymentRecipients.ts 111
        val recipients = listOf(
            StorePaymentRecipient("u1", "A", "T1", "", ""),
            StorePaymentRecipient("u2", "B", "T2", "", ""),
        )
        assertTrue(StorePaymentRecipients.filterByIds(recipients, emptyList()).isEmpty())
        assertTrue(StorePaymentRecipients.filterByIds(recipients, listOf("  ")).isEmpty())
        assertEquals(listOf("u2"), StorePaymentRecipients.filterByIds(recipients, listOf("u2")).map { it.userId })
    }

    @Test
    fun `dedupe usa o id e cai para nome e telefone`() {
        // paymentRecipients.ts 224-227
        val result = StorePaymentRecipients.dedupe(
            listOf(
                StorePaymentRecipient("u1", "A", "T", "9", ""),
                StorePaymentRecipient("u1", "A repetido", "T", "9", ""),
                StorePaymentRecipient("", "B", "T", "8", ""),
                StorePaymentRecipient("", "B", "T", "8", ""),
            ),
        )
        assertEquals(2, result.size)
        assertEquals("A", result[0].name)
    }

    @Test
    fun `documento de recebedores so ganha prefixo quando o dono nao e o tenant`() {
        // paymentRecipients.ts 44-47
        assertEquals(
            "product_payment_receivers",
            StorePaymentRecipients.resolveDocId(StorePaymentRecipients.ProductsDocId, "tenant", "abc"),
        )
        assertEquals(
            "product_payment_receivers",
            StorePaymentRecipients.resolveDocId(StorePaymentRecipients.ProductsDocId, "league", ""),
        )
        assertEquals(
            "league:l1:product_payment_receivers",
            StorePaymentRecipients.resolveDocId(StorePaymentRecipients.ProductsDocId, "League", "l1"),
        )
    }

    @Test
    fun `payment_config nasce so com pagamento, recebedor ou whatsapp`() {
        // `produtos/page.tsx` 859-862 e 890-905
        assertFalse(StorePaymentRecipients.hasPaymentConfig(false, emptyList(), "  "))
        assertTrue(StorePaymentRecipients.hasPaymentConfig(true, emptyList(), ""))
        assertTrue(StorePaymentRecipients.hasPaymentConfig(false, emptyList(), "11999999999"))
        assertTrue(
            StorePaymentRecipients.hasPaymentConfig(
                paymentEnabled = false,
                selectedRecipients = listOf(StorePaymentRecipient("u1", "A", "T", "", "")),
                whatsapp = "",
            ),
        )
    }
}
