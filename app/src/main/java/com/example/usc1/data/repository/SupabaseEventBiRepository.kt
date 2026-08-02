package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.EventBiDataset
import com.example.usc1.domain.model.EventBiEvent
import com.example.usc1.domain.model.EventBiFilter
import com.example.usc1.domain.model.EventBiMemberMeta
import com.example.usc1.domain.model.EventBiOption
import com.example.usc1.domain.model.EventBiOrder
import com.example.usc1.domain.model.EventBiProduct
import com.example.usc1.domain.model.EventBiRsvp
import com.example.usc1.domain.model.EventBiScope
import com.example.usc1.domain.model.EventBiScopeRef
import com.example.usc1.domain.model.EventBiStatus
import com.example.usc1.domain.model.EventBiTicket
import com.example.usc1.domain.model.buildEventBiMemberIndex
import com.example.usc1.domain.model.eventBiDateInPeriod
import com.example.usc1.domain.model.eventCapacity
import com.example.usc1.domain.model.firstText
import com.example.usc1.domain.model.num
import com.example.usc1.domain.model.obj
import com.example.usc1.domain.model.objects
import com.example.usc1.domain.model.orderApproverName
import com.example.usc1.domain.model.orderBuyerId
import com.example.usc1.domain.model.orderCreatedAt
import com.example.usc1.domain.model.orderEventId
import com.example.usc1.domain.model.orderProductId
import com.example.usc1.domain.model.orderQuantity
import com.example.usc1.domain.model.orderTotal
import com.example.usc1.domain.model.orderDiscount
import com.example.usc1.domain.model.orderClassName
import com.example.usc1.domain.model.parseEventBiDate
import com.example.usc1.domain.model.productEventId
import com.example.usc1.domain.model.productName
import com.example.usc1.domain.model.statusValue
import com.example.usc1.domain.model.str
import com.example.usc1.domain.model.ticketApprovalDate
import com.example.usc1.domain.model.ticketApproverName
import com.example.usc1.domain.model.ticketClassName
import com.example.usc1.domain.model.ticketBuyerId
import com.example.usc1.domain.model.ticketEventId
import com.example.usc1.domain.model.ticketLotName
import com.example.usc1.domain.model.ticketPurchaseDate
import com.example.usc1.domain.model.ticketQuantity
import com.example.usc1.domain.model.ticketScannedCount
import com.example.usc1.domain.model.ticketDiscount
import com.example.usc1.domain.model.ticketValue
import com.example.usc1.domain.repository.CollectiveEventBiRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/**
 * Motor do BI de Eventos com Supabase direto (M8.1 + M8.1b).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`
 * (`loadBiData`, `buildEventScopeIndex`, `eventScopeIds`, `canonicalEventOwnerScope`,
 * `matchesActiveScope`, `selectedData`, `eventOptions` e `productOptions`).
 *
 * ## Diferença de consulta em relação ao web
 *
 * `loadBiData` (linha ~539) baixa sete tabelas **inteiras** do tenant — `eventos` 600,
 * `solicitacoes_ingressos` 6000, `eventos_rsvps` 12000, `produtos` 3000, `orders` 8000,
 * `users` 6000 e `ligas_config` 1000 — e só depois filtra em memória por escopo, evento,
 * produto e período. Isso é inviável no celular e contra `docs/PROJECT_CONSTRAINTS.md`.
 *
 * Aqui o escopo vai **para a consulta**:
 * - liga/comissão/diretório: um `SELECT` em `ligas_config` pelo id do coletivo devolve as chaves
 *   de evento; `eventos`, `solicitacoes_ingressos`, `eventos_rsvps` e `orders` são filtrados por
 *   essas chaves com `tenant_id` sempre presente;
 * - tenant: `ligas_config` é lido uma vez (200 registros) só para montar o índice
 *   evento → entidade e **excluir** os eventos de coletivo, como `isTenantOwnedRow` faz no web;
 * - `users` **não** é baixada inteira (M8.1b): só as linhas dos compradores que aparecem no
 *   recorte (`isIn("uid", ...)`, teto de 300) e um `count` de cabeçalho para o
 *   `tenantParticipationRate`, sem trazer linha nenhuma;
 * - ingressos e pedidos são consultados por **todos** os eventos do escopo, sem o filtro de
 *   evento nem o de período na consulta. O custo é o mesmo (um SELECT, mesmo teto), e é o que
 *   permite calcular a recorrência histórica, que no web lê `data.tickets`/`data.orders` — o
 *   tenant inteiro. O filtro de evento e o de período são aplicados em memória, gerando
 *   `tickets`/`orders` (o `selectedData`) a partir de `scopeTickets`/`scopeOrders`;
 * - degradação de coluna: `queryRows` do web derruba coluna ausente e repete a consulta. Aqui
 *   cada tabela tem um conjunto rico e um conjunto mínimo de fallback.
 */
class SupabaseEventBiRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : CollectiveEventBiRepository {

    override suspend fun getDataset(
        tenantId: String,
        filter: EventBiFilter,
        includeTransactions: Boolean,
    ): EventBiDataset = withContext(Dispatchers.IO) {
        if (!SupabaseClientProvider.config.isConfigured) return@withContext EventBiDataset()

        val client = clientProvider()
        val cleanTenantId = resolveTenantId(client, tenantId)
        if (cleanTenantId.isBlank()) return@withContext EventBiDataset()

        val scope = filter.scope
        val scopeRows = fetchScopeRows(client, cleanTenantId, scope)
        // `buildEventScopeIndex`: evento -> entidade dona.
        val eventOwners = buildEventOwnerIndex(scopeRows)

        val scopedEvents = fetchScopedEvents(client, cleanTenantId, scope, scopeRows, eventOwners)

        // `eventOptions` do web: todos os eventos do escopo, ordenados por nome.
        val eventOptions = scopedEvents
            .map { EventBiOption(it.id, it.name) }
            .distinctBy { it.id }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

        // `selectedEvents`: o filtro de evento estreita o recorte.
        val selectedEvents = scopedEvents.filter { !filter.hasEventFilter || it.id == filter.eventId }
        val scopeEventIds = scopedEvents.map { it.id }.filter { it.isNotBlank() }.distinct()
        val selectedEventIds = selectedEvents.map { it.id }.filter { it.isNotBlank() }.distinct()

        val scopedProducts = fetchScopedProducts(
            client = client,
            tenantId = cleanTenantId,
            scope = scope,
            allowedEventIds = scopeEventIds.toSet(),
        )
        val productOptions = scopedProducts
            .map { EventBiOption(it.id, it.name) }
            .distinctBy { it.id }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

        val selectedProducts = scopedProducts.filter { product ->
            (selectedEventIds.isEmpty() || selectedEventIds.contains(product.eventId)) &&
                (!filter.hasProductFilter || product.id == filter.productId)
        }

        if (!includeTransactions || scopeEventIds.isEmpty()) {
            return@withContext EventBiDataset(
                eventOptions = eventOptions,
                productOptions = productOptions,
                events = selectedEvents,
                products = selectedProducts,
                hasTransactions = false,
            )
        }

        val scopeProductIds = scopedProducts.map { it.id }.filter { it.isNotBlank() }

        // Escopo inteiro: é o `data.*` do web, base da recorrência histórica.
        val scopeTickets = fetchTickets(client, cleanTenantId, scopeEventIds)
        val scopeOrders = fetchOrders(client, cleanTenantId, scopeEventIds, scopeProductIds)

        // `selectedData.*`: o mesmo recorte, agora com evento, produto e período aplicados.
        val tickets = scopeTickets.filter { ticket ->
            (!filter.hasEventFilter || ticket.eventId == filter.eventId) &&
                eventBiDateInPeriod(ticket.purchasedAtMillis, filter.startDate, filter.endDate)
        }
        val orders = scopeOrders.filter { order ->
            val orderEventId = order.eventId.ifBlank {
                scopedProducts.firstOrNull { it.id == order.productId }?.eventId.orEmpty()
            }
            (!filter.hasEventFilter || orderEventId == filter.eventId) &&
                (!filter.hasProductFilter || order.productId == filter.productId) &&
                eventBiDateInPeriod(order.createdAtMillis, filter.startDate, filter.endDate)
        }
        val rsvps = fetchRsvps(client, cleanTenantId, selectedEventIds, filter)

        // `userById` (3723): só os compradores citados pelo recorte.
        val buyerIds = (
            tickets.map { it.raw.str("userId") } + orders.map { it.raw.str("userId") }
            ).filter { it.isNotBlank() }.distinct().take(UsersLimit.toInt())
        val usersById = fetchUsers(client, cleanTenantId, buyerIds)

        EventBiDataset(
            eventOptions = eventOptions,
            productOptions = productOptions,
            events = selectedEvents,
            tickets = tickets,
            orders = orders,
            products = selectedProducts,
            rsvps = rsvps,
            scopeTickets = scopeTickets,
            scopeOrders = scopeOrders,
            usersById = usersById,
            tenantUserCount = fetchTenantUserCount(client, cleanTenantId),
            // `entityMemberIndex` (3732): usado por `classifyTicketOperationalCategory`.
            memberIndex = buildMemberIndex(scopeRows, scope),
            hasTransactions = true,
        )
    }

