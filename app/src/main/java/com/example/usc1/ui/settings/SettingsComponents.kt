package com.example.usc1.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.tenant.TenantPalette

internal val SettingsBackground = Color(0xFF050505)
internal val SettingsPanel = Color(0xFF18181B)
internal val SettingsBorder = Color(0xFF27272A)
internal val SettingsTextMuted = Color(0xFFA1A1AA)
internal val SettingsTextDim = Color(0xFF71717A)

internal fun TenantPalette?.settingsAccent(): Color = when (this) {
    TenantPalette.Yellow -> Color(0xFFF59E0B)
    TenantPalette.Red -> Color(0xFFEF4444)
    TenantPalette.Blue -> Color(0xFF3B82F6)
    TenantPalette.Orange -> Color(0xFFF97316)
    TenantPalette.Purple -> Color(0xFF8B5CF6)
    TenantPalette.Pink -> Color(0xFFEC4899)
    TenantPalette.Green, null -> Color(0xFF10B981)
}

@Composable
internal fun SettingsStickyHeader(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SettingsBackground.copy(alpha = 0.96f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Voltar",
                    tint = SettingsTextMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = "CENTRAL DO SÓCIO",
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-0.4).sp,
            )
        }
    }
}

@Composable
internal fun SettingsProfileCard(
    state: SettingsUiState,
    onMembershipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = state.tenantPalette.settingsAccent()
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(SettingsPanel, Color.Black),
                ),
                shape = shape,
            )
            .border(1.dp, SettingsBorder, shape)
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(108.dp)
                .background(accent.copy(alpha = 0.06f), CircleShape),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(
                avatarUrl = state.avatarUrl,
                initials = state.userInitials,
                classLogoUrl = state.classLogoUrl,
                classLabel = state.classLabel,
                accent = accent,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = state.userName.ifBlank { "Usuário USC" },
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val identityLine = listOf(state.roleLabel, state.classLabel)
                    .filter(String::isNotBlank)
                    .joinToString(" • ")
                    .ifBlank { state.userEmail }
                if (identityLine.isNotBlank()) {
                    Text(
                        text = identityLine,
                        color = SettingsTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.planLabel.isNotBlank()) {
                        SettingsBadge(
                            label = state.planLabel,
                            color = planColor(state.planColorKey, accent),
                            icon = Icons.Outlined.WorkspacePremium,
                        )
                    }
                    if (state.statusLabel.isNotBlank()) {
                        SettingsBadge(
                            label = state.statusLabel,
                            color = if (state.isAccountActive) Color(0xFF10B981) else Color(0xFFEF4444),
                        )
                    }
                }

                Surface(
                    onClick = onMembershipClick,
                    shape = RoundedCornerShape(9.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CreditCard,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "ABRIR CARTEIRINHA",
                            color = Color(0xFFD4D4D8),
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    initials: String,
    classLogoUrl: String?,
    classLabel: String,
    accent: Color,
) {
    Box(modifier = Modifier.size(84.dp)) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f))),
                    shape = CircleShape,
                )
                .padding(4.dp)
                .background(SettingsBackground, CircleShape)
                .padding(4.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Foto do perfil",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = initials.ifBlank { "US" },
                    color = accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        if (!classLogoUrl.isNullOrBlank() || classLabel.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .size(32.dp)
                    .background(SettingsBackground, CircleShape)
                    .padding(3.dp)
                    .background(Color(0xFF09090B), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (!classLogoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = classLogoUrl,
                        contentDescription = "Logo da turma $classLabel",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = classLabel.take(2).uppercase(),
                        color = accent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SettingsInvitePanel(
    state: SettingsInviteUiModel,
    tenantName: String,
    onCreateInviteClick: () -> Unit,
    onOpenInvitesClick: () -> Unit,
    onCopyInviteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isVisible) return

    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF78350F).copy(alpha = 0.42f),
                        Color(0xFF0A0A0A),
                        Color(0xFF78350F).copy(alpha = 0.24f),
                    ),
                ),
                shape = shape,
            )
            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.25f), shape)
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFBBF24).copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            Color(0xFFFDE68A).copy(alpha = 0.30f),
                            RoundedCornerShape(16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFDE68A),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TRAZER AMIGO",
                        color = Color(0xFFFCD34D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.2.sp,
                    )
                    Text(
                        text = "TRAGA MAIS GENTE PARA ${tenantName.ifBlank { "SUA ATLÉTICA" }.uppercase()}",
                        color = Color.White,
                        fontSize = 17.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Gera 1 link para convidar amigos, de uso único e validade de 72h.",
                        color = Color(0xFFFDE68A).copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }

            Surface(
                onClick = onCreateInviteClick,
                enabled = state.canCreate,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFBBF24), Color(0xFFFDE047), Color(0xFFF59E0B)),
                            ),
                        )
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF1B1300),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null,
                            tint = Color(0xFF1B1300),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.isLoading) "GERANDO CONVITE" else "TRAZER AMIGO PARA A ATLÉTICA",
                        color = Color(0xFF1B1300),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                }
            }

            if (state.generatedLink.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            Color(0xFFFDE68A).copy(alpha = 0.15f),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "LINK PRONTO PARA ENVIAR",
                            color = Color(0xFFFCD34D),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp,
                        )
                        Surface(
                            onClick = { onCopyInviteClick(state.generatedLink) },
                            shape = CircleShape,
                            color = Color(0xFFFBBF24).copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.20f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (state.isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    tint = Color(0xFFFEF3C7),
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = if (state.isCopied) "COPIADO" else "COPIAR",
                                    color = Color(0xFFFEF3C7),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Text(
                        text = state.generatedLink,
                        color = Color(0xFFE4E4E7),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                    )
                }
            }

            Surface(
                onClick = onOpenInvitesClick,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFBBF24).copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.20f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = Color(0xFFFEF3C7),
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "ABRIR MEUS CONVITES",
                        color = Color(0xFFFEF3C7),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    section: SettingsSectionUiModel,
    notificationsEnabled: Boolean,
    onNotificationChange: (Boolean) -> Unit,
    onItemClick: (SettingsItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleItems = section.items.filter(SettingsItemUiModel::isVisible)
    if (visibleItems.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = section.title.uppercase(),
            color = Color(0xFF52525B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SettingsPanel, RoundedCornerShape(16.dp))
                .border(1.dp, SettingsBorder, RoundedCornerShape(16.dp)),
        ) {
            visibleItems.forEachIndexed { index, item ->
                if (item.action == SettingsAction.Notifications) {
                    SettingsNotificationItem(
                        item = item,
                        checked = notificationsEnabled,
                        onCheckedChange = onNotificationChange,
                    )
                } else {
                    SettingsItem(
                        item = item,
                        onClick = { onItemClick(item) },
                    )
                }
                if (index != visibleItems.lastIndex) {
                    HorizontalDivider(color = SettingsBorder)
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    item: SettingsItemUiModel,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.isEnabled, onClick = onClick)
            .alpha(if (item.isEnabled) 1f else 0.60f)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconFor(item.action),
            contentDescription = null,
            tint = SettingsTextMuted,
            modifier = Modifier.size(18.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = item.title,
                color = Color(0xFFE4E4E7),
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    color = SettingsTextDim,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
        item.badge?.takeIf(String::isNotBlank)?.let { badge ->
            SettingsBadge(
                label = badge,
                color = if (item.isEnabled) Color(0xFF34D399) else SettingsTextMuted,
            )
        }
        if (item.isEnabled) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF52525B),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SettingsNotificationItem(
    item: SettingsItemUiModel,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = null,
            tint = SettingsTextMuted,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = item.title,
            color = Color(0xFFE4E4E7),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        )
        CompactSettingsSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun CompactSettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF10B981) else Color(0xFF3F3F46),
        label = "settings-switch-color",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 4.dp,
        label = "settings-switch-position",
    )
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 20.dp)
            .background(trackColor, CircleShape)
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 4.dp)
                .size(12.dp)
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
private fun SettingsBadge(
    label: String,
    color: Color,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(5.dp))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(10.dp),
            )
        }
        Text(
            text = label.uppercase(),
            color = color,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
        )
    }
}

