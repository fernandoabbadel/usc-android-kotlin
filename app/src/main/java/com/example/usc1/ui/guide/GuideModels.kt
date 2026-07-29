package com.example.usc1.ui.guide

enum class GuideCategory(
    val raw: String,
    val label: String,
    val shortLabel: String,
) {
    Academic("academico", "Acadêmico", "Acad"),
    Groups("grupos", "Grupos", "Grupo"),
    Transport("transporte", "Transporte", "Bus"),
    Tourism("turismo", "Turismo", "Local"),
    Emergency("emergencia", "Emergência", "SOS");

    companion object {
        fun from(raw: String?): GuideCategory {
            val clean = raw.orEmpty().trim().lowercase()
            return entries.firstOrNull { it.raw == clean } ?: Academic
        }
    }
}

data class GuideItem(
    val id: String,
    val category: GuideCategory,
    val order: Int,
    val title: String,
    val description: String,
    val badge: String,
    val url: String? = null,
    val schedule: String? = null,
    val detail: String? = null,
    val photoUrl: String? = null,
    val phone: String? = null,
    val color: String? = null,
)

data class GuideSection(
    val category: GuideCategory,
    val items: List<GuideItem>,
)

data class GuideUiState(
    val sections: List<GuideSection> = GuideMockData.sections,
    val faqItems: List<GuideItem> = GuideMockData.faqItems,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class LegalDocUiModel(
    val id: String,
    val title: String,
    val content: String,
    val iconName: String,
    val type: String,
    val updatedAtLabel: String,
)

data class LegalUiState(
    val docs: List<LegalDocUiModel> = GuideMockData.legalDocs,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val terms: List<GuideItem>
        get() = docs.map { doc ->
            GuideItem(
                id = doc.id,
                category = GuideCategory.Academic,
                order = 0,
                title = doc.title,
                description = doc.content.take(120).ifBlank { "Documento legal da plataforma USC." },
                badge = doc.type.ifBlank { "Legal" },
            )
        }
}

object GuideMockData {
    val sections = listOf(
        GuideSection(
            category = GuideCategory.Academic,
            items = listOf(
                GuideItem(
                    id = "primeiros-passos",
                    category = GuideCategory.Academic,
                    order = 1,
                    title = "Primeiros passos",
                    description = "Como usar carteirinha, eventos, loja e scanner visual.",
                    badge = "Guia",
                ),
                GuideItem(
                    id = "eventos",
                    category = GuideCategory.Academic,
                    order = 2,
                    title = "Eventos",
                    description = "Compras, ingressos, pedidos e validação por QR.",
                    badge = "FAQ",
                ),
            ),
        ),
        GuideSection(
            category = GuideCategory.Emergency,
            items = listOf(
                GuideItem(
                    id = "suporte-usc",
                    category = GuideCategory.Emergency,
                    order = 1,
                    title = "Suporte USC",
                    description = "Canais oficiais para atendimento e dúvidas.",
                    badge = "Contato",
                    phone = "USC",
                    color = "emerald",
                ),
            ),
        ),
    )

    val faqItems = listOf(
        GuideItem(
            id = "faq-pedido",
            category = GuideCategory.Academic,
            order = 1,
            title = "Como aprovar pedido?",
            description = "Pedidos ficam pendentes até aprovação da atlética.",
            badge = "Pedido",
        ),
        GuideItem(
            id = "faq-ingresso",
            category = GuideCategory.Academic,
            order = 2,
            title = "Posso transferir ingresso?",
            description = "A transferência aparece apenas quando o fluxo estiver liberado pela atlética.",
            badge = "Ticket",
        ),
        GuideItem(
            id = "faq-plano",
            category = GuideCategory.Academic,
            order = 3,
            title = "Meu plano está ativo?",
            description = "Confira Planos e Carteirinha para validar o status do sócio.",
            badge = "Plano",
        ),
    )

    val legalDocs = listOf(
        LegalDocUiModel(
            id = "termos-de-uso",
            title = "Termos de uso",
            content = "Regras gerais de acesso, conta e serviços digitais da plataforma USC.",
            iconName = "gavel",
            type = "Legal",
            updatedAtLabel = "Atualizado recentemente",
        ),
        LegalDocUiModel(
            id = "privacidade-lgpd",
            title = "Privacidade e LGPD",
            content = "Direitos do titular, consentimentos e solicitações relacionadas aos dados.",
            iconName = "lock",
            type = "LGPD",
            updatedAtLabel = "Atualizado recentemente",
        ),
        LegalDocUiModel(
            id = "pagamentos",
            title = "Pagamentos",
            content = "Regras de compras, planos, ingressos e conciliação financeira.",
            iconName = "receipt",
            type = "Segurança",
            updatedAtLabel = "Atualizado recentemente",
        ),
    )
}