    // ------------------------------------------------------------------
    // Consulta genérica com degradação de coluna (`queryRows` do web)
    // ------------------------------------------------------------------

    private suspend fun queryRows(
        client: SupabaseClient,
        table: String,
        columns: String,
        fallbackColumns: String,
        limit: Long,
        orderColumn: String? = null,
        extraFilter: PostgrestFilterBuilder.() -> Unit = {},
    ): List<JsonObject> {
        suspend fun query(selected: String): List<JsonObject> =
            client.from(table)
                .select(columns = Columns.raw(selected)) {
                    filter { extraFilter() }
                    orderColumn?.let { order(column = it, order = Order.DESCENDING) }
                    limit(count = limit)
                }
                .decodeList<JsonObject>()

        return runCatching { query(columns) }
            .recoverCatching { query(fallbackColumns) }
            .getOrDefault(emptyList())
    }

    // ------------------------------------------------------------------
    // Escopo
    // ------------------------------------------------------------------

    /** `data.entities` do web (`ligas_config`). */
    private suspend fun fetchScopeRows(
        client: SupabaseClient,
        tenantId: String,
        scope: EventBiScopeRef,
    ): List<JsonObject> = queryRows(
        client = client,
        table = CollectivesTable,
        columns = ScopeColumns,
        fallbackColumns = ScopeFallbackColumns,
        limit = if (scope.isCollective) 1L else ScopeRowsLimit,
    ) {
        eq("tenant_id", tenantId)
        if (scope.isCollective) eq("id", scope.cleanId)
    }

    /** `buildEntityMemberIndex` (1701). */
    private fun buildMemberIndex(
        scopeRows: List<JsonObject>,
        scope: EventBiScopeRef,
    ): Map<String, EventBiMemberMeta> = buildEventBiMemberIndex(
        scopeRows.map { row ->
            // `resolvedEntityScopeType` (1764): o escopo travado vence para a própria entidade.
            val resolved = if (scope.isCollective && row.str("id") == scope.cleanId) {
                scope.type
            } else {
                row.entityScopeType()
            }
            row to resolved
        },
    )

