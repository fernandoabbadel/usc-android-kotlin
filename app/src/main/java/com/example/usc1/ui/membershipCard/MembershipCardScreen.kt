package com.example.usc1.ui.membershipCard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.example.usc1.R
import com.example.usc1.ui.home.BottomNavDestination
import com.example.usc1.ui.home.FloatingBottomNavigation

@Composable
fun MembershipCardScreen(
    state: MembershipCardUiState,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPlansClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onEventsClick: () -> Unit = {},
    onScannerClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
) {
    var showQrModal by rememberSaveable { mutableStateOf(false) }
    val card = state.card
    val accent = membershipTenantAccent(card.tenantPalette)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MembershipScreenBackground),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MembershipStickyHeader(
                accent = accent,
                onBackClick = onBackClick,
            )

            if (card.userName.isBlank()) {
                MembershipEmptyContent(
                    title = if (state.errorMessage == null) {
                        "Carteirinha não carregada"
                    } else {
                        "Não foi possível carregar a identidade"
                    },
                    message = state.errorMessage
                        ?: "Entre com Google e aguarde a sessão real do Supabase.",
                    accent = accent,
                    onRefreshClick = onRefreshClick,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        top = 32.dp,
                        end = 24.dp,
                        bottom = 122.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    item(key = "membership-card") {
                        MembershipCard(
                            card = card,
                            isConfigLoading = state.isConfigLoading,
                            modifier = Modifier.widthIn(max = 448.dp),
                        )
                    }

                    if (card.canUpgrade) {
                        item(key = "membership-upgrade") {
                            MembershipUpgradeCard(
                                planName = card.planName,
                                accent = accent,
                                onClick = onPlansClick,
                            )
                        }
                    }

                    item(key = "membership-qr-button") {
                        MembershipQrButton(
                            accent = accent,
                            onClick = { showQrModal = true },
                        )
                    }

                    item(key = "membership-official-label") {
                        Text(
                            text = "Documento Digital Oficial • ${card.tenantAcronym.ifBlank { "USC" }.uppercase()}",
                            color = MembershipZinc600,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.8.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .widthIn(max = 448.dp)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }

        FloatingBottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter),
            selectedDestination = BottomNavDestination.Wallet,
            onHomeClick = onHomeClick,
            onEventsClick = onEventsClick,
            onScannerClick = onScannerClick,
            onWalletClick = onWalletClick,
            onMenuClick = onMenuClick,
        )

        if (showQrModal) {
            MembershipQrDialog(
                card = card,
                onDismiss = { showQrModal = false },
            )
        }
    }
}

@Composable
private fun MembershipStickyHeader(
    accent: Color,
    onBackClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MembershipScreenBackground.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .height(56.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Voltar",
                    tint = MembershipZinc400,
                    modifier = Modifier.size(24.dp),
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CreditCard,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "IDENTIDADE",
                    color = accent,
                    fontSize = 13.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.6.sp,
                )
            }
            Spacer(modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun MembershipEmptyContent(
    title: String,
    message: String,
    accent: Color,
    onRefreshClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp)
            .widthIn(max = 448.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
        )
        Text(
            text = message,
            color = MembershipZinc500,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(onClick = onRefreshClick),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "ATUALIZAR",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
private fun MembershipUpgradeCard(
    planName: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 448.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    ) {
        Box(
            modifier = Modifier.background(
                Brush.horizontalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.22f),
                        Color.Black,
                    ),
                ),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = accent.copy(alpha = 0.18f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "NÍVEL ATUAL: ${planName.ifBlank { "Bicho" }.uppercase()}",
                            color = accent,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Fazer Upgrade",
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MembershipZinc500,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun MembershipQrButton(
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 448.dp)
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0x8018181B),
        border = BorderStroke(1.dp, Color(0xFF27272A)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "AMPLIAR QR CODE",
                color = Color(0xFFE4E4E7),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun MembershipQrDialog(
    card: MembershipCardUiModel,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 360.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 20.dp,
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF4F4F5))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Fechar",
                            tint = Color(0xFF71717A),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black,
                        ) {
                            AsyncImage(
                                model = card.tenantLogoUrl,
                                contentDescription = null,
                                placeholder = painterResource(R.drawable.logo_usc),
                                fallback = painterResource(R.drawable.logo_usc),
                                error = painterResource(R.drawable.logo_usc),
                                modifier = Modifier.padding(9.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ACESSO ATLETA",
                            color = Color.Black,
                            fontSize = 20.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp,
                        )
                        Text(
                            text = "APROXIME DO LEITOR",
                            color = Color(0xFF71717A),
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.6.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                        )

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            border = BorderStroke(6.dp, Color.Black),
                            shadowElevation = 12.dp,
                        ) {
                            IdentityQrCode(
                                payload = card.qrPayload.ifBlank { card.userId },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(220.dp),
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .padding(top = 24.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFAFAFA),
                            border = BorderStroke(1.dp, Color(0xFFE4E4E7)),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "ID SEGURANÇA",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.6.sp,
                                )
                                Text(
                                    text = "${card.userId.take(12)}...",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val MembershipScreenBackground = Color(0xFF050505)
private val MembershipZinc400 = Color(0xFFA1A1AA)
private val MembershipZinc500 = Color(0xFF71717A)
private val MembershipZinc600 = Color(0xFF52525B)

@Preview(showBackground = true, backgroundColor = 0xFF050505, widthDp = 390, heightDp = 844)
@Composable
private fun MembershipCardScreenPreview() {
    MembershipCardScreen(
        state = MembershipCardUiState(
            isConfigLoading = false,
            card = MembershipCardUiModel(
                userId = "3e4fc3ca0b7f",
                userName = "Fernando Lopes Abbade",
                tenantName = "Atlética Demo USC",
                tenantAcronym = "USC",
                tenantCourse = "Medicina",
                classCode = "T2",
                registrationNumber = "10125496",
                planName = "Atleta",
                planColorKey = MembershipPlanColorKey.Emerald,
                qrPayload = "{\"t\":\"usuario\",\"v\":1}",
            ),
        ),
        onRefreshClick = {},
        onBackClick = {},
    )
}
