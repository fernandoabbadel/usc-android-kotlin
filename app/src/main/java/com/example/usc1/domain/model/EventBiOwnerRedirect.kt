package com.example.usc1.domain.model

/**
 * "Este evento pertence a outro portal" (M8.1b).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`,
 * `canonicalEventWorkspacePath` (1967) e `eventOwnerRedirectHref` (6622-6634), mais o banner
 * das linhas 6670-6681.
 */

/** Destino do redirecionamento, já resolvido para o dono real do evento. */
data class EventBiOwnerRedirect(
    val eventId: String,
    val ownerScope: EventBiScope,
    val ownerId: String,
    /** `canonicalEventWorkspacePath(...)`: a rota web do workspace do evento. */
    val webPath: String,
) {
    /** Texto do banner (6674). */
    val message: String
        get() = "Este evento pertence a outro portal. Redirecionando para a página correta..."

    /** Rótulo do link (6676). */
    val actionLabel: String get() = "Abrir agora"
}

/**
 * `canonicalEventWorkspacePath` (1967).
 *
 * O destino é o workspace de evento, que é o M10. Enquanto ele não existe no app, o caminho web
 * fica registrado aqui para o banner e para o M10 ligar a navegação sem recalcular a regra.
 */
fun canonicalEventWorkspacePath(
    scope: EventBiScope,
    scopeId: String,
    eventId: String,
    sectionPath: String = "edicao",
): String {
    val section = sectionPath.trimStart('/').ifBlank { "edicao" }
    return when (scope) {
        EventBiScope.Directory -> "/diretorio/configurar/$scopeId/eventos/$eventId/$section"
        EventBiScope.Commission -> "/comissoes/configurar/$scopeId/eventos/$eventId/$section"
        EventBiScope.League -> "/ligas/$scopeId/eventos/$eventId/$section"
        EventBiScope.Tenant -> "/admin/eventos/$eventId/$section"
    }
}

/**
 * `eventOwnerRedirectHref` (6622).
 *
 * Devolve `null` — o `""` do web — quando:
 * - o player é o tenant (`lockedScopeType === "tenant"`);
 * - nenhum evento está selecionado (`eventFilter === "todos"`);
 * - o evento selecionado não está no recorte carregado;
 * - o dono do evento bate com o escopo travado.
 */
fun eventBiOwnerRedirect(
    dataset: EventBiDataset,
    filter: EventBiFilter,
    context: EventBiContext,
): EventBiOwnerRedirect? {
    if (context.scope.type == EventBiScope.Tenant) return null
    if (!filter.hasEventFilter) return null

    val selectedEvent = dataset.events.firstOrNull { it.id == filter.eventId } ?: return null

    // `canonicalEventOwnerScope` (1953): já resolvido na carga do evento.
    val ownerScope = selectedEvent.ownerScope
    val ownerId = selectedEvent.ownerId.ifBlank { EventBiScopeRef.All }
    val lockedScopeId = context.scope.cleanId.ifBlank { EventBiScopeRef.All }

    val ownerMismatch = ownerScope != context.scope.type ||
        (lockedScopeId != EventBiScopeRef.All && ownerId != lockedScopeId)
    if (!ownerMismatch) return null

    return EventBiOwnerRedirect(
        eventId = selectedEvent.id,
        ownerScope = ownerScope,
        ownerId = ownerId,
        webPath = canonicalEventWorkspacePath(ownerScope, ownerId, selectedEvent.id),
    )
}
