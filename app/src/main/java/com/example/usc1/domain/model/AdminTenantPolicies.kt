package com.example.usc1.domain.model

data class AdminTenantPoliciesBundle(
    val tenantId: String,
    val tenantName: String,
    val tenantSlug: String,
    val policies: List<TenantPolicyDocument>,
)

data class TenantPolicyDocument(
    val id: String? = null,
    val tenantId: String? = null,
    val module: String,
    val title: String,
    val description: String,
    val placeholder: String,
    val content: String,
    val visible: Boolean,
    val updatedAt: String? = null,
)

object TenantPolicyCatalog {
    val templates = listOf(
        template(
            module = "reembolso_cancelamento",
            title = "Reembolso e cancelamento",
            description = "Evento cancelado, evento adiado, chargeback, pedido aprovado por erro e regras gerais.",
            placeholder = "Ex.: em caso de cancelamento do evento, os compradores serão informados pelos canais oficiais. Descreva prazo, forma de reembolso, taxas não reembolsáveis quando aplicável e responsável pelo atendimento.",
        ),
        template(
            module = "eventos",
            title = "Eventos, ingressos e check-in",
            description = "Transferência de ingresso, QR Code, entrada, lote, consumíveis, pulseira e validação.",
            placeholder = "Ex.: o ingresso é pessoal, pode ser transferido até determinada data e exige QR Code válido no check-in. Descreva regras para ficha/consumível já retirado.",
        ),
        template(
            module = "bebidas_alcoolicas",
            title = "Eventos com bebida alcoólica",
            description = "Idade mínima, documento oficial, consumo responsável e responsabilidade do organizador.",
            placeholder = "Ex.: quando houver bebida alcoólica, a entrada/consumo será restrita a maiores de 18 anos mediante documento oficial com foto.",
        ),
        template(
            module = "menores_de_idade",
            title = "Menores de idade",
            description = "Compra de evento/produto por menores, autorização responsável e bloqueios sociais.",
            placeholder = "Ex.: menores de 18 anos dependem de autorização responsável quando aplicável e podem ter recursos sociais limitados conforme a política da USC e do organizador.",
        ),
        template(
            module = "loja",
            title = "Loja, produtos, retirada e entrega",
            description = "Produto esgotado, produto personalizado, retirada, entrega e troca.",
            placeholder = "Ex.: produtos personalizados podem ter regras específicas de troca. Informe prazos de retirada, entrega e contato de suporte.",
        ),
        template(
            module = "planos",
            title = "Planos de sócio e benefícios",
            description = "Cancelamento, benefício já usado, período de cobrança e elegibilidade.",
            placeholder = "Ex.: benefícios já utilizados podem não gerar reembolso integral. Informe como solicitar cancelamento e prazos de resposta.",
        ),
        template(
            module = "mini_vendor",
            title = "Mini vendor e repasses",
            description = "Responsabilidade do vendedor, produtos permitidos, repasse e contestação.",
            placeholder = "Ex.: o vendedor é responsável por descrição, disponibilidade e entrega do produto. Descreva prazos de repasse e regras de contestação.",
        ),
        template(
            module = "checkout",
            title = "Checkout e pagamento",
            description = "Comprovante, aprovação manual, PIX, erro operacional e contestação.",
            placeholder = "Ex.: pagamentos manuais dependem de validação do comprovante. Descreva prazo de análise e o que acontece em caso de comprovante inválido.",
        ),
        template(
            module = "termos_tenant",
            title = "Termos do tenant/organizador",
            description = "Regras próprias da organização, uso de dados, sigilo e responsabilidade.",
            placeholder = "Ex.: descreva regras específicas da entidade, responsáveis oficiais, canais de suporte e obrigações dos administradores.",
        ),
    )

    val modules = templates.map { it.module }.toSet()

    fun mergeLoaded(loadedPolicies: List<TenantPolicyDocument>): List<TenantPolicyDocument> {
        val loadedByModule = loadedPolicies.associateBy { it.module }
        return templates.map { template ->
            val loaded = loadedByModule[template.module]
            template.copy(
                id = loaded?.id,
                tenantId = loaded?.tenantId,
                content = loaded?.content ?: template.content,
                visible = loaded?.visible ?: template.visible,
                updatedAt = loaded?.updatedAt,
            )
        }
    }

    fun sanitizeForSave(policy: TenantPolicyDocument): TenantPolicyDocument {
        val cleanContent = policy.content.trim().take(MaxContentLength)
        return policy.copy(
            title = policy.title.trim().take(MaxTitleLength),
            content = cleanContent,
            visible = policy.visible && cleanContent.isNotBlank(),
        )
    }

    private fun template(
        module: String,
        title: String,
        description: String,
        placeholder: String,
    ) = TenantPolicyDocument(
        module = module,
        title = title,
        description = description,
        placeholder = placeholder,
        content = "",
        visible = false,
    )

    const val MaxTitleLength = 160
    const val MaxContentLength = 12_000
}
