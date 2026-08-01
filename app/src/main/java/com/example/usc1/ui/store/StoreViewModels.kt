package com.example.usc1.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.R
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseStoreCatalogRepository
import com.example.usc1.data.repository.SupabaseStoreOrdersRepository
import com.example.usc1.domain.model.StoreCatalogCategory
import com.example.usc1.domain.model.StoreCatalogProduct
import com.example.usc1.domain.model.StoreSellerType
import com.example.usc1.domain.repository.StoreCatalogRepository
import com.example.usc1.domain.repository.StoreOrdersRepository
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StoreViewModel(
    private val storeRepository: StoreCatalogRepository = SupabaseStoreCatalogRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(StoreUiState(isLoading = true))
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        loadStore()
    }

    fun selectCategory(category: String) {
        loadStore(category = category)
    }

    fun refresh() {
        loadStore(category = _uiState.value.selectedCategory, forceRefresh = true)
    }

    private fun loadStore(
        category: String = _uiState.value.selectedCategory,
        page: Int = 1,
        forceRefresh: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedCategory = category,
                    errorMessage = null,
                )
            }
            try {
                val pageResult = storeRepository.getProductsPage(
                    category = category.takeUnless { it == AllCategory },
                    page = page,
                    pageSize = StorePageSize,
                    forceRefresh = forceRefresh,
                )
                val categories = buildList {
                    add(AllCategory)
                    addAll(pageResult.categories.map { it.name })
                    addAll(pageResult.products.map { it.category })
                }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedCategory = category,
                        categories = categories,
                        categoryCards = pageResult.categories.map { storeCategory -> storeCategory.toUiModel() },
                        products = pageResult.products.map { product -> product.toUiModel() },
                        hasMore = pageResult.hasMore,
                        page = pageResult.page,
                        activeTenantId = pageResult.activeTenantId,
                        errorMessage = null,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        products = emptyList(),
                        errorMessage = error.message ?: "Não foi possível carregar a loja.",
                    )
                }
            }
        }
    }

    private companion object {
        const val AllCategory = "Todos"
        const val StorePageSize = 20
    }
}

class ProductDetailViewModel(
    private val storeRepository: StoreCatalogRepository = SupabaseStoreCatalogRepository(),
    private val ordersRepository: StoreOrdersRepository = SupabaseStoreOrdersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductDetailUiState(isLoading = true))
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: String, session: UserSession? = null) {
        viewModelScope.launch {
            _uiState.value = ProductDetailUiState(isLoading = true)
            try {
                val product = storeRepository.getProductById(productId)
                _uiState.value = if (product == null) {
                    ProductDetailUiState(errorMessage = "Produto não encontrado no tenant ativo.")
                } else {
                    ProductDetailUiState(product = product.toUiModel())
                }
            } catch (error: Throwable) {
                _uiState.value = ProductDetailUiState(
                    errorMessage = error.message ?: "Não foi possível carregar o produto.",
                )
                return@launch
            }

            val tenantId = session?.tenant?.id.orEmpty().trim()
            val userId = session?.user?.id.orEmpty().trim()
            if (userId.isBlank()) return@launch

            _uiState.update { it.copy(myOrdersLoading = true) }
            runCatching { ordersRepository.getOrders(tenantId = tenantId, userId = userId) }
                .onSuccess { orders ->
                    // O web lista apenas os pedidos deste produto na ficha.
                    val scoped = orders.filter { order ->
                        order.items.any { it.product.id == productId }
                    }
                    _uiState.update { it.copy(myOrdersLoading = false, myOrders = scoped) }
                }
                .onFailure {
                    _uiState.update { it.copy(myOrdersLoading = false, myOrders = emptyList()) }
                }
        }
    }
}

