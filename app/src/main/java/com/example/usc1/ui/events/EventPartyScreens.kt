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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import com.example.usc1.core.ui.PremiumQrCode
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.EventMenuProduct
import com.example.usc1.domain.model.EventPartyOrder
import com.example.usc1.domain.model.EventPartyVoucher
import com.example.usc1.domain.model.EventPartyVoucherStatus

/** `/eventos/[id]/produtos` — menu do evento. */
@Composable
fun EventPartyMenuScreen(
    state: EventPartyMenuUiState,
    onProductClick: (String) -> Unit,
    onVouchersClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando menu do evento", modifier = modifier)
        return
    }

    val event = state.event
    PremiumScreen(modifier = modifier, bottomPadding = 120.dp) {
        PremiumHeader(
            title = event?.eventMenuTitle ?: "Menu do evento",
            subtitle = event?.title ?: "Evento",
            icon = Icons.Outlined.ShoppingBag,
            onBackClick = onBackClick,
        )

        PremiumSecondaryButton(
            text = "Minhas fichas",
            onClick = onVouchersClick,
            icon = Icons.Outlined.QrCode,
            accent = PremiumBrand,
        )

        when {
            state.errorMessage != null -> PremiumEmptyState(
                title = "Menu indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.ShoppingBag,
            )

            event == null -> PremiumEmptyState(
                title = "Evento não encontrado",
                subtitle = "Este evento não está disponível no tenant ativo.",
                icon = Icons.Outlined.ShoppingBag,
            )

            state.isMenuDisabled -> PremiumEmptyState(
                title = "Menu ainda não liberado",
                subtitle = "O menu deste evento ainda não está ativo.",
                icon = Icons.Outlined.ShoppingBag,
            )

            event.menuProducts.isEmpty() -> PremiumEmptyState(
                title = "Nenhum produto disponível",
                subtitle = "Nenhum produto disponível neste evento.",
                icon = Icons.Outlined.ShoppingBag,
            )

            else -> event.menuProducts.forEach { product ->
                EventPartyProductCard(
                    product = product,
                    fallbackCategory = event.eventMenuCategory,
                    onClick = { onProductClick(product.id) },
                )
            }
        }
    }
}

@Composable
private fun EventPartyProductCard(
    product: EventMenuProduct,
    fallbackCategory: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PremiumZinc900.copy(alpha = 0.86f))
            .border(BorderStroke(1.dp, PremiumZinc800), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (!product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    tint = PremiumZinc500,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = product.category.ifBlank { fallbackCategory }.uppercase(),
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
            )
            Text(
                text = product.name,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = product.description.ifBlank { "Produto disponível no evento." },
                color = PremiumZinc500,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = product.priceLabel,
                    color = PremiumBrand,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                PremiumChip(
                    label = if (product.stockCount > 0) "estoque ${product.stockCount}" else "sob demanda",
                    accent = PremiumZinc400,
                )
            }
            PremiumPrimaryButton(
                text = "Comprar",
                onClick = onClick,
                icon = Icons.Outlined.ShoppingCart,
                accent = PremiumBrand,
            )
        }
    }
}

