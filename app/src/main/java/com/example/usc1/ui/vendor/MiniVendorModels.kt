package com.example.usc1.ui.vendor

import com.example.usc1.R

enum class MiniVendorOrderStatus(val label: String, val remoteValue: String) {
    Pending("Pendente", "pendente"),
    Approved("Confirmado", "approved"),
    Rejected("Rejeitado", "rejected"),
    Delivered("Entregue", "delivered"),
}

data class MiniVendorProduct(
    val id: String,
    val name: String,
    val priceLabel: String,
    val oldPriceLabel: String? = null,
    val category: String = "",
    val description: String = "",
    val stockCount: Int = 0,
    val soldCount: Int = 0,
    val clicksCount: Int = 0,
    val stockLabel: String,
    val status: String,
    val soldLabel: String = "",
    val clicksLabel: String = "",
    val tagLabel: String = "",
    val tagColor: String = "zinc",
    val tagEffect: String = "none",
    val lot: String = "",
    val imageUrl: String? = null,
    val imageRes: Int = R.drawable.logo_usc_wide,
    val remoteStatus: String = "ativo",
    val active: Boolean = true,
)

data class MiniVendorProductForm(
    val productId: String? = null,
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val price: String = "",
    val oldPrice: String = "",
    val stock: String = "",
    val lot: String = "",
    val tagLabel: String = "",
    val tagColor: String = "emerald",
    val tagEffect: String = "none",
    val remoteStatus: String = "ativo",
    val active: Boolean = true,
)

fun MiniVendorProduct.toProductForm(): MiniVendorProductForm {
    return MiniVendorProductForm(
        productId = id,
        name = name,
        description = description,
        imageUrl = imageUrl.orEmpty(),
        price = priceLabel.moneyLabelToInput(),
        oldPrice = oldPriceLabel.moneyLabelToInput(),
        stock = stockCount.takeIf { it > 0 }?.toString().orEmpty(),
        lot = lot,
        tagLabel = tagLabel,
        tagColor = tagColor,
        tagEffect = tagEffect,
        remoteStatus = remoteStatus,
        active = active,
    )
}

data class MiniVendorOrder(
    val id: String,
    val customerName: String,
    val productName: String,
    val amountLabel: String,
    val createdAtLabel: String,
    val status: MiniVendorOrderStatus,
    val productId: String = "",
    val userId: String = "",
    val approvedBy: String = "",
    /** Nome resolvido de quem aprovou, como `fetchCanonicalUserVisuals` faz no web. */
    val approvedByName: String = "",
    val approvedAtLabel: String = "",
    val quantity: Int = 1,
)

data class MiniVendorUiState(
    val isLoading: Boolean = false,
    val isSavingProfile: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val profileId: String = "",
    val storeName: String = "Mini Vendor",
    /** Status cru do cadastro (`pending`, `approved`, `rejected`, `disabled`). */
    val profileStatus: String = "",
    val statusLabel: String = "Carregando dados reais",
    val slug: String = "",
    val description: String = "",
    val logoUrl: String? = null,
    val coverUrl: String? = null,
    val pixKey: String = "",
    val pixBank: String = "",
    val pixHolder: String = "",
    val pixWhatsapp: String = "",
    val instagram: String = "",
    val instagramEnabled: Boolean = false,
    val whatsapp: String = "",
    val whatsappEnabled: Boolean = false,
    val profileVisible: Boolean = true,
    val categoryVisible: Boolean = true,
    val productsVisible: Boolean = true,
    val categoryButtonColor: String = "#2563EB",
    val approvedAtLabel: String = "",
    val totalRevenueLabel: String = "R$ 0,00",
    val pendingAmountLabel: String = "R$ 0,00",
    val products: List<MiniVendorProduct> = emptyList(),
    val pendingOrders: List<MiniVendorOrder> = emptyList(),
    val approvedOrders: List<MiniVendorOrder> = emptyList(),
) {
    val hasProfile: Boolean
        get() = profileId.isNotBlank()

    val ordersCount: Int
        get() = pendingOrders.size + approvedOrders.size
}

