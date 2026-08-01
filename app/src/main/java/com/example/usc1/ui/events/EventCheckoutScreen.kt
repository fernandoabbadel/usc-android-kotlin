package com.example.usc1.ui.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.Event
import com.example.usc1.domain.model.EventPaymentRecipient
import com.example.usc1.domain.model.EventProduct
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

@Composable
fun EventCheckoutScreen(
    state: EventDetailUiState,
    preferredLotId: String = "",
    buyerName: String = "",
    buyerTurma: String = "",
    buyerPhone: String = "",
    onSubmitTicketRequest: (
        lot: EventProduct,
        quantity: Int,
        recipient: EventPaymentRecipient?,
        onSuccess: (String) -> Unit,
    ) -> Unit,
    onOrdersClick: () -> Unit,
    onTicketsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando checkout", modifier = modifier)
        state.errorMessage != null -> EventFlowUnavailableScreen(
            title = "Pedido do evento",
            subtitle = state.errorMessage,
            onBackClick = onBackClick,
            modifier = modifier,
        )
        state.event != null -> EventCheckoutLoadedContent(
            event = state.event,
            state = state,
            preferredLotId = preferredLotId,
            buyerName = buyerName,
            buyerTurma = buyerTurma,
            buyerPhone = buyerPhone,
            onSubmitTicketRequest = onSubmitTicketRequest,
            onOrdersClick = onOrdersClick,
            onTicketsClick = onTicketsClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun EventCheckoutLoadedContent(
    event: Event,
    state: EventDetailUiState,
    preferredLotId: String,
    buyerName: String,
    buyerTurma: String,
    buyerPhone: String,
    onSubmitTicketRequest: (
        lot: EventProduct,
        quantity: Int,
        recipient: EventPaymentRecipient?,
        onSuccess: (String) -> Unit,
    ) -> Unit,
    onOrdersClick: () -> Unit,
    onTicketsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var quantity by rememberSaveable { mutableIntStateOf(1) }
    var selectedLotId by rememberSaveable(event.id, preferredLotId) {
        mutableStateOf(
            event.products.firstOrNull { lot -> lot.id == preferredLotId }?.id
                ?: event.products.firstOrNull()?.id.orEmpty(),
        )
    }
    var selectedRecipientKey by rememberSaveable(event.id) {
        mutableStateOf(event.receiptRecipients.firstOrNull()?.recipientKey().orEmpty())
    }
    val selectedRecipient = event.receiptRecipients.firstOrNull { it.recipientKey() == selectedRecipientKey }
        ?: event.receiptRecipients.firstOrNull()
    val selectedLot = event.products.firstOrNull { it.id == selectedLotId } ?: event.products.firstOrNull()
    val unitPriceLabel = selectedLot?.priceLabel ?: event.priceLabel
    val totalLabel = remember(unitPriceLabel, quantity) { formatEventTotal(unitPriceLabel, quantity) }

    PremiumScreen(
        modifier = modifier,
        horizontalPadding = 20.dp,
        bottomPadding = 120.dp,
    ) {
        PremiumHeader(
            title = "Pedido do evento",
            subtitle = event.title,
            icon = Icons.Outlined.ShoppingCart,
            onBackClick = onBackClick,
        )

        EventCheckoutHeader(event = event, step = step)

        when (step) {
            1 -> EventCheckoutStepOne(
                event = event,
                lots = event.products,
                selectedLotId = selectedLot?.id.orEmpty(),
                onLotSelect = { lot ->
                    selectedLotId = lot.id
                    quantity = 1
                },
                lotName = selectedLot?.name ?: event.lotName,
                unitPriceLabel = unitPriceLabel,
                totalLabel = totalLabel,
                quantity = quantity,
                onMinus = { quantity = (quantity - 1).coerceAtLeast(1) },
                onPlus = { quantity = (quantity + 1).coerceAtMost(10) },
                onNext = { step = 2 },
            )
            2 -> EventCheckoutStepTwo(
                event = event,
                lot = selectedLot,
                quantity = quantity,
                totalLabel = totalLabel,
                buyerName = buyerName,
                buyerTurma = buyerTurma,
                buyerPhone = buyerPhone,
                isSubmitting = state.isSubmittingTicketRequest,
                errorMessage = state.ticketRequestError,
                selectedRecipient = selectedRecipient,
                onRecipientSelect = { recipient -> selectedRecipientKey = recipient.recipientKey() },
                onBack = { step = 1 },
                onNext = { afterCreated ->
                    val lot = selectedLot ?: return@EventCheckoutStepTwo
                    onSubmitTicketRequest(lot, quantity, selectedRecipient) { requestId ->
                        afterCreated(requestId)
                        step = 3
                    }
                },
            )
            else -> EventCheckoutStepThree(
                onOrdersClick = onOrdersClick,
                onTicketsClick = onTicketsClick,
                onBackClick = onBackClick,
            )
        }
    }
}

@Composable
private fun EventCheckoutHeader(event: Event, step: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = PremiumZinc900,
            border = BorderStroke(1.dp, eventOwnerAccent(event.ownerType).copy(alpha = 0.45f)),
        ) {
            EventCover(event = event, modifier = Modifier.fillMaxWidth().height(72.dp))
        }
        Text(
            text = event.title.uppercase(),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Passo $step de 3",
            color = PremiumZinc400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(PremiumZinc800, RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(step / 3f)
                    .height(3.dp)
                    .background(PremiumPurple, RoundedCornerShape(999.dp)),
            )
        }
    }
}

