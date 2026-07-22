package com.example.usc1.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminDatabaseTableFields

@Composable
fun AdminDatabaseScannerScreen(
    state: AdminDatabaseScannerUiState,
    onScanClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 18.dp) {
        PremiumHeader(
            title = "Scanner de Campos do Banco",
            subtitle = if (state.tenantId.isBlank()) {
                "users, produtos, eventos, orders e parceiros"
            } else {
                "Tenant ativo: ${state.tenantId}"
            },
            icon = Icons.Outlined.DataObject,
            accent = PremiumBrand,
            onBackClick = onBackClick,
        )
        PremiumPrimaryButton(
            text = if (state.isLoading) "Escaneando tabelas..." else "Iniciar escaneamento",
            onClick = onScanClick,
            loading = state.isLoading,
            icon = Icons.Outlined.PlayArrow,
        )
        state.errorMessage?.let { message ->
            Text(text = message, color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (state.report.isEmpty() && !state.isLoading) {
            PremiumEmptyState(
                title = "Nenhum campo escaneado.",
                subtitle = "Toque em Iniciar escaneamento para consultar o Supabase do tenant ativo.",
                icon = Icons.Outlined.DataObject,
                accent = PremiumBrand,
            )
        }
        state.report.forEach { table ->
            DatabaseTableCard(table = table)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DatabaseTableCard(table: AdminDatabaseTableFields) {
    PremiumCard(accent = PremiumBrand, containerColor = PremiumZinc900) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = table.tableName.replaceFirstChar { it.titlecase() },
                color = Color(0xFF60A5FA),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            if (table.fields.isEmpty()) {
                Text(
                    text = "Nenhum campo identificado.",
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    table.fields.forEach { field ->
                        PremiumChip(
                            label = field,
                            accent = Color(0xFF22C55E),
                            filled = false,
                        )
                    }
                }
            }
            Text(
                text = "${table.fields.size} campos",
                color = PremiumZinc400,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
