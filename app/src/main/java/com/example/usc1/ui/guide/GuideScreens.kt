@file:OptIn(ExperimentalLayoutApi::class)

package com.example.usc1.ui.guide

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ContactSupport
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.R
import com.example.usc1.core.ui.NativeModuleHeroCard
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumMenuRow
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

@Composable
fun GuideScreen(
    state: GuideUiState,
    onFaqClick: () -> Unit,
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Guia do Bixo",
            subtitle = "Central de info da atlética",
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
        )

        if (state.isLoading) {
            PremiumLoadingState(text = "Carregando guia")
            return@PremiumScreen
        }

        state.errorMessage?.takeIf(String::isNotBlank)?.let { message ->
            PremiumEmptyState(
                title = "Guia em fallback",
                subtitle = message,
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
            )
        }

        NativeModuleHeroCard(
            title = "GUIA DO CARDUME",
            subtitle = "Info útil no bolso",
            body = "Links acadêmicos, grupos, transporte, turismo e emergência no padrão do web app.",
            imageRes = R.drawable.logo_platform_web,
        )

        if (state.sections.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum item publicado",
                subtitle = "Quando a atlética alimentar o guia, os cards aparecerão aqui.",
                icon = Icons.Outlined.Description,
            )
        } else {
            state.sections.forEach { section ->
                GuideSectionCard(section = section)
            }
        }

        PremiumMenuRow(
            title = "Dúvidas frequentes",
            subtitle = "Perguntas rápidas sobre app, eventos e planos",
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            badge = "FAQ",
            onClick = onFaqClick,
        )
        PremiumMenuRow(
            title = "Suporte",
            subtitle = "Ajuda para pedidos, conta e acesso",
            icon = Icons.AutoMirrored.Outlined.ContactSupport,
            badge = "Ajuda",
            onClick = onSupportClick,
        )
    }
}

