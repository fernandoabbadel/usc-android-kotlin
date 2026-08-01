package com.example.usc1.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.BuildConfig
import com.example.usc1.core.roles.UserRole
import com.example.usc1.core.session.UserSession
import com.example.usc1.core.tenant.TenantMembershipStatus
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.SettingsInviteActivity
import com.example.usc1.domain.model.SettingsInviteApprovalStatus
import com.example.usc1.domain.model.SettingsInviteDashboard
import com.example.usc1.domain.model.SettingsInviteEntry
import com.example.usc1.domain.model.SettingsInviteQuotaStatus
import kotlinx.coroutines.delay

/**
 * `/configuracoes/convites` — convites ainda válidos, cota do dia e pedido de bônus.
 * Aprovados e encerrados ficam em [SettingsInvitesHistoryScreen], como no web.
 */
@Composable
fun SettingsInvitesScreen(
    state: SettingsUiState,
    session: UserSession,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onCopyInviteClick: (String) -> Unit,
    onRevokeInviteClick: (SettingsInviteEntry) -> Unit,
    onRequestMoreClick: () -> Unit,
    onOpenHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tenantName = session.tenant?.name.orEmpty()
        .ifBlank { state.tenantName.ifBlank { "sua atlética" } }
    val dashboard = state.inviteDashboard
    val quota = dashboard.quota
    val canCreate = session.user != null &&
        session.tenant?.membershipStatus == TenantMembershipStatus.Approved &&
        session.user.role != UserRole.Guest

    // A validade e o cooldown do bônus são contagens ao vivo, iguais ao `setInterval` do web.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    PremiumScreen(modifier = modifier, bottomPadding = 40.dp) {
        PremiumHeader(
            title = "Convites ativos",
            subtitle = "Links válidos de $tenantName",
            icon = Icons.Outlined.PersonAdd,
            accent = PremiumAmber,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumAmber) {
            Text(
                text = "Convite travado na tenant",
                color = PremiumAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
            )
            Text(
                text = "Todo link desta tela cai direto em $tenantName. Quem usar um convite daqui " +
                    "já entra com o cadastro preso nesta tenant, sem poder trocar de atlética no fluxo.",
                color = PremiumZinc400,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            )

            PremiumInfoRow(
                label = "Cota de hoje",
                value = "${dashboard.remainingToday}/${quota.totalLimit}",
                accent = PremiumAmber,
            )
            PremiumInfoRow(
                label = "Gerados hoje",
                value = "${dashboard.totalCreatedToday}",
                accent = PremiumAmber,
            )

            PremiumSecondaryButton(
                text = if (state.isRequestingBonusInvites) "Solicitando" else "Pedir mais convites",
                onClick = onRequestMoreClick,
                enabled = quota.canRequestMore && !state.isRequestingBonusInvites,
                accent = PremiumAmber,
                icon = Icons.Outlined.AddCircleOutline,
            )

            when (quota.status) {
                SettingsInviteQuotaStatus.Pending -> InviteQuotaNotice(
                    title = "Pedido em processamento",
                    body = "Os ${SettingsInviteDashboard.BonusLimit} novos convites liberam em " +
                        formatInviteCooldown(quota.remainingMillis(nowMillis)) + ".",
                    accent = Color(0xFF22D3EE),
                )

                SettingsInviteQuotaStatus.Granted -> InviteQuotaNotice(
                    title = "Bonus liberado",
                    body = "Seus ${SettingsInviteDashboard.BonusLimit} convites extras já estão disponíveis hoje.",
                    accent = PremiumBrand,
                )

                SettingsInviteQuotaStatus.Idle -> Unit
            }
        }

        // Criar convite roda em rota de servidor com service role no web; ver relatório do M4.
        InviteQuotaNotice(
            title = "Gerar novo convite",
            body = if (canCreate) {
                "A geração de link roda no servidor da plataforma e ainda não está disponível no app. " +
                    "Aqui você acompanha, copia e encerra os convites já criados."
            } else {
                "Seu perfil precisa estar aprovado na atlética para gerar novos links."
            },
            accent = if (canCreate) PremiumZinc500 else PremiumRed,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InviteStatTile("Ativos", dashboard.activeCount.toString(), PremiumAmber, Modifier.weight(1f))
            InviteStatTile("Aprovados", dashboard.approvedCount.toString(), PremiumBrand, Modifier.weight(1f))
            InviteStatTile("Histórico", dashboard.historyCount.toString(), PremiumZinc400, Modifier.weight(1f))
        }

        PremiumSecondaryButton(
            text = "Abrir aprovados e expirados",
            onClick = onOpenHistoryClick,
            accent = PremiumAmber,
            icon = Icons.Outlined.History,
        )

        if (state.inviteDashboardError.isNotBlank()) {
            InviteQuotaNotice(
                title = "Erro",
                body = state.inviteDashboardError,
                accent = PremiumRed,
            )
        }

        PremiumSecondaryButton(
            text = if (state.isInviteDashboardLoading) "Atualizando" else "Atualizar",
            onClick = onRefreshClick,
            enabled = !state.isInviteDashboardLoading,
            accent = PremiumZinc400,
            icon = Icons.Outlined.Schedule,
        )

        when {
            state.isInviteDashboardLoading && dashboard.entries.isEmpty() ->
                PremiumLoadingState(text = "Carregando convites")

            dashboard.activeEntries.isEmpty() -> PremiumEmptyState(
                title = "Nenhum convite ativo",
                subtitle = "Convites aprovados, expirados ou encerrados ficam em \"aprovados e expirados\".",
                icon = Icons.Outlined.PersonAdd,
            )

            else -> dashboard.activeEntries.forEach { entry ->
                InviteEntryCard(
                    entry = entry,
                    inviteLink = buildInviteLink(session.tenant?.slug.orEmpty(), entry.token),
                    nowMillis = nowMillis,
                    isRevoking = state.revokingInviteId == entry.id,
                    onCopyClick = onCopyInviteClick,
                    onRevokeClick = { onRevokeInviteClick(entry) },
                )
            }
        }
    }
}

