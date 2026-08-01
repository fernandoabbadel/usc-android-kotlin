package com.example.usc1.data.repository

import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.data.supabase.SupabaseTenantResolver
import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventComment
import com.example.usc1.domain.model.EventMenuProduct
import com.example.usc1.domain.model.EventOwnerType
import com.example.usc1.domain.model.EventPartyOrder
import com.example.usc1.domain.model.EventPartyVoucher
import com.example.usc1.domain.model.EventPartyVoucherStatus
import com.example.usc1.domain.model.EventPaymentRecipient
import com.example.usc1.domain.model.EventPoll
import com.example.usc1.domain.model.EventPollOption
import com.example.usc1.domain.model.EventProduct
import com.example.usc1.domain.model.EventRsvp
import com.example.usc1.domain.model.EventRsvpStatus
import com.example.usc1.domain.model.EventStatus
import com.example.usc1.domain.model.EventTicketOrder
import com.example.usc1.domain.model.EventTicketOrderStatus
import com.example.usc1.domain.model.EventVisibility
import com.example.usc1.domain.repository.EventsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.NumberFormat
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SupabaseEventsRepository(
    private val clientProvider: () -> SupabaseClient = { SupabaseClientProvider.client },
) : EventsRepository {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    private val dateLabelFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("pt-BR"))

    override suspend fun getEvents(status: EventStatus?): List<Event> = withContext(Dispatchers.IO) {
        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val rows = client.from(EventsTable)
            .select(columns = Columns.raw(EventFeedColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("status", "ativo")
                }
                order(column = "data", order = Order.ASCENDING)
                limit(count = FetchLimit)
            }
            .decodeList<EventRow>()

        val events = rows.mapNotNull { row -> row.toDomain(tenantId) }
            .filter { event -> status == null || event.status == status }
            .filter { event -> event.status != EventStatus.Closed }
            .sortedBy { event -> event.sortKey() }
            .take(PageSize)

        // Fotos reais de quem confirmou, para os avatares do card do feed.
        val avatarsByEvent = runCatching {
            fetchFeedRsvpPreviews(client, tenantId, events.map(Event::id))
        }.getOrDefault(emptyMap())

        events.map { event ->
            val preview = avatarsByEvent[event.id].orEmpty()
            if (preview.isEmpty()) event else event.copy(rsvps = preview)
        }
    }

    /** Busca uma amostra de RSVPs "going" para exibir avatares na lista de eventos. */
    private suspend fun fetchFeedRsvpPreviews(
        client: SupabaseClient,
        tenantId: String,
        eventIds: List<String>,
    ): Map<String, List<EventRsvp>> {
        val cleanIds = eventIds.map(String::trim).filter(String::isNotBlank).distinct()
        if (cleanIds.isEmpty()) return emptyMap()

        return client.from(EventRsvpsTable)
            .select(columns = Columns.raw(EventRsvpColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("status", EventRsvpStatus.Going.remoteValue)
                    isIn("eventoId", cleanIds)
                }
                limit(count = FeedRsvpPreviewLimit)
            }
            .decodeList<EventRsvpRow>()
            .mapNotNull { row -> row.toDomain()?.let { row.eventoId.trim() to it } }
            .filter { (eventId, _) -> eventId.isNotBlank() }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, rsvps) -> rsvps.take(FeedRsvpPreviewPerEvent) }
    }

    override suspend fun getEventById(
        eventId: String,
        userId: String,
        userPlanNames: List<String>,
        userPlanIds: List<String>,
    ): Event? = withContext(Dispatchers.IO) {
        val cleanEventId = eventId.trim()
        if (cleanEventId.isBlank()) return@withContext null

        val client = clientProvider()
        val tenantId = SupabaseTenantResolver.resolveActiveTenantId(client)
        val event = client.from(EventsTable)
            .select(columns = Columns.raw(EventDetailColumns)) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("id", cleanEventId)
                }
                limit(count = 1)
            }
            .decodeList<EventRow>()
            .firstOrNull()
            ?.toDomain(tenantId, userPlanNames, userPlanIds)
            ?.takeUnless { it.status == EventStatus.Closed }

        if (event == null) {
            return@withContext null
        }

        val menuProducts = if (event.isEventMenuEnabled) {
            fetchEventMenuProducts(
                client = client,
                eventId = event.id,
                tenantId = tenantId,
            )
        } else {
            emptyList()
        }
        val interactions = fetchEventInteractions(
            client = client,
            eventId = event.id,
            tenantId = tenantId,
            viewerUserId = userId,
        )

        event.copy(
            menuProducts = menuProducts,
            viewerRsvpStatus = interactions.viewerRsvpStatus,
            rsvps = interactions.rsvps,
            comments = interactions.comments,
            polls = interactions.polls,
            confirmedCount = if (interactions.rsvpsLoaded) {
                interactions.rsvps.count { it.status == EventRsvpStatus.Going }
            } else {
                event.confirmedCount
            },
            maybeCount = if (interactions.rsvpsLoaded) {
                interactions.rsvps.count { it.status == EventRsvpStatus.Maybe }
            } else {
                event.maybeCount
            },
        )
    }

    override suspend fun createTicketRequest(
        tenantId: String,
        userId: String,
        userName: String,
        userTurma: String,
        event: Event,
        lot: EventProduct,
        quantity: Int,
        userPlanNames: List<String>,
        userPlanIds: List<String>,
        recipient: EventPaymentRecipient?,
    ): String = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanEventId = event.id.trim()
        val cleanLotId = lot.id.trim()
        val safeQuantity = quantity.coerceAtLeast(1)

        if (!SupabaseClientProvider.config.isConfigured) {
            throw IllegalStateException("Supabase não configurado para registrar ingresso.")
        }
        if (cleanTenantId.isBlank()) {
            throw IllegalStateException("Entre em uma atlética para reservar o ingresso.")
        }
        if (cleanUserId.isBlank()) {
            throw IllegalStateException("Entre com sua conta para reservar o ingresso.")
        }
        if (cleanEventId.isBlank() || cleanLotId.isBlank()) {
            throw IllegalStateException("Evento ou lote inválido.")
        }

        val client = clientProvider()
        val eventRow = client.from(EventsTable)
            .select(columns = Columns.raw(EventTicketRequestEventColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("id", cleanEventId)
                }
                limit(count = 1)
            }
            .decodeList<EventTicketRequestEventRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Evento fora do tenant ativo.")

        if (eventRow.isVisibilityBlockedForTicketRequest()) {
            throw IllegalStateException("Este evento está indisponível no momento.")
        }

        val lotObject = eventRow.lotes.asJsonArrayOrEmpty()
            .mapNotNull { it.asJsonObjectOrNull() }
            .firstOrNull { candidate ->
                val candidateId = candidate.stringValue("id")
                candidateId == cleanLotId || candidate.stringValue("nome") == cleanLotId || candidate.stringValue("name") == cleanLotId
            }
            ?: throw IllegalStateException("Este lote não está mais disponível.")

        val baseUnitPrice = lotObject.stringValue("preco")
            .ifBlank { lotObject.stringValue("price") }
            .replace(",", ".")
            .toDoubleOrNull()
            ?: lot.priceValue
        val resolvedUnitPrice = resolveEventPlanScopedPrice(
            basePrice = baseUnitPrice,
            entries = lotObject["planPrices"].asJsonArrayOrEmpty().ifEmpty {
                lotObject["plan_prices"].asJsonArrayOrEmpty()
            },
            userPlanNames = userPlanNames,
            userPlanIds = userPlanIds,
        )
        val total = (resolvedUnitPrice * safeQuantity).roundMoney()
        val discountAmount = ((baseUnitPrice - resolvedUnitPrice).coerceAtLeast(0.0) * safeQuantity).roundMoney()
        val now = LocalDateTime.now().toString()
        val requestId = UUID.randomUUID().toString()
        val paymentConfig = buildEventPaymentConfig(
            remoteConfig = eventRow.paymentConfig,
            event = event,
            selectedRecipient = recipient,
        )
        val lotName = lotObject.stringValue("nome")
            .ifBlank { lotObject.stringValue("name") }
            .ifBlank { lot.name }

        val payload = eventJsonPayloadOf(
            "id" to requestId,
            "tenant_id" to cleanTenantId,
            "userId" to cleanUserId,
            "userName" to userName.trim().ifBlank { "Aluno" },
            "userTurma" to userTurma.trim().ifBlank { "Geral" },
            "eventoId" to cleanEventId,
            "eventoNome" to event.title.trim().ifBlank { "Evento" },
            "loteNome" to lotName.ifBlank { "Lote" },
            "loteId" to cleanLotId,
            "quantidade" to safeQuantity,
            "valorUnitario" to resolvedUnitPrice.toEventCurrencyPayload(),
            "valorTotal" to total.toEventCurrencyPayload(),
            "metodo" to "whatsapp",
            "status" to "pendente",
            "dataSolicitacao" to now,
            "itemType" to "ingresso",
            "itemName" to lotName.ifBlank { "Ingresso" },
            "itemCategory" to inferTicketCategory(lotName),
            "discountValue" to "R$ ${discountAmount.toEventCurrencyPayload()}",
            "discountKind" to if (discountAmount > 0.0) "plano" else "",
            "discountSource" to if (discountAmount > 0.0) {
                "Plano ${userPlanNames.firstOrNull()?.trim()?.takeIf(String::isNotBlank) ?: userPlanIds.firstOrNull()?.trim().orEmpty().ifBlank { "ativo" }}"
            } else {
                ""
            },
            "payment_config" to paymentConfig,
        )

        insertTicketRequestWithOptionalColumnFallback(client, payload)
        requestId
    }

    override suspend fun createEventProductOrder(
        tenantId: String,
        userId: String,
        userName: String,
        event: Event,
        product: EventMenuProduct,
        quantity: Int,
        userPlanNames: List<String>,
        userPlanIds: List<String>,
    ): String = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanEventId = event.id.trim()
        val cleanProductId = product.id.trim()
        val safeQuantity = quantity.coerceAtLeast(1)

        if (!SupabaseClientProvider.config.isConfigured) {
            throw IllegalStateException("Supabase não configurado para registrar produto do evento.")
        }
        if (cleanTenantId.isBlank()) {
            throw IllegalStateException("Entre em uma atlética para comprar no evento.")
        }
        if (cleanUserId.isBlank()) {
            throw IllegalStateException("Entre com sua conta para registrar o pedido.")
        }
        if (cleanEventId.isBlank() || cleanProductId.isBlank()) {
            throw IllegalStateException("Evento ou produto inválido.")
        }

        val client = clientProvider()
        val eventRow = client.from(EventsTable)
            .select(columns = Columns.raw(EventTicketRequestEventColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("id", cleanEventId)
                }
                limit(count = 1)
            }
            .decodeList<EventTicketRequestEventRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Evento fora do tenant ativo.")

        if (eventRow.isVisibilityBlockedForTicketRequest()) {
            throw IllegalStateException("Este evento está indisponível no momento.")
        }

        val productRow = client.from(ProductsTable)
            .select(columns = Columns.raw(EventMenuProductColumns)) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("id", cleanProductId)
                }
                limit(count = 1)
            }
            .decodeList<EventMenuProductRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Produto fora do tenant ativo.")

        if (productRow.eventPartyEventId() != cleanEventId) {
            throw IllegalStateException("Produto não pertence ao menu deste evento.")
        }
        if (productRow.active == false || productRow.aprovado != true) {
            throw IllegalStateException("Produto indisponível no momento.")
        }
        if (productRow.status.orEmpty().normalizeForMatch() in HiddenProductStatuses) {
            throw IllegalStateException("Produto indisponível no momento.")
        }
        val stock = productRow.estoque ?: productRow.data.asJsonObjectOrEmpty().intValue("estoque")
        if (stock > 0 && stock < safeQuantity) {
            throw IllegalStateException("Estoque insuficiente para este produto.")
        }

        val basePrice = productRow.preco ?: product.priceValue
        val resolvedUnitPrice = resolveEventPlanScopedPrice(
            basePrice = basePrice,
            entries = productRow.planPrices.asJsonArrayOrEmpty(),
            userPlanNames = userPlanNames,
            userPlanIds = userPlanIds,
        )
        val total = (resolvedUnitPrice * safeQuantity).roundMoney()
        val now = LocalDateTime.now().toString()
        val orderId = UUID.randomUUID().toString()
        val productSection = productRow.eventPartySection().ifBlank { product.category.ifBlank { "Geral" } }
        val productName = productRow.nome.trim().ifBlank { product.name.ifBlank { "Produto do evento" } }
        val paymentConfig = mergeEventPartyPaymentConfig(
            productConfig = productRow.paymentConfig,
            eventConfig = buildEventPaymentConfig(eventRow.paymentConfig, event),
        )

        val orderData = buildEventProductOrderData(
            event = event,
            productId = cleanProductId,
            productName = productName,
            section = productSection,
            quantity = safeQuantity,
            userId = cleanUserId,
            now = now,
        )

        val insertPayload = eventJsonPayloadOf(
            "id" to orderId,
            "tenant_id" to cleanTenantId,
            "userId" to cleanUserId,
            "userName" to userName.trim().ifBlank { "Aluno" },
            "productId" to cleanProductId,
            "productName" to productName,
            "price" to total,
            "quantidade" to safeQuantity,
            "itens" to safeQuantity,
            "status" to "pendente",
            "approvedBy" to "",
            "payment_config" to paymentConfig,
            "seller_type" to productRow.sellerType?.trim().orEmpty().ifBlank { "tenant" },
            "seller_id" to productRow.sellerId?.trim().orEmpty().ifBlank { cleanTenantId },
            "seller_name" to productRow.sellerName?.trim().orEmpty().ifBlank { event.eventMenuTitle },
            "seller_logo_url" to resolveRemoteImageUrl(productRow.sellerLogoUrl),
            "data" to orderData,
            "createdAt" to now,
            "updatedAt" to now,
        )

        insertEventProductOrderWithOptionalColumnFallback(client, insertPayload)
        syncEventProductOrderColumns(
            client = client,
            orderId = orderId,
            event = event,
            productName = productName,
            productSection = productSection,
            now = now,
        )
        insertEventProductOrderNotification(
            client = client,
            userId = cleanUserId,
            eventId = cleanEventId,
            productId = cleanProductId,
            productName = productName,
            now = now,
        )
        orderId
    }

    override suspend fun setEventRsvp(
        tenantId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        userTurma: String,
        eventId: String,
        status: EventRsvpStatus,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanEventId = eventId.trim()
        if (cleanTenantId.isBlank() || cleanUserId.isBlank() || cleanEventId.isBlank()) {
            throw IllegalStateException("Entre na atlética para responder presença.")
        }

        val client = clientProvider()
        ensureEventInTenant(client, cleanTenantId, cleanEventId)

        val existing = client.from(EventRsvpsTable)
            .select(columns = Columns.raw("id,status")) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("eventoId", cleanEventId)
                    eq("userId", cleanUserId)
                }
                limit(count = 1)
            }
            .decodeList<EventRsvpLookupRow>()
            .firstOrNull()

        val now = LocalDateTime.now().toString()
        val currentStatus = EventRsvpStatus.fromRemote(existing?.status)
        if (currentStatus == status) {
            client.from(EventRsvpsTable)
                .delete {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("eventoId", cleanEventId)
                        eq("userId", cleanUserId)
                    }
                }
        } else if (!existing?.id.isNullOrBlank()) {
            client.from(EventRsvpsTable)
                .update(
                    EventRsvpUpdatePayload(
                        status = status.remoteValue,
                        userName = userName.trim().ifBlank { "Aluno" },
                        userAvatar = userAvatar.trim(),
                        userTurma = userTurma.trim().ifBlank { "Geral" },
                        timestamp = now,
                    ),
                ) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("eventoId", cleanEventId)
                        eq("userId", cleanUserId)
                    }
                }
        } else {
            client.from(EventRsvpsTable)
                .insert(
                    EventRsvpInsertPayload(
                        id = UUID.randomUUID().toString(),
                        tenantId = cleanTenantId,
                        eventoId = cleanEventId,
                        userId = cleanUserId,
                        status = status.remoteValue,
                        userName = userName.trim().ifBlank { "Aluno" },
                        userAvatar = userAvatar.trim(),
                        userTurma = userTurma.trim().ifBlank { "Geral" },
                        timestamp = now,
                    ),
                )
        }
    }

    override suspend fun createEventComment(
        tenantId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        userTurma: String,
        eventId: String,
        text: String,
    ): String = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanEventId = eventId.trim()
        val cleanText = text.trim().take(300)
        if (cleanTenantId.isBlank() || cleanUserId.isBlank() || cleanEventId.isBlank()) {
            throw IllegalStateException("Entre na atlética para comentar.")
        }
        if (cleanText.isBlank()) {
            throw IllegalStateException("Escreva uma mensagem para publicar no mural.")
        }

        val client = clientProvider()
        ensureEventInTenant(client, cleanTenantId, cleanEventId)
        val now = LocalDateTime.now().toString()
        val commentId = UUID.randomUUID().toString()
        val payload = eventJsonPayloadOf(
            "id" to commentId,
            "tenant_id" to cleanTenantId,
            "eventoId" to cleanEventId,
            "userId" to cleanUserId,
            "userName" to userName.trim().ifBlank { "Aluno" },
            "userAvatar" to userAvatar.trim(),
            "userTurma" to userTurma.trim().ifBlank { "Geral" },
            "role" to "",
            "text" to cleanText,
            "likes" to JsonArray(emptyList()),
            "reports" to JsonArray(emptyList()),
            "hidden" to false,
            "createdAt" to now,
            "updatedAt" to now,
        )
        insertEventCommentWithOptionalColumnFallback(client, payload)
        commentId
    }

    override suspend fun voteEventPollOption(
        tenantId: String,
        userId: String,
        userTurma: String,
        eventId: String,
        pollId: String,
        optionIndex: Int,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanEventId = eventId.trim()
        val cleanPollId = pollId.trim()
        if (cleanTenantId.isBlank() || cleanUserId.isBlank() || cleanEventId.isBlank() || cleanPollId.isBlank()) {
            throw IllegalStateException("Entre na atlética para votar na enquete.")
        }
        if (optionIndex < 0) {
            throw IllegalStateException("Opção inválida.")
        }

        val client = clientProvider()
        ensureEventInTenant(client, cleanTenantId, cleanEventId)
        val pollRow = client.from(EventPollsTable)
            .select(columns = Columns.raw("id,eventoId,tenant_id,options,userVotes,voters")) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("eventoId", cleanEventId)
                    eq("id", cleanPollId)
                }
                limit(count = 1)
            }
            .decodeList<EventPollRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Enquete não encontrada.")
        if (optionIndex >= pollRow.options.asJsonArrayOrEmpty().size) {
            throw IllegalStateException("Opção inválida.")
        }

        try {
            val currentVotes = client.from(EventPollVotesTable)
                .select(columns = Columns.raw("id,optionIndex")) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("enqueteId", cleanPollId)
                        eq("userId", cleanUserId)
                    }
                    limit(count = 25)
                }
                .decodeList<EventPollVoteRow>()
            if (currentVotes.any { it.optionIndex == optionIndex }) {
                return@withContext
            }

            client.from(EventPollVotesTable)
                .insert(
                    EventPollVoteInsertPayload(
                        id = UUID.randomUUID().toString(),
                        tenantId = cleanTenantId,
                        pollId = cleanPollId,
                        eventoId = cleanEventId,
                        userId = cleanUserId,
                        optionIndex = optionIndex,
                        userTurma = userTurma.trim().ifBlank { "Geral" },
                        createdAt = LocalDateTime.now().toString(),
                    ),
                )
        } catch (error: Throwable) {
            if (!isMissingEventRelationError(error) && !isDuplicateVoteError(error)) {
                throw error
            }
            if (!isDuplicateVoteError(error)) {
                updateLegacyPollVote(
                    client = client,
                    tenantId = cleanTenantId,
                    eventId = cleanEventId,
                    poll = pollRow,
                    userId = cleanUserId,
                    userTurma = userTurma.trim().ifBlank { "Geral" },
                    optionIndex = optionIndex,
                )
            }
        }
    }

    override suspend fun getViewerTicketOrders(
        tenantId: String,
        userId: String,
        eventId: String,
        limit: Int,
    ): List<EventTicketOrder> = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanEventId = eventId.trim()
        if (cleanTenantId.isBlank() || cleanUserId.isBlank() || cleanEventId.isBlank()) {
            return@withContext emptyList()
        }

        runCatching {
            clientProvider().from(TicketRequestsTable)
                .select(columns = Columns.raw(ViewerTicketOrderColumns)) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("userId", cleanUserId)
                        eq("eventoId", cleanEventId)
                    }
                    order(column = "dataSolicitacao", order = Order.DESCENDING)
                    limit(count = limit.coerceIn(1, 60).toLong())
                }
                .decodeList<ViewerTicketOrderRow>()
                .mapNotNull { it.toDomain(currencyFormatter) }
        }.getOrDefault(emptyList())
    }

    override suspend fun cancelTicketRequest(tenantId: String, requestId: String): Unit =
        withContext(Dispatchers.IO) {
            val cleanTenantId = tenantId.trim()
            val cleanRequestId = requestId.trim()
            if (cleanTenantId.isBlank() || cleanRequestId.isBlank()) {
                throw IllegalStateException("Pedido inválido.")
            }
            clientProvider().from(TicketRequestsTable).delete {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("id", cleanRequestId)
                }
            }
        }

    override suspend fun toggleEventCommentLike(
        tenantId: String,
        eventId: String,
        commentId: String,
        userId: String,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanEventId = eventId.trim()
        val cleanCommentId = commentId.trim()
        val cleanUserId = userId.trim()
        if (cleanTenantId.isBlank() || cleanEventId.isBlank() || cleanCommentId.isBlank() || cleanUserId.isBlank()) {
            throw IllegalStateException("Entre na atlética para curtir comentários.")
        }

        val client = clientProvider()
        val row = loadCommentRow(client, cleanTenantId, cleanEventId, cleanCommentId)
            ?: throw IllegalStateException("Comentário não encontrado.")
        val currentLikes = row.likes.map(String::trim).filter(String::isNotBlank)
        val nextLikes = if (currentLikes.contains(cleanUserId)) {
            currentLikes - cleanUserId
        } else {
            currentLikes + cleanUserId
        }

        client.from(EventCommentsTable).update(
            JsonObject(
                eventJsonPayloadOf(
                    "likes" to JsonArray(nextLikes.map(::JsonPrimitive)),
                    "updatedAt" to LocalDateTime.now().toString(),
                ),
            ),
        ) {
            filter {
                eq("tenant_id", cleanTenantId)
                eq("eventoId", cleanEventId)
                eq("id", cleanCommentId)
            }
        }
    }

    override suspend fun reportEventComment(
        tenantId: String,
        eventId: String,
        commentId: String,
        userId: String,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanEventId = eventId.trim()
        val cleanCommentId = commentId.trim()
        val cleanUserId = userId.trim()
        if (cleanTenantId.isBlank() || cleanEventId.isBlank() || cleanCommentId.isBlank() || cleanUserId.isBlank()) {
            throw IllegalStateException("Entre na atlética para denunciar comentários.")
        }

        val client = clientProvider()
        val row = loadCommentRow(client, cleanTenantId, cleanEventId, cleanCommentId) ?: return@withContext
        val currentReports = row.reports.map(String::trim).filter(String::isNotBlank)
        if (currentReports.contains(cleanUserId)) return@withContext

        client.from(EventCommentsTable).update(
            JsonObject(
                eventJsonPayloadOf(
                    "reports" to JsonArray((currentReports + cleanUserId).map(::JsonPrimitive)),
                    "updatedAt" to LocalDateTime.now().toString(),
                ),
            ),
        ) {
            filter {
                eq("tenant_id", cleanTenantId)
                eq("eventoId", cleanEventId)
                eq("id", cleanCommentId)
            }
        }
    }

    override suspend fun deleteEventComment(
        tenantId: String,
        eventId: String,
        commentId: String,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanEventId = eventId.trim()
        val cleanCommentId = commentId.trim()
        if (cleanTenantId.isBlank() || cleanEventId.isBlank() || cleanCommentId.isBlank()) {
            throw IllegalStateException("Comentário inválido.")
        }

        clientProvider().from(EventCommentsTable).delete {
            filter {
                eq("tenant_id", cleanTenantId)
                eq("eventoId", cleanEventId)
                eq("id", cleanCommentId)
            }
        }
    }

    override suspend fun setEventCommentHidden(
        tenantId: String,
        eventId: String,
        commentId: String,
        hidden: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanEventId = eventId.trim()
        val cleanCommentId = commentId.trim()
        if (cleanTenantId.isBlank() || cleanEventId.isBlank() || cleanCommentId.isBlank()) {
            throw IllegalStateException("Comentário inválido.")
        }

        clientProvider().from(EventCommentsTable).update(
            JsonObject(
                eventJsonPayloadOf(
                    "hidden" to hidden,
                    "updatedAt" to LocalDateTime.now().toString(),
                ),
            ),
        ) {
            filter {
                eq("tenant_id", cleanTenantId)
                eq("eventoId", cleanEventId)
                eq("id", cleanCommentId)
            }
        }
    }

    override suspend fun addEventPollOption(
        tenantId: String,
        eventId: String,
        pollId: String,
        userId: String,
        userName: String,
        userAvatar: String,
        userTurma: String,
        text: String,
    ): Unit = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanEventId = eventId.trim()
        val cleanPollId = pollId.trim()
        val cleanUserId = userId.trim()
        val cleanText = text.trim().take(EventPollOptionMaxChars)
        if (cleanTenantId.isBlank() || cleanEventId.isBlank() || cleanPollId.isBlank() || cleanUserId.isBlank()) {
            throw IllegalStateException("Entre na atlética para sugerir uma resposta.")
        }
        if (cleanText.isBlank()) {
            throw IllegalStateException("Resposta inválida.")
        }

        val client = clientProvider()
        ensureEventInTenant(client, cleanTenantId, cleanEventId)
        val pollRow = client.from(EventPollsTable)
            .select(columns = Columns.raw("id,eventoId,tenant_id,allowUserOptions,options,userVotes,voters")) {
                filter {
                    eq("tenant_id", cleanTenantId)
                    eq("eventoId", cleanEventId)
                    eq("id", cleanPollId)
                }
                limit(count = 1)
            }
            .decodeList<EventPollRow>()
            .firstOrNull()
            ?: throw IllegalStateException("Enquete não encontrada.")

        if (!pollRow.allowUserOptions) {
            throw IllegalStateException("Essa enquete não aceita novas respostas.")
        }

        val currentOptions = pollRow.options.asJsonArrayOrEmpty()
        if (currentOptions.size >= EventPollOptionMaxCount) {
            throw IllegalStateException(
                "Cada enquete aceita no máximo $EventPollOptionMaxCount respostas.",
            )
        }
        val currentOptionObjects = currentOptions.map { it.asJsonObjectOrEmpty() }
        if (currentOptionObjects.any { it.stringValue("text").equals(cleanText, ignoreCase = true) }) {
            throw IllegalStateException("Essa resposta já existe na enquete.")
        }
        if (currentOptionObjects.any { it.stringValue("creatorId") == cleanUserId }) {
            throw IllegalStateException(
                "Cada usuário pode sugerir no máximo uma nova resposta por enquete.",
            )
        }

        val nextOption = JsonObject(
            mapOf(
                "text" to JsonPrimitive(cleanText),
                "votes" to JsonPrimitive(0),
                "creatorId" to JsonPrimitive(cleanUserId),
                "creatorName" to JsonPrimitive(
                    userName.trim().substringBefore(' ').ifBlank { "Anônimo" },
                ),
                "creatorAvatar" to JsonPrimitive(userAvatar.trim()),
                "votesByTurma" to JsonObject(emptyMap()),
            ),
        )
        client.from(EventPollsTable).update(
            JsonObject(
                eventJsonPayloadOf(
                    "options" to JsonArray(currentOptions + nextOption),
                    "updatedAt" to LocalDateTime.now().toString(),
                ),
            ),
        ) {
            filter {
                eq("tenant_id", cleanTenantId)
                eq("eventoId", cleanEventId)
                eq("id", cleanPollId)
            }
        }

        // O web registra o voto do autor na própria resposta que ele acabou de criar.
        voteEventPollOption(
            tenantId = cleanTenantId,
            userId = cleanUserId,
            userTurma = userTurma.trim().ifBlank { "Geral" },
            eventId = cleanEventId,
            pollId = cleanPollId,
            optionIndex = currentOptions.size,
        )
    }

    override suspend fun getViewerEventPartyOrders(
        tenantId: String,
        userId: String,
        eventId: String,
    ): List<EventPartyOrder> = withContext(Dispatchers.IO) {
        val cleanTenantId = tenantId.trim()
        val cleanUserId = userId.trim()
        val cleanEventId = eventId.trim()
        if (cleanTenantId.isBlank() || cleanUserId.isBlank() || cleanEventId.isBlank()) {
            return@withContext emptyList()
        }

        val client = clientProvider()
        val descriptionByProductId = fetchEventMenuProducts(client, cleanEventId, cleanTenantId)
            .associate { it.id to it.description }

        runCatching {
            client.from(OrdersTable)
                .select(columns = Columns.raw(EventPartyOrderColumns)) {
                    filter {
                        eq("tenant_id", cleanTenantId)
                        eq("userId", cleanUserId)
                    }
                    order(column = "createdAt", order = Order.DESCENDING)
                    limit(count = EventPartyOrderFetchLimit)
                }
                .decodeList<EventPartyOrderRow>()
                .mapNotNull { it.toDomain(cleanEventId, currencyFormatter, descriptionByProductId) }
        }.getOrDefault(emptyList())
    }

    private suspend fun loadCommentRow(
        client: SupabaseClient,
        tenantId: String,
        eventId: String,
        commentId: String,
    ): EventCommentRow? = client.from(EventCommentsTable)
        .select(columns = Columns.raw("id,eventoId,userId,likes,reports,hidden,tenant_id")) {
            filter {
                eq("tenant_id", tenantId)
                eq("eventoId", eventId)
                eq("id", commentId)
            }
            limit(count = 1)
        }
        .decodeList<EventCommentRow>()
        .firstOrNull()

    private fun EventRow.toDomain(
        activeTenantId: String,
        userPlanNames: List<String> = emptyList(),
        userPlanIds: List<String> = emptyList(),
    ): Event? {
        val cleanId = id.trim()
        val cleanTenantId = tenantId?.trim().orEmpty()
        if (cleanId.isBlank() || cleanTenantId.isBlank() || isVisibilityBlocked()) return null

        val statsObject = stats.asJsonObjectOrEmpty()
        val ownerType = resolveOwnerType(statsObject)
        val ownerId = when (ownerType) {
            EventOwnerType.Tenant -> cleanTenantId
            EventOwnerType.Liga,
            EventOwnerType.Comissao,
            EventOwnerType.Diretorio -> statsObject.stringValue("leagueId").ifBlank { cleanTenantId }
        }
        val lots = lotes.asJsonArrayOrEmpty()
            .mapNotNull { it.asJsonObjectOrNull() }
            .mapNotNull { it.toLot() }
        val activeLot = lots.firstOrNull { it.status == "ativo" }
        val firstLot = activeLot ?: lots.firstOrNull()
        val normalizedSaleStatus = normalizeSaleStatus(saleStatus, status, activeLot)
        val eventStatus = normalizeEventStatus(status, normalizedSaleStatus)
        val eventDateTime = parseEventDateTime(data, hora)
        val confirmed = statsObject.intValue("confirmados")
        val maybe = statsObject.intValue("talvez")
        val likes = statsObject.intValue("likes")
        val dataExtraObject = dataExtra.asJsonObjectOrEmpty()
        val eventPartyConfig = dataExtraObject.eventPartyConfig(categoria)
        val paymentConfigObject = paymentConfig.asJsonObjectOrEmpty()
        val recipientObject = paymentConfigObject["recipient"].asJsonObjectOrEmpty()
        val visibility = resolveVisibility(statsObject, dataExtraObject)
        val topTurmas = statsObject.stringListValue("topTurmas")
            .ifEmpty { statsObject.stringListValue("turmas") }
            .ifEmpty { dataExtraObject.stringListValue("topTurmas") }
        val pixKey = paymentConfigObject.stringValue("chave")
            .ifBlank { paymentConfigObject.stringValue("pixKey") }
            .ifBlank { pixChave?.trim().orEmpty() }
        val pixBank = paymentConfigObject.stringValue("banco")
            .ifBlank { paymentConfigObject.stringValue("bank") }
            .ifBlank { pixBanco?.trim().orEmpty() }
        val pixHolder = paymentConfigObject.stringValue("titular")
            .ifBlank { paymentConfigObject.stringValue("holder") }
            .ifBlank { pixTitular?.trim().orEmpty() }
        val receiptWhatsapp = paymentConfigObject.stringValue("whatsapp")
            .ifBlank { recipientObject.stringValue("whatsapp") }
            .ifBlank { contatoComprovante?.trim().orEmpty() }
        val receiptName = recipientObject.stringValue("name")
            .ifBlank { recipientObject.stringValue("nome") }
            .ifBlank { paymentConfigObject.stringValue("responsavel") }

        return Event(
            id = cleanId,
            tenantId = cleanTenantId,
            title = titulo.trim().ifBlank { "Evento" },
            description = descricao?.trim().orEmpty(),
            dateLabel = formatDateLabel(data, eventDateTime),
            timeLabel = hora?.trim()?.takeIf { it.isNotBlank() } ?: "00:00",
            rawDate = data?.trim().orEmpty(),
            rawTime = hora?.trim().orEmpty(),
            location = local?.trim().orEmpty().ifBlank { "Local a definir" },
            priceLabel = resolvePriceLabel(normalizedSaleStatus, firstLot),
            status = eventStatus,
            saleStatus = normalizedSaleStatus,
            imageUrl = resolveRemoteImageUrl(imagem),
            coverColorName = resolveCoverLabel(ownerType),
            lotName = firstLot?.name ?: if (normalizedSaleStatus == "em_breve") "Em breve" else "Lote",
            availableSpots = confirmed + maybe,
            ownerType = ownerType,
            ownerId = ownerId,
            ownerName = ownerType.label,
            likesCount = likes,
            confirmedCount = confirmed,
            maybeCount = maybe,
            visibility = visibility,
            isHighlighted = isHighlighted(dataExtraObject),
            isLowStock = isLowStock == true,
            topTurmas = topTurmas,
            pixKey = pixKey,
            pixBank = pixBank,
            pixHolder = pixHolder,
            receiptContactName = receiptName,
            receiptContactWhatsapp = receiptWhatsapp,
            // Eventos de liga não expõem recebedores no web (`effectivePixData`).
            receiptRecipients = if (ownerType == EventOwnerType.Liga) {
                emptyList()
            } else {
                paymentConfigObject["recipients"].asJsonArrayOrEmpty()
                    .mapNotNull { it.asJsonObjectOrNull() }
                    .map { entry ->
                        EventPaymentRecipient(
                            userId = entry.stringValue("userId"),
                            name = entry.stringValue("name").ifBlank { entry.stringValue("nome") },
                            turma = entry.stringValue("turma"),
                            phone = entry.stringValue("phone")
                                .ifBlank { entry.stringValue("whatsapp") },
                        )
                    }
                    .filter { it.name.isNotBlank() || it.phone.isNotBlank() }
            },
            products = lots.map { lot ->
                // `resolveLotePriceInfo` do web: o plano do usuário pode baixar o preço do lote.
                val planPrice = resolveEventPlanScopedPrice(
                    basePrice = lot.price,
                    entries = lot.planPrices,
                    userPlanNames = userPlanNames,
                    userPlanIds = userPlanIds,
                )
                val hasDiscount = planPrice < lot.price
                EventProduct(
                    id = lot.id.ifBlank { lot.name },
                    name = lot.name,
                    priceValue = planPrice,
                    priceLabel = currencyFormatter.format(planPrice),
                    status = lot.status,
                    basePriceLabel = if (hasDiscount) lot.priceLabel(currencyFormatter) else "",
                    planBenefitLabel = if (hasDiscount) {
                        "Benefício ${userPlanNames.firstOrNull()?.trim().orEmpty().ifBlank { "do seu plano" }}"
                    } else {
                        ""
                    },
                )
            },
            isEventMenuEnabled = eventPartyConfig.enabled,
            eventMenuTitle = eventPartyConfig.title,
            eventMenuCategory = eventPartyConfig.category,
        )
    }

    private suspend fun fetchEventMenuProducts(
        client: SupabaseClient,
        eventId: String,
        tenantId: String,
    ): List<EventMenuProduct> {
        return runCatching {
            client.from(ProductsTable)
                .select(columns = Columns.raw(EventMenuProductColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                    }
                    order(column = "createdAt", order = Order.ASCENDING)
                    limit(count = EventMenuProductFetchLimit)
                }
                .decodeList<EventMenuProductRow>()
                .asSequence()
                .filter { row -> row.eventPartyEventId() == eventId }
                .filter { row -> row.active != false && row.aprovado == true }
                .filterNot { row -> row.status.orEmpty().normalizeForMatch() in HiddenProductStatuses }
                .sortedWith(
                    compareBy<EventMenuProductRow>(
                        { row -> row.eventPartySection().normalizeForMatch() },
                        { row -> row.eventPartyOrder() },
                        { row -> row.nome.normalizeForMatch() },
                    ),
                )
                .map { row -> row.toMenuProduct(currencyFormatter) }
                .toList()
        }.getOrElse {
            emptyList()
        }
    }

    private suspend fun fetchEventInteractions(
        client: SupabaseClient,
        eventId: String,
        tenantId: String,
        viewerUserId: String,
    ): EventInteractions {
        val rsvpRows = runCatching {
            client.from(EventRsvpsTable)
                .select(columns = Columns.raw(EventRsvpColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                        eq("eventoId", eventId)
                    }
                    limit(count = EventDetailsRsvpsLimit)
                }
                .decodeList<EventRsvpRow>()
        }
        val rsvps = rsvpRows.getOrNull()
            ?.mapNotNull { it.toDomain() }
            .orEmpty()
        val viewerStatus = viewerUserId.trim().takeIf(String::isNotBlank)?.let { cleanViewerId ->
            rsvps.firstOrNull { it.userId == cleanViewerId }?.status
        }

        // O web carrega todos os comentários e só esconde os ocultos na renderização,
        // porque o admin precisa ver e restaurar o que foi ocultado.
        val comments = runCatching {
            client.from(EventCommentsTable)
                .select(columns = Columns.raw(EventCommentColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                        eq("eventoId", eventId)
                    }
                    order(column = "createdAt", order = Order.DESCENDING)
                    limit(count = EventDetailsCommentsLimit)
                }
                .decodeList<EventCommentRow>()
                .mapNotNull { it.toDomain(viewerUserId) }
        }.getOrDefault(emptyList())

        val pollRows = runCatching {
            client.from(EventPollsTable)
                .select(columns = Columns.raw(EventPollColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                        eq("eventoId", eventId)
                    }
                    order(column = "createdAt", order = Order.DESCENDING)
                    limit(count = EventDetailsPollsLimit)
                }
                .decodeList<EventPollRow>()
        }.getOrDefault(emptyList())

        val votesByPoll = fetchEventPollVotes(
            client = client,
            tenantId = tenantId,
            pollIds = pollRows.map { it.id },
        )
        val polls = pollRows.map { row ->
            row.toDomain(
                viewerUserId = viewerUserId,
                relationalVotes = votesByPoll[row.id].orEmpty(),
            )
        }

        return EventInteractions(
            rsvps = rsvps,
            comments = comments,
            polls = polls,
            viewerRsvpStatus = viewerStatus,
            rsvpsLoaded = rsvpRows.isSuccess,
        )
    }

    private suspend fun fetchEventPollVotes(
        client: SupabaseClient,
        tenantId: String,
        pollIds: List<String>,
    ): Map<String, List<EventPollVoteRow>> {
        val cleanPollIds = pollIds.map(String::trim).filter(String::isNotBlank).distinct()
        if (cleanPollIds.isEmpty()) return emptyMap()
        return runCatching {
            client.from(EventPollVotesTable)
                .select(columns = Columns.raw(EventPollVoteColumns)) {
                    filter {
                        eq("tenant_id", tenantId)
                        isIn("enqueteId", cleanPollIds)
                    }
                    limit(count = EventDetailsPollVotesLimit)
                }
                .decodeList<EventPollVoteRow>()
                .groupBy { it.pollId }
        }.getOrDefault(emptyMap())
    }

    private suspend fun ensureEventInTenant(
        client: SupabaseClient,
        tenantId: String,
        eventId: String,
    ) {
        val exists = client.from(EventsTable)
            .select(columns = Columns.raw("id")) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("id", eventId)
                }
                limit(count = 1)
            }
            .decodeList<EventIdRow>()
            .isNotEmpty()
        if (!exists) {
            throw IllegalStateException("Evento fora do tenant ativo.")
        }
    }

    private suspend fun updateLegacyPollVote(
        client: SupabaseClient,
        tenantId: String,
        eventId: String,
        poll: EventPollRow,
        userId: String,
        userTurma: String,
        optionIndex: Int,
    ) {
        val options = poll.options.asJsonArrayOrEmpty().toMutableList()
        val currentVotes = poll.userVotes.asJsonObjectOrEmpty().intListValue(userId)
        if (currentVotes.contains(optionIndex)) return

        val target = options.getOrNull(optionIndex).asJsonObjectOrEmpty()
        val nextTarget = LinkedHashMap<String, JsonElement>().also { map ->
            target.forEach { (key, value) -> map[key] = value }
        }
        val nextVotes = target.intValue("votes") + 1
        val votesByTurma = target.objectValue("votesByTurma").toMutableMap()
        votesByTurma[userTurma] = JsonPrimitive((votesByTurma[userTurma]?.jsonPrimitive?.intOrNull ?: 0) + 1)
        nextTarget["votes"] = JsonPrimitive(nextVotes)
        nextTarget["votesByTurma"] = JsonObject(votesByTurma)
        options[optionIndex] = JsonObject(nextTarget)

        val userVotes = poll.userVotes.asJsonObjectOrEmpty().toMutableMap()
        userVotes[userId] = JsonArray((currentVotes + optionIndex).distinct().map { JsonPrimitive(it) })
        val voters = (poll.voters + userId).filter(String::isNotBlank).distinct()

        client.from(EventPollsTable)
            .update(
                EventPollUpdatePayload(
                    options = JsonArray(options),
                    userVotes = JsonObject(userVotes),
                    voters = voters,
                    updatedAt = LocalDateTime.now().toString(),
                ),
            ) {
                filter {
                    eq("tenant_id", tenantId)
                    eq("eventoId", eventId)
                    eq("id", poll.id)
                }
            }
    }

    private fun EventMenuProductRow.toMenuProduct(formatter: NumberFormat): EventMenuProduct {
        val stock = estoque ?: data.asJsonObjectOrEmpty().intValue("estoque")
        val normalizedStatus = status?.trim().orEmpty().ifBlank {
            if (active == false) "indisponivel" else "disponivel"
        }

        return EventMenuProduct(
            id = id,
            name = nome.trim().ifBlank { "Produto do evento" },
            description = descricao?.trim().orEmpty(),
            category = eventPartySection()
                .ifBlank { categoria?.trim().orEmpty() }
                .ifBlank { "Menu do evento" },
            imageUrl = resolveRemoteImageUrl(img),
            priceValue = preco ?: 0.0,
            priceLabel = formatter.format(preco ?: 0.0),
            status = normalizedStatus,
            stockLabel = when {
                stock == null -> normalizedStatus
                stock <= 0 -> "Esgotado"
                stock == 1 -> "1 unidade"
                else -> "$stock unidades"
            },
            stockCount = stock ?: 0,
            orderIndex = eventPartyOrder(),
        )
    }

    private fun EventMenuProductRow.eventPartyEventId(): String {
        return data.asJsonObjectOrEmpty()
            .objectValue("eventParty")
            .stringValue("eventId")
    }

    private fun EventMenuProductRow.eventPartySection(): String {
        val eventParty = data.asJsonObjectOrEmpty().objectValue("eventParty")
        return eventParty.stringValue("section")
            .ifBlank { eventParty.stringValue("categoryName") }
            .ifBlank { eventParty.stringValue("categoria") }
            .ifBlank { categoria?.trim().orEmpty() }
    }

    private fun EventMenuProductRow.eventPartyOrder(): Int {
        return data.asJsonObjectOrEmpty()
            .objectValue("eventParty")
            .intValue("order")
    }

    private fun EventRow.resolveVisibility(
        statsObject: JsonObject,
        dataExtraObject: JsonObject,
    ): EventVisibility {
        val raw = listOf(
            statsObject.stringValue("leagueEventVisibility"),
            statsObject.stringValue("eventVisibility"),
            statsObject.stringValue("tenantEventVisibility"),
            dataExtraObject.stringValue("visibility"),
            dataExtraObject.stringValue("eventVisibility"),
            dataExtraObject.stringValue("publico"),
            tipo,
            categoria,
        ).joinToString(" ").normalizeForMatch()

        return when {
            raw.contains("interno") || raw.contains("internal") || raw.contains("privado") -> EventVisibility.Internal
            else -> EventVisibility.Public
        }
    }

    private fun EventRow.isHighlighted(dataExtraObject: JsonObject): Boolean {
        val raw = listOf(
            destaque,
            dataExtraObject.stringValue("destaque"),
            dataExtraObject.stringValue("highlight"),
        ).joinToString(" ").normalizeForMatch()
        return raw.contains("destaque") ||
            raw.contains("highlight") ||
            dataExtraObject.booleanValue("destaque") ||
            dataExtraObject.booleanValue("highlight")
    }

    private fun EventRow.isVisibilityBlocked(): Boolean {
        val dataExtra = dataExtra.asJsonObjectOrEmpty()
        val statsObject = stats.asJsonObjectOrEmpty()
        val blocked = dataExtra.booleanValue("adminVisibilityBlock") ||
            dataExtra.booleanValue("visibilityBlocked") ||
            statsObject.booleanValue("adminVisibilityBlock") ||
            statsObject.booleanValue("visibilityBlocked")
        return blocked
    }

    private fun EventRow.resolveOwnerType(statsObject: JsonObject): EventOwnerType {
        val raw = listOf(
            tipo,
            categoria,
            statsObject.stringValue("owner_type"),
            statsObject.stringValue("organizer_type"),
            statsObject.stringValue("scope_type"),
            statsObject.stringValue("eventScope"),
        ).joinToString(" ").normalizeForMatch()

        return when {
            raw.contains("diretorio") || raw.contains("directory") -> EventOwnerType.Diretorio
            raw.contains("comissao") || raw.contains("commission") || raw.contains("formatura") -> EventOwnerType.Comissao
            raw.contains("liga") || raw.contains("league") || statsObject.stringValue("leagueId").isNotBlank() -> EventOwnerType.Liga
            else -> EventOwnerType.Tenant
        }
    }

    private fun normalizeSaleStatus(
        saleStatus: String?,
        status: String?,
        activeLot: EventLot?,
    ): String {
        val cleanSaleStatus = saleStatus?.trim()?.lowercase().orEmpty()
        val cleanStatus = status?.trim()?.lowercase().orEmpty()
        return when {
            cleanStatus in ClosedStatuses -> "esgotado"
            cleanSaleStatus == "em_breve" || cleanSaleStatus == "agendado" -> "em_breve"
            cleanSaleStatus == "esgotado" || cleanSaleStatus == "encerrado" -> "esgotado"
            activeLot != null -> "ativo"
            cleanSaleStatus == "ativo" -> "ativo"
            else -> "em_breve"
        }
    }

    private fun normalizeEventStatus(status: String?, saleStatus: String): EventStatus {
        val cleanStatus = status?.trim()?.lowercase().orEmpty()
        return when {
            cleanStatus in ClosedStatuses -> EventStatus.Closed
            saleStatus == "em_breve" -> EventStatus.ComingSoon
            saleStatus == "esgotado" -> EventStatus.SoldOut
            else -> EventStatus.Open
        }
    }

    private fun resolvePriceLabel(saleStatus: String, lot: EventLot?): String {
        if (saleStatus == "em_breve") return "Em breve"
        if (lot == null) return if (saleStatus == "esgotado") "Esgotado" else "Em breve"
        if (lot.status == "esgotado") return "Esgotado"
        return lot.priceLabel(currencyFormatter)
    }

    private fun formatDateLabel(rawDate: String?, dateTime: LocalDateTime?): String {
        if (dateTime != null) {
            return dateTime.toLocalDate().format(dateLabelFormatter).replace(".", "")
        }
        return rawDate?.trim().orEmpty().ifBlank { "Data a definir" }
    }

    private fun Event.sortKey(): Long = parseEventDateTime(rawDate.ifBlank { dateLabel }, rawTime.ifBlank { timeLabel })
        ?.toLocalDate()
        ?.toEpochDay()
        ?: Long.MAX_VALUE

    private fun parseEventDateTime(rawDate: String?, rawTime: String?): LocalDateTime? {
        val cleanDate = rawDate?.trim().orEmpty()
        if (cleanDate.isBlank()) return null
        val cleanTime = rawTime?.trim().orEmpty().ifBlank { "00:00" }
        val time = runCatching { LocalTime.parse(cleanTime.take(5)) }.getOrDefault(LocalTime.MIDNIGHT)

        DateFormats.forEach { formatter ->
            try {
                return LocalDate.parse(cleanDate, formatter).atTime(time)
            } catch (_: DateTimeParseException) {
            }
        }

        val parts = cleanDate.normalizeForMatch().split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            val day = parts[0].toIntOrNull()
            val month = MonthMap[parts[1].take(3)]
            val year = parts.getOrNull(2)?.toIntOrNull() ?: Year.now().value
            if (day != null && month != null) {
                return runCatching { LocalDate.of(year, month, day).atTime(time) }.getOrNull()
            }
        }
        return null
    }

    private fun JsonObject.toLot(): EventLot? {
        val name = stringValue("nome").ifBlank { stringValue("name") }
        if (name.isBlank()) return null
        val price = stringValue("preco").ifBlank { stringValue("price") }
        return EventLot(
            id = stringValue("id"),
            name = name,
            price = price.replace(",", ".").toDoubleOrNull() ?: 0.0,
            status = stringValue("status").ifBlank { "ativo" }.lowercase(),
            planPrices = this["planPrices"].asJsonArrayOrEmpty()
                .ifEmpty { this["plan_prices"].asJsonArrayOrEmpty() },
        )
    }

    private fun EventLot.priceLabel(formatter: NumberFormat): String = formatter.format(price)

    private fun resolveCoverLabel(ownerType: EventOwnerType): String {
        return ownerType.label
    }

    private companion object {
        const val PageSize = 24
        const val FetchLimit = 72L
        const val EventMenuProductFetchLimit = 300L
        const val EventsTable = "eventos"
        const val ProductsTable = "produtos"
        const val OrdersTable = "orders"
        const val EventPartyOrderFetchLimit = 300L
        const val TicketRequestsTable = "solicitacoes_ingressos"
        const val EventRsvpsTable = "eventos_rsvps"
        const val EventCommentsTable = "eventos_comentarios"
        const val EventPollsTable = "eventos_enquetes"
        const val EventPollVotesTable = "eventos_enquete_votos"
        const val EventDetailsRsvpsLimit = 200L
        const val FeedRsvpPreviewLimit = 400L
        const val FeedRsvpPreviewPerEvent = 6
        const val EventDetailsCommentsLimit = 100L
        const val EventDetailsPollsLimit = 20L
        const val EventDetailsPollVotesLimit = 600L
        const val EventFeedColumns =
            "id,titulo,data,hora,local,imagem,tipo,categoria,destaque,status,sale_status,isLowStock,stats,lotes,data_extra,capacidade,tenant_id,createdAt,updatedAt"
        const val EventDetailColumns =
            "id,titulo,descricao,data,hora,local,imagem,imagePositionY,tipo,categoria,destaque,mapsUrl,status,sale_status,payment_config,pixChave,pixBanco,pixTitular,contatoComprovante,isLowStock,stats,lotes,data_extra,capacidade,tenant_id,createdAt,updatedAt"
        const val ViewerTicketOrderColumns =
            "id,eventoId,eventoNome,userId,status,loteNome,quantidade,valorTotal,payment_config,dataSolicitacao,dataAprovacao,createdAt"
        const val EventPartyOrderColumns =
            "id,tenant_id,userId,productId,productName,price,total,quantidade,itens,status,createdAt,updatedAt,data"
        const val EventPollOptionMaxChars = 60
        const val EventPollOptionMaxCount = 20
        const val EventTicketRequestEventColumns =
            "id,tenant_id,lotes,stats,data_extra,payment_config,pixChave,pixBanco,pixTitular,contatoComprovante"
        const val EventMenuProductColumns =
            "id,tenant_id,nome,preco,img,descricao,categoria,estoque,active,aprovado,status,payment_config,plan_prices,seller_type,seller_id,seller_name,seller_logo_url,data,createdAt,updatedAt"
        const val EventRsvpColumns =
            "id,eventoId,userId,status,userName,userAvatar,userTurma,timestamp,tenant_id"
        const val EventCommentColumns =
            "id,eventoId,userId,userName,userAvatar,userTurma,role,text,likes,reports,hidden,createdAt,updatedAt,tenant_id"
        const val EventPollColumns =
            "id,eventoId,question,allowUserOptions,options,userVotes,voters,createdAt,updatedAt,tenant_id"
        const val EventPollVoteColumns =
            "id,enqueteId,eventoId,userId,optionIndex,userTurma,tenant_id,createdAt"
        val ClosedStatuses = setOf("encerrado", "cancelado", "inativo")
        val HiddenProductStatuses = setOf("rascunho", "inativo", "arquivado", "removido", "reprovado")
        val DateFormats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        )
        val MonthMap = mapOf(
            "jan" to 1,
            "fev" to 2,
            "mar" to 3,
            "abr" to 4,
            "mai" to 5,
            "jun" to 6,
            "jul" to 7,
            "ago" to 8,
            "set" to 9,
            "out" to 10,
            "nov" to 11,
            "dez" to 12,
        )
    }
}

