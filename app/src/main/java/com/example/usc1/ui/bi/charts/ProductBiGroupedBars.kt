package com.example.usc1.ui.bi.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.usc1.domain.model.EventBiValueFormat
import com.example.usc1.domain.model.ProductBiMetricRow
import com.example.usc1.domain.model.formatEventBiShortValue

/**
 * `BarsDual` de `web-reference/src/components/ProductManagementAnalytics.tsx` (241-256).
 *
 * É o **único** gráfico do BI Loja que o kit do BI de Eventos não tinha. A diferença é real e
 * não cosmética: o `BarsDual` do `AdminEventBiDashboard` — já portado como [EventBiBarsDual] —
 * é um `ComposedChart` com barra de receita à esquerda e linha de quantidade à direita, em dois
 * eixos. Este aqui é um `BarChart` com **duas barras agrupadas** (`qtd` e `valor`) dividindo um
 * único `<YAxis>` (248).
 *
 * O eixo compartilhado é intencional no web e foi mantido: com receita em reais e quantidade em
 * unidades na mesma escala, a barra de quantidade fica curta. Trocar por dois eixos deixaria o
 * painel mais legível do que o web, o que é divergência — não melhoria.
 */
@Composable
fun ProductBiGroupedBars(
    data: List<ProductBiMetricRow>,
    modifier: Modifier = Modifier,
    quantityName: String = "Qtd",
    valueName: String = "Receita",
    valueFormat: EventBiValueFormat = EventBiValueFormat.Currency,
) {
    if (data.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    // Um eixo só, com o teto vindo da maior das duas séries (248 do web).
    val max = axisTop(data.fold(0.0) { current, row -> maxOf(current, row.qtd, row.valor) })

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val frame = drawCartesianFrame(
                measurer = measurer,
                names = data.map { it.name },
                leftMax = max,
                leftFormat = valueFormat,
            )
            val bandWidth = frame.bandWidth(data.size)
            // Duas barras por categoria, como o `<Bar>` duplo do Recharts agrupa.
            val barWidth = (bandWidth * 0.28f).coerceAtMost(18f)

            data.forEachIndexed { index, row ->
                val centerX = frame.xOfBand(index, data.size)
                listOf(
                    Triple(row.qtd, ChartQuantityAxisColor, -barWidth / 2f),
                    Triple(row.valor, ChartValueAxisColor, barWidth / 2f),
                ).forEach { (value, color, offset) ->
                    val top = frame.yOfLeft(value)
                    val (origin, boxSize) = barRect(centerX + offset, barWidth, top, frame.plot.bottom)
                    drawRoundRect(
                        color = color,
                        topLeft = origin,
                        size = boxSize,
                        cornerRadius = CornerRadius(5f, 5f),
                    )
                }
                // Só a receita ganha rótulo: dois números sobre barras de 18px se sobrepõem.
                if (row.valor > 0.0) {
                    drawChartText(
                        measurer = measurer,
                        text = formatEventBiShortValue(row.valor, valueFormat),
                        x = centerX,
                        y = frame.yOfLeft(row.valor) - 8f,
                        color = Color.White,
                        sizeSp = 8f,
                        weight = FontWeight.Black,
                        anchor = TextAnchor.Middle,
                    )
                }
            }
        }
        EventBiChartLegend(
            listOf(quantityName to ChartQuantityAxisColor, valueName to ChartValueAxisColor),
        )
    }
}
