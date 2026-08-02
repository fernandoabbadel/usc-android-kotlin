package com.example.usc1.ui.collectives.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.collectiveAccent

/**
 * Componentes compartilhados do painel de gestão dos coletivos.
 *
 * Web: cabeçalho e `LeagueAdminQuickNav` de `app/ligas/LigasAdminPageContent.tsx`,
 * reaproveitados por loja, gestão, frequência e extrato.
 */

/** `renderLeagueHeaderIdentity` + botão de voltar/sair do painel. */
@Composable
fun CollectiveManagementHeader(
    collective: ManagedCollective,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String = "Painel de Gestão",
    onExitClick: (() -> Unit)? = null,
) {
    val accent = collectiveAccent(collective.kind)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onBackClick),
            shape = RoundedCornerShape(12.dp),
            color = Color.Black,
            border = BorderStroke(1.dp, PremiumZinc800),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = collective.kind.managementBackLabel,
                modifier = Modifier.padding(10.dp),
                tint = PremiumZinc400,
            )
        }

        CollectiveLogo(collective = collective, accent = accent)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(),
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
            Text(
                text = collective.headerTitle.uppercase(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (collective.headerSubtitle.isNotBlank()) {
                Text(
                    text = collective.headerSubtitle,
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (onExitClick != null) {
            Surface(
                modifier = Modifier
                    .size(38.dp)
                    .clickable(onClick = onExitClick),
                shape = RoundedCornerShape(10.dp),
                color = PremiumRed.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, PremiumRed.copy(alpha = 0.30f)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LayersClear,
                    contentDescription = "Encerrar sessão do painel",
                    modifier = Modifier.padding(9.dp),
                    tint = PremiumRed,
                )
            }
        }
    }
}

@Composable
private fun CollectiveLogo(
    collective: ManagedCollective,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        val logo = collective.logoUrl
        if (logo.isNullOrBlank()) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    modifier = Modifier.padding(11.dp),
                    tint = accent,
                )
            }
        } else {
            AsyncImage(
                model = logo,
                contentDescription = collective.headerTitle,
                modifier = Modifier.size(44.dp),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** `LeagueAdminQuickNav` do web: Board Round só aparece na liga. */
@Composable
fun CollectiveManagementQuickNav(
    kind: CollectiveKind,
    active: CollectiveManagementNav,
    onNavClick: (CollectiveManagementNav) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = CollectiveManagementNav.entries.filter {
        it != CollectiveManagementNav.Board || kind.showsBoardRound
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = PremiumZinc900.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { item ->
                CollectiveNavChip(
                    item = item,
                    isActive = item == active,
                    onClick = { onNavClick(item) },
                )
            }
        }
    }
}

@Composable
private fun CollectiveNavChip(
    item: CollectiveManagementNav,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val accent = PremiumBrand

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) accent.copy(alpha = 0.15f) else PremiumZinc900,
        border = BorderStroke(1.dp, if (isActive) accent.copy(alpha = 0.40f) else PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isActive) accent else PremiumZinc400,
            )
            Text(
                text = item.label.uppercase(),
                color = if (isActive) accent else PremiumZinc400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
                maxLines = 1,
            )
        }
    }
}

private val CollectiveManagementNav.icon: ImageVector
    get() = when (this) {
        CollectiveManagementNav.Home -> Icons.Outlined.Home
        CollectiveManagementNav.Info -> Icons.Outlined.Info
        CollectiveManagementNav.Members -> Icons.Outlined.Groups
        CollectiveManagementNav.Agenda -> Icons.Outlined.CalendarMonth
        CollectiveManagementNav.Store -> Icons.Outlined.Storefront
        CollectiveManagementNav.Finance -> Icons.Outlined.Wallet
        CollectiveManagementNav.Board -> Icons.Outlined.LayersClear
    }

/** `MetricCard` do `LeagueFinanceDashboard`. */
@Composable
fun CollectiveMetricCard(
    label: String,
    value: String,
    hint: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
) {
    PremiumCard(modifier = modifier, accent = accent, borderAlpha = 0.22f) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = accent)
        }
        Text(
            text = value,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
        )
        Text(text = hint, color = PremiumZinc500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Card de atalho do hub e da gestão. */
@Composable
fun CollectiveActionCard(
    eyebrow: String,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
    description: String = "",
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.40f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = accent)
            Text(
                text = eyebrow.uppercase(),
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
            )
            Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = PremiumZinc500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Linha de rótulo/valor usada nas listas de produto, pedido e extrato. */
@Composable
fun CollectiveDetailLine(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PremiumZinc500,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** Seletor horizontal simples (cargo, filtro de status, filtro de evento). */
@Composable
fun CollectiveOptionRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isActive = option.equals(selected, ignoreCase = true)
            Surface(
                modifier = Modifier.clickable { onSelect(option) },
                shape = RoundedCornerShape(9.dp),
                color = if (isActive) accent.copy(alpha = 0.16f) else PremiumZinc900,
                border = BorderStroke(1.dp, if (isActive) accent.copy(alpha = 0.42f) else PremiumZinc800),
            ) {
                Text(
                    text = option.uppercase(),
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    color = if (isActive) accent else PremiumZinc400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
