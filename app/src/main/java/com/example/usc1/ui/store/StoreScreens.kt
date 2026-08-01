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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
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
import com.example.usc1.core.ui.PremiumInfoRow
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
import java.net.URLEncoder

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
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val visibleProducts = state.products.filter { product ->
        val query = searchQuery.trim()
        query.isBlank() ||
            product.name.contains(query, ignoreCase = true) ||
            product.description.contains(query, ignoreCase = true) ||
            product.category.contains(query, ignoreCase = true) ||
            product.sellerName.contains(query, ignoreCase = true) ||
            product.badge.contains(query, ignoreCase = true)
    }

    when {
        state.isLoading -> PremiumLoadingState(text = "Carregando loja", modifier = modifier)
        state.errorMessage != null -> PremiumScreen(modifier = modifier) {
            PremiumHeader(
                title = "Loja",
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
                title = "Loja",
                subtitle = "Produtos, carrinho e pedidos da atlética",
                icon = Icons.Outlined.Storefront,
            )

            StoreHeroCard(
                cartCount = state.cartCount,
                onCartClick = onCartClick,
                onOrdersClick = onOrdersClick,
            )

            PremiumTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "O que você procura?",
                leadingIcon = Icons.Outlined.Search,
            )

            if (state.categoryCards.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.categoryCards.forEach { category ->
                        StoreCategoryCard(
                            category = category,
                            selected = state.selectedCategory == category.name,
                            onClick = { onCategoryClick(category.name) },
                        )
                    }
                }
            }

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

            if (visibleProducts.isEmpty()) {
                PremiumEmptyState(
                    title = "Nenhum produto",
                    subtitle = "Não há produtos publicados para este tenant e filtro.",
                    icon = Icons.Outlined.Storefront,
                )
            } else {
                visibleProducts.forEach { product ->
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
    onAddToCartClick: (StoreProduct, Int, String, String) -> Unit,
    onCartClick: () -> Unit,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOrderClick: (StoreOrder) -> Unit = {},
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
            myOrders = state.myOrders,
            myOrdersLoading = state.myOrdersLoading,
            onOrderClick = onOrderClick,
        )
    }
}

