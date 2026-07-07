package com.example.usc1.ui.store

import androidx.annotation.DrawableRes
import com.example.usc1.R

enum class StoreProductStatus(val label: String) {
    Available("Disponível"),
    SoldOut("Esgotado"),
    ComingSoon("Em breve"),
}

enum class StoreOrderStatus(val label: String) {
    Pending("Pendente"),
    Approved("Aprovado"),
    Cancelled("Cancelado"),
}

enum class StorePaymentStatus(val label: String) {
    WaitingPayment("Aguardando pagamento"),
    Paid("Pagamento aprovado"),
    Cancelled("Pagamento cancelado"),
}

data class StoreProduct(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val priceLabel: String,
    val status: StoreProductStatus,
    val badge: String,
    @DrawableRes val imageRes: Int,
    val stockLabel: String,
    val reviewLabel: String,
)

data class CartItem(
    val product: StoreProduct,
    val quantity: Int,
)

data class StoreOrder(
    val id: String,
    val title: String,
    val createdAtLabel: String,
    val status: StoreOrderStatus,
    val paymentStatus: StorePaymentStatus,
    val amountLabel: String,
    val items: List<CartItem>,
    val pickupLabel: String,
)

data class StoreUiState(
    val isLoading: Boolean = false,
    val selectedCategory: String = "Todos",
    val categories: List<String> = StoreMockData.categories,
    val products: List<StoreProduct> = StoreMockData.products,
    val cartCount: Int = 3,
    val errorMessage: String? = null,
)

data class CartUiState(
    val items: List<CartItem> = StoreMockData.cartItems,
    val subtotalLabel: String = "R$ 154,70",
    val serviceLabel: String = "R$ 0,00",
    val totalLabel: String = "R$ 154,70",
)

data class StoreOrdersUiState(
    val selectedStatus: StoreOrderStatus? = null,
    val orders: List<StoreOrder> = StoreMockData.orders,
)

object StoreMockData {
    val categories = listOf("Todos", "Fardas", "Festas", "Treino", "Acessórios")

    val products = listOf(
        StoreProduct(
            id = "camiseta-tubarao",
            name = "Camiseta Tubarão USC",
            description = "Dry fit preto com marca USC e detalhe neon. Visual de arquibancada para jogo, treino e rolê.",
            category = "Fardas",
            priceLabel = "R$ 79,90",
            status = StoreProductStatus.Available,
            badge = "Mais vendido",
            imageRes = R.drawable.logo_usc_wide,
            stockLabel = "18 unidades",
            reviewLabel = "4.9 • 128 avaliações",
        ),
        StoreProduct(
            id = "tirante-openbar",
            name = "Tirante Open Bar",
            description = "Tirante premium para evento, retirada no balcão e validação futura por QR Code.",
            category = "Festas",
            priceLabel = "R$ 34,90",
            status = StoreProductStatus.Available,
            badge = "Evento",
            imageRes = R.drawable.battle_forest,
            stockLabel = "Últimas 32",
            reviewLabel = "4.8 • 86 avaliações",
        ),
        StoreProduct(
            id = "short-treino",
            name = "Short Treino AAAKN",
            description = "Short leve com recorte esportivo e acabamento escuro, feito para treino e jogos.",
            category = "Treino",
            priceLabel = "R$ 69,90",
            status = StoreProductStatus.ComingSoon,
            badge = "Prévia",
            imageRes = R.drawable.logo_aaakn,
            stockLabel = "Chega em breve",
            reviewLabel = "Novo drop",
        ),
        StoreProduct(
            id = "caneca-cardume",
            name = "Caneca Cardume Livre",
            description = "Caneca colecionável com arte USC e acabamento dourado para membros ativos.",
            category = "Acessórios",
            priceLabel = "R$ 49,90",
            status = StoreProductStatus.SoldOut,
            badge = "Esgotado",
            imageRes = R.drawable.carteirinha_bg,
            stockLabel = "0 unidades",
            reviewLabel = "4.7 • 44 avaliações",
        ),
    )

    val cartItems = listOf(
        CartItem(products[0], quantity = 1),
        CartItem(products[1], quantity = 2),
    )

    val orders = listOf(
        StoreOrder(
            id = "LOJA-1842",
            title = "Kit Tubarão + Tirante",
            createdAtLabel = "Hoje • 14:20",
            status = StoreOrderStatus.Pending,
            paymentStatus = StorePaymentStatus.WaitingPayment,
            amountLabel = "R$ 154,70",
            items = cartItems,
            pickupLabel = "Retirada no balcão USC",
        ),
        StoreOrder(
            id = "LOJA-1721",
            title = "Camiseta Tubarão USC",
            createdAtLabel = "02 JUL • 19:10",
            status = StoreOrderStatus.Approved,
            paymentStatus = StorePaymentStatus.Paid,
            amountLabel = "R$ 79,90",
            items = listOf(CartItem(products[0], quantity = 1)),
            pickupLabel = "Entregue na sede",
        ),
        StoreOrder(
            id = "LOJA-1660",
            title = "Caneca Cardume Livre",
            createdAtLabel = "26 JUN • 09:40",
            status = StoreOrderStatus.Cancelled,
            paymentStatus = StorePaymentStatus.Cancelled,
            amountLabel = "R$ 49,90",
            items = listOf(CartItem(products[3], quantity = 1)),
            pickupLabel = "Cancelado pelo estoque",
        ),
    )

    fun productById(id: String): StoreProduct =
        products.firstOrNull { it.id == id } ?: products.first()

    fun orderById(id: String): StoreOrder =
        orders.firstOrNull { it.id == id } ?: orders.first()
}
