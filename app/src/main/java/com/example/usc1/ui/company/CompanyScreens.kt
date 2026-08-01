package com.example.usc1.ui.company

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.example.usc1.core.ui.PremiumRed
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumTextField
import com.example.usc1.core.ui.PremiumZinc300
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900
import com.example.usc1.domain.model.PartnerCoupon
import com.example.usc1.domain.model.PartnerLeadForm
import com.example.usc1.domain.model.PartnerRegistrationRules
import com.example.usc1.domain.model.PartnerScanRecord
import com.example.usc1.domain.model.PartnerTier

/** `/empresa` — área do parceiro. */
@Composable
fun CompanyLoginScreen(
    state: CompanyLoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRegisterClick: () -> Unit,
    onSupportClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Área do Parceiro",
            subtitle = "Gerencie seus cupons e métricas",
            icon = Icons.Outlined.Storefront,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumBrand) {
            PremiumTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = "Email corporativo",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            PremiumTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = "Senha",
                visualTransformation = PasswordVisualTransformation(),
            )
            state.message?.let { message ->
                PremiumChip(label = message, accent = PremiumAmber)
            }
            PremiumPrimaryButton(
                text = "Acessar painel",
                onClick = onSubmit,
                loading = state.isSubmitting,
                icon = Icons.Outlined.Login,
            )
        }

        PremiumSecondaryButton(
            text = "Quero me cadastrar",
            onClick = onRegisterClick,
            icon = Icons.Outlined.Business,
        )
        PremiumSecondaryButton(
            text = "Canal de suporte",
            onClick = onSupportClick,
        )
        Text(
            text = "Esqueceu a senha? Peça um código de reset ao suporte. A confirmação do código " +
                "continua no painel web.",
            color = PremiumZinc500,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** `/empresa/{id}` — painel com cupons e últimos scans. */
@Composable
fun CompanyDashboardScreen(
    state: CompanyDashboardUiState,
    onEditClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando painel", modifier = modifier)
        return
    }

    val partner = state.partner
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = partner?.name ?: "Empresa",
            subtitle = partner?.category ?: "Painel do parceiro",
            icon = Icons.Outlined.Storefront,
            onBackClick = onBackClick,
        )

        if (partner == null) {
            PremiumEmptyState(
                title = "Empresa não encontrada",
                subtitle = state.errorMessage ?: "Confira o link do painel do parceiro.",
                icon = Icons.Outlined.Storefront,
            )
            return@PremiumScreen
        }

        if (partner.coverUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
            ) {
                AsyncImage(
                    model = partner.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.7f,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompanyStatTile(
                value = partner.totalScans.toString(),
                label = "Scans",
                modifier = Modifier.weight(1f),
            )
            CompanyStatTile(
                value = partner.coupons.size.toString(),
                label = "Cupons",
                modifier = Modifier.weight(1f),
            )
            CompanyStatTile(
                value = partner.tier.label,
                label = "Plano",
                accent = PremiumGold,
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Editar",
                onClick = onEditClick,
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Edit,
            )
            PremiumSecondaryButton(
                text = "Histórico",
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.History,
            )
        }

        Text(
            text = "CUPONS PUBLICADOS",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
        if (partner.coupons.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum cupom publicado",
                subtitle = "Cadastre cupons na edição da página pública do parceiro.",
                icon = Icons.Outlined.QrCode2,
            )
        } else {
            partner.coupons.forEach { coupon ->
                PremiumCard(accent = if (coupon.active) PremiumBrand else PremiumZinc500) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = coupon.title,
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                        )
                        PremiumChip(
                            label = coupon.valueLabel,
                            accent = if (coupon.active) PremiumBrand else PremiumZinc500,
                            filled = coupon.active,
                        )
                    }
                    if (coupon.rule.isNotBlank()) {
                        Text(
                            text = coupon.rule,
                            color = PremiumZinc400,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Text(
            text = "ÚLTIMOS SCANS",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
        if (state.recentScans.isEmpty()) {
            PremiumEmptyState(
                title = "Nenhum scan registrado",
                subtitle = "As leituras de cupom aparecem aqui assim que forem registradas.",
                icon = Icons.Outlined.QrCode2,
            )
        } else {
            state.recentScans.forEach { scan -> CompanyScanRow(scan = scan) }
        }

        Text(
            text = "O registro de leitura por QR do painel web depende da câmera, que ainda não " +
                "existe no app. Os scans aparecem aqui em modo leitura.",
            color = PremiumZinc500,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** `/empresa/{id}/historico` — paginado de 20 em 20. */
@Composable
fun CompanyHistoryScreen(
    state: CompanyHistoryUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Histórico de Scans",
            subtitle = state.partnerName.ifBlank { "Empresa" },
            icon = Icons.Outlined.History,
            onBackClick = onBackClick,
        )

        when {
            state.isLoading -> PremiumLoadingState(text = "Carregando")
            state.errorMessage != null -> PremiumEmptyState(
                title = "Histórico indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.History,
            )
            state.scans.isEmpty() -> PremiumEmptyState(
                title = "Nenhum scan registrado",
                subtitle = "Nenhuma leitura de cupom nesta página.",
                icon = Icons.Outlined.QrCode2,
            )
            else -> state.scans.forEach { scan -> CompanyScanRow(scan = scan, detailed = true) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PremiumSecondaryButton(
                text = "Anterior",
                onClick = onPreviousPage,
                enabled = state.hasPrevious,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Página ${state.page}",
                color = PremiumZinc400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
            PremiumSecondaryButton(
                text = "Próxima",
                onClick = onNextPage,
                enabled = state.hasMore,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** `/empresa/{id}/editar` — contatos públicos e cupons. */
@Composable
fun CompanyEditScreen(
    state: CompanyEditUiState,
    onWhatsAppChange: (String) -> Unit,
    onInstagramChange: (String) -> Unit,
    onSiteChange: (String) -> Unit,
    onToggleVisibility: (CompanyContactField) -> Unit,
    onCouponChange: (String, (PartnerCoupon) -> PartnerCoupon) -> Unit,
    onAddCoupon: () -> Unit,
    onRemoveCoupon: (String) -> Unit,
    onSave: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando parceiro", modifier = modifier)
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Editar página",
            subtitle = state.partnerName.ifBlank { "Parceiro" },
            icon = Icons.Outlined.Edit,
            onBackClick = onBackClick,
        )

        state.errorMessage?.let { message ->
            PremiumEmptyState(
                title = "Parceiro indisponível",
                subtitle = message,
                icon = Icons.Outlined.Edit,
            )
            return@PremiumScreen
        }

        state.message?.let { message ->
            PremiumChip(label = message, accent = PremiumBrand)
        }

        PremiumCard(accent = PremiumBrand) {
            Text(
                text = "CONTATOS PÚBLICOS",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            PremiumTextField(
                value = state.whatsApp,
                onValueChange = onWhatsAppChange,
                label = "WhatsApp",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            CompanyVisibilityToggle(
                label = "Exibir WhatsApp na página pública",
                checked = state.contactVisibility.whatsApp,
                onClick = { onToggleVisibility(CompanyContactField.WhatsApp) },
            )
            PremiumTextField(
                value = state.instagram,
                onValueChange = onInstagramChange,
                label = "Instagram",
            )
            CompanyVisibilityToggle(
                label = "Exibir Instagram na página pública",
                checked = state.contactVisibility.instagram,
                onClick = { onToggleVisibility(CompanyContactField.Instagram) },
            )
            PremiumTextField(
                value = state.site,
                onValueChange = onSiteChange,
                label = "Site",
            )
            CompanyVisibilityToggle(
                label = "Exibir site na página pública",
                checked = state.contactVisibility.site,
                onClick = { onToggleVisibility(CompanyContactField.Site) },
            )
        }

        Text(
            text = "CUPONS",
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
        state.coupons.forEach { coupon ->
            PremiumCard(accent = PremiumBrand, borderAlpha = 0.2f) {
                PremiumTextField(
                    value = coupon.title,
                    onValueChange = { value -> onCouponChange(coupon.id) { it.copy(title = value) } },
                    label = "Título do cupom",
                )
                PremiumTextField(
                    value = coupon.valueLabel,
                    onValueChange = { value -> onCouponChange(coupon.id) { it.copy(valueLabel = value) } },
                    label = "Valor do desconto",
                )
                PremiumTextField(
                    value = coupon.rule,
                    onValueChange = { value -> onCouponChange(coupon.id) { it.copy(rule = value) } },
                    label = "Regra",
                    singleLine = false,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompanyCouponTypeChip(
                        label = "% Porcentagem",
                        selected = coupon.type != "valor",
                        onClick = { onCouponChange(coupon.id) { it.copy(type = "percentual") } },
                    )
                    CompanyCouponTypeChip(
                        label = "Valor fixo",
                        selected = coupon.type == "valor",
                        onClick = { onCouponChange(coupon.id) { it.copy(type = "valor") } },
                    )
                }
                CompanyVisibilityToggle(
                    label = "Cupom ativo",
                    checked = coupon.active,
                    onClick = { onCouponChange(coupon.id) { it.copy(active = !it.active) } },
                )
                PremiumSecondaryButton(
                    text = "Remover cupom",
                    onClick = { onRemoveCoupon(coupon.id) },
                    accent = PremiumRed,
                    icon = Icons.Outlined.Delete,
                )
            }
        }

        PremiumSecondaryButton(
            text = "Adicionar cupom",
            onClick = onAddCoupon,
            icon = Icons.Outlined.Add,
        )
        PremiumPrimaryButton(
            text = "Salvar",
            onClick = onSave,
            loading = state.isSaving,
            icon = Icons.Outlined.Save,
        )
    }
}

/** `/empresa/cadastro` — plano, dados da empresa e envio do lead. */
@Composable
fun CompanyRegisterScreen(
    state: CompanyRegisterUiState,
    onSelectTier: (PartnerTier) -> Unit,
    onFormChange: ((PartnerLeadForm) -> PartnerLeadForm) -> Unit,
    onBackToPlans: () -> Unit,
    onSubmit: () -> Unit,
    onLoginClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Cadastro de parceiro",
            subtitle = "Passo ${state.step.coerceAtMost(3)} de 3",
            icon = Icons.Outlined.Business,
            onBackClick = onBackClick,
        )

        state.message?.let { message ->
            PremiumChip(
                label = message,
                accent = if (state.createdPartnerId.isNotBlank()) PremiumBrand else PremiumAmber,
            )
        }

        when (state.step) {
            1 -> {
                Text(
                    text = "ESCOLHA O PLANO",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                CompanyTierCard(
                    tier = PartnerTier.Ouro,
                    price = "R$ 500",
                    accent = PremiumGold,
                    onClick = { onSelectTier(PartnerTier.Ouro) },
                )
                CompanyTierCard(
                    tier = PartnerTier.Prata,
                    price = "R$ 250",
                    accent = PremiumZinc300,
                    onClick = { onSelectTier(PartnerTier.Prata) },
                )
                CompanyTierCard(
                    tier = PartnerTier.Standard,
                    price = "Grátis",
                    accent = PremiumBrand,
                    onClick = { onSelectTier(PartnerTier.Standard) },
                )
                PremiumSecondaryButton(
                    text = "Já sou parceiro",
                    onClick = onLoginClick,
                    icon = Icons.Outlined.Login,
                )
            }

            2 -> {
                PremiumCard(accent = PremiumBrand) {
                    PremiumChip(
                        label = "Plano ${state.form.tier.label}",
                        accent = PremiumGold,
                        filled = true,
                    )
                    PremiumTextField(
                        value = state.form.name,
                        onValueChange = { value -> onFormChange { it.copy(name = value) } },
                        label = "Nome fantasia",
                    )
                    PremiumTextField(
                        value = state.form.cnpj,
                        onValueChange = { value ->
                            onFormChange { it.copy(cnpj = PartnerRegistrationRules.formatCnpj(value)) }
                        },
                        label = "CNPJ",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    PremiumTextField(
                        value = state.form.responsible,
                        onValueChange = { value -> onFormChange { it.copy(responsible = value) } },
                        label = "Responsável",
                    )
                    PremiumTextField(
                        value = state.form.cpf,
                        onValueChange = { value ->
                            onFormChange { it.copy(cpf = PartnerRegistrationRules.formatCpf(value)) }
                        },
                        label = "CPF do responsável",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    PremiumTextField(
                        value = state.form.email,
                        onValueChange = { value -> onFormChange { it.copy(email = value) } },
                        label = "Email corporativo",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    PremiumTextField(
                        value = state.form.phone,
                        onValueChange = { value ->
                            onFormChange { it.copy(phone = PartnerRegistrationRules.formatPhone(value)) }
                        },
                        label = "Telefone (55 + DDD + número)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                    PremiumTextField(
                        value = state.form.password,
                        onValueChange = { value -> onFormChange { it.copy(password = value) } },
                        label = "Senha (mínimo 8 caracteres)",
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    PremiumTextField(
                        value = state.form.passwordConfirmation,
                        onValueChange = { value -> onFormChange { it.copy(passwordConfirmation = value) } },
                        label = "Confirmar senha",
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    PremiumTextField(
                        value = state.form.description,
                        onValueChange = { value -> onFormChange { it.copy(description = value) } },
                        label = "Descrição",
                        singleLine = false,
                    )
                    PremiumTextField(
                        value = state.form.address,
                        onValueChange = { value -> onFormChange { it.copy(address = value) } },
                        label = "Endereço",
                    )
                    PremiumTextField(
                        value = state.form.businessHours,
                        onValueChange = { value -> onFormChange { it.copy(businessHours = value) } },
                        label = "Horário de funcionamento",
                    )
                }

                Text(
                    text = "CATEGORIA",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                PartnerRegistrationRules.Categories.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { category ->
                            CompanyCouponTypeChip(
                                label = category,
                                selected = state.form.category == category,
                                onClick = { onFormChange { it.copy(category = category) } },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Box(modifier = Modifier.weight(1f))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumSecondaryButton(
                        text = "Voltar",
                        onClick = onBackToPlans,
                        modifier = Modifier.weight(1f),
                    )
                    PremiumPrimaryButton(
                        text = "Enviar cadastro",
                        onClick = onSubmit,
                        loading = state.isSubmitting,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            else -> {
                PremiumCard(accent = PremiumBrand) {
                    PremiumChip(
                        label = "Cadastro enviado",
                        icon = Icons.Outlined.CheckCircle,
                        accent = PremiumBrand,
                        filled = true,
                    )
                    Text(
                        text = "AGUARDANDO APROVAÇÃO",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                    )
                    Text(
                        text = "A atlética analisa o cadastro e libera o acesso ao painel do parceiro.",
                        color = PremiumZinc400,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    PremiumSecondaryButton(
                        text = "Voltar para o login",
                        onClick = onLoginClick,
                        icon = Icons.Outlined.Login,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanyTierCard(
    tier: PartnerTier,
    price: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = PremiumZinc900.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = tier.label.uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                )
                Text(
                    text = price,
                    color = accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Icon(
                imageVector = Icons.Outlined.Business,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = accent,
            )
        }
    }
}

@Composable
private fun CompanyVisibilityToggle(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (checked) {
                Icons.Outlined.CheckCircle
            } else {
                Icons.Outlined.RadioButtonUnchecked
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (checked) PremiumBrand else PremiumZinc500,
        )
        Text(
            text = label,
            color = if (checked) Color.White else PremiumZinc400,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CompanyCouponTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PremiumBrand.copy(alpha = 0.16f) else PremiumZinc900,
        border = BorderStroke(1.dp, if (selected) PremiumBrand else PremiumZinc800),
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            color = if (selected) PremiumBrand else PremiumZinc400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompanyStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = Color.White,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                color = accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label.uppercase(),
                color = PremiumZinc500,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun CompanyScanRow(
    scan: PartnerScanRecord,
    detailed: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PremiumZinc900.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = PremiumBrand.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.3f)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp),
                    tint = PremiumBrand,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = scan.couponTitle.ifBlank { scan.couponName },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${scan.userDisplayName.ifBlank { scan.userName }} • ${scan.date} ${scan.hora()}",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detailed) {
                    Text(
                        text = "${scanMethodLabel(scan.scanMethod)} • ${approvalLabel(scan.approvalMode)}",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = scan.couponValue.ifBlank { scan.savedValueLabel },
                    color = PremiumBrand,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = if (scan.status == "pending") "PENDENTE" else "APROVADO",
                    color = if (scan.status == "pending") PremiumAmber else PremiumZinc500,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

private fun PartnerScanRecord.hora(): String = hour

/** `methodLabel` do web. */
private fun scanMethodLabel(value: String): String = when (value) {
    "manual" -> "Manual"
    else -> "QR code"
}

/** `approvalLabel` do web. */
private fun approvalLabel(value: String): String = when (value) {
    "manual_partner" -> "Manual pelo parceiro"
    "printed_qr" -> "Leitura do QR impresso"
    else -> "Direta via scan"
}
