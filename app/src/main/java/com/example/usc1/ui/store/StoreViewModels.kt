package com.example.usc1.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.usc1.R
import com.example.usc1.core.session.UserSession
import com.example.usc1.data.repository.SupabaseStoreCatalogRepository
import com.example.usc1.data.repository.SupabaseStoreOrdersRepository
import com.example.usc1.domain.model.StoreCatalogProduct
import com.example.usc1.domain.model.StoreSellerType
import com.example.usc1.domain.repository.StoreCatalogRepository
import com.example.usc1.domain.repository.StoreOrdersRepository
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductDetailUiState(isLoading = true))
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: String) {
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
            }
        }
    }
}

class CartViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()
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
)

private val brlFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

private fun StoreCatalogProduct.toUiModel(): StoreProduct {
    return StoreProduct(
        id = id,
        name = name,
        description = description.ifBlank { "Produto disponível na loja do tenant ativo." },
        category = category,
        priceLabel = brlFormatter.format(price),
        status = when (status) {
            "em_breve" -> StoreProductStatus.ComingSoon
            "esgotado" -> StoreProductStatus.SoldOut
            else -> StoreProductStatus.Available
        },
        badge = seller.label,
        imageRes = seller.type.fallbackImageRes(),
        imageUrl = imageUrl,
        stockLabel = stockLabel(),
        reviewLabel = "Vendedor: ${seller.label}",
        sellerType = seller.type,
        sellerId = seller.id,
        sellerName = seller.label,
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
