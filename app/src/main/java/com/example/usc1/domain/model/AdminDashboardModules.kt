package com.example.usc1.domain.model

data class AdminDashboardModulesBundle(
    val tenantId: String,
    val tenantName: String,
    val tenantSlug: String,
    val activeProfileName: String,
    val config: TenantAppModulesConfig,
    val groups: List<AdminDashboardModulesGroup>,
)

data class AdminDashboardModulesGroup(
    val key: String,
    val label: String,
    val modules: List<TenantAppModuleDefinition>,
)

data class TenantAppModulesConfig(
    val modules: Map<String, Boolean>,
)

data class TenantAppModuleDefinition(
    val key: String,
    val label: String,
    val description: String,
    val surfaces: List<String>,
    val route: String?,
    val group: String,
    val matchRoutes: List<String> = emptyList(),
    val legacyKeys: List<String> = emptyList(),
)

object TenantAppModulesCatalog {
    private const val GroupBase = "base"
    private const val GroupContent = "conteudo"
    private const val GroupAthlete = "atleta"
    private const val GroupInfo = "info"

    val groupOrder = listOf(GroupBase, GroupContent, GroupAthlete, GroupInfo)

    val groupLabels = mapOf(
        GroupBase to "Base",
        GroupContent to "Conteúdo",
        GroupAthlete to "Área do Atleta",
        GroupInfo to "Central de Info",
    )

