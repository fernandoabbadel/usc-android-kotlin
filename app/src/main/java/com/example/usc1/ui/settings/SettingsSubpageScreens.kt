package com.example.usc1.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.BuildConfig
import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.session.AuthUser
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.session.UserStatus
import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.tenant.TenantMembershipStatus
import com.example.usc1.core.tenant.TenantPalette
import com.example.usc1.domain.model.AdminMentorshipLabelsConfig
import com.example.usc1.domain.model.SettingsInviteActivity
import com.example.usc1.domain.model.SettingsInviteApprovalStatus
import com.example.usc1.domain.model.SettingsInviteDashboard
import com.example.usc1.domain.model.SettingsInviteEntry
import com.example.usc1.domain.model.SettingsMentorshipDirection
import com.example.usc1.domain.model.SettingsMentorshipHub
import com.example.usc1.domain.model.SettingsMentorshipRequest
import com.example.usc1.domain.model.SettingsMentorshipRoleCard
import com.example.usc1.domain.model.SettingsMentorshipStatus
import com.example.usc1.domain.model.SettingsMentorshipUser
import com.example.usc1.ui.theme.UscTheme

@Composable
fun SettingsInvitesScreen(
    state: SettingsUiState,
    session: UserSession,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onCopyInviteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = state.tenantPalette.settingsAccent()
    val tenantName = session.tenant?.name.orEmpty().ifBlank { state.tenantName.ifBlank { "sua atlética" } }
    val canCreate = session.user != null &&
        session.tenant?.membershipStatus == TenantMembershipStatus.Approved &&
        session.user.role != UserRole.Guest

    SettingsSubpageScaffold(
        title = "Meus Convites",
        subtitle = "Links gerados para entrada na atlética",
        icon = Icons.Outlined.PersonAdd,
        accent = accent,
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        SettingsNoticeCard(
            title = "TRAZER AMIGO",
            body = "Cada link é de uso único, expira em 72h e leva o convidado para o cadastro da $tenantName.",
            icon = Icons.AutoMirrored.Outlined.Send,
            accent = Color(0xFFFBBF24),
        )

        SettingsStatsRow(
            stats = listOf(
                SettingsMiniStat("Hoje", state.inviteDashboard.totalCreatedToday.toString()),
                SettingsMiniStat("Restantes", state.inviteDashboard.remainingToday.toString()),
                SettingsMiniStat("Ativos", state.inviteDashboard.activeCount.toString()),
            ),
            accent = accent,
        )

        if (!canCreate) {
            SettingsNoticeCard(
                title = "CONVITES BLOQUEADOS",
                body = "Seu perfil precisa estar aprovado na atlética para gerar novos links.",
                icon = Icons.Outlined.Close,
                accent = Color(0xFFF87171),
            )
        }

        SettingsSubpageToolbar(
            title = "Links recentes",
            subtitle = "${state.inviteDashboard.entries.size} encontrados",
            actionLabel = "Atualizar",
            actionIcon = Icons.Outlined.Refresh,
            accent = accent,
            isLoading = state.isInviteDashboardLoading,
            onActionClick = onRefreshClick,
        )

        if (state.inviteDashboardError.isNotBlank()) {
            SettingsErrorCard(message = state.inviteDashboardError, accent = Color(0xFFF87171))
        }

        if (state.isInviteDashboardLoading && state.inviteDashboard.entries.isEmpty()) {
            SettingsLoadingCard(accent = accent)
        } else if (state.inviteDashboard.entries.isEmpty()) {
            SettingsEmptyCard(
                title = "Nenhum convite carregado",
                body = "Quando o Supabase responder, os links criados por você aparecem aqui com status, validade e uso.",
                accent = accent,
            )
        } else {
            state.inviteDashboard.entries.forEach { entry ->
                SettingsInviteEntryCard(
                    entry = entry,
                    inviteLink = buildInviteLink(session.tenant?.slug.orEmpty(), entry.token),
                    accent = accent,
                    onCopyClick = onCopyInviteClick,
                )
            }
        }
    }
}

