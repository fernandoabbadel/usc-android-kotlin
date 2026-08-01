package com.example.usc1.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.session.UserSession
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
import com.example.usc1.domain.model.SettingsMentorshipAction
import com.example.usc1.domain.model.SettingsMentorshipRequest
import com.example.usc1.domain.model.SettingsMentorshipRoleCard
import com.example.usc1.domain.model.SettingsMentorshipRoleOptions
import com.example.usc1.domain.model.SettingsMentorshipRoleSide
import com.example.usc1.domain.model.SettingsMentorshipStatus
import com.example.usc1.domain.model.SettingsTurmaLeaderRequest

/**
 * `/configuracoes/apadrinhamento` — adicionar vínculo (turma → aluno), cards de padrinho e
 * afilhado com troca de rótulo e remoção, e as filas de convites recebidos e enviados.
 */
@Composable
fun SettingsMentorshipScreen(
    state: SettingsUiState,
    session: UserSession,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onSendInviteClick: (targetUserId: String, targetIsMentor: Boolean) -> Unit,
    onRespondClick: (relationshipId: String, action: SettingsMentorshipAction, roleLabel: String) -> Unit,
    onEditRoleLabelClick: (relationshipId: String, side: SettingsMentorshipRoleSide, label: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hub = state.mentorshipHub
    val labels = hub.labels
    val currentUserId = session.user?.id.orEmpty()

    var targetIsMentor by remember { mutableStateOf(true) }
    var selectedClass by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf("") }

    PremiumScreen(modifier = modifier, bottomPadding = 40.dp) {
        PremiumHeader(
            title = labels.hubTitle,
            subtitle = "Convites, aceite e visibilidade em ${state.tenantName.ifBlank { "sua atlética" }}",
            icon = Icons.Outlined.FavoriteBorder,
            accent = PremiumBrand,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumBrand) {
            Text(
                text = "Relações da Atlética",
                color = PremiumBrand,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
            )
            Text(
                text = "${labels.mentorLabel} e ${labels.menteeLabel}".uppercase(),
                color = Color.White,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = labels.requestHelpText,
                color = PremiumZinc400,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        if (state.mentorshipError.isNotBlank()) {
            PremiumCard(accent = PremiumRed) {
                Text(
                    text = "ERRO",
                    color = PremiumRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    text = state.mentorshipError,
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        PremiumSecondaryButton(
            text = if (state.isMentorshipLoading) "Atualizando" else "Atualizar",
            onClick = onRefreshClick,
            enabled = !state.isMentorshipLoading,
            accent = PremiumZinc400,
            icon = Icons.Outlined.Refresh,
        )

        if (state.isMentorshipLoading && !hub.hasVisibleContent()) {
            PremiumLoadingState(text = "Carregando apadrinhamento")
        }

        // --- Adicionar vínculo: tipo -> turma -> aluno, igual aos três selects do web. ---
        PremiumCard(accent = PremiumBrand) {
            Text(
                text = "ADICIONAR VÍNCULO",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
            )

            OptionRow(label = "Tipo") {
                SelectablePill(labels.mentorLabel, targetIsMentor) {
                    targetIsMentor = true
                    selectedUserId = ""
                }
                SelectablePill(labels.menteeLabel, !targetIsMentor) {
                    targetIsMentor = false
                    selectedUserId = ""
                }
            }

            OptionRow(label = "Turma") {
                if (state.mentorshipClassOptions.isEmpty()) {
                    Text(
                        text = "Nenhum membro disponível",
                        color = PremiumZinc500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                state.mentorshipClassOptions.forEach { classCode ->
                    SelectablePill(classCode, selectedClass == classCode) {
                        selectedClass = classCode
                        selectedUserId = ""
                    }
                }
            }

            if (selectedClass.isNotBlank()) {
                OptionRow(label = "Aluno") {
                    state.mentorshipCandidatesForClass(selectedClass).forEach { candidate ->
                        SelectablePill(candidate.name, selectedUserId == candidate.id) {
                            selectedUserId = candidate.id
                        }
                    }
                }
            }

            PremiumSecondaryButton(
                text = if (state.isSendingMentorshipInvite) "Enviando" else "Adicionar",
                onClick = { onSendInviteClick(selectedUserId, targetIsMentor) },
                enabled = selectedUserId.isNotBlank() && !state.isSendingMentorshipInvite,
                accent = PremiumBrand,
                icon = Icons.Outlined.PersonAdd,
            )
        }

        MentorshipRoleCard(
            title = labels.mentorLabel,
            emptyText = "Você ainda não tem ${labels.mentorLabel.lowercase()}.",
            item = hub.mentor,
            options = hub.mentor?.let { SettingsMentorshipRoleOptions.resolve(labels, it.ownerRoleSide) }.orEmpty(),
            isBusy = state.mentorshipActionId.isNotBlank() &&
                state.mentorshipActionId == hub.mentor?.relationshipId,
            accent = PremiumBrand,
            onEditOption = { option ->
                hub.mentor?.let { onEditRoleLabelClick(it.relationshipId, it.ownerRoleSide, option) }
            },
            onRemove = {
                hub.mentor?.let { onRespondClick(it.relationshipId, SettingsMentorshipAction.Remove, "") }
            },
        )

        MentorshipRoleCard(
            title = labels.menteeLabel,
            emptyText = "Você ainda não tem ${labels.menteeLabel.lowercase()}.",
            item = hub.mentee,
            options = hub.mentee?.let { SettingsMentorshipRoleOptions.resolve(labels, it.ownerRoleSide) }.orEmpty(),
            isBusy = state.mentorshipActionId.isNotBlank() &&
                state.mentorshipActionId == hub.mentee?.relationshipId,
            accent = Color(0xFF60A5FA),
            onEditOption = { option ->
                hub.mentee?.let { onEditRoleLabelClick(it.relationshipId, it.ownerRoleSide, option) }
            },
            onRemove = {
                hub.mentee?.let { onRespondClick(it.relationshipId, SettingsMentorshipAction.Remove, "") }
            },
        )

        Text(
            text = "CONVITES RECEBIDOS",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
        if (hub.incoming.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum convite pendente",
                subtitle = "Convites recebidos de outros atletas aparecem aqui.",
                icon = Icons.Outlined.FavoriteBorder,
            )
        } else {
            hub.incoming.forEach { request ->
                // O rótulo é escolhido pelo lado de quem aceita.
                val side = if (request.roleLabel == labels.mentorLabel) {
                    SettingsMentorshipRoleSide.Mentee
                } else {
                    SettingsMentorshipRoleSide.Mentor
                }
                MentorshipRequestCard(
                    request = request,
                    helper = "Quer te ter como ${request.roleLabel}.",
                    isBusy = state.mentorshipActionId == request.id,
                    acceptOptions = SettingsMentorshipRoleOptions.resolve(labels, side),
                    onAccept = { option ->
                        onRespondClick(request.id, SettingsMentorshipAction.Accept, option)
                    },
                    onReject = { onRespondClick(request.id, SettingsMentorshipAction.Reject, "") },
                    onCancel = null,
                    accent = PremiumAmber,
                )
            }
        }

        Text(
            text = "CONVITES ENVIADOS",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
        if (hub.outgoing.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum convite enviado",
                subtitle = "Você ainda não enviou convite de apadrinhamento.",
                icon = Icons.Outlined.PersonAdd,
            )
        } else {
            hub.outgoing.forEach { request ->
                MentorshipRequestCard(
                    request = request,
                    helper = "Convite para ${request.roleLabel.lowercase()}.",
                    isBusy = state.mentorshipActionId == request.id,
                    acceptOptions = emptyList(),
                    onAccept = {},
                    onReject = null,
                    onCancel = { onRespondClick(request.id, SettingsMentorshipAction.Cancel, "") },
                    accent = Color(0xFF22D3EE),
                )
            }
        }

        if (currentUserId.isBlank()) {
            PremiumEmptyState(
                title = "Sessão necessária",
                subtitle = "Entre com sua conta para gerenciar vínculos de apadrinhamento.",
                icon = Icons.Outlined.FavoriteBorder,
            )
        }
    }
}

/**
 * `/configuracoes/lider-turma` — pendências de cadastro da turma do líder (ou de todo o tenant,
 * para gestores). A aprovação em si roda com service role no web; ver relatório do M4.
 */
@Composable
fun SettingsTurmaLeaderScreen(
    state: SettingsUiState,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pending = state.turmaLeader

    PremiumScreen(modifier = modifier, bottomPadding = 40.dp) {
        PremiumHeader(
            title = "Lider da Turma",
            subtitle = if (pending.canManageAll) {
                "Visao completa do tenant ${state.tenantName}".trim()
            } else {
                "Pendencias da turma ${pending.leaderTurma.ifBlank { state.classLabel }}".trim()
            },
            icon = Icons.Outlined.Shield,
            accent = Color(0xFF22D3EE),
            onBackClick = onBackClick,
        )

        PremiumCard(accent = Color(0xFF22D3EE)) {
            Text(
                text = "APROVACOES PENDENTES",
                color = Color(0xFF22D3EE),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp,
            )
            Text(
                text = if (pending.canManageAll) {
                    "Você enxerga todas as solicitações do tenant."
                } else {
                    "Você só pode revisar usuários da sua própria turma."
                },
                color = PremiumZinc400,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        PremiumCard(accent = PremiumZinc500) {
            Text(
                text = "REVISAO NO SERVIDOR",
                color = PremiumZinc400,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )
            Text(
                text = "Aprovar ou rejeitar grava no cadastro de outro usuário e roda com chave de " +
                    "serviço no servidor da plataforma. O app mostra a fila, mas a decisão continua no painel web.",
                color = PremiumZinc400,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        PremiumSecondaryButton(
            text = if (state.isTurmaLeaderLoading) "Atualizando" else "Atualizar",
            onClick = onRefreshClick,
            enabled = !state.isTurmaLeaderLoading,
            accent = PremiumZinc400,
            icon = Icons.Outlined.Refresh,
        )

        when {
            state.isTurmaLeaderLoading && pending.requests.isEmpty() ->
                PremiumLoadingState(text = "Carregando pendencias")

            state.turmaLeaderError.isNotBlank() -> PremiumEmptyState(
                title = "Pendencias indisponíveis",
                subtitle = state.turmaLeaderError,
                icon = Icons.Outlined.Shield,
            )

            pending.requests.isEmpty() -> PremiumEmptyState(
                title = "Nenhuma solicitação pendente",
                subtitle = "Nada na sua faixa de liderança agora.",
                icon = Icons.Outlined.Shield,
            )

            else -> pending.requests.forEach { request ->
                TurmaLeaderRequestCard(request = request)
            }
        }
    }
}

@Composable
private fun TurmaLeaderRequestCard(request: SettingsTurmaLeaderRequest) {
    PremiumCard(accent = Color(0xFF22D3EE)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MentorshipAvatar(
                name = request.requesterName,
                photoUrl = request.requesterPhotoUrl,
                accent = Color(0xFF22D3EE),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = request.requesterName
                        .ifBlank { request.requesterEmail }
                        .ifBlank { request.requesterUserId },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(
                        request.requesterEmail.ifBlank { "Sem email" },
                        request.requesterClass.ifBlank { "Sem turma" },
                    ).joinToString(" • "),
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PremiumChip(label = "Pendente", accent = PremiumAmber)
        }

        PremiumInfoRow(
            label = "Convite",
            value = request.inviteToken.ifBlank { "manual" },
            accent = Color(0xFF22D3EE),
        )
        PremiumInfoRow(
            label = "Origem",
            value = request.inviterName.ifBlank { request.inviterEmail }.ifBlank { "sem criador" },
            accent = Color(0xFF22D3EE),
        )
        PremiumInfoRow(
            label = "Solicitado em",
            value = request.requestedAtLabel.ifBlank { "agora" },
            accent = Color(0xFF22D3EE),
        )
    }
}

@Composable
private fun MentorshipRoleCard(
    title: String,
    emptyText: String,
    item: SettingsMentorshipRoleCard?,
    options: List<String>,
    isBusy: Boolean,
    accent: Color,
    onEditOption: (String) -> Unit,
    onRemove: () -> Unit,
) {
    PremiumCard(accent = accent) {
        Text(
            text = (item?.roleLabel ?: title).uppercase(),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.3.sp,
        )

        if (item == null) {
            Text(
                text = emptyText,
                color = PremiumZinc500,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            return@PremiumCard
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MentorshipAvatar(name = item.user.name, photoUrl = item.user.photoUrl, accent = accent)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.user.name.ifBlank { "Atleta" }.uppercase(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.user.classCode.ifBlank { "Sem turma" },
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (options.size > 1) {
            OptionRow(label = "Meu rotulo") {
                options.forEach { option ->
                    SelectablePill(
                        label = option,
                        selected = option == item.ownerRoleLabel,
                        enabled = !isBusy && option != item.ownerRoleLabel,
                    ) { onEditOption(option) }
                }
            }
        }

        PremiumSecondaryButton(
            text = if (isBusy) "Processando" else "Remover vinculo",
            onClick = onRemove,
            enabled = !isBusy,
            accent = PremiumRed,
            icon = Icons.Outlined.Delete,
        )
    }
}

@Composable
private fun MentorshipRequestCard(
    request: SettingsMentorshipRequest,
    helper: String,
    isBusy: Boolean,
    acceptOptions: List<String>,
    onAccept: (String) -> Unit,
    onReject: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    accent: Color,
) {
    PremiumCard(accent = accent) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MentorshipAvatar(
                name = request.otherUser?.name.orEmpty(),
                photoUrl = request.otherUser?.photoUrl.orEmpty(),
                accent = accent,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = request.otherUser?.name?.ifBlank { "Atleta" } ?: "Atleta",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = request.otherUser?.classCode.orEmpty().ifBlank { "Sem turma" },
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = helper,
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            PremiumChip(label = request.status.label, accent = request.status.mentorshipColor())
        }

        if (request.createdAt.isNotBlank()) {
            PremiumInfoRow(label = "Enviado em", value = request.createdAt, accent = accent)
        }

        acceptOptions.forEach { option ->
            PremiumSecondaryButton(
                text = if (acceptOptions.size > 1) "Aceitar como $option" else "Aceitar",
                onClick = { onAccept(option) },
                enabled = !isBusy,
                accent = PremiumBrand,
                icon = Icons.Outlined.Check,
            )
        }
        if (onReject != null) {
            PremiumSecondaryButton(
                text = "Recusar",
                onClick = onReject,
                enabled = !isBusy,
                accent = PremiumRed,
                icon = Icons.Outlined.Close,
            )
        }
        if (onCancel != null) {
            PremiumSecondaryButton(
                text = if (isBusy) "Processando" else "Cancelar",
                onClick = onCancel,
                enabled = !isBusy,
                accent = PremiumZinc400,
                icon = Icons.Outlined.Close,
            )
        }
    }
}

@Composable
private fun MentorshipAvatar(
    name: String,
    photoUrl: String,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(accent.copy(alpha = 0.14f), CircleShape)
            .border(1.dp, accent.copy(alpha = 0.32f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black, CircleShape),
            )
        } else {
            Text(
                text = name.initials().ifBlank { "US" },
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            color = PremiumZinc500,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SelectablePill(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) PremiumBrand else PremiumZinc900,
        border = BorderStroke(1.dp, if (selected) PremiumBrand else PremiumZinc800),
    ) {
        Text(
            text = label.uppercase(),
            color = when {
                selected -> Color.Black
                enabled -> PremiumZinc400
                else -> PremiumZinc500
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
        )
    }
}

private fun SettingsMentorshipStatus.mentorshipColor(): Color = when (this) {
    SettingsMentorshipStatus.Pending -> PremiumAmber
    SettingsMentorshipStatus.Accepted -> PremiumBrand
    SettingsMentorshipStatus.Rejected -> PremiumRed
    SettingsMentorshipStatus.Cancelled -> PremiumZinc500
}

private fun com.example.usc1.domain.model.SettingsMentorshipHub.hasVisibleContent(): Boolean =
    mentor != null || mentee != null || incoming.isNotEmpty() || outgoing.isNotEmpty()

