package com.example.usc1.ui.bi.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.usc1.domain.model.EventBiMetricRow
import com.example.usc1.domain.model.EventBiTableRow
import com.example.usc1.domain.model.EventBiValueFormat
import com.example.usc1.domain.model.comparePtBr
import com.example.usc1.domain.model.formatEventBiShortValue
import com.example.usc1.domain.model.safeDivide

/**
 * Componentes de barra do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx` —
 * `Bars` (2316), `BarsDual` (2359), `ColumnBars` (2438), `StackedPercentChart` (2915) e
 * `WaterfallMetric` (3192).
 */

/** Chave lida pelo `Bars`/`ColumnBars` (`dataKey`). */
enum class EventBiMetricKey { Quantity, Value }

internal fun EventBiMetricRow.valueOf(key: EventBiMetricKey): Double =
    when (key) {
        EventBiMetricKey.Quantity -> quantity
        EventBiMetricKey.Value -> value
    }

/**
 * `Bars` (2316): barra horizontal, `layout="vertical"`, rótulo do valor à direita da barra.
 * `metricName` é "Receita" quando a chave é `value` e "Quantidade" caso contrário (2318).
 */
@Composable
fun EventBiBars(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
    dataKey: EventBiMetricKey = EventBiMetricKey.Quantity,
    currency: Boolean = false,
) {
    if (data.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val format = if (currency) EventBiValueFormat.Currency else EventBiValueFormat.Number
    val metricName = if (dataKey == EventBiMetricKey.Value) "Receita" else "Quantidade"
    // Sem corte: o `Bars` do web desenha toda a série, e o motor já entrega no máximo 12 linhas
    // pelo `metricRows` (2086). A altura cresce com a série em vez de truncar dado.
    val rows = data
    val max = rows.fold(0.0) { current, row -> maxOf(current, row.valueOf(dataKey)) }
    val height: Dp = (34 * rows.size + 34).dp.coerceAtLeast(120.dp)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas(height = height) { measurer ->
            val leftGutter = 84f
            val plot = Rect(leftGutter, 8f, size.width - 46f, size.height - 20f)
            val band = if (rows.isEmpty()) plot.height else plot.height / rows.size
            val barHeight = (band * 0.58f).coerceAtMost(22f)
            val top = axisTop(max)

            // `CartesianGrid ... horizontal={false}`: só as linhas verticais.
            for (index in 0..4) {
                val x = plot.left + plot.width * index / 4f
                drawLine(ChartGridColor, Offset(x, plot.top), Offset(x, plot.bottom), 1f)
            }

            rows.forEachIndexed { index, row ->
                val centerY = plot.top + band * index + band / 2f
                val width = (safeDivide(row.valueOf(dataKey), top)).toFloat().coerceIn(0f, 1f) * plot.width
                drawChartText(
                    measurer = measurer,
                    text = axisLabel(row.name),
                    x = plot.left - 6f,
                    y = centerY,
                    color = ChartAxisColor,
                    anchor = TextAnchor.End,
                )
                drawRoundRect(
                    color = ChartQuantityAxisColor,
                    topLeft = Offset(plot.left, centerY - barHeight / 2f),
                    size = androidx.compose.ui.geometry.Size(maxOf(width, 1f), barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )
                drawChartText(
                    measurer = measurer,
                    text = formatEventBiShortValue(row.valueOf(dataKey), format),
                    x = plot.left + width + 5f,
                    y = centerY,
                    color = Color.White,
                    weight = androidx.compose.ui.text.font.FontWeight.Black,
                )
            }
        }
        EventBiChartLegend(listOf(metricName to ChartQuantityAxisColor))
    }
}

/**
 * `BarsDual` (2359): barra de `value` no eixo esquerdo (azul) e linha de `quantity` no eixo
 * direito (verde). Só o eixo de `value` recebe a folga de 18% (2384).
 */
@Composable
fun EventBiBarsDual(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
    valueName: String = "Receita",
    quantityName: String = "Quantidade",
    valueFormat: EventBiValueFormat = EventBiValueFormat.Currency,
    quantityFormat: EventBiValueFormat = EventBiValueFormat.Number,
) {
    if (data.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val rows = data
    val valueMax = axisTop(rows.maxOfValue())
    val quantityMax = rows.maxOfQuantity()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val frame = drawCartesianFrame(
                measurer = measurer,
                names = rows.map { it.name },
                leftMax = valueMax,
                rightMax = quantityMax,
                leftFormat = valueFormat,
                rightFormat = quantityFormat,
                leftAxisColor = ChartValueAxisColor,
                rightAxisColor = ChartQuantityAxisColor,
            )
            val bandWidth = frame.bandWidth(rows.size)
            val barWidth = (bandWidth * 0.5f).coerceAtMost(26f)

            rows.forEachIndexed { index, row ->
                val centerX = frame.xOfBand(index, rows.size)
                val top = frame.yOfLeft(row.value)
                val (origin, boxSize) = barRect(centerX, barWidth, top, frame.plot.bottom)
                drawRoundRect(
                    color = ChartValueAxisColor,
                    topLeft = origin,
                    size = boxSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )
                drawChartText(
                    measurer = measurer,
                    text = formatEventBiShortValue(row.value, valueFormat),
                    x = centerX,
                    y = top - 8f,
                    color = Color.White,
                    sizeSp = 8f,
                    weight = androidx.compose.ui.text.font.FontWeight.Black,
                    anchor = TextAnchor.Middle,
                )
            }

            // `<Line type="monotone" dataKey="quantity" ... dot={{ r: 4 }} />`
            drawSeriesLine(
                points = rows.mapIndexed { index, row ->
                    Offset(frame.xOfBand(index, rows.size), frame.yOfRight(row.quantity))
                },
                color = ChartQuantityAxisColor,
            )
        }
        EventBiChartLegend(
            listOf(valueName to ChartValueAxisColor, quantityName to ChartQuantityAxisColor),
        )
    }
}

