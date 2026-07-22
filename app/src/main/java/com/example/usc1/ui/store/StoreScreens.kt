package com.example.usc1.ui.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
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
import com.example.usc1.core.ui.PremiumImageCard
import com.example.usc1.core.ui.PremiumInfoRow
import com.example.usc1.core.ui.PremiumLoadingState
import com.example.usc1.core.ui.PremiumPrimaryButton
import com.example.usc1.core.ui.PremiumScreen
import com.example.usc1.core.ui.PremiumSecondaryButton
import com.example.usc1.core.ui.PremiumZinc400
import com.example.usc1.core.ui.PremiumZinc500
import com.example.usc1.core.ui.PremiumZinc700
import com.example.usc1.core.ui.PremiumZinc800
import com.example.usc1.core.ui.PremiumZinc900

@Composable
fun StoreScreen(
    state: StoreUiState,
    onProductClick: (StoreProduct) -> Unit,
    onCategoryClick: (String) -> Unit,
    onCartClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando loja", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Loja USC",
                subtitle = "Produtos oficiais e retirada no evento",
                icon = Icons.Outlined.Storefront,
            )
            PremiumEmptyState(
                title = "Loja indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.Storefront,
            )
            PremiumPrimaryButton(text = "Tentar novamente", onClick = onRetryClick)
        }
        else -> PremiumScreen(
            modifier = modifier,
            bottomPadding = 116.dp,
        ) {
            PremiumHeader(
                title = "Loja USC",
                subtitle = "Drops, fardas e produtos do evento",
                icon = Icons.Outlined.Storefront,
            )

            StoreHeroCard(
                cartCount = state.cartCount,
                onCartClick = onCartClick,
                onOrdersClick = onOrdersClick,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.categories.forEach { category ->
                    StoreCategoryPill(
                        label = category,
                        selected = state.selectedCategory == category,
                        onClick = { onCategoryClick(category) },
                    )
                }
            }

            if (state.products.isEmpty()) {
                PremiumEmptyState(
                    title = "Nenhum produto",
                    subtitle = "Não há produtos publicados para este tenant e filtro.",
                    icon = Icons.Outlined.Storefront,
                )
            } else {
                state.products.forEach { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product) },
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDetailStateScreen(
    state: ProductDetailUiState,
    onAddToCartClick: (StoreProduct) -> Unit,
    onCartClick: () -> Unit,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando produto", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Produto",
                subtitle = "Detalhe da loja",
                icon = Icons.Outlined.ShoppingBag,
                onBackClick = onBackClick,
            )
            PremiumEmptyState(
                title = "Produto indisponível",
                subtitle = state.errorMessage,
                icon = Icons.Outlined.ShoppingBag,
            )
            PremiumPrimaryButton(text = "Tentar novamente", onClick = onRetryClick)
        }
        state.product != null -> ProductDetailScreen(
            product = state.product,
            onAddToCartClick = onAddToCartClick,
            onCartClick = onCartClick,
            onBackClick = onBackClick,
            modifier = modifier,
        )
    }
}

@Composable
fun ProductDetailScreen(
    product: StoreProduct,
    onAddToCartClick: (StoreProduct) -> Unit,
    onCartClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = product.name,
            subtitle = "Detalhe do produto oficial",
            icon = Icons.Outlined.ShoppingBag,
            accent = productStatusColor(product.status),
            onBackClick = onBackClick,
        )

        PremiumImageCard(
            imageRes = product.imageRes,
            height = 294.dp,
            accent = productStatusColor(product.status),
            imageAlpha = 0.78f,
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumChip(label = product.badge, accent = productStatusColor(product.status), filled = true)
                    ProductStatusChip(status = product.status)
                }
                Text(
                    text = product.name,
                    color = Color.White,
                    fontSize = 31.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.priceLabel,
                    color = productStatusColor(product.status),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        PremiumCard(accent = productStatusColor(product.status)) {
            Text(
                text = product.description,
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            PremiumInfoRow(label = "Categoria", value = product.category, accent = productStatusColor(product.status))
            PremiumInfoRow(label = "Vendedor", value = product.sellerName.ifBlank { product.sellerType.label }, accent = PremiumBrandAccent)
            PremiumInfoRow(label = "Estoque", value = product.stockLabel, accent = productStatusColor(product.status))
            PremiumInfoRow(label = "Avaliações", value = product.reviewLabel, accent = PremiumAmber)
        }

        PremiumPrimaryButton(
            text = if (product.status == StoreProductStatus.Available) "Adicionar ao carrinho" else product.status.label,
            onClick = { onAddToCartClick(product) },
            enabled = product.status == StoreProductStatus.Available,
            icon = Icons.Outlined.ShoppingCart,
            accent = productStatusColor(product.status),
        )
        PremiumSecondaryButton(
            text = "Abrir carrinho",
            onClick = onCartClick,
            icon = Icons.Outlined.AccountBalanceWallet,
        )
    }
}

