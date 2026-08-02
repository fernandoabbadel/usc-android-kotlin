package com.example.usc1.domain.model

/**
 * Regras da aprovação de pedido da loja, portadas de
 * `web-reference/src/lib/storeService.ts` — `approveStoreOrder` (1109-1345) e
 * `syncApprovedOrderVariantStock` (1017-1107).
 *
 * O web tenta uma callable e cai num fallback Supabase-direto completo (1128-1254); o bloco de
 * evento/voucher (1257-1332) e o de estoque de variação (1334-1342) rodam **sempre no cliente**,
 * fora da callable. O Android executa o caminho direto, com a mesma sessão autenticada e sem
 * `service_role` — nenhuma policy nova foi necessária (ver `docs/ANDROID_PROGRESS.md`, M9).
 */
object StoreOrderApproval {

    /** storeService.ts 1147: o XP sai do valor pago, truncado. */
    fun xpGain(price: Double): Long = Math.floor(maxOf(0.0, price) * 10.0).toLong()

    /**
     * `AdminStoreOrdersStatusPage.tsx` 376: `Number(row.total || row.price || 0)`. O `||` do JS
     * trata `0` como ausente, então um pedido com `total` zerado cai para `price` — o `?:` do
     * Kotlin não faria isso sozinho.
     */
    fun approvalPrice(total: Double?, price: Double?): Double {
        val cleanTotal = total ?: 0.0
        if (cleanTotal != 0.0) return cleanTotal
        val cleanPrice = price ?: 0.0
        if (cleanPrice != 0.0) return cleanPrice
        return 0.0
    }

    /** storeService.ts 1148-1153 e 1040-1043. */
    fun quantity(quantidade: Double?, itens: Double?): Int {
        val raw = quantidade ?: itens ?: 1.0
        val floored = if (raw.isFinite()) Math.floor(raw).toInt() else 1
        return maxOf(1, floored)
    }

    /** storeService.ts 94-100. */
    fun buildVariantKey(variantId: String, size: String, color: String, index: Int): String {
        val explicitId = variantId.trim()
        if (explicitId.isNotEmpty()) return explicitId
        val cleanSize = size.trim().ifEmpty { "sem-tamanho" }
        val cleanColor = color.trim().ifEmpty { "sem-cor" }
        return "$cleanSize-$cleanColor-$index"
    }

    /** storeService.ts 102-109. */
    fun buildVariantLabel(size: String, color: String): String = listOf(
        size.trim().takeIf { it.isNotEmpty() }?.let { "Tamanho $it" }.orEmpty(),
        color.trim().takeIf { it.isNotEmpty() }?.let { "Cor $it" }.orEmpty(),
    ).filter { it.isNotEmpty() }.joinToString(" • ")

    /**
     * storeService.ts 1271-1293.
     *
     * Com entradas já gravadas no pedido, o web **corta** em `quantity` e normaliza cada uma;
     * sem entradas, gera `quantity` fichas novas. Um pedido com 3 vouchers gravados e quantidade
     * 1 sai com 1 voucher — o corte é destrutivo, e é o comportamento do web.
     */
    fun buildVoucherEntries(
        existing: List<StoreVoucherEntry>,
        quantity: Int,
    ): List<StoreVoucherEntry> {
        if (existing.isNotEmpty()) {
            return existing.take(quantity).mapIndexed { index, entry ->
                entry.copy(
                    id = entry.id.trim().ifEmpty { entry.voucherId.trim() }.ifEmpty { "item-${index + 1}" },
                    label = entry.label.trim().ifEmpty { "Ficha ${index + 1}" },
                    // 1282: só o literal "inativo" desativa; qualquer outro texto vira "ativo".
                    status = if (entry.status.trim().lowercase() == "inativo") "inativo" else "ativo",
                )
            }
        }
        return (1..quantity).map { index ->
            StoreVoucherEntry(
                id = "item-$index",
                label = "Ficha $index",
                status = "ativo",
                usedAt = "",
                usedByUserId = "",
                usedByUserName = "",
                usedMethod = "",
            )
        }
    }