@Composable
internal fun SettingsRiskZone(
    state: SettingsUiState,
    onToggleAccountClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = Color(0xFF18181B),
                shape = RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp),
            )
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = Color(0xFFEF4444).copy(alpha = 0.55f),
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "ZONA DE RISCO",
                color = Color(0xFFEF4444).copy(alpha = 0.55f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp,
            )
        }

        SettingsRiskButton(
            label = if (state.isAccountActive) "Pausar Conta" else "Reativar Conta",
            icon = Icons.Outlined.PowerSettingsNew,
            color = if (state.isAccountActive) Color(0xFFEAB308) else Color(0xFF10B981),
            onClick = onToggleAccountClick,
            enabled = !state.isAccountActionLoading,
        )
        SettingsRiskButton(
            label = "Sair da Conta",
            icon = Icons.Outlined.Logout,
            color = Color(0xFFD4D4D8),
            onClick = onSignOutClick,
        )
        SettingsRiskButton(
            label = "Excluir Permanentemente",
            icon = Icons.Outlined.DeleteForever,
            color = Color(0xFFEF4444).copy(alpha = 0.72f),
            background = Color(0xFF450A0A).copy(alpha = 0.12f),
            border = Color(0xFF7F1D1D).copy(alpha = 0.25f),
            onClick = onDeleteAccountClick,
            enabled = !state.isAccountActionLoading,
        )

        val footer = listOf(
            state.tenantName.trim().takeIf(String::isNotBlank),
            state.userIdLabel.trim().takeIf(String::isNotBlank)?.let { "ID: $it" },
        ).filterNotNull().joinToString(" • ")
        if (footer.isNotBlank()) {
            Text(
                text = footer,
                color = Color(0xFF3F3F46),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun SettingsRiskButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = SettingsPanel,
    border: Color = SettingsBorder,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.60f),
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!enabled) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = color,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label.uppercase(),
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
        }
    }
}

