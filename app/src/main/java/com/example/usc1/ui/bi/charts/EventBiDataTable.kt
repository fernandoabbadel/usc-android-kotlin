package com.example.usc1.ui.bi.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.domain.model.EventBiTableRow
import com.example.usc1.domain.model.formatEventBiCurrency
import com.example.usc1.domain.model.formatEventBiDecimal
import com.example.usc1.domain.model.formatEventBiNumber
import com.example.usc1.domain.model.formatEventBiPercent

/**
 * `DataTable` (3267) do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 3267-3377.
 * Mantém a paginação por `pageSize`, o rodapé "Página X de Y · N registros" e o estado vazio
 * "Sem dados para o filtro atual." (3343).
 *
 * Adaptação de tela: a tabela do web usa `overflow-x-auto`; no celular o app faz o mesmo com
 * `horizontalScroll`, com largura de coluna fixa para o cabeçalho e o corpo não se descolarem.
 */

/** `format` da coluna (3278). */
enum class EventBiColumnFormat { Text, Currency, Percent, Decimal, Number }

/**
 * Uma coluna do `DataTable`. `hrefKey` aponta para a chave da linha que carrega o link; sem
 * `hrefKey`, só a primeira coluna usa `row.href` (3325).
 */
data class EventBiColumn(
    val key: String,
    val label: String,
    val format: EventBiColumnFormat = EventBiColumnFormat.Text,
    val hrefKey: String = "",
    val width: Int = 0,
)

@Composable
fun EventBiDataTable(
    title: String,
    rows: List<EventBiTableRow>,
    columns: List<EventBiColumn>,
    modifier: Modifier = Modifier,
    pageSize: Int = 0,
) {
    var page by remember(rows.size, pageSize) { mutableIntStateOf(1) }
    val totalPages = eventBiTotalPages(rows.size, pageSize)
    val safePage = page.coerceIn(1, totalPages)
    val visibleRows = eventBiTablePage(rows, pageSize, safePage)
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column {
            Text(
                text = title.uppercase(),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                color = PremiumZinc300,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
            )
            HorizontalDivider(color = PremiumZinc800)

            if (visibleRows.isEmpty()) {
                Text(
                    text = "Sem dados para o filtro atual.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp),
                    color = PremiumZinc500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            } else {
                Column(modifier = Modifier.horizontalScroll(scrollState)) {
                    Row(modifier = Modifier.background(Color.Black.copy(alpha = 0.30f))) {
                        columns.forEach { column ->
                            Text(
                                text = column.label.uppercase(),
                                modifier = Modifier
                                    .width(column.resolvedWidth().dp)
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                color = PremiumZinc500,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.7.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    visibleRows.forEach { row ->
                        HorizontalDivider(color = PremiumZinc800)
                        Row {
                            columns.forEachIndexed { index, column ->
                                val href = if (column.hrefKey.isNotBlank()) {
                                    row.text(column.hrefKey)
                                } else if (index == 0) {
                                    row.href
                                } else {
                                    ""
                                }
                                Text(
                                    text = column.render(row),
                                    modifier = Modifier
                                        .width(column.resolvedWidth().dp)
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    // O link só ganha destaque quando existe de verdade; com
                                    // `EventBiLinkBuilder.Inert` (M10 pendente) o href vem vazio.
                                    color = if (href.isNotBlank()) PremiumBrand else PremiumZinc300,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // `pageSize && rows.length > pageSize` (3350).
            if (pageSize > 0 && rows.size > pageSize) {
                HorizontalDivider(color = PremiumZinc800)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Página $safePage de $totalPages · " +
                            "${formatEventBiNumber(rows.size.toDouble())} registros",
                        modifier = Modifier.weight(1f),
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { page = (safePage - 1).coerceAtLeast(1) }, enabled = safePage > 1) {
                        Text(text = "ANTERIOR", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    TextButton(
                        onClick = { page = (safePage + 1).coerceAtMost(totalPages) },
                        enabled = safePage < totalPages,
                    ) {
                        Text(text = "PRÓXIMA", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/** Larguras derivadas do rótulo, já que o web usa `min-w-full` com colunas fluidas. */
private fun EventBiColumn.resolvedWidth(): Int = when {
    width > 0 -> width
    format != EventBiColumnFormat.Text -> 104
    else -> 132
}

/** Conversão `column.format` do web (3315-3324). */
private fun EventBiColumn.render(row: EventBiTableRow): String = when (format) {
    EventBiColumnFormat.Currency -> formatEventBiCurrency(row.number(key))
    EventBiColumnFormat.Percent -> formatEventBiPercent(row.number(key))
    EventBiColumnFormat.Decimal -> formatEventBiDecimal(row.number(key))
    EventBiColumnFormat.Number -> formatEventBiNumber(row.number(key))
    EventBiColumnFormat.Text -> row.text(key)
}

/** Espaço reservado que mantém o alinhamento quando a tabela está vazia. */
@Composable
internal fun EventBiTableSpacer() {
    Box(modifier = Modifier.fillMaxWidth())
}
