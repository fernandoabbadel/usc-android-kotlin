package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
import com.example.usc1.core.ui.PremiumBrandAccent
import com.example.usc1.core.ui.PremiumCard
import com.example.usc1.core.ui.PremiumChip
import com.example.usc1.core.ui.PremiumEmptyState
import com.example.usc1.core.ui.PremiumHeader
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.AdminPartnersTierCounts
import com.example.usc1.domain.model.PartnerForm
import com.example.usc1.domain.model.PartnerRecord
import com.example.usc1.domain.model.PartnerScanRecord
import com.example.usc1.domain.model.PartnerStatus
import com.example.usc1.domain.model.PartnerTier
import java.text.NumberFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminPartnersHubScreen(
    onActiveClick: () -> Unit,
    onCompaniesClick: () -> Unit,
    onBiClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCreatePartnerClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
        verticalSpacing = 16.dp,
    ) {
        PremiumHeader(
            title = "Admin Parceiros",
            subtitle = "Menu leve: sem carregar tabelas nesta tela",
            icon = Icons.Outlined.Storefront,
            accent = PremiumBrand,
            onBackClick = onBackClick,
        )

        PremiumSecondaryButton(
            text = "Criar Parceiro",
            onClick = onCreatePartnerClick,
            icon = Icons.Outlined.Business,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AdminPartnerHubCard(
                title = "Parceiros Ativos",
                description = "Resumo por plano e status",
                icon = Icons.Outlined.BarChart,
                accent = Color(0xFF34D399),
                onClick = onActiveClick,
            )
            AdminPartnerHubCard(
                title = "Empresas",
                description = "Tabela paginada de parceiros",
                icon = Icons.Outlined.Business,
                accent = Color(0xFF60A5FA),
                onClick = onCompaniesClick,
            )
            AdminPartnerHubCard(
                title = "Dados Cadastrais",
                description = "Campos administrativos e contatos",
                icon = Icons.Outlined.PieChart,
                accent = PremiumAmber,
                onClick = onBiClick,
            )
            AdminPartnerHubCard(
                title = "Histórico",
                description = "Scans validados 20 em 20",
                icon = Icons.Outlined.History,
                accent = Color(0xFFC084FC),
                onClick = onHistoryClick,
            )
        }
    }
}

