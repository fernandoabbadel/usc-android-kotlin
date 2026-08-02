package com.example.usc1.ui.collectives.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumGold
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumPurple
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.ui.collectives.CollectiveKind
import com.example.usc1.ui.collectives.collectiveAccent
import java.text.NumberFormat
import java.util.Locale

private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

private fun money(value: Double): String = currencyFormatter.format(value)

/**
 * Painel de gestão do coletivo: seleção, hub, informações e membros.
 *
 * Web: `CommissionManagementGate`/`DirectoryManagementGate` mais `LigasAdminPageContent`
 * (`pageVariant="hub"`, `lockedTab="visual"`, `lockedTab="members"`).
 */
@Composable
fun CollectiveManagementScreen(
    state: CollectiveManagementUiState,
    onBackClick: () -> Unit,
    onSelectCollective: (ManagedCollective) -> Unit,
    onNavClick: (CollectiveManagementNav) -> Unit,
    modifier: Modifier = Modifier,
    onInfoChange: ((CollectiveInfoForm) -> CollectiveInfoForm) -> Unit = {},
    onAddLink: () -> Unit = {},
    onUpdateLink: (String, (CollectiveLinkDraft) -> CollectiveLinkDraft) -> Unit = { _, _ -> },
    onRemoveLink: (String) -> Unit = {},
    onSaveInfo: () -> Unit = {},
    onOpenUserSearch: () -> Unit = {},
    onCloseUserSearch: () -> Unit = {},
    onSearchTermChange: (String) -> Unit = {},
    onAddMember: (CollectiveUserOption) -> Unit = {},
    onMemberRoleChange: (String, String) -> Unit = { _, _ -> },
    onRemoveMember: (String) -> Unit = {},
    onRequestRoleChange: (String, String) -> Unit = { _, _ -> },
    onApproveRequest: (String) -> Unit = {},
    onRejectRequest: (String) -> Unit = {},
    onSaveMembers: () -> Unit = {},
) {
    val kind = state.kind
    val accent = collectiveAccent(kind)

    if (state.isLoading && state.selected == null && state.managedCollectives.isEmpty()) {
        PremiumLoadingState(text = "Carregando gestão", modifier = modifier)
        return
    }

    val selected = state.selected
    if (selected == null) {
        CollectiveManagementSelectionScreen(
            state = state,
            onBackClick = onBackClick,
            onSelectCollective = onSelectCollective,
            modifier = modifier,
        )
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        CollectiveManagementHeader(collective = selected, onBackClick = onBackClick)
        CollectiveManagementQuickNav(kind = kind, active = state.activeNav, onNavClick = onNavClick)

        state.errorMessage?.let { message ->
            CollectiveBanner(text = message, accent = PremiumRed)
        }
        state.actionMessage?.let { message ->
            CollectiveBanner(text = message, accent = accent)
        }

        when (state.activeNav) {
            CollectiveManagementNav.Info -> CollectiveInfoSection(
                state = state,
                accent = accent,
                onInfoChange = onInfoChange,
                onAddLink = onAddLink,
                onUpdateLink = onUpdateLink,
                onRemoveLink = onRemoveLink,
                onSaveInfo = onSaveInfo,
            )

            CollectiveManagementNav.Members -> CollectiveMembersSection(
                state = state,
                accent = accent,
                onOpenUserSearch = onOpenUserSearch,
                onCloseUserSearch = onCloseUserSearch,
                onSearchTermChange = onSearchTermChange,
                onAddMember = onAddMember,
                onMemberRoleChange = onMemberRoleChange,
                onRemoveMember = onRemoveMember,
                onRequestRoleChange = onRequestRoleChange,
                onApproveRequest = onApproveRequest,
                onRejectRequest = onRejectRequest,
                onSaveMembers = onSaveMembers,
            )

            else -> CollectiveManagementHub(
                kind = kind,
                accent = accent,
                onNavClick = onNavClick,
            )
        }
    }
}

