package com.example.usc1.ui.vendor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.usc1.R
import com.example.usc1.core.ui.PremiumAmber
import com.example.usc1.core.ui.PremiumBrand
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

@Composable
fun MiniVendorScreen(
    state: MiniVendorUiState,
    onEditClick: () -> Unit,
    onProductsClick: () -> Unit,
    onManagementClick: () -> Unit,
    onPendingOrdersClick: () -> Unit,
    onApprovedOrdersClick: () -> Unit,
    onFinanceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando Mini Vendor", modifier = modifier)
        return
    }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(
            title = "Mini Vendor",
            subtitle = "Produtos, pedidos e financeiro",
            icon = Icons.Outlined.Storefront,
        )

        state.errorMessage?.let { message ->
            PremiumEmptyState(
                title = "Mini Vendor indisponível",
                subtitle = message,
                icon = Icons.Outlined.Storefront,
            )
            return@PremiumScreen
        }

        if (!state.hasProfile) {
            MiniVendorEmptyProfile(state.statusLabel)
            return@PremiumScreen
        }

        MiniVendorStoreHero(
            title = state.storeName,
            eyebrow = "MINI VENDOR",
            description = state.description.ifBlank { "Lojinha do aluno conectada ao ecossistema USC." },
            imageUrl = state.coverUrl ?: state.logoUrl,
            imageRes = R.drawable.logo_usc_wide,
            accent = PremiumBrand,
            status = state.statusLabel,
        )

        MiniVendorStatsGrid(
            firstLabel = "Receita",
            firstValue = state.totalRevenueLabel,
            firstIcon = Icons.Outlined.AccountBalanceWallet,
            secondLabel = "Aguardando baixa",
            secondValue = state.pendingAmountLabel,
            secondIcon = Icons.Outlined.Payment,
            secondAccent = PremiumAmber,
        )

        MiniVendorPublicSummaryPanel(state = state)
        MiniVendorShortcutCard(
            title = "Editar dados da empresa",
            subtitle = "Nome, descrição, logo, capa, contatos, slug e visibilidade pública.",
            icon = Icons.Outlined.Edit,
            accent = state.categoryButtonColor.toMiniVendorColor(PremiumBrand),
            badge = state.statusLabel,
            onClick = onEditClick,
        )
        MiniVendorShortcutCard(
            title = "Gestão da lojinha",
            subtitle = "Receita, compradores, conversão, estoque, recompra e produtos parados.",
            icon = Icons.Outlined.QueryStats,
            onClick = onManagementClick,
        )

        MiniVendorSectionTitle("Operação")
        MiniVendorShortcutCard(
            title = "Produtos",
            subtitle = "Catálogo, estoque, status e cardápio público.",
            icon = Icons.Outlined.Storefront,
            onClick = onProductsClick,
        )
        MiniVendorShortcutCard(
            title = "Pedidos pendentes",
            subtitle = "Comprovantes, aprovação e retirada.",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            accent = PremiumAmber,
            badge = state.pendingOrders.size.takeIf { it > 0 }?.let { "$it pendentes" },
            onClick = onPendingOrdersClick,
        )
        MiniVendorShortcutCard(
            title = "Pedidos aprovados",
            subtitle = "Histórico liberado e controle de entrega.",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            badge = state.approvedOrders.size.takeIf { it > 0 }?.let { "$it aprovados" },
            onClick = onApprovedOrdersClick,
        )
        MiniVendorShortcutCard(
            title = "Financeiro",
            subtitle = "Receita aprovada, saldo futuro e extrato.",
            icon = Icons.Outlined.AccountBalanceWallet,
            onClick = onFinanceClick,
        )
    }
}

