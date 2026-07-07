package com.example.usc1.ui.tenant

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.usc1.R

data class TenantIdentity(
    val id: String,
    val name: String,
    val slug: String,
    val subtitle: String,
    val accent: Color,
    @DrawableRes val logoRes: Int,
    @DrawableRes val heroRes: Int,
    val enabledModules: List<String>,
)

data class TenantUiState(
    val currentTenant: TenantIdentity = TenantMockData.tenants.first(),
    val tenants: List<TenantIdentity> = TenantMockData.tenants,
)

object TenantMockData {
    val tenants = listOf(
        TenantIdentity(
            id = "aaakn",
            name = "AAAKN USC",
            slug = "aaakn",
            subtitle = "Atlética oficial • dark premium neon",
            accent = Color(0xFF10B981),
            logoRes = R.drawable.logo_aaakn,
            heroRes = R.drawable.battle_forest,
            enabledModules = listOf("Dashboard", "Eventos", "Loja", "Planos", "Treinos", "Parceiros", "Scanner"),
        ),
        TenantIdentity(
            id = "usc",
            name = "Universidade Spot Connect",
            slug = "usc",
            subtitle = "Plataforma multiatléticas",
            accent = Color(0xFFEAB308),
            logoRes = R.drawable.logo_usc,
            heroRes = R.drawable.logo_usc_wide,
            enabledModules = listOf("Dashboard", "Tenant", "Ligas", "Diretório", "Comissões", "Games"),
        ),
    )
}