/** Tela "Escolha a comissão"/"Nenhuma liga disponível" do gate. */
@Composable
private fun CollectiveManagementSelectionScreen(
    state: CollectiveManagementUiState,
    onBackClick: () -> Unit,
    onSelectCollective: (ManagedCollective) -> Unit,
    modifier: Modifier = Modifier,
) {
    val kind = state.kind
    val accent = collectiveAccent(kind)

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Gestão de ${kind.entityLabel}s".uppercase(),
            subtitle = "Escolha ${kind.entityArticle} ${kind.entityLabel} que você quer editar",
            icon = Icons.Outlined.Groups,
            accent = accent,
            onBackClick = onBackClick,
        )

        state.errorMessage?.let { CollectiveBanner(text = it, accent = PremiumRed) }

        if (!state.hasAccess) {
            // Bloco "Acesso restrito" do gate.
            PremiumEmptyState(
                title = "Acesso restrito",
                subtitle = "Você não tem ${kind.entityLabel} para gerenciar. O acesso é liberado para " +
                    "Presidente, Vice-Presidente, Secretaria, Tesouraria, Diretoria ou master da plataforma.",
                icon = Icons.Outlined.Groups,
                accent = PremiumRed,
            )
            return@PremiumScreen
        }

        PremiumChip(
            label = "${state.managedCollectives.size} disponíveis",
            accent = accent,
        )

        state.managedCollectives.forEach { collective ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectCollective(collective) },
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, PremiumZinc800),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!collective.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = collective.logoUrl,
                            contentDescription = collective.name,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = accent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                        ) {
                            Icon(
                                Icons.Outlined.Groups,
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp),
                                tint = accent,
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = collective.selectionEyebrow.uppercase(),
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            text = collective.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = collective.managementRole.ifBlank { "Gestão" },
                            color = PremiumZinc500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/** `pageVariant === "hub"`: "Acesso rápido / Escolha a área que você quer editar". */
@Composable
private fun CollectiveManagementHub(
    kind: CollectiveKind,
    accent: Color,
    onNavClick: (CollectiveManagementNav) -> Unit,
) {
    PremiumCard(accent = accent, borderAlpha = 0.22f) {
        Text(
            text = "Acesso rápido".uppercase(),
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
        Text(
            text = "Escolha a área que você quer editar",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )
    }

    CollectiveActionCard(
        eyebrow = "Informações",
        title = "Editar dados ${kind.entityArticle} ${kind.entityLabel}",
        icon = Icons.Outlined.Info,
        accent = PremiumBrand,
        onClick = { onNavClick(CollectiveManagementNav.Info) },
    )
    CollectiveActionCard(
        eyebrow = "Membros",
        title = "Gerir diretoria",
        icon = Icons.Outlined.Groups,
        accent = PremiumPurple,
        onClick = { onNavClick(CollectiveManagementNav.Members) },
    )
    CollectiveActionCard(
        eyebrow = "Eventos",
        title = "Publicar e editar agenda",
        icon = Icons.Outlined.CalendarMonth,
        accent = PremiumAmber,
        onClick = { onNavClick(CollectiveManagementNav.Agenda) },
    )
    if (kind.showsBoardRound) {
        CollectiveActionCard(
            eyebrow = "Board Round",
            title = "Configurar perguntas",
            icon = Icons.Outlined.LayersClear,
            accent = PremiumPurple,
            onClick = { onNavClick(CollectiveManagementNav.Board) },
        )
    }
    CollectiveActionCard(
        eyebrow = "Loja",
        title = "Produtos e pedidos",
        icon = Icons.Outlined.Storefront,
        accent = PremiumBrand,
        onClick = { onNavClick(CollectiveManagementNav.Store) },
    )
    CollectiveActionCard(
        eyebrow = "Gestão",
        title = "Vendas e BI",
        icon = Icons.Outlined.Wallet,
        accent = PremiumGold,
        onClick = { onNavClick(CollectiveManagementNav.Finance) },
    )
}

// ------------------------------------------------------------------
// Informações
// ------------------------------------------------------------------

@Composable
private fun CollectiveInfoSection(
    state: CollectiveManagementUiState,
    accent: Color,
    onInfoChange: ((CollectiveInfoForm) -> CollectiveInfoForm) -> Unit,
    onAddLink: () -> Unit,
    onUpdateLink: (String, (CollectiveLinkDraft) -> CollectiveLinkDraft) -> Unit,
    onRemoveLink: (String) -> Unit,
    onSaveInfo: () -> Unit,
) {
    val info = state.info
    val kind = state.kind

    PremiumCard(accent = accent, borderAlpha = 0.22f) {
        PremiumTextField(
            value = info.acronym,
            onValueChange = { value ->
                onInfoChange { it.copy(acronym = value.uppercase().take(CollectiveInfoForm.AcronymMaxLength)) }
            },
            label = "Sigla",
        )
        CollectiveDetailLine("${info.acronym.length}/${CollectiveInfoForm.AcronymMaxLength} caracteres.")

        PremiumTextField(
            value = info.name,
            onValueChange = { value ->
                onInfoChange { it.copy(name = value.take(CollectiveInfoForm.NameMaxLength)) }
            },
            label = "Nome completo",
        )
        CollectiveDetailLine(
            "Máximo de ${CollectiveInfoForm.NameMaxLength} caracteres para o nome caber melhor nos cards.",
        )

        PremiumTextField(
            value = info.description,
            onValueChange = { value ->
                onInfoChange { it.copy(description = value.take(CollectiveInfoForm.DescriptionMaxLength)) }
            },
            label = "Descrição",
            singleLine = false,
        )
        CollectiveDetailLine("${info.description.length}/${CollectiveInfoForm.DescriptionMaxLength} caracteres.")

        PremiumTextField(
            value = info.overview,
            onValueChange = { value ->
                onInfoChange { it.copy(overview = value.take(CollectiveInfoForm.OverviewMaxLength)) }
            },
            label = "Visão geral ${kind.entityArticle} ${kind.entityLabel}",
            singleLine = false,
        )
        CollectiveDetailLine("${info.overview.length}/${CollectiveInfoForm.OverviewMaxLength} caracteres.")
    }

    // Bloco "Links públicos".
    PremiumCard(accent = PremiumPurple, borderAlpha = 0.22f) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Link,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = PremiumPurple,
                    )
                    Text(
                        text = "Links públicos".uppercase(),
                        color = PremiumPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                    )
                }
                CollectiveDetailLine("Esses links aparecem no perfil público ${kind.entityArticle} ${kind.entityLabel}.")
            }
        }

        PremiumSecondaryButton(
            text = "Adicionar link",
            onClick = onAddLink,
            accent = PremiumPurple,
            icon = Icons.Outlined.Add,
        )

        if (info.links.isEmpty()) {
            CollectiveDetailLine("Nenhum link cadastrado.")
        }

        info.links.forEach { link ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectiveOptionRow(
                    options = CollectiveLinkType.entries.map { it.label },
                    selected = link.type.label,
                    onSelect = { label ->
                        val type = CollectiveLinkType.entries.first { it.label == label }
                        onUpdateLink(link.id) { it.copy(type = type) }
                    },
                    accent = PremiumPurple,
                )
                PremiumTextField(
                    value = link.label,
                    onValueChange = { value ->
                        onUpdateLink(link.id) {
                            it.copy(label = value.take(CollectiveInfoForm.LinkLabelMaxLength))
                        }
                    },
                    label = "Nome do botão",
                )
                PremiumTextField(
                    value = link.url,
                    onValueChange = { value ->
                        onUpdateLink(link.id) { it.copy(url = value.take(CollectiveInfoForm.LinkUrlMaxLength)) }
                    },
                    label = "https://...",
                )
                PremiumSecondaryButton(
                    text = "Remover link",
                    onClick = { onRemoveLink(link.id) },
                    accent = PremiumRed,
                    icon = Icons.Outlined.Delete,
                )
            }
        }
    }

    // Bloco "Informações de pagamento".
    PremiumCard(accent = PremiumBrand, borderAlpha = 0.22f) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = PremiumBrand,
            )
            Text(
                text = "Informações de pagamento".uppercase(),
                color = PremiumBrand,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
        }
        CollectiveDetailLine(
            "Usado no perfil público e como fallback para eventos ${kind.entityArticle} ${kind.entityLabel}.",
        )

        PremiumTextField(
            value = info.pixKey,
            onValueChange = { value ->
                onInfoChange { it.copy(pixKey = value.take(CollectiveInfoForm.PixFieldMaxLength)) }
            },
            label = "Chave PIX",
        )
        PremiumTextField(
            value = info.pixBank,
            onValueChange = { value ->
                onInfoChange { it.copy(pixBank = value.take(CollectiveInfoForm.PixFieldMaxLength)) }
            },
            label = "Banco",
        )
        PremiumTextField(
            value = info.pixHolder,
            onValueChange = { value ->
                onInfoChange { it.copy(pixHolder = value.take(CollectiveInfoForm.PixFieldMaxLength)) }
            },
            label = "Nome do titular",
        )
        PremiumTextField(
            value = info.whatsapp,
            onValueChange = { value ->
                onInfoChange { it.copy(whatsapp = value.take(CollectiveInfoForm.PhoneMaxLength)) }
            },
            label = "WhatsApp para comprovantes",
        )
    }

    // Bloco "Destaque da Semana".
    PremiumCard(accent = PremiumGold, borderAlpha = 0.22f) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Destaque da semana".uppercase(),
                color = PremiumGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Enviar notificação?".uppercase(),
                    color = PremiumZinc400,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
                Switch(
                    checked = info.sendNotification,
                    onCheckedChange = { checked -> onInfoChange { it.copy(sendNotification = checked) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = PremiumBrand),
                )
            }
        }
        PremiumTextField(
            value = info.bizu,
            onValueChange = { value -> onInfoChange { it.copy(bizu = value) } },
            label = "Ex: Encontro aberto para novos membros...",
        )
        if (info.sendNotification) {
            CollectiveDetailLine(
                "Uma notificação será enviada para todos ao salvar!",
                color = PremiumBrand,
            )
        }
    }

    // Bloco "Status no BoardRound" (só leitura, como no web).
    if (state.showsBoardRound) {
        PremiumCard(accent = PremiumZinc800, borderAlpha = 0.65f) {
            Text(
                text = "Status no BoardRound".uppercase(),
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            Text(
                text = if (info.activeOnBoard) "ATIVADA NO TABULEIRO" else "AGUARDANDO ATIVAÇÃO",
                color = if (info.activeOnBoard) PremiumBrand else PremiumZinc500,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }

    PremiumPrimaryButton(
        text = "Salvar informações",
        onClick = onSaveInfo,
        loading = state.isSaving,
        accent = accent,
        icon = Icons.Outlined.CheckCircle,
    )
}

// ------------------------------------------------------------------
// Membros
// ------------------------------------------------------------------

@Composable
private fun CollectiveMembersSection(
    state: CollectiveManagementUiState,
    accent: Color,
    onOpenUserSearch: () -> Unit,
    onCloseUserSearch: () -> Unit,
    onSearchTermChange: (String) -> Unit,
    onAddMember: (CollectiveUserOption) -> Unit,
    onMemberRoleChange: (String, String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onRequestRoleChange: (String, String) -> Unit,
    onApproveRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onSaveMembers: () -> Unit,
) {
    val members = state.members

    PremiumCard(accent = accent, borderAlpha = 0.22f) {
        Text(text = "Diretoria".uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        CollectiveDetailLine("Adicione os membros oficiais.")
        PremiumSecondaryButton(
            text = "Adicionar aluno",
            onClick = onOpenUserSearch,
            accent = accent,
            icon = Icons.Outlined.PersonAdd,
        )
    }

    if (members.isSearchOpen) {
        PremiumCard(accent = accent, borderAlpha = 0.30f) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Buscar aluno".uppercase(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Fechar busca",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onCloseUserSearch),
                    tint = PremiumZinc400,
                )
            }
            PremiumTextField(
                value = members.searchTerm,
                onValueChange = onSearchTermChange,
                label = "Nome ou turma",
                leadingIcon = Icons.Outlined.Search,
            )
            if (members.isLoadingUsers) {
                CollectiveDetailLine("Carregando usuários...")
            } else if (members.filteredUserOptions.isEmpty()) {
                CollectiveDetailLine("Nenhum usuário encontrado.")
            } else {
                members.filteredUserOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddMember(option) }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MemberAvatar(photoUrl = option.photoUrl, accent = accent)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            CollectiveDetailLine(option.turma.ifBlank { "Sem turma" })
                        }
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = accent,
                        )
                    }
                }
            }
        }
    }

    // Bloco "Solicitações pendentes".
    PremiumCard(accent = PremiumAmber, borderAlpha = 0.22f) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Solicitações pendentes".uppercase(),
                color = PremiumAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
            PremiumChip(label = "${members.requests.size} pendentes", accent = PremiumAmber)
        }
        CollectiveDetailLine(
            "Aprove ou rejeite no rascunho e depois clique em salvar membros para persistir.",
        )

        if (members.requests.isEmpty()) {
            CollectiveDetailLine("Nenhuma solicitação pendente.")
        }

        members.requests.forEach { request ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MemberAvatar(photoUrl = request.photoUrl, accent = PremiumAmber)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.name,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        CollectiveDetailLine(request.turma.ifBlank { "Sem turma" })
                    }
                }
                CollectiveOptionRow(
                    options = members.roleOptions,
                    selected = request.requestedRole,
                    onSelect = { role -> onRequestRoleChange(request.id, role) },
                    accent = PremiumAmber,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumSecondaryButton(
                        text = "Aceitar",
                        onClick = { onApproveRequest(request.id) },
                        modifier = Modifier.weight(1f),
                        accent = PremiumBrand,
                        icon = Icons.Outlined.CheckCircle,
                    )
                    PremiumSecondaryButton(
                        text = "Rejeitar",
                        onClick = { onRejectRequest(request.id) },
                        modifier = Modifier.weight(1f),
                        accent = PremiumRed,
                        icon = Icons.Outlined.Close,
                    )
                }
            }
        }
    }

    if (members.members.isEmpty()) {
        PremiumEmptyState(
            title = "Sem membros",
            subtitle = "Adicione a diretoria para publicar os membros na página pública.",
            icon = Icons.Outlined.Groups,
            accent = accent,
        )
    }

    members.members.forEach { member ->
        PremiumCard(accent = if (member.persisted) PremiumZinc800 else accent, borderAlpha = 0.45f) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MemberAvatar(photoUrl = member.photoUrl, accent = accent)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    CollectiveDetailLine(
                        if (member.persisted) member.role else "${member.role} • novo no rascunho",
                    )
                }
            }
            CollectiveOptionRow(
                options = members.roleOptions,
                selected = member.role,
                onSelect = { role -> onMemberRoleChange(member.id, role) },
                accent = accent,
            )
            PremiumSecondaryButton(
                text = "Remover",
                onClick = { onRemoveMember(member.id) },
                accent = PremiumRed,
                icon = Icons.Outlined.Delete,
            )
        }
    }

    PremiumPrimaryButton(
        text = "Salvar membros",
        onClick = onSaveMembers,
        loading = state.isSaving,
        accent = accent,
        icon = Icons.Outlined.CheckCircle,
    )
}