@Composable
private fun EventCheckoutStepOne(
    event: Event,
    lots: List<EventProduct>,
    selectedLotId: String,
    onLotSelect: (EventProduct) -> Unit,
    lotName: String,
    unitPriceLabel: String,
    totalLabel: String,
    quantity: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onNext: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PremiumCard(accent = PremiumPurple) {
            if (lots.size > 1) {
                Text(
                    text = "LOTE",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                lots.forEach { lot ->
                    val selected = lot.id == selectedLotId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) PremiumPurple.copy(alpha = 0.14f) else PremiumZinc900)
                            .border(
                                BorderStroke(1.dp, if (selected) PremiumPurple else PremiumZinc800),
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { onLotSelect(lot) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lot.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = lot.status,
                                color = PremiumZinc500,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = lot.priceLabel,
                            color = if (selected) PremiumPurple else PremiumBrand,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            PremiumInfoRow("Ingresso", lotName, accent = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "QUANTIDADE",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuantityButton(text = "−", onClick = onMinus)
                    Text(text = quantity.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    QuantityButton(text = "+", onClick = onPlus, filled = true)
                }
            }
            PremiumInfoRow("Valor unitário", unitPriceLabel, accent = PremiumZinc300)
            PremiumInfoRow("Total a pagar", totalLabel, accent = PremiumPurple)
        }
        PremiumPrimaryButton(
            text = "Confirmar pedido",
            onClick = onNext,
            icon = Icons.AutoMirrored.Outlined.ArrowForward,
            accent = PremiumPurple,
            enabled = event.products.isNotEmpty(),
        )
        if (event.products.isEmpty()) {
            Text(
                text = "Nenhum lote ativo foi encontrado para este evento.",
                color = PremiumAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EventCheckoutStepTwo(
    event: Event,
    lot: com.example.usc1.domain.model.EventProduct?,
    quantity: Int,
    totalLabel: String,
    buyerName: String,
    buyerTurma: String,
    buyerPhone: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    selectedRecipient: EventPaymentRecipient?,
    onRecipientSelect: (EventPaymentRecipient) -> Unit,
    onBack: () -> Unit,
    onNext: (afterCreated: (String) -> Unit) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val receiptPhone = selectedRecipient?.phone?.trim().orEmpty()
        .ifBlank { event.receiptContactWhatsapp }
    val receiptName = selectedRecipient?.name?.trim().orEmpty()
        .ifBlank { event.receiptContactName }
    val previewWhatsappUrl = buildEventReceiptWhatsappUrl(
        phone = receiptPhone,
        event = event,
        lot = lot,
        quantity = quantity,
        totalLabel = totalLabel,
        buyerName = buyerName,
        buyerTurma = buyerTurma,
        buyerPhone = buyerPhone,
        orderCode = "",
    )

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PremiumCard(accent = PremiumBrand) {
            PremiumChip(label = "Pagamento via Pix", icon = Icons.Outlined.Payment, accent = PremiumBrand)
            PremiumInfoRow("Chave Pix", event.pixKey.ifBlank { "Não configurada" }, accent = PremiumZinc300)
            PremiumInfoRow("Banco", event.pixBank.ifBlank { "Não informado" }, accent = PremiumZinc300)
            PremiumInfoRow("Titular", event.pixHolder.ifBlank { "Não informado" }, accent = PremiumZinc300)
            PremiumInfoRow("Valor exato", totalLabel, accent = PremiumBrand)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PremiumSecondaryButton(
                    text = "Copiar PIX",
                    onClick = { clipboard.setText(AnnotatedString(event.pixKey)) },
                    icon = Icons.Outlined.ContentCopy,
                    accent = PremiumBrand,
                    enabled = event.pixKey.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
                PremiumSecondaryButton(
                    text = "WhatsApp",
                    onClick = { previewWhatsappUrl?.let(uriHandler::openUri) },
                    icon = Icons.Outlined.Send,
                    accent = PremiumBrand,
                    enabled = previewWhatsappUrl != null,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // O web só mostra o seletor quando o evento tem mais de um recebedor configurado.
        if (event.receiptRecipients.size > 1) {
            PremiumCard(accent = PremiumZinc400) {
                Text(
                    text = "ENVIAR COMPROVANTE PARA",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                )
                event.receiptRecipients.forEach { recipient ->
                    val isSelected = recipient == selectedRecipient
                    PremiumSecondaryButton(
                        text = recipient.displayLabel,
                        onClick = { onRecipientSelect(recipient) },
                        icon = if (isSelected) Icons.Outlined.CheckCircle else null,
                        accent = if (isSelected) PremiumBrand else PremiumZinc400,
                    )
                }
            }
        }

        PremiumCard(accent = PremiumBrand, containerColor = PremiumBrand.copy(alpha = 0.11f)) {
            PremiumInfoRow(
                label = "Enviar comprovante",
                value = receiptName.ifBlank { "Responsável financeiro" },
                accent = PremiumBrand,
            )
            Text(
                text = receiptPhone.ifBlank {
                    "Depois do PIX, envie o comprovante para a equipe validar seu ingresso."
                },
                color = PremiumZinc300,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        PremiumPrimaryButton(
            text = if (isSubmitting) "Criando pedido" else "Criar pedido e abrir WhatsApp",
            onClick = {
                onNext { requestId ->
                    buildEventReceiptWhatsappUrl(
                        phone = receiptPhone,
                        event = event,
                        lot = lot,
                        quantity = quantity,
                        totalLabel = totalLabel,
                        buyerName = buyerName,
                        buyerTurma = buyerTurma,
                        buyerPhone = buyerPhone,
                        orderCode = requestId.take(8).uppercase(Locale.ROOT),
                    )?.let(uriHandler::openUri)
                }
            },
            icon = Icons.Outlined.Send,
            accent = PremiumBrand,
            enabled = !isSubmitting && lot != null,
        )
        PremiumSecondaryButton(
            text = "Voltar ao passo anterior",
            onClick = onBack,
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            accent = PremiumZinc400,
        )
    }
}

@Composable
private fun EventCheckoutStepThree(
    onOrdersClick: () -> Unit,
    onTicketsClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PremiumCard(accent = PremiumPurple) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(78.dp),
                    shape = CircleShape,
                    color = PremiumPurple.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, PremiumPurple.copy(alpha = 0.55f)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ConfirmationNumber,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
                        tint = PremiumPurple,
                    )
                }
                Text(
                    text = "Ingresso reservado!",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Agora a equipe do evento confere o PIX e libera seu QR Code oficial. Fique de olho no status.",
                    color = PremiumZinc400,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                PremiumInfoRow("[INFO] Status do pedido", "Análise financeira", accent = PremiumAmber)
            }
        }

        PremiumPrimaryButton(
            text = "Ver pedidos",
            onClick = onOrdersClick,
            icon = Icons.Outlined.ConfirmationNumber,
            accent = PremiumPurple,
        )
        PremiumSecondaryButton(
            text = "Meus ingressos",
            onClick = onTicketsClick,
            icon = Icons.Outlined.ConfirmationNumber,
            accent = PremiumBrand,
        )
        PremiumSecondaryButton(
            text = "Voltar ao menu",
            onClick = onBackClick,
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            accent = PremiumZinc400,
        )
    }
}

@Composable
private fun QuantityButton(
    text: String,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(34.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (filled) PremiumBrand else PremiumZinc800,
        border = BorderStroke(1.dp, if (filled) PremiumBrand else PremiumZinc800),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (filled) Color.Black else PremiumZinc400,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
fun EventFlowUnavailableScreen(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 100.dp,
    ) {
        PremiumHeader(
            title = title,
            subtitle = "Fluxo real pendente",
            icon = Icons.Outlined.ShoppingCart,
            onBackClick = onBackClick,
        )
        PremiumEmptyState(
            title = subtitle,
            subtitle = "Esta etapa precisa seguir exatamente o fluxo seguro do web app antes de gravar no Supabase.",
            icon = Icons.Outlined.Payment,
        )
        PremiumSecondaryButton(
            text = "Voltar",
            onClick = onBackClick,
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
        )
    }
}

private fun buildEventReceiptWhatsappUrl(
    phone: String,
    event: Event,
    lot: EventProduct?,
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
        .ifBlank { event.tenantId.uppercase(Locale.ROOT) }
        .ifBlank { "USC" }
    val cleanTitle = event.title.trim().ifBlank { "evento" }.trimEnd('.', '!', '?')
    val ticketLabel = buildString {
        append(quantity.coerceAtLeast(1))
        append("x ")
        append(lot?.name?.trim()?.takeIf(String::isNotBlank) ?: event.lotName.ifBlank { "Ingresso" })
    }
    val cleanOrderCode = orderCode.trim().ifBlank { "Aguardando criação no app" }
    val message = listOf(
        "Fala, equipe *$organizerLabel*! Quero garantir meu lugar no evento $cleanTitle.",
        "",
        "*NOME:* ${buyerName.trim().ifBlank { "Aluno" }}",
        "*TURMA:* ${buyerTurma.trim().ifBlank { "Sem turma" }}",
        "*CONTATO:* ${buyerPhone.trim().ifBlank { "Não informado" }}",
        "*INGRESSO:* $ticketLabel",
        "*VALOR TOTAL:* $totalLabel",
        "*PEDIDO:* $cleanOrderCode",
        "",
        "Segue o comprovante!",
    ).joinToString("\n")
    return "https://wa.me/$normalizedPhone?text=${URLEncoder.encode(message, "UTF-8")}"
}

/** Chave estável do recebedor, como `buildPaymentRecipientKey` do web. */
private fun EventPaymentRecipient.recipientKey(): String =
    userId.trim().ifBlank { "${name.trim()}-${phone.trim()}" }

private fun formatEventTotal(unitPriceLabel: String, quantity: Int): String {
    val value = unitPriceLabel
        .replace("R$", "")
        .replace(".", "")
        .replace(",", ".")
        .trim()
        .toDoubleOrNull()
    if (value == null) return unitPriceLabel
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value * quantity)
}
