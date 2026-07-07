package com.example.usc1.ui.scanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.NativeAction
import com.example.usc1.core.ui.NativeActionCard
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.ui.theme.UscTheme

@Composable
fun ScannerScreen(
    state: ScannerUiState,
    onEventScannerClick: () -> Unit,
    onPartyScannerClick: () -> Unit,
    onProductScannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.hasPermission) {
        ScannerPermissionDeniedScreen(modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Scanner", subtitle = "Validação visual sem câmera real", icon = Icons.Outlined.QrCodeScanner)
        ScannerFrame(title = "Aponte para o QR", subtitle = "Placeholder nativo para câmera futura")
        NativeActionCard(NativeAction("Eventos", "Validar ingressos de eventos.", Icons.Outlined.QrCodeScanner), onEventScannerClick)
        NativeActionCard(NativeAction("Festas", "Controle de entrada e acesso.", Icons.Outlined.QrCodeScanner, PremiumAmber), onPartyScannerClick)
        NativeActionCard(NativeAction("Produtos/Fichas", "Retirada de pedido e consumo.", Icons.Outlined.QrCodeScanner), onProductScannerClick)
    }
}

@Composable
fun EventCheckInScannerScreen(state: ScannerUiState, onSuccessClick: () -> Unit, onErrorClick: () -> Unit, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    ScannerModeScreen("Scanner Eventos", state, ScannerMode.EventTicket, onSuccessClick, onErrorClick, onBackClick, modifier)
}

@Composable
fun PartyScannerScreen(state: ScannerUiState, onSuccessClick: () -> Unit, onErrorClick: () -> Unit, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    ScannerModeScreen("Scanner Festas", state, ScannerMode.PartyAccess, onSuccessClick, onErrorClick, onBackClick, modifier)
}

@Composable
fun ProductWithdrawalScannerScreen(state: ScannerUiState, onSuccessClick: () -> Unit, onErrorClick: () -> Unit, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    ScannerModeScreen("Scanner Produtos", state, ScannerMode.ProductWithdrawal, onSuccessClick, onErrorClick, onBackClick, modifier)
}

@Composable
private fun ScannerModeScreen(
    title: String,
    state: ScannerUiState,
    mode: ScannerMode,
    onSuccessClick: () -> Unit,
    onErrorClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = title, subtitle = mode.label, icon = Icons.Outlined.QrCodeScanner, onBackClick = onBackClick)
        ScannerFrame(title = mode.label.uppercase(), subtitle = "Simulação de leitura para testes locais")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumPrimaryButton(
                text = "Sucesso",
                onClick = onSuccessClick,
                icon = Icons.Outlined.CheckCircle,
                modifier = Modifier.weight(1f),
            )
            PremiumPrimaryButton(
                text = "Erro",
                onClick = onErrorClick,
                icon = Icons.Outlined.ErrorOutline,
                accent = PremiumRed,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun ScannerResultSuccessScreen(result: ScannerResult, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    ScannerResultScreen(result, true, onBackClick, modifier)
}

@Composable
fun ScannerResultErrorScreen(result: ScannerResult, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    ScannerResultScreen(result, false, onBackClick, modifier)
}

@Composable
fun ScannerPermissionDeniedScreen(modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Acesso negado", subtitle = "Scanner exige role autorizada", icon = Icons.Outlined.Security, accent = PremiumRed)
        PremiumCard(accent = PremiumRed) {
            PremiumChip(label = "Permissão bloqueada", icon = Icons.Outlined.Security, accent = PremiumRed)
            Text(
                text = "Somente roles de vendas, gestor, admin ou master devem acessar scanner/check-in no app nativo.",
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ScannerResultScreen(
    result: ScannerResult,
    success: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (success) PremiumBrand else PremiumRed
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = if (success) "Validado" else "Erro no QR",
            subtitle = result.payload,
            icon = if (success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            accent = accent,
            onBackClick = onBackClick,
        )
        PremiumCard(accent = accent) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(116.dp),
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(2.dp, accent),
            ) {
                Icon(
                    imageVector = if (success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.padding(28.dp),
                    tint = accent,
                )
            }
            Text(
                text = result.title.uppercase(),
                color = Color.White,
                fontSize = 27.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
            Text(
                text = result.subtitle,
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ScannerFrame(
    title: String,
    subtitle: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .background(Color.Black, RoundedCornerShape(32.dp))
            .border(1.dp, PremiumBrand.copy(alpha = 0.35f), RoundedCornerShape(32.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = PremiumBrand,
                style = Stroke(width = 5f),
                cornerRadius = CornerRadius(42f, 42f),
            )
            drawLine(
                color = PremiumBrand.copy(alpha = 0.74f),
                start = center.copy(x = 0f),
                end = center.copy(x = size.width),
                strokeWidth = 3f,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = PremiumBrand.copy(alpha = 0.12f), border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.38f))) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.padding(18.dp).size(48.dp), tint = PremiumBrand)
            }
            Text(text = title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(text = subtitle, color = PremiumZinc500, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun ScannerScreenPreview() {
    UscTheme(darkTheme = true) {
        ScannerScreen(ScannerUiState(), {}, {}, {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun ScannerResultSuccessScreenPreview() {
    UscTheme(darkTheme = true) {
        ScannerResultSuccessScreen(ScannerUiState().successResult, {})
    }
}
