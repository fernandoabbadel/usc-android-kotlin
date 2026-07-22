package com.example.usc1.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminMentorshipLabelsConfig

@Composable
fun AdminMentorshipScreen(
    state: AdminMentorshipUiState,
    onLabelsChange: ((AdminMentorshipLabelsConfig) -> AdminMentorshipLabelsConfig) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando...", modifier = modifier)
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 18.dp) {
        PremiumHeader(
            title = "Admin Apadrinhamento",
            subtitle = "Títulos dinâmicos para a atlética ativa",
            icon = Icons.Outlined.FavoriteBorder,
            accent = PremiumBrandAccent,
            onBackClick = onBackClick,
        )
        PremiumCard(accent = PremiumBrandAccent, containerColor = PremiumZinc900) {
            Text(
                text = "Rótulos Dinâmicos",
                color = PremiumBrandAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Personalize a linguagem do vínculo",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "O que for salvo aqui aparece no perfil público, na central do usuário e na área de aceite dos convites.",
                color = PremiumZinc400,
                fontSize = 13.sp,
            )
        }
        state.errorMessage?.let { message ->
            Text(text = message, color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        state.successMessage?.let { message ->
            Text(text = message, color = Color(0xFF86EFAC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        MentorshipLabelsForm(labels = state.labels, onLabelsChange = onLabelsChange)
        MentorshipPreview(labels = state.labels)
        PremiumPrimaryButton(
            text = if (state.isSaving) "Salvando..." else "Salvar",
            onClick = onSaveClick,
            loading = state.isSaving,
            icon = Icons.Outlined.Save,
            accent = PremiumBrandAccent,
        )
    }
}

@Composable
private fun MentorshipLabelsForm(
    labels: AdminMentorshipLabelsConfig,
    onLabelsChange: ((AdminMentorshipLabelsConfig) -> AdminMentorshipLabelsConfig) -> Unit,
) {
    PremiumCard(accent = PremiumBrandAccent, containerColor = PremiumZinc900) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumTextField(
                value = labels.hubTitle,
                onValueChange = { value -> onLabelsChange { it.copy(hubTitle = value) } },
                label = "Título da área",
                modifier = Modifier.weight(1f),
            )
            PremiumTextField(
                value = labels.mentorLabel,
                onValueChange = { value -> onLabelsChange { it.copy(mentorLabel = value) } },
                label = "Rótulo do mentor",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumTextField(
                value = labels.menteeLabel,
                onValueChange = { value -> onLabelsChange { it.copy(menteeLabel = value) } },
                label = "Rótulo do afilhado",
                modifier = Modifier.weight(1f),
            )
            PremiumTextField(
                value = labels.inviteMentorLabel,
                onValueChange = { value -> onLabelsChange { it.copy(inviteMentorLabel = value) } },
                label = "Botão para pedir mentor",
                modifier = Modifier.weight(1f),
            )
        }
        PremiumTextField(
            value = labels.inviteMenteeLabel,
            onValueChange = { value -> onLabelsChange { it.copy(inviteMenteeLabel = value) } },
            label = "Botão para pedir afilhado",
        )
        PremiumTextField(
            value = labels.requestHelpText,
            onValueChange = { value -> onLabelsChange { it.copy(requestHelpText = value) } },
            label = "Texto de apoio",
            singleLine = false,
        )
    }
}

@Composable
private fun MentorshipPreview(labels: AdminMentorshipLabelsConfig) {
    PremiumCard(accent = Color(0xFF22D3EE), containerColor = PremiumZinc900) {
        Text(text = "Preview", color = PremiumZinc500, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(text = labels.hubTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(text = labels.requestHelpText, color = PremiumZinc400, fontSize = 13.sp)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumCard(accent = Color(0xFF22D3EE), containerColor = Color.Black) {
                Text(text = labels.mentorLabel, color = Color(0xFF67E8F9), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(text = labels.inviteMentorLabel, color = PremiumZinc400, fontSize = 13.sp)
            }
            PremiumCard(accent = Color(0xFF10B981), containerColor = Color.Black) {
                Text(text = labels.menteeLabel, color = Color(0xFF6EE7B7), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(text = labels.inviteMenteeLabel, color = PremiumZinc400, fontSize = 13.sp)
            }
        }
    }
}
