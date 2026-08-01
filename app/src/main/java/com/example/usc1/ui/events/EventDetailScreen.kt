package com.example.usc1.ui.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumPurple
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventComment
import com.example.usc1.domain.model.EventMenuProduct
import com.example.usc1.domain.model.EventPoll
import com.example.usc1.domain.model.EventProduct
import com.example.usc1.domain.model.EventRsvpStatus
import com.example.usc1.domain.model.EventStatus
import com.example.usc1.domain.model.EventTicketOrder
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

@Composable
fun EventDetailScreen(
    state: EventDetailUiState,
    buyerName: String = "",
    buyerTurma: String = "",
    buyerPhone: String = "",
    viewerUserId: String = "",
    onCheckoutClick: (Event, EventProduct) -> Unit,
    onRsvpClick: (EventRsvpStatus) -> Unit,
    onCommentDraftChange: (String) -> Unit,
    onSubmitCommentClick: () -> Unit,
    onVotePollClick: (pollId: String, optionIndex: Int) -> Unit,
    onMenuProductOrderClick: (product: EventMenuProduct, quantity: Int, onSuccess: (String) -> Unit) -> Unit,
    onTicketsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAttendeeClick: (String) -> Unit = {},
    onPollOptionDraftChange: (String) -> Unit = {},
    onSubmitPollOptionClick: (pollId: String) -> Unit = {},
    onCommentLikeClick: (commentId: String) -> Unit = {},
    onCommentReportClick: (commentId: String) -> Unit = {},
    onCommentDeleteClick: (commentId: String) -> Unit = {},
    onCommentHiddenClick: (commentId: String, hidden: Boolean) -> Unit = { _, _ -> },
    onCopyPixClick: (String) -> Unit = {},
    onSendReceiptClick: (EventTicketOrder) -> Unit = {},
    onCancelOrderClick: (EventTicketOrder) -> Unit = {},
    onOpenOrderClick: (EventTicketOrder) -> Unit = {},
    onEventMenuClick: () -> Unit = {},
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando evento", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Evento",
                subtitle = "Erro ao carregar detalhes",
                icon = Icons.Outlined.Event,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Evento indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Event,
            )
        }
        state.event != null -> EventDetailLoadedContent(
            state = state,
            event = state.event,
            buyerName = buyerName,
            buyerTurma = buyerTurma,
            buyerPhone = buyerPhone,
            viewerUserId = viewerUserId,
            onCheckoutClick = onCheckoutClick,
            onRsvpClick = onRsvpClick,
            onCommentDraftChange = onCommentDraftChange,
            onSubmitCommentClick = onSubmitCommentClick,
            onVotePollClick = onVotePollClick,
            onMenuProductOrderClick = onMenuProductOrderClick,
            onTicketsClick = onTicketsClick,
            onBackClick = onBackClick,
            onAttendeeClick = onAttendeeClick,
            onPollOptionDraftChange = onPollOptionDraftChange,
            onSubmitPollOptionClick = onSubmitPollOptionClick,
            onCommentLikeClick = onCommentLikeClick,
            onCommentReportClick = onCommentReportClick,
            onCommentDeleteClick = onCommentDeleteClick,
            onCommentHiddenClick = onCommentHiddenClick,
            onCopyPixClick = onCopyPixClick,
            onSendReceiptClick = onSendReceiptClick,
            onCancelOrderClick = onCancelOrderClick,
            onOpenOrderClick = onOpenOrderClick,
            onEventMenuClick = onEventMenuClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun EventDetailLoadedContent(
    state: EventDetailUiState,
    event: Event,
    buyerName: String,
    buyerTurma: String,
    buyerPhone: String,
    viewerUserId: String,
    onCheckoutClick: (Event, EventProduct) -> Unit,
    onRsvpClick: (EventRsvpStatus) -> Unit,
    onCommentDraftChange: (String) -> Unit,
    onSubmitCommentClick: () -> Unit,
    onVotePollClick: (pollId: String, optionIndex: Int) -> Unit,
    onMenuProductOrderClick: (product: EventMenuProduct, quantity: Int, onSuccess: (String) -> Unit) -> Unit,
    onTicketsClick: () -> Unit,
    onBackClick: () -> Unit,
    onAttendeeClick: (String) -> Unit,
    onPollOptionDraftChange: (String) -> Unit,
    onSubmitPollOptionClick: (pollId: String) -> Unit,
    onCommentLikeClick: (commentId: String) -> Unit,
    onCommentReportClick: (commentId: String) -> Unit,
    onCommentDeleteClick: (commentId: String) -> Unit,
    onCommentHiddenClick: (commentId: String, hidden: Boolean) -> Unit,
    onCopyPixClick: (String) -> Unit,
    onSendReceiptClick: (EventTicketOrder) -> Unit,
    onCancelOrderClick: (EventTicketOrder) -> Unit,
    onOpenOrderClick: (EventTicketOrder) -> Unit,
    onEventMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMenuProduct by remember(event.id) { mutableStateOf<EventMenuProduct?>(null) }
    var menuQuantity by remember(event.id, selectedMenuProduct?.id) { mutableIntStateOf(1) }
    var attendeesDialogStatus by remember(event.id) { mutableStateOf<EventRsvpStatus?>(null) }
    val firstActiveLot = event.products.firstOrNull { it.status.equals("ativo", ignoreCase = true) }

    attendeesDialogStatus?.let { status ->
        EventAttendeesDialog(
            status = status,
            rsvps = event.rsvps,
            onAttendeeClick = { userId ->
                attendeesDialogStatus = null
                onAttendeeClick(userId)
            },
            onDismiss = { attendeesDialogStatus = null },
        )
    }

    PremiumScreen(
        modifier = modifier,
        horizontalPadding = 0.dp,
        verticalSpacing = 18.dp,
        bottomPadding = 120.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PremiumHeader(
                title = event.title,
                subtitle = "${event.dateLabel} • ${event.location}",
                icon = Icons.Outlined.Event,
                onBackClick = onBackClick,
            )
        }

        Box {
            EventCover(
                event = event,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
            )
            EventCountdownBar(
                rawDate = event.rawDate,
                rawTime = event.rawTime,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp),
            )
            EventTurmaRankingRow(
                rsvps = event.rsvps,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp),
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (event.isLowStock) {
                EventLowStockBanner(
                    canBuy = firstActiveLot != null && event.status == EventStatus.Open,
                    onBuyClick = { firstActiveLot?.let { onCheckoutClick(event, it) } },
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PremiumPrimaryButton(
                    text = if (event.viewerRsvpStatus == EventRsvpStatus.Going) "Confirmado" else "Eu vou",
                    onClick = { onRsvpClick(EventRsvpStatus.Going) },
                    icon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.weight(1f),
                    accent = PremiumBrand,
                    enabled = !state.isSubmittingRsvp,
                )
                PremiumSecondaryButton(
                    text = "Talvez",
                    onClick = { onRsvpClick(EventRsvpStatus.Maybe) },
                    icon = Icons.Outlined.HelpOutline,
                    modifier = Modifier.weight(1f),
                    accent = if (event.viewerRsvpStatus == EventRsvpStatus.Maybe) PremiumAmber else PremiumZinc400,
                    enabled = !state.isSubmittingRsvp,
                )
            }
            state.rsvpError?.takeIf(String::isNotBlank)?.let { error ->
                Text(
                    text = error,
                    color = PremiumAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (event.isEventMenuEnabled) {
                EventMenuCard(
                    title = event.eventMenuTitle,
                    category = event.eventMenuCategory,
                    productCount = event.menuProducts.size,
                    onClick = onEventMenuClick,
                )

                if (event.menuProducts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        event.menuProducts.forEach { product ->
                            EventPartyProductMiniRow(
                                product = product,
                                selected = selectedMenuProduct?.id == product.id,
                                onClick = {
                                    selectedMenuProduct = product
                                    menuQuantity = 1
                                },
                            )
                        }
                    }
                }

                selectedMenuProduct?.let { product ->
                    EventPartyProductOrderPanel(
                        event = event,
                        product = product,
                        quantity = menuQuantity,
                        buyerName = buyerName,
                        buyerTurma = buyerTurma,
                        buyerPhone = buyerPhone,
                        isSubmitting = state.isSubmittingMenuProductOrder,
                        errorMessage = state.menuProductOrderError,
                        createdOrderId = state.createdMenuProductOrderId,
                        onQuantityChange = { nextQuantity -> menuQuantity = nextQuantity },
                        onCreateOrderClick = { afterCreated ->
                            onMenuProductOrderClick(product, menuQuantity) { orderId ->
                                selectedMenuProduct = product
                                afterCreated(orderId)
                            }
                        },
                        onOrdersClick = onTicketsClick,
                    )
                }
            }

            PremiumCard(accent = eventOwnerAccent(event.ownerType)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumChip(label = event.ownerType.label, accent = eventOwnerAccent(event.ownerType), filled = true)
                    EventVisibilityBadge(event.visibility)
                    EventStatusChip(status = event.status)
                }
                Text(
                    text = event.description.ifBlank { "Evento oficial da USC." },
                    color = PremiumZinc300,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                EventMetaLine(event = event)
                // No web esses dois contadores abrem a lista completa de quem respondeu.
                Box(modifier = Modifier.clickable { attendeesDialogStatus = EventRsvpStatus.Going }) {
                    PremiumInfoRow("Presença", "+${event.confirmedCount} confirmados", accent = PremiumBrand)
                }
                Box(modifier = Modifier.clickable { attendeesDialogStatus = EventRsvpStatus.Maybe }) {
                    PremiumInfoRow("Interessados", "${event.maybeCount} talvez", accent = PremiumZinc400)
                }

                // Fotos de perfil de quem confirmou, como no /eventos/[id] do web.
                val goingRsvps = event.rsvps.filter { it.status == EventRsvpStatus.Going }
                if (goingRsvps.isNotEmpty()) {
                    EventAttendeeAvatarRow(
                        rsvps = goingRsvps,
                        onAttendeeClick = onAttendeeClick,
                    )
                }
            }

            PremiumHeader(
                title = "Ingressos",
                subtitle = "Lotes disponíveis para este evento",
                icon = Icons.Outlined.ConfirmationNumber,
            )

            if (event.products.isEmpty()) {
                PremiumEmptyState(
                    title = "Sem lotes liberados",
                    subtitle = "Os ingressos serão exibidos quando a venda abrir.",
                    icon = Icons.Outlined.ConfirmationNumber,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    event.products.forEach { lot ->
                        TicketLotRow(
                            product = lot,
                            eventStatus = event.status,
                            onClick = { onCheckoutClick(event, lot) },
                        )
                    }
                }
            }

            EventPollsSection(
                polls = event.polls,
                votingPollId = state.votingPollId,
                errorMessage = state.pollActionError,
                optionDraft = state.pollOptionDraft,
                isSubmittingOption = state.isSubmittingPollOption,
                onVotePollClick = onVotePollClick,
                onOptionDraftChange = onPollOptionDraftChange,
                onSubmitOptionClick = onSubmitPollOptionClick,
            )
            EventCommentsSection(
                comments = state.visibleComments,
                draft = state.commentDraft,
                isSubmitting = state.isSubmittingComment,
                errorMessage = state.commentError,
                actionErrorMessage = state.commentActionError,
                busyCommentId = state.commentActionId,
                viewerUserId = viewerUserId,
                viewerIsAdmin = state.viewerIsAdmin,
                onDraftChange = onCommentDraftChange,
                onSubmitClick = onSubmitCommentClick,
                onLikeClick = onCommentLikeClick,
                onReportClick = onCommentReportClick,
                onDeleteClick = onCommentDeleteClick,
                onHiddenClick = onCommentHiddenClick,
            )
            EventTicketOrdersSection(
                event = event,
                pendingOrders = state.pendingTicketOrders,
                historyOrders = state.historyTicketOrders,
                isLoading = state.isLoadingTicketOrders,
                errorMessage = state.ticketOrdersError,
                cancellingOrderId = state.cancellingOrderId,
                onCopyPix = onCopyPixClick,
                onSendReceipt = onSendReceiptClick,
                onCancelOrder = onCancelOrderClick,
                onOpenOrder = onOpenOrderClick,
            )
            EventOrdersShortcut(onClick = onTicketsClick)

            PremiumSecondaryButton(
                text = "Voltar",
                onClick = onBackClick,
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
            )
        }
    }
}

@Composable
private fun EventMenuCard(
    title: String,
    category: String,
    productCount: Int,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PremiumAmber.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, PremiumAmber), RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PremiumChip(label = title, icon = Icons.Outlined.ShoppingBag, accent = PremiumAmber, filled = true)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (productCount > 0) {
                    "$productCount produtos disponíveis • $category"
                } else {
                    "$category • aguardando produtos"
                },
                color = PremiumAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = PremiumAmber)
    }
}