@Composable
fun CartScreen(
    state: CartUiState,
    onCheckoutClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Carrinho",
            subtitle = "Revise os produtos antes do checkout",
            icon = Icons.Outlined.ShoppingCart,
            onBackClick = onBackClick,
        )

        if (state.items.isEmpty()) {
            PremiumEmptyState(
                title = "Carrinho vazio",
                subtitle = "Escolha produtos oficiais da USC para continuar.",
                icon = Icons.Outlined.ShoppingCart,
            )
        } else {
            state.items.forEach { item ->
                CartItemCard(item = item)
            }

            PremiumCard(accent = PremiumBrandAccent) {
                StoreTotalRow(label = "Subtotal", value = state.subtotalLabel)
                StoreTotalRow(label = "Taxa", value = state.serviceLabel)
                StoreTotalRow(label = "Total", value = state.totalLabel, highlight = true)
            }

            PremiumPrimaryButton(
                text = "Finalizar pedido",
                onClick = onCheckoutClick,
                icon = Icons.Outlined.Payment,
            )
        }
    }
}

@Composable
fun CheckoutScreen(
    state: CartUiState,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Checkout",
            subtitle = "Checkout pendente de integração real",
            icon = Icons.Outlined.CreditCard,
            onBackClick = onBackClick,
        )

        PremiumCard(accent = PremiumBrand) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = CircleShape,
                    color = PremiumBrand.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, PremiumBrand.copy(alpha = 0.34f)),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Payment,
                        contentDescription = null,
                        modifier = Modifier.padding(14.dp),
                        tint = PremiumBrand,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "PIX USC",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Aguardando comprovante e aprovação da atlética.",
                        color = PremiumZinc400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            StoreTotalRow(label = "Produtos", value = state.subtotalLabel)
            StoreTotalRow(label = "Total do pedido", value = state.totalLabel, highlight = true)
        }

        PremiumCard(accent = PremiumAmber) {
            PremiumChip(label = "Retirada no evento", icon = Icons.Outlined.LocalShipping, accent = PremiumAmber)
            Text(
                text = "Leve documento, carteirinha digital ou QR do pedido. A entrega fica liberada após aprovação do pagamento.",
                color = PremiumZinc400,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        state.items.forEach { item ->
            CartItemCard(item = item)
        }

        PremiumPrimaryButton(
            text = "Criar pedido",
            onClick = onConfirmClick,
            icon = Icons.Outlined.CheckCircle,
        )
    }
}

@Composable
fun StoreOrdersScreen(
    state: StoreOrdersUiState,
    onStatusClick: (StoreOrderStatus?) -> Unit,
    onOrderClick: (StoreOrder) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Pedidos Loja",
            subtitle = "Pendentes, aprovados e cancelados",
            icon = Icons.Outlined.ReceiptLong,
            onBackClick = onBackClick,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StoreCategoryPill(
                label = "Todos",
                selected = state.selectedStatus == null,
                onClick = { onStatusClick(null) },
            )
            StoreOrderStatus.values().forEach { status ->
                StoreCategoryPill(
                    label = status.label,
                    selected = state.selectedStatus == status,
                    accent = storeOrderColor(status),
                    onClick = { onStatusClick(status) },
                )
            }
        }

        if (state.orders.isEmpty()) {
            PremiumEmptyState(
                title = "Sem pedidos",
                subtitle = "Nenhum pedido encontrado para esse filtro.",
                icon = Icons.Outlined.ReceiptLong,
            )
        } else {
            state.orders.forEach { order ->
                StoreOrderCard(
                    order = order,
                    onClick = { onOrderClick(order) },
                )
            }
        }
    }
}