    /** `buildEventScopeIndex`: id de evento -> (dono, tipo, nome). */
    private fun buildEventOwnerIndex(rows: List<JsonObject>): Map<String, EventOwner> {
        val index = mutableMapOf<String, EventOwner>()
        rows.forEach { row ->
            val ownerId = row.str("id")
            if (ownerId.isBlank()) return@forEach
            val owner = EventOwner(ownerId, row.entityName(), row.entityScopeType())
            row.linkedEventIds().forEach { eventId -> index[eventId] = owner }
        }
        return index
    }

    /**
     * Eventos do escopo.
     *
     * Coletivo: `id IN (chaves do ligas_config)`. Tenant: os eventos que **não** têm dono
     * entre as entidades e não declaram escopo externo (`isTenantOwnedRow` do web).
     */
    private suspend fun fetchScopedEvents(
        client: SupabaseClient,
        tenantId: String,
        scope: EventBiScopeRef,
        scopeRows: List<JsonObject>,
        eventOwners: Map<String, EventOwner>,
    ): List<EventBiEvent> {
        if (scope.isCollective) {
            val keys = scopeRows.firstOrNull()?.linkedEventIds().orEmpty()
            if (keys.isEmpty()) return emptyList()

            return queryRows(
                client, EventsTable, EventColumns, EventFallbackColumns, EventsLimit,
            ) { isIn("id", keys) }
                .map { it.toEvent(eventOwners) }
                // A entidade dona vem do próprio `ligas_config`, o escopo já está garantido.
                .filter { it.ownerScope == scope.type || it.ownerId == scope.cleanId }
        }

        if (scope.type != EventBiScope.Tenant) {
            // `scopeId === "todos"` com escopo de entidade: `matchesActiveScope` devolve `false`.
            return emptyList()
        }

        return queryRows(
            client, EventsTable, EventColumns, EventFallbackColumns, EventsLimit, orderColumn = "data",
        ) { eq("tenant_id", tenantId) }
            .map { it.toEvent(eventOwners) }
            .filter { it.ownerScope == EventBiScope.Tenant }
    }

    // ------------------------------------------------------------------
    // Transações do escopo
    // ------------------------------------------------------------------

    private suspend fun fetchTickets(
        client: SupabaseClient,
        tenantId: String,
        eventIds: List<String>,
    ): List<EventBiTicket> = queryRows(
        client, TicketsTable, TicketColumns, TicketFallbackColumns, TicketsLimit,
        orderColumn = "dataSolicitacao",
    ) {
        eq("tenant_id", tenantId)
        isIn("eventoId", eventIds)
    }.map { it.toTicket() }

    private suspend fun fetchOrders(
        client: SupabaseClient,
        tenantId: String,
        eventIds: List<String>,
        productIds: List<String>,
    ): List<EventBiOrder> {
        val rows = queryRows(
            client, OrdersTable, OrderColumns, OrderFallbackColumns, OrdersLimit,
            orderColumn = "createdAt",
        ) {
            eq("tenant_id", tenantId)
            or {
                isIn("eventId", eventIds)
                if (productIds.isNotEmpty()) isIn("productId", productIds)
            }
        }
        // Sem a coluna `eventId` o pedido só é alcançado pelo produto do evento.
        val fallback = if (rows.isEmpty() && productIds.isNotEmpty()) {
            queryRows(
                client, OrdersTable, OrderFallbackColumns, OrderFallbackColumns, OrdersLimit,
                orderColumn = "createdAt",
            ) {
                eq("tenant_id", tenantId)
                isIn("productId", productIds)
            }
        } else {
            emptyList()
        }
        return (rows + fallback).map { it.toOrder() }
    }

    private suspend fun fetchRsvps(
        client: SupabaseClient,
        tenantId: String,
        eventIds: List<String>,
        filter: EventBiFilter,
    ): List<EventBiRsvp> {
        if (eventIds.isEmpty()) return emptyList()
        return queryRows(client, RsvpsTable, RsvpColumns, RsvpColumns, RsvpsLimit) {
            eq("tenant_id", tenantId)
            isIn("eventoId", eventIds)
        }
            .map { it.toRsvp() }
            .filter { eventBiDateInPeriod(it.createdAtMillis, filter.startDate, filter.endDate) }
    }