/** `/configuracoes/convites/aprovados` — aprovados de um lado, expirados/encerrados do outro. */
@Composable
fun SettingsInvitesHistoryScreen(
    state: SettingsUiState,
    session: UserSession,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tenantName = session.tenant?.name.orEmpty()
        .ifBlank { state.tenantName.ifBlank { "sua atlética" } }
    val dashboard = state.inviteDashboard
    val nowMillis = remember { System.currentTimeMillis() }

    PremiumScreen(modifier = modifier, bottomPadding = 40.dp) {
        PremiumHeader(
            title = "Aprovados e expirados",
            subtitle = "Histórico de convites de $tenantName",
            icon = Icons.Outlined.History,
            accent = PremiumAmber,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumAmber) {
            Text(
                text = "Histórico da tenant",
                color = PremiumAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
            )
            Text(
                text = "Aqui ficam os convites aprovados e os links que já expiraram, " +
                    "foram encerrados ou revogados.",
                color = PremiumZinc400,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        PremiumSecondaryButton(
            text = if (state.isInviteDashboardLoading) "Atualizando" else "Atualizar",
            onClick = onRefreshClick,
            enabled = !state.isInviteDashboardLoading,
            accent = PremiumZinc400,
            icon = Icons.Outlined.Schedule,
        )

        if (state.isInviteDashboardLoading && dashboard.entries.isEmpty()) {
            PremiumLoadingState(text = "Carregando histórico")
            return@PremiumScreen
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "CONVITES APROVADOS",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            PremiumChip(
                label = "${dashboard.approvedCount} aprovado${if (dashboard.approvedCount == 1) "" else "s"}",
                accent = PremiumBrand,
                icon = Icons.Outlined.CheckCircle,
            )
        }

        if (dashboard.approvedEntries.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum convite aprovado ainda",
                subtitle = "Quando um link virar cadastro aprovado, ele aparece aqui.",
                icon = Icons.Outlined.CheckCircle,
            )
        } else {
            dashboard.approvedEntries.forEach { entry ->
                InviteHistoryCard(entry = entry, nowMillis = nowMillis, accent = PremiumBrand)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "EXPIRADOS E ENCERRADOS",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            PremiumChip(
                label = "${dashboard.closedEntries.size} registro${if (dashboard.closedEntries.size == 1) "" else "s"}",
                accent = PremiumZinc400,
                icon = Icons.Outlined.History,
            )
        }

        if (dashboard.closedEntries.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum convite expirado ou encerrado",
                subtitle = "Links revogados ou vencidos aparecem nesta lista.",
                icon = Icons.Outlined.History,
            )
        } else {
            dashboard.closedEntries.forEach { entry ->
                InviteHistoryCard(entry = entry, nowMillis = nowMillis, accent = PremiumZinc400)
            }
        }
    }
}

@Composable
private fun InviteEntryCard(
    entry: SettingsInviteEntry,
    inviteLink: String,
    nowMillis: Long,
    isRevoking: Boolean,
    onCopyClick: (String) -> Unit,
    onRevokeClick: () -> Unit,
) {
    val accent = entry.activity.inviteColor()
    PremiumCard(accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PremiumChip(label = entry.activity.label, accent = accent)
            PremiumChip(label = entry.approvalStatus.label, accent = entry.approvalStatus.inviteColor())
            PremiumChip(
                label = entry.countdownLabel(nowMillis),
                accent = PremiumAmber,
                icon = Icons.Outlined.Schedule,
            )
        }

        Text(
            text = inviteLink,
            color = PremiumZinc400,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        PremiumInfoRow(
            label = "Usuário cadastrado",
            value = entry.requesterName.ifBlank { "Aguardando uso" },
            accent = accent,
        )
        PremiumInfoRow(
            label = "Turma",
            value = entry.requesterClass.ifBlank { "Sem turma" },
            accent = accent,
        )
        PremiumInfoRow(
            label = "Criado em",
            value = entry.createdAt.ifBlank { "data indisponivel" },
            accent = accent,
        )
        PremiumInfoRow(label = "Usos", value = "${entry.usesCount}/${entry.maxUses}", accent = accent)

        PremiumSecondaryButton(
            text = "Copiar link",
            onClick = { onCopyClick(inviteLink) },
            accent = PremiumZinc400,
            icon = Icons.Outlined.ContentCopy,
        )
        if (entry.isRevocable) {
            PremiumSecondaryButton(
                text = if (isRevoking) "Encerrando" else "Encerrar convite",
                onClick = onRevokeClick,
                enabled = !isRevoking,
                accent = PremiumRed,
                icon = Icons.Outlined.Close,
            )
        }
    }
}

@Composable
private fun InviteHistoryCard(
    entry: SettingsInviteEntry,
    nowMillis: Long,
    accent: Color,
) {
    PremiumCard(accent = accent) {
        Text(
            text = entry.requesterName.ifBlank { "Convite sem uso" }.uppercase(),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = listOf(
                entry.requesterClass.ifBlank { "Sem turma" },
                entry.requesterEmail,
            ).filter(String::isNotBlank).joinToString(" • "),
            color = PremiumZinc500,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PremiumChip(label = entry.approvalStatus.label, accent = entry.approvalStatus.inviteColor())
            PremiumChip(label = entry.activity.label, accent = entry.activity.inviteColor())
        }
        PremiumInfoRow(
            label = "Validade",
            value = entry.countdownLabel(nowMillis),
            accent = accent,
        )
        PremiumInfoRow(
            label = "Criado em",
            value = entry.createdAt.ifBlank { "data indisponivel" },
            accent = accent,
        )
    }
}

@Composable
private fun InviteStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                text = label.uppercase(),
                color = accent,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun InviteQuotaNotice(
    title: String,
    body: String,
    accent: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title.uppercase(),
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.3.sp,
                )
                Text(
                    text = body,
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** `formatCooldown` do web: HH:mm:ss até liberar o bônus. */
internal fun formatInviteCooldown(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

internal fun SettingsInviteActivity.inviteColor(): Color = when (this) {
    SettingsInviteActivity.Active -> PremiumBrand
    SettingsInviteActivity.Expired -> PremiumZinc500
    SettingsInviteActivity.Revoked -> PremiumRed
    SettingsInviteActivity.Closed -> PremiumZinc400
}

internal fun SettingsInviteApprovalStatus.inviteColor(): Color = when (this) {
    SettingsInviteApprovalStatus.Approved -> PremiumBrand
    SettingsInviteApprovalStatus.Pending -> PremiumAmber
    SettingsInviteApprovalStatus.Rejected -> PremiumRed
    SettingsInviteApprovalStatus.Unused -> PremiumZinc500
}

internal fun buildInviteLink(
    tenantSlug: String,
    token: String,
): String {
    val baseUrl = BuildConfig.WEB_APP_URL.trimEnd('/').ifBlank { "https://usc-atleticas.vercel.app" }
    val tenantPath = tenantSlug.trim().takeIf(String::isNotBlank)?.let { "/$it" }.orEmpty()
    return "$baseUrl$tenantPath/cadastro?invite=$token"
}