private data class EventLot(
    val id: String,
    val name: String,
    val price: Double,
    val status: String,
    val planPrices: JsonArray = JsonArray(emptyList()),
)

private data class EventPartyConfig(
    val enabled: Boolean,
    val title: String,
    val category: String,
)

@Serializable
private data class EventTicketRequestEventRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val lotes: JsonElement? = null,
    val stats: JsonElement? = null,
    @SerialName("data_extra") val dataExtra: JsonElement? = null,
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
    @SerialName("pixChave") val pixChave: String? = null,
    @SerialName("pixBanco") val pixBanco: String? = null,
    @SerialName("pixTitular") val pixTitular: String? = null,
    val contatoComprovante: String? = null,
) {
    fun isVisibilityBlockedForTicketRequest(): Boolean {
        val data = dataExtra.asJsonObjectOrEmpty()
        val statsObject = stats.asJsonObjectOrEmpty()
        return data.booleanValue("adminVisibilityBlock") ||
            data.booleanValue("visibilityBlocked") ||
            statsObject.booleanValue("adminVisibilityBlock") ||
            statsObject.booleanValue("visibilityBlocked")
    }
}

@Serializable
private data class EventRow(
    val id: String = "",
    val titulo: String = "",
    val descricao: String? = null,
    val data: String? = null,
    val hora: String? = null,
    val local: String? = null,
    val imagem: String? = null,
    val tipo: String? = null,
    val categoria: String? = null,
    val destaque: String? = null,
    val status: String? = null,
    @SerialName("sale_status") val saleStatus: String? = null,
    @SerialName("isLowStock") val isLowStock: Boolean? = null,
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
    val pixChave: String? = null,
    val pixBanco: String? = null,
    val pixTitular: String? = null,
    val contatoComprovante: String? = null,
    val stats: JsonElement? = null,
    val lotes: JsonElement? = null,
    @SerialName("data_extra") val dataExtra: JsonElement? = null,
    val capacidade: Int? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
private data class EventMenuProductRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val nome: String = "",
    val preco: Double? = null,
    val img: String? = null,
    val descricao: String? = null,
    val categoria: String? = null,
    val estoque: Int? = null,
    val active: Boolean? = null,
    val aprovado: Boolean? = null,
    val status: String? = null,
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
    @SerialName("plan_prices") val planPrices: JsonElement? = null,
    @SerialName("seller_type") val sellerType: String? = null,
    @SerialName("seller_id") val sellerId: String? = null,
    @SerialName("seller_name") val sellerName: String? = null,
    @SerialName("seller_logo_url") val sellerLogoUrl: String? = null,
    val data: JsonElement? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

private data class EventInteractions(
    val rsvps: List<EventRsvp> = emptyList(),
    val comments: List<EventComment> = emptyList(),
    val polls: List<EventPoll> = emptyList(),
    val viewerRsvpStatus: EventRsvpStatus? = null,
    val rsvpsLoaded: Boolean = false,
)

@Serializable
private data class EventIdRow(
    val id: String = "",
)

@Serializable
private data class EventRsvpLookupRow(
    val id: String = "",
    val status: String? = null,
)

@Serializable
private data class EventRsvpInsertPayload(
    val id: String,
    @SerialName("tenant_id") val tenantId: String,
    val eventoId: String,
    val userId: String,
    val status: String,
    val userName: String,
    val userAvatar: String,
    val userTurma: String,
    val timestamp: String,
)

@Serializable
private data class EventRsvpUpdatePayload(
    val status: String,
    val userName: String,
    val userAvatar: String,
    val userTurma: String,
    val timestamp: String,
)

@Serializable
private data class EventRsvpRow(
    val id: String = "",
    val eventoId: String = "",
    val userId: String = "",
    val status: String = "",
    val userName: String? = null,
    val userAvatar: String? = null,
    val userTurma: String? = null,
    val timestamp: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
) {
    fun toDomain(): EventRsvp? {
        val cleanUserId = userId.trim()
        val normalizedStatus = EventRsvpStatus.fromRemote(status) ?: return null
        return EventRsvp(
            id = id.trim(),
            userId = cleanUserId,
            status = normalizedStatus,
            userName = userName?.trim().orEmpty().ifBlank { "Aluno" },
            userAvatar = resolveRemoteImageUrl(userAvatar),
            userTurma = userTurma?.trim().orEmpty(),
            timestampLabel = formatEventDateTimeLabel(timestamp),
        )
    }
}

@Serializable
private data class EventCommentRow(
    val id: String = "",
    val eventoId: String = "",
    val userId: String = "",
    val userName: String? = null,
    val userAvatar: String? = null,
    val userTurma: String? = null,
    val role: String? = null,
    val text: String? = null,
    val likes: List<String> = emptyList(),
    val reports: List<String> = emptyList(),
    val hidden: Boolean = false,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
) {
    fun toDomain(viewerUserId: String): EventComment? {
        val cleanText = text?.trim().orEmpty()
        if (cleanText.isBlank()) return null
        val viewerId = viewerUserId.trim()
        return EventComment(
            id = id.trim(),
            userId = userId.trim(),
            userName = userName?.trim().orEmpty().ifBlank { "Aluno" },
            userAvatar = resolveRemoteImageUrl(userAvatar),
            userTurma = userTurma?.trim().orEmpty(),
            role = role?.trim().orEmpty(),
            text = cleanText,
            likesCount = likes.size,
            createdAtLabel = formatEventDateTimeLabel(createdAt),
            likedByViewer = viewerId.isNotBlank() && likes.any { it.trim() == viewerId },
            reportedByViewer = viewerId.isNotBlank() && reports.any { it.trim() == viewerId },
            hidden = hidden,
        )
    }
}

@Serializable
private data class EventCommentInsertPayload(
    val id: String,
    @SerialName("tenant_id") val tenantId: String,
    val eventoId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val userTurma: String,
    val role: String = "",
    val text: String,
    val likes: List<String> = emptyList(),
    val reports: List<String> = emptyList(),
    val hidden: Boolean = false,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
)

@Serializable
private data class EventPollRow(
    val id: String = "",
    val eventoId: String = "",
    val question: String? = null,
    val allowUserOptions: Boolean = false,
    val options: JsonElement? = null,
    val userVotes: JsonElement? = null,
    val voters: List<String> = emptyList(),
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
) {
    fun toDomain(
        viewerUserId: String,
        relationalVotes: List<EventPollVoteRow>,
    ): EventPoll {
        val viewerId = viewerUserId.trim()
        val relationalStats = relationalVotes
            .mapNotNull { row -> row.optionIndex?.let { index -> row to index } }
            .filter { (_, index) -> index >= 0 }
        val hasRelationalVotes = relationalStats.isNotEmpty()
        val baseOptions = options.asJsonArrayOrEmpty().map { element ->
            element.asJsonObjectOrEmpty()
        }
        val optionRows = baseOptions.mapIndexed { index, option ->
            if (hasRelationalVotes) {
                val votesForIndex = relationalStats.filter { (_, voteIndex) -> voteIndex == index }.map { it.first }
                EventPollOption(
                    label = option.stringValue("text")
                        .ifBlank { option.stringValue("label") }
                        .ifBlank { "Opção ${index + 1}" },
                    votes = votesForIndex.size,
                    votesByTurma = votesForIndex
                        .groupingBy { it.userTurma?.trim().orEmpty().ifBlank { "Geral" } }
                        .eachCount(),
                    creatorId = option.stringValue("creatorId"),
                    creatorName = option.stringValue("creatorName"),
                    creatorAvatar = resolveRemoteImageUrl(option.stringValue("creatorAvatar")).orEmpty(),
                )
            } else {
                EventPollOption(
                    label = option.stringValue("text")
                        .ifBlank { option.stringValue("label") }
                        .ifBlank { "Opção ${index + 1}" },
                    votes = option.intValue("votes"),
                    votesByTurma = option.objectValue("votesByTurma").intMap(),
                    creatorId = option.stringValue("creatorId"),
                    creatorName = option.stringValue("creatorName"),
                    creatorAvatar = resolveRemoteImageUrl(option.stringValue("creatorAvatar")).orEmpty(),
                )
            }
        }
        val relationalVoters = relationalVotes.map { it.userId.trim() }.filter(String::isNotBlank).distinct()
        val viewerVotes = if (viewerId.isBlank()) {
            emptyList()
        } else if (hasRelationalVotes) {
            relationalVotes
                .filter { it.userId.trim() == viewerId }
                .mapNotNull { it.optionIndex }
                .filter { it >= 0 }
                .distinct()
        } else {
            userVotes.asJsonObjectOrEmpty().intListValue(viewerId)
        }

        return EventPoll(
            id = id.trim(),
            question = question?.trim().orEmpty().ifBlank { "Enquete da galera" },
            allowUserOptions = allowUserOptions,
            options = optionRows,
            viewerVotes = viewerVotes,
            votersCount = if (hasRelationalVotes) relationalVoters.size else voters.size,
            createdAtLabel = formatEventDateTimeLabel(createdAt),
        )
    }
}

@Serializable
private data class EventPollUpdatePayload(
    val options: JsonArray,
    val userVotes: JsonObject,
    val voters: List<String>,
    @SerialName("updatedAt") val updatedAt: String,
)

@Serializable
private data class EventPollVoteInsertPayload(
    val id: String,
    @SerialName("tenant_id") val tenantId: String,
    @SerialName("enqueteId") val pollId: String,
    val eventoId: String,
    val userId: String,
    val optionIndex: Int,
    val userTurma: String,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
private data class EventPollVoteRow(
    val id: String = "",
    @SerialName("enqueteId") val pollId: String = "",
    val eventoId: String? = null,
    val userId: String = "",
    val optionIndex: Int? = null,
    val userTurma: String? = null,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
private data class ViewerTicketOrderRow(
    val id: String = "",
    val eventoId: String = "",
    val eventoNome: String? = null,
    val userId: String = "",
    val status: String = "",
    val loteNome: String? = null,
    val quantidade: Int = 1,
    val valorTotal: String? = null,
    @SerialName("payment_config") val paymentConfig: JsonElement? = null,
    val dataSolicitacao: String? = null,
    val dataAprovacao: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
) {
    fun toDomain(formatter: NumberFormat): EventTicketOrder? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val payment = paymentConfig.asJsonObjectOrEmpty()
        val recipient = payment["recipient"].asJsonObjectOrEmpty()
        val total = valorTotal?.trim().orEmpty().parseEventCurrency()
        return EventTicketOrder(
            id = cleanId,
            eventId = eventoId.trim(),
            lotName = loteNome?.trim().orEmpty().ifBlank { "Ingresso" },
            quantity = quantidade.coerceAtLeast(1),
            totalLabel = formatter.format(total),
            status = EventTicketOrderStatus.fromRemote(status),
            requestedAtLabel = formatEventDateTimeLabel(dataSolicitacao ?: createdAt),
            approvedAtLabel = formatEventDateTimeLabel(dataAprovacao),
            pixKey = payment.stringValue("chave"),
            pixBank = payment.stringValue("banco"),
            pixHolder = payment.stringValue("titular"),
            receiptWhatsapp = payment.stringValue("whatsapp")
                .ifBlank { recipient.stringValue("phone") },
            recipientName = recipient.stringValue("name").ifBlank { recipient.stringValue("nome") },
            recipientTurma = recipient.stringValue("turma"),
        )
    }
}

@Serializable
private data class EventPartyOrderRow(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String? = null,
    val userId: String = "",
    @SerialName("productId") val productId: String? = null,
    @SerialName("productName") val productName: String? = null,
    val price: Double = 0.0,
    val total: Double? = null,
    val quantidade: Int? = null,
    val itens: Int? = null,
    val status: String = "",
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    val data: JsonElement? = null,
) {
    fun toDomain(
        eventId: String,
        formatter: NumberFormat,
        descriptionByProductId: Map<String, String>,
    ): EventPartyOrder? {
        val cleanId = id.trim()
        if (cleanId.isBlank()) return null
        val eventParty = data.asJsonObjectOrEmpty().objectValue("eventParty")
        if (eventParty.stringValue("eventId") != eventId) return null

        val cleanProductId = productId?.trim().orEmpty()
        val quantity = (quantidade ?: itens ?: 1).coerceAtLeast(1)
        // `normalizeEventPartyVoucherStatus`: só pedido aprovado/pago libera as fichas.
        val orderApproved = status.trim().lowercase(Locale.ROOT) in ApprovedEventPartyStatuses
        val legacyStatus = EventPartyVoucherStatus.fromRemote(
            eventParty.stringValue("voucherStatus"),
            if (orderApproved) EventPartyVoucherStatus.Active else EventPartyVoucherStatus.Pending,
        )
        val codePrefix = cleanId.take(8).uppercase()
        val rawEntries = eventParty["voucherEntries"].asJsonArrayOrEmpty()
            .ifEmpty { eventParty["vouchers"].asJsonArrayOrEmpty() }
        val vouchers = rawEntries.mapIndexed { index, element ->
            val entry = element.asJsonObjectOrEmpty()
            val entryStatus = EventPartyVoucherStatus.fromRemote(entry.stringValue("status"), legacyStatus)
            val manualNumber = entry.stringValue("manualNumber")
                .ifBlank { entry.stringValue("fichaNumero") }
                .ifBlank { entry.stringValue("numeroFicha") }
            val rawLabel = entry.stringValue("label")
            val voucherId = entry.stringValue("id")
                .ifBlank { entry.stringValue("voucherId") }
                .ifBlank { entry.stringValue("token") }
                .ifBlank { "item-${index + 1}" }
            EventPartyVoucher(
                id = voucherId,
                label = when {
                    manualNumber.isNotBlank() -> "Ficha $manualNumber"
                    rawLabel.isNotBlank() && !ManualFichaLabelRegex.matches(rawLabel) -> rawLabel
                    else -> "Ficha digital"
                },
                status = if (
                    orderApproved &&
                    entryStatus == EventPartyVoucherStatus.Pending &&
                    legacyStatus == EventPartyVoucherStatus.Active
                ) {
                    EventPartyVoucherStatus.Active
                } else {
                    entryStatus
                },
                code = entry.stringValue("code")
                    .ifBlank { entry.stringValue("codigo") }
                    .ifBlank { manualNumber }
                    .ifBlank { "$codePrefix-${index + 1}" },
                manualNumber = manualNumber,
                qrPayload = buildEventPartyVoucherQrPayload(
                    orderId = cleanId,
                    eventId = eventId,
                    productId = cleanProductId,
                    voucherId = voucherId,
                ),
                transferStatus = entry.stringValue("transferStatus"),
                transferredToUserName = entry.stringValue("transferredToUserName"),
                transferredFromUserName = entry.stringValue("transferredFromUserName"),
            )
        }.take(quantity)

        val filled = if (vouchers.size >= quantity) {
            vouchers
        } else {
            vouchers + (vouchers.size until quantity).map { index ->
                EventPartyVoucher(
                    id = "item-${index + 1}",
                    label = "Ficha digital",
                    status = legacyStatus,
                    code = "$codePrefix-${index + 1}",
                    qrPayload = buildEventPartyVoucherQrPayload(
                        orderId = cleanId,
                        eventId = eventId,
                        productId = cleanProductId,
                        voucherId = "item-${index + 1}",
                    ),
                )
            }
        }

        val usedCount = filled.count {
            it.status == EventPartyVoucherStatus.Used || it.status == EventPartyVoucherStatus.Inactive
        }
        val orderCode = eventParty.stringValue("orderCode")
            .ifBlank { eventParty.stringValue("orderNumber") }
            .ifBlank { eventParty.stringValue("pedidoCodigo") }
            .ifBlank { codePrefix }
        val voucherCodes = filled.flatMap { listOf(it.manualNumber, it.code) }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
        val fichaCode = eventParty.stringValue("manualCode")
            .ifBlank { eventParty.stringValue("manualNumber") }
            .ifBlank { voucherCodes.firstOrNull().orEmpty() }
            .ifBlank { orderCode }
        val extraCodes = voucherCodes.filterNot { it == fichaCode }
        val summary = listOf(
            "Pedido #$orderCode",
            if (fichaCode.isNotBlank()) "Ficha $fichaCode" else "",
            if (extraCodes.isNotEmpty()) "Código ${extraCodes.joinToString(" / ")}" else "",
        ).filter(String::isNotBlank).joinToString(" • ")

        val cleanProductName = productName?.trim().orEmpty()
            .ifBlank { eventParty.stringValue("productName") }
            .ifBlank { "Produto do evento" }

        return EventPartyOrder(
            id = cleanId,
            eventId = eventId,
            productId = cleanProductId,
            productName = cleanProductName,
            productDescription = descriptionByProductId[cleanProductId].orEmpty(),
            quantity = quantity,
            totalLabel = formatter.format(total ?: price),
            status = if (!orderApproved) {
                EventPartyVoucherStatus.Pending
            } else {
                resolveEventPartyOrderStatus(filled, usedCount)
            },
            referenceSummary = summary,
            usedCount = usedCount,
            totalCount = filled.size,
            vouchers = filled,
        )
    }

    private companion object {
        val ApprovedEventPartyStatuses = setOf("approved", "aprovado", "paid", "pago", "delivered")
        val ManualFichaLabelRegex = Regex("^ficha\\s+\\d+$", RegexOption.IGNORE_CASE)
    }
}

/** Espelha `getEventPartyVoucherSummary` de `eventPartyService.ts`. */
private fun resolveEventPartyOrderStatus(
    vouchers: List<EventPartyVoucher>,
    usedCount: Int,
): EventPartyVoucherStatus {
    if (vouchers.isEmpty()) return EventPartyVoucherStatus.Pending
    val total = vouchers.size
    val inactive = vouchers.count {
        it.status == EventPartyVoucherStatus.Used ||
            it.status == EventPartyVoucherStatus.Inactive ||
            it.status == EventPartyVoucherStatus.Cancelled ||
            it.status == EventPartyVoucherStatus.Transferred ||
            it.status == EventPartyVoucherStatus.Reversed ||
            it.status == EventPartyVoucherStatus.Refunded
    }
    fun allWith(target: EventPartyVoucherStatus) = vouchers.all { it.status == target }

    if (inactive >= total) {
        return when {
            usedCount >= total -> EventPartyVoucherStatus.Used
            allWith(EventPartyVoucherStatus.Transferred) -> EventPartyVoucherStatus.Transferred
            allWith(EventPartyVoucherStatus.Cancelled) -> EventPartyVoucherStatus.Cancelled
            allWith(EventPartyVoucherStatus.Reversed) -> EventPartyVoucherStatus.Reversed
            allWith(EventPartyVoucherStatus.Refunded) -> EventPartyVoucherStatus.Refunded
            else -> EventPartyVoucherStatus.Inactive
        }
    }
    return if (usedCount > 0) EventPartyVoucherStatus.Partial else EventPartyVoucherStatus.Active
}

/** Espelha `buildEventProductVoucherQrPayload` de `qrPayloads.ts`. */
private fun buildEventPartyVoucherQrPayload(
    orderId: String,
    eventId: String,
    productId: String,
    voucherId: String,
): String {
    val payload = LinkedHashMap<String, JsonElement>().apply {
        put("t", JsonPrimitive("evento-produto"))
        put("v", JsonPrimitive(1))
        put("orderId", JsonPrimitive(orderId))
        put("eventId", JsonPrimitive(eventId))
        put("productId", JsonPrimitive(productId))
        if (voucherId.isNotBlank()) put("voucherId", JsonPrimitive(voucherId))
        put("ts", JsonPrimitive(System.currentTimeMillis()))
    }
    return JsonObject(payload).toString()
}

private fun String.parseEventCurrency(): Double {
    val sanitized = trim().replace(Regex("[^\\d,.-]"), "")
    if (sanitized.isBlank()) return 0.0
    val normalized = if (sanitized.contains(',')) {
        sanitized.replace(".", "").replace(",", ".")
    } else {
        sanitized
    }
    return normalized.toDoubleOrNull() ?: 0.0
}

private fun JsonElement?.asJsonObjectOrEmpty(): JsonObject {
    return when (this) {
        is JsonObject -> this
        else -> JsonObject(emptyMap())
    }
}

private fun JsonElement?.asJsonArrayOrEmpty(): JsonArray {
    return when (this) {
        is JsonArray -> this
        else -> JsonArray(emptyList())
    }
}

private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
    return when (this) {
        is JsonObject -> this
        else -> null
    }
}

private fun JsonObject.objectValue(key: String): JsonObject {
    return when (val value = this[key]) {
        is JsonObject -> value
        else -> JsonObject(emptyMap())
    }
}

private fun JsonObject.eventPartyConfig(fallbackCategory: String?): EventPartyConfig {
    val eventParty = objectValue("eventParty")
    val isEnabled = eventParty.booleanValue("enabled") ||
        eventParty.stringValue("menuTitle").isNotBlank() ||
        eventParty.stringValue("cardapioTitle").isNotBlank() ||
        eventParty.stringValue("categoryName").isNotBlank() ||
        eventParty.stringValue("categoria").isNotBlank()
    val title = eventParty.stringValue("menuTitle")
        .ifBlank { eventParty.stringValue("cardapioTitle") }
        .ifBlank { eventParty.stringValue("title") }
        .ifBlank { "Menu do evento" }
    val category = eventParty.stringValue("categoryName")
        .ifBlank { eventParty.stringValue("categoria") }
        .ifBlank { fallbackCategory?.trim().orEmpty() }
        .ifBlank { "Menu do evento" }

    return EventPartyConfig(
        enabled = isEnabled,
        title = title,
        category = category,
    )
}

private fun JsonObject.stringValue(key: String): String {
    val value = this[key] ?: return ""
    if (value is JsonNull) return ""
    return value.jsonPrimitive.contentOrNull.orEmpty().trim()
}

private fun JsonObject.intValue(key: String): Int {
    val value = this[key] ?: return 0
    if (value is JsonNull) return 0
    return value.jsonPrimitive.intOrNull ?: value.jsonPrimitive.doubleOrNull?.toInt() ?: 0
}

private fun JsonObject.stringListValue(key: String): List<String> {
    val value = this[key] ?: return emptyList()
    if (value is JsonNull) return emptyList()
    return when (value) {
        is JsonArray -> value.mapNotNull { item ->
            if (item is JsonNull) null else item.jsonPrimitive.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        }
        else -> value.jsonPrimitive.contentOrNull
            ?.split(",", ";", "|")
            ?.mapNotNull { it.trim().takeIf(String::isNotBlank) }
            .orEmpty()
    }
}

private fun JsonObject.intListValue(key: String): List<Int> {
    val value = this[key] ?: return emptyList()
    if (value is JsonNull) return emptyList()
    return when (value) {
        is JsonArray -> value.mapNotNull { item ->
            if (item is JsonNull) null else item.jsonPrimitive.intOrNull ?: item.jsonPrimitive.doubleOrNull?.toInt()
        }
        else -> value.jsonPrimitive.contentOrNull
            ?.split(",", ";", "|")
            ?.mapNotNull { it.trim().toIntOrNull() }
            .orEmpty()
    }
}

private fun JsonObject.intMap(): Map<String, Int> {
    return entries.mapNotNull { (key, value) ->
        if (value is JsonNull) {
            null
        } else {
            key to (value.jsonPrimitive.intOrNull ?: value.jsonPrimitive.doubleOrNull?.toInt() ?: 0)
        }
    }.toMap()
}

private fun JsonObject.booleanValue(key: String): Boolean {
    val value = this[key] ?: return false
    if (value is JsonNull) return false
    return value.jsonPrimitive.booleanOrNull ?: false
}

private fun eventJsonPayloadOf(
    vararg pairs: Pair<String, Any?>,
): LinkedHashMap<String, JsonElement> {
    val payload = LinkedHashMap<String, JsonElement>()
    pairs.forEach { (key, value) ->
        val element = value.toEventJsonElementOrNull() ?: return@forEach
        payload[key] = element
    }
    return payload
}

private fun Any?.toEventJsonElementOrNull(): JsonElement? {
    return when (this) {
        null -> null
        is JsonElement -> this.takeUnless { it is JsonNull }
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Int -> JsonPrimitive(this)
        is Long -> JsonPrimitive(this)
        is Float -> JsonPrimitive(this)
        is Double -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this.toDouble())
        else -> JsonPrimitive(toString())
    }
}

