package com.example.usc1.ui.bi.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.domain.model.EventBiColors
import com.example.usc1.domain.model.EventBiMetricRow
import com.example.usc1.domain.model.EventBiValueFormat
import com.example.usc1.domain.model.formatEventBiShortValue
import com.example.usc1.domain.model.formatEventBiValue

/**
 * Base dos 26 componentes de gráfico/tabela do BI de Eventos (M8.2).
 *
 * Fonte: `web-reference/src/app/admin/bi/_components/AdminEventBiDashboard.tsx`, linhas 2139-3378
 * (`KpiCard`, `KpiGrid`, `ChartPanel`, `EmptyChart`, `FilterLinkChips` e o restante do bloco).
 *
 * O web desenha com Recharts. O app desenha em `Canvas` do Compose, sem biblioteca de gráfico:
 * o Vico 2.1.3 — a opção avaliada — cobre só barra vertical, linha e combo; pizza, rosca, barra
 * horizontal, radar e dispersão ficariam de fora, e conviver com dois renderizadores no mesmo
 * scroll deixaria metade dos painéis com uma linguagem visual e metade com outra.
 *
 * Adaptação de tela declarada: o web usa `xl:grid-cols-2`/`xl:grid-cols-3` para os painéis e
 * `md:grid-cols-2 xl:grid-cols-4` para os KPIs; no celular tudo isso vira coluna única no web.
 * O app mantém painel em coluna única e KPI em duas colunas, que é o `md` do web.
 */

// ------------------------------------------------------------------
// Paleta
// ------------------------------------------------------------------

/** `COLORS` (192) já em `Color` do Compose. */
val EventBiChartColors: List<Color> = EventBiColors.map(::parseEventBiColor)

/** Converte o `#rrggbb` que o motor devolve (`scoreColor`, 415) em `Color`. */
fun parseEventBiColor(hex: String): Color {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6) return PremiumBrand
    val value = clean.toLongOrNull(16) ?: return PremiumBrand
    return Color(0xFF000000L or value)
}

internal fun chartColorAt(index: Int): Color =
    EventBiChartColors[((index % EventBiChartColors.size) + EventBiChartColors.size) % EventBiChartColors.size]

/** `stroke="rgba(255,255,255,0.08)"` do `CartesianGrid`. */
internal val ChartGridColor = Color.White.copy(alpha = 0.08f)

/** `stroke="rgba(255,255,255,0.45)"` dos eixos neutros. */
internal val ChartAxisColor = Color.White.copy(alpha = 0.45f)

/** `rgba(34,197,94,0.8)` — eixo de quantidade. */
internal val ChartQuantityAxisColor = Color(0xFF22C55E)

/** `rgba(56,189,248,0.75)` — eixo de valor. */
internal val ChartValueAxisColor = Color(0xFF38BDF8)

internal val ChartAmber = Color(0xFFFACC15)
internal val ChartRose = Color(0xFFFB7185)

/** Altura de `h-[340px] sm:h-[360px]` (2237), reduzida para a proporção do celular. */
internal val ChartHeight: Dp = 264.dp

// ------------------------------------------------------------------
// Texto no Canvas
// ------------------------------------------------------------------

internal fun DrawScope.drawChartText(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    color: Color,
    sizeSp: Float = 9f,
    weight: FontWeight = FontWeight.Bold,
    anchor: TextAnchor = TextAnchor.Start,
    rotationDegrees: Float = 0f,
) {
    if (text.isBlank()) return
    val layout = measurer.measure(
        AnnotatedString(text),
        style = TextStyle(color = color, fontSize = sizeSp.sp, fontWeight = weight),
    )
    val dx = when (anchor) {
        TextAnchor.Start -> 0f
        TextAnchor.Middle -> -layout.size.width / 2f
        TextAnchor.End -> -layout.size.width.toFloat()
    }
    val topLeft = Offset(x + dx, y - layout.size.height / 2f)
    if (rotationDegrees == 0f) {
        drawText(layout, topLeft = topLeft)
    } else {
        rotate(degrees = rotationDegrees, pivot = Offset(x, y)) {
            drawText(layout, topLeft = topLeft)
        }
    }
}

internal enum class TextAnchor { Start, Middle, End }

/** Corta o rótulo do eixo como o `width={118}` do `YAxis` categórico faz no web. */
internal fun axisLabel(name: String, maxChars: Int = 14): String =
    if (name.length <= maxChars) name else name.take(maxChars - 1).trimEnd() + "…"

// ------------------------------------------------------------------
// Moldura cartesiana
// ------------------------------------------------------------------

/** Teto do eixo, com a folga de 18% do web — ver `eventBiAxisTop` em `EventBiChartData.kt`. */
internal fun axisTop(dataMax: Double): Double = eventBiAxisTop(dataMax)

