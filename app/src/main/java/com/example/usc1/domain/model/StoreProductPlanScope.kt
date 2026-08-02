package com.example.usc1.domain.model

/**
 * Preço e visibilidade por plano do produto, portados de
 * `web-reference/src/app/admin/loja/produtos/page.tsx` — `buildPlanScopeRows` (257-286) e o
 * payload de `plan_prices`/`plan_visibility` em `handleSaveProduct` (877-889).
 *
 * O catálogo de planos vem de `fetchPlanCatalog` (`plansPublicService.ts` 121-165), com
 * `tenant_id` e teto de 40 linhas.
 */
object StoreProductPlanScope {

    const val PlanCatalogMaxResults = 40

    /**
     * page.tsx 262-285. A chave de casamento é `planId || planName` em minúsculas — e o
     * **fallback é diferente nos dois mapas**: preço ausente vira string vazia (usa o preço geral
     * do produto), visibilidade ausente vira `true`.
     */
    fun buildRows(
        plans: List<StorePlanOption>,
        planPrices: List<StorePlanPrice>,
        planVisibility: List<StorePlanVisibility>,
    ): List<StorePlanScopeRow> {
        val priceMap = planPrices.associate { entry ->
            planKey(entry.planId, entry.planName) to entry.price
                ?.takeIf { it.isFinite() }
                ?.let { formatPriceInput(it) }
                .orEmpty()
        }
        // 273: `entry.visible !== false` — só o `false` explícito esconde.
        val visibilityMap = planVisibility.associate { entry ->
            planKey(entry.planId, entry.planName) to (entry.visible != false)
        }

        return plans.map { plan ->
            val key = planKey(plan.id, plan.nome)
            StorePlanScopeRow(
                planId = plan.id,
                planName = plan.nome,
                price = priceMap[key].orEmpty(),
                visible = visibilityMap[key] ?: true,
            )
        }
    }

    /** page.tsx 278: `(plan.id || plan.nome).trim().toLowerCase()`. */
    fun planKey(planId: String?, planName: String?): String {
        val id = planId?.trim().orEmpty()
        if (id.isNotEmpty()) return id.lowercase()
        return planName?.trim().orEmpty().lowercase()
    }

    /**
     * page.tsx 877-884. Só linha com preço preenchido entra em `plan_prices`, e a vírgula decimal
     * do usuário é convertida antes do parse. Preço inválido ou negativo é descartado.
     */
    fun toPlanPrices(rows: List<StorePlanScopeRow>): List<StorePlanPrice> = rows
        .filter { it.price.trim().isNotEmpty() }
        .map { row ->
            StorePlanPrice(
                planId = row.planId,
                planName = row.planName,
                price = row.price.replace(",", ".").toDoubleOrNull(),
            )
        }
        .filter { it.price != null && it.price.isFinite() && it.price >= 0.0 }

    /** page.tsx 885-889: visibilidade grava **todas** as linhas, inclusive as sem preço. */
    fun toPlanVisibility(rows: List<StorePlanScopeRow>): List<StorePlanVisibility> = rows.map { row ->
        StorePlanVisibility(planId = row.planId, planName = row.planName, visible = row.visible)
    }