@Composable
private fun MemberAvatar(photoUrl: String?, accent: Color) {
    if (photoUrl.isNullOrBlank()) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = accent.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
        ) {
            Icon(
                Icons.Outlined.Groups,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
                tint = accent,
            )
        }
    } else {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}

// ------------------------------------------------------------------
// Loja
// ------------------------------------------------------------------

/** Web: `app/ligas/LeagueStoreAdminPage.tsx`. */
@Composable
fun CollectiveStoreAdminScreen(
    state: CollectiveStoreAdminUiState,
    onBackClick: () -> Unit,
    onNavClick: (CollectiveManagementNav) -> Unit,
    onStoreModeClick: (CollectiveStoreMode) -> Unit,
    modifier: Modifier = Modifier,
    onCoverChange: (String) -> Unit = {},
    onSaveStore: (Boolean?) -> Unit = {},
    onToggleProducts: (Boolean) -> Unit = {},
    onOpenProductForm: (CollectiveAdminProduct?) -> Unit = {},
    onCloseProductForm: () -> Unit = {},
    onProductFormChange: ((CollectiveProductForm) -> CollectiveProductForm) -> Unit = {},
    onSaveProduct: () -> Unit = {},
    onToggleProduct: (CollectiveAdminProduct) -> Unit = {},
    onApproveOrder: (CollectiveStoreOrder) -> Unit = {},
    onOrderStatus: (CollectiveStoreOrder, String) -> Unit = { _, _ -> },
) {
    val collective = state.collective
    if (state.isLoading || collective == null) {
        PremiumLoadingState(text = "Carregando loja", modifier = modifier)
        return
    }

    val kind = state.kind
    val accent = collectiveAccent(kind)

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        CollectiveManagementHeader(
            collective = collective,
            onBackClick = onBackClick,
            eyebrow = "Loja ${kind.entityArticle} ${kind.entityLabel}",
        )
        CollectiveManagementQuickNav(
            kind = kind,
            active = CollectiveManagementNav.Store,
            onNavClick = onNavClick,
        )

        // Sub-navegação da loja: Loja | Produtos | Pendentes | Aprovados.
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CollectiveStoreMode.entries.forEach { mode ->
                val isActive = mode == state.mode
                Surface(
                    modifier = Modifier.clickable { onStoreModeClick(mode) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isActive) accent.copy(alpha = 0.16f) else PremiumZinc900,
                    border = BorderStroke(
                        1.dp,
                        if (isActive) accent.copy(alpha = 0.42f) else PremiumZinc800,
                    ),
                ) {
                    Text(
                        text = mode.title.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (isActive) accent else PremiumZinc400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                    )
                }
            }
        }

        state.errorMessage?.let { CollectiveBanner(text = it, accent = PremiumRed) }
        state.actionMessage?.let { CollectiveBanner(text = it, accent = accent) }

        when (state.mode) {
            CollectiveStoreMode.Overview -> CollectiveStoreOverview(
                state = state,
                accent = accent,
                onCoverChange = onCoverChange,
                onSaveStore = onSaveStore,
                onToggleProducts = onToggleProducts,
            )

            CollectiveStoreMode.Products -> CollectiveStoreProducts(
                state = state,
                accent = accent,
                onOpenProductForm = onOpenProductForm,
                onCloseProductForm = onCloseProductForm,
                onProductFormChange = onProductFormChange,
                onSaveProduct = onSaveProduct,
                onToggleProduct = onToggleProduct,
            )

            CollectiveStoreMode.PendingOrders,
            CollectiveStoreMode.ApprovedOrders,
            -> CollectiveStoreOrders(
                state = state,
                accent = accent,
                onApproveOrder = onApproveOrder,
                onOrderStatus = onOrderStatus,
            )
        }
    }
}