@Composable
fun AdminPartnersActiveScreen(
    state: AdminPartnersUiState,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.partners.isEmpty()) {
        PremiumLoadingState(text = "Carregando...", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 16.dp) {
        AdminPartnersHeader(state = state, icon = Icons.Outlined.BarChart, onBackClick = onBackClick)
        AdminPartnersMessages(state)
        CountsGrid(counts = state.counts)
        TierCountsGrid(counts = state.counts)
        Text(text = "Empresa | Categoria | Plano | Scans", color = PremiumZinc500, fontSize = 10.sp, fontWeight = FontWeight.Black)
        if (state.partners.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum parceiro ativo encontrado.",
                subtitle = "A consulta paginada do tenant ativo não retornou parceiros ativos.",
                icon = Icons.Outlined.Storefront,
                accent = PremiumBrand,
            )
        } else {
            state.partners.forEach { partner ->
                PartnerActiveRow(partner = partner)
            }
        }
        AdminPartnersPager(state = state, onPreviousPageClick = onPreviousPageClick, onNextPageClick = onNextPageClick)
        PremiumSecondaryButton(text = "Atualizar", onClick = onRefreshClick, icon = Icons.Outlined.Refresh)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminPartnersCompaniesScreen(
    state: AdminPartnersUiState,
    onSearchChange: (String) -> Unit,
    onStatusFilterClick: (PartnerStatus?) -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (PartnerRecord) -> Unit,
    onResetPasswordClick: (PartnerRecord) -> Unit,
    onToggleStatusClick: (PartnerRecord) -> Unit,
    onFormChange: ((PartnerForm) -> PartnerForm) -> Unit,
    onCloseFormClick: () -> Unit,
    onSaveFormClick: () -> Unit,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.partners.isEmpty()) {
        PremiumLoadingState(text = "Carregando...", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 16.dp) {
        AdminPartnersHeader(state = state, icon = Icons.Outlined.Business, onBackClick = onBackClick)
        AdminPartnersMessages(state)
        PremiumTextField(
            value = state.search,
            onValueChange = onSearchChange,
            label = "Buscar empresa",
        )
        PremiumSecondaryButton(text = "Criar Parceiro", onClick = onCreateClick, icon = Icons.Outlined.Business)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusFilterChip("Todos", state.statusFilter == null) { onStatusFilterClick(null) }
            StatusFilterChip("Ativos", state.statusFilter == PartnerStatus.Active) { onStatusFilterClick(PartnerStatus.Active) }
            StatusFilterChip("Pendentes", state.statusFilter == PartnerStatus.Pending) { onStatusFilterClick(PartnerStatus.Pending) }
            StatusFilterChip("Desativados", state.statusFilter == PartnerStatus.Disabled) { onStatusFilterClick(PartnerStatus.Disabled) }
        }
        Text(text = "Empresa | Categoria | Plano | Status | Scans | Ações", color = PremiumZinc500, fontSize = 10.sp, fontWeight = FontWeight.Black)
        if (state.filteredPartners.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhuma empresa encontrada.",
                subtitle = "A busca/filtro atual não retornou parceiros para este tenant.",
                icon = Icons.Outlined.Business,
                accent = Color(0xFF60A5FA),
            )
        } else {
            state.filteredPartners.forEach { partner ->
                PartnerCompanyRow(
                    partner = partner,
                    isBusy = state.mutatingId == partner.id || state.mutatingId == "reset:${partner.id}",
                    onEditClick = { onEditClick(partner) },
                    onResetPasswordClick = { onResetPasswordClick(partner) },
                    onToggleStatusClick = { onToggleStatusClick(partner) },
                )
            }
        }
        AdminPartnersPager(state = state, onPreviousPageClick = onPreviousPageClick, onNextPageClick = onNextPageClick)
        PremiumSecondaryButton(text = "Atualizar", onClick = onRefreshClick, icon = Icons.Outlined.Refresh)
        state.form?.let { form ->
            PartnerFormCard(
                form = form,
                saving = state.mutatingId == "form",
                onFormChange = onFormChange,
                onCloseClick = onCloseFormClick,
                onSaveClick = onSaveFormClick,
            )
        }
    }
}

@Composable
fun AdminPartnersBiScreen(
    state: AdminPartnersUiState,
    onMetricClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando BI...", modifier = modifier)
        return
    }
    val partnerScanData = state.partnerScanData()
    val couponUsageData = state.couponUsageData()
    val qrTypeData = state.qrTypeData()
    val topUsers = state.topUsers().take(8)
    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 16.dp) {
        AdminPartnersHeader(state = state, icon = Icons.Outlined.PieChart, onBackClick = onBackClick)
        AdminPartnersMessages(state)
        PremiumSecondaryButton(
            text = "Métrica: ${state.biMetric.label}",
            onClick = onMetricClick,
            icon = Icons.Outlined.BarChart,
        )
        BiSection(title = "Scans por parceiro", rows = partnerScanData)
        BiSection(title = "Cupons mais usados", rows = couponUsageData)
        BiSection(title = "Tipos mais usados de QR Code", rows = qrTypeData)
        PremiumCard(accent = Color(0xFF60A5FA), containerColor = PremiumZinc900) {
            Text(text = "Usuários que mais usaram cupons", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
            topUsers.forEach { row ->
                InfoLine(
                    label = "${row.user} (${row.userId.ifBlank { "-" }})",
                    value = "${row.scans} scans | ${formatCurrency(row.value)}",
                )
            }
            if (topUsers.isEmpty()) {
                Text(text = "Nenhum uso registrado.", color = PremiumZinc500, fontSize = 12.sp)
            }
        }
        PremiumSecondaryButton(text = "Atualizar", onClick = onRefreshClick, icon = Icons.Outlined.Refresh)
    }
}