internal class CartesianFrame(
    val plot: Rect,
    val leftMax: Double,
    val rightMax: Double,
) {
    fun yOfLeft(value: Double): Float =
        plot.bottom - (if (leftMax <= 0.0) 0f else (value / leftMax).toFloat().coerceIn(0f, 1f)) * plot.height

    fun yOfRight(value: Double): Float =
        plot.bottom - (if (rightMax <= 0.0) 0f else (value / rightMax).toFloat().coerceIn(0f, 1f)) * plot.height

    /** Centro da coluna `index` de `count` colunas, como o eixo de categoria do Recharts. */
    fun xOfBand(index: Int, count: Int): Float {
        if (count <= 0) return plot.left
        val band = plot.width / count
        return plot.left + band * index + band / 2f
    }

    fun bandWidth(count: Int): Float = if (count <= 0) plot.width else plot.width / count
}

/**
 * Desenha grade, ticks do eixo esquerdo, ticks do eixo direito e os rótulos de categoria
 * rotacionados em -18° (`angle={-18} textAnchor="end"`, 2378).
 */
internal fun DrawScope.drawCartesianFrame(
    measurer: TextMeasurer,
    names: List<String>,
    leftMax: Double,
    rightMax: Double = 0.0,
    leftFormat: EventBiValueFormat = EventBiValueFormat.Number,
    rightFormat: EventBiValueFormat = EventBiValueFormat.Currency,
    leftAxisColor: Color = ChartAxisColor,
    rightAxisColor: Color = ChartValueAxisColor,
    ticks: Int = 4,
): CartesianFrame {
    val hasRight = rightMax > 0.0
    val leftGutter = 40f
    val rightGutter = if (hasRight) 44f else 12f
    val bottomGutter = 46f
    val plot = Rect(
        left = leftGutter,
        top = 18f,
        right = size.width - rightGutter,
        bottom = size.height - bottomGutter,
    )
    val frame = CartesianFrame(plot, leftMax, rightMax)

    for (index in 0..ticks) {
        val ratio = index.toFloat() / ticks
        val y = plot.bottom - plot.height * ratio
        drawLine(
            color = ChartGridColor,
            start = Offset(plot.left, y),
            end = Offset(plot.right, y),
            strokeWidth = 1f,
        )
        drawChartText(
            measurer = measurer,
            text = formatEventBiShortValue(leftMax * ratio, leftFormat),
            x = plot.left - 5f,
            y = y,
            color = leftAxisColor,
            anchor = TextAnchor.End,
        )
        if (hasRight) {
            drawChartText(
                measurer = measurer,
                text = formatEventBiShortValue(rightMax * ratio, rightFormat),
                x = plot.right + 5f,
                y = y,
                color = rightAxisColor,
                anchor = TextAnchor.Start,
            )
        }
    }

    // Densidade: em vez de cortar a série (o que perderia dado), o eixo mostra 1 rótulo a cada N
    // quando a categoria não cabe. O Recharts faz o mesmo com `interval="preserveStartEnd"`.
    val maxLabels = (plot.width / 46f).toInt().coerceAtLeast(1)
    val step = if (names.size <= maxLabels) 1 else (names.size + maxLabels - 1) / maxLabels
    names.forEachIndexed { index, name ->
        if (index % step != 0) return@forEachIndexed
        drawChartText(
            measurer = measurer,
            text = axisLabel(name, maxChars = 12),
            x = frame.xOfBand(index, names.size),
            y = plot.bottom + 16f,
            color = ChartAxisColor,
            sizeSp = 8f,
            anchor = TextAnchor.End,
            rotationDegrees = -18f,
        )
    }
    return frame
}

/** Linha tracejada `strokeDasharray="4 4"` das `ReferenceLine`. */
internal fun DrawScope.drawDashedLine(start: Offset, end: Offset, color: Color) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
    )
}

// ------------------------------------------------------------------
// `EmptyChart` (2263)
// ------------------------------------------------------------------

@Composable
fun EventBiEmptyChart(modifier: Modifier = Modifier, height: Dp = ChartHeight) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "Sem dados para o filtro atual.",
                color = PremiumZinc500,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ------------------------------------------------------------------
// `KpiCard` (2139) e `KpiGrid` (2188)
// ------------------------------------------------------------------

/**
 * `KpiCard`. `href` só vira clique quando o construtor de link está ativo — hoje ele é
 * `EventBiLinkBuilder.Inert` e devolve string vazia até o M10 (workspace de evento).
 */