@Composable
private fun EventPartyProductMiniRow(
    product: EventMenuProduct,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (selected) PremiumBrand else PremiumAmber
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PremiumZinc900.copy(alpha = 0.88f))
            .border(BorderStroke(1.dp, accent.copy(alpha = if (selected) 0.88f else 0.34f)), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.40f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.88f,
                )
            } else {
                Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = accent)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PremiumChip(label = product.category, accent = accent, filled = true)
                PremiumChip(label = product.status, accent = PremiumBrand)
            }
            Text(
                text = product.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (product.description.isNotBlank()) {
                Text(
                    text = product.description,
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = product.priceLabel,
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = product.stockLabel,
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (selected) "selecionado" else "pedir",
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
        }
    }
}

@Composable
private fun EventPartyProductOrderPanel(
    event: Event,
    product: EventMenuProduct,
    quantity: Int,
    buyerName: String,
    buyerTurma: String,
    buyerPhone: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    createdOrderId: String?,
    onQuantityChange: (Int) -> Unit,
    onCreateOrderClick: (afterCreated: (String) -> Unit) -> Unit,
    onOrdersClick: () -> Unit,
) {
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")) }
    val uriHandler = LocalUriHandler.current
    val maxQuantity = if (product.stockCount > 0) product.stockCount.coerceAtMost(10) else 10
    val safeQuantity = quantity.coerceIn(1, maxQuantity.coerceAtLeast(1))
    val totalLabel = formatter.format((product.priceValue * safeQuantity).coerceAtLeast(0.0))
    val isUnavailable = product.status.normalizeProductStatus() != "ativo" &&
        product.status.normalizeProductStatus() != "disponivel" &&
        product.status.normalizeProductStatus() != "disponível"
    val canSubmit = !isSubmitting && !isUnavailable && product.priceValue >= 0.0

    PremiumCard(accent = PremiumAmber) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PremiumChip(label = "MODO VENDAS", icon = Icons.Outlined.ShoppingBag, accent = PremiumAmber, filled = true)
                Text(
                    text = product.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                )
                Text(
                    text = product.description.ifBlank { "Produto disponível no menu deste evento." },
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                )
            }
            Text(
                text = totalLabel,
                color = PremiumAmber,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "QUANTIDADE",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = product.stockLabel,
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                EventQuantityButton(
                    icon = Icons.Outlined.Remove,
                    enabled = safeQuantity > 1 && !isSubmitting,
                    onClick = { onQuantityChange((safeQuantity - 1).coerceAtLeast(1)) },
                )
                Text(
                    text = safeQuantity.toString(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                EventQuantityButton(
                    icon = Icons.Outlined.Add,
                    enabled = safeQuantity < maxQuantity && !isSubmitting,
                    onClick = { onQuantityChange((safeQuantity + 1).coerceAtMost(maxQuantity)) },
                )
            }
        }

        errorMessage?.takeIf(String::isNotBlank)?.let { message ->
            Text(
                text = message,
                color = PremiumAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        createdOrderId?.takeIf(String::isNotBlank)?.let { orderId ->
            PremiumChip(
                label = "Pedido gerado • ${orderId.take(8).uppercase(Locale.ROOT)}",
                icon = Icons.Outlined.ReceiptLong,
                accent = PremiumBrand,
                filled = true,
            )
        }

        PremiumPrimaryButton(
            text = if (isSubmitting) "Gerando pedido..." else "Confirmar pedido",
            onClick = {
                onCreateOrderClick { orderId ->
                    buildEventProductReceiptWhatsappUrl(
                        phone = event.receiptContactWhatsapp,
                        event = event,
                        product = product,
                        quantity = safeQuantity,
                        totalLabel = totalLabel,
                        buyerName = buyerName,
                        buyerTurma = buyerTurma,
                        buyerPhone = buyerPhone,
                        orderCode = orderId.take(8).uppercase(Locale.ROOT),
                    )?.let(uriHandler::openUri)
                }
            },
            icon = Icons.Outlined.ShoppingBag,
            enabled = canSubmit,
            accent = PremiumAmber,
        )
        PremiumSecondaryButton(
            text = "Ver meus pedidos",
            onClick = onOrdersClick,
            icon = Icons.Outlined.ReceiptLong,
            accent = PremiumBrand,
        )
    }
}

@Composable
private fun EventQuantityButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) PremiumAmber.copy(alpha = 0.16f) else PremiumZinc800.copy(alpha = 0.45f))
            .border(BorderStroke(1.dp, if (enabled) PremiumAmber else PremiumZinc800), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) PremiumAmber else PremiumZinc500)
    }
}