/** `/eventos/[id]/produtos/[productId]` — ficha do produto do evento. */
@Composable
fun EventPartyProductScreen(
    state: EventPartyProductUiState,
    onQuantityChange: (Int) -> Unit,
    onOrderClick: () -> Unit,
    onVouchersClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando produto", modifier = modifier)
        return
    }

    val event = state.event
    val product = state.product
    PremiumScreen(modifier = modifier, bottomPadding = 120.dp) {
        PremiumHeader(
            title = product?.name ?: "Produto do evento",
            subtitle = event?.title ?: "Evento",
            icon = Icons.Outlined.ShoppingBag,
            onBackClick = onBackClick,
        )

        if (product == null || event == null) {
            PremiumEmptyState(
                title = "Produto indisponível",
                subtitle = state.errorMessage ?: "Este produto não está disponível neste evento.",
                icon = Icons.Outlined.ShoppingBag,
            )
            return@PremiumScreen
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (!product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    tint = PremiumZinc500,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        PremiumCard(accent = PremiumBrand) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumChip(
                    label = product.category.ifBlank { event.eventMenuCategory },
                    accent = PremiumAmber,
                    filled = true,
                )
                PremiumChip(label = product.status, accent = PremiumBrand)
            }
            Text(
                text = product.description.ifBlank { "Produto disponível no evento." },
                color = PremiumZinc300,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            PremiumInfoRow("Preço unitário", product.priceLabel, accent = PremiumBrand)
            PremiumInfoRow("Estoque", product.stockLabel, accent = PremiumZinc400)
        }

        PremiumCard(accent = PremiumAmber) {
            Text(
                text = "QUANTIDADE",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PremiumChip(
                    label = "−",
                    accent = PremiumZinc400,
                    modifier = Modifier.clickable { onQuantityChange(-1) },
                )
                Text(
                    text = state.quantity.toString(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                PremiumChip(
                    label = "+",
                    accent = PremiumBrand,
                    filled = true,
                    modifier = Modifier.clickable { onQuantityChange(1) },
                )
            }
            PremiumInfoRow(
                label = "Total",
                value = formatEventPartyTotal(product.priceValue, state.quantity),
                accent = PremiumBrand,
            )
        }

        state.submitError?.takeIf(String::isNotBlank)?.let { error ->
            Text(text = error, color = PremiumAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (state.createdOrderId != null) {
            PremiumCard(accent = PremiumBrand) {
                Text(
                    text = "Pedido enviado!",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "A equipe do evento vai conferir o pagamento e liberar sua ficha.",
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                PremiumSecondaryButton(
                    text = "Ver minhas fichas",
                    onClick = onVouchersClick,
                    icon = Icons.Outlined.QrCode,
                    accent = PremiumBrand,
                )
            }
        } else {
            PremiumPrimaryButton(
                text = if (state.isSubmitting) "Enviando pedido..." else "Confirmar pedido",
                onClick = onOrderClick,
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                accent = PremiumBrand,
                enabled = !state.isSubmitting,
            )
        }

        PremiumSecondaryButton(
            text = "Voltar ao menu",
            onClick = onBackClick,
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            accent = PremiumZinc400,
        )
    }
}

/** `/eventos/[id]/produtos/fichas` — fichas digitais do usuário no evento. */
@Composable
fun EventPartyVouchersScreen(
    state: EventPartyVouchersUiState,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando suas fichas", modifier = modifier)
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 120.dp) {
        PremiumHeader(
            title = "Minhas fichas",
            subtitle = state.event?.title ?: "Evento",
            icon = Icons.Outlined.QrCode,
            onBackClick = onBackClick,
        )
        Text(
            text = "Use o QR no evento. Se a leitura falhar, informe o código exibido no cartão.",
            color = PremiumZinc500,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        PremiumSecondaryButton(
            text = "Menu do evento",
            onClick = onMenuClick,
            icon = Icons.Outlined.ShoppingBag,
            accent = PremiumZinc400,
        )

        when {
            state.errorMessage != null -> PremiumEmptyState(
                title = "Fichas indisponíveis",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.QrCode,
            )

            state.orders.isEmpty() -> PremiumEmptyState(
                title = "Nenhuma ficha",
                subtitle = "Nenhuma ficha comprada para este evento.",
                icon = Icons.Outlined.QrCode,
            )

            else -> state.orders.forEach { order ->
                EventPartyVoucherOrderCard(order = order)
            }
        }
    }
}

@Composable
private fun EventPartyVoucherOrderCard(order: EventPartyOrder) {
    PremiumCard(accent = eventPartyStatusAccent(order.status)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = order.productName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "${order.quantity} un. • ${order.totalLabel}",
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = order.referenceSummary,
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (order.status != EventPartyVoucherStatus.Pending) {
                    Text(
                        text = "${order.usedCount}/${order.totalCount} retirada(s)",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            PremiumChip(label = order.status.label, accent = eventPartyStatusAccent(order.status))
        }

        order.vouchers.forEach { voucher ->
            EventPartyVoucherCard(voucher = voucher)
        }

        Text(
            text = order.productDescription.ifBlank {
                "Apresente esta ficha no evento quando o pedido for aprovado."
            },
            color = PremiumZinc500,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun EventPartyVoucherCard(voucher: EventPartyVoucher) {
    val isActive = voucher.status == EventPartyVoucherStatus.Active
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PremiumZinc900.copy(alpha = 0.7f))
            .border(BorderStroke(1.dp, PremiumZinc800), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = voucher.label.uppercase(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            PremiumChip(
                label = if (isActive) "Ativo" else voucher.transferNote.ifBlank { voucher.status.label },
                accent = eventPartyStatusAccent(voucher.status),
            )
        }

        if (isActive) {
            PremiumQrCode(payload = voucher.qrPayload, label = "Ficha oficial")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "CÓDIGO DO QR",
                    color = PremiumZinc500,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = voucher.code,
                    color = PremiumZinc300,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                voucher.transferNote.takeIf(String::isNotBlank)?.let { note ->
                    Text(
                        text = note.uppercase(),
                        color = PremiumAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        } else {
            Text(
                text = if (voucher.status == EventPartyVoucherStatus.Pending) {
                    "Aguardando aprovação"
                } else {
                    voucher.transferNote.ifBlank { voucher.status.label }
                },
                color = PremiumZinc400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
            if (voucher.code.isNotBlank()) {
                Text(
                    text = voucher.code,
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun eventPartyStatusAccent(status: EventPartyVoucherStatus): Color = when (status) {
    EventPartyVoucherStatus.Active -> PremiumBrand
    EventPartyVoucherStatus.Partial -> PremiumAmber
    EventPartyVoucherStatus.Pending -> PremiumAmber
    EventPartyVoucherStatus.Used,
    EventPartyVoucherStatus.Inactive,
    EventPartyVoucherStatus.Cancelled,
    -> PremiumRed
    EventPartyVoucherStatus.Transferred,
    EventPartyVoucherStatus.Reversed,
    EventPartyVoucherStatus.Refunded,
    -> PremiumZinc400
}

private fun formatEventPartyTotal(unitPrice: Double, quantity: Int): String =
    java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("pt-BR"))
        .format(unitPrice * quantity.coerceAtLeast(1))
