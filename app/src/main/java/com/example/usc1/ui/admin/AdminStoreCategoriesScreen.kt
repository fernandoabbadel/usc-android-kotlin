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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.usc1.domain.model.AdminStoreCategory
import com.example.usc1.domain.model.AdminStoreSellerType

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AdminStoreCategoriesScreen(
    state: AdminStoreCategoriesUiState,
    onProductsClick: () -> Unit,
    onNewProductClick: () -> Unit,
    onToggleOrderPanelClick: () -> Unit,
    onMoveCategoryUp: (AdminStoreCategory) -> Unit,
    onMoveCategoryDown: (AdminStoreCategory) -> Unit,
    onRestoreOrderClick: () -> Unit,
    onSaveOrderClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onCoverUrlChange: (String) -> Unit,
    onButtonColorChange: (String) -> Unit,
    onStartNewCategoryClick: () -> Unit,
    onSaveCategoryClick: () -> Unit,
    onEditCategoryClick: (AdminStoreCategory) -> Unit,
    onToggleVisibilityClick: (AdminStoreCategory) -> Unit,
    onPendingOrdersClick: (String) -> Unit,
    onApprovedOrdersClick: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading && state.categories.isEmpty()) {
        PremiumLoadingState(text = "Carregando categorias...", modifier = modifier)
        return
    }

    PremiumScreen(
        modifier = modifier,
        bottomPadding = 110.dp,
        verticalSpacing = 16.dp,
    ) {
        PremiumHeader(
            title = "Categorias da Loja",
            subtitle = "Tenant, categorias detectadas nos produtos e visão das lojinhas.",
            icon = Icons.Outlined.Category,
            accent = Color(0xFF22D3EE),
            onBackClick = onBackClick,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumSecondaryButton(
                text = "Produtos",
                onClick = onProductsClick,
                enabled = false,
                icon = Icons.Outlined.Inventory2,
                modifier = Modifier.weight(1f),
            )
            PremiumSecondaryButton(
                text = "Novo Produto",
                onClick = onNewProductClick,
                enabled = false,
                icon = Icons.Outlined.Edit,
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CategoryStat("Total", state.categories.size.toString(), Color.White, Modifier.weight(1f))
            CategoryStat("Tenant", state.tenantCategories.size.toString(), PremiumBrandAccent, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CategoryStat("Mini Vendors", state.miniVendorCategories.size.toString(), Color(0xFF60A5FA), Modifier.weight(1f))
            CategoryStat("Ligas", state.leagueCategories.size.toString(), Color(0xFFA5B4FC), Modifier.weight(1f))
        }

        state.actionMessage?.let { message ->
            AdminStoreCategoryMessage(message = message, color = PremiumBrandAccent)
        }
        state.errorMessage?.let { message ->
            AdminStoreCategoryMessage(message = message, color = Color(0xFFFCA5A5))
        }

        PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ordem publica das categorias",
                        color = PremiumZinc500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Arraste para definir a ordem em que as categorias aparecem na loja.",
                        color = PremiumZinc400,
                        fontSize = 12.sp,
                    )
                }
                CategorySmallAction(
                    text = if (state.isOrderPanelOpen) "Fechar" else "Abrir",
                    icon = Icons.Outlined.Category,
                    accent = PremiumZinc400,
                    onClick = onToggleOrderPanelClick,
                )
            }

            if (state.isOrderPanelOpen) {
                if (state.orderableCategories.isEmpty()) {
                    CategoryInfoBox("Salve pelo menos uma categoria para poder ordenar aqui.")
                } else {
                    state.orderableCategories.forEachIndexed { index, category ->
                        OrderableCategoryRow(
                            index = index,
                            category = category,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.orderableCategories.lastIndex,
                            onMoveUp = { onMoveCategoryUp(category) },
                            onMoveDown = { onMoveCategoryDown(category) },
                        )
                    }
                }

                if (state.nonOrderableCategoriesCount > 0) {
                    CategoryInfoBox(
                        "${state.nonOrderableCategoriesCount} categoria${if (state.nonOrderableCategoriesCount == 1) "" else "s"} ainda aparece apenas nos produtos. Complete o cadastro dela antes de arrastar.",
                        accent = PremiumAmber,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumSecondaryButton(
                        text = "Restaurar ordem",
                        onClick = onRestoreOrderClick,
                        enabled = state.isOrderDirty && !state.isSavingOrder,
                        icon = Icons.Outlined.RestartAlt,
                        modifier = Modifier.weight(1f),
                    )
                    PremiumPrimaryButton(
                        text = if (state.isSavingOrder) "Salvando ordem..." else "Salvar ordem",
                        onClick = onSaveOrderClick,
                        enabled = state.isOrderDirty && !state.isSavingOrder,
                        loading = state.isSavingOrder,
                        icon = Icons.Outlined.Save,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.form.categoryId != null) "Editar Categoria do Tenant" else "Nova Categoria do Tenant",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Nome com até 80 caracteres. A logo é herdada automaticamente da atlética.",
                        color = PremiumZinc500,
                        fontSize = 11.sp,
                    )
                }
                if (state.form.categoryId != null) {
                    CategorySmallAction(
                        text = "Nova",
                        icon = Icons.Outlined.RestartAlt,
                        accent = PremiumZinc400,
                        onClick = onStartNewCategoryClick,
                    )
                }
            }

            PremiumTextField(
                value = state.form.nome,
                onValueChange = onNameChange,
                label = "Nome da categoria",
            )

            CategoryInfoBox("As categorias do tenant usam a mesma logo do tenant. Por isso o campo de logo não aparece mais aqui.")

            PremiumTextField(
                value = state.form.coverImg,
                onValueChange = onCoverUrlChange,
                label = "URL da capa",
            )

            CategoryInfoBox("Upload da capa: pendente no Android até configurar Supabase Storage com limite de tamanho, validação de tipo e compressão.")

            PremiumTextField(
                value = state.form.buttonColor,
                onValueChange = onButtonColorChange,
                label = "Cor do botão",
            )

            CategoryPreview(
                name = state.form.nome.ifBlank { "Categoria" },
                coverUrl = state.form.coverImg,
                logoUrl = state.tenantLogoUrl,
                color = parseHexColor(state.form.buttonColor, PremiumBrandAccent),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PremiumSecondaryButton(
                    text = "Limpar",
                    onClick = onStartNewCategoryClick,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                )
                PremiumPrimaryButton(
                    text = if (state.isSaving) {
                        "Salvando..."
                    } else if (state.form.categoryId != null) {
                        "Salvar Categoria"
                    } else {
                        "Criar Categoria"
                    },
                    onClick = onSaveCategoryClick,
                    enabled = !state.isSaving,
                    loading = state.isSaving,
                    icon = Icons.Outlined.Save,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        PremiumCard(accent = PremiumZinc800, containerColor = PremiumZinc900) {
            Text(
                text = "Categorias Cadastradas",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Itens do tenant podem ser completados aqui. Categorias de mini vendor continuam no cadastro da lojinha.",
                color = PremiumZinc500,
                fontSize = 11.sp,
            )

            if (state.categories.isEmpty()) {
                PremiumEmptyState(
                    title = "Nenhuma categoria encontrada.",
                    subtitle = "A consulta não retornou categorias nem produtos para este tenant.",
                    icon = Icons.Outlined.Category,
                )
            } else {
                state.categories.forEach { category ->
                    AdminStoreCategoryCard(
                        category = category,
                        isBusy = state.visibilityActionKey == category.key,
                        onEditClick = { onEditCategoryClick(category) },
                        onToggleVisibilityClick = { onToggleVisibilityClick(category) },
                        onPendingOrdersClick = { onPendingOrdersClick(category.nome) },
                        onApprovedOrdersClick = { onApprovedOrdersClick(category.nome) },
                    )
                }
            }
        }

        PremiumSecondaryButton(
            text = "Atualizar",
            onClick = onRefreshClick,
            icon = Icons.Outlined.Refresh,
        )
    }
}