@Composable
fun AdminPartnersHistoryScreen(
    state: AdminPartnersUiState,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.scans.isEmpty()) {
        PremiumLoadingState(text = "Carregando...", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 110.dp, verticalSpacing = 16.dp) {
        AdminPartnersHeader(state = state, icon = Icons.Outlined.History, onBackClick = onBackClick)
        AdminPartnersMessages(state)
        Text(
            text = "Parceiro | ID da leitura | ID do cupom | Cupom | Usuário | Data | Hora | Método | Aprovação | Código QR | Tipo | Valor",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
        )
        if (state.scans.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum scan encontrado.",
                subtitle = "A consulta paginada do tenant ativo não retornou leituras.",
                icon = Icons.Outlined.History,
                accent = Color(0xFFC084FC),
            )
        } else {
            state.scans.forEach { scan ->
                PartnerScanRow(scan = scan)
            }
        }
        AdminPartnersPager(state = state, onPreviousPageClick = onPreviousPageClick, onNextPageClick = onNextPageClick)
        PremiumSecondaryButton(text = "Atualizar", onClick = onRefreshClick, icon = Icons.Outlined.Refresh)
    }
}

@Composable
private fun AdminPartnerHubCard(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(11.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(text = description, color = PremiumZinc400, fontSize = 12.sp)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = PremiumZinc500)
        }
    }
}

@Composable
private fun AdminPartnersHeader(
    state: AdminPartnersUiState,
    icon: ImageVector,
    onBackClick: () -> Unit,
) {
    PremiumHeader(
        title = state.title,
        subtitle = state.subtitle,
        icon = icon,
        accent = PremiumBrand,
        onBackClick = onBackClick,
    )
}