    /**
     * `produtos` do escopo. O vínculo produto -> evento mora em `data.eventParty.eventId`
     * (`productEventId` do web), que não dá para filtrar com segurança no PostgREST;
     * a consulta filtra por vendedor/tenant e o vínculo é resolvido nas linhas trazidas.
     */
    private suspend fun fetchScopedProducts(
        client: SupabaseClient,
        tenantId: String,
        scope: EventBiScopeRef,
        allowedEventIds: Set<String>,
    ): List<EventBiProduct> {
        if (allowedEventIds.isEmpty()) return emptyList()

        return queryRows(client, ProductsTable, ProductColumns, ProductColumns, ProductsLimit) {
            eq("tenant_id", tenantId)
            if (scope.isCollective) eq("seller_id", scope.cleanId)
        }.mapNotNull { row ->
            val eventId = row.productEventId()
            if (eventId.isBlank() || !allowedEventIds.contains(eventId)) return@mapNotNull null
            row.toProduct(eventId)
        }
    }

    /**
     * `userById` (3723). O web baixa 6000 linhas de `users`; aqui só entram os compradores que
     * o recorte cita, e apenas as colunas que `classifyTicketAudience` e `ticketContact` leem.
     */
    private suspend fun fetchUsers(
        client: SupabaseClient,
        tenantId: String,
        userIds: List<String>,
    ): Map<String, JsonObject> {
        if (userIds.isEmpty()) return emptyMap()
        return queryRows(client, UsersTable, UserColumns, UserFallbackColumns, UsersLimit) {
            eq("tenant_id", tenantId)
            isIn("uid", userIds)
        }.associateBy { it.str("uid").ifBlank { it.str("id") } }
    }

    /**
     * `tenantUserCount` (5467): o web conta as linhas que baixou. Aqui é um `count` de
     * cabeçalho — a resposta não traz linha nenhuma.
     */
    private suspend fun fetchTenantUserCount(client: SupabaseClient, tenantId: String): Int =
        runCatching {
            client.from(UsersTable)
                .select(columns = Columns.raw("uid")) {
                    filter { eq("tenant_id", tenantId) }
                    count(Count.PLANNED)
                    limit(count = 1L)
                }
                .countOrNull()
                ?.toInt()
                ?: 0
        }.getOrDefault(0)

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private suspend fun resolveTenantId(client: SupabaseClient, tenantId: String): String {
        val clean = tenantId.trim()
        if (clean.isNotBlank()) return clean
        return runCatching { SupabaseTenantResolver.resolveActiveTenantId(client) }.getOrDefault("")
    }

    private data class EventOwner(val id: String, val name: String, val scope: EventBiScope)

    // ------------------------------------------------------------------
    // Mapeamento das linhas
    // ------------------------------------------------------------------

    /** `canonicalEventOwnerScope`: dono declarado no próprio evento ou herdado da entidade. */
    private fun JsonObject.toEvent(owners: Map<String, EventOwner>): EventBiEvent {
        val stats = obj("stats")
        val declared = declaredExternalScope()
        val linked = owners[str("id")]
        val ownerScope = declared ?: linked?.scope ?: EventBiScope.Tenant
        val ownerId = if (ownerScope == EventBiScope.Tenant) "" else linked?.id.orEmpty()

        return EventBiEvent(
            id = str("id"),
            name = firstText(str("titulo"), str("nome")).ifBlank { "Evento sem nome" },
            startsAtMillis = eventStartMillis(),
            ownerScope = ownerScope,
            ownerId = ownerId,
            ownerName = linked?.name.orEmpty(),
            capacity = eventCapacity(),
            // `eventCardClickCount`/`eventBuyClickCount`: o maior valor entre os apelidos.
            cardClicks = stats.maxInt("cardClicks", "eventCardClicks", "cliquesCard"),
            buyClicks = stats.maxInt(
                "cliquesCompra", "buyClicks", "checkoutClicks", "purchaseClicks", "clicks",
            ),
            confirmedCount = stats.maxInt("confirmados"),
            maybeCount = stats.maxInt("talvez"),
            raw = this,
        )
    }