@Composable
fun ProductDetailScreen(
    product: StoreProduct,
    onAddToCartClick: (StoreProduct, Int, String, String) -> Unit,
    onCartClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    myOrders: List<StoreOrder> = emptyList(),
    myOrdersLoading: Boolean = false,
    onOrderClick: (StoreOrder) -> Unit = {},
) {
    var quantity by rememberSaveable(product.id) { mutableStateOf(1) }
    var selectedVariant by rememberSaveable(product.id, product.variants.joinToString("|")) {
        mutableStateOf(product.variants.firstOrNull().orEmpty())
    }
    var selectedColor by rememberSaveable(product.id, product.colors.joinToString("|")) {
        mutableStateOf(product.colors.firstOrNull().orEmpty())
    }

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

        StoreProductImageCard(
            product = product,
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
                product.oldPriceLabel?.let { oldPrice ->
                    Text(
                        text = oldPrice,
                        color = PremiumZinc500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
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
            if (product.colors.isNotEmpty()) {
                PremiumInfoRow(label = "Cores", value = product.colors.take(4).joinToString(", "), accent = PremiumBrandAccent)
            }
            if (product.variants.isNotEmpty()) {
                PremiumInfoRow(label = "Variações", value = product.variants.take(3).joinToString(", "), accent = productStatusColor(product.status))
            }
            if (product.characteristics.isNotEmpty()) {
                PremiumInfoRow(label = "Características", value = product.characteristics.take(3).joinToString(", "), accent = PremiumZinc400)
            }
            PremiumInfoRow(label = "Métricas", value = "${product.likesCount} curtidas • ${product.soldCount} vendidos", accent = PremiumAmber)
            if (product.pixKey.isNotBlank() || product.pixBank.isNotBlank() || product.pixHolder.isNotBlank()) {
                PremiumInfoRow(label = "PIX", value = product.pixHolder.ifBlank { product.pixBank.ifBlank { "Configurado" } }, accent = PremiumBrand)
            }
            PremiumInfoRow(label = "Avaliações", value = product.reviewLabel, accent = PremiumAmber)
        }

        if (product.colors.isNotEmpty()) {
            StoreOptionSelector(
                title = "Cor",
                options = product.colors,
                selected = selectedColor,
                onSelect = { selectedColor = it },
                accent = productStatusColor(product.status),
            )
        }
        if (product.variants.isNotEmpty()) {
            StoreOptionSelector(
                title = "Variação",
                options = product.variants,
                selected = selectedVariant,
                onSelect = { selectedVariant = it },
                accent = productStatusColor(product.status),
            )
        }
        StoreQuantitySelector(
            quantity = quantity,
            onDecrease = { quantity = (quantity - 1).coerceAtLeast(1) },
            onIncrease = { quantity += 1 },
            accent = productStatusColor(product.status),
        )

        PremiumPrimaryButton(
            text = if (product.status == StoreProductStatus.Available) "Adicionar ao carrinho" else product.status.label,
            onClick = { onAddToCartClick(product, quantity, selectedVariant, selectedColor) },
            enabled = product.status == StoreProductStatus.Available,
            icon = Icons.Outlined.ShoppingCart,
            accent = productStatusColor(product.status),
        )
        PremiumSecondaryButton(
            text = "Abrir carrinho",
            onClick = onCartClick,
            icon = Icons.Outlined.AccountBalanceWallet,
        )

        // "Seus pedidos" da ficha do produto no web: pendentes e aprovados deste item.
        if (myOrdersLoading || myOrders.isNotEmpty()) {
            Text(
                text = "SEUS PEDIDOS",
                color = PremiumZinc500,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 2.dp),
            )
            if (myOrdersLoading) {
                PremiumLoadingState(text = "Carregando seus pedidos")
            } else {
                val pending = myOrders.filter { it.status == StoreOrderStatus.Pending }
                val approved = myOrders.filter { it.status != StoreOrderStatus.Pending }
                if (pending.isNotEmpty()) {
                    Text(
                        text = "PENDENTES",
                        color = PremiumAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                    pending.forEach { order ->
                        ProductOrderRow(order = order, onClick = { onOrderClick(order) })
                    }
                }
                if (approved.isNotEmpty()) {
                    Text(
                        text = "APROVADOS",
                        color = PremiumBrand,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 2.dp),
                    )
                    approved.forEach { order ->
                        ProductOrderRow(order = order, onClick = { onOrderClick(order) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductOrderRow(
    order: StoreOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumCard(modifier = modifier.clickable(onClick = onClick), accent = storeOrderColor(order.status)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Pedido #${order.id.take(8).uppercase()}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = listOf(order.createdAtLabel, order.amountLabel)
                        .filter(String::isNotBlank)
                        .joinToString(" • "),
                    color = PremiumZinc500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            PremiumChip(
                label = order.status.label,
                accent = storeOrderColor(order.status),
            )
        }
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
            subtitle = "PIX, comprovante e aprovação",
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
                        text = "Pagamento via PIX",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "O pedido fica pendente até a validação do comprovante.",
                        color = PremiumZinc400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            StoreTotalRow(label = "Produtos", value = state.subtotalLabel)
            StoreTotalRow(label = "Total do pedido", value = state.totalLabel, highlight = true)
        }

        StorePaymentInstructionsCard(
            items = state.items,
            totalLabel = state.totalLabel,
            orderCode = "",
        )

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
fun CheckoutConnectedScreen(
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
            subtitle = "PIX, comprovante e aprovação",
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
                        text = "Pagamento via PIX",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Cada produto usa o recebedor configurado no produto, Mini Vendor, liga, comissão, diretório ou loja oficial.",
                        color = PremiumZinc400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            StoreTotalRow(label = "Produtos", value = state.subtotalLabel)
            StoreTotalRow(label = "Total do pedido", value = state.totalLabel, highlight = true)
        }

        StorePaymentInstructionsCard(
            items = state.items,
            totalLabel = state.totalLabel,
            orderCode = "",
        )

        state.checkoutError?.let { message ->
            PremiumCard(accent = PremiumAmber) {
                Text(
                    text = message,
                    color = PremiumAmber,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        state.items.forEach { item ->
            CartItemCard(item = item)
        }

        PremiumPrimaryButton(
            text = if (state.isSubmitting) "Criando pedido" else "Criar pedido",
            onClick = onConfirmClick,
            enabled = state.items.isNotEmpty(),
            loading = state.isSubmitting,
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
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
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

        when {
            state.isLoading -> PremiumLoadingState(text = "Carregando pedidos")
            state.errorMessage != null -> PremiumEmptyState(
                title = "Pedidos indisponíveis",
                subtitle = state.errorMessage,
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            )
            state.filteredOrders.isEmpty() -> {
                PremiumEmptyState(
                    title = "Sem pedidos",
                    subtitle = "Nenhum pedido encontrado para esse filtro.",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                )
            }
            else -> state.filteredOrders.forEach { order ->
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
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
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

        StorePaymentInstructionsCard(
            items = order.items,
            totalLabel = order.amountLabel,
            orderCode = order.id.take(8).uppercase(),
        )
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
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            onBackClick = onBackClick,
        )
        PremiumEmptyState(
            title = "Pedido não carregado",
            subtitle = "Reabra Meus Pedidos para carregar o pedido dentro do tenant ativo.",
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
        )
    }
}

@Composable
private fun StorePaymentInstructionsCard(
    items: List<CartItem>,
    totalLabel: String,
    orderCode: String,
) {
    if (items.isEmpty()) return

    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val groupedItems = items.groupBy { item ->
        val product = item.product
        listOf(
            product.sellerType.remoteValue,
            product.sellerId,
            product.pixKey,
            product.pixHolder,
            product.receiptWhatsapp,
        ).joinToString("|")
    }.values.toList()

    groupedItems.forEachIndexed { index, group ->
        val product = group.first().product
        val recipientName = product.receiptName
            .ifBlank { product.sellerName }
            .ifBlank { product.sellerType.label }
        val productNames = group.joinToString(", ") { item ->
            buildString {
                append(item.quantity.coerceAtLeast(1))
                append("x ")
                append(item.product.name)
                if (item.variantLabel.isNotBlank()) append(" • ${item.variantLabel}")
                if (item.colorLabel.isNotBlank()) append(" • ${item.colorLabel}")
            }
        }
        val hasPix = product.pixKey.isNotBlank() || product.pixBank.isNotBlank() || product.pixHolder.isNotBlank()
        val whatsappUrl = buildStoreWhatsappUrl(
            phone = product.receiptWhatsapp,
            sellerName = recipientName,
            productNames = productNames,
            totalLabel = totalLabel,
            orderCode = orderCode,
        )
        val accent = if (hasPix) PremiumBrand else PremiumAmber

        PremiumCard(accent = accent) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PremiumChip(
                        label = if (groupedItems.size > 1) "Recebedor ${index + 1}" else "Pagamento",
                        icon = Icons.Outlined.Payment,
                        accent = accent,
                        filled = hasPix,
                    )
                    Text(
                        text = recipientName,
                        color = Color.White,
                        fontSize = 18.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = product.sellerType.label.uppercase(),
                        color = PremiumZinc500,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                }
                ProductStatusChip(status = product.status)
            }

            Text(
                text = productNames,
                color = PremiumZinc400,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
            )

            if (hasPix) {
                PremiumInfoRow(
                    label = "Chave PIX",
                    value = product.pixKey.ifBlank { "Não configurada" },
                    accent = accent,
                )
                PremiumInfoRow(
                    label = "Banco",
                    value = product.pixBank.ifBlank { "-" },
                    accent = PremiumZinc400,
                )
                PremiumInfoRow(
                    label = "Titular",
                    value = product.pixHolder.ifBlank { recipientName },
                    accent = PremiumZinc400,
                )
            } else {
                Text(
                    text = "Este produto ainda não tem PIX público configurado. O pedido será criado como pendente e a equipe informará o pagamento pelo canal oficial.",
                    color = PremiumAmber,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PremiumSecondaryButton(
                    text = "Copiar PIX",
                    onClick = { clipboard.setText(AnnotatedString(product.pixKey)) },
                    enabled = product.pixKey.isNotBlank(),
                    icon = Icons.Outlined.ContentCopy,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                PremiumSecondaryButton(
                    text = "WhatsApp",
                    onClick = { whatsappUrl?.let(uriHandler::openUri) },
                    enabled = whatsappUrl != null,
                    icon = Icons.Outlined.Send,
                    accent = PremiumBrandAccent,
                    modifier = Modifier.weight(1f),
                )
            }

            PremiumChip(
                label = if (orderCode.isNotBlank()) "Pedido #$orderCode" else "Será criado como pendente",
                accent = if (orderCode.isNotBlank()) storeOrderColor(StoreOrderStatus.Pending) else PremiumAmber,
            )
        }
    }
}

@Composable
private fun StoreOptionSelector(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    accent: Color,
) {
    PremiumCard(accent = accent) {
        Text(
            text = title.uppercase(),
            color = PremiumZinc500,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                StoreCategoryPill(
                    label = option,
                    selected = option == selected,
                    accent = accent,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun StoreQuantitySelector(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    accent: Color,
) {
    PremiumCard(accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "QUANTIDADE",
                    color = PremiumZinc500,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                )
                Text(
                    text = "Escolha antes de adicionar ao carrinho",
                    color = PremiumZinc400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StoreRoundIconButton(icon = Icons.Outlined.Remove, enabled = quantity > 1, accent = accent, onClick = onDecrease)
                Text(text = quantity.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                StoreRoundIconButton(icon = Icons.Outlined.Add, enabled = true, accent = accent, onClick = onIncrease)
            }
        }
    }
}

@Composable
private fun StoreRoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = if (enabled) accent.copy(alpha = 0.18f) else PremiumZinc900,
        border = BorderStroke(1.dp, if (enabled) accent.copy(alpha = 0.38f) else PremiumZinc800),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) accent else PremiumZinc500,
            modifier = Modifier.padding(10.dp),
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
                    StoreHeroButton(text = "Pedidos", icon = Icons.AutoMirrored.Outlined.ReceiptLong, onClick = onOrdersClick)
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

private fun buildStoreWhatsappUrl(
    phone: String,
    sellerName: String,
    productNames: String,
    totalLabel: String,
    orderCode: String,
): String? {
    val digits = phone.filter(Char::isDigit)
    if (digits.isBlank()) return null
    val normalizedPhone = if (digits.startsWith("55")) digits else "55$digits"
    val message = buildString {
        append("Olá, ")
        append(sellerName.ifBlank { "equipe USC" })
        append("! Segue o comprovante do pedido")
        if (orderCode.isNotBlank()) append(" #$orderCode")
        append(".\n\nItens: ")
        append(productNames)
        append("\nTotal: ")
        append(totalLabel)
    }
    return "https://wa.me/$normalizedPhone?text=${URLEncoder.encode(message, "UTF-8")}"
}
