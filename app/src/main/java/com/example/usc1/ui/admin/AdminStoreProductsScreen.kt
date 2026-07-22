package com.example.usc1.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.usc1.core.ui.PremiumAmber
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
import com.example.usc1.domain.model.AdminStoreProduct
import com.example.usc1.domain.model.AdminStoreProductStatus
import com.example.usc1.domain.model.AdminStoreSellerType
import java.text.NumberFormat
import java.util.Locale

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminStoreProductsScreen(
    state: AdminStoreProductsUiState,
    onInactiveProductsClick: () -> Unit,
    onActiveProductsClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onPendingOrdersClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onNewProductClick: () -> Unit,
    onCloseProductFormClick: () -> Unit,
    onEditProductClick: (AdminStoreProduct) -> Unit,
    onOpenProductClick: (String) -> Unit,
    onToggleProductActiveClick: (AdminStoreProduct) -> Unit,
    onCategoryClick: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onImageChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onOldPriceChange: (String) -> Unit,
    onStatusChange: (AdminStoreProductStatus) -> Unit,
    onStockChange: (String) -> Unit,
    onLotChange: (String) -> Unit,
    onTagLabelChange: (String) -> Unit,
    onTagColorChange: (String) -> Unit,
    onTagEffectChange: (String) -> Unit,
    onColorsTextChange: (String) -> Unit,
    onFeaturesTextChange: (String) -> Unit,
    onSaveProductClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.products.isEmpty()) {
        PremiumLoadingState(
            text = if (state.inactiveOnly) "Carregando produtos desativados..." else "Carregando produtos de ${state.selectedCategoryLabel}...",
            modifier = modifier,
        )
        return
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
        verticalSpacing = 16.dp,
    ) {
        PremiumHeader(
            title = state.title,
            subtitle = state.subtitle,
            icon = Icons.Outlined.Inventory2,
            accent = if (state.inactiveOnly) Color(0xFFF87171) else PremiumBrandAccent,
            onBackClick = onBackClick,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProductSmallAction(
                text = if (state.inactiveOnly) "Página Atual" else "Ver Desativados",
                icon = Icons.Outlined.PowerSettingsNew,
                accent = if (state.inactiveOnly) Color(0xFFF87171) else PremiumZinc400,
                onClick = if (state.inactiveOnly) onInactiveProductsClick else onInactiveProductsClick,
            )
            ProductSmallAction(
                text = "Categorias",
                icon = Icons.Outlined.Tag,
                accent = Color(0xFF60A5FA),
                onClick = onCategoriesClick,
            )
            ProductSmallAction(
                text = "Recebedores produtos",
                icon = Icons.Outlined.PersonAdd,
                accent = Color(0xFF22D3EE),
                enabled = false,
                onClick = {},
            )
            ProductSmallAction(
                text = "Novo Produto",
                icon = Icons.Outlined.Edit,
                accent = PremiumBrandAccent,
                enabled = state.canCreateProduct,
                onClick = onNewProductClick,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProductModuleShortcut(
                title = "Pedidos Pendentes",
                subtitle = "Aprovação manual continua ativa.",
                icon = Icons.Outlined.ShoppingBag,
                accent = PremiumAmber,
                onClick = onPendingOrdersClick,
                modifier = Modifier.weight(1f),
            )
            ProductModuleShortcut(
                title = "Reviews",
                subtitle = "Avaliações continuam moderadas após compra.",
                icon = Icons.Outlined.Message,
                accent = PremiumBrandAccent,
                onClick = onReviewsClick,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.inactiveOnly) {
            PremiumCard(accent = Color(0xFFF87171), containerColor = Color(0xFF1F0B0B)) {
                Text(
                    text = "Histórico dos Produtos Desativados",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Imagem, categoria, lote, variações e preço continuam salvos aqui para reativação segura.",
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                )
                ProductInfoBox(
                    label = "Itens no histórico",
                    value = "${state.products.size} produto${if (state.products.size == 1) "" else "s"}",
                    accent = Color.White,
                )
                Text(
                    text = "Ao ativar novamente, o produto volta a aparecer na categoria original sem perder o contexto comercial.",
                    color = Color(0xFFFECACA),
                    fontSize = 11.sp,
                )
                PremiumSecondaryButton(
                    text = "Voltar Produtos",
                    onClick = onActiveProductsClick,
                    accent = PremiumZinc400,
                )
            }
        } else {
            PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Produtos por Categoria",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "Abra só a categoria que você quer revisar para não puxar todos os produtos de uma vez.",
                            color = PremiumZinc500,
                            fontSize = 11.sp,
                        )
                    }
                    ProductInfoBox(
                        label = "Categoria aberta",
                        value = state.selectedCategoryLabel,
                        accent = Color.White,
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.categoryNames.forEach { name ->
                        ProductPill(
                            label = name,
                            selected = name == state.selectedCategoryLabel,
                            accent = PremiumBrandAccent,
                            onClick = { onCategoryClick(name) },
                        )
                    }
                }
            }
        }

        state.actionMessage?.let { message ->
            ProductMessage(message = message, color = PremiumBrandAccent)
        }
        state.errorMessage?.let { message ->
            ProductMessage(message = message, color = Color(0xFFFCA5A5))
        }

        if (state.isProductOpen) {
            AdminStoreProductFormCard(
                state = state,
                onNameChange = onNameChange,
                onCategoryChange = onCategoryChange,
                onDescriptionChange = onDescriptionChange,
                onImageChange = onImageChange,
                onPriceChange = onPriceChange,
                onOldPriceChange = onOldPriceChange,
                onStatusChange = onStatusChange,
                onStockChange = onStockChange,
                onLotChange = onLotChange,
                onTagLabelChange = onTagLabelChange,
                onTagColorChange = onTagColorChange,
                onTagEffectChange = onTagEffectChange,
                onColorsTextChange = onColorsTextChange,
                onFeaturesTextChange = onFeaturesTextChange,
                onSaveProductClick = onSaveProductClick,
                onCloseProductFormClick = onCloseProductFormClick,
            )
        }

        ProductListSection(
            state = state,
            onEditProductClick = onEditProductClick,
            onOpenProductClick = onOpenProductClick,
            onToggleProductActiveClick = onToggleProductActiveClick,
        )

        if (!state.inactiveOnly) {
            PremiumCard(accent = Color(0xFFF87171), containerColor = Color(0xFF1F0B0B)) {
                Text(
                    text = "Produtos Desativados em Página Separada",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "O histórico agora fica fora do catálogo principal para evitar perda de contexto e manter a operação mais limpa.",
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                )
                PremiumSecondaryButton(
                    text = "Abrir Histórico",
                    onClick = onInactiveProductsClick,
                    accent = Color(0xFFF87171),
                    icon = Icons.Outlined.PowerSettingsNew,
                )
            }
        }

        ProductMessage(
            message = if (state.inactiveOnly) {
                "Os desativados ficam separados do catálogo ativo, mas continuam completos para futuras reativações."
            } else {
                "Cada abertura consulta só a categoria ativa. Pedidos e reviews continuam em módulos separados para manter leve."
            },
            color = PremiumZinc500,
        )

        PremiumSecondaryButton(
            text = "Atualizar",
            onClick = onRefreshClick,
            icon = Icons.Outlined.Refresh,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AdminStoreProductFormCard(
    state: AdminStoreProductsUiState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onImageChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onOldPriceChange: (String) -> Unit,
    onStatusChange: (AdminStoreProductStatus) -> Unit,
    onStockChange: (String) -> Unit,
    onLotChange: (String) -> Unit,
    onTagLabelChange: (String) -> Unit,
    onTagColorChange: (String) -> Unit,
    onTagEffectChange: (String) -> Unit,
    onColorsTextChange: (String) -> Unit,
    onFeaturesTextChange: (String) -> Unit,
    onSaveProductClick: () -> Unit,
    onCloseProductFormClick: () -> Unit,
) {
    PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
        Text(
            text = if (state.isEditingProduct) "Editar Produto" else "Novo Produto",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "Imagem, preço, categoria, lote e status seguem o cadastro do web app.",
            color = PremiumZinc500,
            fontSize = 11.sp,
        )
        PremiumTextField(
            value = state.form.nome,
            onValueChange = onNameChange,
            label = "Nome do produto",
        )
        PremiumTextField(
            value = state.form.categoria,
            onValueChange = onCategoryChange,
            label = "Categoria",
        )
        PremiumTextField(
            value = state.form.descricao,
            onValueChange = onDescriptionChange,
            label = "Descrição",
            singleLine = false,
        )
        PremiumTextField(
            value = state.form.img,
            onValueChange = onImageChange,
            label = "URL da imagem",
            leadingIcon = Icons.Outlined.Image,
        )
        ProductMessage(
            message = "Upload de imagem: pendente no Android até configurar Supabase Storage com limite de tamanho, validação de tipo e compressão.",
            color = PremiumZinc400,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumTextField(
                value = state.form.preco,
                onValueChange = onPriceChange,
                label = "Preço",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            PremiumTextField(
                value = state.form.precoAntigo,
                onValueChange = onOldPriceChange,
                label = "Preço antigo",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumTextField(
                value = state.form.estoque,
                onValueChange = onStockChange,
                label = "Estoque",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            PremiumTextField(
                value = state.form.lote,
                onValueChange = onLotChange,
                label = "Lote",
                modifier = Modifier.weight(1f),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AdminStoreProductStatus.entries.forEach { status ->
                ProductPill(
                    label = status.label,
                    selected = state.form.status == status,
                    accent = statusColor(status),
                    onClick = { onStatusChange(status) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumTextField(
                value = state.form.tagLabel,
                onValueChange = onTagLabelChange,
                label = "Badge",
                modifier = Modifier.weight(1f),
            )
            PremiumTextField(
                value = state.form.tagColor,
                onValueChange = onTagColorChange,
                label = "Cor do badge",
                modifier = Modifier.weight(1f),
            )
        }
        PremiumTextField(
            value = state.form.tagEffect,
            onValueChange = onTagEffectChange,
            label = "Efeito do badge",
        )
        PremiumTextField(
            value = state.form.coresText,
            onValueChange = onColorsTextChange,
            label = "Cores (texto livre)",
            singleLine = false,
        )
        PremiumTextField(
            value = state.form.caracteristicasText,
            onValueChange = onFeaturesTextChange,
            label = "Características (1 por linha)",
            singleLine = false,
        )
        ProductMessage(
            message = "Preço e Visibilidade por Plano, recebedores e variações avançadas ainda dependem da próxima tradução dos componentes web equivalentes.",
            color = PremiumAmber,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Cancelar",
                onClick = onCloseProductFormClick,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
            PremiumPrimaryButton(
                text = if (state.isSaving) "Salvando..." else if (state.isEditingProduct) "Salvar Alterações" else "Criar Produto",
                onClick = onSaveProductClick,
                enabled = !state.isSaving,
                loading = state.isSaving,
                icon = Icons.Outlined.Save,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProductListSection(
    state: AdminStoreProductsUiState,
    onEditProductClick: (AdminStoreProduct) -> Unit,
    onOpenProductClick: (String) -> Unit,
    onToggleProductActiveClick: (AdminStoreProduct) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                text = state.listTitle,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = if (state.inactiveOnly) {
                    "Aqui ficam os produtos fora do ar, com todos os dados preservados para auditoria e reativação."
                } else {
                    "Só os itens de ${state.selectedCategoryLabel} são consultados agora."
                },
                color = PremiumZinc500,
                fontSize = 11.sp,
            )
        }

        if (state.products.isEmpty()) {
            PremiumEmptyState(
                title = state.emptyTitle,
                subtitle = "A consulta paginada não retornou produtos para este filtro.",
                icon = Icons.Outlined.Inventory2,
                accent = if (state.inactiveOnly) Color(0xFFF87171) else PremiumBrandAccent,
            )
        } else {
            state.products.forEach { product ->
                AdminStoreProductCard(
                    product = product,
                    isBusy = state.mutatingProductId == product.id,
                    onEditProductClick = { onEditProductClick(product) },
                    onOpenProductClick = { onOpenProductClick(product.id) },
                    onToggleProductActiveClick = { onToggleProductActiveClick(product) },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AdminStoreProductCard(
    product: AdminStoreProduct,
    isBusy: Boolean,
    onEditProductClick: () -> Unit,
    onOpenProductClick: () -> Unit,
    onToggleProductActiveClick: () -> Unit,
) {
    PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(14.dp),
                color = PremiumZinc800,
                border = BorderStroke(1.dp, PremiumZinc700),
            ) {
                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = PremiumZinc400, modifier = Modifier.padding(14.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.nome.ifBlank { "Produto" },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (product.tagLabel.isNotBlank()) {
                        PremiumChip(label = product.tagLabel, accent = PremiumZinc400)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PremiumChip(label = sellerLabel(product), accent = sellerColor(product.sellerType))
                    if (product.status != AdminStoreProductStatus.Ativo) {
                        PremiumChip(label = product.status.label, accent = statusColor(product.status))
                    }
                    PremiumChip(
                        label = if (product.active) "Ativo" else "Inativo",
                        accent = if (product.active) PremiumBrandAccent else Color(0xFFF87171),
                    )
                }
                Text(
                    text = "${product.categoria.ifBlank { "Sem categoria" }} • Lote: ${product.lote.ifBlank { "-" }}",
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (product.variantCount > 0) {
                    Text(
                        text = "Variações: ${product.variantCount}",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                    )
                }
                if (product.cores.isNotBlank()) {
                    Text(
                        text = "Cores: ${product.cores}",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = formatCurrency(product.preco),
                    color = PremiumBrandAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                )
                if (product.precoAntigo > product.preco) {
                    Text(
                        text = formatCurrency(product.precoAntigo),
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                    )
                }
                Text(
                    text = "Estoque: ${product.estoque} • Vendidos: ${product.vendidos}",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProductSmallAction("Editar produto", Icons.Outlined.Edit, PremiumZinc400, onClick = onEditProductClick)
            ProductSmallAction(
                text = if (product.active) "Desativar produto" else "Ativar produto",
                icon = Icons.Outlined.PowerSettingsNew,
                accent = if (product.active) Color(0xFFF87171) else PremiumBrandAccent,
                enabled = !isBusy,
                onClick = onToggleProductActiveClick,
            )
            ProductSmallAction("Abrir produto", Icons.Outlined.OpenInNew, PremiumZinc400, onClick = onOpenProductClick)
        }
    }
}

@Composable
private fun ProductModuleShortcut(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                Text(
                    text = title,
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = subtitle,
                color = PremiumZinc400,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ProductPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) accent.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.40f) else PremiumZinc700),
        onClick = onClick,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = if (selected) accent else PremiumZinc400,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun ProductSmallAction(
    text: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = if (enabled) accent else PremiumZinc700, modifier = Modifier.size(13.dp))
            Text(
                text = text,
                color = if (enabled) accent else PremiumZinc700,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProductInfoBox(
    label: String,
    value: String,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = label,
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = value,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProductMessage(
    message: String,
    color: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = color,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

private fun sellerLabel(product: AdminStoreProduct): String {
    return when (product.sellerType) {
        AdminStoreSellerType.MiniVendor -> product.sellerName.ifBlank { "Mini Vendor" }
        AdminStoreSellerType.League -> product.sellerName.ifBlank { "Liga" }
        AdminStoreSellerType.Tenant -> product.sellerName.ifBlank { "Tenant" }
    }
}

private fun sellerColor(type: AdminStoreSellerType): Color {
    return when (type) {
        AdminStoreSellerType.Tenant -> PremiumBrandAccent
        AdminStoreSellerType.MiniVendor -> Color(0xFF60A5FA)
        AdminStoreSellerType.League -> Color(0xFFA5B4FC)
    }
}

private fun statusColor(status: AdminStoreProductStatus): Color {
    return when (status) {
        AdminStoreProductStatus.Ativo -> PremiumBrandAccent
        AdminStoreProductStatus.EmBreve -> PremiumAmber
        AdminStoreProductStatus.Esgotado -> Color(0xFFF87171)
    }
}

private fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}