internal fun iconFor(action: SettingsAction): ImageVector = when (action) {
    SettingsAction.Profile -> Icons.Outlined.Description
    SettingsAction.Membership -> Icons.Outlined.CreditCard
    SettingsAction.Security -> Icons.Outlined.Security
    SettingsAction.Orders -> Icons.Outlined.History
    SettingsAction.Plans -> Icons.Outlined.WorkspacePremium
    SettingsAction.TermsPrivacy,
    SettingsAction.Lgpd,
    -> Icons.Outlined.Description
    SettingsAction.Tickets -> Icons.Outlined.CreditCard
    SettingsAction.Community,
    SettingsAction.Leagues,
    SettingsAction.Directory,
    SettingsAction.Commissions,
    SettingsAction.Tenant,
    -> Icons.Outlined.Groups
    SettingsAction.Album,
    SettingsAction.Games,
    SettingsAction.Boardround,
    SettingsAction.Achievements,
    SettingsAction.Loyalty,
    -> Icons.Outlined.Star
    SettingsAction.MiniVendor,
    SettingsAction.SalesMode,
    -> Icons.Outlined.Storefront
    SettingsAction.Scanner -> Icons.Outlined.QrCodeScanner
    SettingsAction.SignOut -> Icons.Outlined.Logout
    SettingsAction.Invites -> Icons.Outlined.PersonAdd
    SettingsAction.Mentorship -> Icons.Outlined.FavoriteBorder
    SettingsAction.Notifications -> Icons.Outlined.Notifications
    SettingsAction.Support -> Icons.Outlined.WarningAmber
    SettingsAction.Guide -> Icons.Outlined.Settings
}

private fun planColor(key: String, tenantAccent: Color): Color = when (key.trim().lowercase()) {
    "emerald", "green", "verde" -> Color(0xFF34D399)
    "amber", "yellow", "amarelo", "gold", "dourado" -> Color(0xFFFBBF24)
    "blue", "azul", "cyan" -> Color(0xFF60A5FA)
    "red", "vermelho" -> Color(0xFFF87171)
    "purple", "roxo", "violet" -> Color(0xFFA78BFA)
    "pink", "rosa" -> Color(0xFFF472B6)
    "brand" -> tenantAccent
    else -> Color(0xFFD4D4D8)
}
