package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminStoreOrder
import com.example.usc1.domain.model.AdminStoreOrdersMode
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminStoreOrdersScreen(
    state: AdminStoreOrdersUiState,
    onPendingModeClick: () -> Unit,
    onApprovedModeClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onCategoryModeClick: (AdminStoreOrdersMode, String) -> Unit,
    onAllCategoriesClick: () -> Unit,
    onApproveClick: (AdminStoreOrder) -> Unit,
    onRejectClick: (AdminStoreOrder) -> Unit,
    onEditApprovalClick: (AdminStoreOrder) -> Unit,
    onReturnToPendingClick: (AdminStoreOrder) -> Unit,
    onMarkDeliveredClick: (AdminStoreOrder) -> Unit,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.rows.isEmpty()) {
        PremiumLoadingState(text = "Carregando...", modifier = modifier)
        return
    }

    val groupedRows = if (state.categoryLabel.isBlank()) {
        state.rows.groupBy { it.productCategory.ifBlank { "Sem categoria" } }
            .toSortedMap(compareBy { it })
            .map { (category, rows) -> category to rows }
    } else {
        listOf(state.categoryLabel to state.rows)
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
        verticalSpacing = 16.dp,
    ) {
        PremiumHeader(
            title = state.title,
            subtitle = state.subtitle,
            icon = if (state.mode == AdminStoreOrdersMode.Pending) Icons.Outlined.Schedule else Icons.Outlined.ShoppingBag,
            accent = if (state.mode == AdminStoreOrdersMode.Pending) PremiumAmber else Color(0xFF22D3EE),
            onBackClick = onBackClick,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Pendentes",
                onClick = onPendingModeClick,
                accent = if (state.mode == AdminStoreOrdersMode.Pending) PremiumAmber else PremiumZinc400,
                icon = Icons.Outlined.Schedule,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Aprovados",
                onClick = onApprovedModeClick,
                accent = if (state.mode == AdminStoreOrdersMode.Approved) Color(0xFF22D3EE) else PremiumZinc400,
                icon = Icons.Outlined.ShoppingBag,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.categoryNames.isNotEmpty()) {
            PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Categorias",
                            color = PremiumZinc500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "Cada botão abre a página paginada da categoria.",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (state.categoryLabel.isNotBlank()) {
                        CategoryPill(
                            label = "Voltar para todas",
                            selected = false,
                            accent = PremiumZinc400,
                            onClick = onAllCategoriesClick,
                        )
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.categoryNames.forEach { label ->
                        CategoryPill(
                            label = label,
                            selected = state.categoryLabel == label,
                            accent = if (state.mode == AdminStoreOrdersMode.Pending) PremiumAmber else Color(0xFF22D3EE),
                            onClick = { onCategoryClick(label) },
                        )
                    }
                }
            }
        }

        state.actionMessage?.let { message ->
            AdminStoreOrdersMessage(message = message, color = PremiumBrandAccent)
        }
        state.errorMessage?.let { message ->
            AdminStoreOrdersMessage(message = message, color = Color(0xFFFCA5A5))
        }

        if (state.rows.isEmpty()) {
            PremiumEmptyState(
                title = state.emptyText,
                subtitle = "A consulta paginada não retornou pedidos para este filtro.",
                icon = Icons.Outlined.ShoppingBag,
                accent = if (state.mode == AdminStoreOrdersMode.Pending) PremiumAmber else Color(0xFF22D3EE),
            )
        } else {
            groupedRows.forEach { (category, rows) ->
                if (state.categoryLabel.isBlank()) {
                    StoreOrdersCategoryHeader(
                        category = category,
                        count = rows.size,
                        onPendingClick = { onCategoryModeClick(AdminStoreOrdersMode.Pending, category) },
                        onApprovedClick = { onCategoryModeClick(AdminStoreOrdersMode.Approved, category) },
                    )
                }
                rows.forEach { order ->
                    AdminStoreOrderCard(
                        order = order,
                        mode = state.mode,
                        isBusy = state.mutatingOrderId == order.id,
                        isEditing = state.editingOrderId == order.id,
                        onApproveClick = { onApproveClick(order) },
                        onRejectClick = { onRejectClick(order) },
                        onEditApprovalClick = { onEditApprovalClick(order) },
                        onReturnToPendingClick = { onReturnToPendingClick(order) },
                        onMarkDeliveredClick = { onMarkDeliveredClick(order) },
                    )
                }
            }
        }

        if (state.page > 1 || state.hasMore) {
            StoreOrdersPager(
                page = state.page,
                hasMore = state.hasMore,
                onPreviousPageClick = onPreviousPageClick,
                onNextPageClick = onNextPageClick,
            )
        }

        PremiumSecondaryButton(
            text = "Atualizar",
            onClick = onRefreshClick,
            icon = Icons.Outlined.Refresh,
        )
    }
}