private suspend fun insertEventCommentWithOptionalColumnFallback(
    client: SupabaseClient,
    payload: LinkedHashMap<String, JsonElement>,
) {
    val nonRemovableColumns = setOf(
        "id",
        "tenant_id",
        "eventoId",
        "userId",
        "text",
    )
    val attemptedRemovedColumns = mutableSetOf<String>()
    var mutablePayload = LinkedHashMap(payload)

    while (mutablePayload.isNotEmpty()) {
        try {
            client.from("eventos_comentarios").insert(JsonObject(mutablePayload))
            return
        } catch (error: Throwable) {
            val missingColumn = extractEventProblematicColumn(error)
            if (
                missingColumn.isNullOrBlank() ||
                missingColumn in nonRemovableColumns ||
                missingColumn in attemptedRemovedColumns ||
                !mutablePayload.containsKey(missingColumn)
            ) {
                throw error
            }
            attemptedRemovedColumns += missingColumn
            mutablePayload = LinkedHashMap(mutablePayload).also { it.remove(missingColumn) }
        }
    }
}

private suspend fun insertTicketRequestWithOptionalColumnFallback(
    client: SupabaseClient,
    payload: LinkedHashMap<String, JsonElement>,
) {
    val nonRemovableColumns = setOf(
        "id",
        "tenant_id",
        "userId",
        "userName",
        "eventoId",
        "eventoNome",
        "loteId",
        "loteNome",
        "quantidade",
        "valorUnitario",
        "valorTotal",
        "status",
    )
    val attemptedRemovedColumns = mutableSetOf<String>()
    var mutablePayload = LinkedHashMap(payload)

    while (mutablePayload.isNotEmpty()) {
        try {
            client.from("solicitacoes_ingressos").insert(JsonObject(mutablePayload))
            return
        } catch (error: Throwable) {
            val missingColumn = extractEventProblematicColumn(error)
            if (
                missingColumn.isNullOrBlank() ||
                missingColumn in nonRemovableColumns ||
                missingColumn in attemptedRemovedColumns ||
                !mutablePayload.containsKey(missingColumn)
            ) {
                throw error
            }
            attemptedRemovedColumns += missingColumn
            mutablePayload = LinkedHashMap(mutablePayload).also { it.remove(missingColumn) }
        }
    }
}

