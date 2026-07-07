package com.example.usc1.ui.tenant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Palette
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.NativeModuleHeroCard
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.theme.UscTheme

@Composable
fun TenantSwitcherScreen(
    state: TenantUiState,
    onTenantClick: (TenantIdentity) -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Atlética",
            subtitle = "Identidade, módulos e tenant atual",
            icon = Icons.Outlined.Groups,
            accent = state.currentTenant.accent,
        )
        TenantIdentityHeader(tenant = state.currentTenant)
        TenantThemePreviewCard(tenant = state.currentTenant)
        NativeSectionTitle(title = "Tenants disponíveis", accent = state.currentTenant.accent)
        state.tenants.forEach { tenant ->
            TenantRow(
                tenant = tenant,
                selected = tenant.id == state.currentTenant.id,
                onClick = { onTenantClick(tenant) },
            )
        }
    }
}

@Composable
fun TenantIdentityHeader(
    tenant: TenantIdentity,
    modifier: Modifier = Modifier,
) {
    NativeModuleHeroCard(
        title = tenant.name,
        subtitle = tenant.slug,
        body = tenant.subtitle,
        imageRes = tenant.heroRes,
        accent = tenant.accent,
        status = "Tenant atual",
        modifier = modifier,
    )
}

@Composable
fun TenantThemePreviewCard(
    tenant: TenantIdentity,
    modifier: Modifier = Modifier,
) {
    PremiumCard(modifier = modifier, accent = tenant.accent) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = Color.Black,
                border = BorderStroke(2.dp, tenant.accent),
            ) {
                Image(
                    painter = painterResource(id = tenant.logoRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = tenant.name,
                    color = Color.White,
                    fontSize = 21.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tenant.enabledModules.joinToString(" • "),
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumChip(label = "Logo real", icon = Icons.Outlined.CheckCircle, accent = tenant.accent)
            PremiumChip(label = "Cores mockadas", icon = Icons.Outlined.Palette, accent = tenant.accent)
        }
    }
}

@Composable
private fun TenantRow(
    tenant: TenantIdentity,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, if (selected) tenant.accent else PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(2.dp, tenant.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = tenant.logoRes),
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = tenant.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(text = tenant.subtitle, color = PremiumZinc500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (selected) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = tenant.accent)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun TenantSwitcherScreenPreview() {
    UscTheme(darkTheme = true) {
        TenantSwitcherScreen(
            state = TenantUiState(),
            onTenantClick = {},
        )
    }
}
