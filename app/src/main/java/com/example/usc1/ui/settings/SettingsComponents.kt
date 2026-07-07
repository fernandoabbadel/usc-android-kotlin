package com.example.usc1.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.AppSectionHeader

@Composable
fun SettingsSection(
    section: SettingsSectionUiModel,
    onItemClick: (SettingsItemUiModel) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppSectionHeader(title = section.title)
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
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconFor(item.action),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = if (item.action == SettingsAction.SignOut) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
    SettingsAction.Invites,
    SettingsAction.Support,
    SettingsAction.SignOut,
    -> Icons.Outlined.Settings
}