private suspend fun insertEventProductOrderWithOptionalColumnFallback(
    client: SupabaseClient,
    payload: LinkedHashMap<String, JsonElement>,
) {
    val nonRemovableColumns = setOf(
        "id",
        "tenant_id",
        "userId",
        "userName",
        "productId",
        "productName",
        "price",
        "status",
    )
    val attemptedRemovedColumns = mutableSetOf<String>()
    var mutablePayload = LinkedHashMap(payload)

    while (mutablePayload.isNotEmpty()) {
        try {
            client.from("orders").insert(JsonObject(mutablePayload))
            return
        } catch (error: Throwable) {
            val missingColumn = extractEventProblematicColumn(error)
            if (
                missingColumn.isNullOrBlank() ||
                missingColumn in nonRemovableColumns ||
                missingColumn in attemptedRemovedColumns ||
                !mutablePayload.containsKey(missingColumn)
            ) {
                throw error
            }
            attemptedRemovedColumns += missingColumn
            mutablePayload = LinkedHashMap(mutablePayload).also { it.remove(missingColumn) }
        }
    }
}

private suspend fun syncEventProductOrderColumns(
    client: SupabaseClient,
    orderId: String,
    event: Event,
    productName: String,
    productSection: String,
    now: String,
) {
    val payload = eventJsonPayloadOf(
        "eventId" to event.id,
        "eventItemType" to "produto",
        "eventItemName" to productName,
        "eventLoteNome" to "-",
        "eventItemCategory" to productSection.ifBlank { "Geral" },
        "eventDiscountValue" to "R$ 0,00",
        "eventDiscountKind" to "",
        "eventDiscountSource" to "",
        "eventCreatedManually" to false,
        "eventCreatedByName" to "",
        "updatedAt" to now,
    )
    updateEventProductOrderWithOptionalColumnFallback(client, orderId, payload)
}

