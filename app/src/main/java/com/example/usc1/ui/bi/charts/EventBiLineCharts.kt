package com.example.usc1.ui.bi.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.usc1.domain.model.EventBiMetricRow
import com.example.usc1.domain.model.EventBiTableRow
import com.example.usc1.domain.model.EventBiValueFormat
import com.example.usc1.domain.model.formatEventBiShortValue
import com.example.usc1.domain.model.safeDivide

/**
 * Componentes de linha e combinados do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx` —
 * `LineMetric` (2480), `ParetoMetric` (2638), `ScanModeByHourChart` (2728) e
 * `ComboBarsLines` (2842).
 */

/** `<Line type="monotone" ... strokeWidth={3} dot={{ r: 4 }} />` do Recharts. */
internal fun DrawScope.drawSeriesLine(points: List<Offset>, color: Color) {
    if (points.isEmpty()) return
    points.zipWithNext { start, end ->
        drawLine(color = color, start = start, end = end, strokeWidth = 3f)
    }
    points.forEach { point -> drawCircle(color = color, radius = 4f, center = point) }
}

/** `LineMetric` (2480): linha única de `quantity`, sem eixo secundário. */
@Composable
fun EventBiLineMetric(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val rows = data
    val max = rows.maxOfQuantity().coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val frame = drawCartesianFrame(
                measurer = measurer,
                names = rows.map { it.name },
                leftMax = max,
                leftFormat = EventBiValueFormat.Number,
            )
            drawSeriesLine(
                points = rows.mapIndexed { index, row ->
                    Offset(frame.xOfBand(index, rows.size), frame.yOfLeft(row.quantity))
                },
                color = ChartQuantityAxisColor,
            )
        }
        EventBiChartLegend(listOf("Quantidade" to ChartQuantityAxisColor))
    }
}

/**
 * `ParetoMetric` (2638): top 10 por quantidade com a linha de acumulado em percentual.
 *
 * O total do acumulado usa **todas** as linhas recebidas (2639), não só as 10 exibidas — por isso
 * o acumulado pode terminar abaixo de 100%.
 */
@Composable
fun EventBiParetoMetric(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
) {
    val rows = eventBiParetoRows(data)
    if (rows.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val max = axisTop(rows.maxOf { it.first.quantity })

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val frame = drawCartesianFrame(
                measurer = measurer,
                names = rows.map { it.first.name },
                leftMax = max,
                rightMax = 100.0,
                leftFormat = EventBiValueFormat.Number,
                rightFormat = EventBiValueFormat.Percent,
                leftAxisColor = ChartQuantityAxisColor,
                rightAxisColor = ChartValueAxisColor,
            )
            val barWidth = (frame.bandWidth(rows.size) * 0.5f).coerceAtMost(26f)
            rows.forEachIndexed { index, (row, _) ->
                val centerX = frame.xOfBand(index, rows.size)
                val top = frame.yOfLeft(row.quantity)
                val (origin, boxSize) = barRect(centerX, barWidth, top, frame.plot.bottom)
                drawRoundRect(
                    color = ChartQuantityAxisColor,
                    topLeft = origin,
                    size = boxSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )
                drawChartText(
                    measurer = measurer,
                    text = formatEventBiShortValue(row.quantity, EventBiValueFormat.Number),
                    x = centerX,
                    y = top - 8f,
                    color = Color.White,
                    sizeSp = 8f,
                    weight = FontWeight.Black,
                    anchor = TextAnchor.Middle,
                )
            }
            drawSeriesLine(
                points = rows.mapIndexed { index, (_, accumulated) ->
                    Offset(frame.xOfBand(index, rows.size), frame.yOfRight(accumulated))
                },
                color = ChartValueAxisColor,
            )
        }
        EventBiChartLegend(
            listOf("Quantidade" to ChartQuantityAxisColor, "Acumulado" to ChartValueAxisColor),
        )
    }
}

/**
 * `ScanModeByHourChart` (2728): barra empilhada QR + manual com a linha de manualidade no eixo
 * direito, fixo em 0-100.
 */