    /**
     * storeService.ts 1060-1079. Casa a variação do pedido com a do produto por chave **ou** por
     * rótulo, e devolve `null` quando nenhuma casa — nesse caso o web não grava nada.
     */
    fun applyVariantStock(
        variants: List<StoreProductVariantStock>,
        variantId: String,
        variantLabel: String,
        quantity: Int,
    ): List<StoreProductVariantStock>? {
        val normalizedVariantId = variantId.trim().lowercase()
        if (normalizedVariantId.isEmpty()) return null
        val normalizedLabel = variantLabel.trim().lowercase()
        var matched = false

        val next = variants.mapIndexed { index, variant ->
            val key = buildVariantKey(variant.id, variant.tamanho, variant.cor, index).lowercase()
            val label = buildVariantLabel(variant.tamanho, variant.cor).lowercase()
            val matches = key == normalizedVariantId ||
                (normalizedLabel.isNotEmpty() && label == normalizedLabel)
            if (!matches) {
                variant
            } else {
                matched = true
                variant.copy(
                    estoque = maxOf(0, variant.estoque - quantity),
                    vendidos = variant.vendidos + quantity,
                )
            }
        }
        return if (matched) next else null
    }

    /** storeService.ts 1081: o estoque do produto passa a ser a soma das variações. */
    fun sumVariantStock(variants: List<StoreProductVariantStock>): Int =
        variants.sumOf { maxOf(0, it.estoque) }

    /**
     * storeService.ts 1032: `variantStockAppliedAt` é o marcador de idempotência que o próprio
     * web mantém — reaprovar um pedido não baixa o estoque da variação duas vezes.
     */
    fun hasVariantStockApplied(variantStockAppliedAt: String?): Boolean =
        !variantStockAppliedAt?.trim().isNullOrEmpty()

    /**
     * Guarda de aprovação duplicada.
     *
     * **Isto não existe no web**: `approveStoreOrder` reescreve `status="approved"` e repete a
     * baixa de estoque, o ganho de XP, o selo e a notificação a cada clique. O comportamento foi
     * adicionado no Android por decisão explícita do usuário no M9, porque o cliente não tem
     * transação e a repetição corrompe estoque e XP de forma silenciosa. A divergência está
     * declarada em `docs/PARITY_MATRIX.md`.
     */
    fun shouldSkipApproval(currentStatus: String?): Boolean =
        currentStatus?.trim()?.lowercase() == AdminStoreOrderStatus.Approved.remoteValue

    /**
     * Resultado parcial da aprovação. O web engole cada falha secundária com `console.warn`
     * (1191-1193, 1231-1233, 1248-1250, 1330-1332, 1340-1342) e considera o pedido aprovado; o
     * Android faz o mesmo, mas **relata** o que falhou em vez de silenciar.
     */
    fun summarize(failures: List<StoreApprovalStep>): StoreApprovalOutcome = when {
        failures.isEmpty() -> StoreApprovalOutcome(approved = true, failures = emptyList())
        else -> StoreApprovalOutcome(approved = true, failures = failures)
    }
}

/** Etapas secundárias da aprovação, na ordem em que o web as executa. */
enum class StoreApprovalStep(val label: String) {
    ProductStock("estoque do produto"),
    UserRewards("XP e selos do comprador"),
    Notification("notificação do comprador"),
    EventVoucher("fichas do modo vendas"),
    VariantStock("estoque da variação"),
}

data class StoreApprovalOutcome(
    val approved: Boolean,
    val failures: List<StoreApprovalStep>,
) {
    val hasPartialFailure: Boolean get() = failures.isNotEmpty()

    /** Mensagem equivalente aos `console.warn` do web, agora visível para o admin. */
    fun partialFailureMessage(): String? {
        if (failures.isEmpty()) return null
        return "Pedido aprovado, mas falhou ao atualizar: " +
            failures.joinToString(", ") { it.label } + "."
    }
}

data class StoreVoucherEntry(
    val id: String = "",
    val voucherId: String = "",
    val label: String = "",
    val status: String = "ativo",
    val usedAt: String = "",
    val usedByUserId: String = "",
    val usedByUserName: String = "",
    val usedMethod: String = "",
)

data class StoreProductVariantStock(
    val id: String = "",
    val tamanho: String = "",
    val cor: String = "",
    val estoque: Int = 0,
    val vendidos: Int = 0,
)
