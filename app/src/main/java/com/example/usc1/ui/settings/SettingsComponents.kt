package com.example.usc1.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumMenuRow
import com.example.usc1.core.ui.PremiumRed

@Composable
fun SettingsSection(
    section: SettingsSectionUiModel,
    onItemClick: (SettingsItemUiModel) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        section.items.forEach { item ->
            SettingsItem(
                item = item,
                onClick = { onItemClick(item) },
            )
        }
    }
}

@Composable
fun SettingsItem(
    item: SettingsItemUiModel,
    onClick: () -> Unit,
) {
    val accent = when (item.action) {
        SettingsAction.SignOut -> PremiumRed
        SettingsAction.Plans,
        SettingsAction.Invites,
        -> PremiumAmber
        else -> com.example.usc1.core.ui.PremiumBrand
    }
    PremiumMenuRow(
        title = item.title,
        subtitle = item.description,
        icon = iconFor(item.action),
        accent = accent,
        badge = if (item.action == SettingsAction.Orders) "Novo" else null,
        onClick = onClick,
    )
}

private fun iconFor(action: SettingsAction): ImageVector = when (action) {
    SettingsAction.Profile -> Icons.Outlined.Person
    SettingsAction.Security -> Icons.Outlined.Security
    SettingsAction.Orders,
    SettingsAction.Plans,
    SettingsAction.TermsPrivacy,
    SettingsAction.Lgpd,
    -> Icons.Outlined.CreditCard
    SettingsAction.Tickets -> Icons.Outlined.Event
    SettingsAction.MiniVendor,
    SettingsAction.SalesMode,
    -> Icons.Outlined.Storefront
    SettingsAction.SignOut -> Icons.Outlined.Logout
    SettingsAction.Invites,
    SettingsAction.Support,
    -> Icons.Outlined.Settings
}