private suspend fun updateEventProductOrderWithOptionalColumnFallback(
    client: SupabaseClient,
    orderId: String,
    payload: LinkedHashMap<String, JsonElement>,
) {
    val attemptedRemovedColumns = mutableSetOf<String>()
    var mutablePayload = LinkedHashMap(payload)

    while (mutablePayload.isNotEmpty()) {
        try {
            client.from("orders").update(JsonObject(mutablePayload)) {
                filter {
                    eq("id", orderId)
                }
            }
            return
        } catch (error: Throwable) {
            val missingColumn = extractEventProblematicColumn(error)
            if (
                missingColumn.isNullOrBlank() ||
                missingColumn in attemptedRemovedColumns ||
                !mutablePayload.containsKey(missingColumn)
            ) {
                return
            }
            attemptedRemovedColumns += missingColumn
            mutablePayload = LinkedHashMap(mutablePayload).also { it.remove(missingColumn) }
        }
    }
}

private suspend fun insertEventProductOrderNotification(
    client: SupabaseClient,
    userId: String,
    eventId: String,
    productId: String,
    productName: String,
    now: String,
) {
    runCatching {
        client.from("notifications").insert(
            EventProductOrderNotificationInsertPayload(
                userId = userId,
                title = "Pedido em análise",
                message = "Seu pedido de $productName foi enviado para aprovação no evento.",
                link = "/eventos/$eventId/produtos/$productId",
                read = false,
                type = "order",
                createdAt = now,
            ),
        )
    }
}