    private fun JsonObject.toTicket(): EventBiTicket = EventBiTicket(
        id = str("id"),
        eventId = ticketEventId(),
        eventName = str("eventoNome"),
        status = EventBiStatus.classify(statusValue()),
        rawStatus = str("status"),
        quantity = ticketQuantity(),
        value = ticketValue(),
        discount = ticketDiscount(),
        lotName = ticketLotName(),
        buyerId = ticketBuyerId(),
        buyerName = str("userName").ifBlank { "Sem nome" },
        buyerClass = ticketClassName(),
        approver = ticketApproverName(),
        purchasedAtMillis = ticketPurchaseDate(),
        approvedAtMillis = ticketApprovalDate(),
        scannedCount = ticketScannedCount(),
        raw = this,
    )

    private fun JsonObject.toOrder(): EventBiOrder = EventBiOrder(
        id = str("id"),
        eventId = orderEventId(),
        productId = orderProductId(),
        productName = str("productName").ifBlank { "Produto" },
        status = EventBiStatus.classify(statusValue()),
        rawStatus = str("status"),
        quantity = orderQuantity(),
        total = orderTotal(),
        discount = orderDiscount(),
        buyerId = orderBuyerId(),
        buyerName = str("userName").ifBlank { "Sem nome" },
        buyerClass = orderClassName(null),
        approver = orderApproverName(),
        createdAtMillis = orderCreatedAt(),
        raw = this,
    )

    private fun JsonObject.toRsvp(): EventBiRsvp = EventBiRsvp(
        id = str("id"),
        eventId = firstText(str("eventoId"), str("eventId"), str("event_id")),
        userId = str("userId"),
        userClass = str("userTurma").ifBlank { "Sem turma" },
        status = rsvpStatus(str("status")),
        createdAtMillis = parseEventBiDate(
            firstText(str("timestamp"), str("createdAt"), str("created_at")),
        ),
    )

    private fun JsonObject.toProduct(eventId: String): EventBiProduct = EventBiProduct(
        id = str("id"),
        eventId = eventId,
        name = productName(),
        category = str("categoria"),
        lot = str("lote"),
        price = num("preco"),
        stock = num("estoque").toInt(),
        sold = num("vendidos").toInt(),
        clicks = num("cliques").toInt(),
        raw = this,
    )

