package com.example.usc1.ui.bi.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.domain.model.EventBiMetricRow
import com.example.usc1.domain.model.EventBiValueFormat
import com.example.usc1.domain.model.eventBiClamp
import com.example.usc1.domain.model.formatEventBiNumber
import com.example.usc1.domain.model.formatEventBiShortValue
import com.example.usc1.domain.model.safeDivide
import com.example.usc1.domain.model.scoreColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pizza, rosca, meia-rosca e velocímetro do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx` —
 * `PieMetric` (2500), `SimplePieMetric` (2562), `SemiDonutMetric` (2595) e `ScoreGauge` (2762).
 */

private const val PaddingAngle = 3f

/**
 * `PieMetric` (2500): rosca com `innerRadius={62} outerRadius={98}`, rótulo externo com o valor
 * curto e o rodapé de `FilterLinkChips` com as 8 primeiras fatias (2551).
 */
@Composable
fun EventBiPieMetric(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
    dataKey: EventBiMetricKey = EventBiMetricKey.Quantity,
    valueName: String = "Quantidade",
    valueFormat: EventBiValueFormat = EventBiValueFormat.Number,
) {
    val visible = eventBiPieSlices(data, dataKey)
    if (visible.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    // Sem corte: o `<Pie>` do web desenha todas as fatias filtradas.
    val slices = visible

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            drawDonut(
                measurer = measurer,
                values = slices.map { it.valueOf(dataKey) },
                labels = slices.map { formatEventBiShortValue(it.valueOf(dataKey), valueFormat) },
                innerRatio = 0.63f,
            )
        }
        EventBiChartLegend(slices.mapIndexed { index, row -> row.name to chartColorAt(index) })
        EventBiFilterLinkChips(
            links = visible.take(8).mapIndexed { index, row ->
                EventBiChipLink(label = row.name, href = row.href, color = chartColorAt(index))
            },
        )
        // `valueName` alimenta o tooltip do Recharts; no app ele vira o rótulo da unidade.
        androidx.compose.material3.Text(
            text = valueName.uppercase(),
            color = com.example.usc1.core.ui.PremiumZinc500,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
        )
    }
}

/** `SimplePieMetric` (2562): pizza cheia, sem furo, filtrando `quantity > 0 || value > 0`. */
@Composable
fun EventBiSimplePieMetric(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
) {
    val visible = eventBiWideSlices(data)
    if (visible.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val slices = visible

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            drawDonut(
                measurer = measurer,
                values = slices.map { it.quantity },
                labels = slices.map { formatEventBiShortValue(it.quantity, EventBiValueFormat.Number) },
                innerRatio = 0f,
                paddingAngle = 1f,
            )
        }
        EventBiChartLegend(slices.mapIndexed { index, row -> row.name to chartColorAt(index) })
    }
}

/**
 * `SemiDonutMetric` (2595): meia-rosca de `startAngle={180}` a `endAngle={0}`, ancorada em
 * `cy="76%"`.
 */
@Composable
fun EventBiSemiDonutMetric(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
    valueFormat: EventBiValueFormat = EventBiValueFormat.Number,
) {
    val visible = eventBiWideSlices(data)
    if (visible.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val slices = visible
    val total = slices.sumOf { it.quantity }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val centerX = size.width / 2f
            val centerY = size.height * 0.76f
            val outer = minOf(size.width * 0.42f, size.height * 0.62f)
            val inner = outer * 0.64f
            val stroke = outer - inner
            val radius = (outer + inner) / 2f

            var start = 180f
            slices.forEachIndexed { index, row ->
                val sweep = (safeDivide(row.quantity, total) * 180.0).toFloat()
                if (sweep > 0f) {
                    drawArc(
                        color = chartColorAt(index),
                        startAngle = start,
                        sweepAngle = maxOf(sweep - PaddingAngle, 0.6f),
                        useCenter = false,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = stroke),
                    )
                }
                start += sweep
            }
            drawChartText(
                measurer = measurer,
                text = formatEventBiShortValue(total, valueFormat),
                x = centerX,
                y = centerY - 14f,
                color = Color.White,
                sizeSp = 18f,
                weight = FontWeight.Black,
                anchor = TextAnchor.Middle,
            )
        }
        EventBiChartLegend(slices.mapIndexed { index, row -> row.name to chartColorAt(index) })
    }
}

/**
 * `ScoreGauge` (2762): arco 0-100 com a cor de `scoreColor`, o número no centro e a faixa de
 * decisão. `score === null` cai no `EmptyChart` (2769).
 */
@Composable
fun EventBiScoreGauge(
    score: Int?,
    label: String,
    modifier: Modifier = Modifier,
) {
    if (score == null) {
        EventBiEmptyChart(modifier)
        return
    }
    val safeScore = eventBiClamp(score.toDouble())

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val centerX = size.width / 2f
            val centerY = size.height * 0.74f
            val radius = minOf(size.width * 0.38f, size.height * 0.56f)
            val stroke = radius * 0.30f

            drawArc(
                color = com.example.usc1.core.ui.PremiumZinc800,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawArc(
                color = parseEventBiColor(scoreColor(safeScore)),
                startAngle = 180f,
                sweepAngle = (safeScore / 100.0 * 180.0).toFloat(),
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawChartText(
                measurer = measurer,
                text = formatEventBiNumber(safeScore),
                x = centerX,
                y = centerY - 24f,
                color = Color.White,
                sizeSp = 30f,
                weight = FontWeight.Black,
                anchor = TextAnchor.Middle,
            )
            drawChartText(
                measurer = measurer,
                text = label.uppercase(),
                x = centerX,
                y = centerY - 2f,
                color = Color.White.copy(alpha = 0.58f),
                sizeSp = 10f,
                weight = FontWeight.Black,
                anchor = TextAnchor.Middle,
            )
            drawChartText(measurer, "0", centerX - radius, centerY + 18f, ChartAxisColor, anchor = TextAnchor.Middle)
            drawChartText(measurer, "100", centerX + radius, centerY + 18f, ChartAxisColor, anchor = TextAnchor.Middle)
        }
        // As quatro faixas do rodapé (2798).
        EventBiChartLegend(
            listOf(
                "0-39 repensar" to ChartRose,
                "40-69 ajustar" to ChartAmber,
                "70-84 repetir" to ChartValueAxisColor,
                "85-100 escalar" to ChartQuantityAxisColor,
            ),
        )
    }
}

/**
 * Rosca genérica: `innerRatio = 0` desenha pizza cheia (`SimplePieMetric`), acima de zero
 * desenha rosca (`PieMetric`).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDonut(
    measurer: androidx.compose.ui.text.TextMeasurer,
    values: List<Double>,
    labels: List<String>,
    innerRatio: Float,
    paddingAngle: Float = PaddingAngle,
) {
    val total = values.sum()
    if (total <= 0.0) return
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val outer = minOf(size.width, size.height) * 0.34f
    val inner = outer * innerRatio

    var start = -90f
    values.forEachIndexed { index, value ->
        val sweep = (value / total * 360.0).toFloat()
        if (sweep > 0f) {
            val color = chartColorAt(index)
            val drawnSweep = maxOf(sweep - paddingAngle, 0.6f)
            if (inner > 0f) {
                val radius = (outer + inner) / 2f
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = drawnSweep,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = outer - inner),
                )
            } else {
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = drawnSweep,
                    useCenter = true,
                    topLeft = Offset(centerX - outer, centerY - outer),
                    size = Size(outer * 2, outer * 2),
                )
            }
            // `<LabelList position="outside" />` (2532).
            val midRadians = Math.toRadians((start + sweep / 2f).toDouble())
            drawChartText(
                measurer = measurer,
                text = labels.getOrElse(index) { "" },
                x = centerX + cos(midRadians).toFloat() * (outer + 16f),
                y = centerY + sin(midRadians).toFloat() * (outer + 16f),
                color = Color.White,
                sizeSp = 8f,
                weight = FontWeight.Black,
                anchor = TextAnchor.Middle,
            )
        }
        start += sweep
    }
}