@Composable
private fun CategoryStat(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = value,
                color = accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun OrderableCategoryRow(
    index: Int,
    category: AdminStoreCategory,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${index + 1}",
                color = PremiumZinc400,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.nome,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = sellerText(category),
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                )
            }
            CategoryIconAction(Icons.Outlined.ArrowUpward, canMoveUp, onMoveUp)
            CategoryIconAction(Icons.Outlined.ArrowDownward, canMoveDown, onMoveDown)
        }
    }
}

@Composable
private fun CategoryIconAction(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(10.dp),
        color = PremiumZinc900,
        border = BorderStroke(1.dp, PremiumZinc700),
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) PremiumZinc400 else PremiumZinc700,
            modifier = Modifier.padding(9.dp),
        )
    }
}

@Composable
private fun CategoryPreview(
    name: String,
    coverUrl: String,
    logoUrl: String,
    color: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Preview da categoria",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = color.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, color.copy(alpha = 0.42f)),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = coverUrl.ifBlank { logoUrl.ifBlank { "Sem capa configurada" } },
                            color = PremiumZinc500,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AdminStoreCategoryCard(
    category: AdminStoreCategory,
    isBusy: Boolean,
    onEditClick: () -> Unit,
    onToggleVisibilityClick: () -> Unit,
    onPendingOrdersClick: () -> Unit,
    onApprovedOrdersClick: () -> Unit,
) {
    val accent = parseHexColor(category.buttonColor, sellerColor(category.sellerType))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, PremiumZinc800),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
                ) {
                    Icon(Icons.Outlined.Category, contentDescription = null, tint = accent, modifier = Modifier.padding(12.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = category.nome,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CategoryBadge(sellerText(category), sellerColor(category.sellerType))
                        CategoryBadge(
                            if (category.categoryVisible) "Categoria visível" else "Categoria oculta",
                            if (category.categoryVisible) PremiumBrandAccent else Color(0xFFF87171),
                        )
                        CategoryBadge("${category.productCount} produto${if (category.productCount == 1) "" else "s"}", PremiumZinc400)
                        if (category.derivedOnly) {
                            CategoryBadge("Só nos produtos", PremiumZinc400)
                        }
                    }
                    Text(
                        text = categoryDescription(category),
                        color = PremiumZinc500,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (category.categoryId != null || category.sellerType != AdminStoreSellerType.Tenant) {
                    CategorySmallAction("Pendentes", Icons.Outlined.OpenInNew, PremiumAmber, onClick = onPendingOrdersClick)
                    CategorySmallAction("Aprovados", Icons.Outlined.OpenInNew, Color(0xFF22D3EE), onClick = onApprovedOrdersClick)
                }
                if (category.categoryId != null || category.sellerType == AdminStoreSellerType.MiniVendor) {
                    CategorySmallAction(
                        text = if (category.categoryVisible) "Ocultar categoria" else "Exibir categoria",
                        icon = if (category.categoryVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        accent = if (category.categoryVisible) Color(0xFFF87171) else PremiumBrandAccent,
                        enabled = !isBusy,
                        onClick = onToggleVisibilityClick,
                    )
                }
                if (category.sellerType == AdminStoreSellerType.Tenant) {
                    CategorySmallAction(
                        text = if (category.derivedOnly) "Completar" else "Editar",
                        icon = Icons.Outlined.Edit,
                        accent = PremiumZinc400,
                        onClick = onEditClick,
                    )
                } else {
                    CategorySmallAction(
                        text = if (category.sellerType == AdminStoreSellerType.League) "Abrir ligas" else "Abrir mini vendor",
                        icon = Icons.Outlined.OpenInNew,
                        accent = if (category.sellerType == AdminStoreSellerType.League) Color(0xFFA5B4FC) else Color(0xFF60A5FA),
                        enabled = false,
                        onClick = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBadge(label: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun CategorySmallAction(
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
private fun CategoryInfoBox(
    text: String,
    accent: Color = PremiumZinc400,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(13.dp),
            color = accent,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun AdminStoreCategoryMessage(
    message: String,
    color: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun sellerText(category: AdminStoreCategory): String {
    return when (category.sellerType) {
        AdminStoreSellerType.Tenant -> "Tenant"
        AdminStoreSellerType.MiniVendor -> "Mini Vendor"
        AdminStoreSellerType.League -> "Liga"
    }
}

private fun categoryDescription(category: AdminStoreCategory): String {
    return when {
        category.sellerType == AdminStoreSellerType.MiniVendor ->
            "A logo continua sendo editada dentro do cadastro da lojinha."
        category.sellerType == AdminStoreSellerType.League ->
            "A logo continua sendo editada dentro do cadastro da liga."
        category.derivedOnly ->
            "Categoria detectada nos produtos do tenant e pronta para ser completada."
        else ->
            "Categoria persistida e editável no admin da loja."
    }
}

private fun sellerColor(type: AdminStoreSellerType): Color {
    return when (type) {
        AdminStoreSellerType.Tenant -> PremiumBrandAccent
        AdminStoreSellerType.MiniVendor -> Color(0xFF60A5FA)
        AdminStoreSellerType.League -> Color(0xFFA5B4FC)
    }
}

private fun parseHexColor(value: String, fallback: Color): Color {
    val clean = value.trim()
    if (!clean.startsWith("#")) return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor(clean))
    }.getOrElse { fallback }
}