@Composable
fun EventBiScanModeByHourChart(
    data: List<EventBiTableRow>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val rows = data
    val max = rows.fold(0.0) { current, row -> maxOf(current, row.number("qr") + row.number("manual")) }
        .coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val frame = drawCartesianFrame(
                measurer = measurer,
                names = rows.map { it.text("name") },
                leftMax = max,
                rightMax = 100.0,
                leftFormat = EventBiValueFormat.Number,
                rightFormat = EventBiValueFormat.Percent,
                leftAxisColor = ChartQuantityAxisColor,
                rightAxisColor = ChartAmber,
            )
            val barWidth = (frame.bandWidth(rows.size) * 0.55f).coerceAtMost(24f)
            rows.forEachIndexed { index, row ->
                val centerX = frame.xOfBand(index, rows.size)
                val qrTop = frame.yOfLeft(row.number("qr"))
                val stackTop = frame.yOfLeft(row.number("qr") + row.number("manual"))
                val (qrOrigin, qrSize) = barRect(centerX, barWidth, qrTop, frame.plot.bottom)
                drawRect(color = ChartQuantityAxisColor, topLeft = qrOrigin, size = qrSize)
                val (manualOrigin, manualSize) = barRect(centerX, barWidth, stackTop, qrTop)
                drawRect(color = ChartAmber, topLeft = manualOrigin, size = manualSize)
            }
            drawSeriesLine(
                points = rows.mapIndexed { index, row ->
                    Offset(frame.xOfBand(index, rows.size), frame.yOfRight(row.number("manualRate")))
                },
                color = ChartRose,
            )
        }
        EventBiChartLegend(
            listOf(
                "QR" to ChartQuantityAxisColor,
                "Manual" to ChartAmber,
                "Manual %" to ChartRose,
            ),
        )
    }
}

/** `sortBy` do `ComboBarsLines` (2857). */
enum class EventBiComboSort { Quantity, Value, None }

/**
 * `ComboBarsLines` (2842): barra de `quantity` no eixo esquerdo, linha de `value` no direito e
 * uma segunda linha de `secondary` **só quando alguma linha tem `secondary > 0`** (2865).
 */
@Composable
fun EventBiComboBarsLines(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
    barName: String = "Quantidade",
    lineOneName: String = "Receita",
    lineTwoName: String = "Secundário",
    valueFormat: EventBiValueFormat = EventBiValueFormat.Currency,
    secondaryFormat: EventBiValueFormat = EventBiValueFormat.Currency,
    sortBy: EventBiComboSort = EventBiComboSort.Quantity,
) {
    if (data.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val rows = eventBiComboRows(data, sortBy)
    val hasSecondary = eventBiHasSecondary(rows)
    val quantityMax = axisTop(rows.maxOfQuantity())
    val valueMax = rows.fold(0.0) { current, row ->
        maxOf(current, row.value, if (hasSecondary) row.secondary else 0.0)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val frame = drawCartesianFrame(
                measurer = measurer,
                names = rows.map { it.name },
                leftMax = quantityMax,
                rightMax = valueMax,
                leftFormat = EventBiValueFormat.Number,
                rightFormat = valueFormat,
                leftAxisColor = ChartQuantityAxisColor,
                rightAxisColor = ChartValueAxisColor,
            )
            val barWidth = (frame.bandWidth(rows.size) * 0.5f).coerceAtMost(24f)
            rows.forEachIndexed { index, row ->
                val centerX = frame.xOfBand(index, rows.size)
                val top = frame.yOfLeft(row.quantity)
                val (origin, boxSize) = barRect(centerX, barWidth, top, frame.plot.bottom)
                drawRoundRect(
                    color = ChartQuantityAxisColor,
                    topLeft = origin,
                    size = boxSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )
                drawChartText(
                    measurer = measurer,
                    text = formatEventBiShortValue(row.quantity, EventBiValueFormat.Number),
                    x = centerX,
                    y = top - 8f,
                    color = Color.White,
                    sizeSp = 8f,
                    weight = FontWeight.Black,
                    anchor = TextAnchor.Middle,
                )
            }
            drawSeriesLine(
                points = rows.mapIndexed { index, row ->
                    Offset(frame.xOfBand(index, rows.size), frame.yOfRight(row.value))
                },
                color = ChartValueAxisColor,
            )
            if (hasSecondary) {
                drawSeriesLine(
                    points = rows.mapIndexed { index, row ->
                        Offset(frame.xOfBand(index, rows.size), frame.yOfRight(row.secondary))
                    },
                    color = ChartAmber,
                )
            }
        }
        // `Legend formatter` (2899): a barra é o eixo esquerdo, o resto é o direito.
        EventBiChartLegend(
            buildList {
                add("$barName · eixo esquerdo" to ChartQuantityAxisColor)
                add("$lineOneName · eixo direito" to ChartValueAxisColor)
                if (hasSecondary) add("$lineTwoName · eixo direito" to ChartAmber)
            },
        )
    }
}

/** Formato do eixo secundário, mantido para paridade de assinatura com o web. */
internal fun secondaryAxisFormat(format: EventBiValueFormat): EventBiValueFormat = format
