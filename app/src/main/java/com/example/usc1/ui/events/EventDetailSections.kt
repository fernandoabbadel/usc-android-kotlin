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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.core.ui.PremiumZinc950
import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventRsvp
import com.example.usc1.domain.model.EventRsvpStatus
import com.example.usc1.domain.model.EventTicketOrder
import com.example.usc1.domain.model.EventTicketOrderStatus
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.delay

/**
 * Contagem regressiva do hero de `/eventos/[id]`, com os mesmos estados do web:
 * "CALCULANDO...", "DATA INDEFINIDA", "ESTÁ ROLANDO!" e os quatro blocos D/H/M/S.
 */
@Composable
fun EventCountdownBar(
    rawDate: String,
    rawTime: String,
    modifier: Modifier = Modifier,
) {
    val target = remember(rawDate, rawTime) { parseEventTargetDateTime(rawDate, rawTime) }
    var remainingSeconds by remember(target) { mutableStateOf<Long?>(null) }
    var statusLabel by remember(target) { mutableStateOf("CALCULANDO...") }

    LaunchedEffect(target) {
        if (target == null) {
            statusLabel = "DATA INDEFINIDA"
            remainingSeconds = null
            return@LaunchedEffect
        }
        while (true) {
            val nowMillis = System.currentTimeMillis()
            val targetMillis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val diff = targetMillis - nowMillis
            if (diff <= 0) {
                statusLabel = "ESTÁ ROLANDO!"
                remainingSeconds = null
                return@LaunchedEffect
            }
            statusLabel = ""
            remainingSeconds = diff / 1000L
            delay(1_000L)
        }
    }

    val seconds = remainingSeconds
    if (statusLabel.isNotBlank() || seconds == null) {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.5f)),
        ) {
            Text(
                text = statusLabel,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = PremiumBrand,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
        }
        return
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.42f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(18.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CountdownCell(value = seconds / 86_400L, label = "Dias")
        CountdownCell(value = (seconds % 86_400L) / 3_600L, label = "Hrs")
        CountdownCell(value = (seconds % 3_600L) / 60L, label = "Min")
        CountdownCell(value = seconds % 60L, label = "Seg", accent = true)
    }
}

@Composable
private fun CountdownCell(
    value: Long,
    label: String,
    accent: Boolean = false,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (accent) PremiumBrand.copy(alpha = 0.16f) else PremiumZinc900.copy(alpha = 0.86f))
            .border(
                BorderStroke(1.dp, if (accent) PremiumBrand.copy(alpha = 0.45f) else PremiumZinc800),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value.coerceAtLeast(0L).toString().padStart(2, '0'),
            color = if (accent) PremiumBrand else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = label.uppercase(),
            color = if (accent) PremiumBrand.copy(alpha = 0.75f) else PremiumZinc500,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
    }
}

