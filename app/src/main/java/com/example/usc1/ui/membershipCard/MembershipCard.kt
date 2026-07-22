package com.example.usc1.ui.membershipCard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageVector
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.R
import com.example.usc1.core.tenant.TenantPalette
import com.example.usc1.data.repository.normalizeMembershipClassCode
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlin.math.min

private val CardEmerald = Color(0xFF34D399)
private val CardZinc400 = Color(0xFFA1A1AA)
private val CardZinc500 = Color(0xFF71717A)

@Composable
fun MembershipCard(
    card: MembershipCardUiModel,
    isConfigLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val planVisual = membershipPlanVisual(card.planColorKey)
    val tenantAccent = membershipTenantAccent(card.tenantPalette)
    val cardShape = RoundedCornerShape(16.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .shadow(
                elevation = 24.dp,
                shape = cardShape,
                ambientColor = planVisual.accent.copy(alpha = 0.22f),
                spotColor = planVisual.accent.copy(alpha = 0.22f),
            )
            .clip(cardShape)
            .background(Color(0xFF18181B))
            .border(1.dp, Color.White.copy(alpha = 0.10f), cardShape),
    ) {
        val scale = (maxWidth.value / 360f).coerceIn(0.82f, 1.12f)
        val contentPadding = 20.dp * scale

        MembershipCardBackground(
            card = card,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MembershipCardTop(
                card = card,
                scale = scale,
                tenantAccent = tenantAccent,
                planVisual = planVisual,
            )
            MembershipCardIdentity(
                card = card,
                scale = scale,
                planVisual = planVisual,
            )
            MembershipCardFooter(
                card = card,
                scale = scale,
                isConfigLoading = isConfigLoading,
            )
        }
    }
}

