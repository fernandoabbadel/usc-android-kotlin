package com.example.usc1.ui.membershipCard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.R
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumQrCode
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.ui.theme.UscTheme

@Composable
fun MembershipCard(
    card: MembershipCardUiModel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp)),
    ) {
        Image(
            painter = painterResource(id = R.drawable.carteirinha_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.56f,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_usc),
                            contentDescription = "USC",
                            modifier = Modifier.padding(6.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Column {
                        Text(
                            text = "USC",
                            color = Color.White,
                            fontSize = 19.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = card.tenantName.uppercase(),
                            color = PremiumBrand,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                PremiumChip(
                    label = card.planName,
                    icon = Icons.Outlined.Star,
                    accent = PremiumAmber,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(width = 74.dp, height = 96.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.62f),
                    border = BorderStroke(2.dp, PremiumBrand.copy(alpha = 0.70f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_aaakn),
                            contentDescription = "Foto do atleta",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.86f,
                        )
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                            tint = Color.White.copy(alpha = 0.30f),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "NOME DO ATLETA",
                        color = PremiumZinc400,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = card.userName.uppercase(),
                        color = Color.White,
                        fontSize = 17.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column {
                            Text(
                                text = "TURMA",
                                color = PremiumZinc500,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = card.className,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Column {
                            Text(
                                text = "MATRÍCULA",
                                color = PremiumZinc500,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = card.memberCode.takeLast(6),
                                color = PremiumZinc400,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(1.dp))
                    .padding(top = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "VALIDADE",
                        color = PremiumZinc500,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = card.validUntil,
                        color = PremiumBrandAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "ESCANEIE\nPARA VALIDAR",
                        color = PremiumZinc500,
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                    PremiumQrCode(
                        payload = card.memberCode,
                        cells = 7,
                        cellSize = 3.dp,
                    )
                }
            }
        }
    }
}

@Composable
fun MembershipQrModalCard(
    card: MembershipCardUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.Black,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_usc),
                    contentDescription = "USC",
                    modifier = Modifier.padding(8.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                text = "ACESSO ATLETA",
                color = Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
            PremiumQrCode(
                payload = card.memberCode,
                cells = 13,
                cellSize = 9.dp,
                label = "QR mockado",
            )
            Text(
                text = card.memberCode,
                color = PremiumZinc500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun MembershipCardPreview() {
    UscTheme(darkTheme = true) {
        MembershipCard(
            card = MembershipCardUiModel(),
            modifier = Modifier.padding(16.dp),
        )
    }
}