    /** page.tsx 265-267: número finito vira texto; o resto vira campo vazio. */
    private fun formatPriceInput(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

data class StorePlanOption(
    val id: String,
    val nome: String,
)

data class StorePlanScopeRow(
    val planId: String,
    val planName: String,
    val price: String = "",
    val visible: Boolean = true,
)

data class StorePlanPrice(
    val planId: String,
    val planName: String,
    val price: Double?,
)

data class StorePlanVisibility(
    val planId: String,
    val planName: String,
    val visible: Boolean,
)

/**
 * Recebedores de pagamento do produto, portados de
 * `web-reference/src/lib/paymentRecipients.ts`.
 *
 * O documento vive em `app_config`, com id `tenant:{tenant}::product_payment_receivers` para o
 * escopo de produtos (25-29 e 39-57). O diretório de candidatos sai de
 * `fetchTenantMembershipDirectory` com `status=approved` e teto de 400 (148-152).
 */
object StorePaymentRecipients {

    const val DirectoryLimit = 400
    const val DefaultAvatarUrl = "https://github.com/shadcn.png"

    /** paymentRecipients.ts 25-29. */
    const val ProductsDocId = "product_payment_receivers"
    const val EventsDocId = "event_payment_receivers"
    const val TenantDocId = "payment_receivers"

    /**
     * paymentRecipients.ts 39-48. Dono que não é o tenant prefixa o documento; `ownerType`
     * vazio ou "tenant" usa o documento raiz.
     */
    fun resolveDocId(
        scopeDocId: String,
        ownerType: String?,
        ownerId: String?,
    ): String {
        val cleanOwnerType = ownerType?.trim()?.lowercase().orEmpty()
        val cleanOwnerId = ownerId?.trim().orEmpty()
        if (cleanOwnerId.isEmpty() || cleanOwnerType.isEmpty() || cleanOwnerType == "tenant") {
            return scopeDocId
        }
        return "$cleanOwnerType:$cleanOwnerId:$scopeDocId"
    }

    /**
     * paymentRecipients.ts 75-93. Linha totalmente vazia é descartada; o resto ganha os
     * literais de fallback do web.
     */
    fun normalize(
        userId: String?,
        name: String?,
        turma: String?,
        phone: String?,
        avatarUrl: String?,
    ): StorePaymentRecipient? {
        val cleanUserId = userId?.trim().orEmpty()
        val cleanName = name?.trim().orEmpty()
        val cleanTurma = turma?.trim().orEmpty()
        val cleanPhone = phone?.trim().orEmpty()
        val cleanAvatar = avatarUrl?.trim().orEmpty()

        if (cleanUserId.isEmpty() && cleanName.isEmpty() && cleanTurma.isEmpty() &&
            cleanPhone.isEmpty() && cleanAvatar.isEmpty()
        ) {
            return null
        }

        return StorePaymentRecipient(
            userId = cleanUserId,
            name = cleanName.ifEmpty { "Usuário" },
            turma = cleanTurma.ifEmpty { "Sem turma" },
            phone = cleanPhone,
            avatarUrl = cleanAvatar.ifEmpty { DefaultAvatarUrl },
        )
    }

    /** paymentRecipients.ts 104-113: seleção vazia devolve lista vazia, não a lista inteira. */
    fun filterByIds(
        recipients: List<StorePaymentRecipient>,
        userIds: List<String>,
    ): List<StorePaymentRecipient> {
        val selected = userIds.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        if (selected.isEmpty()) return emptyList()
        return recipients.filter { selected.contains(it.userId) }
    }

    /** paymentRecipients.ts 221-227: dedupe por `userId`, ou por `nome:telefone` quando não há id. */
    fun dedupe(recipients: List<StorePaymentRecipient>): List<StorePaymentRecipient> {
        val seen = mutableSetOf<String>()
        return recipients.filter { entry ->
            val key = entry.userId.ifEmpty { "${entry.name}:${entry.phone}" }
            seen.add(key)
        }
    }

    /**
     * `produtos/page.tsx` 859-905: o `payment_config` só é gravado quando há pagamento próprio,
     * recebedor escolhido ou WhatsApp. Sem nada disso, o campo vai a `null`.
     */
    fun hasPaymentConfig(
        paymentEnabled: Boolean,
        selectedRecipients: List<StorePaymentRecipient>,
        whatsapp: String,
    ): Boolean = paymentEnabled || selectedRecipients.isNotEmpty() || whatsapp.trim().isNotEmpty()
}

data class StorePaymentRecipient(
    val userId: String,
    val name: String,
    val turma: String,
    val phone: String,
    val avatarUrl: String,
)