@Serializable
private data class EventProductOrderNotificationInsertPayload(
    val userId: String,
    val title: String,
    val message: String,
    val link: String,
    val read: Boolean,
    val type: String,
    @SerialName("createdAt") val createdAt: String,
)

private fun buildEventProductOrderData(
    event: Event,
    productId: String,
    productName: String,
    section: String,
    quantity: Int,
    userId: String,
    now: String,
): JsonObject {
    val codePrefix = userId.take(8).uppercase(Locale.ROOT).ifBlank { "FICHA" }
    return JsonObject(
        mapOf(
            "eventParty" to JsonObject(
                mapOf(
                    "eventId" to JsonPrimitive(event.id),
                    "eventTitle" to JsonPrimitive(event.title),
                    "productId" to JsonPrimitive(productId),
                    "productName" to JsonPrimitive(productName),
                    "section" to JsonPrimitive(section.ifBlank { "Geral" }),
                    "voucherStatus" to JsonPrimitive("pendente"),
                    "voucherEntries" to buildEventPartyVoucherEntries(quantity, codePrefix),
                    "createdAt" to JsonPrimitive(now),
                ),
            ),
        ),
    )
}

private fun buildEventPartyVoucherEntries(quantity: Int, codePrefix: String): JsonArray {
    return JsonArray(
        List(quantity.coerceAtLeast(1)) { index ->
            val number = index + 1
            JsonObject(
                mapOf(
                    "id" to JsonPrimitive("item-$number"),
                    "label" to JsonPrimitive("Ficha digital"),
                    "status" to JsonPrimitive("pendente"),
                    "code" to JsonPrimitive("$codePrefix-$number"),
                    "manualNumber" to JsonPrimitive(""),
                    "usedAt" to JsonPrimitive(""),
                    "usedByUserId" to JsonPrimitive(""),
                    "usedByUserName" to JsonPrimitive(""),
                    "usedMethod" to JsonPrimitive(""),
                    "transferStatus" to JsonPrimitive(""),
                    "transferredAt" to JsonPrimitive(""),
                    "transferredToUserId" to JsonPrimitive(""),
                    "transferredToUserName" to JsonPrimitive(""),
                    "transferredFromUserId" to JsonPrimitive(""),
                    "transferredFromUserName" to JsonPrimitive(""),
                ),
            )
        },
    )
}

