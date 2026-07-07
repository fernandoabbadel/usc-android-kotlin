package com.example.usc1.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.R

val PremiumBlack = Color(0xFF050505)
val PremiumBlueBlack = Color(0xFF02050D)
val PremiumZinc950 = Color(0xFF09090B)
val PremiumZinc900 = Color(0xFF18181B)
val PremiumZinc850 = Color(0xFF1F1F23)
val PremiumZinc800 = Color(0xFF27272A)
val PremiumZinc700 = Color(0xFF3F3F46)
val PremiumZinc600 = Color(0xFF52525B)
val PremiumZinc500 = Color(0xFF71717A)
val PremiumZinc400 = Color(0xFFA1A1AA)
val PremiumZinc300 = Color(0xFFD4D4D8)
val PremiumBrand = Color(0xFF10B981)
val PremiumBrandAccent = Color(0xFF34D399)
val PremiumGold = Color(0xFFEAB308)
val PremiumAmber = Color(0xFFF59E0B)
val PremiumRed = Color(0xFFEF4444)
val PremiumPurple = Color(0xFFA855F7)

val PremiumCardShape = RoundedCornerShape(28.dp)
val PremiumSmallShape = RoundedCornerShape(14.dp)

@Composable
fun PremiumBackground(
    modifier: Modifier = Modifier,
    useBlueGlow: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val base = if (useBlueGlow) PremiumBlueBlack else PremiumBlack
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (useBlueGlow) Color(0xFF071735) else base,
                        base,
                        Color.Black,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            (if (useBlueGlow) Color(0xFF3B82F6) else PremiumBrand).copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                        center = Offset(220f, 120f),
                        radius = 620f,
                    ),
                ),
        )
        content()
    }
}

@Composable
fun PremiumScreen(
    modifier: Modifier = Modifier,
    useBlueGlow: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    bottomPadding: Dp = 32.dp,
    verticalSpacing: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    PremiumBackground(modifier = modifier, useBlueGlow = useBlueGlow) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = horizontalPadding,
                    top = 24.dp,
                    end = horizontalPadding,
                    bottom = bottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
    }
}

@Composable
fun PremiumHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = PremiumBrand,
    onBackClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBackClick != null) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBackClick),
                shape = CircleShape,
                color = PremiumZinc900,
                border = BorderStroke(1.dp, PremiumZinc800),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Voltar",
                    modifier = Modifier.padding(11.dp),
                    tint = PremiumZinc400,
                )
            }
        }

        if (icon != null) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(16.dp),
                color = accent.copy(alpha = 0.13f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.38f)),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(11.dp),
                    tint = accent,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 26.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = PremiumZinc500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PremiumLogoHero(
    modifier: Modifier = Modifier,
    logoRes: Int = R.drawable.logo_platform,
    title: String = "UNIVERSIDADE SPOT CONNECT",
    subtitle: String = "Plataforma oficial multiatléticas",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(142.dp)
                    .clip(CircleShape)
                    .background(PremiumBrand.copy(alpha = 0.12f)),
            )
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = "USC",
                modifier = Modifier.size(132.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 28.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(16.dp), tint = PremiumBrand)
            Text(
                text = subtitle,
                color = PremiumZinc400,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Icon(Icons.Outlined.Waves, contentDescription = null, modifier = Modifier.size(16.dp), tint = PremiumBrand)
        }
    }
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
    containerColor: Color = PremiumZinc900.copy(alpha = 0.86f),
    borderAlpha: Float = 0.28f,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PremiumCardShape,
        color = containerColor,
        border = BorderStroke(1.dp, accent.copy(alpha = borderAlpha)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
fun PremiumImageCard(
    imageRes: Int,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    accent: Color = PremiumBrand,
    imageAlpha: Float = 0.52f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(PremiumCardShape)
            .background(PremiumZinc900)
            .border(1.dp, accent.copy(alpha = 0.34f), PremiumCardShape),
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = imageAlpha,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black,
                        ),
                    ),
                ),
        )
        content()
    }
}

@Composable
fun PremiumChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = PremiumBrand,
    filled: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = if (filled) accent else accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = if (filled) 1f else 0.38f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (filled) Color.Black else accent,
                )
            }
            Text(
                text = label.uppercase(),
                color = if (filled) Color.Black else accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PremiumPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = PremiumBrand,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = Color.Black,
            disabledContainerColor = PremiumZinc800,
            disabledContentColor = PremiumZinc500,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.Black,
                strokeWidth = 2.dp,
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Text(
                text = text.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
fun PremiumSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = PremiumBrand,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = accent,
            disabledContentColor = PremiumZinc500,
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(
            text = text.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: ImageVector? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        label = {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
            )
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = PremiumZinc500,
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.Black,
            unfocusedContainerColor = Color.Black,
            focusedBorderColor = PremiumBrand,
            unfocusedBorderColor = PremiumZinc800,
            focusedLabelColor = PremiumBrandAccent,
            unfocusedLabelColor = PremiumZinc500,
            cursorColor = PremiumBrand,
        ),
    )
}

@Composable
fun PremiumInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.34f))
            .border(1.dp, PremiumZinc800, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        Text(
            text = value,
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PremiumMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PremiumZinc900)
            .border(1.dp, PremiumZinc800, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(14.dp),
            color = accent.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(9.dp),
                tint = accent,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = PremiumZinc500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (badge != null) {
            PremiumChip(label = badge, accent = accent)
        } else {
            Icon(
                imageVector = Icons.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = PremiumZinc600,
            )
        }
    }
}

@Composable
fun PremiumQrCode(
    payload: String,
    modifier: Modifier = Modifier,
    cells: Int = 11,
    cellSize: Dp = 8.dp,
    label: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(cells) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(cells) { column ->
                            val filled = row in 0..2 && column in 0..2 ||
                                row in 0..2 && column in (cells - 3) until cells ||
                                row in (cells - 3) until cells && column in 0..2 ||
                                (payload.length + row * 7 + column * 11) % 5 != 0
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .background(
                                        color = if (filled) Color.Black else Color.White,
                                        shape = RoundedCornerShape(1.dp),
                                    ),
                            )
                        }
                    }
                }
            }
            if (label != null) {
                Text(
                    text = label.uppercase(),
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
fun PremiumLoadingState(
    text: String,
    modifier: Modifier = Modifier,
) {
    PremiumBackground(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = PremiumBrand)
            Text(
                text = text.uppercase(),
                modifier = Modifier.padding(top = 14.dp),
                color = PremiumBrand,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
fun PremiumEmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Outlined.CheckCircle,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
) {
    PremiumCard(modifier = modifier, accent = PremiumZinc700, borderAlpha = 0.65f) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(14.dp), tint = accent)
            }
            Text(
                text = title.uppercase(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = subtitle,
                color = PremiumZinc500,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