@Composable
private fun MembershipCardBackground(
    card: MembershipCardUiModel,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color(0xFF18181B))) {
        val imageAlpha = 0.16f + card.backgroundOpacity.coerceIn(0, 100) / 100f * 0.58f
        val saturation = 0.65f + card.backgroundOpacity.coerceIn(0, 100) / 100f * 0.70f
        val colorMatrix = remember(saturation) {
            ColorMatrix().apply { setToSaturation(saturation) }
        }
        val backgroundModifier = Modifier
            .fillMaxSize()
            .alpha(imageAlpha)

        when {
            !card.backgroundUrl.isNullOrBlank() -> AsyncImage(
                model = card.backgroundUrl,
                contentDescription = null,
                modifier = backgroundModifier,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(colorMatrix),
            )
            membershipClassBackgroundRes(card.classCode) != null -> Image(
                painter = painterResource(requireNotNull(membershipClassBackgroundRes(card.classCode))),
                contentDescription = null,
                modifier = backgroundModifier,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(colorMatrix),
            )
            else -> AsyncImage(
                model = card.tenantLogoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp)
                    .alpha(0.18f),
                contentScale = ContentScale.Fit,
                fallback = painterResource(R.drawable.logo_usc),
                error = painterResource(R.drawable.logo_usc),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.34f),
                            Color.Black.copy(alpha = 0.95f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun MembershipCardTop(
    card: MembershipCardUiModel,
    scale: Float,
    tenantAccent: Color,
    planVisual: MembershipPlanVisual,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp * scale),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp * scale),
                shape = RoundedCornerShape(8.dp * scale),
                color = Color.Black.copy(alpha = 0.80f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                shadowElevation = 6.dp,
            ) {
                AsyncImage(
                    model = card.tenantLogoUrl,
                    contentDescription = "Logo ${card.tenantName.ifBlank { card.tenantAcronym }}",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp * scale),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.logo_usc),
                    fallback = painterResource(R.drawable.logo_usc),
                    error = painterResource(R.drawable.logo_usc),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = card.tenantAcronym.uppercase(),
                    color = Color.White,
                    fontSize = (18f * scale).sp,
                    lineHeight = (18f * scale).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.35f).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = card.tenantCourse.uppercase(),
                    color = tenantAccent,
                    fontSize = (9f * scale).sp,
                    lineHeight = (10f * scale).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (1.1f * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp * scale),
            color = planVisual.softBackground,
            border = BorderStroke(1.dp, planVisual.border),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 9.dp * scale,
                    vertical = 5.dp * scale,
                ),
                horizontalArrangement = Arrangement.spacedBy(5.dp * scale),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = membershipPlanIcon(card.planIconKey),
                    contentDescription = null,
                    modifier = Modifier.size(11.dp * scale),
                    tint = planVisual.accent,
                )
                Text(
                    text = card.planName.uppercase(),
                    color = planVisual.accent,
                    fontSize = (9f * scale).sp,
                    lineHeight = (10f * scale).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (0.7f * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MembershipCardIdentity(
    card: MembershipCardUiModel,
    scale: Float,
    planVisual: MembershipPlanVisual,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp * scale),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 72.dp * scale, height = 96.dp * scale)
                .clip(RoundedCornerShape(8.dp * scale))
                .background(Color.Black.copy(alpha = 0.50f))
                .border(
                    width = 2.dp,
                    color = planVisual.border,
                    shape = RoundedCornerShape(8.dp * scale),
                )
                .padding(2.dp * scale),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(40.dp * scale),
                tint = Color.White.copy(alpha = 0.44f),
            )
            if (!card.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = card.avatarUrl,
                    contentDescription = "Foto de ${card.userName}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(5.dp * scale)),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp * scale),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp * scale)) {
                MembershipCardLabel(text = "Nome do Atleta", scale = scale)
                Text(
                    text = card.userName.uppercase(),
                    color = Color.White,
                    fontSize = (16f * scale).sp,
                    lineHeight = (17f * scale).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp * scale)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp * scale)) {
                    MembershipCardLabel(text = "Turma", scale = scale)
                    Text(
                        text = card.classCode.ifBlank { "CALOURO" },
                        color = Color.White,
                        fontSize = (14f * scale).sp,
                        lineHeight = (15f * scale).sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp * scale)) {
                    MembershipCardLabel(text = "Matrícula", scale = scale)
                    Text(
                        text = card.registrationNumber.ifBlank { "---" },
                        color = Color(0xFFD4D4D8),
                        fontSize = (14f * scale).sp,
                        lineHeight = (15f * scale).sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun MembershipCardFooter(
    card: MembershipCardUiModel,
    scale: Float,
    isConfigLoading: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(1.dp),
            )
            .padding(top = 9.dp * scale),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp * scale)) {
            Text(
                text = "VALIDADE",
                color = CardZinc500,
                fontSize = (8f * scale).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (1.4f * scale).sp,
            )
            if (isConfigLoading) {
                Box(
                    modifier = Modifier
                        .width(48.dp * scale)
                        .height(10.dp * scale)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                )
            } else {
                Text(
                    text = card.validity,
                    color = CardEmerald,
                    fontSize = (10f * scale).sp,
                    lineHeight = (11f * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (0.7f * scale).sp,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp * scale),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ESCANEIE PARA\nVALIDAR ACESSO",
                color = CardZinc500,
                fontSize = (7f * scale).sp,
                lineHeight = (8f * scale).sp,
                fontWeight = FontWeight.Medium,
            )
            Surface(
                shape = RoundedCornerShape(3.dp * scale),
                color = Color.White,
                shadowElevation = 4.dp,
            ) {
                IdentityQrCode(
                    payload = card.qrPayload.ifBlank { card.userId },
                    modifier = Modifier
                        .padding(3.dp * scale)
                        .size(36.dp * scale),
                )
            }
        }
    }
}

@Composable
private fun MembershipCardLabel(text: String, scale: Float) {
    Text(
        text = text.uppercase(),
        color = CardZinc400,
        fontSize = (9f * scale).sp,
        lineHeight = (9f * scale).sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (0.75f * scale).sp,
    )
}

@Composable
fun IdentityQrCode(
    payload: String,
    modifier: Modifier = Modifier,
) {
    val qrMatrix = remember(payload) {
        runCatching {
            QRCodeWriter().encode(
                payload.ifBlank { "USC" },
                BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN to 1,
                ),
            )
        }.getOrNull()
    }

    Canvas(
        modifier = modifier
            .background(Color.White)
            .semantics { contentDescription = "QR Code da identidade" },
    ) {
        drawRect(Color.White)
        qrMatrix?.let { matrix -> drawQrMatrix(matrix, Color.Black) }
    }
}