private fun mergeEventPartyPaymentConfig(
    productConfig: JsonElement?,
    eventConfig: JsonElement?,
): JsonObject? {
    val productObject = productConfig.asJsonObjectOrEmpty()
    val eventObject = eventConfig.asJsonObjectOrEmpty()
    if (productObject.isEmpty() && eventObject.isEmpty()) return null
    if (productObject.isEmpty()) return eventObject
    if (eventObject.isEmpty()) return productObject

    val merged = LinkedHashMap<String, JsonElement>()
    eventObject.forEach { (key, value) -> merged[key] = value }
    productObject.forEach { (key, value) -> merged[key] = value }

    fun preferredText(key: String): String {
        return productObject.stringValue(key).ifBlank { eventObject.stringValue(key) }
    }

    listOf("chave", "banco", "titular", "whatsapp").forEach { key ->
        preferredText(key).takeIf(String::isNotBlank)?.let { merged[key] = JsonPrimitive(it) }
    }
    val productRecipient = productObject.objectValue("recipient")
    val eventRecipient = eventObject.objectValue("recipient")
    when {
        productRecipient.isNotEmpty() -> merged["recipient"] = productRecipient
        eventRecipient.isNotEmpty() -> merged["recipient"] = eventRecipient
    }
    return JsonObject(merged).takeIf { it.isNotEmpty() }
}

