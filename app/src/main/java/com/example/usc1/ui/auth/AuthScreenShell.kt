package com.example.usc1.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBlueBlack
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumLogoHero
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSmallShape
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900

@Composable
fun AuthScreenShell(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Shield,
    content: @Composable ColumnScope.() -> Unit,
) {
    PremiumScreen(
        modifier = modifier,
        useBlueGlow = true,
        bottomPadding = 36.dp,
        verticalSpacing = 22.dp,
    ) {
        PremiumLogoHero(
            title = "UNIVERSIDADE SPOT CONNECT",
            subtitle = "Plataforma oficial multiatléticas",
        )

        PremiumCard(
            accent = PremiumBrand,
            containerColor = PremiumZinc900.copy(alpha = 0.82f),
            borderAlpha = 0.55f,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = PremiumSmallShape,
                    color = PremiumBrand.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.34f)),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = PremiumBrandAccent,
                    )
                }
                Text(
                    text = title,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 23.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = subtitle,
                    color = PremiumZinc400,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            content()
        }
    }
}

@Composable
fun AuthInlineMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PremiumSmallShape,
        color = PremiumBlueBlack.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PremiumZinc800),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            color = PremiumZinc400,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
