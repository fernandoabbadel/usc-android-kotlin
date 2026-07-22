package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminStoreMenuItem
import com.example.usc1.domain.model.AdminStoreMenuKind

@Composable
fun AdminStoreScreen(
    state: AdminStoreUiState,
    onPixKeyChange: (String) -> Unit,
    onBankChange: (String) -> Unit,
    onHolderChange: (String) -> Unit,
    onWhatsappChange: (String) -> Unit,
    onSaveFinanceClick: () -> Unit,
    onMenuItemClick: (AdminStoreMenuItem) -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.tenantId.isBlank()) {
        PremiumLoadingState(text = "Carregando loja...", modifier = modifier)
        return
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
    ) {
        PremiumHeader(
            title = "Admin Loja",
            subtitle = "PIX / Comprovante da Loja",
            icon = Icons.Outlined.ShoppingBag,
            onBackClick = onBackClick,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Categoria",
                onClick = {},
                enabled = false,
                icon = Icons.Outlined.Category,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Novo Produto",
                onClick = {},
                enabled = false,
                icon = Icons.Outlined.Inventory2,
                modifier = Modifier.weight(1f),
            )
        }

        PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PIX / Comprovante da Loja",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Esses dados aparecem na sequencia de confirmacao do pedido da loja e em Meus Pedidos.",
                        color = PremiumZinc500,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Wallet,
                    contentDescription = null,
                    tint = PremiumBrandAccent,
                    modifier = Modifier.size(22.dp),
                )
            }

            PremiumTextField(
                value = state.finance.chave,
                onValueChange = onPixKeyChange,
                label = "Chave PIX (email/CNPJ/telefone/aleatoria)",
            )
            PremiumTextField(
                value = state.finance.banco,
                onValueChange = onBankChange,
                label = "Banco",
            )
            PremiumTextField(
                value = state.finance.titular,
                onValueChange = onHolderChange,
                label = "Nome do titular",
            )
            PremiumTextField(
                value = state.finance.whatsapp,
                onValueChange = onWhatsappChange,
                label = "WhatsApp para enviar comprovante (somente número com DDI)",
            )
            PremiumPrimaryButton(
                text = if (state.isSavingFinance) "Salvando..." else "Salvar PIX",
                onClick = onSaveFinanceClick,
                enabled = !state.isSavingFinance,
                loading = state.isSavingFinance,
                icon = Icons.Outlined.Save,
            )
        }

        state.saveMessage?.let { message ->
            AdminInlineStoreMessage(message = message, color = PremiumBrandAccent)
        }
        state.errorMessage?.let { error ->
            AdminInlineStoreMessage(message = error, color = Color(0xFFFCA5A5))
        }

        state.menuItems.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    AdminStoreMenuCard(
                        item = item,
                        onClick = { onMenuItemClick(item) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }

        PremiumSecondaryButton(
            text = "Atualizar",
            onClick = onRefreshClick,
            icon = Icons.Outlined.Refresh,
        )
    }
}

@Composable
private fun AdminStoreMenuCard(
    item: AdminStoreMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumCard(
        modifier = modifier.clickable(onClick = onClick),
        accent = menuColor(item.kind),
        containerColor = PremiumZinc900,
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = menuColor(item.kind).copy(alpha = 0.10f),
            border = BorderStroke(1.dp, menuColor(item.kind).copy(alpha = 0.30f)),
        ) {
            Icon(
                imageVector = menuIcon(item.kind),
                contentDescription = null,
                tint = menuColor(item.kind),
                modifier = Modifier.padding(11.dp),
            )
        }
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.description,
            color = PremiumZinc400,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AdminInlineStoreMessage(message: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun menuColor(kind: AdminStoreMenuKind): Color {
    return when (kind) {
        AdminStoreMenuKind.Category -> Color(0xFF22D3EE)
        AdminStoreMenuKind.Products -> Color(0xFF60A5FA)
        AdminStoreMenuKind.Disabled -> Color(0xFFF87171)
        AdminStoreMenuKind.PendingOrders -> PremiumAmber
        AdminStoreMenuKind.ApprovedOrders -> Color(0xFF22D3EE)
        AdminStoreMenuKind.Reviews -> PremiumBrand
    }
}

private fun menuIcon(kind: AdminStoreMenuKind): ImageVector {
    return when (kind) {
        AdminStoreMenuKind.Category -> Icons.Outlined.Category
        AdminStoreMenuKind.Products,
        AdminStoreMenuKind.Disabled -> Icons.Outlined.Inventory2
        AdminStoreMenuKind.PendingOrders,
        AdminStoreMenuKind.ApprovedOrders -> Icons.Outlined.ShoppingBag
        AdminStoreMenuKind.Reviews -> Icons.Outlined.Message
    }
}