@Composable
fun SettingsMentorshipScreen(
    state: SettingsUiState,
    session: UserSession,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = state.tenantPalette.settingsAccent()
    val hub = state.mentorshipHub
    val labels = hub.labels

    SettingsSubpageScaffold(
        title = labels.hubTitle,
        subtitle = "Vínculos, convites recebidos e enviados",
        icon = Icons.Outlined.FavoriteBorder,
        accent = accent,
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        SettingsNoticeCard(
            title = "AMBIENTE SOCIAL",
            body = labels.requestHelpText,
            icon = Icons.Outlined.SupervisorAccount,
            accent = accent,
        )

        SettingsSubpageToolbar(
            title = "Meus vínculos",
            subtitle = session.tenant?.name.orEmpty(),
            actionLabel = "Atualizar",
            actionIcon = Icons.Outlined.Refresh,
            accent = accent,
            isLoading = state.isMentorshipLoading,
            onActionClick = onRefreshClick,
        )

        if (state.mentorshipError.isNotBlank()) {
            SettingsErrorCard(message = state.mentorshipError, accent = Color(0xFFF87171))
        }

        if (state.isMentorshipLoading && !hub.hasVisibleContent()) {
            SettingsLoadingCard(accent = accent)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsRoleSlotCard(
                title = labels.mentorLabel,
                role = hub.mentor,
                emptyText = "Nenhum vínculo ativo",
                accent = accent,
                modifier = Modifier.weight(1f),
            )
            SettingsRoleSlotCard(
                title = labels.menteeLabel,
                role = hub.mentee,
                emptyText = "Nenhum vínculo ativo",
                accent = Color(0xFF60A5FA),
                modifier = Modifier.weight(1f),
            )
        }

        SettingsRequestBlock(
            title = "Recebidos",
            empty = "Nenhum convite recebido.",
            requests = hub.incoming,
            accent = Color(0xFFFBBF24),
        )
        SettingsRequestBlock(
            title = "Enviados",
            empty = "Nenhum convite enviado.",
            requests = hub.outgoing,
            accent = accent,
        )

        SettingsNoticeCard(
            title = "AÇÕES DE APADRINHAMENTO",
            body = "Enviar, aceitar, recusar, cancelar ou remover vínculo altera dados no Supabase. Deixei a leitura pronta; as ações ficam para ativarmos com confirmação.",
            icon = Icons.Outlined.Check,
            accent = Color(0xFF60A5FA),
        )
    }
}