class CartViewModel(
    private val ordersRepository: StoreOrdersRepository = SupabaseStoreOrdersRepository(),
) : ViewModel() {
    val uiState: StateFlow<CartUiState> = sharedCartState.asStateFlow()

    fun addProduct(
        product: StoreProduct,
        quantity: Int,
        variantLabel: String = "",
        colorLabel: String = "",
    ) {
        val safeQuantity = quantity.coerceAtLeast(1)
        sharedCartState.update { current ->
            val existingIndex = current.items.indexOfFirst {
                it.product.id == product.id &&
                    it.variantLabel == variantLabel &&
                    it.colorLabel == colorLabel
            }
            val nextItems = if (existingIndex >= 0) {
                current.items.mapIndexed { index, item ->
                    if (index == existingIndex) item.copy(quantity = item.quantity + safeQuantity) else item
                }
            } else {
                current.items + CartItem(
                    product = product,
                    quantity = safeQuantity,
                    variantLabel = variantLabel,
                    colorLabel = colorLabel,
                )
            }
            nextItems.toCartState()
        }
    }

    fun clearCart() {
        sharedCartState.value = CartUiState()
    }

    fun submitCheckout(
        session: UserSession,
        onSuccess: (String?) -> Unit,
    ) {
        val current = sharedCartState.value
        if (current.isSubmitting) return
        val items = current.items
        if (items.isEmpty()) {
            sharedCartState.update {
                it.copy(checkoutError = "Seu carrinho está vazio.")
            }
            return
        }
        val tenantId = session.tenant?.id.orEmpty()
        val user = session.user
        if (tenantId.isBlank() || user == null) {
            sharedCartState.update {
                it.copy(checkoutError = "Entre na atlética para concluir o pedido.")
            }
            return
        }

        viewModelScope.launch {
            sharedCartState.update { it.copy(isSubmitting = true, checkoutError = null) }
            try {
                val userPlanNames = listOf(
                    user.planName,
                    user.planBadge,
                    user.role.name,
                ).map(String::trim).filter(String::isNotBlank)
                val userPlanIds = listOf(
                    user.planBadge,
                    user.role.name,
                ).map(String::trim).filter(String::isNotBlank)

                val createdOrders = items.map { item ->
                    ordersRepository.createOrder(
                        tenantId = tenantId,
                        userId = user.id,
                        userName = user.name.ifBlank { user.email },
                        item = item,
                        userPlanNames = userPlanNames,
                        userPlanIds = userPlanIds,
                    )
                }
                val whatsappUrl = buildCheckoutWhatsappUrl(
                    items = items,
                    orderCode = createdOrders.firstOrNull()?.id?.take(8)?.uppercase(Locale.ROOT).orEmpty(),
                    totalLabel = current.totalLabel,
                )
                sharedCartState.value = CartUiState()
                onSuccess(whatsappUrl)
            } catch (error: Throwable) {
                sharedCartState.update {
                    it.copy(
                        isSubmitting = false,
                        checkoutError = error.message ?: "Não foi possível criar o pedido.",
                    )
                }
            }
        }
    }

    private companion object {
        private val sharedCartState = MutableStateFlow(CartUiState())
    }
}

private fun buildCheckoutWhatsappUrl(
    items: List<CartItem>,
    orderCode: String,
    totalLabel: String,
): String? {
    val firstProductWithWhatsapp = items
        .map { it.product }
        .firstOrNull { product -> product.receiptWhatsapp.filter(Char::isDigit).isNotBlank() }
        ?: return null
    val digits = firstProductWithWhatsapp.receiptWhatsapp.filter(Char::isDigit)
    val normalizedPhone = if (digits.startsWith("55")) digits else "55$digits"
    val sellerName = firstProductWithWhatsapp.receiptName
        .ifBlank { firstProductWithWhatsapp.sellerName }
        .ifBlank { firstProductWithWhatsapp.sellerType.label }
        .ifBlank { "equipe USC" }
    val productLines = items.joinToString("\n") { item ->
        buildString {
            append("- ")
            append(item.quantity.coerceAtLeast(1))
            append("x ")
            append(item.product.name)
            if (item.variantLabel.isNotBlank()) append(" • ${item.variantLabel}")
            if (item.colorLabel.isNotBlank()) append(" • ${item.colorLabel}")
        }
    }
    val message = buildString {
        append("Olá, ")
        append(sellerName)
        append("! Segue o comprovante do pedido")
        if (orderCode.isNotBlank()) append(" #$orderCode")
        append(".\n\nItens:\n")
        append(productLines)
        append("\n\nTotal: ")
        append(totalLabel)
    }
    return "https://wa.me/$normalizedPhone?text=${URLEncoder.encode(message, "UTF-8")}"
}

