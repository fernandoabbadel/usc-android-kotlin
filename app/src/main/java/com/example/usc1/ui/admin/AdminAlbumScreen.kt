package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Save
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminAlbumUiConfig

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminAlbumScreen(
    state: AdminAlbumUiState,
    onTitleChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onCoverChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onAddClassClick: () -> Unit,
    onCacaCalouroClick: () -> Unit,
    onPontuaCalouroClick: () -> Unit,
    onPontuaGeralClick: () -> Unit,
    onCustomizationClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando capa do álbum...", modifier = modifier)
        return
    }

    val menuItems = listOf(
        AlbumHubItem("Caça Calouro", "Predadores de bixos T8", Icons.Outlined.Shield, PremiumAmber, onCacaCalouroClick),
        AlbumHubItem("Pontuação Calouro", "Ranking interno dos calouros", Icons.Outlined.EmojiEvents, Color(0xFF60A5FA), onPontuaCalouroClick),
        AlbumHubItem("Pontuação Geral", "Top geral de capturas", Icons.Outlined.EmojiEvents, PremiumBrand, onPontuaGeralClick),
        AlbumHubItem("Customização", "Editar por turma e layout avançado", Icons.Outlined.Palette, Color(0xFFE879F9), onCustomizationClick),
    )

    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 16.dp) {
        PremiumHeader(
            title = "Admin Álbum",
            subtitle = "Integrado com a capa da página /album",
            icon = Icons.Outlined.EmojiEvents,
            onBackClick = onBackClick,
        )
        state.actionMessage?.let { message ->
            Text(text = message, color = PremiumBrandAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        state.errorMessage?.let { message ->
            Text(text = message, color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        AlbumConfigCard(
            config = state.config,
            saving = state.isSaving,
            onTitleChange = onTitleChange,
            onSubtitleChange = onSubtitleChange,
            onCoverChange = onCoverChange,
            onSaveClick = onSaveClick,
            onAddClassClick = onAddClassClick,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            menuItems.forEach { item ->
                AlbumHubCard(item = item)
            }
        }
    }
}

@Composable
private fun AlbumConfigCard(
    config: AdminAlbumUiConfig,
    saving: Boolean,
    onTitleChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onCoverChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onAddClassClick: () -> Unit,
) {
    PremiumCard(accent = PremiumBrand, containerColor = PremiumZinc900) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Capa da Página /album", color = PremiumBrandAccent, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(text = "Edita em tempo real título, subtítulo e imagem da home do álbum.", color = PremiumZinc500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            PremiumSecondaryButton(
                text = "Adicionar turma",
                onClick = onAddClassClick,
                icon = Icons.Outlined.Add,
                modifier = Modifier.weight(1f),
            )
        }
        PremiumTextField(value = config.title, onValueChange = onTitleChange, label = "Título")
        PremiumTextField(value = config.subtitle, onValueChange = onSubtitleChange, label = "Subtítulo")
        PremiumTextField(value = config.cover, onValueChange = onCoverChange, label = "Imagem da Capa (/public ou URL)")
        Text(
            text = "Para capa/título/subtítulo por turma, use o módulo `Customização`.",
            color = PremiumZinc500,
            fontSize = 11.sp,
        )
        PremiumPrimaryButton(
            text = "Salvar Capa",
            onClick = onSaveClick,
            loading = saving,
            icon = Icons.Outlined.Save,
        )
    }
}

private data class AlbumHubItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit,
)

@Composable
private fun AlbumHubCard(item: AlbumHubItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
        onClick = item.onClick,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = item.accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, item.accent.copy(alpha = 0.34f)),
            ) {
                Icon(item.icon, contentDescription = null, tint = item.accent, modifier = Modifier.padding(11.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(text = item.description, color = PremiumZinc400, fontSize = 12.sp)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = PremiumZinc500)
        }
    }
}