@Composable
fun StoreOrderDetailScreen(
    order: StoreOrder,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Pedido ${order.id}",
            subtitle = order.createdAtLabel,
            icon = Icons.Outlined.ReceiptLong,
            accent = storeOrderColor(order.status),
            onBackClick = onBackClick,
        )

        PremiumCard(accent = storeOrderColor(order.status)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = order.title,
                        color = Color.White,
                        fontSize = 21.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = order.pickupLabel,
                        color = PremiumZinc400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                StoreOrderStatusChip(status = order.status)
            }
            PremiumInfoRow(label = "Pagamento", value = order.paymentStatus.label, accent = storeOrderColor(order.status))
            PremiumInfoRow(label = "Total", value = order.amountLabel, accent = storeOrderColor(order.status))
        }

        order.items.forEach { item ->
            CartItemCard(item = item)
        }

        PremiumCard(accent = PremiumZinc700) {
            PremiumChip(label = "Status visual", icon = Icons.Outlined.CheckCircle, accent = storeOrderColor(order.status))
            Text(
                text = "Detalhe de pedido pendente de integração com o fluxo real do web app.",
                color = PremiumZinc400,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun StoreOrderDetailUnavailableScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumScreen(
        modifier = modifier,
        bottomPadding = 116.dp,
    ) {
        PremiumHeader(
            title = "Pedido",
            subtitle = "Detalhe da loja",
            icon = Icons.Outlined.ReceiptLong,
            onBackClick = onBackClick,
        )
        PremiumEmptyState(
            title = "Pedido não carregado",
            subtitle = "O detalhe de pedidos da loja ainda precisa ser portado do web app com Supabase real.",
            icon = Icons.Outlined.ReceiptLong,
        )
    }
}

@Composable
private fun StoreHeroCard(
    cartCount: Int,
    onCartClick: () -> Unit,
    onOrdersClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(226.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        PremiumBrand.copy(alpha = 0.28f),
                        PremiumZinc900,
                        Color.Black,
                    ),
                ),
            ),
    ) {
        Image(
            painter = painterResource(id = com.example.usc1.R.drawable.logo_platform_web),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(180.dp),
            contentScale = ContentScale.Fit,
            alpha = 0.22f,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumChip(label = "Drop ativo", accent = PremiumBrand, filled = true)
                PremiumChip(label = "$cartCount itens", accent = PremiumAmber)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PRODUTOS OFICIAIS",
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                )
                Text(
                    text = "Fardas, tirantes e acessórios com a identidade visual da USC.",
                    color = PremiumZinc400,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StoreHeroButton(text = "Carrinho", icon = Icons.Outlined.ShoppingCart, onClick = onCartClick)
                    StoreHeroButton(text = "Pedidos", icon = Icons.Outlined.ReceiptLong, onClick = onOrdersClick)
                }
            }
        }
    }
}

@Composable
private fun StoreHeroButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
            Text(
                text = text.uppercase(),
                color = Color.Black,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun StoreCategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = PremiumBrand,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) accent else PremiumZinc900,
        border = BorderStroke(1.dp, if (selected) accent else PremiumZinc800),
    ) {
        Text(
            text = label.uppercase(),
            color = if (selected) Color.Black else PremiumZinc400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun StoreTotalRow(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            color = if (highlight) Color.White else PremiumZinc500,
            fontSize = if (highlight) 13.sp else 11.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = value,
            color = if (highlight) PremiumBrandAccent else PremiumZinc400,
            fontSize = if (highlight) 24.sp else 14.sp,
            fontWeight = FontWeight.Black,
        )
    }
}