    private companion object {
        const val CollectivesTable = "ligas_config"
        const val EventsTable = "eventos"
        const val TicketsTable = "solicitacoes_ingressos"
        const val RsvpsTable = "eventos_rsvps"
        const val ProductsTable = "produtos"
        const val OrdersTable = "orders"
        const val UsersTable = "users"

        /** Web: 1000 registros de `ligas_config`. */
        const val ScopeRowsLimit = 200L

        /** Web: 600 eventos do tenant inteiro. */
        const val EventsLimit = 120L

        /** Web: 6000 ingressos do tenant inteiro. */
        const val TicketsLimit = 800L

        /** Web: 12000 RSVPs do tenant inteiro. */
        const val RsvpsLimit = 1200L

        /** Web: 3000 produtos do tenant inteiro. */
        const val ProductsLimit = 300L

        /** Web: 8000 pedidos do tenant inteiro. */
        const val OrdersLimit = 800L

        /** Web: 6000 usuários do tenant inteiro; aqui só os compradores do recorte. */
        const val UsersLimit = 300L

        /** `membros`/`membrosIds` entram no M8.1b para `buildEntityMemberIndex`. */
        const val ScopeColumns =
            "id,tenant_id,nome,sigla,category,categoria,turmaId,eventos,data,membros,membrosIds"
        const val ScopeFallbackColumns = "id,tenant_id,nome,sigla,category,turmaId,eventos,data"

        /** `lotes` e as colunas de custo entram no M8.1b (`eventLotRows`, `eventCost`). */
        const val EventColumns =
            "id,tenant_id,titulo,nome,data,hora,stats,data_extra,status,tipo,categoria," +
                "capacidade,capacity,vagas,lotes,custo,custos,cost,totalCost,custoTotal,valorCusto"
        const val EventFallbackColumns =
            "id,tenant_id,titulo,nome,data,hora,stats,data_extra,status,tipo,categoria," +
                "capacidade,capacity,vagas"

        /**
         * M8.1b acrescenta `data` (auditoria de check-in, origem, desconto, turma),
         * `checkinAuditLog`, o bloco de transferência e o de check-in da própria linha.
         */
        const val TicketColumns =
            "id,tenant_id,eventoId,eventoNome,userId,userName,userTurma,status,loteNome,loteId," +
                "quantidade,valorTotal,valorUnitario,discountValue,discountSource,dataSolicitacao," +
                "dataAprovacao,aprovadoPor,approvalMethod,itemName,itemCategory,source," +
                "checkinAt,checkinBy,checkinByUserName,checkinMethod,checkinAuditLog," +
                "transferAt,transferHistory,transferByUserName,transferFromUserId," +
                "transferFromUserName,transferToUserId,transferToUserName,data,payment_config"
        const val TicketFallbackColumns =
            "id,tenant_id,eventoId,eventoNome,userId,userName,userTurma,status,loteNome," +
                "quantidade,valorTotal,discountValue,dataSolicitacao,dataAprovacao,aprovadoPor," +
                "checkinAt,payment_config"

        const val RsvpColumns = "id,tenant_id,eventoId,userId,userTurma,status,timestamp,createdAt"

        const val ProductColumns =
            "id,tenant_id,nome,preco,categoria,lote,estoque,vendidos,cliques,active,status," +
                "seller_type,seller_id,data"

        /** M8.1b acrescenta `data` (vouchers, transferências, `eventParty`) e a baixa. */
        const val OrderColumns =
            "id,tenant_id,userId,userName,userTurma,productId,productName,price,total," +
                "quantidade,itens,status,approvedBy,createdAt,updatedAt,eventId," +
                "eventDiscountValue,eventDiscountSource,eventApprovalAt,eventApprovalMethod," +
                "eventCreatedManually,eventCreatedByName,eventItemName,eventItemCategory," +
                "eventLoteNome,eventCheckinAt,eventCheckinMethod,eventCheckinByUserName," +
                "seller_type,seller_id,code,codigo,paymentSource,paymentMethod,qrStatus," +
                "data,payment_config"
        const val OrderFallbackColumns =
            "id,tenant_id,userId,userName,userTurma,productId,productName,price,total," +
                "quantidade,itens,status,approvedBy,createdAt,data"

        /** Só o que `classifyTicketAudience`/`ticketContact` leem. */
        const val UserColumns = "uid,id,tenant_id,turma,email,telefone,phone"
        const val UserFallbackColumns = "uid,tenant_id,turma,email"
    }
}

// ------------------------------------------------------------------
// Leitura das linhas cruas
// ------------------------------------------------------------------

/** `entityName` do web (1751): sigla, senão nome, senão id. */
private fun JsonObject.entityName(): String =
    firstText(str("sigla"), str("nome"), str("name")).ifBlank { str("id") }