@Composable
private fun CollectiveStoreOverview(
    state: CollectiveStoreAdminUiState,
    accent: Color,
    onCoverChange: (String) -> Unit,
    onSaveStore: (Boolean?) -> Unit,
    onToggleProducts: (Boolean) -> Unit,
) {
    val kind = state.kind
    val collective = state.collective ?: return

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CollectiveMetricCard(
            label = "Categoria",
            value = collective.headerTitle,
            hint = if (state.categoryVisible) "Visível" else "Oculta",
            icon = Icons.Outlined.Storefront,
            modifier = Modifier.weight(1f),
            accent = accent,
        )
        CollectiveMetricCard(
            label = "Produtos",
            value = state.products.size.toString(),
            hint = "${state.visibleProducts.size} visíveis",
            icon = Icons.Outlined.Inventory2,
            modifier = Modifier.weight(1f),
            accent = accent,
        )
    }

    PremiumSecondaryButton(
        text = if (state.categoryVisible) "Ocultar categoria" else "Ativar categoria",
        onClick = { onSaveStore(!state.categoryVisible) },
        enabled = !state.isSaving,
        accent = accent,
    )
    PremiumSecondaryButton(
        text = if (state.allProductsVisible) "Ocultar produtos" else "Exibir produtos",
        onClick = { onToggleProducts(!state.allProductsVisible) },
        enabled = !state.isSaving && state.products.isNotEmpty(),
        accent = PremiumPurple,
    )

    PremiumCard(accent = accent, borderAlpha = 0.22f) {
        Text(
            text = "Informações da loja".uppercase(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
        PremiumTextField(
            value = state.storeCoverUrl,
            onValueChange = onCoverChange,
            label = "URL da capa",
        )
        CollectiveDetailLine(
            "A capa da loja ${kind.entityArticle} ${kind.entityLabel} é a mesma imagem exibida na loja pública.",
        )
        if (!state.hasCompletePayment) {
            CollectiveDetailLine(
                "Configure chave PIX, banco, titular e WhatsApp em Informações para liberar a venda.",
                color = PremiumAmber,
            )
        }
    }

    PremiumPrimaryButton(
        text = "Salvar loja",
        onClick = { onSaveStore(true) },
        loading = state.isSaving,
        accent = accent,
        icon = Icons.Outlined.CheckCircle,
    )
}

@Composable
private fun CollectiveStoreProducts(
    state: CollectiveStoreAdminUiState,
    accent: Color,
    onOpenProductForm: (CollectiveAdminProduct?) -> Unit,
    onCloseProductForm: () -> Unit,
    onProductFormChange: ((CollectiveProductForm) -> CollectiveProductForm) -> Unit,
    onSaveProduct: () -> Unit,
    onToggleProduct: (CollectiveAdminProduct) -> Unit,
) {
    val kind = state.kind

    PremiumCard(accent = accent, borderAlpha = 0.22f) {
        Text(
            text = "Produtos ${kind.entityArticle} ${kind.entityLabel}".uppercase(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
        CollectiveDetailLine(
            "O WhatsApp de comprovante vem da seção de informações ${kind.entityArticle} ${kind.entityLabel}.",
        )
        PremiumSecondaryButton(
            text = "Adicionar produto",
            onClick = { onOpenProductForm(null) },
            accent = accent,
            icon = Icons.Outlined.Add,
        )
    }

    state.form?.let { form ->
        PremiumCard(accent = accent, borderAlpha = 0.32f) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (form.isEditing) "Editar produto".uppercase() else "Novo produto".uppercase(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Fechar formulário",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onCloseProductForm),
                    tint = PremiumZinc400,
                )
            }

            PremiumTextField(
                value = form.name,
                onValueChange = { value ->
                    onProductFormChange { it.copy(name = value.take(CollectiveProductForm.NameMaxLength)) }
                },
                label = "Nome do produto",
            )
            PremiumTextField(
                value = form.price,
                onValueChange = { value -> onProductFormChange { it.copy(price = value) } },
                label = "Preço",
            )
            PremiumTextField(
                value = form.oldPrice,
                onValueChange = { value -> onProductFormChange { it.copy(oldPrice = value) } },
                label = "Preço antigo (opcional)",
            )
            PremiumTextField(
                value = form.stock,
                onValueChange = { value -> onProductFormChange { it.copy(stock = value.filter(Char::isDigit)) } },
                label = "Estoque",
            )
            PremiumTextField(
                value = form.lot,
                onValueChange = { value ->
                    onProductFormChange { it.copy(lot = value.take(CollectiveProductForm.LotMaxLength)) }
                },
                label = "Lote",
            )
            PremiumTextField(
                value = form.imageUrl,
                onValueChange = { value -> onProductFormChange { it.copy(imageUrl = value) } },
                label = "URL da imagem",
            )
            PremiumTextField(
                value = form.description,
                onValueChange = { value ->
                    onProductFormChange {
                        it.copy(description = value.take(CollectiveProductForm.DescriptionMaxLength))
                    }
                },
                label = "Descrição",
                singleLine = false,
            )
            PremiumTextField(
                value = form.tagLabel,
                onValueChange = { value ->
                    onProductFormChange { it.copy(tagLabel = value.take(CollectiveProductForm.BadgeMaxLength)) }
                },
                label = "Selo (opcional)",
            )

            CollectiveDetailLine("Status de venda")
            CollectiveOptionRow(
                options = CollectiveProductStatus.entries.map { it.label },
                selected = form.status.label,
                onSelect = { label ->
                    val status = CollectiveProductStatus.entries.first { it.label == label }
                    onProductFormChange { it.copy(status = status) }
                },
                accent = accent,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Usar dados de pagamento próprios".uppercase(),
                    color = PremiumZinc400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
                Switch(
                    checked = form.useOwnPayment,
                    onCheckedChange = { checked -> onProductFormChange { it.copy(useOwnPayment = checked) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = PremiumBrand),
                )
            }

            if (form.useOwnPayment) {
                PremiumTextField(
                    value = form.pixKey,
                    onValueChange = { value -> onProductFormChange { it.copy(pixKey = value) } },
                    label = "Chave PIX",
                )
                PremiumTextField(
                    value = form.pixBank,
                    onValueChange = { value -> onProductFormChange { it.copy(pixBank = value) } },
                    label = "Banco",
                )
                PremiumTextField(
                    value = form.pixHolder,
                    onValueChange = { value -> onProductFormChange { it.copy(pixHolder = value) } },
                    label = "Nome do titular",
                )
            }

            PremiumPrimaryButton(
                text = "Salvar produto",
                onClick = onSaveProduct,
                loading = state.isSaving,
                accent = accent,
                icon = Icons.Outlined.CheckCircle,
            )
        }
    }

    if (state.products.isEmpty()) {
        PremiumEmptyState(
            title = "Nenhum produto cadastrado",
            subtitle = "Adicione o primeiro produto para abrir a loja ${kind.entityArticle} ${kind.entityLabel}.",
            icon = Icons.Outlined.Inventory2,
            accent = accent,
        )
    }

    state.products.forEach { product ->
        PremiumCard(accent = PremiumZinc800, borderAlpha = 0.55f) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    CollectiveDetailLine(
                        "${product.priceLabel} • Estoque ${product.stock} • ${product.visibilityLabel}",
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumSecondaryButton(
                    text = "Editar",
                    onClick = { onOpenProductForm(product) },
                    modifier = Modifier.weight(1f),
                    accent = PremiumZinc400,
                    icon = Icons.Outlined.Edit,
                )
                PremiumSecondaryButton(
                    text = if (product.active) "Ocultar" else "Exibir",
                    onClick = { onToggleProduct(product) },
                    modifier = Modifier.weight(1f),
                    accent = PremiumPurple,
                )
            }
        }
    }
}

@Composable
private fun CollectiveStoreOrders(
    state: CollectiveStoreAdminUiState,
    accent: Color,
    onApproveOrder: (CollectiveStoreOrder) -> Unit,
    onOrderStatus: (CollectiveStoreOrder, String) -> Unit,
) {
    val kind = state.kind
    val isPending = state.mode == CollectiveStoreMode.PendingOrders

    PremiumCard(accent = accent, borderAlpha = 0.22f) {
        Text(
            text = state.mode.title.uppercase(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
        CollectiveDetailLine(
            "Pedidos da loja geral filtrados pelos produtos ${kind.entityArticle} ${kind.entityLabel}.",
        )
    }

    if (state.orders.isEmpty()) {
        PremiumEmptyState(
            title = "Nenhum pedido encontrado",
            subtitle = "Assim que houver pedidos desses produtos, eles aparecem aqui.",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            accent = accent,
        )
        return
    }

    state.orders.forEach { order ->
        val busy = state.busyOrderId == order.id
        PremiumCard(accent = PremiumZinc800, borderAlpha = 0.55f) {
            Text(
                text = order.productName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
            CollectiveDetailLine("Comprador: ${order.userName} • ${order.createdAtLabel}")
            CollectiveDetailLine("Qtd ${order.quantity} • ${order.totalLabel}")

            if (isPending) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumSecondaryButton(
                        text = "Aprovar",
                        onClick = { onApproveOrder(order) },
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        accent = PremiumBrand,
                        icon = Icons.Outlined.CheckCircle,
                    )
                    PremiumSecondaryButton(
                        text = "Rejeitar",
                        onClick = { onOrderStatus(order, "rejected") },
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        accent = PremiumRed,
                        icon = Icons.Outlined.Close,
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumSecondaryButton(
                        text = "Reabrir",
                        onClick = { onOrderStatus(order, "pendente") },
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        accent = PremiumGold,
                    )
                    PremiumSecondaryButton(
                        text = "Entregue",
                        onClick = { onOrderStatus(order, "delivered") },
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        accent = PremiumBrand,
                        icon = Icons.Outlined.CheckCircle,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Gestão / BI
// ------------------------------------------------------------------

/**
 * Web: `app/ligas/_components/LeagueFinanceDashboard.tsx` com `view="hub"`.
 *
 * O `view="produtos"` saiu daqui no M8.3: é `ProductManagementAnalytics`, hoje na
 * `ProductBiScreen`, com o mesmo motor dos outros quatro players.
 */
@Composable
fun CollectiveFinanceScreen(
    state: CollectiveFinanceUiState,
    onBackClick: () -> Unit,
    onNavClick: (CollectiveManagementNav) -> Unit,
    modifier: Modifier = Modifier,
    onFrequencyClick: () -> Unit = {},
    onEventsBiClick: () -> Unit = {},
    onProductsBiClick: () -> Unit = {},
    onStatementClick: () -> Unit = {},
    onScannerClick: () -> Unit = {},
) {
    val collective = state.collective
    if (state.isLoading || collective == null) {
        PremiumLoadingState(text = "Carregando gestão", modifier = modifier)
        return
    }

    val kind = state.kind
    val accent = collectiveAccent(kind)

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        CollectiveManagementHeader(
            collective = collective,
            onBackClick = onBackClick,
            eyebrow = "Gestão financeira",
        )
        CollectiveManagementQuickNav(
            kind = kind,
            active = CollectiveManagementNav.Finance,
            onNavClick = onNavClick,
        )

        state.errorMessage?.let { CollectiveBanner(text = it, accent = PremiumRed) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CollectiveMetricCard(
                label = "Receita total",
                value = money(state.totalRevenue),
                hint = "${state.totalQuantity} itens vendidos",
                icon = Icons.Outlined.Wallet,
                modifier = Modifier.weight(1f),
                accent = accent,
            )
            CollectiveMetricCard(
                label = "Produtos",
                value = money(state.productRevenue),
                hint = "${state.productQuantity} produtos aprovados",
                icon = Icons.Outlined.Inventory2,
                modifier = Modifier.weight(1f),
                accent = accent,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CollectiveMetricCard(
                label = "Eventos",
                value = money(state.eventRevenue),
                hint = "${state.eventQuantity} ingressos aprovados",
                icon = Icons.Outlined.CalendarMonth,
                modifier = Modifier.weight(1f),
                accent = accent,
            )
            CollectiveMetricCard(
                label = "Catálogo",
                value = state.catalogCount.toString(),
                hint = "produtos cadastrados ${kind.entityArticle} ${kind.entityLabel}",
                icon = Icons.Outlined.QueryStats,
                modifier = Modifier.weight(1f),
                accent = accent,
            )
        }

        // Os quatro atalhos do `view === "hub"` (707-750) e o botão do scanner.
        CollectiveActionCard(
            eyebrow = "Frequência",
            title = "Frequência",
            description = "Matriz de presença por membro e evento.",
            icon = Icons.Outlined.CheckCircle,
            accent = PremiumBrand,
            onClick = onFrequencyClick,
        )
        CollectiveActionCard(
            eyebrow = "Eventos",
            title = "Eventos",
            description = "Funil, aprovação, portaria, recorrência e lotes.",
            icon = Icons.Outlined.CalendarMonth,
            accent = PremiumAmber,
            onClick = onEventsBiClick,
        )
        CollectiveActionCard(
            eyebrow = "Produtos",
            title = "Produtos",
            description = "Receita, estoque, recompra, conversão e curva ABC.",
            icon = Icons.Outlined.Inventory2,
            accent = PremiumPurple,
            onClick = onProductsBiClick,
        )
        CollectiveActionCard(
            eyebrow = "Financeiro",
            title = "Financeiro",
            description = "Extrato completo com filtros.",
            icon = Icons.Outlined.AccountBalanceWallet,
            accent = PremiumGold,
            onClick = onStatementClick,
        )
        PremiumPrimaryButton(
            text = "Abrir scanner de QR",
            onClick = onScannerClick,
            accent = accent,
        )
    }
}

// ------------------------------------------------------------------
// Frequência
// ------------------------------------------------------------------

/** Web: `app/ligas/_components/LeagueFrequencyPage.tsx`. */
@Composable
fun CollectiveFrequencyScreen(
    state: CollectiveFrequencyUiState,
    onBackClick: () -> Unit,
    onNavClick: (CollectiveManagementNav) -> Unit,
    onFilterChange: (CollectiveFrequencyFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val collective = state.collective
    if (state.isLoading || collective == null) {
        PremiumLoadingState(text = "Carregando frequência", modifier = modifier)
        return
    }

    val kind = state.kind
    val accent = collectiveAccent(kind)

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        CollectiveManagementHeader(
            collective = collective,
            onBackClick = onBackClick,
            eyebrow = "Frequência",
        )
        CollectiveManagementQuickNav(
            kind = kind,
            active = CollectiveManagementNav.Finance,
            onNavClick = onNavClick,
        )

        state.errorMessage?.let { CollectiveBanner(text = it, accent = PremiumRed) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CollectiveMetricCard(
                label = "Membros",
                value = state.members.size.toString(),
                hint = "na lista de presença",
                icon = Icons.Outlined.Groups,
                modifier = Modifier.weight(1f),
                accent = accent,
            )
            CollectiveMetricCard(
                label = "Presenças",
                value = state.presentCount.toString(),
                hint = "${state.approvedCount} com ingresso aprovado",
                icon = Icons.Outlined.CheckCircle,
                modifier = Modifier.weight(1f),
                accent = accent,
            )
        }

        CollectiveOptionRow(
            options = CollectiveFrequencyFilter.entries.map { it.label },
            selected = state.filter.label,
            onSelect = { label ->
                onFilterChange(CollectiveFrequencyFilter.entries.first { it.label == label })
            },
            accent = accent,
        )

        // O ajuste manual continua no painel web: a rota `/api/admin/ligas/frequency`
        // roda com service role e não foi portada.
        PremiumCard(accent = PremiumAmber, borderAlpha = 0.22f) {
            Text(
                text = "Ajuste manual".uppercase(),
                color = PremiumAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
            )
            CollectiveDetailLine(
                "O app mostra ${state.manualEntryCount} ajuste(s) manual(is) já gravado(s). " +
                    "Registrar novo ajuste continua no painel web.",
            )
        }

        if (state.members.isEmpty() || state.filteredEvents.isEmpty()) {
            PremiumEmptyState(
                title = "Sem frequência",
                subtitle = "Cadastre membros e eventos ${kind.entityArticle} ${kind.entityLabel} para " +
                    "montar a matriz de presença.",
                icon = Icons.Outlined.CheckCircle,
                accent = accent,
            )
            return@PremiumScreen
        }

        state.members.forEach { member ->
            PremiumCard(accent = PremiumZinc800, borderAlpha = 0.55f) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MemberAvatar(photoUrl = member.photoUrl, accent = accent)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        CollectiveDetailLine("${member.turma} - ${member.role}")
                    }
                }

                state.filteredEvents.forEach { event ->
                    val status = state.statusFor(event.key, member.id)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                color = PremiumZinc400,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            CollectiveDetailLine(event.visibilityLabel)
                        }
                        PremiumChip(
                            label = status.label,
                            accent = status.accent,
                        )
                    }
                }
            }
        }
    }
}

private val CollectiveFrequencyStatus.accent: Color
    get() = when (this) {
        CollectiveFrequencyStatus.Present -> PremiumBrand
        CollectiveFrequencyStatus.Approved -> PremiumPurple
        CollectiveFrequencyStatus.Absent -> PremiumRed
        CollectiveFrequencyStatus.Justified -> PremiumAmber
        CollectiveFrequencyStatus.None -> PremiumZinc800
    }

// ------------------------------------------------------------------
// Extrato
// ------------------------------------------------------------------

/** Web: `components/financeiro/FinancialStatementPage.tsx` no escopo do coletivo. */
@Composable
fun CollectiveStatementScreen(
    state: CollectiveStatementUiState,
    onBackClick: () -> Unit,
    onNavClick: (CollectiveManagementNav) -> Unit,
    onSearchChange: (String) -> Unit,
    onTypeFilter: (CollectiveStatementType?) -> Unit,
    onStatusFilter: (CollectiveStatementStatus?) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val collective = state.collective
    if (state.isLoading || collective == null) {
        PremiumLoadingState(text = "Carregando financeiro", modifier = modifier)
        return
    }

    val kind = state.kind
    val accent = collectiveAccent(kind)

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        CollectiveManagementHeader(
            collective = collective,
            onBackClick = onBackClick,
            eyebrow = "Financeiro ${kind.entityArticle} ${kind.entityLabel}",
        )
        CollectiveManagementQuickNav(
            kind = kind,
            active = CollectiveManagementNav.Finance,
            onNavClick = onNavClick,
        )

        state.errorMessage?.let { CollectiveBanner(text = it, accent = PremiumRed) }

        PremiumCard(accent = accent, borderAlpha = 0.22f) {
            CollectiveDetailLine(
                "Extrato isolado ${kind.entityArticle} ${kind.entityLabel}: loja e eventos " +
                    "vinculados somente a esta entidade.",
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CollectiveMetricCard(
                label = "Faturamento",
                value = money(state.totalValue),
                hint = "${state.filteredRows.size} lançamentos",
                icon = Icons.Outlined.Wallet,
                modifier = Modifier.weight(1f),
                accent = accent,
            )
            CollectiveMetricCard(
                label = "Aprovado",
                value = money(state.approvedValue),
                hint = "descontos ${money(state.totalDiscount)}",
                icon = Icons.Outlined.CheckCircle,
                modifier = Modifier.weight(1f),
                accent = accent,
            )
        }

        PremiumTextField(
            value = state.searchTerm,
            onValueChange = onSearchChange,
            label = "Buscar cliente, item ou lote",
            leadingIcon = Icons.Outlined.Search,
        )

        CollectiveOptionRow(
            options = listOf("Todos") + CollectiveStatementType.entries.map { it.label },
            selected = state.typeFilter?.label ?: "Todos",
            onSelect = { label ->
                onTypeFilter(CollectiveStatementType.entries.firstOrNull { it.label == label })
            },
            accent = accent,
        )
        CollectiveOptionRow(
            options = listOf("Todos") + CollectiveStatementStatus.entries.map { it.label },
            selected = state.statusFilter?.label ?: "Todos",
            onSelect = { label ->
                onStatusFilter(CollectiveStatementStatus.entries.firstOrNull { it.label == label })
            },
            accent = PremiumPurple,
        )

        if (state.filteredRows.isEmpty()) {
            PremiumEmptyState(
                title = "Sem lançamentos",
                subtitle = "Nenhum movimento encontrado para os filtros escolhidos.",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                accent = accent,
            )
            return@PremiumScreen
        }

        state.pageRows.forEach { row ->
            PremiumCard(accent = PremiumZinc800, borderAlpha = 0.55f) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PremiumChip(label = row.type.label, accent = accent)
                    PremiumChip(label = row.statusGroup.label, accent = row.statusGroup.accent)
                }
                Text(
                    text = row.item,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                CollectiveDetailLine(
                    "${row.client}${if (row.clientTurma.isBlank()) "" else " • ${row.clientTurma}"}",
                )
                CollectiveDetailLine("Lote ${row.lot} • ${row.category} • Qtd ${row.quantity}")
                CollectiveDetailLine("Pedido ${row.orderedAtLabel}")
                if (row.approvedAtLabel.isNotBlank() && row.approvedAtLabel != "Não informado") {
                    CollectiveDetailLine(
                        "Aprovado ${row.approvedAtLabel}" +
                            if (row.approvedBy.isBlank()) "" else " por ${row.approvedBy}",
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CollectiveDetailLine(
                        row.paymentSource.ifBlank { "Fonte não informada" },
                    )
                    Text(
                        text = money(row.value),
                        color = accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        if (state.hasMore) {
            PremiumSecondaryButton(
                text = "Carregar mais",
                onClick = onLoadMore,
                accent = accent,
            )
        }
    }
}

private val CollectiveStatementStatus.accent: Color
    get() = when (this) {
        CollectiveStatementStatus.Approved -> PremiumBrand
        CollectiveStatementStatus.Pending -> PremiumGold
        CollectiveStatementStatus.Rejected -> PremiumRed
        CollectiveStatementStatus.Cancelled -> PremiumRed
        CollectiveStatementStatus.Other -> PremiumZinc800
    }

// ------------------------------------------------------------------
// Comum
// ------------------------------------------------------------------

@Composable
private fun CollectiveBanner(text: String, accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
