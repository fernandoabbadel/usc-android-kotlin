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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.usc1.domain.model.AdminReportItem
import com.example.usc1.domain.model.AdminReportStatus
import com.example.usc1.domain.model.AdminReportsSection

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminReportsHubScreen(
    onSectionClick: (AdminReportsSection) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
        verticalSpacing = 16.dp,
    ) {
        PremiumHeader(
            title = "Admin Denúncias",
            subtitle = "Rotas separadas por categoria",
            icon = Icons.Outlined.Warning,
            accent = Color(0xFFF87171),
            onBackClick = onBackClick,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReportHubCard(
                title = "Banidos",
                description = "Recursos de bloqueio",
                icon = Icons.Outlined.Warning,
                accent = Color(0xFFF87171),
                onClick = { onSectionClick(AdminReportsSection.Banned) },
            )
            ReportHubCard(
                title = "Comunidade",
                description = "Mensagens denunciadas",
                icon = Icons.Outlined.Message,
                accent = Color(0xFF60A5FA),
                onClick = { onSectionClick(AdminReportsSection.Community) },
            )
            ReportHubCard(
                title = "Gym",
                description = "Denuncias relacionadas a treino",
                icon = Icons.Outlined.FitnessCenter,
                accent = PremiumAmber,
                onClick = { onSectionClick(AdminReportsSection.Gym) },
            )
            ReportHubCard(
                title = "Suporte",
                description = "Chamados de /configuracoes/suporte",
                icon = Icons.Outlined.SupportAgent,
                accent = PremiumBrandAccent,
                onClick = { onSectionClick(AdminReportsSection.Support) },
            )
        }
    }
}

@Composable
fun AdminReportsListScreen(
    state: AdminReportsUiState,
    onResponseChange: (String, String) -> Unit,
    onResolveClick: (AdminReportItem) -> Unit,
    onDeleteClick: (AdminReportItem) -> Unit,
    onUserClick: (String) -> Unit,
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

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
        verticalSpacing = 16.dp,
    ) {
        PremiumHeader(
            title = state.section.title,
            subtitle = state.section.subtitle,
            icon = sectionIcon(state.section),
            accent = sectionColor(state.section),
            onBackClick = onBackClick,
        )

        state.actionMessage?.let { message ->
            ReportMessage(message = message, color = PremiumBrandAccent)
        }
        state.errorMessage?.let { message ->
            ReportMessage(message = message, color = Color(0xFFFCA5A5))
        }

        if (state.visibleRows.isEmpty()) {
            PremiumEmptyState(
                title = state.section.emptyText,
                subtitle = "A consulta do tenant ativo não retornou itens para esta rota.",
                icon = sectionIcon(state.section),
                accent = sectionColor(state.section),
            )
        } else {
            state.visibleRows.forEach { report ->
                AdminReportCard(
                    report = report,
                    response = state.responsesById[report.id].orEmpty(),
                    supportsResponseActions = state.supportsResponseActions,
                    isBusy = state.busyId == report.id,
                    onResponseChange = { onResponseChange(report.id, it) },
                    onResolveClick = { onResolveClick(report) },
                    onDeleteClick = { onDeleteClick(report) },
                    onUserClick = { if (report.reporterId.isNotBlank()) onUserClick(report.reporterId) },
                )
            }
        }

        if (state.rows.size > state.pageSize) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Página ${state.page} de ${state.totalPages}",
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                PremiumSecondaryButton(
                    text = "Anterior",
                    onClick = onPreviousPageClick,
                    enabled = state.canGoPrevious,
                    modifier = Modifier.weight(1f),
                )
                PremiumSecondaryButton(
                    text = "Próxima",
                    onClick = onNextPageClick,
                    enabled = state.canGoNext,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (state.section == AdminReportsSection.Community || state.section == AdminReportsSection.Gym) {
            ReportMessage(
                message = "Leitura paginada e cacheada para reduzir custo.",
                color = PremiumZinc500,
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
private fun ReportHubCard(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(11.dp))
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = description,
                color = PremiumZinc400,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AdminReportCard(
    report: AdminReportItem,
    response: String,
    supportsResponseActions: Boolean,
    isBusy: Boolean,
    onResponseChange: (String) -> Unit,
    onResolveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUserClick: () -> Unit,
) {
    PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = report.author,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (report.reason.isNotBlank()) {
                    Text(
                        text = report.reason,
                        color = PremiumZinc500,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            PremiumChip(
                label = report.status.label,
                accent = if (report.status == AdminReportStatus.Resolved) PremiumBrandAccent else PremiumAmber,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.30f),
            border = BorderStroke(1.dp, PremiumZinc800),
        ) {
            Text(
                text = report.description,
                modifier = Modifier.padding(12.dp),
                color = PremiumZinc400,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = report.dateLabel,
                color = PremiumZinc500,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )
            if (report.targetType.isNotBlank()) {
                Text(
                    text = report.targetType.uppercase(),
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        if (report.reporterId.isNotBlank()) {
            ReportSmallAction(
                text = "Usuário",
                icon = Icons.Outlined.Message,
                accent = PremiumBrandAccent,
                onClick = onUserClick,
            )
        }

        if (supportsResponseActions) {
            OutlinedTextField(
                value = response,
                onValueChange = onResponseChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                label = { Text("Devolutiva do admin") },
                placeholder = { Text("Escreva a resposta para o usuário...") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Black.copy(alpha = 0.34f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.34f),
                    focusedBorderColor = PremiumBrandAccent,
                    unfocusedBorderColor = PremiumZinc700,
                    focusedLabelColor = PremiumBrandAccent,
                    unfocusedLabelColor = PremiumZinc500,
                    cursorColor = PremiumBrandAccent,
                ),
            )

            if (report.adminResponse.isNotBlank()) {
                ReportMessage(
                    message = "Última resposta salva\n${report.adminResponse}",
                    color = PremiumBrandAccent,
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportSmallAction(
                    text = "Excluir",
                    icon = Icons.Outlined.Delete,
                    accent = PremiumZinc400,
                    enabled = !isBusy,
                    onClick = onDeleteClick,
                )
                ReportSmallAction(
                    text = "Responder e concluir",
                    icon = Icons.Outlined.Send,
                    accent = PremiumBrandAccent,
                    enabled = !isBusy && response.trim().isNotBlank(),
                    onClick = onResolveClick,
                )
            }
        }
    }
}

@Composable
private fun ReportSmallAction(
    text: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
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
            Icon(icon, contentDescription = null, tint = if (enabled) accent else PremiumZinc700, modifier = Modifier.size(14.dp))
            Text(
                text = text,
                color = if (enabled) accent else PremiumZinc700,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun ReportMessage(
    message: String,
    color: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = color,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

private fun sectionIcon(section: AdminReportsSection): ImageVector {
    return when (section) {
        AdminReportsSection.Banned -> Icons.Outlined.Warning
        AdminReportsSection.Community -> Icons.Outlined.Message
        AdminReportsSection.Gym -> Icons.Outlined.FitnessCenter
        AdminReportsSection.Support -> Icons.Outlined.SupportAgent
    }
}

private fun sectionColor(section: AdminReportsSection): Color {
    return when (section) {
        AdminReportsSection.Banned -> Color(0xFFF87171)
        AdminReportsSection.Community -> Color(0xFF60A5FA)
        AdminReportsSection.Gym -> PremiumAmber
        AdminReportsSection.Support -> PremiumBrandAccent
    }
}
