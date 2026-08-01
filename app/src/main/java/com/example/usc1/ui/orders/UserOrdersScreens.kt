package com.example.usc1.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.NativeAction
import com.example.usc1.core.ui.NativeActionCard
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.UserOrder
import com.example.usc1.domain.model.UserOrderStatus
import com.example.usc1.domain.model.UserOrderTab
import com.example.usc1.domain.model.UserOrderTicketEntry
import java.net.URLEncoder
import java.util.Locale

/** `/configuracoes/pedidos` — hub com os três cards de tipo. */
@Composable
fun UserOrdersHubScreen(
    onTabClick: (UserOrderTab) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Meus Ingressos e Compras",
            subtitle = "Eventos, loja e planos",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            onBackClick = onBackClick,
        )
        NativeActionCard(
            NativeAction(
                UserOrderTab.Eventos.title,
                UserOrderTab.Eventos.description,
                Icons.Outlined.Event,
            ),
            { onTabClick(UserOrderTab.Eventos) },
        )
        NativeActionCard(
            NativeAction(
                UserOrderTab.Loja.title,
                UserOrderTab.Loja.description,
                Icons.Outlined.Storefront,
            ),
            { onTabClick(UserOrderTab.Loja) },
        )
        NativeActionCard(
            NativeAction(
                UserOrderTab.Planos.title,
                UserOrderTab.Planos.description,
                Icons.Outlined.CreditCard,
            ),
            { onTabClick(UserOrderTab.Planos) },
        )
    }
}

/**
 * `/configuracoes/pedidos/{eventos|loja|planos}` — contadores por status, lista filtrada e
 * paginação de 10 em 10.
 */