@Composable
private fun StoreOrdersCategoryHeader(
    category: String,
    count: Int,
    onPendingClick: () -> Unit,
    onApprovedClick: () -> Unit,
) {
    PremiumCard(accent = PremiumZinc800, containerColor = Color.Black.copy(alpha = 0.20f)) {
        Text(
            text = "Categoria",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = category,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$count pedido${if (count == 1) "" else "s"} nesta página.",
            color = PremiumZinc500,
            fontSize = 11.sp,
        )
        if (category != "Sem categoria") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumSecondaryButton(
                    text = "Pendentes",
                    onClick = onPendingClick,
                    accent = PremiumAmber,
                    modifier = Modifier.weight(1f),
                )
                PremiumSecondaryButton(
                    text = "Aprovados",
                    onClick = onApprovedClick,
                    accent = Color(0xFF22D3EE),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AdminStoreOrderCard(
    order: AdminStoreOrder,
    mode: AdminStoreOrdersMode,
    isBusy: Boolean,
    isEditing: Boolean,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    onEditApprovalClick: () -> Unit,
    onReturnToPendingClick: () -> Unit,
    onMarkDeliveredClick: () -> Unit,
) {
    PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.productName.ifBlank { "Produto" },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    PremiumChip(
                        label = if (mode == AdminStoreOrdersMode.Pending) "Pendente" else "Confirmado",
                        accent = if (mode == AdminStoreOrdersMode.Pending) PremiumAmber else Color(0xFF22D3EE),
                    )
                }
                Text(
                    text = "Comprador: ${order.userName.ifBlank { "Usuário" }}",
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                )
                Text(
                    text = "Quantidade: ${maxOf(1, order.quantidade)}",
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                )
                if (order.variantLabel.isNotBlank()) {
                    Text(
                        text = order.variantLabel,
                        color = PremiumZinc400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "Comprovante para: ${order.receiverLabel}",
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                )
                Text(
                    text = "#${order.id.take(10)}",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Text(
                text = formatCurrency(order.total.takeIf { it > 0.0 } ?: order.price),
                color = PremiumBrandAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
        }

        if (mode == AdminStoreOrdersMode.Approved) {
            ApprovedOrderInfo(order = order)
        }

        if (mode == AdminStoreOrdersMode.Pending) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumSecondaryButton(
                    text = "Aprovar",
                    onClick = onApproveClick,
                    enabled = !isBusy,
                    accent = PremiumBrandAccent,
                    icon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.weight(1f),
                )
                PremiumSecondaryButton(
                    text = "Rejeitar",
                    onClick = onRejectClick,
                    enabled = !isBusy,
                    accent = Color(0xFFF87171),
                    icon = Icons.Outlined.Cancel,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            PremiumSecondaryButton(
                text = if (isEditing) "Fechar edição" else "Editar aprovação",
                onClick = onEditApprovalClick,
                accent = Color(0xFF22D3EE),
                icon = Icons.Outlined.Edit,
            )
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumSecondaryButton(
                        text = "Voltar para pendente",
                        onClick = onReturnToPendingClick,
                        enabled = !isBusy,
                        accent = PremiumAmber,
                        icon = Icons.Outlined.RestartAlt,
                    )
                    PremiumSecondaryButton(
                        text = "Marcar entregue",
                        onClick = onMarkDeliveredClick,
                        enabled = !isBusy,
                        accent = PremiumBrandAccent,
                        icon = Icons.Outlined.LocalShipping,
                    )
                    PremiumSecondaryButton(
                        text = "Rejeitar",
                        onClick = onRejectClick,
                        enabled = !isBusy,
                        accent = Color(0xFFF87171),
                        icon = Icons.Outlined.Cancel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ApprovedOrderInfo(order: AdminStoreOrder) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OrderInfoBox(label = "Aprovado por", value = order.approvedBy.ifBlank { "Não informado" })
        OrderInfoBox(label = "Comprovante para", value = order.receiverLabel)
        OrderInfoBox(label = "Data da aprovação", value = formatDateTime(order.updatedAt.ifBlank { order.createdAt }))
    }
}

@Composable
private fun OrderInfoBox(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CategoryPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accent.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.40f) else PremiumZinc700),
        onClick = onClick,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = if (selected) accent else PremiumZinc400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun StoreOrdersPager(
    page: Int,
    hasMore: Boolean,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Página $page",
            color = PremiumZinc500,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
        )
        PremiumSecondaryButton(
            text = "Anterior",
            onClick = onPreviousPageClick,
            enabled = page > 1,
            modifier = Modifier.weight(1f),
        )
        PremiumSecondaryButton(
            text = "Próxima",
            onClick = onNextPageClick,
            enabled = hasMore,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AdminStoreOrdersMessage(
    message: String,
    color: Color,
) {
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

private fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}

private fun formatDateTime(value: String): String {
    if (value.isBlank()) return "Não informado"
    return runCatching {
        OffsetDateTime.parse(value).format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale("pt", "BR")),
        )
    }.getOrElse { "Não informado" }
}