private fun DrawScope.drawQrMatrix(
    matrix: com.google.zxing.common.BitMatrix,
    foreground: Color,
) {
    val moduleSize = min(size.width / matrix.width, size.height / matrix.height)
    val qrSize = moduleSize * matrix.width
    val origin = Offset(
        x = (size.width - qrSize) / 2f,
        y = (size.height - qrSize) / 2f,
    )
    for (row in 0 until matrix.height) {
        for (column in 0 until matrix.width) {
            if (matrix[column, row]) {
                drawRect(
                    color = foreground,
                    topLeft = Offset(
                        x = origin.x + column * moduleSize,
                        y = origin.y + row * moduleSize,
                    ),
                    size = Size(moduleSize, moduleSize),
                )
            }
        }
    }
}

@DrawableRes
internal fun membershipClassBackgroundRes(classCode: String): Int? {
    return when (normalizeMembershipClassCode(classCode)) {
        "T1" -> R.drawable.turma1
        "T2" -> R.drawable.turma2
        "T3" -> R.drawable.turma3
        "T4" -> R.drawable.turma4
        "T5" -> R.drawable.turma5
        "T6" -> R.drawable.turma6
        "T7" -> R.drawable.turma7
        "T8" -> R.drawable.turma8
        "T9" -> R.drawable.turma9
        else -> null
    }
}

internal data class MembershipPlanVisual(
    val accent: Color,
    val border: Color,
    val softBackground: Color,
)

internal fun membershipPlanVisual(key: MembershipPlanColorKey): MembershipPlanVisual {
    val accent = when (key) {
        MembershipPlanColorKey.Yellow -> Color(0xFFFACC15)
        MembershipPlanColorKey.Emerald -> Color(0xFF34D399)
        MembershipPlanColorKey.Purple -> Color(0xFFC084FC)
        MembershipPlanColorKey.Blue -> Color(0xFF60A5FA)
        MembershipPlanColorKey.Red -> Color(0xFFEF4444)
        MembershipPlanColorKey.Orange -> Color(0xFFFB923C)
        MembershipPlanColorKey.Zinc -> Color(0xFFA1A1AA)
    }
    return MembershipPlanVisual(
        accent = accent,
        border = accent.copy(alpha = 0.50f),
        softBackground = accent.copy(alpha = 0.20f),
    )
}

internal fun membershipTenantAccent(palette: TenantPalette): Color {
    return when (palette) {
        TenantPalette.Green -> Color(0xFF10B981)
        TenantPalette.Yellow -> Color(0xFFF59E0B)
        TenantPalette.Red -> Color(0xFFEF4444)
        TenantPalette.Blue -> Color(0xFF3B82F6)
        TenantPalette.Orange -> Color(0xFFF97316)
        TenantPalette.Purple -> Color(0xFF8B5CF6)
        TenantPalette.Pink -> Color(0xFFEC4899)
    }
}

private fun membershipPlanIcon(rawValue: String): ImageVector {
    return when (rawValue.trim().lowercase().filter(Char::isLetterOrDigit)) {
        "star" -> Icons.Outlined.StarBorder
        "crown", "trophy" -> Icons.Outlined.EmojiEvents
        "gem" -> Icons.Outlined.Diamond
        "zap" -> Icons.Outlined.Bolt
        "rocket" -> Icons.Outlined.RocketLaunch
        "medal", "award" -> Icons.Outlined.MilitaryTech
        "heart" -> Icons.Outlined.FavoriteBorder
        "layoutgrid", "grid" -> Icons.Outlined.Workspaces
        "userplus", "users" -> Icons.Outlined.Groups
        "shoppingbag", "shopping", "cart" -> Icons.Outlined.ShoppingBag
        "user" -> Icons.Outlined.PersonOutline
        else -> Icons.Outlined.AutoAwesome
    }
}