@Composable
fun EventBiKpiCard(
    label: String,
    value: String,
    hint: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    info: String = "",
) {
    var infoOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = label.uppercase(),
                    modifier = Modifier.weight(1f),
                    color = PremiumZinc500,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
                if (info.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = info,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { infoOpen = true },
                        tint = ChartValueAxisColor,
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PremiumBrand,
                )
            }
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = hint,
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    if (infoOpen) {
        EventBiInfoDialog(title = label, text = info, onDismiss = { infoOpen = false })
    }
}

/**
 * `KpiGrid` (2188). O web é `grid gap-3 md:grid-cols-2 xl:grid-cols-4`; o celular usa o recorte
 * `md`, de duas colunas, e cada card recebe `Modifier.weight(1f)` do `FlowRowScope`.
 */
@Composable
fun EventBiKpiGrid(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
        content = content,
    )
}

// ------------------------------------------------------------------
// `ChartPanel` (2192)
// ------------------------------------------------------------------

@Composable
fun EventBiChartPanel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    info: String = "",
    cornerMetric: String = "",
    toolbar: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var infoOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title.uppercase(),
                            color = PremiumZinc300,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.9.sp,
                        )
                        if (info.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Entender $title",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { infoOpen = true },
                                tint = ChartValueAxisColor,
                            )
                        }
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            color = PremiumZinc500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                toolbar?.invoke()
            }

            if (cornerMetric.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.70f),
                    border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.25f)),
                ) {
                    Text(
                        text = cornerMetric.uppercase(),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        color = PremiumBrand,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.7.sp,
                    )
                }
            }

            content()
            footer?.invoke()
        }
    }

    if (infoOpen) {
        EventBiInfoDialog(title = title, text = info, onDismiss = { infoOpen = false })
    }
}

/** O modal "Como funciona" do `ChartPanel` (2239). */
@Composable
private fun EventBiInfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Fechar", color = PremiumBrand, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Como funciona",
                    color = ChartValueAxisColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        text = {
            Text(
                text = text,
                color = PremiumZinc300,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
            )
        },
        containerColor = com.example.usc1.core.ui.PremiumZinc950,
    )
}

// ------------------------------------------------------------------
// `FilterLinkChips` (2289)
// ------------------------------------------------------------------

/** Um item da legenda com link para o extrato. */
data class EventBiChipLink(val label: String, val href: String = "", val color: Color? = null)

/**
 * `FilterLinkChips`: `links.filter(link => link.href?.trim())` — sem href, nada é renderizado.
 * Com `EventBiLinkBuilder.Inert` (M10 pendente) todos os href chegam vazios, então o rodapé
 * simplesmente não aparece, igual ao web quando não há evento selecionado.
 */
@Composable
fun EventBiFilterLinkChips(
    links: List<EventBiChipLink>,
    modifier: Modifier = Modifier,
    label: String = "Abrir filtro no extrato",
) {
    val visible = links.filter { it.href.isNotBlank() }
    if (visible.isEmpty()) return

    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "$label:",
            color = PremiumZinc500,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
        visible.forEach { link ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, PremiumZinc700),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    link.color?.let { EventBiLegendDot(it) }
                    Text(
                        text = link.label,
                        color = PremiumZinc300,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
internal fun EventBiLegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color = color, shape = RoundedCornerShape(4.dp)),
    )
}

/** Legenda `<Legend />` do Recharts: bolinha + nome da série. */
@Composable
internal fun EventBiChartLegend(
    entries: List<Pair<String, Color>>,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        entries.forEach { (name, color) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EventBiLegendDot(color)
                Text(
                    text = name,
                    color = PremiumZinc400Legend,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private val PremiumZinc400Legend = Color(0xFFA1A1AA)

// ------------------------------------------------------------------
// Utilitários de série
// ------------------------------------------------------------------

/** Canvas com `TextMeasurer` já preparado, na altura padrão do painel. */
@Composable
internal fun ChartCanvas(
    modifier: Modifier = Modifier,
    height: Dp = ChartHeight,
    onDraw: DrawScope.(TextMeasurer) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        onDraw(measurer)
    }
}

internal fun barRect(centerX: Float, width: Float, top: Float, bottom: Float): Pair<Offset, Size> {
    val safeTop = minOf(top, bottom)
    val safeHeight = kotlin.math.abs(bottom - top)
    return Offset(centerX - width / 2f, safeTop) to Size(width, safeHeight)
}

internal fun List<EventBiMetricRow>.maxOfQuantity(): Double =
    fold(0.0) { current, row -> maxOf(current, row.quantity) }

internal fun List<EventBiMetricRow>.maxOfValue(): Double =
    fold(0.0) { current, row -> maxOf(current, row.value) }

internal fun formatAxis(value: Double, format: EventBiValueFormat): String =
    formatEventBiValue(value, format)
