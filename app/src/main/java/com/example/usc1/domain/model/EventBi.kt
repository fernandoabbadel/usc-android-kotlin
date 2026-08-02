package com.example.usc1.domain.model

import com.example.usc1.ui.collectives.CollectiveKind
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.serialization.json.JsonObject

/** Linha vazia — o `?? {}` que o web usa quando a coluna não veio. */
val EmptyJsonObject: JsonObject = JsonObject(emptyMap())

/**
 * Motor do BI de Eventos (M8.1).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 1-6770.
 * Os quatro players chamam o **mesmo** componente, mudando só as props de escopo:
 * - tenant: `app/admin/bi/page.tsx` e `app/admin/gestao/eventos/page.tsx` (sem `lockedScope*`);
 * - liga: `app/ligas/_components/LeagueEventBiDashboard.tsx`;
 * - comissão: `CommissionManagementEventBiPage` de `components/collectives/CommissionManagementPages.tsx`;
 * - diretório: `DirectoryManagementEventBiPage` de `components/collectives/DirectoryManagementPages.tsx`.
 *
 * Aqui vale o mesmo princípio: um motor só, parametrizado por escopo.
 */

/** `ScopeType` do web (`"tenant" | "league" | "directory" | "commission"`). */
enum class EventBiScope(val remoteValue: String) {
    Tenant("tenant"),
    League("league"),
    Commission("commission"),
    Directory("directory");

    /** `scopeLabel` de cada wrapper; o player tenant não passa nenhum. */
    val defaultScopeLabel: String
        get() = when (this) {
            Tenant -> ""
            League -> "da liga"
            Commission -> "da comissão"
            Directory -> "do diretório"
        }

    /** `contextEyebrow` de cada wrapper; o tenant cai no default do `DashboardShell`. */
    val defaultEyebrow: String
        get() = when (this) {
            Tenant -> DefaultEyebrow
            League -> "BI da liga"
            Commission -> "BI da comissão"
            Directory -> "BI do diretório"
        }

    companion object {
        /** `contextEyebrow?.trim() || "BI Administrativo"` do `DashboardShell`. */
        const val DefaultEyebrow = "BI Administrativo"

        /** `entityScopeType` do web: categoria do registro em `ligas_config`. */
        fun fromRemote(value: String?): EventBiScope {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return when {
                normalized.contains("diretorio") || normalized.contains("directory") -> Directory
                normalized.contains("comiss") || normalized.contains("commission") -> Commission
                normalized == "liga" || normalized.contains("league") -> League
                else -> Tenant
            }
        }

        fun fromCollectiveKind(kind: CollectiveKind): EventBiScope = when (kind) {
            CollectiveKind.League -> League
            CollectiveKind.Commission -> Commission
            CollectiveKind.Directory -> Directory
        }
    }
}

/** `lockedScopeType` + `lockedScopeId` das props do dashboard. */
data class EventBiScopeRef(
    val type: EventBiScope = EventBiScope.Tenant,
    val id: String = All,
) {
    /** `scopeId === "todos"`: o escopo não aponta para uma entidade específica. */
    val isAllEntities: Boolean get() = id.isBlank() || id.trim() == All

    /** Escopo de coletivo com id resolvido — o caso dos players liga/comissão/diretório. */
    val isCollective: Boolean get() = type != EventBiScope.Tenant && !isAllEntities

    val cleanId: String get() = id.trim()

    companion object {
        /** `"todos"` do web. */
        const val All = "todos"

        /**
         * `scopeLocked={Boolean(lockedScopeType)}` do web. Como `lockedScopeType` tem default
         * `"tenant"`, a expressão é sempre verdadeira: o seletor de escopo nunca aparece — nem
         * no player tenant, que mostra apenas o rótulo "Atlética". O app repete o comportamento.
         */
        const val SelectorLocked = true
    }
}

