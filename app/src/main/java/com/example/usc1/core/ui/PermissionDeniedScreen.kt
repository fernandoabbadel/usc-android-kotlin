package com.example.usc1.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun PermissionDeniedScreen(
    title: String,
    subtitle: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = title,
            subtitle = "Permissão bloqueada",
            icon = Icons.Outlined.Security,
            accent = PremiumRed,
        )
        PremiumEmptyState(
            title = "Acesso restrito",
            subtitle = subtitle,
            icon = Icons.Outlined.Security,
            accent = PremiumRed,
        )
    }
}