@Composable
private fun AdminPartnersMessages(state: AdminPartnersUiState) {
    state.actionMessage?.let { message ->
        Text(text = message, color = PremiumBrandAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    state.errorMessage?.let { message ->
        Text(text = message, color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CountsGrid(counts: AdminPartnersTierCounts) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CountCard("Total", counts.total.toString(), Color.White, Modifier.weight(1f))
        CountCard("Ativos", counts.active.toString(), Color(0xFF6EE7B7), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CountCard("Pendentes", counts.pending.toString(), Color(0xFFFDE68A), Modifier.weight(1f))
        CountCard("Desativados", counts.disabled.toString(), Color(0xFFFCA5A5), Modifier.weight(1f))
    }
}

@Composable
private fun TierCountsGrid(counts: AdminPartnersTierCounts) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CountCard("Ouro", counts.gold.toString(), PremiumAmber, Modifier.weight(1f))
        CountCard("Prata", counts.silver.toString(), PremiumZinc400, Modifier.weight(1f))
        CountCard("Standard", counts.standard.toString(), PremiumBrand, Modifier.weight(1f))
    }
}

@Composable
private fun CountCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PartnerActiveRow(partner: PartnerRecord) {
    PremiumCard(accent = tierColor(partner.tier), containerColor = PremiumZinc900) {
        InfoLine(label = "Empresa", value = partner.name)
        InfoLine(label = "Categoria", value = partner.category.ifBlank { "-" })
        InfoLine(label = "Plano", value = partner.tier.remoteValue.uppercase())
        InfoLine(label = "Scans", value = partner.totalScans.toString())
    }
}

@Composable
private fun PartnerCompanyRow(
    partner: PartnerRecord,
    isBusy: Boolean,
    onEditClick: () -> Unit,
    onResetPasswordClick: () -> Unit,
    onToggleStatusClick: () -> Unit,
) {
    PremiumCard(accent = statusColor(partner.status), containerColor = PremiumZinc900) {
        InfoLine(label = "Empresa", value = partner.name)
        InfoLine(label = "E-mail", value = partner.email.ifBlank { "-" })
        InfoLine(label = "Categoria", value = partner.category.ifBlank { "-" })
        InfoLine(label = "Plano", value = partner.tier.remoteValue.uppercase())
        InfoLine(label = "Status", value = partner.status.label)
        InfoLine(label = "Scans", value = partner.totalScans.toString())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumSecondaryButton(text = "Editar", onClick = onEditClick, icon = Icons.Outlined.Edit, modifier = Modifier.weight(1f))
            PremiumSecondaryButton(text = "Reset", onClick = onResetPasswordClick, icon = Icons.Outlined.Key, enabled = !isBusy, modifier = Modifier.weight(1f))
            PremiumSecondaryButton(text = "Status", onClick = onToggleStatusClick, icon = Icons.Outlined.PowerSettingsNew, enabled = !isBusy, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PartnerScanRow(scan: PartnerScanRecord) {
    PremiumCard(accent = Color(0xFFC084FC), containerColor = PremiumZinc900) {
        InfoLine(label = "Parceiro", value = scan.companyName)
        InfoLine(label = "ID da leitura", value = scan.id, mono = true)
        InfoLine(label = "ID do cupom", value = scan.couponId.ifBlank { "-" }, mono = true)
        InfoLine(label = "Cupom", value = scan.couponTitle.ifBlank { scan.couponName })
        InfoLine(label = "Usuário", value = scan.userDisplayName.ifBlank { scan.userName })
        InfoLine(label = "Data", value = scan.date.ifBlank { "-" })
        InfoLine(label = "Hora", value = scan.hour.ifBlank { "-" })
        InfoLine(label = "Método", value = methodLabel(scan.scanMethod))
        InfoLine(label = "Aprovação", value = approvalLabel(scan.approvalMode))
        InfoLine(label = "Código QR", value = scan.qrCode.ifBlank { "-" }, mono = true)
        InfoLine(label = "Tipo", value = couponTypeLabel(scan.couponType))
        InfoLine(label = "Valor", value = scan.couponValue.ifBlank { scan.savedValueLabel })
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PartnerFormCard(
    form: PartnerForm,
    saving: Boolean,
    onFormChange: ((PartnerForm) -> PartnerForm) -> Unit,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    PremiumCard(accent = PremiumBrand, containerColor = PremiumZinc900) {
        Text(
            text = if (form.partnerId.isBlank()) "Criar Parceiro" else "Editar Parceiro",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = if (form.partnerId.isBlank()) "Cadastro completo da empresa parceira" else "Campos de cadastro, página pública e painel da empresa",
            color = PremiumZinc500,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        PremiumTextField(value = form.name, onValueChange = { value -> onFormChange { it.copy(name = value) } }, label = "Nome")
        PremiumTextField(value = form.category, onValueChange = { value -> onFormChange { it.copy(category = value) } }, label = "Categoria")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TierChip("Ouro", form.tier == PartnerTier.Ouro) { onFormChange { it.copy(tier = PartnerTier.Ouro) } }
            TierChip("Prata", form.tier == PartnerTier.Prata) { onFormChange { it.copy(tier = PartnerTier.Prata) } }
            TierChip("Standard", form.tier == PartnerTier.Standard) { onFormChange { it.copy(tier = PartnerTier.Standard) } }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusFilterChip("Ativo", form.status == PartnerStatus.Active) { onFormChange { it.copy(status = PartnerStatus.Active) } }
            StatusFilterChip("Pendente", form.status == PartnerStatus.Pending) { onFormChange { it.copy(status = PartnerStatus.Pending) } }
            StatusFilterChip("Desativado", form.status == PartnerStatus.Disabled) { onFormChange { it.copy(status = PartnerStatus.Disabled) } }
        }
        PremiumTextField(value = form.responsible, onValueChange = { value -> onFormChange { it.copy(responsible = value) } }, label = "Responsável")
        PremiumTextField(value = form.cnpj, onValueChange = { value -> onFormChange { it.copy(cnpj = value) } }, label = "CNPJ")
        PremiumTextField(value = form.email, onValueChange = { value -> onFormChange { it.copy(email = value) } }, label = "E-mail")
        PremiumTextField(value = form.phone, onValueChange = { value -> onFormChange { it.copy(phone = value) } }, label = "Telefone")
        PremiumTextField(value = form.description, onValueChange = { value -> onFormChange { it.copy(description = value) } }, label = "Descrição")
        PremiumTextField(value = form.address, onValueChange = { value -> onFormChange { it.copy(address = value) } }, label = "Endereço")
        PremiumTextField(value = form.businessHours, onValueChange = { value -> onFormChange { it.copy(businessHours = value) } }, label = "Horário")
        PremiumTextField(value = form.instagram, onValueChange = { value -> onFormChange { it.copy(instagram = value) } }, label = "Instagram")
        PremiumTextField(value = form.whatsApp, onValueChange = { value -> onFormChange { it.copy(whatsApp = value) } }, label = "WhatsApp")
        PremiumTextField(value = form.site, onValueChange = { value -> onFormChange { it.copy(site = value) } }, label = "Site")
        PremiumTextField(value = form.logoUrl, onValueChange = { value -> onFormChange { it.copy(logoUrl = value) } }, label = "Logo do parceiro")
        PremiumTextField(value = form.coverUrl, onValueChange = { value -> onFormChange { it.copy(coverUrl = value) } }, label = "Capa do parceiro")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(text = "Cancelar", onClick = onCloseClick, modifier = Modifier.weight(1f))
            PremiumPrimaryButton(text = if (form.partnerId.isBlank()) "Criar parceiro" else "Salvar parceiro", onClick = onSaveClick, loading = saving, icon = Icons.Outlined.Save, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AdminPartnersPager(
    state: AdminPartnersUiState,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit,
) {
    if (state.page <= 1 && !state.hasMore) return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Página ${state.page}",
            color = PremiumZinc500,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
        )
        PremiumSecondaryButton(text = "Anterior", onClick = onPreviousPageClick, enabled = state.page > 1, modifier = Modifier.weight(1f))
        PremiumSecondaryButton(text = "Próxima", onClick = onNextPageClick, enabled = state.hasMore, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatusFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = if (selected) Color.White else PremiumZinc400
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color.White else PremiumZinc900,
        border = BorderStroke(1.dp, if (selected) Color.White else PremiumZinc800),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label.uppercase(),
            color = if (selected) Color.Black else accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun TierChip(label: String, selected: Boolean, onClick: () -> Unit) {
    StatusFilterChip(label = label, selected = selected, onClick = onClick)
}

@Composable
private fun InfoLine(label: String, value: String, mono: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(0.38f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(0.62f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BiSection(title: String, rows: List<BiRow>) {
    PremiumCard(accent = PremiumBrand, containerColor = PremiumZinc900) {
        Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        if (rows.isEmpty()) {
            Text(text = "Nenhum uso registrado.", color = PremiumZinc500, fontSize = 12.sp)
        } else {
            rows.take(12).forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = row.label, color = PremiumZinc400, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = row.displayValue, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .background(PremiumZinc800, RoundedCornerShape(999.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(row.fraction)
                                .height(7.dp)
                                .background(PremiumBrand, RoundedCornerShape(999.dp)),
                        )
                    }
                }
            }
        }
    }
}

private data class BiRow(
    val label: String,
    val quantity: Int,
    val value: Double,
    val metricValue: Double,
    val maxMetricValue: Double,
) {
    val fraction: Float
        get() = if (maxMetricValue <= 0.0) 0f else (metricValue / maxMetricValue).toFloat().coerceIn(0.04f, 1f)
    val displayValue: String
        get() = if (value > 0.0 && metricValue == value) formatCurrency(value) else quantity.toString()
}

private data class TopUserRow(
    val user: String,
    val userId: String,
    val scans: Int,
    val value: Double,
)

private fun AdminPartnersUiState.partnerScanData(): List<BiRow> {
    val names = partners.associate { it.id to it.name }
    return scans.groupBy { it.companyId }
        .map { (partnerId, rows) ->
            val value = rows.sumOf { it.moneyValue() }
            BiRow(
                label = names[partnerId] ?: rows.firstOrNull()?.companyName ?: "Parceiro",
                quantity = rows.size,
                value = value,
                metricValue = if (biMetric == AdminPartnersBiMetric.Quantity) rows.size.toDouble() else value,
                maxMetricValue = 1.0,
            )
        }
        .withMax()
}

private fun AdminPartnersUiState.couponUsageData(): List<BiRow> {
    return scans.groupBy { it.couponTitle.ifBlank { it.couponName.ifBlank { "Cupom" } } }
        .map { (coupon, rows) ->
            val value = rows.sumOf { it.moneyValue() }
            BiRow(
                label = coupon,
                quantity = rows.size,
                value = value,
                metricValue = if (biMetric == AdminPartnersBiMetric.Quantity) rows.size.toDouble() else value,
                maxMetricValue = 1.0,
            )
        }
        .withMax()
}

private fun AdminPartnersUiState.qrTypeData(): List<BiRow> {
    return scans.groupBy { approvalLabel(it.approvalMode) }
        .map { (label, rows) ->
            BiRow(
                label = label,
                quantity = rows.size,
                value = 0.0,
                metricValue = rows.size.toDouble(),
                maxMetricValue = 1.0,
            )
        }
        .withMax()
}

private fun AdminPartnersUiState.topUsers(): List<TopUserRow> {
    return scans.groupBy { it.userId.ifBlank { it.userName } }
        .map { (key, rows) ->
            TopUserRow(
                user = rows.firstOrNull()?.userDisplayName?.ifBlank { rows.firstOrNull()?.userName.orEmpty() }.orEmpty().ifBlank { "Usuário" },
                userId = key,
                scans = rows.size,
                value = rows.sumOf { it.moneyValue() },
            )
        }
        .sortedWith(compareByDescending<TopUserRow> { it.scans }.thenByDescending { it.value })
}

private fun List<BiRow>.withMax(): List<BiRow> {
    val maxValue = maxOfOrNull { it.metricValue } ?: 0.0
    return map { it.copy(maxMetricValue = maxValue) }
        .sortedByDescending { it.metricValue }
}

private fun PartnerScanRecord.moneyValue(): Double {
    if (couponValueNumeric > 0.0) return couponValueNumeric
    val raw = couponValue.ifBlank { savedValueLabel }
    if (raw.contains("%")) return 0.0
    return raw
        .replace(Regex("[^\\d,.-]"), "")
        .replace(".", "")
        .replace(",", ".")
        .toDoubleOrNull()
        ?: 0.0
}

private fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}

private fun tierColor(tier: PartnerTier): Color {
    return when (tier) {
        PartnerTier.Ouro -> PremiumAmber
        PartnerTier.Prata -> PremiumZinc400
        PartnerTier.Standard -> PremiumBrand
    }
}

private fun statusColor(status: PartnerStatus): Color {
    return when (status) {
        PartnerStatus.Active -> PremiumBrand
        PartnerStatus.Pending -> PremiumAmber
        PartnerStatus.Disabled -> Color(0xFFF87171)
    }
}

private fun methodLabel(value: String): String {
    return if (value == "manual") "Manual" else "QR code"
}

private fun approvalLabel(value: String): String {
    return when (value) {
        "manual_partner" -> "Manual pelo parceiro"
        "printed_qr" -> "Leitura do QR impresso"
        else -> "Direta via scan"
    }
}

private fun couponTypeLabel(value: String): String {
    return when (value) {
        "percentual" -> "% porcentagem de desconto"
        "valor" -> "Valor de desconto"
        else -> "-"
    }
}