/** `AdminEventBiView` do web, com os títulos e subtítulos exatos do array `MODULES`. */
enum class EventBiView(
    val routeValue: String,
    val title: String,
    val subtitle: String,
) {
    Home("inicio", "BI de Eventos", ""),
    Commercial("comercial", "BI Comercial", "Venda, receita, lote, preço, turma e funil."),
    Operational("operacional", "BI Operacional", "Aprovação, comprovante, fila, gargalo e aprovadores."),
    Gate("portaria", "BI Portaria", "Entrada, presença, ausência e leitura de QR code."),
    Strategic("estrategico", "BI Estratégico", "Recorrência, previsão, comportamento e repetição."),
    Sales("vendas", "BI Modo Vendas", "Produto, ficha, bar, retirada, baixa e auditoria.");

    /** `showProduct={view === "vendas"}` do `Filters`. */
    val showsProductFilter: Boolean get() = this == Sales

    companion object {
        /** `MODULES` do web: os cinco cards do hub, na ordem do array. */
        val modules: List<EventBiView> get() = entries.filter { it != Home }

        fun fromRoute(value: String?): EventBiView {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.routeValue == normalized } ?: Home
        }
    }
}

/** `AUDIENCE_BASIS_OPTIONS` do web. */
enum class EventBiAudienceBasis(val remoteValue: String, val label: String) {
    Orders("pedidos", "Pedidos"),
    Approved("aprovados", "Aprovados"),
    CheckIn("checkin", "Check-in"),
}

/** Opção de `<select>` (`ScopeOption` do web). */
data class EventBiOption(val id: String, val name: String)

/** Estado dos filtros do dashboard (`useState` do componente web). */
data class EventBiFilter(
    val scope: EventBiScopeRef = EventBiScopeRef(),
    val eventId: String = EventBiScopeRef.All,
    val productId: String = EventBiScopeRef.All,
    val startDate: String = "",
    val endDate: String = "",
    /** `useState<AudienceBasis>("aprovados")`. */
    val audienceBasis: EventBiAudienceBasis = EventBiAudienceBasis.Approved,
) {
    val hasEventFilter: Boolean get() = eventId.isNotBlank() && eventId != EventBiScopeRef.All

    val hasProductFilter: Boolean get() = productId.isNotBlank() && productId != EventBiScopeRef.All

    val hasPeriodFilter: Boolean get() = startDate.isNotBlank() || endDate.isNotBlank()
}

/**
 * `isApprovedStatus`/`isRejectedStatus`/`isCancelledStatus`/`isRefundedStatus` do BI.
 *
 * Atenção: a lista de aprovados do BI é maior que a de `statusIsApproved` usada no M7
 * (inclui `paid`, `pago`, `confirmado`, `confirmada` e `redeemed`), então não dá para
 * reaproveitar `ApprovedStatuses` do repositório de gestão.
 */
enum class EventBiStatus {
    Approved,
    Rejected,
    Cancelled,
    Refunded,
    Pending;

    companion object {
        private val ApprovedValues = listOf(
            "approved", "aprovado", "aprovada", "paid", "pago",
            "confirmado", "confirmada", "entregue", "validado", "redeemed",
        )
        private val RejectedValues = listOf(
            "rejected", "recusado", "recusada", "reprovado", "reprovada", "denied",
        )
        private val CancelledValues = listOf(
            "cancelled", "canceled", "cancelado", "cancelada", "expired", "expirado",
        )
        private val RefundedValues = listOf("refunded", "refund", "reembolsado", "estornado")

        fun classify(value: String?): EventBiStatus {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return when (normalized) {
                in ApprovedValues -> Approved
                in RejectedValues -> Rejected
                in CancelledValues -> Cancelled
                in RefundedValues -> Refunded
                else -> Pending
            }
        }
    }
}

/**
 * `dateInPeriod` do web: registro sem data sempre passa; `startDate` corta em `T00:00:00`
 * e `endDate` em `T23:59:59`.
 */