private fun buildEventPaymentConfig(
    remoteConfig: JsonElement?,
    event: Event,
    selectedRecipient: EventPaymentRecipient? = null,
): JsonObject? {
    val remoteObject = remoteConfig.asJsonObjectOrEmpty()
    val recipient = remoteObject.objectValue("recipient")
    val pixKey = remoteObject.stringValue("chave")
        .ifBlank { remoteObject.stringValue("pixKey") }
        .ifBlank { event.pixKey }
    val bank = remoteObject.stringValue("banco")
        .ifBlank { remoteObject.stringValue("bank") }
        .ifBlank { event.pixBank }
    val holder = remoteObject.stringValue("titular")
        .ifBlank { remoteObject.stringValue("holder") }
        .ifBlank { event.pixHolder }
    // Quando o comprador escolhe o recebedor, o web sobrescreve recipient e whatsapp.
    val whatsapp = selectedRecipient?.phone?.trim().orEmpty()
        .ifBlank { remoteObject.stringValue("whatsapp") }
        .ifBlank { recipient.stringValue("whatsapp") }
        .ifBlank { event.receiptContactWhatsapp }
    val name = selectedRecipient?.name?.trim().orEmpty()
        .ifBlank { recipient.stringValue("name") }
        .ifBlank { recipient.stringValue("nome") }
        .ifBlank { event.receiptContactName }
    val turma = selectedRecipient?.turma?.trim().orEmpty()
        .ifBlank { recipient.stringValue("turma") }
    val recipientUserId = selectedRecipient?.userId?.trim().orEmpty()
        .ifBlank { recipient.stringValue("userId") }

    val entries = linkedMapOf<String, JsonElement>()
    if (pixKey.isNotBlank()) entries["chave"] = JsonPrimitive(pixKey)
    if (bank.isNotBlank()) entries["banco"] = JsonPrimitive(bank)
    if (holder.isNotBlank()) entries["titular"] = JsonPrimitive(holder)
    if (whatsapp.isNotBlank()) entries["whatsapp"] = JsonPrimitive(whatsapp)
    if (name.isNotBlank() || whatsapp.isNotBlank()) {
        val recipientEntries = linkedMapOf<String, JsonElement>()
        if (name.isNotBlank()) recipientEntries["name"] = JsonPrimitive(name)
        if (turma.isNotBlank()) recipientEntries["turma"] = JsonPrimitive(turma)
        if (recipientUserId.isNotBlank()) recipientEntries["userId"] = JsonPrimitive(recipientUserId)
        if (whatsapp.isNotBlank()) {
            recipientEntries["whatsapp"] = JsonPrimitive(whatsapp)
            recipientEntries["phone"] = JsonPrimitive(whatsapp)
        }
        entries["recipient"] = JsonObject(recipientEntries)
    }
    return JsonObject(entries).takeIf { it.isNotEmpty() }
}

private fun inferTicketCategory(lotName: String): String {
    val normalized = lotName.normalizeForMatch()
    return when {
        normalized.contains("nao aluno") || normalized.contains("nao estudante") -> "Não aluno"
        normalized.contains("aluno") || normalized.contains("estudante") -> "Aluno"
        normalized.contains("vip") -> "VIP"
        normalized.contains("camarote") -> "Camarote"
        else -> "Ingresso"
    }
}

private fun resolveEventPlanScopedPrice(
    basePrice: Double,
    entries: List<JsonElement>,
    userPlanNames: List<String>,
    userPlanIds: List<String>,
): Double {
    val referenceKeys = buildEventPlanReferenceKeys(userPlanNames + userPlanIds)
    if (referenceKeys.isEmpty() || entries.isEmpty()) return basePrice

    return entries.firstNotNullOfOrNull { element ->
        val row = element.asJsonObjectOrNull() ?: return@firstNotNullOfOrNull null
        val planId = row.stringValue("planId").ifBlank { row.stringValue("id") }
        val planName = row.stringValue("planName").ifBlank { row.stringValue("nome") }
        val entryKeys = buildEventPlanReferenceKeys(listOf(planId, planName))
        if (entryKeys.none { it in referenceKeys }) return@firstNotNullOfOrNull null
        row.stringValue("price").ifBlank { row.stringValue("preco") }
            .replace(",", ".")
            .toDoubleOrNull()
    } ?: basePrice
}

private fun buildEventPlanReferenceKeys(values: List<String>): Set<String> {
    return values
        .flatMap { value ->
            val clean = value.trim()
            val baseId = clean.substringAfterLast("::", clean)
            listOf(clean, baseId, clean.normalizeForMatch().replace(" ", ""), baseId.normalizeForMatch().replace(" ", ""))
        }
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter { it.isNotBlank() }
        .toSet()
}

private fun extractEventProblematicColumn(error: Throwable): String? {
    val message = generateSequence(error) { it.cause }
        .joinToString("\n") { it.message.orEmpty() }
    val patterns = listOf(
        Regex("column\\s+[a-z0-9_]+\\.([a-zA-Z0-9_]+)\\s+does not exist", RegexOption.IGNORE_CASE),
        Regex("column\\s+[\"']?([a-zA-Z0-9_]+)[\"']?\\s+does not exist", RegexOption.IGNORE_CASE),
        Regex("could not find the [\"']?([a-zA-Z0-9_]+)[\"']? column", RegexOption.IGNORE_CASE),
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        pattern.find(message)?.groupValues?.getOrNull(1)
    }
}

private fun isMissingEventRelationError(error: Throwable): Boolean {
    val message = generateSequence(error) { it.cause }
        .joinToString("\n") { it.message.orEmpty() }
        .normalizeForMatch()
    return message.contains("eventos enquete votos") ||
        message.contains("relation") && message.contains("does not exist") ||
        message.contains("schema cache") ||
        message.contains("not found")
}

private fun isDuplicateVoteError(error: Throwable): Boolean {
    val message = generateSequence(error) { it.cause }
        .joinToString("\n") { it.message.orEmpty() }
        .normalizeForMatch()
    return message.contains("23505") || message.contains("duplicate") || message.contains("unique")
}

private fun formatEventDateTimeLabel(raw: String?): String {
    val clean = raw?.trim().orEmpty()
    if (clean.isBlank()) return ""
    return clean
        .replace("T", " ")
        .replace(Regex("\\.\\d+"), "")
        .take(16)
}

private fun Double.toEventCurrencyPayload(): String = "%.2f".format(Locale.US, this).replace(".", ",")

private fun Double.roundMoney(): Double = kotlin.math.round(this * 100.0) / 100.0

private fun String.normalizeForMatch(): String {
    val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
    return decomposed.trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
