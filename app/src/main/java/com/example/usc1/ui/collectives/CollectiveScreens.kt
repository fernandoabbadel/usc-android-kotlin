package com.example.usc1.ui.collectives

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.usc1.core.ui.NativeAction
import com.example.usc1.core.ui.NativeActionCard
import com.example.usc1.core.ui.NativeModuleHeroCard
import com.example.usc1.core.ui.NativeSectionTitle
import com.example.usc1.core.ui.NativeStatCard
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.ui.theme.UscTheme

@Composable
fun LeaguesScreen(
    state: LeagueUiState,
    onLeagueClick: (CollectiveGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectiveListScreen(
        title = "Ligas",
        subtitle = "Membros, agenda, loja e eventos",
        groups = state.leagues,
        onGroupClick = onLeagueClick,
        card = { group, onClick -> LeagueCard(league = group, onClick = onClick) },
        modifier = modifier,
    )
}

@Composable
fun LeagueDetailScreen(
    league: CollectiveGroup,
    onMembersClick: () -> Unit,
    onAgendaClick: () -> Unit,
    onStoreClick: () -> Unit,
    onEventsClick: () -> Unit,
    onInfoClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectiveDetailScreen(
        group = league,
        onMembersClick = onMembersClick,
        onAgendaClick = onAgendaClick,
        onStoreClick = onStoreClick,
        onEventsClick = onEventsClick,
        onInfoClick = onInfoClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
fun LeagueMembersScreen(league: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveMembersScreen(group = league, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun LeagueAgendaScreen(league: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveAgendaScreen(group = league, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun LeagueStoreScreen(league: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveStoreScreen(group = league, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun LeagueEventsScreen(league: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveEventsScreen(group = league, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun LeagueInfoScreen(league: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveInfoScreen(group = league, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun DirectoryScreen(
    state: DirectoryUiState,
    onDirectoryClick: (CollectiveGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectiveListScreen(
        title = "Diretório",
        subtitle = "Gestão, agenda e loja institucional",
        groups = state.directories,
        onGroupClick = onDirectoryClick,
        card = { group, onClick -> DirectoryCard(directory = group, onClick = onClick) },
        modifier = modifier,
    )
}

@Composable
fun DirectoryDetailScreen(
    directory: CollectiveGroup,
    onMembersClick: () -> Unit,
    onAgendaClick: () -> Unit,
    onStoreClick: () -> Unit,
    onEventsClick: () -> Unit,
    onInfoClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectiveDetailScreen(
        group = directory,
        onMembersClick = onMembersClick,
        onAgendaClick = onAgendaClick,
        onStoreClick = onStoreClick,
        onEventsClick = onEventsClick,
        onInfoClick = onInfoClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
fun DirectoryMembersScreen(directory: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveMembersScreen(group = directory, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun DirectoryAgendaScreen(directory: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveAgendaScreen(group = directory, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun DirectoryStoreScreen(directory: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveStoreScreen(group = directory, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun DirectoryEventsScreen(directory: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveEventsScreen(group = directory, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun DirectoryInfoScreen(directory: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveInfoScreen(group = directory, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun CommissionsScreen(
    state: CommissionUiState,
    onCommissionClick: (CollectiveGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectiveListScreen(
        title = "Comissões",
        subtitle = "Operação, eventos e comunicação",
        groups = state.commissions,
        onGroupClick = onCommissionClick,
        card = { group, onClick -> CommissionCard(commission = group, onClick = onClick) },
        modifier = modifier,
    )
}

@Composable
fun CommissionDetailScreen(
    commission: CollectiveGroup,
    onMembersClick: () -> Unit,
    onAgendaClick: () -> Unit,
    onStoreClick: () -> Unit,
    onEventsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectiveDetailScreen(
        group = commission,
        onMembersClick = onMembersClick,
        onAgendaClick = onAgendaClick,
        onStoreClick = onStoreClick,
        onEventsClick = onEventsClick,
        onInfoClick = null,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
fun CommissionMembersScreen(commission: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveMembersScreen(group = commission, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun CommissionAgendaScreen(commission: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveAgendaScreen(group = commission, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun CommissionStoreScreen(commission: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveStoreScreen(group = commission, onBackClick = onBackClick, modifier = modifier)
}

@Composable
fun CommissionEventsScreen(commission: CollectiveGroup, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    CollectiveEventsScreen(group = commission, onBackClick = onBackClick, modifier = modifier)
}

@Composable
private fun CollectiveListScreen(
    title: String,
    subtitle: String,
    groups: List<CollectiveGroup>,
    onGroupClick: (CollectiveGroup) -> Unit,
    card: @Composable (CollectiveGroup, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = title, subtitle = subtitle, icon = Icons.Outlined.Groups)
        groups.forEach { group ->
            card(group) { onGroupClick(group) }
        }
    }
}

@Composable
private fun CollectiveDetailScreen(
    group: CollectiveGroup,
    onMembersClick: () -> Unit,
    onAgendaClick: () -> Unit,
    onStoreClick: () -> Unit,
    onEventsClick: () -> Unit,
    onInfoClick: (() -> Unit)?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = collectiveAccent(group)
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = group.name,
            subtitle = group.kind.label,
            icon = Icons.Outlined.Groups,
            accent = accent,
            onBackClick = onBackClick,
        )
        NativeModuleHeroCard(
            title = group.name,
            subtitle = group.subtitle,
            body = group.description,
            imageRes = group.imageRes,
            accent = accent,
            status = group.status,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NativeStatCard(
                label = "Membros",
                value = "${group.memberCount}",
                icon = Icons.Outlined.Groups,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
            NativeStatCard(
                label = "Agenda",
                value = "${group.agenda.size}",
                icon = Icons.Outlined.CalendarMonth,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
        }
        NativeActionCard(
            action = NativeAction("Membros", "Diretoria, operação e participantes.", Icons.Outlined.Person, accent),
            onClick = onMembersClick,
        )
        NativeActionCard(
            action = NativeAction("Agenda", "Reuniões, tarefas e encontros.", Icons.Outlined.CalendarMonth, accent),
            onClick = onAgendaClick,
        )
        NativeActionCard(
            action = NativeAction("Loja", "Produtos e kits do coletivo.", Icons.Outlined.Storefront, accent),
            onClick = onStoreClick,
        )
        NativeActionCard(
            action = NativeAction("Eventos", "Inscrições e calendário público.", Icons.Outlined.Event, accent),
            onClick = onEventsClick,
        )
        if (onInfoClick != null) {
            NativeActionCard(
                action = NativeAction("Informações", "Regras, descrição e identidade.", Icons.Outlined.Info, accent),
                onClick = onInfoClick,
            )
        }
    }
}

@Composable
private fun CollectiveMembersScreen(
    group: CollectiveGroup,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = collectiveAccent(group)
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Membros", subtitle = group.name, icon = Icons.Outlined.Groups, accent = accent, onBackClick = onBackClick)
        group.members.forEach { member ->
            CollectiveInfoRow(member.name, "${member.role} • ${member.status}", accent = accent)
        }
    }
}

@Composable
private fun CollectiveAgendaScreen(
    group: CollectiveGroup,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = collectiveAccent(group)
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Agenda", subtitle = group.name, icon = Icons.Outlined.CalendarMonth, accent = accent, onBackClick = onBackClick)
        group.agenda.forEach { item ->
            CollectiveInfoRow(item.title, "${item.dateLabel} • ${item.place}", accent = accent)
        }
    }
}

@Composable
private fun CollectiveStoreScreen(
    group: CollectiveGroup,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = collectiveAccent(group)
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Loja", subtitle = group.name, icon = Icons.Outlined.Storefront, accent = accent, onBackClick = onBackClick)
        group.store.forEach { item ->
            CollectiveInfoRow(item.name, "${item.priceLabel} • ${item.status}", accent = accent)
        }
    }
}

@Composable
private fun CollectiveEventsScreen(
    group: CollectiveGroup,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = collectiveAccent(group)
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Eventos", subtitle = group.name, icon = Icons.Outlined.Event, accent = accent, onBackClick = onBackClick)
        group.events.forEach { event ->
            CollectiveInfoRow(event.title, "${event.dateLabel} • ${event.status}", accent = accent)
        }
    }
}

@Composable
private fun CollectiveInfoScreen(
    group: CollectiveGroup,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = collectiveAccent(group)
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Informações", subtitle = group.name, icon = Icons.Outlined.Info, accent = accent, onBackClick = onBackClick)
        NativeModuleHeroCard(
            title = group.name,
            subtitle = group.accentName,
            body = group.description,
            imageRes = group.imageRes,
            accent = accent,
            status = group.status,
        )
        PremiumInfoRow("Tipo", group.kind.label, accent = accent)
        PremiumInfoRow("Membros", "${group.memberCount}", accent = accent)
        PremiumInfoRow("Status", group.status, accent = accent)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun LeaguesScreenPreview() {
    UscTheme(darkTheme = true) {
        LeaguesScreen(state = LeagueUiState(), onLeagueClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun LeagueDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        LeagueDetailScreen(
            league = CollectiveMockData.leagues.first(),
            onMembersClick = {},
            onAgendaClick = {},
            onStoreClick = {},
            onEventsClick = {},
            onInfoClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun DirectoryScreenPreview() {
    UscTheme(darkTheme = true) {
        DirectoryScreen(state = DirectoryUiState(), onDirectoryClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun DirectoryDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        DirectoryDetailScreen(
            directory = CollectiveMockData.directories.first(),
            onMembersClick = {},
            onAgendaClick = {},
            onStoreClick = {},
            onEventsClick = {},
            onInfoClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun CommissionsScreenPreview() {
    UscTheme(darkTheme = true) {
        CommissionsScreen(state = CommissionUiState(), onCommissionClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505)
@Composable
fun CommissionDetailScreenPreview() {
    UscTheme(darkTheme = true) {
        CommissionDetailScreen(
            commission = CollectiveMockData.commissions.first(),
            onMembersClick = {},
            onAgendaClick = {},
            onStoreClick = {},
            onEventsClick = {},
            onBackClick = {},
        )
    }
}
