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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminMiniVendor
import com.example.usc1.domain.model.AdminMiniVendorDirectoryMode
import com.example.usc1.domain.model.AdminMiniVendorStatus

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminMiniVendorsScreen(
    state: AdminMiniVendorsUiState,
    onApproveClick: (AdminMiniVendor) -> Unit,
    onRejectClick: (AdminMiniVendor) -> Unit,
    onDisableClick: (AdminMiniVendor) -> Unit,
    onToggleCategoryVisibilityClick: (AdminMiniVendor) -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.rows.isEmpty()) {
        PremiumLoadingState(text = "Carregando...", modifier = modifier)
        return
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
        verticalSpacing = 16.dp,
    ) {
        PremiumHeader(
            title = state.title,
            subtitle = state.subtitle,
            icon = Icons.Outlined.Store,
            accent = if (state.mode == AdminMiniVendorDirectoryMode.Approvals) PremiumAmber else Color(0xFF60A5FA),
            onBackClick = onBackClick,
        )

        state.actionMessage?.let { message ->
            AdminMiniVendorMessage(message = message, color = PremiumBrandAccent)
        }
        state.errorMessage?.let { message ->
            AdminMiniVendorMessage(message = message, color = Color(0xFFFCA5A5))
        }

        PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniVendorStat("Total", state.rows.size.toString(), PremiumZinc400, Modifier.weight(1f))
                MiniVendorStat(
                    "Pendentes",
                    state.rows.count { it.status == AdminMiniVendorStatus.Pending }.toString(),
                    PremiumAmber,
                    Modifier.weight(1f),
                )
                MiniVendorStat(
                    "Aprovados",
                    state.rows.count { it.status == AdminMiniVendorStatus.Approved }.toString(),
                    Color(0xFF60A5FA),
                    Modifier.weight(1f),
                )
            }
        }

        if (state.visibleRows.isEmpty()) {
            PremiumEmptyState(
                title = state.emptyText,
                subtitle = "A consulta do tenant ativo não retornou lojinhas para este modo.",
                icon = Icons.Outlined.Store,
                accent = if (state.mode == AdminMiniVendorDirectoryMode.Approvals) PremiumAmber else Color(0xFF60A5FA),
            )
        } else {
            state.visibleRows.forEach { row ->
                AdminMiniVendorCard(
                    row = row,
                    mode = state.mode,
                    isBusy = state.mutatingId == row.id,
                    isCategoryBusy = state.mutatingId == "category:${row.id}",
                    onApproveClick = { onApproveClick(row) },
                    onRejectClick = { onRejectClick(row) },
                    onDisableClick = { onDisableClick(row) },
                    onToggleCategoryVisibilityClick = { onToggleCategoryVisibilityClick(row) },
                )
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
private fun MiniVendorStat(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                color = PremiumZinc500,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = value,
                color = accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AdminMiniVendorCard(
    row: AdminMiniVendor,
    mode: AdminMiniVendorDirectoryMode,
    isBusy: Boolean,
    isCategoryBusy: Boolean,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    onDisableClick: () -> Unit,
    onToggleCategoryVisibilityClick: () -> Unit,
) {
    PremiumCard(accent = PremiumZinc800, containerColor = Color.Black.copy(alpha = 0.20f)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF60A5FA).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.30f)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Store,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp),
                    tint = Color(0xFF60A5FA),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.storeName.ifBlank { "Loja sem nome" },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    PremiumChip(label = row.status.label, accent = statusColor(row.status))
                }
                Text(
                    text = "Usuário: ${row.userId}",
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.description.ifBlank { "Sem descrição." },
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = if (mode == AdminMiniVendorDirectoryMode.Approvals) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (mode == AdminMiniVendorDirectoryMode.Vendors) {
                    Text(
                        text = "PIX: ${row.pixKey.ifBlank { "-" }} | Banco: ${row.pixBank.ifBlank { "-" }}",
                        color = PremiumZinc500,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Categoria na loja: ${if (row.categoryVisible) "visivel" else "oculta"}",
                        color = if (row.categoryVisible) PremiumBrandAccent else Color(0xFFF87171),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Instagram: ${if (row.instagramEnabled) row.instagram.ifBlank { "-" } else "desligado"} | WhatsApp: ${if (row.whatsappEnabled) row.whatsapp.ifBlank { "-" } else "desligado"}",
                        color = PremiumZinc500,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (mode == AdminMiniVendorDirectoryMode.Vendors) {
                MiniVendorDisabledAction("Pagina do mini vendor", Icons.Outlined.OpenInNew)
                MiniVendorDisabledAction("Editar dados", Icons.Outlined.Edit)
                MiniVendorDisabledAction("Editar produtos", Icons.Outlined.Inventory2)
                MiniVendorDisabledAction("Pedidos pendentes", Icons.Outlined.OpenInNew)
                MiniVendorDisabledAction("Pedidos aprovados", Icons.Outlined.OpenInNew)
                MiniVendorAction(
                    text = if (row.categoryVisible) "Ocultar categoria" else "Exibir categoria",
                    accent = if (row.categoryVisible) Color(0xFFF87171) else PremiumBrandAccent,
                    icon = if (row.categoryVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    enabled = !isCategoryBusy,
                    onClick = onToggleCategoryVisibilityClick,
                )
            }

            if (row.status != AdminMiniVendorStatus.Approved) {
                MiniVendorAction(
                    text = "Aprovar",
                    accent = if (mode == AdminMiniVendorDirectoryMode.Approvals) PremiumBrandAccent else Color(0xFF60A5FA),
                    icon = Icons.Outlined.CheckCircle,
                    enabled = !isBusy,
                    onClick = onApproveClick,
                )
                MiniVendorAction(
                    text = "Rejeitar",
                    accent = Color(0xFFF87171),
                    icon = Icons.Outlined.Cancel,
                    enabled = !isBusy,
                    onClick = onRejectClick,
                )
            } else {
                MiniVendorAction(
                    text = "Desativar",
                    accent = PremiumZinc400,
                    icon = Icons.Outlined.Cancel,
                    enabled = !isBusy,
                    onClick = onDisableClick,
                )
            }
        }
    }
}

@Composable
private fun MiniVendorAction(
    text: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            Text(text = text, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MiniVendorDisabledAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    MiniVendorAction(
        text = text,
        accent = PremiumZinc500,
        icon = icon,
        enabled = false,
        onClick = {},
    )
}

@Composable
private fun AdminMiniVendorMessage(
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

private fun statusColor(status: AdminMiniVendorStatus): Color {
    return when (status) {
        AdminMiniVendorStatus.Approved -> Color(0xFF60A5FA)
        AdminMiniVendorStatus.Rejected -> Color(0xFFF87171)
        AdminMiniVendorStatus.Disabled -> PremiumZinc400
        AdminMiniVendorStatus.Pending -> PremiumAmber
    }
}
