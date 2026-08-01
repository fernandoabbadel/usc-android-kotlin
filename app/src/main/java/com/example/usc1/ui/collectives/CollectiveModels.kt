package com.example.usc1.ui.collectives

import com.example.usc1.R

/**
 * Modelos das áreas públicas de coletivos.
 *
 * Fonte web: `ligas_config` normalizada por `web-reference/src/lib/leaguesService.ts`
 * (`LeagueRecord`), consumida pelas rotas `app/ligas_usc`, `components/collectives/CollectiveCatalogPage.tsx`
 * e `components/collectives/CollectivePublicDetailClient.tsx`.
 */
enum class CollectiveKind(
    val label: String,
    /** `normalizeLeagueCategory` do web. */
    val category: String,
) {
    League("Liga", "liga"),
    Directory("Diretório", "diretorio"),
    Commission("Comissão", "comissao"),
}

/** Abas públicas do web: `overview | membros | agenda | loja`. */
enum class CollectiveTab(val label: String) {
    Overview("Visão geral"),
    Members("Membros"),
    Agenda("Agenda"),
    Store("Loja"),
}

/** `normalizeLeagueEventVisibility` do web. */
enum class CollectiveEventVisibility {
    Public,
    Internal,
}

data class CollectiveMember(
    val id: String = "",
    val name: String,
    val role: String,
    val photoUrl: String? = null,
    val profileLink: String = "",
)

/** `LeagueMemberJoinRequestRecord` do web. */
data class CollectiveMemberRequest(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val photoUrl: String? = null,
    val turma: String = "",
    val requestedRole: String = LeagueRoleCatalog.DefaultRole,
    val createdAt: String = "",
)

data class CollectiveEvent(
    val id: String = "",
    val title: String,
    val date: String = "",
    val time: String = "",
    val place: String = "",
    val description: String = "",
    val visibility: CollectiveEventVisibility = CollectiveEventVisibility.Public,
    val imageUrl: String? = null,
    val eventLink: String = "",
    val globalEventId: String = "",
) {
    val isInternal: Boolean get() = visibility == CollectiveEventVisibility.Internal

    /** `getVisibilityLabel` do web. */
    val visibilityLabel: String
        get() = if (isInternal) "Evento interno" else "Aberto ao público"

    /** `eventHref` do web: link explícito, senão o evento global publicado. */
    val resolvedEventId: String
        get() = globalEventId.trim().ifBlank { id.trim() }
}

data class CollectiveLink(
    val id: String,
    val label: String,
    val type: String,
    val url: String,
)

data class CollectivePaymentInfo(
    val pixKey: String = "",
    val bank: String = "",
    val holder: String = "",
    val whatsapp: String = "",
) {
    /** `hasPaymentInfo` do web. */
    val hasAnyValue: Boolean
        get() = pixKey.isNotBlank() || bank.isNotBlank() || holder.isNotBlank() || whatsapp.isNotBlank()
}

