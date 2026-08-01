package com.example.usc1.ui.guide

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.PlatformFaqIcon
import com.example.usc1.domain.model.PlatformFaqQuestion
import com.example.usc1.domain.model.PlatformFaqSection

/** `/faq` — categorias, busca, respostas e envio de dúvida ao painel master. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FaqScreen(
    state: FaqUiState,
    onQueryChange: (String) -> Unit,
    onSectionClick: (String) -> Unit,
    onQuestionClick: (String) -> Unit,
    onDoubtToggle: (String) -> Unit,
    onDoubtTextChange: (String) -> Unit,
    onSendDoubt: (PlatformFaqSection, String) -> Unit,
    onSupportClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp, useBlueGlow = true) {
        PremiumHeader(
            title = state.config.heroTitle.ifBlank { "FAQ" },
            subtitle = state.config.eyebrow.ifBlank { "Central de ajuda USC" },
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            onBackClick = onBackClick,
        )

        if (state.isLoading) {
            PremiumLoadingState(text = "Carregando FAQ")
            return@PremiumScreen
        }

        state.errorMessage?.let { message ->
            PremiumEmptyState(
                title = "FAQ indisponível",
                subtitle = message,
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
            )
            return@PremiumScreen
        }

        if (state.config.isEmpty) {
            PremiumEmptyState(
                title = "FAQ ainda não publicado",
                subtitle = "As perguntas aparecem aqui quando o painel master publica o conteúdo da plataforma.",
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
            )
            return@PremiumScreen
        }

        if (state.config.heroDescription.isNotBlank()) {
            Text(
                text = state.config.heroDescription,
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        PremiumTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = state.config.searchPlaceholder,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FaqStatTile(
                value = state.config.sections.size.toString(),
                label = "Seções",
                modifier = Modifier.weight(1f),
            )
            FaqStatTile(
                value = state.config.totalQuestions.toString(),
                label = "Respostas",
                modifier = Modifier.weight(1f),
            )
            FaqStatTile(
                value = if (state.query.isBlank()) "24/7" else state.matchCount.toString(),
                label = if (state.query.isBlank()) {
                    state.config.updatedLabel.ifBlank { "Guia oficial" }
                } else {
                    "Encontradas"
                },
                accent = PremiumBrand,
                modifier = Modifier.weight(1f),
            )
        }

        state.message?.let { message ->
            PremiumChip(label = message, accent = PremiumBrand)
        }

        Text(
            text = "CATEGORIAS",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.config.sections.forEach { section ->
                FaqSectionChip(
                    section = section,
                    isActive = section.id == state.activeSectionId && state.query.isBlank(),
                    onClick = { onSectionClick(section.id) },
                )
            }
        }

        if (state.visibleSections.isEmpty()) {
            PremiumEmptyState(
                title = "Nada encontrado",
                subtitle = "Tente buscar por outro termo ou abra uma categoria acima.",
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
            )
        } else {
            state.visibleSections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = PremiumBrand.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.3f)),
                        ) {
                            Icon(
                                imageVector = faqSectionIcon(section.icon),
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp),
                                tint = PremiumBrand,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (section.audience.isNotBlank()) {
                                Text(
                                    text = section.audience.uppercase(),
                                    color = PremiumBrand,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                )
                            }
                            Text(
                                text = section.title.uppercase(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                    if (section.description.isNotBlank()) {
                        Text(
                            text = section.description,
                            color = PremiumZinc400,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    section.questions.forEach { question ->
                        FaqQuestionCard(
                            question = question,
                            isOpen = state.openQuestionId == question.id,
                            isDoubtOpen = state.doubtQuestionId == question.id,
                            doubtText = state.doubtText,
                            sendingDoubt = state.sendingDoubt,
                            onQuestionClick = { onQuestionClick(question.id) },
                            onDoubtToggle = { onDoubtToggle(question.id) },
                            onDoubtTextChange = onDoubtTextChange,
                            onSendDoubt = { onSendDoubt(section, question.id) },
                        )
                    }
                }
            }
        }

        if (state.config.supportTitle.isNotBlank()) {
            PremiumCard(accent = PremiumBrand) {
                Text(
                    text = state.config.supportTitle.uppercase(),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
                if (state.config.supportDescription.isNotBlank()) {
                    Text(
                        text = state.config.supportDescription,
                        color = PremiumZinc400,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                PremiumSecondaryButton(
                    text = state.config.supportCtaLabel.ifBlank { "Falar com a USC" },
                    onClick = onSupportClick,
                    icon = Icons.Outlined.SupportAgent,
                )
            }
        }
    }
}

@Composable
private fun FaqQuestionCard(
    question: PlatformFaqQuestion,
    isOpen: Boolean,
    isDoubtOpen: Boolean,
    doubtText: String,
    sendingDoubt: Boolean,
    onQuestionClick: () -> Unit,
    onDoubtToggle: () -> Unit,
    onDoubtTextChange: (String) -> Unit,
    onSendDoubt: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PremiumZinc900.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onQuestionClick),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = question.question,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Black,
                )
                Icon(
                    imageVector = if (isOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = PremiumBrand,
                )
            }

            if (isOpen) {
                Text(
                    text = question.answer,
                    color = PremiumZinc400,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (!question.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                    ) {
                        AsyncImage(
                            model = question.imageUrl,
                            contentDescription = question.imageAlt.ifBlank { question.question },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FaqReactionCounter(
                        icon = Icons.Outlined.ThumbUp,
                        value = question.likes,
                        accent = PremiumBrand,
                    )
                    FaqReactionCounter(
                        icon = Icons.Outlined.ThumbDown,
                        value = question.dislikes,
                        accent = PremiumRed,
                    )
                }
                PremiumSecondaryButton(
                    text = if (isDoubtOpen) "Fechar dúvida" else "Enviar dúvida",
                    onClick = onDoubtToggle,
                    icon = Icons.Outlined.Send,
                )
                if (isDoubtOpen) {
                    Text(
                        text = "A mensagem chega ao painel master com a seção e a pergunta de origem.",
                        color = PremiumZinc500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    PremiumTextField(
                        value = doubtText,
                        onValueChange = onDoubtTextChange,
                        label = "Escreva a sua dúvida sobre esta resposta",
                        singleLine = false,
                    )
                    PremiumPrimaryButton(
                        text = "Enviar ao master",
                        onClick = onSendDoubt,
                        loading = sendingDoubt,
                        icon = Icons.Outlined.Send,
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqReactionCounter(
    icon: ImageVector,
    value: Int,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = accent)
            Text(text = value.toString(), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun FaqSectionChip(
    section: PlatformFaqSection,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) PremiumBrand.copy(alpha = 0.16f) else PremiumZinc900,
        border = BorderStroke(
            1.dp,
            if (isActive) PremiumBrand else PremiumZinc800,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = faqSectionIcon(section.icon),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isActive) PremiumBrand else PremiumZinc400,
            )
            Text(
                text = section.title.uppercase(),
                color = if (isActive) PremiumBrand else PremiumZinc400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${section.questions.size}",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun FaqStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = Color.White,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                text = label.uppercase(),
                color = PremiumZinc500,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** `sectionIconMap` da página do web. */
private fun faqSectionIcon(icon: PlatformFaqIcon): ImageVector = when (icon) {
    PlatformFaqIcon.Start -> Icons.Outlined.MenuBook
    PlatformFaqIcon.Profile -> Icons.Outlined.AccountCircle
    PlatformFaqIcon.Card -> Icons.Outlined.Badge
    PlatformFaqIcon.Events -> Icons.Outlined.ConfirmationNumber
    PlatformFaqIcon.Store -> Icons.Outlined.ShoppingBag
    PlatformFaqIcon.Training -> Icons.Outlined.FitnessCenter
    PlatformFaqIcon.Admin -> Icons.Outlined.Shield
    PlatformFaqIcon.Support -> Icons.Outlined.SupportAgent
}