class StoreOrdersViewModel(
    private val ordersRepository: StoreOrdersRepository = SupabaseStoreOrdersRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(StoreOrdersUiState(isLoading = true))
    val uiState: StateFlow<StoreOrdersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var loadedKey: String? = null

    fun load(
        session: UserSession,
        forceRefresh: Boolean = false,
    ) {
        val tenantId = session.tenant?.id.orEmpty()
        val userId = session.user?.id.orEmpty()
        val key = "$tenantId:$userId"
        if (!forceRefresh && loadedKey == key && !_uiState.value.isLoading) return
        if (tenantId.isBlank() || userId.isBlank()) {
            _uiState.value = StoreOrdersUiState(
                isLoading = false,
                errorMessage = "Entre na atlética para ver seus pedidos da loja.",
            )
            return
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val orders = ordersRepository.getOrders(
                    tenantId = tenantId,
                    userId = userId,
                )
                loadedKey = key
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        orders = orders,
                        errorMessage = null,
                    )
                }
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Não foi possível carregar os pedidos da loja.",
                    )
                }
            }
        }
    }

    fun selectStatus(status: StoreOrderStatus?) {
        _uiState.update { current ->
            current.copy(selectedStatus = status)
        }
    }

    fun findOrder(orderId: String): StoreOrder? {
        return _uiState.value.orders.firstOrNull { it.id == orderId }
    }
}

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: StoreProduct? = null,
    val errorMessage: String? = null,
    /** Bloco "Seus pedidos" da ficha do produto no web (`/loja/[id]`). */
    val myOrders: List<StoreOrder> = emptyList(),
    val myOrdersLoading: Boolean = false,
) {
    val pendingOrders: List<StoreOrder>
        get() = myOrders.filter { it.status == StoreOrderStatus.Pending }

    val approvedOrders: List<StoreOrder>
        get() = myOrders.filter { it.status != StoreOrderStatus.Pending }
}

private val brlFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

private fun List<CartItem>.toCartState(): CartUiState {
    val subtotal = sumOf { item -> item.product.priceValue * item.quantity.coerceAtLeast(1) }
    return CartUiState(
        items = this,
        subtotalLabel = brlFormatter.format(subtotal),
        serviceLabel = brlFormatter.format(0.0),
        totalLabel = brlFormatter.format(subtotal),
        checkoutError = null,
    )
}

private fun StoreCatalogCategory.toUiModel(): StoreCategory {
    return StoreCategory(
        id = id,
        name = name,
        sellerType = seller.type,
        sellerId = seller.id,
        sellerName = seller.label,
        sellerLogoUrl = seller.logoUrl,
        coverImageUrl = coverImageUrl,
        buttonColor = buttonColor,
        isReceivingOrders = isReceivingOrders,
    )
}

private fun StoreCatalogProduct.toUiModel(): StoreProduct {
    return StoreProduct(
        id = id,
        name = name,
        description = description.ifBlank { "Produto disponível na loja do tenant ativo." },
        category = category,
        priceValue = price,
        priceLabel = brlFormatter.format(price),
        status = when (status) {
            "em_breve" -> StoreProductStatus.ComingSoon
            "esgotado" -> StoreProductStatus.SoldOut
            else -> StoreProductStatus.Available
        },
        badge = tagLabel ?: seller.label,
        tagColor = tagColor,
        tagEffect = tagEffect,
        imageRes = seller.type.fallbackImageRes(),
        imageUrl = imageUrl,
        oldPriceLabel = oldPrice?.let { brlFormatter.format(it) },
        colors = colors,
        variants = variants.map { variant ->
            listOf(variant.color, variant.size)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
                .ifBlank { "Variação ${variant.id}" }
        },
        characteristics = characteristics,
        likesCount = likesCount,
        soldCount = soldCount,
        clicksCount = clicksCount,
        stockLabel = stockLabel(),
        reviewLabel = "Vendedor: ${seller.label}",
        pixKey = paymentConfig?.pixKey.orEmpty(),
        pixBank = paymentConfig?.bank.orEmpty(),
        pixHolder = paymentConfig?.holder.orEmpty(),
        receiptWhatsapp = paymentConfig?.whatsapp.orEmpty(),
        receiptName = paymentConfig?.recipientName.orEmpty(),
        sellerType = seller.type,
        sellerId = seller.id,
        sellerName = seller.label,
        sellerLogoUrl = seller.logoUrl,
    )
}

private fun StoreCatalogProduct.stockLabel(): String {
    val cleanStock = stock
    return when {
        status == "em_breve" -> "Chega em breve"
        cleanStock == null -> lote?.let { "Lote $it" } ?: "Estoque sob consulta"
        cleanStock <= 0 -> "0 unidades"
        cleanStock == 1 -> "1 unidade"
        else -> "$cleanStock unidades"
    }
}

private fun StoreSellerType.fallbackImageRes(): Int {
    return when (this) {
        StoreSellerType.Tenant -> R.drawable.logo_usc_wide
        StoreSellerType.MiniVendor -> R.drawable.logo_platform_web
        StoreSellerType.Liga,
        StoreSellerType.Comissao,
        StoreSellerType.Diretorio -> R.drawable.logo_platform_web
        StoreSellerType.Unknown -> R.drawable.carteirinha_bg
    }
}
