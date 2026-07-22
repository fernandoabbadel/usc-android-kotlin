package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
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
import com.example.usc1.domain.model.TenantPolicyDocument

@Composable
fun AdminTenantPoliciesScreen(
    state: AdminTenantPoliciesUiState,
    onContentChange: (String, String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onSaveClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando políticas...", modifier = modifier)
        state.errorMessage != null && state.policies.isEmpty() -> PremiumScreen(modifier = modifier, useBlueGlow = true) {
            PremiumHeader(
                title = "Políticas públicas",
                subtitle = "Erro ao carregar políticas.",
                icon = Icons.Outlined.Shield,
                accent = Color(0xFF60A5FA),
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Erro ao carregar políticas.",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Shield,
            )
            PremiumPrimaryButton(
                text = "Tentar novamente",
                onClick = onRefreshClick,
                icon = Icons.Outlined.Refresh,
                accent = Color(0xFF2563EB),
            )
        }
        else -> AdminTenantPoliciesLoadedContent(
            state = state,
            onContentChange = onContentChange,
            onToggleVisibility = onToggleVisibility,
            onSaveClick = onSaveClick,
            onRefreshClick = onRefreshClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun AdminTenantPoliciesLoadedContent(
    state: AdminTenantPoliciesUiState,
    onContentChange: (String, String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onSaveClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        useBlueGlow = true,
        bottomPadding = 110.dp,
    ) {
        PremiumSecondaryButton(
            text = "Voltar ao admin",
            onClick = onBackClick,
            icon = Icons.Outlined.ArrowBack,
            accent = PremiumZinc400,
        )

        PremiumCard(
            accent = Color(0xFF60A5FA),
            containerColor = Color(0xFF1D4ED8).copy(alpha = 0.12f),
            borderAlpha = 0.22f,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color(0xFFDBEAFE),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Governança do tenant",
                        color = Color(0xFFDBEAFE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
            }
            Text(
                text = "Políticas públicas",
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
            Text(
                text = "Configure regras específicas de ${state.tenantLabel}. Uma política só deve aparecer ao público quando estiver preenchida e marcada como visível.",
                color = PremiumZinc400,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = PremiumAmber.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, PremiumAmber.copy(alpha = 0.24f)),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color(0xFFFEF3C7),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "A USC continua tendo termos globais. Este espaço cobre regras operacionais do tenant/organizador, como cancelamento, reembolso, menores de idade, bebida alcoólica e retirada/entrega.",
                        color = Color(0xFFFEF3C7),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (!state.hasActiveTenant) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.24f)),
            ) {
                Text(
                    text = "Tenant não identificado. Entre pelo caminho do tenant para editar estas políticas.",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFFEE2E2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
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

            state.errorMessage?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.24f)),
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(14.dp),
                        color = Color(0xFFFEE2E2),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            state.policies.forEach { policy ->
                TenantPolicyCard(
                    policy = policy,
                    onContentChange = { content -> onContentChange(policy.module, content) },
                    onToggleVisibility = { onToggleVisibility(policy.module) },
                )
            }

            PremiumPrimaryButton(
                text = "Salvar políticas",
                onClick = onSaveClick,
                enabled = !state.isSaving,
                loading = state.isSaving,
                icon = Icons.Outlined.Save,
                accent = Color(0xFF2563EB),
            )
        }
    }
}

@Composable
private fun TenantPolicyCard(
    policy: TenantPolicyDocument,
    onContentChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
) {
    val canBeVisible = policy.content.trim().isNotBlank()
    val isVisible = policy.visible && canBeVisible

    PremiumCard(
        accent = PremiumZinc800,
        containerColor = Color(0xFF09090B),
        borderAlpha = 1f,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = policy.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = policy.description,
                    color = PremiumZinc500,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Surface(
                modifier = Modifier.widthIn(min = 92.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isVisible) PremiumBrand.copy(alpha = 0.10f) else PremiumZinc900,
                border = BorderStroke(
                    1.dp,
                    if (isVisible) PremiumBrand.copy(alpha = 0.42f) else PremiumZinc700,
                ),
                onClick = onToggleVisibility,
                enabled = canBeVisible || policy.visible,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isVisible) PremiumBrandAccent else PremiumZinc400,
                    )
                    Text(
                        text = if (isVisible) "Visível" else "Oculta",
                        color = if (isVisible) PremiumBrandAccent else PremiumZinc400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                }
            }
        }

        OutlinedTextField(
            value = policy.content,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 144.dp),
            placeholder = {
                Text(
                    text = policy.placeholder,
                    color = PremiumZinc700,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            minLines = 5,
            maxLines = 10,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedBorderColor = Color(0xFF60A5FA),
                unfocusedBorderColor = PremiumZinc800,
                cursorColor = Color(0xFF60A5FA),
            ),
        )
    }
}
