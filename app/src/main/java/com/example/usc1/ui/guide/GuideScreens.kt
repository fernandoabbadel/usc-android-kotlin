package com.example.usc1.ui.guide

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContactSupport
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.R
import com.example.usc1.core.ui.NativeModuleHeroCard
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumMenuRow
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.ui.theme.UscTheme

@Composable
fun GuideScreen(state: GuideUiState, onFaqClick: () -> Unit, onSupportClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Guia USC", subtitle = "Como navegar pelo app", icon = Icons.Outlined.HelpOutline)
        NativeModuleHeroCard("GUIA DO CARDUME", "Central de ajuda", "Fluxos principais do app nativo, sem WebView e sem rede real.", R.drawable.logo_platform_web)
        state.guideItems.forEach { item ->
            PremiumMenuRow(item.title, item.description, Icons.Outlined.Description, badge = item.badge, onClick = if (item.badge == "FAQ") onFaqClick else onSupportClick)
        }
    }
}

@Composable
fun FaqScreen(state: GuideUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    InfoListScreen("FAQ", "Dúvidas frequentes", Icons.Outlined.HelpOutline, state.faqItems, onBackClick, modifier)
}

@Composable
fun ContactUscScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    InfoListScreen(
        "Contato USC",
        "Canais oficiais mockados",
        Icons.Outlined.Mail,
        listOf(
            GuideItem("E-mail", "contato mockado da plataforma USC.", "USC"),
            GuideItem("Instagram", "Canal social da atlética e comunidade.", "Social"),
        ),
        onBackClick,
        modifier,
    )
}

@Composable
fun SupportScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    InfoListScreen(
        "Suporte",
        "Atendimento e solicitações",
        Icons.Outlined.ContactSupport,
        listOf(
            GuideItem("Pedido com problema", "Abra solicitação para loja, eventos ou planos.", "Pedido"),
            GuideItem("Conta e acesso", "Convite, aprovação, banimento e segurança.", "Conta"),
        ),
        onBackClick,
        modifier,
    )
}

@Composable
fun TermsScreen(state: LegalUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    InfoListScreen("Termos", "Documentos legais", Icons.Outlined.Gavel, state.terms, onBackClick, modifier)
}

@Composable
fun PrivacyLgpdScreen(state: LegalUiState, onRequestClick: () -> Unit, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "LGPD", subtitle = "Privacidade e direitos", icon = Icons.Outlined.Lock, onBackClick = onBackClick)
        state.terms.forEach { item ->
            PremiumMenuRow(item.title, item.description, Icons.Outlined.Lock, badge = item.badge, onClick = onRequestClick)
        }
    }
}

@Composable
fun LgpdRequestScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    InfoListScreen(
        "Solicitar LGPD",
        "Formulário visual mockado",
        Icons.Outlined.Lock,
        listOf(
            GuideItem("Acesso aos dados", "Solicitação local sem envio real.", "Dados"),
            GuideItem("Exclusão de conta", "Fluxo futuro com autenticação e auditoria.", "Conta"),
        ),
        onBackClick,
        modifier,
    )
}

@Composable
fun LegalDocumentScreen(state: LegalUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    InfoListScreen("Documento Legal", "Texto resumido mockado", Icons.Outlined.Description, state.terms, onBackClick, modifier)
}

@Composable
private fun InfoListScreen(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<GuideItem>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = title, subtitle = subtitle, icon = icon, accent = PremiumBrand, onBackClick = onBackClick)
        NativeSectionTitle(title = subtitle)
        items.forEach { item ->
            PremiumMenuRow(item.title, item.description, icon, badge = item.badge, onClick = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun GuideScreenPreview() {
    UscTheme(darkTheme = true) {
        GuideScreen(GuideUiState(), {}, {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun TermsScreenPreview() {
    UscTheme(darkTheme = true) {
        TermsScreen(LegalUiState(), {})
    }
}