@Composable
fun ContactUscScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoListScreen(
        title = "Contato USC",
        subtitle = "Canais oficiais",
        icon = Icons.Outlined.Mail,
        items = listOf(
            GuideItem("email-usc", GuideCategory.Groups, 1, "E-mail", "Contato oficial da plataforma USC.", "USC"),
            GuideItem("instagram", GuideCategory.Groups, 2, "Instagram", "Canal social da atlética e comunidade.", "Social"),
        ),
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
fun TermsScreen(
    state: LegalUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LegalDocsScreen(
        title = "Termos",
        subtitle = "Documentos legais",
        icon = Icons.Outlined.Gavel,
        docs = state.docs,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
fun PrivacyLgpdScreen(
    state: LegalUiState,
    onRequestClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val privacyDocs = state.docs.filter { doc ->
        val haystack = "${doc.title} ${doc.content} ${doc.type}".lowercase()
        haystack.contains("privacidade") || haystack.contains("lgpd") || haystack.contains("dados")
    }.ifEmpty { state.docs }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "LGPD",
            subtitle = "Privacidade e direitos",
            icon = Icons.Outlined.Lock,
            onBackClick = onBackClick,
        )

        if (state.isLoading) {
            PremiumLoadingState(text = "Carregando LGPD")
            return@PremiumScreen
        }

        state.errorMessage?.takeIf(String::isNotBlank)?.let { message ->
            PremiumEmptyState(
                title = "LGPD em fallback",
                subtitle = message,
                icon = Icons.Outlined.Lock,
            )
        }

        privacyDocs.forEach { doc ->
            LegalDocCard(doc = doc, compact = true)
        }

        PremiumPrimaryButton(
            text = "Solicitar meus dados",
            onClick = onRequestClick,
            icon = Icons.Outlined.Lock,
        )
    }
}

@Composable
fun LgpdRequestScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoListScreen(
        title = "Solicitar LGPD",
        subtitle = "Direitos do titular",
        icon = Icons.Outlined.Lock,
        items = listOf(
            GuideItem("acesso-dados", GuideCategory.Academic, 1, "Acesso aos dados", "Solicitação para consultar dados vinculados à conta.", "Dados"),
            GuideItem("exclusao-conta", GuideCategory.Academic, 2, "Exclusão de conta", "Fluxo com autenticação, auditoria e validação administrativa.", "Conta"),
        ),
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

/** `/legal/[slug]`: sem slug lista tudo, com slug abre o documento pedido. */
@Composable
fun LegalDocumentScreen(
    state: LegalUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    slug: String = "",
) {
    val selected = state.documentBySlug(slug)
    LegalDocsScreen(
        title = selected?.title ?: "Documento Legal",
        subtitle = selected?.updatedAtLabel ?: "Texto e versão vigente",
        icon = Icons.Outlined.Description,
        docs = selected?.let { listOf(it) } ?: state.docs,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        compact = false,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun GuideSectionCard(section: GuideSection) {
    val accent = categoryAccent(section.category)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = categoryIcon(section.category),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = section.category.label.uppercase(),
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }

        if (section.category == GuideCategory.Tourism || section.category == GuideCategory.Emergency) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                section.items.forEach { item ->
                    GuideCompactCard(
                        item = item,
                        accent = accent,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            section.items.forEach { item ->
                GuideWideCard(item = item, accent = accent)
            }
        }
    }
}

@Composable
private fun GuideWideCard(
    item: GuideItem,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    PremiumCard(
        modifier = modifier,
        accent = accent,
        borderAlpha = 0.28f,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PremiumChip(label = item.badge, icon = categoryIcon(item.category), accent = accent)
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = item.description,
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                item.schedule?.let { schedule ->
                    Text(
                        text = schedule,
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideCompactCard(
    item: GuideItem,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(if (item.category == GuideCategory.Tourism) 150.dp else 118.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.category == GuideCategory.Tourism && !item.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.photoUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.62f,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                            ),
                        ),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = item.phone ?: item.title,
                    color = if (item.color == "red") PremiumRed else accent,
                    fontSize = if (item.category == GuideCategory.Emergency) 24.sp else 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (item.category == GuideCategory.Emergency) item.title else item.description,
                    color = Color.White,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun InfoListScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    items: List<GuideItem>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = title, subtitle = subtitle, icon = icon, accent = PremiumBrand, onBackClick = onBackClick)

        if (isLoading) {
            PremiumLoadingState(text = "Carregando")
            return@PremiumScreen
        }

        errorMessage?.takeIf(String::isNotBlank)?.let { message ->
            PremiumEmptyState(
                title = "$title em fallback",
                subtitle = message,
                icon = icon,
            )
        }

        items.forEach { item ->
            GuideWideCard(item = item, accent = categoryAccent(item.category))
        }
    }
}

@Composable
private fun LegalDocsScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    docs: List<LegalDocUiModel>,
    isLoading: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = title, subtitle = subtitle, icon = icon, accent = PremiumBrand, onBackClick = onBackClick)

        if (isLoading) {
            PremiumLoadingState(text = "Carregando documentos")
            return@PremiumScreen
        }

        errorMessage?.takeIf(String::isNotBlank)?.let { message ->
            PremiumEmptyState(
                title = "Documentos em fallback",
                subtitle = message,
                icon = icon,
            )
        }

        if (docs.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum documento publicado",
                subtitle = "Os termos aparecerão aqui quando forem publicados para a atlética.",
                icon = icon,
            )
        } else {
            docs.forEach { doc ->
                LegalDocCard(doc = doc, compact = compact)
            }
        }
    }
}

@Composable
private fun LegalDocCard(
    doc: LegalDocUiModel,
    compact: Boolean,
) {
    PremiumCard(accent = PremiumBrand, borderAlpha = 0.25f) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PremiumChip(label = doc.type, icon = legalIcon(doc.iconName), accent = PremiumBrand)
                Text(
                    text = doc.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = doc.content,
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (compact) 4 else 30,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = doc.updatedAtLabel,
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

private fun categoryAccent(category: GuideCategory): Color = when (category) {
    GuideCategory.Academic -> PremiumBrand
    GuideCategory.Groups -> Color(0xFF22D3EE)
    GuideCategory.Transport -> PremiumAmber
    GuideCategory.Tourism -> Color(0xFF3B82F6)
    GuideCategory.Emergency -> PremiumRed
}

private fun categoryIcon(category: GuideCategory): ImageVector = when (category) {
    GuideCategory.Academic -> Icons.Outlined.Description
    GuideCategory.Groups -> Icons.AutoMirrored.Outlined.ContactSupport
    GuideCategory.Transport -> Icons.Outlined.Description
    GuideCategory.Tourism -> Icons.Outlined.Description
    GuideCategory.Emergency -> Icons.AutoMirrored.Outlined.ContactSupport
}

private fun legalIcon(raw: String): ImageVector {
    val clean = raw.lowercase()
    return when {
        clean.contains("lock") || clean.contains("privacy") || clean.contains("lgpd") -> Icons.Outlined.Lock
        clean.contains("gavel") || clean.contains("legal") -> Icons.Outlined.Gavel
        clean.contains("mail") -> Icons.Outlined.Mail
        else -> Icons.Outlined.Description
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
