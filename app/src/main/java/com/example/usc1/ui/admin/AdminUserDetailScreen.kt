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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminUserPlan
import com.example.usc1.domain.model.AdminUserStatus

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminUserDetailScreen(
    state: AdminUserDetailUiState,
    onNomeChange: (String) -> Unit,
    onTelefoneChange: (String) -> Unit,
    onMatriculaChange: (String) -> Unit,
    onTurmaChange: (String) -> Unit,
    onPlanoChange: (AdminUserPlan) -> Unit,
    onStatusChange: (AdminUserStatus) -> Unit,
    onSaveClick: () -> Unit,
    onToggleStatusClick: () -> Unit,
    onRequestDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Confirmar exclusão permanente deste usuário?") },
            text = { Text(state.profile?.nome.orEmpty()) },
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

    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando...", modifier = modifier)
        return
    }

    val profile = state.profile
    val form = state.form
    if (profile == null || form == null) {
        PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Perfil do Usuário",
                subtitle = "UID: ${state.userId}",
                icon = Icons.Outlined.Person,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Usuário não encontrado.",
                subtitle = state.errorMessage ?: "O Supabase não retornou perfil para este tenant.",
                icon = Icons.Outlined.Person,
            )
            PremiumSecondaryButton(
                text = "Atualizar",
                onClick = onRefreshClick,
                icon = Icons.Outlined.Refresh,
            )
        }
        return
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
        verticalSpacing = 16.dp,
    ) {
        PremiumHeader(
            title = "Perfil do Usuário",
            subtitle = "UID: ${state.userId}",
            icon = Icons.Outlined.Person,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
            Text(
                text = profile.email.ifBlank { "sem email" },
                color = PremiumZinc400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "role: ${roleLabel(profile.role)}",
                color = PremiumZinc500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
            PremiumTextField(
                value = form.nome,
                onValueChange = onNomeChange,
                label = "Nome",
            )
            PremiumTextField(
                value = form.telefone,
                onValueChange = onTelefoneChange,
                label = "Telefone",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            PremiumTextField(
                value = form.matricula,
                onValueChange = onMatriculaChange,
                label = "Matricula (RA)",
            )
            PremiumTextField(
                value = form.turma,
                onValueChange = onTurmaChange,
                label = "Turma",
            )

            AdminUserDetailOptionGroup(label = "Plano") {
                listOf(
                    AdminUserPlan.Bicho,
                    AdminUserPlan.Cardume,
                    AdminUserPlan.Atleta,
                    AdminUserPlan.Lenda,
                ).forEach { option ->
                    AdminUserDetailOptionPill(
                        label = option.label,
                        selected = form.plano == option,
                        selectedColor = Color.White,
                        onClick = { onPlanoChange(option) },
                    )
                }
            }

            AdminUserDetailOptionGroup(label = "Status") {
                AdminUserStatus.entries.forEach { option ->
                    AdminUserDetailOptionPill(
                        label = option.label,
                        selected = form.status == option,
                        selectedColor = statusColor(option),
                        onClick = { onStatusChange(option) },
                    )
                }
            }

            state.actionMessage?.let { message ->
                AdminUserDetailMessage(message = message, color = PremiumBrandAccent)
            }
            state.errorMessage?.let { message ->
                AdminUserDetailMessage(message = message, color = Color(0xFFFCA5A5))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumPrimaryButton(
                    text = "Salvar",
                    onClick = onSaveClick,
                    loading = state.isSaving,
                    icon = Icons.Outlined.Save,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumSecondaryButton(
                        text = if (form.status == AdminUserStatus.Bloqueado) "Desbloquear" else "Bloquear",
                        onClick = onToggleStatusClick,
                        enabled = !state.isChangingStatus,
                        icon = if (form.status == AdminUserStatus.Bloqueado) {
                            Icons.Outlined.VerifiedUser
                        } else {
                            Icons.Outlined.Block
                        },
                        modifier = Modifier.weight(1f),
                    )
                    PremiumSecondaryButton(
                        text = "Excluir",
                        onClick = onRequestDelete,
                        enabled = !state.isDeleting,
                        accent = Color(0xFFFCA5A5),
                        icon = Icons.Outlined.Delete,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AdminUserDetailOptionGroup(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = PremiumZinc500,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun AdminUserDetailOptionPill(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) selectedColor else Color.Black,
        border = BorderStroke(1.dp, if (selected) selectedColor else PremiumZinc700),
        onClick = onClick,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = if (selected) Color.Black else PremiumZinc400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun AdminUserDetailMessage(
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

private fun statusColor(status: AdminUserStatus): Color {
    return when (status) {
        AdminUserStatus.Ativo -> PremiumBrandAccent
        AdminUserStatus.Inadimplente -> Color(0xFFF87171)
        AdminUserStatus.Pendente -> Color(0xFFFACC15)
        AdminUserStatus.Bloqueado -> PremiumZinc400
    }
}

private fun roleLabel(roleRaw: String): String {
    return when (roleRaw.trim().lowercase()) {
        "guest", "visitante" -> "Visitante"
        "user" -> "Membro"
        "mini_vendor" -> "Mini Vendor"
        "treinador" -> "Treinador"
        "empresa" -> "Empresa"
        "admin_treino" -> "Admin Treino"
        "admin_geral", "admin_tenant" -> "Admin Geral"
        "admin_gestor" -> "Admin Gestor"
        "master_tenant" -> "Master Tenant"
        "master" -> "Master"
        "vendas" -> "Vendas"
        "inactive" -> "Inativo"
        "banned" -> "Banido"
        "" -> "Visitante"
        else -> roleRaw
            .trim()
            .split("_", " ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar(Char::uppercaseChar) }
    }
}