/** Ranking das três turmas com mais confirmados, como as pílulas do hero no web. */
@Composable
fun EventTurmaRankingRow(
    rsvps: List<EventRsvp>,
    modifier: Modifier = Modifier,
) {
    val ranking = remember(rsvps) {
        rsvps.filter { it.status == EventRsvpStatus.Going }
            .groupingBy { it.userTurma.trim().ifBlank { "Geral" }.uppercase() }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
    }
    if (ranking.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ranking.forEach { (turma, count) ->
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.62f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = turma,
                        color = PremiumZinc300,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "+$count",
                        color = PremiumBrand,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

/** Faixa "Últimas Vagas" que o web mostra quando `isLowStock` está ligado. */
@Composable
fun EventLowStockBanner(
    canBuy: Boolean,
    onBuyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PremiumAmber.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, PremiumAmber.copy(alpha = 0.6f)), RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Star, contentDescription = null, tint = PremiumAmber)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Últimas vagas",
                color = PremiumAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Text(
                text = "O lote vai virar em breve!",
                color = PremiumZinc400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (canBuy) {
            PremiumChip(
                label = "Garantir",
                accent = PremiumAmber,
                filled = true,
                modifier = Modifier.clickable(onClick = onBuyClick),
            )
        }
    }
}

/** Modal de confirmados/interessados do web, com a lista completa clicável. */
@Composable
fun EventAttendeesDialog(
    status: EventRsvpStatus,
    rsvps: List<EventRsvp>,
    onAttendeeClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val filtered = remember(rsvps, status) { rsvps.filter { it.status == status } }
    val accent = if (status == EventRsvpStatus.Going) PremiumBrand else PremiumAmber

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = PremiumZinc950,
            border = BorderStroke(1.dp, PremiumZinc800),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (status == EventRsvpStatus.Going) {
                                Icons.Outlined.CheckCircle
                            } else {
                                Icons.Outlined.HelpOutline
                            },
                            contentDescription = null,
                            tint = accent,
                        )
                        Text(
                            text = if (status == EventRsvpStatus.Going) "Confirmados" else "Interessados",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Fechar",
                        tint = PremiumZinc500,
                        modifier = Modifier.clickable(onClick = onDismiss),
                    )
                }

                if (filtered.isEmpty()) {
                    Text(
                        text = "Ninguém nesta lista ainda.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 32.dp),
                        color = PremiumZinc500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 6.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(filtered, key = { it.id.ifBlank { it.userId } }) { rsvp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onAttendeeClick(rsvp.userId) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.14f))
                                        .border(BorderStroke(1.dp, accent.copy(alpha = 0.45f)), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!rsvp.userAvatar.isNullOrBlank()) {
                                        AsyncImage(
                                            model = rsvp.userAvatar,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Icon(Icons.Outlined.Person, contentDescription = null, tint = accent)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rsvp.userName,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${rsvp.userTurma.trim().ifBlank { "Sem turma" }} • Ver perfil",
                                        color = PremiumZinc500,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = null,
                                    tint = PremiumZinc500,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bloco "Seus Pedidos" de `/eventos/[id]`: pendentes com dados do PIX, cópia da chave,
 * envio de comprovante e cancelamento; finalizados com atalho para o pedido.
 */
@Composable
fun EventTicketOrdersSection(
    event: Event,
    pendingOrders: List<EventTicketOrder>,
    historyOrders: List<EventTicketOrder>,
    isLoading: Boolean,
    errorMessage: String?,
    cancellingOrderId: String?,
    onCopyPix: (String) -> Unit,
    onSendReceipt: (EventTicketOrder) -> Unit,
    onCancelOrder: (EventTicketOrder) -> Unit,
    onOpenOrder: (EventTicketOrder) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumHeader(
            title = "Seus pedidos",
            subtitle = "Reservas de ingresso deste evento",
            icon = Icons.Outlined.ReceiptLong,
        )
        errorMessage?.takeIf(String::isNotBlank)?.let { error ->
            Text(text = error, color = PremiumAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        when {
            isLoading -> Text(
                text = "Carregando seus pedidos...",
                color = PremiumZinc500,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )

            pendingOrders.isEmpty() && historyOrders.isEmpty() -> PremiumCard(accent = PremiumZinc800) {
                Text(
                    text = "Você ainda não fez pedidos deste evento.",
                    color = PremiumZinc500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            else -> {
                if (pendingOrders.isNotEmpty()) {
                    Text(
                        text = "PENDENTES",
                        color = PremiumAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    pendingOrders.forEach { order ->
                        PendingTicketOrderCard(
                            event = event,
                            order = order,
                            isCancelling = cancellingOrderId == order.id,
                            onCopyPix = onCopyPix,
                            onSendReceipt = onSendReceipt,
                            onCancelOrder = onCancelOrder,
                        )
                    }
                }
                if (historyOrders.isNotEmpty()) {
                    Text(
                        text = "FINALIZADOS",
                        color = PremiumZinc400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    historyOrders.forEach { order ->
                        FinishedTicketOrderCard(order = order, onOpenOrder = onOpenOrder)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingTicketOrderCard(
    event: Event,
    order: EventTicketOrder,
    isCancelling: Boolean,
    onCopyPix: (String) -> Unit,
    onSendReceipt: (EventTicketOrder) -> Unit,
    onCancelOrder: (EventTicketOrder) -> Unit,
) {
    val pixKey = order.resolvePixKey(event)
    val whatsapp = order.resolveWhatsapp(event)

    PremiumCard(accent = PremiumAmber) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pedido #${order.shortCode}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = order.requestedAtLabel.ifBlank { "Não informado" },
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            PremiumChip(label = order.status.label, accent = PremiumAmber)
        }

        Text(
            text = "${order.quantity}x ${order.lotName} • ${order.totalLabel}",
            color = PremiumZinc300,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.34f))
                .border(BorderStroke(1.dp, PremiumZinc800), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Wallet, contentDescription = null, tint = PremiumBrand)
                Text(
                    text = "INFORMAÇÕES DO PIX",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }
            PixField(label = "Chave PIX", value = pixKey.ifBlank { "Consulte o financeiro" })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixField(
                    label = "Banco",
                    value = order.resolvePixBank(event).ifBlank { "--" },
                    modifier = Modifier.weight(1f),
                )
                PixField(
                    label = "Titular",
                    value = order.resolvePixHolder(event).ifBlank { "--" },
                    modifier = Modifier.weight(1f),
                )
            }
            PixField(
                label = "Envie o comprovante para",
                value = whatsapp.ifBlank { "(Consulte a diretoria)" },
            )
        }

        PremiumSecondaryButton(
            text = "Copiar PIX",
            onClick = { onCopyPix(pixKey) },
            icon = Icons.Outlined.ContentCopy,
            accent = PremiumZinc400,
            enabled = pixKey.isNotBlank(),
        )
        if (whatsapp.isNotBlank()) {
            PremiumSecondaryButton(
                text = order.recipientName.trim().takeIf(String::isNotBlank)
                    ?.let { "Enviar comprovante para $it" }
                    ?: "Enviar comprovante no WhatsApp",
                onClick = { onSendReceipt(order) },
                icon = Icons.Outlined.Send,
                accent = PremiumBrand,
            )
        }
        PremiumSecondaryButton(
            text = if (isCancelling) "Cancelando..." else "Cancelar pedido",
            onClick = { onCancelOrder(order) },
            icon = Icons.Outlined.Close,
            accent = PremiumRed,
            enabled = !isCancelling,
        )
    }
}

@Composable
private fun FinishedTicketOrderCard(
    order: EventTicketOrder,
    onOpenOrder: (EventTicketOrder) -> Unit,
) {
    PremiumCard(
        accent = if (order.status == EventTicketOrderStatus.Approved) PremiumBrand else PremiumRed,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pedido #${order.shortCode}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = order.approvedAtLabel.ifBlank { order.requestedAtLabel }.ifBlank { "Não informado" },
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            PremiumChip(
                label = order.status.label,
                accent = if (order.status == EventTicketOrderStatus.Approved) PremiumBrand else PremiumRed,
            )
        }
        Text(
            text = "${order.quantity}x ${order.lotName} • ${order.totalLabel}",
            color = PremiumZinc300,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        PremiumSecondaryButton(
            text = "Ver pedido / ingresso",
            onClick = { onOpenOrder(order) },
            icon = Icons.AutoMirrored.Outlined.ArrowForward,
            accent = PremiumBrand,
        )
    }
}

@Composable
private fun PixField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PremiumZinc950.copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, PremiumZinc800), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = PremiumZinc500,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        Text(
            text = value,
            color = PremiumZinc300,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

/**
 * O web resolve o PIX do pedido em cascata: `payment_config` do pedido, depois os
 * campos do evento, depois o financeiro global do tenant.
 */
internal fun EventTicketOrder.resolvePixKey(event: Event): String =
    pixKey.trim().ifBlank { event.pixKey.trim() }

internal fun EventTicketOrder.resolvePixBank(event: Event): String =
    pixBank.trim().ifBlank { event.pixBank.trim() }

internal fun EventTicketOrder.resolvePixHolder(event: Event): String =
    pixHolder.trim().ifBlank { event.pixHolder.trim() }

internal fun EventTicketOrder.resolveWhatsapp(event: Event): String =
    receiptWhatsapp.trim().ifBlank { event.receiptContactWhatsapp.trim() }

/**
 * Link de comprovante do pedido pendente, com o mesmo conteúdo de
 * `buildEventReceiptWhatsappMessage` usado em `/eventos/[id]`.
 */
fun buildEventOrderReceiptWhatsappUrl(
    order: EventTicketOrder,
    event: Event,
    buyerName: String,
    buyerTurma: String,
    buyerPhone: String = "",
): String? {
    val digits = order.resolveWhatsapp(event).filter(Char::isDigit)
    if (digits.isBlank()) return null
    val normalizedPhone = if (digits.startsWith("55")) digits else "55$digits"
    val organizerLabel = event.ownerName.trim()
        .ifBlank { event.tenantId.uppercase() }
        .ifBlank { "USC" }
    val recipientLine = listOf(order.recipientName.trim(), order.recipientTurma.trim())
        .filter(String::isNotBlank)
        .joinToString(" - ")
    val message = buildList {
        add("Fala, equipe *$organizerLabel*! Segue o comprovante do meu pedido.")
        add("")
        add("*NOME:* ${buyerName.trim().ifBlank { "Aluno" }}")
        add("*TURMA:* ${buyerTurma.trim().ifBlank { "Sem turma" }}")
        add("*CONTATO:* ${buyerPhone.trim().ifBlank { "Não informado" }}")
        add("*EVENTO:* ${event.title.trim().ifBlank { "Evento" }}")
        add("*INGRESSO:* ${order.quantity}x ${order.lotName}")
        add("*VALOR TOTAL:* ${order.totalLabel}")
        add("*PEDIDO:* ${order.shortCode}")
        if (recipientLine.isNotBlank()) add("*RECEBEDOR:* $recipientLine")
    }.joinToString("\n")
    return "https://wa.me/$normalizedPhone?text=${URLEncoder.encode(message, "UTF-8")}"
}

/** Espelha `parseEventDate` do web: aceita `yyyy-MM-dd` com `HH:mm` opcional. */
private fun parseEventTargetDateTime(rawDate: String, rawTime: String): LocalDateTime? {
    val date = runCatching { LocalDate.parse(rawDate.trim()) }.getOrNull() ?: return null
    val parts = rawTime.trim().split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return LocalDateTime.of(date, LocalTime.of(hour, minute))
}