fun eventBiDateInPeriod(millis: Long, startDate: String, endDate: String): Boolean {
    if (millis <= 0L) return true
    val zone = ZoneId.systemDefault()

    val start = parseBoundary(startDate)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
    if (start != null && millis < start) return false

    val end = parseBoundary(endDate)?.atTime(LocalTime.of(23, 59, 59))?.atZone(zone)
        ?.toInstant()?.toEpochMilli()
    if (end != null && millis > end) return false

    return true
}

private fun parseBoundary(value: String): LocalDate? =
    value.trim().takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

// ------------------------------------------------------------------
// Registros já normalizados (`BiData` do web, no escopo ativo)
// ------------------------------------------------------------------

/**
 * Linha de `eventos` com o dono resolvido por `eventScopeIds`/`canonicalEventOwnerScope`.
 *
 * `raw` é a linha como veio do Supabase. O motor de métricas (M8.1b) lê dela com os mesmos
 * acessores do web (`eventCapacity`, `eventCost`, `eventLotRows`), em vez de duplicar a cadeia
 * de apelidos aqui e no `EventBiAccessors.kt`.
 */
data class EventBiEvent(
    val id: String,
    val name: String,
    val startsAtMillis: Long = 0L,
    val ownerScope: EventBiScope = EventBiScope.Tenant,
    val ownerId: String = "",
    val ownerName: String = "",
    val capacity: Int = 0,
    val cardClicks: Int = 0,
    val buyClicks: Int = 0,
    val confirmedCount: Int = 0,
    val maybeCount: Int = 0,
    val raw: JsonObject = EmptyJsonObject,
)

/** Linha de `solicitacoes_ingressos`. */
data class EventBiTicket(
    val id: String,
    val eventId: String,
    val eventName: String = "",
    val status: EventBiStatus = EventBiStatus.Pending,
    val rawStatus: String = "",
    val quantity: Int = 1,
    val value: Double = 0.0,
    val discount: Double = 0.0,
    val lotName: String = "Sem lote",
    val buyerId: String = "",
    val buyerName: String = "",
    val buyerClass: String = "Sem turma",
    val approver: String = "Sem aprovador",
    val purchasedAtMillis: Long = 0L,
    val approvedAtMillis: Long = 0L,
    /** `ticketScannedCount`: entradas com QR já lido. */
    val scannedCount: Int = 0,
    val raw: JsonObject = EmptyJsonObject,
)

/** Linha de `orders` ligada a evento (produto de festa / modo vendas). */
data class EventBiOrder(
    val id: String,
    val eventId: String = "",
    val productId: String = "",
    val productName: String = "Produto",
    val status: EventBiStatus = EventBiStatus.Pending,
    val rawStatus: String = "",
    val quantity: Int = 1,
    val total: Double = 0.0,
    val discount: Double = 0.0,
    val buyerId: String = "",
    val buyerName: String = "",
    val buyerClass: String = "Sem turma",
    val approver: String = "Sem aprovador",
    val createdAtMillis: Long = 0L,
    val raw: JsonObject = EmptyJsonObject,
)

/** Linha de `produtos` vinculada a um evento (`productEventId` do web). */
data class EventBiProduct(
    val id: String,
    val eventId: String,
    val name: String = "Produto",
    val category: String = "",
    val lot: String = "",
    val price: Double = 0.0,
    val stock: Int = 0,
    val sold: Int = 0,
    val clicks: Int = 0,
    val raw: JsonObject = EmptyJsonObject,
)

/** Linha de `eventos_rsvps`. */
data class EventBiRsvp(
    val id: String,
    val eventId: String,
    val userId: String = "",
    val userClass: String = "Sem turma",
    /** `rsvpStatus`: `going`, `maybe` ou vazio. */
    val status: String = "",
    val createdAtMillis: Long = 0L,
)

/**
 * `selectedData` do web: o recorte do escopo + filtros já aplicado.
 *
 * Diferente do web, que baixa sete tabelas inteiras do tenant e filtra em memória, aqui o
 * escopo vai para a consulta (ver `SupabaseEventBiRepository`).
 */
