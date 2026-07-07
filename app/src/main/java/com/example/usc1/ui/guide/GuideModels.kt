package com.example.usc1.ui.guide

data class GuideItem(
    val title: String,
    val description: String,
    val badge: String,
)

data class GuideUiState(
    val guideItems: List<GuideItem> = listOf(
        GuideItem("Primeiros passos", "Como usar carteirinha, eventos, loja e scanner visual.", "Guia"),
        GuideItem("Eventos", "Compras, ingressos, pedidos e validação por QR.", "FAQ"),
        GuideItem("Suporte USC", "Canais mockados para atendimento e dúvidas.", "Contato"),
    ),
    val faqItems: List<GuideItem> = listOf(
        GuideItem("Como aprovar pedido?", "Pedidos ficam pendentes até aprovação da atlética.", "Pedido"),
        GuideItem("Posso transferir ingresso?", "Transferência aparece como fluxo futuro no app nativo.", "Ticket"),
        GuideItem("Meu plano está ativo?", "Confira Planos e Carteirinha para status mockado.", "Plano"),
    ),
)

data class LegalUiState(
    val terms: List<GuideItem> = listOf(
        GuideItem("Termos de uso", "Regras gerais de acesso, conta e serviços digitais.", "Legal"),
        GuideItem("Privacidade e LGPD", "Direitos do titular, consentimentos e solicitações.", "LGPD"),
        GuideItem("Pagamentos", "Pagamentos reais entram só após integração segura.", "Segurança"),
    ),
)
