package com.example.usc1.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ContactSupport
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.SettingsSupportCategory
import com.example.usc1.domain.model.SettingsSupportTicket

/**
 * `/configuracoes/suporte` — abrir chamado em `support_requests` e ver os últimos 20,
 * com a resposta da diretoria quando existir.
 */
@Composable
fun SettingsSupportScreen(
    state: SettingsSupportUiState,
    onBackClick: () -> Unit,
    onSubmitClick: (SettingsSupportCategory, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var category by remember { mutableStateOf(SettingsSupportCategory.Geral) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Suporte",
            subtitle = "Denúncias e ajuda",
            icon = Icons.AutoMirrored.Outlined.ContactSupport,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumBrand) {
            Text(
                text = "CATEGORIA",
                color = PremiumZinc500,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingsSupportCategory.entries.forEach { option ->
                    Surface(
                        modifier = Modifier.clickable { category = option },
                        shape = RoundedCornerShape(999.dp),
                        color = if (category == option) PremiumBrand else PremiumZinc900,
                        border = BorderStroke(
                            1.dp,
                            if (category == option) PremiumBrand else PremiumZinc800,
                        ),
                    ) {
                        Text(
                            text = option.label.uppercase(),
                            color = if (category == option) Color.Black else PremiumZinc400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        )
                    }
                }
            }

            PremiumTextField(
                value = subject,
                onValueChange = {
                    subject = it.take(SettingsSupportCategory.SubjectMaxLength)
                },
                label = "Assunto",
            )
            Text(
                text = "${subject.length}/${SettingsSupportCategory.SubjectMaxLength}",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )

            PremiumTextField(
                value = message,
                onValueChange = {
                    message = it.take(SettingsSupportCategory.MessageMaxLength)
                },
                label = "Mensagem",
                singleLine = false,
            )
            Text(
                text = "${message.length}/${SettingsSupportCategory.MessageMaxLength}",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )

            if (state.errorMessage.isNotBlank()) {
                Text(
                    text = state.errorMessage,
                    color = PremiumRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            PremiumPrimaryButton(
                text = if (state.isSending) "Enviando" else "Enviar chamado",
                onClick = {
                    onSubmitClick(category, subject, message)
                    if (subject.isNotBlank() && message.isNotBlank()) {
                        subject = ""
                        message = ""
                    }
                },
                enabled = !state.isSending && subject.isNotBlank() && message.isNotBlank(),
                loading = state.isSending,
                icon = Icons.AutoMirrored.Outlined.Send,
            )
        }

        Text(
            text = "ULTIMOS CHAMADOS",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )

        when {
            state.isLoading -> PremiumLoadingState(text = "Carregando histórico")

            state.tickets.isEmpty() -> PremiumEmptyState(
                title = "Nenhum chamado aberto ainda",
                subtitle = "Quando você abrir um chamado, ele aparece aqui com a resposta da diretoria.",
                icon = Icons.AutoMirrored.Outlined.ContactSupport,
            )

            else -> state.tickets.forEach { ticket -> SupportTicketCard(ticket) }
        }
    }
}

@Composable
private fun SupportTicketCard(ticket: SettingsSupportTicket) {
    val accent = if (ticket.isResolved) PremiumBrand else PremiumAmber
    PremiumCard(accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = ticket.subject,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            PremiumChip(
                label = if (ticket.isResolved) "Resolvido" else "Pendente",
                accent = accent,
            )
        }
        Text(
            text = ticket.message,
            color = PremiumZinc400,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
        if (ticket.response.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = PremiumBrand.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.24f)),
            ) {
                Text(
                    text = "RESPOSTA DA DIRETORIA\n${ticket.response}",
                    color = PremiumBrand,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        Text(
            text = listOf(ticket.category, ticket.createdAtLabel)
                .filter(String::isNotBlank)
                .joinToString(" • "),
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
