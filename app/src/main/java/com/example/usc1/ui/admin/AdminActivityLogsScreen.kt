package com.example.usc1.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminActivityLogRecord
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun AdminActivityLogsScreen(
    state: AdminActivityLogsUiState,
    onSearchChange: (String) -> Unit,
    onLoadMoreClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando logs...", modifier = modifier)
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 16.dp) {
        PremiumHeader(
            title = "Centro de Auditoria",
            subtitle = "Logs administrativos do tenant ativo",
            icon = Icons.Outlined.Security,
            accent = PremiumBrand,
            onBackClick = onBackClick,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumTextField(
                value = state.searchTerm,
                onValueChange = onSearchChange,
                label = "Buscar por usuário, ação ou detalhe...",
                leadingIcon = Icons.Outlined.Search,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Filtro",
                onClick = {},
                icon = Icons.Outlined.FilterList,
                modifier = Modifier.weight(0.42f),
            )
        }
        state.errorMessage?.let { message ->
            Text(text = message, color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (state.filteredLogs.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum log encontrado para o filtro aplicado.",
                subtitle = "A consulta de activity_logs não retornou registros para o termo atual.",
                icon = Icons.Outlined.Warning,
                accent = PremiumBrand,
            )
        } else {
            state.filteredLogs.forEach { log ->
                ActivityLogCard(log = log)
            }
        }
        if (state.hasMore) {
            PremiumSecondaryButton(
                text = if (state.isLoadingMore) "Carregando..." else "Carregar mais logs",
                onClick = onLoadMoreClick,
                enabled = !state.isLoadingMore,
                icon = Icons.Outlined.ExpandMore,
            )
        }
    }
}

@Composable
private fun ActivityLogCard(log: AdminActivityLogRecord) {
    PremiumCard(accent = actionAccent(log.action), containerColor = PremiumZinc900) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = log.userName.ifBlank { "Sistema" }, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    PremiumChip(label = log.resource.ifBlank { "Sistema" }, accent = actionAccent(log.action))
                }
                Text(
                    text = log.details.ifBlank { "Sem detalhes." },
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                )
                Text(
                    text = log.action.ifBlank { "UNKNOWN" },
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = formatLogTime(log.timestamp), color = PremiumZinc400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = formatLogDate(log.timestamp), color = PremiumZinc500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun actionAccent(action: String): Color {
    return when (action.uppercase()) {
        "CREATE" -> Color(0xFF10B981)
        "UPDATE" -> Color(0xFF3B82F6)
        "DELETE" -> Color(0xFFEF4444)
        "LOGIN" -> Color(0xFFA1A1AA)
        else -> Color(0xFFFACC15)
    }
}

private fun formatLogTime(value: String): String {
    val dateTime = parseLogDateTime(value) ?: return "--:--"
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR")))
}

private fun formatLogDate(value: String): String {
    val dateTime = parseLogDateTime(value) ?: return "--/--/----"
    return dateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale("pt", "BR")))
}

private fun parseLogDateTime(value: String): OffsetDateTime? {
    val clean = value.trim()
    if (clean.isBlank()) return null
    return runCatching { OffsetDateTime.parse(clean) }
        .recoverCatching {
            Instant.parse(clean).atZone(ZoneId.systemDefault()).toOffsetDateTime()
        }
        .getOrNull()
}