@Composable
private fun SettingsSubpageScaffold(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SettingsBackground),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 448.dp)
                .fillMaxSize(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SettingsBackground.copy(alpha = 0.96f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = SettingsTextMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
                            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title.uppercase(),
                            color = Color.White,
                            fontSize = 18.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            color = SettingsTextDim,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNoticeCard(
    title: String,
    body: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.14f), SettingsPanel, Color.Black),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = accent,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
            )
            Text(
                text = body,
                color = Color(0xFFE4E4E7),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SettingsStatsRow(
    stats: List<SettingsMiniStat>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        stats.forEach { stat ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(SettingsPanel, RoundedCornerShape(18.dp))
                    .border(1.dp, SettingsBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stat.value,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = stat.label.uppercase(),
                    color = accent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SettingsSubpageToolbar(
    title: String,
    subtitle: String,
    actionLabel: String,
    actionIcon: ImageVector,
    accent: Color,
    isLoading: Boolean,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = title.uppercase(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = SettingsTextDim,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Surface(
            onClick = onActionClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(999.dp),
            color = accent.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = accent, strokeWidth = 2.dp)
                } else {
                    Icon(actionIcon, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
                }
                Text(
                    text = actionLabel.uppercase(),
                    color = accent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.9.sp,
                )
            }
        }
    }
}

@Composable
private fun SettingsInviteEntryCard(
    entry: SettingsInviteEntry,
    inviteLink: String,
    accent: Color,
    onCopyClick: (String) -> Unit,
) {
    val activityColor = entry.activity.color(accent)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SettingsPanel, RoundedCornerShape(18.dp))
            .border(1.dp, activityColor.copy(alpha = 0.24f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "CONVITE ${entry.token.take(8).uppercase()}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Criado ${entry.createdAt.ifBlank { "sem data" }}",
                    color = SettingsTextDim,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            SettingsCompactChip(entry.activity.label, activityColor)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCompactChip(entry.approvalStatus.label, entry.approvalStatus.color())
            SettingsCompactChip("${entry.usesCount}/${entry.maxUses} uso", SettingsTextMuted)
        }

        if (entry.requesterName.isNotBlank()) {
            Text(
                text = listOf(entry.requesterName, entry.requesterClass)
                    .filter(String::isNotBlank)
                    .joinToString(" • "),
                color = Color(0xFFE4E4E7),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = inviteLink,
                color = SettingsTextMuted,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                onClick = { onCopyClick(inviteLink) },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copiar convite",
                    tint = accent,
                    modifier = Modifier.padding(8.dp).size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsRoleSlotCard(
    title: String,
    role: SettingsMentorshipRoleCard?,
    emptyText: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SettingsPanel, RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title.uppercase(),
            color = accent,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.9.sp,
        )
        if (role == null) {
            Text(
                text = emptyText,
                color = SettingsTextDim,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            SettingsUserMiniRow(user = role.user, subtitle = role.roleLabel, accent = accent)
        }
    }
}

@Composable
private fun SettingsRequestBlock(
    title: String,
    empty: String,
    requests: List<SettingsMentorshipRequest>,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SettingsPanel, RoundedCornerShape(18.dp))
            .border(1.dp, SettingsBorder, RoundedCornerShape(18.dp)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.uppercase(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            SettingsCompactChip(requests.size.toString(), accent)
        }
        HorizontalDivider(color = SettingsBorder)
        if (requests.isEmpty()) {
            Text(
                text = empty,
                color = SettingsTextDim,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(14.dp),
            )
        } else {
            requests.forEachIndexed { index, request ->
                SettingsMentorshipRequestRow(request = request, accent = accent)
                if (index != requests.lastIndex) HorizontalDivider(color = SettingsBorder)
            }
        }
    }
}

@Composable
private fun SettingsMentorshipRequestRow(
    request: SettingsMentorshipRequest,
    accent: Color,
) {
    Row(
        modifier = Modifier.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsUserAvatar(user = request.otherUser, accent = accent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = request.otherUser?.name?.ifBlank { "Usuário USC" } ?: "Usuário USC",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(
                    request.roleLabel,
                    request.otherUser?.classCode.orEmpty(),
                    request.createdAt,
                ).filter(String::isNotBlank).joinToString(" • "),
                color = SettingsTextDim,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SettingsCompactChip(request.status.label, request.status.color())
    }
}

@Composable
private fun SettingsUserMiniRow(
    user: SettingsMentorshipUser,
    subtitle: String,
    accent: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsUserAvatar(user = user, accent = accent)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = user.name.ifBlank { "Usuário USC" },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(subtitle, user.classCode).filter(String::isNotBlank).joinToString(" • "),
                color = SettingsTextDim,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SettingsUserAvatar(
    user: SettingsMentorshipUser?,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(accent.copy(alpha = 0.14f), CircleShape)
            .border(1.dp, accent.copy(alpha = 0.32f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!user?.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = user?.photoUrl,
                contentDescription = user?.name,
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.Black, CircleShape),
            )
        } else {
            Text(
                text = user?.name?.initials().orEmpty().ifBlank { "US" },
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun SettingsCompactChip(
    label: String,
    color: Color,
) {
    Text(
        text = label.uppercase(),
        color = color,
        fontSize = 8.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        maxLines = 1,
    )
}

@Composable
private fun SettingsLoadingCard(accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SettingsPanel, RoundedCornerShape(18.dp))
            .border(1.dp, SettingsBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accent, strokeWidth = 2.dp)
        Text(
            text = "Carregando dados...",
            color = SettingsTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SettingsEmptyCard(
    title: String,
    body: String,
    accent: Color,
) {
    SettingsNoticeCard(
        title = title.uppercase(),
        body = body,
        icon = Icons.Outlined.Schedule,
        accent = accent,
    )
}

@Composable
private fun SettingsErrorCard(
    message: String,
    accent: Color,
) {
    SettingsNoticeCard(
        title = "ERRO AO CARREGAR",
        body = message,
        icon = Icons.Outlined.Close,
        accent = accent,
    )
}

private data class SettingsMiniStat(
    val label: String,
    val value: String,
)

private fun SettingsInviteActivity.color(accent: Color): Color = when (this) {
    SettingsInviteActivity.Active -> accent
    SettingsInviteActivity.Expired -> SettingsTextMuted
    SettingsInviteActivity.Revoked -> Color(0xFFF87171)
    SettingsInviteActivity.Closed -> Color(0xFFA1A1AA)
}

private fun SettingsInviteApprovalStatus.color(): Color = when (this) {
    SettingsInviteApprovalStatus.Approved -> Color(0xFF34D399)
    SettingsInviteApprovalStatus.Pending -> Color(0xFFFBBF24)
    SettingsInviteApprovalStatus.Rejected -> Color(0xFFF87171)
    SettingsInviteApprovalStatus.Unused -> SettingsTextMuted
}

private fun SettingsMentorshipStatus.color(): Color = when (this) {
    SettingsMentorshipStatus.Pending -> Color(0xFFFBBF24)
    SettingsMentorshipStatus.Accepted -> Color(0xFF34D399)
    SettingsMentorshipStatus.Rejected -> Color(0xFFF87171)
    SettingsMentorshipStatus.Cancelled -> SettingsTextMuted
}

private fun SettingsMentorshipHub.hasVisibleContent(): Boolean {
    return mentor != null || mentee != null || incoming.isNotEmpty() || outgoing.isNotEmpty()
}

private fun buildInviteLink(
    tenantSlug: String,
    token: String,
): String {
    val baseUrl = BuildConfig.WEB_APP_URL.trimEnd('/').ifBlank { "https://usc-atleticas.vercel.app" }
    val tenantPath = tenantSlug.trim().takeIf(String::isNotBlank)?.let { "/$it" }.orEmpty()
    return "$baseUrl$tenantPath/cadastro?convite=$token"
}

@Preview(showBackground = true, backgroundColor = 0xFF050505, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsInvitesScreenPreview() {
    val session = previewSettingsSession()
    UscTheme(darkTheme = true) {
        SettingsInvitesScreen(
            state = SettingsUiState(
                tenantName = "Atlética Demo USC",
                tenantPalette = TenantPalette.Green,
                inviteDashboard = SettingsInviteDashboard(
                    entries = listOf(
                        SettingsInviteEntry(
                            id = "1",
                            token = "abc12345",
                            createdAt = "28/07/2026 22:10",
                            expiresAt = "31/07/2026 22:10",
                            usesCount = 0,
                            maxUses = 1,
                        ),
                    ),
                    totalCreatedToday = 1,
                    remainingToday = 4,
                ),
            ),
            session = session,
            onBackClick = {},
            onRefreshClick = {},
            onCopyInviteClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050505, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsMentorshipScreenPreview() {
    val session = previewSettingsSession()
    UscTheme(darkTheme = true) {
        SettingsMentorshipScreen(
            state = SettingsUiState(
                tenantPalette = TenantPalette.Green,
                mentorshipHub = SettingsMentorshipHub(
                    labels = AdminMentorshipLabelsConfig(),
                    mentor = SettingsMentorshipRoleCard(
                        relationshipId = "m1",
                        user = SettingsMentorshipUser("2", "Renata Dahe", "T1", ""),
                        roleLabel = "Madrinha",
                    ),
                    incoming = listOf(
                        SettingsMentorshipRequest(
                            id = "r1",
                            otherUser = SettingsMentorshipUser("3", "Pietro Natal", "T2", ""),
                            status = SettingsMentorshipStatus.Pending,
                            direction = SettingsMentorshipDirection.Incoming,
                            roleLabel = "Afilhado",
                            createdAt = "28/07/2026 21:45",
                        ),
                    ),
                ),
            ),
            session = session,
            onBackClick = {},
            onRefreshClick = {},
        )
    }
}

private fun previewSettingsSession(): UserSession {
    return UserSession(
        user = AuthUser(
            id = "user_1",
            name = "Fernando Lopes Abbade",
            email = "fernando@example.com",
            role = UserRole.MasterTenant,
            status = UserStatus.Ativo,
        ),
        tenant = TenantContext(
            id = "tenant_1",
            slug = "aaakn",
            name = "Atlética Demo USC",
            acronym = "USC",
            palette = TenantPalette.Green,
            membershipStatus = TenantMembershipStatus.Approved,
        ),
    )
}