/**
 * `ColumnBars` (2438): descarta linha zerada, ordena decrescente pela própria chave e desenha
 * uma barra verde por categoria.
 */
@Composable
fun EventBiColumnBars(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
    dataKey: EventBiMetricKey = EventBiMetricKey.Value,
    valueName: String = "Valor",
    valueFormat: EventBiValueFormat = EventBiValueFormat.Currency,
) {
    val rows = eventBiColumnRows(data, dataKey)
    if (rows.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val max = axisTop(rows.fold(0.0) { current, row -> maxOf(current, row.valueOf(dataKey)) })

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val frame = drawCartesianFrame(
                measurer = measurer,
                names = rows.map { it.name },
                leftMax = max,
                leftFormat = valueFormat,
            )
            val barWidth = (frame.bandWidth(rows.size) * 0.55f).coerceAtMost(32f)
            rows.forEachIndexed { index, row ->
                val centerX = frame.xOfBand(index, rows.size)
                val top = frame.yOfLeft(row.valueOf(dataKey))
                val (origin, boxSize) = barRect(centerX, barWidth, top, frame.plot.bottom)
                drawRoundRect(
                    color = ChartQuantityAxisColor,
                    topLeft = origin,
                    size = boxSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )
                drawChartText(
                    measurer = measurer,
                    text = formatEventBiShortValue(row.valueOf(dataKey), valueFormat),
                    x = centerX,
                    y = top - 8f,
                    color = Color.White,
                    sizeSp = 8f,
                    weight = androidx.compose.ui.text.font.FontWeight.Black,
                    anchor = TextAnchor.Middle,
                )
            }
        }
        EventBiChartLegend(listOf(valueName to ChartQuantityAxisColor))
    }
}

/**
 * `StackedPercentChart` (2915): barra horizontal 100% com as quatro chaves fixas do web.
 * Linha sem nenhuma das quatro chaves soma zero e `safeDivide` devolve 0 — a barra fica vazia,
 * como no web.
 */
