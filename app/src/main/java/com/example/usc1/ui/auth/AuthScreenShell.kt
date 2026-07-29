package com.example.usc1.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBackground
import com.example.usc1.core.ui.PremiumBlueBlack
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumLogoHero
import com.example.usc1.core.ui.PremiumSmallShape
import com.example.usc1.core.ui.PremiumZinc400
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
    PremiumBackground(
        modifier = modifier,
        useBlueGlow = true,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 34.dp, end = 20.dp, bottom = 36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 384.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                PremiumLogoHero(
                    title = "UNIVERSIDADE SPOT CONNECT",
                    subtitle = "Plataforma oficial multiatléticas",
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = PremiumZinc900.copy(alpha = 0.80f),
                    border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.88f)),
                    shadowElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = PremiumSmallShape,
                            color = Color.Black.copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, PremiumZinc800),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = PremiumSmallShape,
                                    color = PremiumBrand.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.34f)),
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.padding(10.dp),
                                        tint = PremiumBrandAccent,
                                    )
                                }

                                Text(
                                    text = title.uppercase(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    textAlign = TextAlign.Center,
                                )

                                Text(
                                    text = subtitle,
                                    color = PremiumZinc400,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        content()
                    }
                }
            }
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
        border = BorderStroke(1.dp, PremiumZinc800),
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
