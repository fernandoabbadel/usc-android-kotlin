package com.example.usc1.ui.tenant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.R
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

private val DirectoryAccent = Color(0xFF10B981)

@Composable
fun TenantSwitcherScreen(
    state: TenantUiState,
    onTenantClick: (TenantIdentity) -> Unit,
    onRetryClick: () -> Unit,
    selectionEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 48.dp) {
        PremiumHeader(
            title = "Escolha sua atlética",
            subtitle = "Entrada pública da USC",
            icon = Icons.Outlined.Groups,
            accent = DirectoryAccent,
        )

        PremiumCard(accent = DirectoryAccent) {
            Text(
                text = "PROCURE E ENTRE",
                color = DirectoryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Selecione a página oficial da sua atlética para abrir a Home pública.",
                color = PremiumZinc300,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        when {
            state.isLoading -> DirectoryLoading()
            state.errorMessage != null && state.tenants.isEmpty() -> DirectoryError(
                message = state.errorMessage,
                onRetryClick = onRetryClick,
            )
            else -> {
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                NativeSectionTitle(
                    title = "Atléticas disponíveis • ${state.tenants.size}",
                    accent = DirectoryAccent,
                )
                if (!selectionEnabled) {
                    Text(
                        text = "Seu vínculo atual permanece ativo. A troca de atlética para membros será disponibilizada em um fluxo próprio.",
                        modifier = Modifier.fillMaxWidth(),
                        color = PremiumZinc400,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                state.tenants.forEach { tenant ->
                    TenantRow(
                        tenant = tenant,
                        isSelecting = state.selectingTenantId == tenant.id,
                        enabled = selectionEnabled && !state.isSelecting,
                        onClick = { onTenantClick(tenant) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectoryLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(34.dp),
            color = DirectoryAccent,
            strokeWidth = 3.dp,
        )
        Text(
            text = "Carregando atléticas...",
            color = PremiumZinc400,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DirectoryError(
    message: String,
    onRetryClick: () -> Unit,
) {
    PremiumCard(accent = Color(0xFFEF4444)) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFFCA5A5),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        PremiumSecondaryButton(
            text = "Tentar novamente",
            onClick = onRetryClick,
            accent = DirectoryAccent,
            icon = Icons.Outlined.Refresh,
        )
    }
}

@Composable
private fun TenantRow(
    tenant: TenantIdentity,
    isSelecting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelecting) tenant.accent else PremiumZinc800,
        ),
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(2.dp, tenant.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = tenant.logoUrl,
                    contentDescription = "Logo ${tenant.name}",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.logo_usc),
                    error = painterResource(R.drawable.logo_usc),
                    fallback = painterResource(R.drawable.logo_usc),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = tenant.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tenant.subtitle,
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tenant.slug.uppercase(),
                    color = tenant.accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            if (isSelecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = tenant.accent,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Abrir ${tenant.name}",
                    tint = tenant.accent,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun TenantSwitcherScreenPreview() {
    UscTheme(darkTheme = true) {
        TenantSwitcherScreen(
            state = TenantUiState(
                isLoading = false,
                tenants = listOf(
                    TenantIdentity(
                        id = "preview",
                        name = "Atlética Acadêmica",
                        slug = "atletica",
                        subtitle = "Universidade • Curso",
                        accent = DirectoryAccent,
                    ),
                ),
            ),
            onTenantClick = {},
            onRetryClick = {},
        )
    }
}