    val definitions = listOf(
        moduleItem(
            key = "dashboard",
            label = "Dashboard",
            description = "Controla a entrada principal do app da atlética.",
            surfaces = listOf("dashboard", "route"),
            route = "/dashboard",
            group = GroupBase,
            matchRoutes = listOf("/dashboard"),
        ),
        moduleItem(
            key = "perfil",
            label = "Perfil",
            description = "Acesso ao perfil do atleta no topo do dashboard e na lateral.",
            surfaces = listOf("dashboard", "sidebar"),
            route = "/perfil",
            group = GroupBase,
            matchRoutes = listOf("/perfil"),
        ),
        moduleItem(
            key = "perfil_mini_vendor",
            label = "Perfil Público Mini Vendor",
            description = "Controla o perfil público das lojinhas mini vendor.",
            surfaces = listOf("route"),
            route = "/perfil/mini-vendor",
            group = GroupBase,
            matchRoutes = listOf("/perfil/mini-vendor"),
            legacyKeys = listOf("perfil"),
        ),
        moduleItem(
            key = "carteirinha",
            label = "Carteirinha",
            description = "Exibe a carteirinha digital no dashboard e no menu.",
            surfaces = listOf("dashboard", "sidebar", "bottom_nav"),
            route = "/carteirinha",
            group = GroupBase,
            matchRoutes = listOf("/carteirinha"),
        ),
        moduleItem(
            key = "configuracoes",
            label = "Configurações",
            description = "Central de configurações do usuário, segurança e pedidos.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes"),
        ),
        moduleItem(
            key = "configuracoes_lider_turma",
            label = "Configurações - Líder de Turma",
            description = "Acesso aos ajustes de líder de turma.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/lider-turma",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/lider-turma"),
            legacyKeys = listOf("configuracoes"),
        ),
        moduleItem(
            key = "seguranca",
            label = "Configurações - Segurança",
            description = "Acesso aos ajustes de segurança da conta.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/seguranca",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/seguranca"),
            legacyKeys = listOf("configuracoes"),
        ),
        moduleItem(
            key = "suporte",
            label = "Configurações - Suporte",
            description = "Acesso aos canais de suporte.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/suporte",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/suporte"),
            legacyKeys = listOf("configuracoes"),
        ),
        moduleItem(
            key = "termos",
            label = "Configurações - Termos",
            description = "Acesso aos termos e documentos do app.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/termos",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/termos"),
            legacyKeys = listOf("configuracoes"),
        ),
        moduleItem(
            key = "pedidos",
            label = "Configurações - Pedidos",
            description = "Histórico consolidado de pedidos do usuário.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/pedidos",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/pedidos"),
            legacyKeys = listOf("configuracoes"),
        ),
        moduleItem(
            key = "pedidos_eventos",
            label = "Pedidos - Eventos",
            description = "Pedidos e ingressos de eventos.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/pedidos/eventos",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/pedidos/eventos"),
            legacyKeys = listOf("pedidos", "configuracoes"),
        ),
        moduleItem(
            key = "pedidos_loja",
            label = "Pedidos - Loja",
            description = "Pedidos feitos na loja.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/pedidos/loja",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/pedidos/loja"),
            legacyKeys = listOf("pedidos", "configuracoes"),
        ),
        moduleItem(
            key = "pedidos_planos",
            label = "Pedidos - Planos",
            description = "Pedidos e adesões de planos.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/pedidos/planos",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/pedidos/planos"),
            legacyKeys = listOf("pedidos", "configuracoes"),
        ),
        moduleItem(
            key = "mini_vendor",
            label = "Mini Vendor",
            description = "Libera o hub da lojinha, seus produtos e pedidos na área de configurações.",
            surfaces = listOf("settings"),
            route = "/configuracoes/mini-vendor",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/mini-vendor"),
        ),
        moduleItem(
            key = "mini_vendor_editar",
            label = "Mini Vendor - Editar Loja",
            description = "Edição da loja mini vendor.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/mini-vendor/editar",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/mini-vendor/editar"),
            legacyKeys = listOf("mini_vendor"),
        ),
        moduleItem(
            key = "mini_vendor_pedidos_aprovados",
            label = "Mini Vendor - Pedidos Aprovados",
            description = "Pedidos aprovados da loja mini vendor.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/mini-vendor/pedidos-aprovados",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/mini-vendor/pedidos-aprovados"),
            legacyKeys = listOf("mini_vendor"),
        ),
        moduleItem(
            key = "mini_vendor_pedidos_pendentes",
            label = "Mini Vendor - Pedidos Pendentes",
            description = "Pedidos pendentes da loja mini vendor.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/mini-vendor/pedidos-pendentes",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/mini-vendor/pedidos-pendentes"),
            legacyKeys = listOf("mini_vendor"),
        ),
        moduleItem(
            key = "mini_vendor_produtos",
            label = "Mini Vendor - Produtos",
            description = "Cadastro de produtos da loja mini vendor.",
            surfaces = listOf("settings", "route"),
            route = "/configuracoes/mini-vendor/produtos",
            group = GroupBase,
            matchRoutes = listOf("/configuracoes/mini-vendor/produtos"),
            legacyKeys = listOf("mini_vendor"),
        ),
        moduleItem(
            key = "album",
            label = "Album da Galera",
            description = "Libera o album e o scanner no app.",
            surfaces = listOf("dashboard", "sidebar", "bottom_nav"),
            route = "/album",
            group = GroupContent,
            matchRoutes = listOf("/album"),
        ),
        moduleItem(
            key = "eventos",
            label = "Eventos",
            description = "Controla eventos no dashboard e na navegacao principal.",
            surfaces = listOf("dashboard", "sidebar", "bottom_nav"),
            route = "/eventos",
            group = GroupContent,
            matchRoutes = listOf("/eventos"),
        ),
        moduleItem(
            key = "eventos_compra",
            label = "Eventos - Compra",
            description = "Fluxo de compra vinculado aos eventos.",
            surfaces = listOf("route"),
            route = "/eventos/compra",
            group = GroupContent,
            matchRoutes = listOf("/eventos/compra"),
            legacyKeys = listOf("eventos"),
        ),
        moduleItem(
            key = "loja",
            label = "Loja",
            description = "Mostra a lojinha no dashboard e na lateral.",
            surfaces = listOf("dashboard", "sidebar"),
            route = "/loja",
            group = GroupContent,
            matchRoutes = listOf("/loja"),
        ),
        moduleItem(
            key = "carrinho",
            label = "Loja - Carrinho",
            description = "Carrinho da loja do tenant.",
            surfaces = listOf("route"),
            route = "/carrinho",
            group = GroupContent,
            matchRoutes = listOf("/carrinho"),
            legacyKeys = listOf("loja"),
        ),
        moduleItem(
            key = "checkout",
            label = "Loja - Checkout",
            description = "Checkout da loja e dos pedidos.",
            surfaces = listOf("route"),
            route = "/checkout",
            group = GroupContent,
            matchRoutes = listOf("/checkout"),
            legacyKeys = listOf("loja"),
        ),
        moduleItem(
            key = "comunidade",
            label = "Comunidade",
            description = "Mostra comunidade no dashboard e na barra lateral.",
            surfaces = listOf("dashboard", "sidebar"),
            route = "/comunidade",
            group = GroupContent,
            matchRoutes = listOf("/comunidade"),
        ),
        moduleItem(
            key = "parceiros",
            label = "Parceiros",
            description = "Exibe parceiros premium no dashboard e o atalho no menu.",
            surfaces = listOf("dashboard", "sidebar"),
            route = "/parceiros",
            group = GroupContent,
            matchRoutes = listOf("/parceiros"),
        ),
        moduleItem(
            key = "empresa",
            label = "Painel Empresa",
            description = "Painel público e histórico dos parceiros.",
            surfaces = listOf("route"),
            route = "/empresa",
            group = GroupContent,
            matchRoutes = listOf("/empresa"),
            legacyKeys = listOf("parceiros"),
        ),
        moduleItem(
            key = "sharkround",
            label = "BoardRound",
            description = "Mostra o card e o atalho do BoardRound.",
            surfaces = listOf("dashboard", "sidebar"),
            route = "/boardround",
            group = GroupAthlete,
            matchRoutes = listOf("/boardround", "/sharkround"),
        ),
        moduleItem(
            key = "sharkround_estatisticas",
            label = "BoardRound - Estatisticas",
            description = "Estatisticas detalhadas do BoardRound.",
            surfaces = listOf("route"),
            route = "/boardround/estatisticas",
            group = GroupAthlete,
            matchRoutes = listOf("/boardround/estatisticas", "/sharkround/estatisticas"),
            legacyKeys = listOf("sharkround"),
        ),
        moduleItem(
            key = "sharkround_ranking",
            label = "BoardRound - Ranking",
            description = "Ranking do BoardRound.",
            surfaces = listOf("route"),
            route = "/boardround/ranking",
            group = GroupAthlete,
            matchRoutes = listOf("/boardround/ranking", "/sharkround/ranking"),
            legacyKeys = listOf("sharkround"),
        ),
        moduleItem(
            key = "treinos",
            label = "Treinos",
            description = "Lista treinos no dashboard e no menu lateral.",
            surfaces = listOf("dashboard", "sidebar"),
            route = "/treinos",
            group = GroupAthlete,
            matchRoutes = listOf("/treinos"),
        ),
        moduleItem(
            key = "gym_rats",
            label = "Gym / Check-in",
            description = "Controla o acesso ao módulo gym e check-in da atlética.",
            surfaces = listOf("sidebar", "route"),
            route = "/gym",
            group = GroupAthlete,
            matchRoutes = listOf("/gym", "/gym-rats"),
        ),
        moduleItem(
            key = "gym_checkin",
            label = "Gym - Check-in",
            description = "Tela de check-in do módulo gym.",
            surfaces = listOf("route"),
            route = "/gym/checkin",
            group = GroupAthlete,
            matchRoutes = listOf("/gym/checkin"),
            legacyKeys = listOf("gym_rats"),
        ),
        moduleItem(
            key = "gym_checkin_details",
            label = "Gym - Check-in Details",
            description = "Detalhes do check-in do módulo gym.",
            surfaces = listOf("route"),
            route = "/gym/checkin/details",
            group = GroupAthlete,
            matchRoutes = listOf("/gym/checkin/details"),
            legacyKeys = listOf("gym_checkin", "gym_rats"),
        ),
        moduleItem(
            key = "arena_games",
            label = "Arena Games",
            description = "Mostra o atalho da Arena Games na lateral.",
            surfaces = listOf("sidebar", "route"),
            route = "/games",
            group = GroupAthlete,
            matchRoutes = listOf("/games", "/arena-games"),
        ),
        moduleItem(
            key = "ranking",
            label = "Ranking",
            description = "Mostra o atalho de ranking na lateral.",
            surfaces = listOf("sidebar", "route"),
            route = "/ranking",
            group = GroupAthlete,
            matchRoutes = listOf("/ranking"),
        ),
        moduleItem(
            key = "ligas",
            label = "Ligas USC",
            description = "Exibe a área principal das ligas no dashboard e no menu.",
            surfaces = listOf("dashboard", "sidebar"),
            route = "/ligas_usc",
            group = GroupInfo,
            matchRoutes = listOf("/ligas_usc", "/ligas_unitau"),
        ),
        moduleItem(
            key = "ligas_gerenciar",
            label = "Ligas - Gerenciar",
            description = "Tela de gestão interna das ligas e do quiz.",
            surfaces = listOf("route"),
            route = "/ligas",
            group = GroupInfo,
            matchRoutes = listOf("/ligas"),
            legacyKeys = listOf("ligas"),
        ),
        moduleItem(
            key = "comissoes",
            label = "Comissões",
            description = "Exibe a área de comissões no menu lateral e libera suas rotas públicas e administrativas.",
            surfaces = listOf("sidebar", "route"),
            route = "/comissoes",
            group = GroupInfo,
            matchRoutes = listOf("/comissoes"),
        ),
        moduleItem(
            key = "diretorio",
            label = "Diretório",
            description = "Exibe a área de diretório no menu lateral e libera suas rotas públicas e administrativas.",
            surfaces = listOf("sidebar", "route"),
            route = "/diretorio",
            group = GroupInfo,
            matchRoutes = listOf("/diretorio"),
        ),
        moduleItem(
            key = "planos",
            label = "Planos",
            description = "Catálogo de planos e assinaturas do tenant.",
            surfaces = listOf("sidebar", "route"),
            route = "/planos",
            group = GroupInfo,
            matchRoutes = listOf("/planos"),
        ),
        moduleItem(
            key = "planos_adesao",
            label = "Planos - Adesao",
            description = "Fluxo de adesao e contratacao de planos.",
            surfaces = listOf("route"),
            route = "/planos/adesao",
            group = GroupInfo,
            matchRoutes = listOf("/planos/adesao"),
            legacyKeys = listOf("planos"),
        ),
        moduleItem(
            key = "avaliacao",
            label = "Avaliacao",
            description = "Mostra o atalho de avaliacao de professores.",
            surfaces = listOf("sidebar"),
            route = "/avaliacao",
            group = GroupInfo,
            matchRoutes = listOf("/avaliacao"),
        ),
        moduleItem(
            key = "conquistas",
            label = "Conquistas",
            description = "Mostra o acesso a conquistas na lateral.",
            surfaces = listOf("sidebar", "route"),
            route = "/conquistas",
            group = GroupInfo,
            matchRoutes = listOf("/conquistas"),
        ),
        moduleItem(
            key = "fidelidade",
            label = "Fidelidade",
            description = "Mostra o acesso ao clube de fidelidade na lateral.",
            surfaces = listOf("sidebar", "route"),
            route = "/fidelidade",
            group = GroupInfo,
            matchRoutes = listOf("/fidelidade"),
        ),
        moduleItem(
            key = "guia",
            label = "Guia",
            description = "Controla o atalho do guia do app na central de informações.",
            surfaces = listOf("sidebar", "route"),
            route = "/guia",
            group = GroupInfo,
            matchRoutes = listOf("/guia"),
        ),
        moduleItem(
            key = "historico",
            label = "Nossa Historia",
            description = "Controla o acesso ao histórico institucional da atlética.",
            surfaces = listOf("sidebar", "route"),
            route = "/historico",
            group = GroupInfo,
            matchRoutes = listOf("/historico"),
        ),
    )

    val defaultConfig = TenantAppModulesConfig(
        modules = definitions.associate { it.key to true },
    )

    fun normalizeModules(raw: Map<String, Boolean>): Map<String, Boolean> {
        val normalized = defaultConfig.modules.toMutableMap()
        definitions.forEach { definition ->
            raw[definition.key]?.let { normalized[definition.key] = it }
        }
        return normalized
    }

    fun isVisible(source: Map<String, Boolean>, definition: TenantAppModuleDefinition): Boolean {
        source[definition.key]?.let { return it }
        definition.legacyKeys.forEach { legacyKey ->
            source[legacyKey]?.let { return it }
        }
        return true
    }

    fun groupDefinitions(
        modules: Map<String, Boolean>,
        profileAppModules: Map<String, Boolean>,
    ): List<AdminDashboardModulesGroup> {
        return groupOrder.mapNotNull { groupKey ->
            val items = definitions.filter { definition ->
                definition.group == groupKey && isVisible(profileAppModules, definition)
            }
            if (items.isEmpty()) {
                null
            } else {
                AdminDashboardModulesGroup(
                    key = groupKey,
                    label = groupLabels[groupKey].orEmpty(),
                    modules = items,
                )
            }
        }
    }

    fun isEnabled(config: TenantAppModulesConfig, key: String): Boolean {
        return config.modules[key] != false
    }

    private fun moduleItem(
        key: String,
        label: String,
        description: String,
        surfaces: List<String>,
        route: String?,
        group: String,
        matchRoutes: List<String> = emptyList(),
        legacyKeys: List<String> = emptyList(),
    ) = TenantAppModuleDefinition(
        key = key,
        label = label,
        description = description,
        surfaces = surfaces,
        route = route,
        group = group,
        matchRoutes = matchRoutes,
        legacyKeys = legacyKeys,
    )
}
