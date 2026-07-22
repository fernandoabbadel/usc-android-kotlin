package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminUserListItem
import com.example.usc1.domain.model.AdminUserStatus

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminUsersScreen(
    state: AdminUsersUiState,
    onSearchChange: (String) -> Unit,
    onPlanFilterChange: (com.example.usc1.domain.model.AdminUserPlan) -> Unit,
    onLetterGroupChange: (com.example.usc1.domain.model.AdminUsersLetterGroup) -> Unit,
    onEditUser: (AdminUserListItem) -> Unit,
    onToggleStatus: (AdminUserListItem) -> Unit,
    onRequestDelete: (AdminUserListItem) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onLoadMoreClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    state.pendingDeleteUser?.let { user ->
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Confirmar exclusão permanente deste usuário?") },
            text = { Text(user.nome) },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (state.isLoading && state.users.isEmpty()) {
        PremiumLoadingState(text = "Carregando usuários...", modifier = modifier)
        return
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
        verticalSpacing = 16.dp,
    ) {
        PremiumHeader(
            title = "Admin Usuários",
            subtitle = "Paginação 20 em 20 para reduzir leituras",
            icon = Icons.Outlined.Security,
            onBackClick = onBackClick,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Configurar Cadastro",
                onClick = {},
                enabled = false,
                icon = Icons.Outlined.Settings,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Atualizar",
                onClick = onRefreshClick,
                icon = Icons.Outlined.Refresh,
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminUsersStatChip("Usuários", state.total.toString(), PremiumZinc400, Modifier.weight(1f))
            AdminUsersStatChip("Ativos", state.active.toString(), PremiumBrandAccent, Modifier.weight(1f))
            AdminUsersStatChip("Bloqueados", state.blocked.toString(), PremiumZinc400, Modifier.weight(1f))
        }

        PremiumTextField(
            value = state.filters.search,
            onValueChange = onSearchChange,
            label = "Buscar por nome, email, turma ou matrícula",
            leadingIcon = Icons.Outlined.Search,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            adminUsersLetterFilters.forEach { option ->
                FilterPill(
                    label = option.label,
                    selected = state.filters.letterGroup == option,
                    selectedColor = PremiumBrand,
                    onClick = { onLetterGroupChange(option) },
                )
            }
            FilterPill(
                label = "Recontar Follows",
                selected = false,
                selectedColor = Color(0xFF22D3EE),
                enabled = false,
                onClick = {},
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            adminUserPlanFilters.forEach { option ->
                FilterPill(
                    label = option.label,
                    selected = state.filters.plan == option,
                    selectedColor = Color.White,
                    onClick = { onPlanFilterChange(option) },
                )
            }
        }

        state.actionMessage?.let { message ->
            AdminInlineMessage(message = message, color = PremiumBrandAccent)
        }
        state.errorMessage?.let { message ->
            AdminInlineMessage(message = message, color = Color(0xFFFCA5A5))
        }

        PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
            if (state.visibleUsers.isEmpty()) {
                PremiumEmptyState(
                    title = "Nenhum usuário encontrado.",
                    subtitle = "A busca ou filtro atual não retornou usuários para este tenant.",
                    icon = Icons.Outlined.Security,
                )
            } else {
                state.visibleUsers.forEach { user ->
                    AdminUserRow(
                        user = user,
                        isMutating = state.mutatingUserId == user.id,
                        onEdit = { onEditUser(user) },
                        onToggleStatus = { onToggleStatus(user) },
                        onDelete = { onRequestDelete(user) },
                    )
                }
            }
        }

        if (state.hasMore) {
            PremiumSecondaryButton(
                text = if (state.isLoadingMore) "Carregando" else "Carregar mais",
                onClick = onLoadMoreClick,
                enabled = !state.isLoadingMore,
                icon = Icons.Outlined.ExpandMore,
            )
        }
    }
}

@Composable
private fun AdminUsersStatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc700),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                color = PremiumZinc500,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) selectedColor else PremiumZinc900,
        border = BorderStroke(1.dp, if (selected) selectedColor else PremiumZinc800),
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = if (selected && selectedColor == Color.White) Color.Black else if (selected) Color.Black else PremiumZinc400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun AdminInlineMessage(
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

@Composable
private fun AdminUserRow(
    user: AdminUserListItem,
    isMutating: Boolean,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = user.nome,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = user.email.ifBlank { "sem email" },
                        color = PremiumZinc500,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PremiumChip(
                    label = user.status.label,
                    accent = statusColor(user.status),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UserField("Turma", user.turma, Modifier.weight(1f))
                UserField("Plano", user.plano.remoteValue.uppercase(), Modifier.weight(1f))
            }
            UserField("Matrícula (RA)", user.matricula)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumSecondaryButton(
                    text = "Editar",
                    onClick = onEdit,
                    icon = Icons.Outlined.Edit,
                    modifier = Modifier.weight(1f),
                )
                PremiumSecondaryButton(
                    text = if (user.status == AdminUserStatus.Bloqueado) "Desbloquear" else "Bloquear",
                    onClick = onToggleStatus,
                    enabled = !isMutating,
                    icon = if (user.status == AdminUserStatus.Bloqueado) Icons.Outlined.VerifiedUser else Icons.Outlined.Block,
                    modifier = Modifier.weight(1f),
                )
                PremiumSecondaryButton(
                    text = "Excluir",
                    onClick = onDelete,
                    enabled = !isMutating,
                    accent = Color(0xFFFCA5A5),
                    icon = Icons.Outlined.Delete,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun UserField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = PremiumZinc500,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = value.ifBlank { "-" },
            color = PremiumZinc400,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun statusColor(status: AdminUserStatus): Color {
    return when (status) {
        AdminUserStatus.Ativo -> PremiumBrandAccent
        AdminUserStatus.Inadimplente -> Color(0xFFF87171)
        AdminUserStatus.Pendente -> Color(0xFFFACC15)
        AdminUserStatus.Bloqueado -> PremiumZinc400
    }
}
