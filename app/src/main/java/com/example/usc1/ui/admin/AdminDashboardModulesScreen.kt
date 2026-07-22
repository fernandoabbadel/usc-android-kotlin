package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminDashboardModulesGroup
import com.example.usc1.domain.model.TenantAppModuleDefinition

@Composable
fun AdminDashboardModulesScreen(
    state: AdminDashboardModulesUiState,
    onToggleModule: (String) -> Unit,
    onSaveClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando modulos...", modifier = modifier)
        state.errorMessage != null && state.groups.isEmpty() -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Modulos do App",
                subtitle = "Erro ao carregar modulos da tenant.",
                icon = Icons.Outlined.Home,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Erro ao carregar modulos da tenant.",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Home,
            )
            PremiumPrimaryButton(
                text = "Tentar novamente",
                onClick = onRefreshClick,
                icon = Icons.Outlined.Refresh,
            )
        }
        else -> AdminDashboardModulesLoadedContent(
            state = state,
            onToggleModule = onToggleModule,
            onSaveClick = onSaveClick,
            onRefreshClick = onRefreshClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun AdminDashboardModulesLoadedContent(
    state: AdminDashboardModulesUiState,
    onToggleModule: (String) -> Unit,
    onSaveClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier,
) {
    val tenantLabel = state.tenantSlug.ifBlank { state.tenantName.ifBlank { "tenant" } }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
    ) {
        PremiumHeader(
            title = "Modulos do App",
            subtitle = "Dashboard e lateral de $tenantLabel",
            icon = Icons.Outlined.Home,
            onBackClick = onBackClick,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumPrimaryButton(
                text = if (state.isSaving) "Salvando..." else "Salvar",
                onClick = onSaveClick,
                enabled = !state.isSaving && !state.isLoading && state.hasActiveTenant,
                loading = state.isSaving,
                icon = Icons.Outlined.Save,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Atualizar",
                onClick = onRefreshClick,
                enabled = !state.isSaving,
                icon = Icons.Outlined.Refresh,
                modifier = Modifier.weight(1f),
            )
        }

        if (!state.hasActiveTenant) {
            PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
                Text(
                    text = "Nenhuma tenant ativa foi encontrada para editar os modulos.",
                    color = PremiumZinc400,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
                Text(
                    text = "Perfil do master ativo para esta tenant: ${state.activeProfileName}.",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Itens ocultos pelo perfil não aparecem aqui e continuam bloqueados para os usuários.",
                    color = PremiumZinc500,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        state.saveMessage?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = PremiumBrand.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.32f)),
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(14.dp),
                    color = PremiumBrandAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        state.groups.forEach { group ->
            DashboardModulesGroupCard(
                group = group,
                state = state,
                onToggleModule = onToggleModule,
            )
        }

        PremiumSecondaryButton(
            text = "Voltar",
            onClick = onBackClick,
            icon = Icons.Outlined.ArrowBack,
        )
    }
}

@Composable
private fun DashboardModulesGroupCard(
    group: AdminDashboardModulesGroup,
    state: AdminDashboardModulesUiState,
    onToggleModule: (String) -> Unit,
) {
    PremiumCard(
        accent = PremiumZinc800,
        containerColor = PremiumZinc900,
    ) {
        Text(
            text = group.label,
            color = PremiumZinc400,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        group.modules.forEachIndexed { index, module ->
            if (index > 0) {
                HorizontalDivider(color = PremiumZinc800)
            }
            DashboardModuleRow(
                module = module,
                enabled = state.isModuleEnabled(module),
                onToggle = { onToggleModule(module.key) },
            )
        }
    }
}

@Composable
private fun DashboardModuleRow(
    module: TenantAppModuleDefinition,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = module.label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, PremiumZinc700),
                ) {
                    Text(
                        text = module.surfaces.joinToString(" / "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = PremiumZinc500,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = module.description,
                color = PremiumZinc500,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            module.route?.let { route ->
                Text(
                    text = route,
                    color = PremiumBrandAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Surface(
            modifier = Modifier.widthIn(min = 96.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (enabled) PremiumBrand.copy(alpha = 0.10f) else Color.Black,
            border = BorderStroke(
                1.dp,
                if (enabled) PremiumBrand.copy(alpha = 0.42f) else PremiumZinc700,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (enabled) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (enabled) PremiumBrandAccent else PremiumZinc500,
                )
                Text(
                    text = if (enabled) "Visivel" else "Oculto",
                    color = if (enabled) PremiumBrandAccent else PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
        }
    }
}
