package com.example.usc1.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.usc1.domain.model.AppModuleKey

@Composable
fun ModuleIcon(
    key: AppModuleKey,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = iconFor(key),
        contentDescription = null,
        modifier = modifier,
        tint = MaterialTheme.colorScheme.primary,
    )
}

private fun iconFor(key: AppModuleKey): ImageVector = when (key) {
    AppModuleKey.Dashboard -> Icons.Outlined.Home
    AppModuleKey.Profile -> Icons.Outlined.Person
    AppModuleKey.Settings -> Icons.Outlined.Settings
    AppModuleKey.Store -> Icons.Outlined.Storefront
    AppModuleKey.Events -> Icons.Outlined.Event
    AppModuleKey.Plans,
    AppModuleKey.MembershipCard,
    AppModuleKey.Orders,
    -> Icons.Outlined.CreditCard
    AppModuleKey.Training,
    AppModuleKey.Gym,
    -> Icons.Outlined.FitnessCenter
    AppModuleKey.Community,
    AppModuleKey.Leagues,
    AppModuleKey.Directory,
    AppModuleKey.Commissions,
    -> Icons.Outlined.Groups
    else -> Icons.Outlined.Apps
}