@Composable
fun EventBiStackedPercentChart(
    data: List<EventBiTableRow>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val colors = listOf(ChartQuantityAxisColor, ChartAmber, ChartValueAxisColor, ChartRose)
    val labels = listOf("Retirado", "Pendente", "Parcial", "Cancelado")
    val rows = data
    val height: Dp = (34 * rows.size + 30).dp.coerceAtLeast(120.dp)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas(height = height) { measurer ->
            val plot = Rect(84f, 8f, size.width - 12f, size.height - 20f)
            val band = plot.height / rows.size
            val barHeight = (band * 0.58f).coerceAtMost(22f)

            for (index in 0..4) {
                val x = plot.left + plot.width * index / 4f
                drawLine(ChartGridColor, Offset(x, plot.top), Offset(x, plot.bottom), 1f)
            }

            rows.forEachIndexed { index, row ->
                val centerY = plot.top + band * index + band / 2f
                drawChartText(
                    measurer = measurer,
                    text = axisLabel(row.text("name")),
                    x = plot.left - 6f,
                    y = centerY,
                    color = ChartAxisColor,
                    anchor = TextAnchor.End,
                )
                var cursor = plot.left
                eventBiStackedShares(row).forEachIndexed { keyIndex, share ->
                    val width = (share / 100.0).toFloat() * plot.width
                    if (width > 0.5f) {
                        drawRect(
                            color = colors[keyIndex],
                            topLeft = Offset(cursor, centerY - barHeight / 2f),
                            size = androidx.compose.ui.geometry.Size(width, barHeight),
                        )
                    }
                    cursor += width
                }
            }
        }
        EventBiChartLegend(labels.zip(colors))
    }
}

/**
 * `WaterfallMetric` (3192): barra por etapa, verde quando `value >= 0` e rosa quando negativo.
 * Como o web filtra por `quantity !== 0 || value !== 0`, uma etapa zerada some da cascata.
 */
@Composable
fun EventBiWaterfallMetric(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
) {
    val rows = eventBiWaterfallRows(data)
    if (rows.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val maxValue = rows.maxOf { it.value }.coerceAtLeast(0.0)
    val minValue = rows.minOf { it.value }.coerceAtMost(0.0)
    val span = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val plot = Rect(46f, 18f, size.width - 12f, size.height - 46f)
            val zeroY = plot.bottom - ((0.0 - minValue) / span).toFloat() * plot.height

            for (index in 0..4) {
                val ratio = index / 4.0
                val y = plot.bottom - plot.height * ratio.toFloat()
                drawLine(ChartGridColor, Offset(plot.left, y), Offset(plot.right, y), 1f)
                drawChartText(
                    measurer = measurer,
                    text = formatEventBiShortValue(minValue + span * ratio, EventBiValueFormat.Currency),
                    x = plot.left - 5f,
                    y = y,
                    color = ChartAxisColor,
                    anchor = TextAnchor.End,
                )
            }

            val band = plot.width / rows.size
            val barWidth = (band * 0.55f).coerceAtMost(30f)
            rows.forEachIndexed { index, row ->
                val centerX = plot.left + band * index + band / 2f
                val valueY = plot.bottom - ((row.value - minValue) / span).toFloat() * plot.height
                val (origin, boxSize) = barRect(centerX, barWidth, valueY, zeroY)
                drawRoundRect(
                    color = parseEventBiColor(eventBiWaterfallColor(row.value)),
                    topLeft = origin,
                    size = boxSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )
                drawChartText(
                    measurer = measurer,
                    text = formatEventBiShortValue(row.value, EventBiValueFormat.Currency),
                    x = centerX,
                    y = minOf(valueY, zeroY) - 8f,
                    color = Color.White,
                    sizeSp = 8f,
                    weight = androidx.compose.ui.text.font.FontWeight.Black,
                    anchor = TextAnchor.Middle,
                )
                drawChartText(
                    measurer = measurer,
                    text = axisLabel(row.name, maxChars = 12),
                    x = centerX,
                    y = plot.bottom + 16f,
                    color = ChartAxisColor,
                    sizeSp = 8f,
                    anchor = TextAnchor.End,
                    rotationDegrees = -18f,
                )
            }
        }
        EventBiChartLegend(listOf("Valor" to ChartQuantityAxisColor))
    }
}

/** Ordenação `localeCompare(..., "pt-BR")` reaproveitada pelos gráficos que reordenam. */
internal fun compareNamesPtBr(left: String, right: String): Int = comparePtBr(left, right)