@Composable
fun MiniVendorProductsScreen(
    state: MiniVendorUiState,
    onBackClick: () -> Unit,
    onSaveProduct: (MiniVendorProductForm) -> Unit,
    onToggleProductActive: (MiniVendorProduct, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando produtos", modifier = modifier)
        return
    }
    var form by remember(state.profileId) { mutableStateOf(MiniVendorProductForm()) }
    var isFormOpen by remember(state.profileId) { mutableStateOf(false) }
    LaunchedEffect(state.actionMessage) {
        if (!state.actionMessage.isNullOrBlank() && isFormOpen && !state.isSavingProfile) {
            form = MiniVendorProductForm()
            isFormOpen = false
        }
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        MiniVendorTopBar(title = "Produtos", subtitle = state.storeName, icon = Icons.Outlined.Storefront, onBackClick = onBackClick)
        when {
            state.errorMessage != null -> PremiumEmptyState("Produtos indisponíveis", state.errorMessage, Icons.Outlined.Storefront)
            !state.hasProfile -> MiniVendorEmptyProfile(state.statusLabel)
            else -> {
                state.actionMessage?.takeIf(String::isNotBlank)?.let { message ->
                    MiniVendorFeedbackPanel(message = message, accent = PremiumBrand)
                }
                MiniVendorInfoPanel {
                    MiniVendorInfoRow("Categoria", state.storeName)
                    MiniVendorInfoRow("Produtos", "${state.products.size} cadastrados")
                    MiniVendorInfoRow("Visibilidade", if (state.productsVisible) "Produtos públicos" else "Produtos ocultos")
                }
                PremiumPrimaryButton(
                    text = if (isFormOpen && form.productId == null) "Fechar cadastro" else "Novo produto",
                    onClick = {
                        if (isFormOpen && form.productId == null) {
                            isFormOpen = false
                        } else {
                            form = MiniVendorProductForm(active = true, remoteStatus = "ativo")
                            isFormOpen = true
                        }
                    },
                    icon = Icons.Outlined.Storefront,
                )
                if (isFormOpen) {
                    MiniVendorProductFormPanel(
                        form = form,
                        storeName = state.storeName,
                        isSaving = state.isSavingProfile,
                        onFormChange = { form = it },
                        onSave = { onSaveProduct(form) },
                        onCancel = {
                            form = MiniVendorProductForm()
                            isFormOpen = false
                        },
                    )
                }
                if (state.products.isEmpty()) {
                    PremiumEmptyState("Nenhum produto", "Cadastre produtos no Mini Vendor para aparecerem aqui.", Icons.Outlined.Storefront)
                } else {
                    MiniVendorSectionTitle("Catálogo publicado")
                    state.products.forEach { product ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniVendorProductCard(product = product)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PremiumSecondaryButton(
                                    text = "Editar",
                                    onClick = {
                                        form = product.toProductForm()
                                        isFormOpen = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.Edit,
                                )
                                PremiumSecondaryButton(
                                    text = if (product.active) "Desativar" else "Ativar",
                                    onClick = { onToggleProductActive(product, !product.active) },
                                    modifier = Modifier.weight(1f),
                                    accent = if (product.active) PremiumAmber else PremiumBrand,
                                    icon = Icons.Outlined.Visibility,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniVendorProductFormPanel(
    form: MiniVendorProductForm,
    storeName: String,
    isSaving: Boolean,
    onFormChange: (MiniVendorProductForm) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.30f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumChip(
                label = if (form.productId.isNullOrBlank()) "Novo produto" else "Editando produto",
                icon = Icons.Outlined.Inventory2,
                accent = PremiumBrand,
                filled = true,
            )
            MiniVendorInfoRow("Categoria fixa", storeName)
            PremiumTextField(
                value = form.name,
                onValueChange = { value -> onFormChange(form.copy(name = value)) },
                label = "Nome do produto",
            )
            PremiumTextField(
                value = form.description,
                onValueChange = { value -> onFormChange(form.copy(description = value)) },
                label = "Descrição",
                singleLine = false,
            )
            PremiumTextField(
                value = form.imageUrl,
                onValueChange = { value -> onFormChange(form.copy(imageUrl = value)) },
                label = "Imagem do produto",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumTextField(
                    value = form.price,
                    onValueChange = { value -> onFormChange(form.copy(price = value)) },
                    label = "Preço",
                    modifier = Modifier.weight(1f),
                )
                PremiumTextField(
                    value = form.oldPrice,
                    onValueChange = { value -> onFormChange(form.copy(oldPrice = value)) },
                    label = "Preço anterior",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumTextField(
                    value = form.stock,
                    onValueChange = { value -> onFormChange(form.copy(stock = value)) },
                    label = "Estoque",
                    modifier = Modifier.weight(1f),
                )
                PremiumTextField(
                    value = form.lot,
                    onValueChange = { value -> onFormChange(form.copy(lot = value)) },
                    label = "Lote",
                    modifier = Modifier.weight(1f),
                )
            }
            PremiumTextField(
                value = form.tagLabel,
                onValueChange = { value -> onFormChange(form.copy(tagLabel = value)) },
                label = "Tag / selo",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumSecondaryButton(
                    text = if (form.remoteStatus == "ativo") "Status: ativo" else "Marcar ativo",
                    onClick = { onFormChange(form.copy(remoteStatus = "ativo", active = true)) },
                    modifier = Modifier.weight(1f),
                    accent = PremiumBrand,
                )
                PremiumSecondaryButton(
                    text = if (form.remoteStatus == "em_breve") "Em breve" else "Marcar em breve",
                    onClick = { onFormChange(form.copy(remoteStatus = "em_breve", active = true)) },
                    modifier = Modifier.weight(1f),
                    accent = PremiumAmber,
                )
            }
            PremiumSecondaryButton(
                text = if (form.active) "Produto visível" else "Produto oculto",
                onClick = { onFormChange(form.copy(active = !form.active)) },
                accent = if (form.active) PremiumBrand else PremiumAmber,
                icon = Icons.Outlined.Visibility,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumSecondaryButton(
                    text = "Cancelar",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving,
                    accent = PremiumZinc500,
                )
                PremiumPrimaryButton(
                    text = if (form.productId.isNullOrBlank()) "Criar produto" else "Salvar produto",
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = form.name.trim().isNotBlank() && form.price.trim().isNotBlank(),
                    loading = isSaving,
                    icon = Icons.Outlined.Storefront,
                )
            }
        }
    }
}

@Composable
fun MiniVendorPendingOrdersScreen(
    state: MiniVendorUiState,
    onBackClick: () -> Unit,
    onSetOrderStatus: (MiniVendorOrder, MiniVendorOrderStatus) -> Unit,
    modifier: Modifier = Modifier,
    categoryLabel: String = "",
    onAllCategoriesClick: (() -> Unit)? = null,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando pedidos", modifier = modifier)
        return
    }
    val cleanCategory = categoryLabel.trim()
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        MiniVendorTopBar(
            title = if (cleanCategory.isBlank()) "Pendentes" else "Pendentes - $cleanCategory",
            subtitle = if (cleanCategory.isBlank()) {
                state.storeName
            } else {
                "Mostra somente os pedidos da categoria $cleanCategory."
            },
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            accent = PremiumAmber,
            onBackClick = onBackClick,
        )
        if (cleanCategory.isNotBlank() && onAllCategoriesClick != null) {
            PremiumSecondaryButton(
                text = "Todas categorias",
                onClick = onAllCategoriesClick,
                accent = PremiumAmber,
            )
        }
        when {
            state.errorMessage != null -> PremiumEmptyState("Pedidos indisponíveis", state.errorMessage, Icons.AutoMirrored.Outlined.ReceiptLong, accent = PremiumAmber)
            !state.hasProfile -> MiniVendorEmptyProfile(state.statusLabel)
            state.pendingOrders.isEmpty() -> PremiumEmptyState("Sem pendências", "Nenhum pedido aguardando aprovação agora.", Icons.AutoMirrored.Outlined.ReceiptLong, accent = PremiumAmber)
            else -> {
                state.actionMessage?.takeIf(String::isNotBlank)?.let { message ->
                    MiniVendorFeedbackPanel(message = message, accent = PremiumAmber)
                }
                MiniVendorSectionTitle("Pedidos aguardando baixa")
                state.pendingOrders.forEach { order ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniVendorOrderCard(order = order)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PremiumPrimaryButton(
                                text = "Aprovar",
                                onClick = { onSetOrderStatus(order, MiniVendorOrderStatus.Approved) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isSavingProfile,
                                loading = state.isSavingProfile,
                                icon = Icons.Outlined.CheckCircle,
                            )
                            PremiumSecondaryButton(
                                text = "Rejeitar",
                                onClick = { onSetOrderStatus(order, MiniVendorOrderStatus.Rejected) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isSavingProfile,
                                accent = PremiumAmber,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniVendorApprovedOrdersScreen(
    state: MiniVendorUiState,
    onBackClick: () -> Unit,
    onSetOrderStatus: (MiniVendorOrder, MiniVendorOrderStatus) -> Unit,
    modifier: Modifier = Modifier,
    categoryLabel: String = "",
    onAllCategoriesClick: (() -> Unit)? = null,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando aprovados", modifier = modifier)
        return
    }
    val cleanCategory = categoryLabel.trim()
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        MiniVendorTopBar(
            title = if (cleanCategory.isBlank()) "Aprovados" else "Aprovados - $cleanCategory",
            subtitle = if (cleanCategory.isBlank()) {
                state.storeName
            } else {
                "Mostra somente os pedidos da categoria $cleanCategory."
            },
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            onBackClick = onBackClick,
        )
        if (cleanCategory.isNotBlank() && onAllCategoriesClick != null) {
            PremiumSecondaryButton(
                text = "Todas categorias",
                onClick = onAllCategoriesClick,
                accent = PremiumBrand,
            )
        }
        when {
            state.errorMessage != null -> PremiumEmptyState("Pedidos indisponíveis", state.errorMessage, Icons.AutoMirrored.Outlined.ReceiptLong)
            !state.hasProfile -> MiniVendorEmptyProfile(state.statusLabel)
            state.approvedOrders.isEmpty() -> PremiumEmptyState("Sem aprovados", "Nenhum pedido aprovado encontrado.", Icons.AutoMirrored.Outlined.ReceiptLong)
            else -> {
                state.actionMessage?.takeIf(String::isNotBlank)?.let { message ->
                    MiniVendorFeedbackPanel(message = message, accent = PremiumBrand)
                }
                MiniVendorSectionTitle("Pedidos aprovados")
                state.approvedOrders.forEach { order ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniVendorOrderCard(order = order)
                        MiniVendorInfoRow(
                            "Aprovado por",
                            order.approvedByName.ifBlank { "Não informado" },
                        )
                        MiniVendorInfoRow(
                            "Data da aprovação",
                            order.approvedAtLabel.ifBlank { "Não informado" },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PremiumSecondaryButton(
                                text = "Pendente",
                                onClick = { onSetOrderStatus(order, MiniVendorOrderStatus.Pending) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isSavingProfile,
                                accent = PremiumAmber,
                            )
                            PremiumSecondaryButton(
                                text = "Rejeitar",
                                onClick = { onSetOrderStatus(order, MiniVendorOrderStatus.Rejected) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isSavingProfile,
                                accent = PremiumAmber,
                            )
                        }
                        PremiumPrimaryButton(
                            text = "Marcar entregue",
                            onClick = { onSetOrderStatus(order, MiniVendorOrderStatus.Delivered) },
                            enabled = !state.isSavingProfile && order.status != MiniVendorOrderStatus.Delivered,
                            loading = state.isSavingProfile,
                            icon = Icons.Outlined.CheckCircle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniVendorFinanceScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando financeiro", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        MiniVendorTopBar(title = "Financeiro", subtitle = "Gestão do Mini Vendor", icon = Icons.Outlined.AccountBalanceWallet, onBackClick = onBackClick)
        if (state.errorMessage != null) {
            PremiumEmptyState("Financeiro indisponível", state.errorMessage, Icons.Outlined.AccountBalanceWallet)
            return@PremiumScreen
        }
        if (!state.hasProfile) {
            MiniVendorEmptyProfile(state.statusLabel)
            return@PremiumScreen
        }
        MiniVendorStatsGrid(
            firstLabel = "Receita aprovada",
            firstValue = state.totalRevenueLabel,
            firstIcon = Icons.Outlined.AccountBalanceWallet,
            secondLabel = "Aguardando baixa",
            secondValue = state.pendingAmountLabel,
            secondIcon = Icons.Outlined.Payment,
            secondAccent = PremiumAmber,
        )
        MiniVendorFinancePanel(state)
        MiniVendorSectionTitle("Últimos aprovados")
        if (state.approvedOrders.isEmpty()) {
            PremiumEmptyState("Sem movimentação", "Os pedidos aprovados aparecerão aqui.", Icons.Outlined.AccountBalanceWallet)
        } else {
            state.approvedOrders.forEach { order -> MiniVendorOrderCard(order = order) }
        }
    }
}

@Composable
fun SalesModeScreen(
    state: MiniVendorUiState,
    onEventMenuClick: () -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando modo vendas", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        PremiumHeader(title = "Modo Vendas", subtitle = "Menu do evento e retiradas", icon = Icons.Outlined.QrCodeScanner)
        if (state.errorMessage != null) {
            PremiumEmptyState("Modo vendas indisponível", state.errorMessage, Icons.Outlined.QrCodeScanner)
            return@PremiumScreen
        }
        if (!state.hasProfile) {
            MiniVendorEmptyProfile(state.statusLabel)
            return@PremiumScreen
        }
        MiniVendorStoreHero(
            title = "Menu do evento",
            eyebrow = "MODO VENDAS",
            description = "Produtos, fichas e scanner visual para retirada.",
            imageUrl = state.coverUrl ?: state.logoUrl,
            imageRes = R.drawable.battle_forest,
            accent = PremiumAmber,
            status = state.storeName,
        )
        MiniVendorShortcutCard(
            title = "Menu do evento",
            subtitle = "Produtos e fichas disponíveis.",
            icon = Icons.Outlined.Storefront,
            onClick = onEventMenuClick,
        )
        MiniVendorShortcutCard(
            title = "Scanner de retirada",
            subtitle = "Validar retirada por QR.",
            icon = Icons.Outlined.QrCodeScanner,
            accent = PremiumAmber,
            onClick = onScannerClick,
        )
    }
}

@Composable
fun SalesModeEventMenuScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando menu", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        MiniVendorTopBar(title = "Menu do Evento", subtitle = "Produtos do vendedor", icon = Icons.Outlined.Storefront, onBackClick = onBackClick)
        when {
            state.errorMessage != null -> PremiumEmptyState("Menu indisponível", state.errorMessage, Icons.Outlined.Storefront)
            !state.hasProfile -> MiniVendorEmptyProfile(state.statusLabel)
            state.products.isEmpty() -> PremiumEmptyState("Nenhum produto", "Produtos ativos do vendedor aparecerão neste menu.", Icons.Outlined.Storefront)
            else -> state.products.forEach { product -> MiniVendorProductCard(product = product) }
        }
    }
}

@Composable
fun MiniVendorEditScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando dados da empresa", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        MiniVendorTopBar(
            title = "Dados da empresa",
            subtitle = state.storeName,
            icon = Icons.Outlined.Edit,
            onBackClick = onBackClick,
        )
        when {
            state.errorMessage != null -> PremiumEmptyState("Dados indisponíveis", state.errorMessage, Icons.Outlined.Edit)
            !state.hasProfile -> MiniVendorEmptyProfile(state.statusLabel)
            else -> {
                MiniVendorStoreHero(
                    title = state.storeName,
                    eyebrow = "MINI VENDOR",
                    description = state.description.ifBlank { "Cadastro público da lojinha do aluno." },
                    imageUrl = state.coverUrl ?: state.logoUrl,
                    imageRes = R.drawable.logo_usc_wide,
                    accent = state.categoryButtonColor.toMiniVendorColor(PremiumBrand),
                    status = state.statusLabel,
                )
                MiniVendorSectionTitle("Identidade pública")
                MiniVendorInfoPanel {
                    MiniVendorInfoRow("Nome da loja", state.storeName)
                    MiniVendorInfoRow("Slug público", state.slug.ifBlank { "Sem slug definido" })
                    MiniVendorInfoRow("Descrição", state.description.ifBlank { "Sem descrição cadastrada" })
                    MiniVendorInfoRow("Status", state.statusLabel)
                    MiniVendorInfoRow("Aprovado em", state.approvedAtLabel.ifBlank { "Aguardando análise" })
                }
                MiniVendorSectionTitle("Contatos e recebimento")
                MiniVendorInfoPanel {
                    MiniVendorInfoRow(
                        "Instagram",
                        if (state.instagramEnabled) state.instagram.ifBlank { "Ligado sem usuário" } else "Desligado",
                    )
                    MiniVendorInfoRow(
                        "WhatsApp",
                        if (state.whatsappEnabled) state.whatsapp.ifBlank { "Ligado sem número" } else "Desligado",
                    )
                    MiniVendorInfoRow("Banco PIX", state.pixBank.ifBlank { "Não informado" })
                    MiniVendorInfoRow("Titular PIX", state.pixHolder.ifBlank { "Não informado" })
                    MiniVendorInfoRow("WhatsApp PIX", state.pixWhatsapp.ifBlank { "Não informado" })
                }
                MiniVendorSectionTitle("Visibilidade")
                MiniVendorInfoPanel {
                    MiniVendorInfoRow("Perfil público", if (state.profileVisible) "Visível" else "Oculto")
                    MiniVendorInfoRow("Categoria na loja", if (state.categoryVisible) "Visível" else "Oculta")
                    MiniVendorInfoRow("Produtos públicos", if (state.productsVisible) "Visíveis" else "Ocultos")
                    MiniVendorInfoRow("Cor da categoria", state.categoryButtonColor)
                }
            }
        }
    }
}

@Composable
fun MiniVendorManagementScreen(state: MiniVendorUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando gestão da lojinha", modifier = modifier)
        return
    }
    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        MiniVendorTopBar(
            title = "Gestão da lojinha",
            subtitle = "BI do Mini Vendor",
            icon = Icons.Outlined.QueryStats,
            onBackClick = onBackClick,
        )
        when {
            state.errorMessage != null -> PremiumEmptyState("Gestão indisponível", state.errorMessage, Icons.Outlined.QueryStats)
            !state.hasProfile -> MiniVendorEmptyProfile(state.statusLabel)
            else -> {
                val soldCount = state.products.sumOf(MiniVendorProduct::soldCount)
                val clicksCount = state.products.sumOf(MiniVendorProduct::clicksCount)
                val stockCount = state.products.sumOf(MiniVendorProduct::stockCount)
                val inactiveProducts = state.products.count { it.status.equals("Inativo", ignoreCase = true) }
                val stoppedProducts = state.products.filter { it.soldCount == 0 && it.stockCount > 0 }

                MiniVendorStatsGrid(
                    firstLabel = "Produtos",
                    firstValue = state.products.size.toString(),
                    firstIcon = Icons.Outlined.Inventory2,
                    secondLabel = "Pedidos",
                    secondValue = state.ordersCount.toString(),
                    secondIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
                )
                MiniVendorStatsGrid(
                    firstLabel = "Vendidos",
                    firstValue = soldCount.toString(),
                    firstIcon = Icons.Outlined.Storefront,
                    secondLabel = "Cliques",
                    secondValue = clicksCount.toString(),
                    secondIcon = Icons.Outlined.Visibility,
                    secondAccent = PremiumAmber,
                )
                MiniVendorSectionTitle("Resumo comercial")
                MiniVendorInfoPanel {
                    MiniVendorInfoRow("Receita aprovada", state.totalRevenueLabel)
                    MiniVendorInfoRow("Aguardando baixa", state.pendingAmountLabel)
                    MiniVendorInfoRow("Estoque total", "$stockCount unidades")
                    MiniVendorInfoRow("Produtos inativos", inactiveProducts.toString())
                    MiniVendorInfoRow("Produtos parados", stoppedProducts.size.toString())
                }
                MiniVendorSectionTitle("Produtos parados")
                if (stoppedProducts.isEmpty()) {
                    PremiumEmptyState("Sem produto parado", "Todos os produtos com estoque já possuem venda registrada.", Icons.Outlined.QueryStats)
                } else {
                    stoppedProducts.take(5).forEach { product ->
                        MiniVendorProductCard(product = product)
                    }
                }
            }
        }
    }
}

@Composable
fun MiniVendorEditableProfileScreen(
    state: MiniVendorUiState,
    onBackClick: () -> Unit,
    onSaveProfile: (MiniVendorProfileForm) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        PremiumLoadingState(text = "Carregando dados da empresa", modifier = modifier)
        return
    }

    var form by remember { mutableStateOf(state.toProfileForm()) }
    LaunchedEffect(state.profileId, state.storeName, state.pixKey, state.approvedAtLabel, state.isLoading) {
        if (!state.isSavingProfile) {
            form = state.toProfileForm()
        }
    }

    PremiumScreen(modifier = modifier, bottomPadding = 116.dp) {
        MiniVendorTopBar(
            title = "Editar empresa",
            subtitle = if (state.hasProfile) state.storeName else "Enviar cadastro",
            icon = Icons.Outlined.Edit,
            onBackClick = onBackClick,
        )

        state.errorMessage?.let { message ->
            MiniVendorFeedbackPanel(message = message, accent = PremiumAmber)
        }
        state.actionMessage?.let { message ->
            MiniVendorFeedbackPanel(message = message, accent = PremiumBrand)
        }

        MiniVendorStoreHero(
            title = form.storeName.ifBlank { "Sua loja mini vendor" },
            eyebrow = "MINI VENDOR",
            description = form.description.ifBlank { "Cadastro público da lojinha do aluno." },
            imageUrl = form.coverUrl.ifBlank { form.logoUrl }.takeIf(String::isNotBlank),
            imageRes = R.drawable.logo_usc_wide,
            accent = form.categoryButtonColor.toMiniVendorColor(PremiumBrand),
            status = state.statusLabel,
        )

        MiniVendorSectionTitle("Identidade pública")
        MiniVendorInfoPanel {
            PremiumTextField(
                value = form.storeName,
                onValueChange = { form = form.copy(storeName = it) },
                label = "Nome da loja",
                leadingIcon = Icons.Outlined.Storefront,
            )
            PremiumTextField(
                value = form.description,
                onValueChange = { form = form.copy(description = it) },
                label = "Descrição",
                singleLine = false,
                leadingIcon = Icons.Outlined.Edit,
            )
            PremiumTextField(
                value = form.logoUrl,
                onValueChange = { form = form.copy(logoUrl = it) },
                label = "URL do logo",
                leadingIcon = Icons.Outlined.Storefront,
            )
            PremiumTextField(
                value = form.coverUrl,
                onValueChange = { form = form.copy(coverUrl = it) },
                label = "URL da capa",
                leadingIcon = Icons.Outlined.Storefront,
            )
            MiniVendorInfoRow("Slug", state.slug.ifBlank { "Será gerado pelo nome da loja" })
            MiniVendorInfoRow("Status", state.statusLabel)
            MiniVendorInfoRow("Aprovado em", state.approvedAtLabel.ifBlank { "Aguardando análise" })
        }

        MiniVendorSectionTitle("Recebimento PIX")
        MiniVendorInfoPanel {
            PremiumTextField(
                value = form.pixKey,
                onValueChange = { form = form.copy(pixKey = it) },
                label = "Chave PIX",
                leadingIcon = Icons.Outlined.Payment,
            )
            PremiumTextField(
                value = form.pixBank,
                onValueChange = { form = form.copy(pixBank = it) },
                label = "Banco",
                leadingIcon = Icons.Outlined.AccountBalanceWallet,
            )
            PremiumTextField(
                value = form.pixHolder,
                onValueChange = { form = form.copy(pixHolder = it) },
                label = "Titular",
                leadingIcon = Icons.Outlined.AccountBalanceWallet,
            )
            PremiumTextField(
                value = form.pixWhatsapp,
                onValueChange = { form = form.copy(pixWhatsapp = it) },
                label = "WhatsApp para comprovante",
                leadingIcon = Icons.Outlined.Payment,
            )
        }

        MiniVendorSectionTitle("Contatos públicos")
        MiniVendorInfoPanel {
            PremiumTextField(
                value = form.instagram,
                onValueChange = { form = form.copy(instagram = it) },
                label = "Instagram",
                leadingIcon = Icons.Outlined.Visibility,
            )
            PremiumSecondaryButton(
                text = if (form.instagramEnabled) "Instagram ligado" else "Instagram desligado",
                onClick = { form = form.copy(instagramEnabled = !form.instagramEnabled) },
                accent = if (form.instagramEnabled) PremiumBrand else PremiumZinc500,
                icon = Icons.Outlined.Visibility,
            )
            PremiumTextField(
                value = form.whatsapp,
                onValueChange = { form = form.copy(whatsapp = it) },
                label = "WhatsApp da loja",
                leadingIcon = Icons.Outlined.Payment,
            )
            PremiumSecondaryButton(
                text = if (form.whatsappEnabled) "WhatsApp ligado" else "WhatsApp desligado",
                onClick = { form = form.copy(whatsappEnabled = !form.whatsappEnabled) },
                accent = if (form.whatsappEnabled) PremiumBrand else PremiumZinc500,
                icon = Icons.Outlined.Visibility,
            )
        }

        MiniVendorSectionTitle("Visibilidade")
        MiniVendorInfoPanel {
            PremiumTextField(
                value = form.categoryButtonColor,
                onValueChange = { form = form.copy(categoryButtonColor = it) },
                label = "Cor da categoria",
                leadingIcon = Icons.Outlined.Visibility,
            )
            PremiumSecondaryButton(
                text = if (form.profileVisible) "Perfil público visível" else "Perfil público oculto",
                onClick = { form = form.copy(profileVisible = !form.profileVisible) },
                accent = if (form.profileVisible) PremiumBrand else PremiumAmber,
                icon = Icons.Outlined.Visibility,
            )
            PremiumSecondaryButton(
                text = if (form.categoryVisible) "Categoria visível na loja" else "Categoria oculta na loja",
                onClick = { form = form.copy(categoryVisible = !form.categoryVisible) },
                accent = if (form.categoryVisible) PremiumBrand else PremiumAmber,
                icon = Icons.Outlined.Storefront,
            )
            PremiumSecondaryButton(
                text = if (form.productsVisible) "Produtos públicos visíveis" else "Produtos públicos ocultos",
                onClick = { form = form.copy(productsVisible = !form.productsVisible) },
                accent = if (form.productsVisible) PremiumBrand else PremiumAmber,
                icon = Icons.Outlined.Inventory2,
            )
        }

        PremiumPrimaryButton(
            text = if (state.hasProfile) "Salvar loja" else "Enviar cadastro",
            onClick = { onSaveProfile(form) },
            loading = state.isSavingProfile,
            icon = Icons.Outlined.CheckCircle,
        )
    }
}

@Composable
private fun MiniVendorFeedbackPanel(message: String, accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun MiniVendorPublicSummaryPanel(state: MiniVendorUiState) {
    val accent = state.categoryButtonColor.toMiniVendorColor(PremiumBrand)
    MiniVendorSectionTitle("Visão geral")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumChip(label = state.statusLabel, icon = Icons.Outlined.CheckCircle, accent = accent)
                PremiumChip(
                    label = if (state.profileVisible) "Perfil público" else "Perfil oculto",
                    icon = Icons.Outlined.Visibility,
                    accent = if (state.profileVisible) PremiumBrand else PremiumAmber,
                )
            }
            MiniVendorInfoRow("Slug", state.slug.ifBlank { "Sem slug público definido" })
            MiniVendorInfoRow(
                "Categoria pública",
                if (state.categoryVisible) state.storeName else "Categoria oculta na loja",
            )
            MiniVendorInfoRow(
                "Produtos",
                if (state.productsVisible) "${state.products.size} produtos visíveis/cadastrados" else "Produtos ocultos do perfil público",
            )
            MiniVendorInfoRow(
                "Instagram",
                if (state.instagramEnabled) state.instagram.ifBlank { "Ligado sem usuário" } else "Desligado",
            )
            MiniVendorInfoRow(
                "WhatsApp",
                if (state.whatsappEnabled) state.whatsapp.ifBlank { "Ligado sem número" } else "Desligado",
            )
        }
    }
}

@Composable
private fun MiniVendorInfoPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun MiniVendorInfoRow(label: String, value: String, accent: Color = PremiumBrand) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                color = PremiumZinc500,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(0.9f),
            )
            Text(
                text = value,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.1f),
            )
        }
    }
}

@Composable
private fun MiniVendorTopBar(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .size(46.dp)
                .clickable(onClick = onBackClick),
            shape = CircleShape,
            color = PremiumZinc900,
            border = BorderStroke(1.dp, PremiumZinc800),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = PremiumZinc400,
                modifier = Modifier.padding(11.dp),
            )
        }
        Surface(
            modifier = Modifier.size(58.dp),
            shape = RoundedCornerShape(18.dp),
            color = accent.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(15.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = PremiumZinc500,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MiniVendorEmptyProfile(statusLabel: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PremiumChip(label = "Cadastro", icon = Icons.Outlined.Storefront, accent = PremiumAmber)
            Text(
                text = "Mini Vendor ainda não publicado",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
            )
            Text(
                text = statusLabel,
                color = PremiumZinc400,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun MiniVendorStoreHero(
    title: String,
    eyebrow: String,
    description: String,
    imageUrl: String?,
    imageRes: Int,
    accent: Color,
    status: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
    ) {
        Box(modifier = Modifier.height(246.dp)) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = imageRes),
                    fallback = painterResource(id = imageRes),
                    error = painterResource(id = imageRes),
                )
            } else {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.12f),
                                PremiumZinc900.copy(alpha = 0.62f),
                                Color.Black.copy(alpha = 0.96f),
                            ),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PremiumChip(label = eyebrow, icon = Icons.Outlined.Storefront, accent = accent, filled = true)
                PremiumChip(label = status, icon = Icons.Outlined.CheckCircle, accent = accent)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    color = PremiumZinc400,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MiniVendorStatsGrid(
    firstLabel: String,
    firstValue: String,
    firstIcon: ImageVector,
    secondLabel: String,
    secondValue: String,
    secondIcon: ImageVector,
    modifier: Modifier = Modifier,
    firstAccent: Color = PremiumBrand,
    secondAccent: Color = PremiumBrand,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MiniVendorStatCard(firstLabel, firstValue, firstIcon, firstAccent, modifier = Modifier.weight(1f))
        MiniVendorStatCard(secondLabel, secondValue, secondIcon, secondAccent, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MiniVendorStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(10.dp))
            }
            Text(
                text = label.uppercase(),
                color = PremiumZinc500,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MiniVendorShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
    badge: String? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(17.dp),
                color = accent.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(13.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(text = subtitle, color = PremiumZinc400, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
            }
            if (!badge.isNullOrBlank()) {
                PremiumChip(label = badge, accent = accent)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = PremiumZinc500)
        }
    }
}

@Composable
private fun MiniVendorFinancePanel(state: MiniVendorUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumChip(label = "Resumo financeiro", icon = Icons.Outlined.AccountBalanceWallet, accent = PremiumBrand)
            MiniVendorFinanceRow("Produtos cadastrados", state.products.size.toString())
            MiniVendorFinanceRow("Pedidos aprovados", state.approvedOrders.size.toString())
            MiniVendorFinanceRow("Pedidos pendentes", state.pendingOrders.size.toString(), PremiumAmber)
            Text(
                text = "Valores calculados a partir dos pedidos reais vinculados ao seller_id do Mini Vendor.",
                color = PremiumZinc500,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MiniVendorFinanceRow(label: String, value: String, accent: Color = PremiumBrand) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                color = PremiumZinc500,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            )
            Text(text = value, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MiniVendorSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = PremiumBrand,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.8.sp,
    )
}

private fun String?.toMiniVendorColor(fallback: Color): Color {
    val clean = orEmpty().trim()
    if (clean.isBlank()) return fallback
    return runCatching {
        val normalized = if (clean.startsWith("#")) clean else "#$clean"
        Color(android.graphics.Color.parseColor(normalized))
    }.getOrDefault(fallback)
}