@Composable
fun UserOrdersByTabScreen(
    state: UserOrdersUiState,
    onStatusClick: (UserOrderStatus) -> Unit,
    onOrderClick: (UserOrder) -> Unit,
    onPageChange: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = state.tab.title,
            subtitle = state.tab.description,
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            onBackClick = onBackClick,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OrderCountTile(
                label = "Aprovado",
                value = state.approvedCount,
                accent = PremiumBrand,
                selected = state.statusFilter == UserOrderStatus.Aprovado,
                modifier = Modifier.weight(1f),
            ) { onStatusClick(UserOrderStatus.Aprovado) }
            OrderCountTile(
                label = "Pendente",
                value = state.pendingCount,
                accent = PremiumAmber,
                selected = state.statusFilter == UserOrderStatus.Pendente,
                modifier = Modifier.weight(1f),
            ) { onStatusClick(UserOrderStatus.Pendente) }
            OrderCountTile(
                label = "Negado",
                value = state.rejectedCount,
                accent = PremiumRed,
                selected = state.statusFilter == UserOrderStatus.Rejeitado,
                modifier = Modifier.weight(1f),
            ) { onStatusClick(UserOrderStatus.Rejeitado) }
        }

        when {
            state.isLoading -> PremiumLoadingState(text = "Carregando pedidos")

            state.errorMessage.isNotBlank() -> PremiumEmptyState(
                title = "Pedidos indisponíveis",
                subtitle = state.errorMessage,
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            )

            state.filteredOrders.isEmpty() -> PremiumEmptyState(
                title = "Nenhum pedido encontrado",
                subtitle = "Não há pedidos ${state.statusFilter.label.lowercase()}s neste filtro.",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            )

            else -> {
                Text(
                    text = "${state.statusFilter.label}S".uppercase(),
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
                state.paginatedOrders.forEach { order ->
                    UserOrderCard(order = order, onClick = { onOrderClick(order) })
                }
                if (state.totalPages > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PremiumSecondaryButton(
                            text = "Anterior",
                            onClick = { onPageChange(state.page - 1) },
                            enabled = state.page > 1,
                            accent = PremiumZinc400,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${minOf(state.page, state.totalPages)}/${state.totalPages}",
                            color = PremiumZinc500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                        )
                        PremiumSecondaryButton(
                            text = "Próxima",
                            onClick = { onPageChange(state.page + 1) },
                            enabled = state.page < state.totalPages,
                            accent = PremiumZinc400,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * `/configuracoes/pedidos/eventos/[status]/[pedidoId]` e o modal equivalente das outras abas:
 * dados do pedido, bloco PIX com cópia de chave, contato do comprovante e lista de ingressos.
 */
@Composable
fun UserOrderDetailScreen(
    state: UserOrdersUiState,
    order: UserOrder,
    onBackClick: () -> Unit,
    onCopyPixClick: (String) -> Unit,
    onSendReceiptClick: (String) -> Unit,
    onTicketClick: (UserOrderTicketEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val payment = state.resolvePaymentConfig(order)
    val recipientName = state.resolveRecipientName(order)
    val recipientClass = state.resolveRecipientClass(order)
    val accent = order.status.orderAccent()

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Detalhe do pedido",
            subtitle = order.title,
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            accent = accent,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = accent) {
            PremiumChip(label = order.status.label, accent = accent)
            PremiumInfoRow(label = "Pedido", value = "#${order.shortCode}", accent = accent)
            PremiumInfoRow(label = "Item", value = order.subtitle, accent = accent)
            PremiumInfoRow(label = "Valor", value = order.amount.toBrl(), accent = accent)
            PremiumInfoRow(label = "Data e hora", value = order.createdAtLabel, accent = accent)
            if (order.selectedColor.isNotBlank()) {
                PremiumInfoRow(label = "Cor", value = order.selectedColor, accent = accent)
            }
            if (order.sellerName.isNotBlank()) {
                PremiumInfoRow(label = "Vendedor", value = order.sellerName, accent = accent)
            }
        }

        PremiumCard(accent = PremiumBrand) {
            Text(
                text = "PAGAMENTO VIA PIX",
                color = PremiumBrand,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp,
            )
            Text(
                text = payment.pixKey,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            PremiumSecondaryButton(
                text = "Copiar chave PIX",
                onClick = { onCopyPixClick(payment.pixKey) },
                accent = PremiumBrand,
                icon = Icons.Outlined.ContentCopy,
            )
            PremiumInfoRow(label = "Banco", value = payment.bank, accent = PremiumBrand)
            PremiumInfoRow(label = "Titular", value = payment.holder, accent = PremiumBrand)
            PremiumInfoRow(
                label = "WhatsApp p/ comprovante",
                value = payment.whatsapp.ifBlank { "Não informado" },
                accent = PremiumBrand,
            )
            Text(
                text = "Envie o comprovante informando o número do pedido #${order.shortCode}.",
                color = PremiumZinc400,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
            )

            val receiptUrl = buildReceiptWhatsappUrl(
                phone = payment.whatsapp,
                order = order,
                brandLabel = state.tenantBrandLabel,
                buyerName = state.buyerName,
                buyerClass = state.buyerClass,
                buyerPhone = state.buyerPhone,
                recipientName = recipientName,
                recipientClass = recipientClass,
            )
            if (receiptUrl != null) {
                PremiumSecondaryButton(
                    text = "Enviar comprovante para $recipientName",
                    onClick = { onSendReceiptClick(receiptUrl) },
                    accent = PremiumBrand,
                    icon = Icons.Outlined.Send,
                )
            }
        }

        if (order.tab == UserOrderTab.Eventos) {
            val tickets = order.paymentConfig.ticketEntries
            if (tickets.isEmpty()) {
                PremiumEmptyState(
                    title = "Ingressos ainda não emitidos",
                    subtitle = "Os ingressos digitais aparecem aqui depois que o pedido é aprovado.",
                    icon = Icons.Outlined.QrCode,
                )
            } else {
                Text(
                    text = "INGRESSOS",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                )
                tickets.forEach { ticket ->
                    TicketRow(ticket = ticket, onClick = { onTicketClick(ticket) })
                }
            }
        }
    }
}

/** `.../ingressos/[ticketToken]` — a ficha de um ingresso dentro do pedido. */
@Composable
fun UserOrderTicketDetailScreen(
    state: UserOrdersUiState,
    order: UserOrder,
    ticket: UserOrderTicketEntry,
    onBackClick: () -> Unit,
    onOpenQrClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val payment = state.resolvePaymentConfig(order)
    val accent = if (ticket.isBlocked) PremiumRed else PremiumBrand

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Detalhe do ingresso",
            subtitle = ticket.label,
            icon = Icons.Outlined.QrCode,
            accent = accent,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = accent) {
            PremiumChip(label = ticket.statusLabel, accent = accent)
            PremiumInfoRow(label = "Evento", value = order.title, accent = accent)
            PremiumInfoRow(label = "Pedido", value = "#${order.shortCode}", accent = accent)
            PremiumInfoRow(label = "Valor", value = order.amount.toBrl(), accent = accent)
            PremiumInfoRow(label = "Data e hora do pedido", value = order.createdAtLabel, accent = accent)
            PremiumInfoRow(label = "Ingresso", value = order.subtitle, accent = accent)

            if (ticket.transferredToUserName.isNotBlank()) {
                Text(
                    text = "Transferido para ${ticket.transferredToUserName}.",
                    color = PremiumAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (ticket.transferredFromUserName.isNotBlank()) {
                Text(
                    text = "Recebido de ${ticket.transferredFromUserName}.",
                    color = PremiumBrand,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        PremiumCard(accent = PremiumBrand) {
            Text(
                text = "PAGAMENTO VIA PIX",
                color = PremiumBrand,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp,
            )
            PremiumInfoRow(label = "Banco", value = payment.bank, accent = PremiumBrand)
            PremiumInfoRow(label = "Titular", value = payment.holder, accent = PremiumBrand)
        }

        PremiumSecondaryButton(
            text = "Abrir QR Code",
            onClick = onOpenQrClick,
            accent = PremiumBrand,
            icon = Icons.Outlined.QrCode,
        )
    }
}

@Composable
private fun TicketRow(
    ticket: UserOrderTicketEntry,
    onClick: () -> Unit,
) {
    val accent = if (ticket.isBlocked) PremiumRed else PremiumBrand
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ticket.label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PremiumChip(label = ticket.statusLabel, accent = accent)
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = accent,
            )
        }
    }
}

@Composable
private fun UserOrderCard(
    order: UserOrder,
    onClick: () -> Unit,
) {
    val accent = order.status.orderAccent()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = order.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = order.subtitle,
                        color = PremiumZinc400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                PremiumChip(label = order.status.label, accent = accent)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = order.createdAtLabel,
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = order.amount.toBrl(),
                    color = PremiumBrand,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun OrderCountTile(
    label: String,
    value: Int,
    accent: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, if (selected) accent else PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label.uppercase(),
                color = PremiumZinc500,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
                maxLines = 1,
            )
            Text(text = "$value", color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

internal fun UserOrderStatus.orderAccent(): Color = when (this) {
    UserOrderStatus.Aprovado -> PremiumBrand
    UserOrderStatus.Pendente -> PremiumAmber
    UserOrderStatus.Rejeitado -> PremiumRed
}

internal fun Double.toBrl(): String = "R$ %.2f".format(Locale.forLanguageTag("pt-BR"), this)

/**
 * Espelha `buildEventReceiptWhatsappMessage` / `buildProductReceiptWhatsappMessage`:
 * mesmo corpo, mesmos rótulos e o bloco "Enviado para" no fim.
 */
internal fun buildReceiptWhatsappUrl(
    phone: String,
    order: UserOrder,
    brandLabel: String,
    buyerName: String,
    buyerClass: String,
    buyerPhone: String,
    recipientName: String,
    recipientClass: String,
): String? {
    val digits = phone.filter(Char::isDigit)
    if (digits.isBlank()) return null
    val normalizedPhone = if (digits.startsWith("55")) digits else "55$digits"

    val name = buyerName.trim().ifBlank { if (order.tab == UserOrderTab.Loja) "Cliente" else "Aluno" }
    val turma = buyerClass.trim().ifBlank { "Sem turma" }
    val contact = buyerPhone.trim().ifBlank { "Não informado" }
    val total = "%.2f".format(Locale.US, order.amount)
    val organizer = if (order.tab == UserOrderTab.Loja) {
        order.sellerName.trim().ifBlank { brandLabel }
    } else {
        brandLabel
    }

    val message = when (order.tab) {
        UserOrderTab.Eventos -> listOf(
            "Fala, equipe *$organizer*! Quero garantir meu lugar no evento ${order.title}.",
            "",
            "*NOME:* $name",
            "*TURMA:* $turma",
            "*CONTATO:* $contact",
            "*INGRESSO:* ${order.subtitle}",
            "*VALOR TOTAL:* R$ $total",
            "*PEDIDO:* ${order.shortCode}",
            "",
            "Segue o comprovante!",
            "Enviado para",
            recipientName,
            recipientClass,
        )

        UserOrderTab.Loja -> listOf(
            "Fala, equipe *$organizer*! Quero finalizar a compra do produto ${order.title}.",
            "",
            "*CLIENTE:* $name",
            "*TURMA:* $turma",
            "*CONTATO:* $contact",
            "*PRODUTO:* ${order.title}",
            "*QTD:* ${order.quantity}",
            "*VALOR:* R$ $total",
            "*PEDIDO:* ${order.shortCode}",
            "",
            "Segue o comprovante do PIX!",
            "Enviado para",
            recipientName,
            recipientClass,
        )

        UserOrderTab.Planos -> listOf(
            "Fala, equipe *$brandLabel*! Segue o comprovante do pedido *${order.shortCode}*.",
        )
    }.joinToString("\n")

    return "https://wa.me/$normalizedPhone?text=${URLEncoder.encode(message, "UTF-8")}"
}