data class CollectiveGroup(
    val id: String,
    val name: String,
    val acronym: String = "",
    val turmaId: String = "",
    val president: String = "",
    val description: String = "",
    /** `visaoGeral` do web; renderizado linha a linha na aba Visão geral. */
    val overview: String = "",
    val bizu: String = "",
    val kind: CollectiveKind,
    val visible: Boolean = true,
    val active: Boolean = true,
    val likesCount: Int = 0,
    val membersCount: Int = 0,
    val imageUrl: String? = null,
    val members: List<CollectiveMember> = emptyList(),
    val memberRequests: List<CollectiveMemberRequest> = emptyList(),
    val events: List<CollectiveEvent> = emptyList(),
    val links: List<CollectiveLink> = emptyList(),
    val paymentInfo: CollectivePaymentInfo = CollectivePaymentInfo(),
    val managerUserIds: List<String> = emptyList(),
) {
    val imageRes: Int
        get() = when (kind) {
            CollectiveKind.League -> R.drawable.battle_forest
            CollectiveKind.Directory -> R.drawable.logo_usc_wide
            CollectiveKind.Commission -> R.drawable.carteirinha_bg
        }

    /** `league.sigla || league.nome` do web. */
    val displayAcronym: String get() = acronym.ifBlank { name }

    /** `publicLinks` do web: só links com URL preenchida. */
    val publicLinks: List<CollectiveLink> get() = links.filter { it.url.isNotBlank() }

    /** `overviewHighlights` do web. */
    val overviewHighlights: List<String>
        get() = overview.split(Regex("\r?\n")).map { it.trim() }.filter { it.isNotEmpty() }

    /** `sortedMembers` do web (`sortLeagueMembersByRole` + `resolveLeagueRoleLabel`). */
    val sortedMembers: List<CollectiveMember>
        get() = LeagueRoleCatalog
            .sortMembersByRole(members)
            .map { it.copy(role = LeagueRoleCatalog.resolveRoleLabel(it.role)) }

    /** `managementMembers` do web. */
    val managementMembers: List<CollectiveMember>
        get() = sortedMembers.filter { LeagueRoleCatalog.canManageRole(it.role) }

    /**
     * `publicMembers` do web: comissão mostra apenas a diretoria; liga e diretório
     * mostram todos os membros publicados.
     */
    val publicMembers: List<CollectiveMember>
        get() = if (kind == CollectiveKind.Commission) managementMembers else sortedMembers

    /** `presidentName` do web. */
    val presidentName: String
        get() = sortedMembers.firstOrNull { it.role.trim().equals("Presidente", ignoreCase = true) }
            ?.name
            ?.takeIf { it.isNotBlank() }
            ?: president

    /** `sortEvents` do web (data/hora e depois título). */
    val sortedEvents: List<CollectiveEvent>
        get() = events.sortedWith(
            compareBy<CollectiveEvent> { CollectiveDateUtils.parseEventDateTimeMs(it.date, it.time) ?: Long.MAX_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
        )

    val publicAgendaEvents: List<CollectiveEvent> get() = sortedEvents.filterNot { it.isInternal }

    val internalAgendaEvents: List<CollectiveEvent> get() = sortedEvents.filter { it.isInternal }
}

/** `CollectiveAreaUiConfig`/`LigasUscUiConfig` do web, lidos de `app_config`. */
data class CollectiveAreaUiConfig(
    val titulo: String,
    val subtitulo: String,
    val rotuloCard: String,
    val sidebarLabel: String,
    val managerUserIds: List<String> = emptyList(),
) {
    companion object {
        fun default(kind: CollectiveKind): CollectiveAreaUiConfig = when (kind) {
            // DEFAULT_LIGAS_USC_UI_CONFIG do web (ligas_usc não expõe sidebarLabel).
            CollectiveKind.League -> CollectiveAreaUiConfig(
                titulo = "LIGAS USC",
                subtitulo = "Ecossistema Acadêmico",
                rotuloCard = "Liga USC",
                sidebarLabel = "Ligas USC",
            )
            CollectiveKind.Commission -> CollectiveAreaUiConfig(
                titulo = "COMISSÕES",
                subtitulo = "Representação por turma",
                rotuloCard = "Comissão",
                sidebarLabel = "Comissões",
            )
            CollectiveKind.Directory -> CollectiveAreaUiConfig(
                titulo = "DIRETÓRIO",
                subtitulo = "Organização acadêmica",
                rotuloCard = "Diretório",
                sidebarLabel = "Diretório",
            )
        }
    }
}

/** Produto vinculado ao coletivo (`fetchStoreProductsBySeller` do web). */
data class CollectiveStoreProduct(
    val id: String,
    val name: String,
    val priceLabel: String,
    val imageUrl: String? = null,
    val category: String = "",
    val tagLabel: String = "",
)

/** Categoria da loja do coletivo (`fetchStoreCategories` + `isLeagueStoreCategory`). */
data class CollectiveStoreState(
    val enabled: Boolean = true,
    val coverImageUrl: String? = null,
    val products: List<CollectiveStoreProduct> = emptyList(),
    val isLoading: Boolean = false,
)

/** `StoreSellerProductStats` do web, usado para ordenar o catálogo de comissões. */
data class CollectiveSellerStats(
    val sellerId: String,
    val soldCount: Int = 0,
    val exposedCount: Int = 0,
    val likesCount: Int = 0,
)

/** `fetchUserLeagueInteractionState` do web (`users.extra`). */
data class CollectiveInteractionState(
    val likedIds: List<String> = emptyList(),
    val followedIds: List<String> = emptyList(),
)

/** Retorno de `toggleUserLeagueLike`. */
data class CollectiveLikeResult(
    val likedIds: List<String>,
    val isLiked: Boolean,
)