data class MiniVendorProfileForm(
    val storeName: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val coverUrl: String = "",
    val pixKey: String = "",
    val pixBank: String = "",
    val pixHolder: String = "",
    val pixWhatsapp: String = "",
    val instagram: String = "",
    val instagramEnabled: Boolean = false,
    val whatsapp: String = "",
    val whatsappEnabled: Boolean = false,
    val profileVisible: Boolean = true,
    val categoryVisible: Boolean = true,
    val productsVisible: Boolean = true,
    val categoryButtonColor: String = "#2563EB",
)

fun MiniVendorUiState.toProfileForm(): MiniVendorProfileForm {
    return MiniVendorProfileForm(
        storeName = storeName.takeIf { hasProfile }.orEmpty(),
        description = description,
        logoUrl = logoUrl.orEmpty(),
        coverUrl = coverUrl.orEmpty(),
        pixKey = pixKey,
        pixBank = pixBank,
        pixHolder = pixHolder,
        pixWhatsapp = pixWhatsapp,
        instagram = instagram,
        instagramEnabled = instagramEnabled,
        whatsapp = whatsapp,
        whatsappEnabled = whatsappEnabled,
        profileVisible = profileVisible,
        categoryVisible = categoryVisible,
        productsVisible = productsVisible,
        categoryButtonColor = categoryButtonColor,
    )
}

private fun String?.moneyLabelToInput(): String {
    return orEmpty()
        .replace("R$", "")
        .replace(".", "")
        .trim()
}

object MiniVendorMockData {
    val products = listOf(
        MiniVendorProduct(
            id = "mv-kit-01",
            name = "Kit Pós-Evento",
            priceLabel = "R$ 39,90",
            stockLabel = "24 disponíveis",
            status = "Publicado",
            imageRes = R.drawable.battle_forest,
        ),
        MiniVendorProduct(
            id = "mv-ficha-02",
            name = "Ficha Bebida",
            priceLabel = "R$ 12,00",
            stockLabel = "148 disponíveis",
            status = "Evento",
            imageRes = R.drawable.logo_usc_wide,
        ),
        MiniVendorProduct(
            id = "mv-copo-03",
            name = "Copo USC",
            priceLabel = "R$ 18,00",
            stockLabel = "Esgotando",
            status = "Destaque",
            imageRes = R.drawable.logo_platform_web,
        ),
    )

    val orders = listOf(
        MiniVendorOrder("MV-901", "Fernando USC", "Kit Pós-Evento", "R$ 39,90", "Hoje • 15:02", MiniVendorOrderStatus.Pending),
        MiniVendorOrder("MV-899", "Ana Costa", "Ficha Bebida", "R$ 24,00", "Hoje • 14:41", MiniVendorOrderStatus.Pending),
        MiniVendorOrder("MV-812", "Lívia Martins", "Copo USC", "R$ 18,00", "Ontem • 21:10", MiniVendorOrderStatus.Approved),
        MiniVendorOrder("MV-780", "Lucas T9", "Kit Pós-Evento", "R$ 39,90", "30 JUN • 19:33", MiniVendorOrderStatus.Approved),
    )

    val previewState = MiniVendorUiState(
        profileId = "preview-mini-vendor",
        storeName = "Mini Vendor USC",
        statusLabel = "Aprovado para vender",
        description = "Produtos, pedidos e financeiro da lojinha do aluno.",
        totalRevenueLabel = "R$ 1.284,70",
        pendingAmountLabel = "R$ 342,90",
        products = products,
        pendingOrders = orders.filter { it.status == MiniVendorOrderStatus.Pending },
        approvedOrders = orders.filter { it.status == MiniVendorOrderStatus.Approved },
    )
}
