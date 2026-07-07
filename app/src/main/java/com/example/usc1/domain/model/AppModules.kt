package com.example.usc1.domain.model

import com.example.usc1.core.roles.UserRole

object AppModules {
    val all = listOf(
        AppModule(
            key = AppModuleKey.Auth,
            title = "Autenticação",
            description = "Login, cadastro, convite, aprovação e segurança da sessão.",
            route = "auth",
            phase = AppModulePhase.EssentialV1,
            requiresAuthentication = false,
        ),
        AppModule(AppModuleKey.Dashboard, "Início", "Dashboard com atalhos, cards e status do usuário.", "dashboard", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Profile, "Perfil", "Dados pessoais, avatar, histórico, planos, pedidos e privacidade.", "profile", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Settings, "Configurações", "Pedidos, segurança, suporte, termos, convites e preferências.", "settings", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Orders, "Pedidos", "Histórico de pedidos de loja, eventos e planos.", "orders", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Store, "Loja", "Produtos, categorias, detalhe, pedido direto e avaliações.", "store", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Events, "Eventos", "Eventos, lotes, inscrição, ingressos e QR Code.", "events", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Plans, "Planos", "Catálogo, adesão, plano ativo e benefícios.", "plans", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.MembershipCard, "Carteirinha", "Carteirinha digital com status de membro e validação.", "membership-card", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Training, "Treinos", "Lista, detalhes, frequência e histórico de treinos.", "training", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Gym, "Gym", "Check-in de academia, feed e detalhes de frequência.", "gym", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Partners, "Parceiros", "Parceiros, empresas, benefícios e histórico visível.", "partners", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Community, "Comunidade", "Feed, publicações, comentários, reações e denúncias básicas.", "community", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Leagues, "Ligas", "Lista, página da liga, membros, agenda, loja e eventos.", "leagues", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Directory, "Diretório", "Diretórios, membros, agenda, loja, eventos e informações.", "directory", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Commissions, "Comissões", "Comissões, membros, agenda, loja, eventos e informações.", "commissions", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Tenant, "Atlética", "Identidade visual, slug, logo, cores e módulos por tenant.", "tenant", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.MiniVendor, "Mini-vendor", "Lojinha, produtos, pedidos e financeiro simplificado.", "mini-vendor", AppModulePhase.EssentialV1),
        AppModule(
            key = AppModuleKey.Scanner,
            title = "Scanner",
            description = "Leitura e validação segura de ingressos, fichas e check-ins.",
            route = "scanner",
            phase = AppModulePhase.EssentialV1,
            allowedRoles = PermissionRoles.scanner,
        ),
        AppModule(AppModuleKey.Guide, "Guia", "Conteúdo informativo, FAQ e contatos úteis.", "guide", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Legal, "Legal e LGPD", "Termos, privacidade, consentimentos e solicitações LGPD.", "legal", AppModulePhase.EssentialV1),
        AppModule(AppModuleKey.Album, "Álbum", "Álbum da galera, caça-calouro, turmas, pontuação e ranking.", "album", AppModulePhase.ImportantV2),
        AppModule(AppModuleKey.Ranking, "Ranking", "Rankings de turmas, álbum, presença e gamificação.", "ranking", AppModulePhase.ImportantV2),
        AppModule(AppModuleKey.Games, "Games", "Arena, atributos, batalhas e progressão.", "games", AppModulePhase.ImportantV2),
        AppModule(AppModuleKey.Boardround, "Boardround", "Boardround, estatísticas, ranking e quizzes.", "boardround", AppModulePhase.ImportantV2),
        AppModule(AppModuleKey.Achievements, "Conquistas", "Conquistas, badges e progresso por atividade.", "achievements", AppModulePhase.ImportantV2),
        AppModule(AppModuleKey.Fidelity, "Fidelidade", "Recompensas, resgates e histórico de fidelidade.", "fidelity", AppModulePhase.ImportantV2),
        AppModule(AppModuleKey.History, "Histórico", "História institucional e organograma.", "history", AppModulePhase.ImportantV2),
        AppModule(AppModuleKey.Company, "Empresa", "Login e gestão pública simplificada de parceiro.", "company", AppModulePhase.ImportantV2),
        AppModule(AppModuleKey.AdminPanel, "Painel admin", "Gestão completa permanece no web/admin.", "admin", AppModulePhase.WebAdminOnly),
        AppModule(AppModuleKey.MasterPanel, "Painel master", "Gestão global da plataforma permanece no web/admin.", "master", AppModulePhase.WebAdminOnly),
    )

    val androidModules = all.filter { it.phase != AppModulePhase.WebAdminOnly && it.phase != AppModulePhase.NotNow }
    val androidV1Modules = androidModules.filter { it.phase == AppModulePhase.EssentialV1 }
}

private object PermissionRoles {
    val scanner = setOf(
        UserRole.Master,
        UserRole.MasterTenant,
        UserRole.AdminGeral,
        UserRole.AdminGestor,
        UserRole.Vendas,
    )
}