data class EventBiDataset(
    val eventOptions: List<EventBiOption> = emptyList(),
    val productOptions: List<EventBiOption> = emptyList(),
    val events: List<EventBiEvent> = emptyList(),
    val tickets: List<EventBiTicket> = emptyList(),
    val orders: List<EventBiOrder> = emptyList(),
    val products: List<EventBiProduct> = emptyList(),
    val rsvps: List<EventBiRsvp> = emptyList(),
    /**
     * `data.tickets` do web: os ingressos de **todo o escopo**, sem o filtro de evento nem o de
     * período. Só a recorrência histórica (`historicalTicketCheckinsByBuyer`, linha 5418) usa
     * essa base maior; todo o resto usa `tickets`.
     */
    val scopeTickets: List<EventBiTicket> = emptyList(),
    /** `data.orders` do web, pelo mesmo motivo (linha 5425). */
    val scopeOrders: List<EventBiOrder> = emptyList(),
    /** `userById` do web (3723): só os usuários citados pelas linhas do escopo. */
    val usersById: Map<String, JsonObject> = emptyMap(),
    /**
     * `tenantUserCount` (5467). O web conta as 6000 linhas de `users` que baixou; o app usa
     * `count` no PostgREST, sem trazer linha nenhuma.
     */
    val tenantUserCount: Int = 0,
    /** `entityMemberIndex` do web (3732), para `classifyTicketOperationalCategory`. */
    val memberIndex: Map<String, EventBiMemberMeta> = emptyMap(),
    /** `false` quando só as opções de filtro foram carregadas (hub). */
    val hasTransactions: Boolean = false,
) {
    val isEmpty: Boolean get() = events.isEmpty() && products.isEmpty()

    /** `eventById` do web (3721). */
    val eventsById: Map<String, EventBiEvent> get() = events.associateBy { it.id }

    /** `productsById` do web (3722). */
    val productsById: Map<String, EventBiProduct> get() = products.associateBy { it.id }
}

// ------------------------------------------------------------------
// Cabeçalho (`DashboardShell` do web)
// ------------------------------------------------------------------

/** `backHref`, `contextTitle`, `contextLogo` e `contextEyebrow` das props. */
data class EventBiContext(
    val scope: EventBiScopeRef = EventBiScopeRef(),
    val scopeLabel: String = "",
    val contextTitle: String = "",
    val contextLogo: String = "",
    val contextEyebrow: String = "",
) {
    /** Rótulo do bloco travado do `Filters`. */
    val lockedScopeLabel: String
        get() = scopeLabel.trim().ifBlank {
            if (scope.type == EventBiScope.Tenant) "Atlética" else "Entidade"
        }
}

/** Textos resolvidos do cabeçalho. */
data class EventBiHeaderText(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
)

/**
 * `DashboardShell` do web:
 * - `title` é "BI de Eventos" no `inicio` e o `MODULES.title` nas demais visões;
 * - `subtitle` no `inicio` é "Escolha a visão analítica{scopeLabel}.";
 * - `titleLabel = contextTitle || title`;
 * - `subtitleLabel = contextTitle ? "{title}. {subtitle}" : subtitle`.
 */
fun EventBiContext.headerFor(view: EventBiView): EventBiHeaderText {
    val title = if (view == EventBiView.Home) EventBiView.Home.title else view.title
    val subtitle = if (view == EventBiView.Home) {
        val suffix = scopeLabel.trim().takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
        "Escolha a visão analítica$suffix."
    } else {
        view.subtitle.ifBlank { "Indicadores do evento." }
    }
    val cleanContextTitle = contextTitle.trim()

    return EventBiHeaderText(
        eyebrow = contextEyebrow.trim().ifBlank { EventBiScope.DefaultEyebrow },
        title = cleanContextTitle.ifBlank { title },
        subtitle = if (cleanContextTitle.isNotBlank()) "$title. $subtitle" else subtitle,
    )
}
