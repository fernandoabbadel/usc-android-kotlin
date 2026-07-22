package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.usc1.domain.model.AdminPermissionRole
import com.example.usc1.domain.model.AdminUserListItem
import com.example.usc1.domain.model.AdminUserStatus
import com.example.usc1.domain.model.AdminUsersLetterGroup

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminPermissionUsersScreen(
    state: AdminPermissionUsersUiState,
    tenantLabel: String,
    currentUserId: String,
    canAssignTurmaLeader: Boolean,
    onSearchChange: (String) -> Unit,
    onLetterGroupChange: (AdminUsersLetterGroup) -> Unit,
    onRoleChange: (AdminUserListItem, String) -> Unit,
    onSaveRole: (AdminUserListItem) -> Unit,
    onSaveAllRoles: () -> Unit,
    onToggleTurmaLeader: (AdminUserListItem) -> Unit,
    onLoadMoreClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onStatusCompleteClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.users.isEmpty()) {
        PremiumLoadingState(text = "Carregando usuários...", modifier = modifier)
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 16.dp) {
        PremiumHeader(
            title = "Cargos de Acesso",
            subtitle = "$tenantLabel • paginação por grupos",
            icon = Icons.Outlined.Security,
            accent = Color(0xFF22D3EE),
            onBackClick = onBackClick,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Status Completo",
                onClick = onStatusCompleteClick,
                icon = Icons.Outlined.People,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Atualizar",
                onClick = onRefreshClick,
                icon = Icons.Outlined.Refresh,
                modifier = Modifier.weight(1f),
            )
        }

        PremiumCard(accent = Color(0xFF22D3EE), containerColor = PremiumZinc900) {
            PremiumTextField(
                value = state.filters.search,
                onValueChange = onSearchChange,
                label = "Buscar usuário por nome, email, turma ou matrícula...",
                leadingIcon = Icons.Outlined.Search,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                adminPermissionUsersLetterFilters.forEach { filter ->
                    PermissionFilterPill(
                        label = filter.label,
                        selected = state.filters.letterGroup == filter,
                        onClick = { onLetterGroupChange(filter) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        state.pendingRolesCount == 1 -> "1 alteração pendente."
                        state.pendingRolesCount > 1 -> "${state.pendingRolesCount} alterações pendentes."
                        state.filters.search.isNotBlank() -> "Busca em todos os usuários do tenant."
                        else -> "Escolha o cargo e clique em salvar."
                    },
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                PremiumPrimaryButton(
                    text = if (state.isSavingAnyRole) "Salvando..." else "Salvar alterações",
                    onClick = onSaveAllRoles,
                    enabled = state.pendingRolesCount > 0 && !state.isSavingAnyRole,
                    loading = state.isSavingAnyRole,
                    icon = Icons.Outlined.Save,
                    accent = PremiumBrandAccent,
                    modifier = Modifier.weight(0.9f),
                )
            }
        }

        state.actionMessage?.let { message ->
            PermissionInlineMessage(message = message, color = PremiumBrandAccent)
        }
        state.errorMessage?.let { message ->
            PermissionInlineMessage(message = message, color = Color(0xFFFCA5A5))
        }

        if (state.users.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum usuário encontrado.",
                subtitle = "A busca ou filtro atual não retornou usuários para este tenant.",
                icon = Icons.Outlined.Security,
            )
        } else {
            state.users.forEach { user ->
                val selectedRole = state.pendingRoles[user.id]
                    ?: AdminPermissionRole.normalize(user.role)
                val roleLocked = user.id == currentUserId
                PermissionUserRow(
                    user = user,
                    selectedRole = selectedRole,
                    hasPendingRole = state.pendingRoles[user.id] != null,
                    isSavingRole = user.id in state.savingUserIds,
                    isMutatingLeader = state.mutatingLeaderUserId == user.id,
                    roleLocked = roleLocked,
                    canAssignTurmaLeader = canAssignTurmaLeader && !roleLocked,
                    onRoleChange = { role -> onRoleChange(user, role) },
                    onSaveRole = { onSaveRole(user) },
                    onToggleTurmaLeader = { onToggleTurmaLeader(user) },
                )
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
private fun PermissionFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color(0xFF22D3EE) else Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, if (selected) Color(0xFF22D3EE) else PremiumZinc700),
        onClick = onClick,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = if (selected) Color.Black else PremiumZinc400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun PermissionInlineMessage(
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
@OptIn(ExperimentalLayoutApi::class)
private fun PermissionUserRow(
    user: AdminUserListItem,
    selectedRole: String,
    hasPendingRole: Boolean,
    isSavingRole: Boolean,
    isMutatingLeader: Boolean,
    roleLocked: Boolean,
    canAssignTurmaLeader: Boolean,
    onRoleChange: (String) -> Unit,
    onSaveRole: () -> Unit,
    onToggleTurmaLeader: () -> Unit,
) {
    PremiumCard(accent = if (hasPendingRole) Color(0xFFFACC15) else PremiumZinc800, containerColor = PremiumZinc900) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.nome.ifBlank { "Sem Nome" },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (roleLocked) {
                        PremiumChip(label = "VOCÊ", accent = PremiumBrandAccent)
                    }
                    if (user.isTurmaLeader) {
                        PremiumChip(label = "LÍDER DA TURMA", accent = Color(0xFF22D3EE))
                    }
                }
                Text(
                    text = user.email.ifBlank { "sem email" },
                    color = PremiumZinc500,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Turma: ${user.turma.ifBlank { "---" }} • Matrícula: ${user.matricula.ifBlank { "---" }}",
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PremiumChip(label = user.status.label, accent = permissionStatusColor(user.status))
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RoleDropdown(
                selectedRole = selectedRole,
                enabled = !roleLocked && !isSavingRole,
                hasPendingRole = hasPendingRole,
                onRoleChange = onRoleChange,
            )
            PremiumSecondaryButton(
                text = if (isSavingRole) "Salvando..." else "Salvar",
                onClick = onSaveRole,
                enabled = hasPendingRole && !roleLocked && !isSavingRole,
                icon = Icons.Outlined.Save,
            )
            PremiumSecondaryButton(
                text = if (user.isTurmaLeader) "Remover líder" else "Virar líder",
                onClick = onToggleTurmaLeader,
                enabled = canAssignTurmaLeader && !isMutatingLeader,
                accent = if (user.isTurmaLeader) Color(0xFF22D3EE) else PremiumZinc400,
            )
        }
    }
}

@Composable
private fun RoleDropdown(
    selectedRole: String,
    enabled: Boolean,
    hasPendingRole: Boolean,
    onRoleChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.Black.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, if (hasPendingRole) Color(0xFFFACC15) else PremiumZinc700),
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = AdminPermissionRole.labelFor(selectedRole),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = PremiumZinc400,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            adminPermissionRoles.forEach { role ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = role.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    onClick = {
                        expanded = false
                        onRoleChange(role.remoteValue)
                    },
                )
            }
        }
    }
}

private fun permissionStatusColor(status: AdminUserStatus): Color {
    return when (status) {
        AdminUserStatus.Ativo -> PremiumBrandAccent
        AdminUserStatus.Inadimplente -> Color(0xFFF87171)
        AdminUserStatus.Pendente -> Color(0xFFFACC15)
        AdminUserStatus.Bloqueado -> PremiumZinc400
    }
}
