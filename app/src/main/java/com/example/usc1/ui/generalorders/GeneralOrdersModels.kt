package com.example.usc1.ui.generalorders

enum class GeneralOrderType(val label: String) {
    Events("Eventos"),
    Store("Loja"),
    Plans("Planos"),
}

enum class GeneralOrderStatus(val label: String) {
    Pending("Pendente"),
    Approved("Aprovado"),
    Cancelled("Cancelado"),
}

data class GeneralOrder(
    val id: String,
    val title: String,
    val type: GeneralOrderType,
    val status: GeneralOrderStatus,
    val amountLabel: String,
    val createdAtLabel: String,
    val description: String,
)

data class GeneralOrdersUiState(
    val selectedType: GeneralOrderType? = null,
    val selectedStatus: GeneralOrderStatus? = null,
    val orders: List<GeneralOrder> = GeneralOrdersMockData.orders,
)

object GeneralOrdersMockData {
    val orders = listOf(
        GeneralOrder("EVT-1001", "Intermed USC", GeneralOrderType.Events, GeneralOrderStatus.Pending, "R$ 120,00", "Hoje • 14:20", "Ingresso aguardando aprovação."),
        GeneralOrder("LOJ-1842", "Kit Tubarão", GeneralOrderType.Store, GeneralOrderStatus.Approved, "R$ 154,70", "Ontem • 19:10", "Pedido liberado para retirada."),
        GeneralOrder("PLA-9102", "Cardume Livre", GeneralOrderType.Plans, GeneralOrderStatus.Pending, "R$ 19,90", "18 JUN • 13:22", "Renovação mockada de plano."),
        GeneralOrder("LOJ-1660", "Caneca Cardume", GeneralOrderType.Store, GeneralOrderStatus.Cancelled, "R$ 49,90", "26 JUN • 09:40", "Cancelado pelo estoque."),
    )

    fun orderById(id: String): GeneralOrder =
        orders.firstOrNull { it.id == id } ?: orders.first()
}