/** `entityScopeType` do web (1755). */
private fun JsonObject.entityScopeType(): EventBiScope {
    val data = obj("data")
    val declared = listOf(
        str("category"), str("categoria"),
        data.str("category"), data.str("categoria"), data.str("tipo"),
    ).firstOrNull { EventBiScope.fromRemote(it) != EventBiScope.Tenant }

    val fromCategory = EventBiScope.fromRemote(declared)
    if (fromCategory != EventBiScope.Tenant) return fromCategory

    // `asString(row.turmaId || data.turmaId).trim()` marca comissão.
    val turma = firstText(str("turmaId"), data.str("turmaId"))
    return if (turma.isNotBlank()) EventBiScope.Commission else EventBiScope.League
}

/**
 * `entityEventRows` + `linkedEventIdsFromEntityEvent` (1843): cada evento da entidade pode
 * aparecer por `id`, `globalEventId`, `eventId`, `eventoId` ou pelo id embutido em
 * `linkEvento`/`href`/`url`.
 */
private fun JsonObject.linkedEventIds(limit: Int = 60): List<String> {
    val data = obj("data")
    val rows = objects("eventos") + data.objects("eventos") + data.objects("events")

    return rows.flatMap { row ->
        listOf(
            row.str("globalEventId"),
            row.str("eventId"),
            row.str("eventoId"),
            row.str("id"),
            eventIdFromLink(row.str("linkEvento")),
            eventIdFromLink(row.str("href")),
            eventIdFromLink(row.str("url")),
        )
    }.filter { it.isNotBlank() }.distinct().take(limit)
}

/** `eventIdFromLink` do web (1829). */
private fun eventIdFromLink(value: String?): String {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return ""
    val cleanPath = raw.substringBefore('?').substringBefore('#')
    val parts = cleanPath.split('/').map { it.trim() }.filter { it.isNotBlank() }
    val eventIndex = parts.indexOfFirst { it.lowercase(Locale.ROOT) == "eventos" }
    val candidate = if (eventIndex >= 0) parts.getOrNull(eventIndex + 1) else parts.lastOrNull()
    return candidate?.trim().orEmpty()
}

/**
 * `declaredExternalScopeType` do web (1770), reduzido às colunas que a consulta traz
 * (`tipo`, `categoria`, `stats` e `data_extra.eventParty`).
 */
private fun JsonObject.declaredExternalScope(): EventBiScope? {
    val stats = obj("stats")
    val extra = obj("data_extra")
    val party = extra.obj("eventParty")
    return listOf(
        str("tipo"), str("categoria"),
        stats.str("scope_type"), stats.str("scopeType"), stats.str("tipo"), stats.str("categoria"),
        extra.str("scope_type"), extra.str("tipo"), extra.str("categoria"),
        party.str("scope_type"), party.str("tipo"), party.str("categoria"),
    )
        .map { EventBiScope.fromRemote(it) }
        .firstOrNull { it != EventBiScope.Tenant }
}

/** `eventDate` do web (676): junta `data` + `hora` quando a data não vem com `T`. */
private fun JsonObject.eventStartMillis(): Long {
    val date = firstText(str("data"), str("date"), str("startsAt"), str("inicio"))
    val hour = firstText(str("hora"), str("time"), str("horario"))
    if (date.isNotBlank() && hour.isNotBlank() && !date.contains("T")) {
        return parseEventBiDate("${date}T$hour")
    }
    return parseEventBiDate(date.ifBlank { str("createdAt") })
}

/** `rsvpStatus` do web (698). */
private fun rsvpStatus(value: String?): String =
    when (value?.trim()?.lowercase(Locale.ROOT).orEmpty()) {
        "going", "vou", "confirmado" -> "going"
        "maybe", "talvez", "interessado" -> "maybe"
        else -> ""
    }

/** `Math.max(parseNumber(...), ...)` do web: o maior valor entre os apelidos da mesma métrica. */
private fun JsonObject?.maxInt(vararg keys: String): Int =
    keys.maxOfOrNull { key -> num(key).toInt() } ?: 0
