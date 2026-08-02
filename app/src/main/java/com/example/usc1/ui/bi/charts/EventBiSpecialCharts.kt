package com.example.usc1.ui.bi.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.domain.model.EventBiBubbleEntry
import com.example.usc1.domain.model.EventBiHeatmapEntry
import com.example.usc1.domain.model.EventBiMetricRow
import com.example.usc1.domain.model.EventBiNetworkEdge
import com.example.usc1.domain.model.EventBiValueFormat
import com.example.usc1.domain.model.comparePtBr
import com.example.usc1.domain.model.eventBiClamp
import com.example.usc1.domain.model.eventBiHourSortValue
import com.example.usc1.domain.model.eventBiMaxValue
import com.example.usc1.domain.model.formatEventBiCurrency
import com.example.usc1.domain.model.formatEventBiNumber
import com.example.usc1.domain.model.formatEventBiPercent
import com.example.usc1.domain.model.formatEventBiShortValue
import com.example.usc1.domain.model.formatEventBiValue
import com.example.usc1.domain.model.safeDivide
import com.example.usc1.domain.model.scoreColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radar, bolha, heatmap, treemap, funil e rede do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx` —
 * `RadarMetric` (2707), `FunnelMetric` (2808), `HeatmapMetric` (2951), `TreemapMetric` (3017),
 * `BubbleTooltip` (3049), `BubbleMetric` (3081) e `NetworkMetric` (3218).
 */

/**
 * `RadarMetric` (2707): até 6 eixos, domínio fixo em 0-100, série lida de `value`.
 * O filtro `value > 0 || quantity > 0` é aplicado **antes** do corte em 6 (2708).
 */
@Composable
fun EventBiRadarMetric(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
) {
    val rows = eventBiRadarRows(data)
    if (rows.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(size.width, size.height) * 0.32f
            val step = (2 * PI / rows.size).toFloat()
            fun pointAt(index: Int, ratio: Float): Offset {
                val angle = step * index - (PI / 2).toFloat()
                return Offset(centerX + cos(angle) * radius * ratio, centerY + sin(angle) * radius * ratio)
            }

            // `PolarGrid`: quatro anéis mais os raios.
            for (ring in 1..4) {
                val ratio = ring / 4f
                val path = Path()
                rows.indices.forEach { index ->
                    val point = pointAt(index, ratio)
                    if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                path.close()
                drawPath(path, color = Color.White.copy(alpha = 0.16f), style = Stroke(width = 1f))
            }
            rows.indices.forEach { index ->
                drawLine(Color.White.copy(alpha = 0.16f), Offset(centerX, centerY), pointAt(index, 1f), 1f)
            }

            val series = Path()
            rows.forEachIndexed { index, row ->
                val ratio = (eventBiClamp(row.value) / 100.0).toFloat()
                val point = pointAt(index, ratio)
                if (index == 0) series.moveTo(point.x, point.y) else series.lineTo(point.x, point.y)
            }
            series.close()
            drawPath(series, color = ChartValueAxisColor.copy(alpha = 0.28f))
            drawPath(series, color = ChartValueAxisColor, style = Stroke(width = 2f))

            rows.forEachIndexed { index, row ->
                val label = pointAt(index, 1.24f)
                drawChartText(
                    measurer = measurer,
                    text = axisLabel(row.name, maxChars = 10),
                    x = label.x,
                    y = label.y,
                    color = Color.White.copy(alpha = 0.65f),
                    sizeSp = 8f,
                    anchor = TextAnchor.Middle,
                )
            }
        }
        EventBiChartLegend(listOf("Indicador" to ChartValueAxisColor))
    }
}

/**
 * `FunnelMetric` (2808): barra proporcional ao maior `quantity`, com piso de 8% de largura
 * (2815) para a etapa menor continuar visível.
 */
@Composable
fun EventBiFunnelMetric(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
) {
    val rows = data.filter { it.quantity > 0 }
    if (rows.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val max = eventBiMaxValue(rows.map { it.quantity })

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEachIndexed { index, row ->
            val fraction = eventBiFunnelFraction(row.quantity, max).toFloat()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.30f),
                border = BorderStroke(1.dp, PremiumZinc800),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = row.name.uppercase(),
                            modifier = Modifier.weight(1f),
                            color = PremiumZinc300,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatEventBiNumber(row.quantity),
                            color = PremiumZinc300,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .background(com.example.usc1.core.ui.PremiumZinc900, RoundedCornerShape(5.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(18.dp)
                                .background(chartColorAt(index), RoundedCornerShape(5.dp)),
                        )
                    }
                }
            }
        }
    }
}

/** `columnOrder` do `HeatmapMetric` (2954). */
enum class EventBiHeatmapOrder { None, Hour, Alpha }

/**
 * `HeatmapMetric` (2951): até 10 linhas e 8 colunas, intensidade
 * `0.12 + intensity / 115` sobre o verde da paleta (2989).
 */
@Composable
fun EventBiHeatmapMetric(
    data: List<EventBiHeatmapEntry>,
    modifier: Modifier = Modifier,
    maxColumns: Int = 8,
    columnOrder: EventBiHeatmapOrder = EventBiHeatmapOrder.None,
) {
    val rows = eventBiHeatmapRowNames(data)
    val columns = eventBiHeatmapColumnNames(data, columnOrder, maxColumns)
    if (rows.isEmpty() || columns.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val max = eventBiMaxValue(data.map { it.value })
    val byKey = data.associateBy { "${it.row}:${it.column}" }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Canto vazio do cabeçalho (2974).
                Box(modifier = Modifier.width(96.dp).height(22.dp))
                rows.forEach { row ->
                    Box(
                        modifier = Modifier.width(96.dp).height(30.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = row,
                            color = PremiumZinc300,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            columns.forEach { column ->
                Column(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier.width(62.dp).height(22.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = column,
                            color = PremiumZinc500,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    rows.forEach { row ->
                        val entry = byKey["$row:$column"]
                        val intensity = eventBiClamp(safeDivide(entry?.value ?: 0.0, max) * 100)
                        Box(
                            modifier = Modifier
                                .width(62.dp)
                                .height(30.dp)
                                .background(
                                    color = ChartQuantityAxisColor.copy(
                                        alpha = (0.12f + intensity.toFloat() / 115f).coerceIn(0f, 1f),
                                    ),
                                    shape = RoundedCornerShape(5.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = entry?.value?.takeIf { it != 0.0 }
                                    ?.let { formatEventBiNumber(it) } ?: "-",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "MENOR", color = PremiumZinc500, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(7.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(ChartQuantityAxisColor.copy(alpha = 0.15f), ChartQuantityAxisColor),
                        ),
                        shape = RoundedCornerShape(4.dp),
                    ),
            )
            Text(text = "MAIOR VALOR", color = PremiumZinc500, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

/**
 * `TreemapMetric` (3017): até 12 blocos dimensionados por `Math.max(value, quantity)`, com o
 * rótulo formatado em moeda mesmo quando o número veio de `quantity` (3031).
 */
@Composable
fun EventBiTreemapMetric(
    data: List<EventBiMetricRow>,
    modifier: Modifier = Modifier,
) {
    val rows = eventBiTreemapRows(data)
    if (rows.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }

    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        maxItemsInEachRow = 2,
    ) {
        // O web dimensiona o bloco por `flexBasis`/`flexGrow`; no celular a proporção vira a
        // ordem de leitura (o motor já entrega ordenado) em duas colunas de mesma largura.
        rows.forEachIndexed { index, row ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = chartColorAt(index),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp).height(72.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = row.name.uppercase(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Column {
                        Text(
                            text = formatEventBiCurrency(if (row.value != 0.0) row.value else row.quantity),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // `typeof row.secondary === "number"` (3032): no port `secondary` sempre existe.
                        Text(
                            text = formatEventBiPercent(row.secondary),
                            color = Color.White.copy(alpha = 0.80f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * `BubbleMetric` (3081) com o conteúdo de `BubbleTooltip` (3049) resolvido como legenda: no
 * celular não há hover, então os dados que o tooltip mostraria viram a lista abaixo do gráfico.
 */
@Composable
fun EventBiBubbleMetric(
    data: List<EventBiBubbleEntry>,
    modifier: Modifier = Modifier,
    xLabel: String = "Presença",
    yLabel: String = "Receita",
    xFormat: EventBiValueFormat = EventBiValueFormat.Percent,
    yFormat: EventBiValueFormat = EventBiValueFormat.Currency,
) {
    val rows = eventBiBubbleRows(data)
    if (rows.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val isPercentX = xFormat == EventBiValueFormat.Percent
    val xMax = if (isPercentX) 100.0 else eventBiMaxValue(rows.map { it.x }).coerceAtLeast(1.0)
    val yMax = eventBiMaxValue(rows.map { it.y }).coerceAtLeast(1.0)
    val zMax = eventBiMaxValue(rows.map { it.z })

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartCanvas { measurer ->
            val plot = androidx.compose.ui.geometry.Rect(46f, 18f, size.width - 14f, size.height - 30f)
            for (index in 0..4) {
                val ratio = index / 4f
                val y = plot.bottom - plot.height * ratio
                drawLine(ChartGridColor, Offset(plot.left, y), Offset(plot.right, y), 1f)
                drawChartText(
                    measurer = measurer,
                    text = formatEventBiShortValue(yMax * ratio, yFormat),
                    x = plot.left - 5f,
                    y = y,
                    color = ChartAxisColor,
                    anchor = TextAnchor.End,
                )
                val x = plot.left + plot.width * ratio
                drawLine(ChartGridColor, Offset(x, plot.top), Offset(x, plot.bottom), 1f)
                drawChartText(
                    measurer = measurer,
                    text = formatEventBiShortValue(xMax * ratio, xFormat),
                    x = x,
                    y = plot.bottom + 12f,
                    color = ChartAxisColor,
                    sizeSp = 8f,
                    anchor = TextAnchor.Middle,
                )
            }
            // `ReferenceLine` de 50% e 100% (3130) e a base y=0 (3139).
            if (isPercentX) {
                val half = plot.left + plot.width * 0.5f
                drawDashedLine(Offset(half, plot.top), Offset(half, plot.bottom), Color.White.copy(alpha = 0.22f))
                drawDashedLine(
                    Offset(plot.right, plot.top),
                    Offset(plot.right, plot.bottom),
                    Color.White.copy(alpha = 0.38f),
                )
            }
            drawDashedLine(
                Offset(plot.left, plot.bottom),
                Offset(plot.right, plot.bottom),
                Color.White.copy(alpha = 0.22f),
            )

            rows.forEach { row ->
                val cx = plot.left + (row.x / xMax).toFloat().coerceIn(0f, 1f) * plot.width
                val cy = plot.bottom - (row.y / yMax).toFloat().coerceIn(0f, 1f) * plot.height
                // `<ZAxis range={[80, 900]} />` (3127) é área; o raio sai da raiz da área.
                val area = 80.0 + safeDivide(row.z, zMax) * 820.0
                val radius = kotlin.math.sqrt(area / PI).toFloat().coerceIn(4f, 26f)
                drawCircle(
                    color = parseEventBiColor(scoreColor(row.value)).copy(alpha = 0.85f),
                    radius = radius,
                    center = Offset(cx, cy),
                )
                drawChartText(
                    measurer = measurer,
                    text = formatEventBiNumber(row.value),
                    x = cx,
                    y = cy - radius - 7f,
                    color = Color.White,
                    sizeSp = 8f,
                    weight = FontWeight.Black,
                    anchor = TextAnchor.Middle,
                )
            }
        }
        EventBiChartLegend(
            listOf(
                "0-39 repensar" to ChartRose,
                "40-69 ajustar" to ChartAmber,
                "70-84 repetir" to ChartValueAxisColor,
                "85-100 escalar" to ChartQuantityAxisColor,
            ),
        )
        // O que o `BubbleTooltip` mostraria no hover (3068-3076). O web lista 8 chips (3096/3103);
        // aqui a lista tem a mesma extensão, porque é ela que substitui o hover.
        rows.take(8).forEach { row ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.30f),
                border = BorderStroke(1.dp, PremiumZinc800),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EventBiLegendDot(parseEventBiColor(scoreColor(row.value)))
                        Text(
                            text = row.name.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "$xLabel: ${formatEventBiValue(row.x, xFormat)} · " +
                            "$yLabel: ${formatEventBiValue(row.y, yFormat)}",
                        color = PremiumZinc300,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Receita/tamanho da bolha: ${formatEventBiCurrency(row.z)} · " +
                            "Score: ${formatEventBiNumber(row.value)}" +
                            row.decision.takeIf { it.isNotBlank() }?.let { " · Decisão: $it" }.orEmpty(),
                        color = PremiumZinc500,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * `NetworkMetric` (3218): até 12 arestas e 10 nós dispostos em círculo, espessura da aresta
 * proporcional ao valor.
 */
@Composable
fun EventBiNetworkMetric(
    data: List<EventBiNetworkEdge>,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
) {
    val edges = eventBiNetworkEdges(data)
    if (edges.isEmpty()) {
        EventBiEmptyChart(modifier)
        return
    }
    val nodes = eventBiNetworkNodes(edges)
    val max = eventBiMaxValue(edges.map { it.value })

    ChartCanvas(modifier = modifier, height = height) { measurer ->
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(size.width, size.height) * 0.36f
        val positions = nodes.mapIndexed { index, node ->
            val angle = (2 * PI * index / maxOf(nodes.size, 1) - PI / 2).toFloat()
            node to Offset(centerX + cos(angle) * radius, centerY + sin(angle) * radius)
        }.toMap()

        edges.forEachIndexed { index, edge ->
            val from = positions[edge.from]
            val to = positions[edge.to]
            if (from != null && to != null) {
                drawLine(
                    color = chartColorAt(index).copy(alpha = 0.38f),
                    start = from,
                    end = to,
                    strokeWidth = 1f + safeDivide(edge.value, max).toFloat() * 8f,
                )
            }
        }
        nodes.forEachIndexed { index, node ->
            val position = positions[node] ?: return@forEachIndexed
            drawCircle(
                color = chartColorAt(index).copy(alpha = 0.92f),
                radius = 22f,
                center = position,
            )
            drawChartText(
                measurer = measurer,
                text = node.take(10),
                x = position.x,
                y = position.y,
                color = Color.White,
                sizeSp = 7f,
                weight = FontWeight.Black,
                anchor = TextAnchor.Middle,
            )
        }
    }
}