@Composable
private fun TicketLotRow(
    product: EventProduct,
    eventStatus: EventStatus,
    onClick: () -> Unit,
) {
    val isOpen = eventStatus == EventStatus.Open && product.status.lowercase() == "ativo"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PremiumZinc900)
            .border(BorderStroke(1.dp, if (isOpen) PremiumBrand else PremiumZinc800), RoundedCornerShape(14.dp))
            .clickable(enabled = isOpen, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = product.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (product.hasPlanDiscount) {
                Text(
                    text = product.basePriceLabel,
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
            Text(
                text = product.priceLabel,
                color = PremiumBrand,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            )
            if (product.hasPlanDiscount) {
                Text(
                    text = product.planBenefitLabel.uppercase(),
                    color = PremiumBrand,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
            }
        }
        PremiumChip(
            label = if (isOpen) "Comprar" else product.status,
            accent = if (isOpen) PremiumBrand else PremiumZinc500,
            filled = isOpen,
        )
    }
}

@Composable
private fun EventPollsSection(
    polls: List<EventPoll>,
    votingPollId: String?,
    errorMessage: String?,
    optionDraft: String,
    isSubmittingOption: Boolean,
    onVotePollClick: (pollId: String, optionIndex: Int) -> Unit,
    onOptionDraftChange: (String) -> Unit,
    onSubmitOptionClick: (pollId: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumHeader(
            title = "Enquete da galera",
            subtitle = "Votação oficial do evento",
            icon = Icons.Outlined.HowToVote,
        )
        errorMessage?.takeIf(String::isNotBlank)?.let { error ->
            Text(
                text = error,
                color = PremiumAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (polls.isEmpty()) {
            PremiumCard(accent = PremiumPurple) {
                Text(
                    text = "Nenhuma enquete ativa",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Quando a organização publicar perguntas, elas aparecerão aqui.",
                    color = PremiumZinc500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            polls.forEach { poll ->
                EventPollCard(
                    poll = poll,
                    isVoting = votingPollId == poll.id,
                    optionDraft = optionDraft,
                    isSubmittingOption = isSubmittingOption,
                    onVotePollClick = onVotePollClick,
                    onOptionDraftChange = onOptionDraftChange,
                    onSubmitOptionClick = onSubmitOptionClick,
                )
            }
        }
    }
}

@Composable
private fun EventPollCard(
    poll: EventPoll,
    isVoting: Boolean,
    optionDraft: String,
    isSubmittingOption: Boolean,
    onVotePollClick: (pollId: String, optionIndex: Int) -> Unit,
    onOptionDraftChange: (String) -> Unit,
    onSubmitOptionClick: (pollId: String) -> Unit,
) {
    PremiumCard(accent = PremiumPurple) {
        PremiumChip(label = "ENQUETE", icon = Icons.Outlined.HowToVote, accent = PremiumPurple, filled = true)
        Text(
            text = poll.question,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 19.sp,
        )
        poll.options.forEachIndexed { index, option ->
            val selected = index in poll.viewerVotes
            val fraction = if (poll.totalVotes <= 0) 0f else option.votes.toFloat() / poll.totalVotes.toFloat()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PremiumZinc900)
                    .border(
                        BorderStroke(1.dp, if (selected) PremiumPurple else PremiumZinc800),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable(enabled = !isVoting && !selected) { onVotePollClick(poll.id, index) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (selected) "seu voto" else "${option.votes} voto(s)",
                        color = if (selected) PremiumPurple else PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(PremiumZinc800, RoundedCornerShape(999.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(PremiumPurple, RoundedCornerShape(999.dp)),
                    )
                }
            }
        }
        Text(
            text = "${poll.votersCount} participante(s) • ${poll.totalVotes} voto(s)",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
        )

        if (poll.allowUserOptions) {
            PremiumTextField(
                value = optionDraft,
                onValueChange = onOptionDraftChange,
                label = "Adicionar resposta...",
                leadingIcon = Icons.Outlined.Add,
            )
            Text(
                text = "Cada usuário sugere 1 resposta nova e a enquete aceita até " +
                    "$EventPollOptionMaxCount respostas. " +
                    "(${optionDraft.length}/$EventPollOptionMaxChars)",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
            )
            PremiumSecondaryButton(
                text = if (isSubmittingOption) "Enviando..." else "Adicionar resposta",
                onClick = { onSubmitOptionClick(poll.id) },
                icon = Icons.Outlined.Add,
                accent = PremiumPurple,
                enabled = !isSubmittingOption && optionDraft.trim().isNotBlank(),
            )
        } else {
            Text(
                text = "Essa enquete está fechada para novas respostas.",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun EventCommentsSection(
    comments: List<EventComment>,
    draft: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    actionErrorMessage: String?,
    busyCommentId: String?,
    viewerUserId: String,
    viewerIsAdmin: Boolean,
    onDraftChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onLikeClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onHiddenClick: (String, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumHeader(
            title = "Mural do rolê",
            subtitle = "Comentários do evento e interação da comunidade",
            icon = Icons.Outlined.ChatBubbleOutline,
        )
        PremiumCard(accent = PremiumBrand) {
            PremiumTextField(
                value = draft,
                onValueChange = onDraftChange,
                label = "Solta o verbo...",
                singleLine = false,
                leadingIcon = Icons.Outlined.ChatBubbleOutline,
            )
            errorMessage?.takeIf(String::isNotBlank)?.let { error ->
                Text(
                    text = error,
                    color = PremiumAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "Comentário: ${draft.length}/$EventCommentMaxChars",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            PremiumPrimaryButton(
                text = if (isSubmitting) "Publicando..." else "Enviar comentário",
                onClick = onSubmitClick,
                icon = Icons.Outlined.Send,
                enabled = !isSubmitting && draft.trim().isNotBlank(),
                accent = PremiumBrand,
            )
        }
        actionErrorMessage?.takeIf(String::isNotBlank)?.let { error ->
            Text(text = error, color = PremiumAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (comments.isEmpty()) {
            PremiumCard(accent = PremiumBrand) {
                Text(
                    text = "Seja o primeiro a comentar!",
                    color = PremiumZinc500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            comments.forEach { comment ->
                EventCommentRow(
                    comment = comment,
                    isBusy = busyCommentId == comment.id,
                    canDelete = viewerIsAdmin || comment.userId == viewerUserId,
                    viewerIsAdmin = viewerIsAdmin,
                    onLikeClick = onLikeClick,
                    onReportClick = onReportClick,
                    onDeleteClick = onDeleteClick,
                    onHiddenClick = onHiddenClick,
                )
            }
        }
    }
}

@Composable
private fun EventCommentRow(
    comment: EventComment,
    isBusy: Boolean,
    canDelete: Boolean,
    viewerIsAdmin: Boolean,
    onLikeClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onHiddenClick: (String, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PremiumZinc900.copy(alpha = if (comment.hidden) 0.42f else 0.82f))
            .border(
                BorderStroke(1.dp, if (comment.hidden) PremiumRed.copy(alpha = 0.4f) else PremiumZinc800),
                RoundedCornerShape(18.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(PremiumBrand.copy(alpha = 0.14f))
                .border(BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.45f)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (!comment.userAvatar.isNullOrBlank()) {
                AsyncImage(
                    model = comment.userAvatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = PremiumBrand)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.userName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOf(comment.userTurma, comment.role)
                            .filter(String::isNotBlank)
                            .joinToString(" • ")
                            .ifBlank { comment.createdAtLabel },
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = comment.text,
                color = PremiumZinc300,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            if (comment.hidden) {
                PremiumChip(label = "Oculto pelo admin", accent = PremiumRed)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumChip(
                    label = "${if (comment.likedByViewer) "♥" else "♡"} ${comment.likesCount}",
                    accent = if (comment.likedByViewer) PremiumRed else PremiumZinc500,
                    filled = comment.likedByViewer,
                    modifier = Modifier.clickable(enabled = !isBusy) { onLikeClick(comment.id) },
                )
                PremiumChip(
                    label = if (comment.reportedByViewer) "Denunciado" else "Denunciar",
                    accent = PremiumAmber,
                    modifier = Modifier.clickable(enabled = !isBusy && !comment.reportedByViewer) {
                        onReportClick(comment.id)
                    },
                )
                if (canDelete) {
                    PremiumChip(
                        label = "Apagar",
                        accent = PremiumRed,
                        modifier = Modifier.clickable(enabled = !isBusy) { onDeleteClick(comment.id) },
                    )
                }
                if (viewerIsAdmin) {
                    PremiumChip(
                        label = if (comment.hidden) "Restaurar" else "Ocultar",
                        accent = PremiumZinc400,
                        modifier = Modifier.clickable(enabled = !isBusy) {
                            onHiddenClick(comment.id, !comment.hidden)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EventOrdersShortcut(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PremiumZinc900.copy(alpha = 0.76f))
            .border(BorderStroke(1.dp, PremiumAmber.copy(alpha = 0.42f)), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PremiumChip(label = "SEUS PEDIDOS", icon = Icons.Outlined.ReceiptLong, accent = PremiumAmber, filled = true)
        Text(
            text = "Acompanhe reservas, PIX, produtos do evento e status financeiro.",
            color = PremiumZinc500,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = PremiumAmber)
    }
}

@Composable
private fun EventSectionPlaceholder(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PremiumZinc900.copy(alpha = 0.76f))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.35f)), RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title.uppercase(),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        Text(
            text = subtitle,
            color = PremiumZinc500,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun buildEventProductReceiptWhatsappUrl(
    phone: String,
    event: Event,
    product: EventMenuProduct,
    quantity: Int,
    totalLabel: String,
    buyerName: String,
    buyerTurma: String,
    buyerPhone: String,
    orderCode: String,
): String? {
    val digits = phone.filter(Char::isDigit)
    if (digits.isBlank()) return null
    val normalizedPhone = if (digits.startsWith("55")) digits else "55$digits"
    val organizerLabel = event.ownerName
        .trim()
        .ifBlank { event.eventMenuTitle.trim() }
        .ifBlank { event.tenantId.uppercase(Locale.ROOT) }
        .ifBlank { "equipe USC" }
    val productLine = buildString {
        append(quantity.coerceAtLeast(1))
        append("x ")
        append(product.name.trim().ifBlank { "Produto do evento" })
        if (product.category.isNotBlank()) append(" • ${product.category}")
    }
    val cleanOrderCode = orderCode.trim().ifBlank { "Aguardando criação no app" }
    val message = listOf(
        "Olá, *$organizerLabel*! Segue o comprovante do pedido de produto do evento.",
        "",
        "*EVENTO:* ${event.title.trim().ifBlank { "Evento USC" }}",
        "*NOME:* ${buyerName.trim().ifBlank { "Aluno" }}",
        "*TURMA:* ${buyerTurma.trim().ifBlank { "Sem turma" }}",
        "*CONTATO:* ${buyerPhone.trim().ifBlank { "Não informado" }}",
        "*PRODUTO:* $productLine",
        "*VALOR TOTAL:* $totalLabel",
        "*PEDIDO:* $cleanOrderCode",
        "",
        "Segue o comprovante!",
    ).joinToString("\n")
    return "https://wa.me/$normalizedPhone?text=${URLEncoder.encode(message, "UTF-8")}"
}

private fun String.normalizeProductStatus(): String = trim().lowercase(Locale.ROOT)
