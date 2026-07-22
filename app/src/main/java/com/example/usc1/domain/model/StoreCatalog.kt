package com.example.usc1.domain.model

enum class StoreSellerType(val remoteValue: String, val label: String) {
    Tenant("tenant", "Atlética"),
    Liga("liga", "Liga"),
    Comissao("comissao", "Comissão"),
    Diretorio("diretorio", "Diretório"),
    MiniVendor("mini_vendor", "Mini-vendor"),
    Unknown("unknown", "Vendedor");

    companion object {
        fun fromRemote(value: String?, sellerId: String?, activeTenantId: String?): StoreSellerType {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return when (normalized) {
                "mini_vendor", "minivendor" -> MiniVendor
                "league", "liga" -> Liga
                "commission", "comissao", "comissão" -> Comissao
                "directory", "diretorio", "diretório", "da" -> Diretorio
                "tenant", "" -> {
                    val cleanSellerId = sellerId.orEmpty().trim()
                    val cleanTenantId = activeTenantId.orEmpty().trim()
                    if (cleanTenantId.isNotEmpty() && cleanSellerId.isNotEmpty() && cleanSellerId != cleanTenantId) {
                        Liga
                    } else {
                        Tenant
                    }
                }
                else -> Unknown
            }
        }
    }
}

data class StoreSeller(
    val type: StoreSellerType,
    val id: String,
    val name: String,
    val logoUrl: String? = null,
) {
    val label: String
        get() = name.ifBlank { type.label }
}

data class StoreCatalogProduct(
    val id: String,
    val tenantId: String,
    val name: String,
    val description: String,
    val category: String,
    val imageUrl: String? = null,
    val price: Double,
    val oldPrice: Double? = null,
    val stock: Int? = null,
    val lote: String? = null,
    val status: String,
    val tagLabel: String? = null,
    val seller: StoreSeller,
    val createdAt: String? = null,
)

data class StoreCatalogCategory(
    val id: String,
    val name: String,
    val seller: StoreSeller,
    val displayOrder: Int? = null,
    val visible: Boolean = true,
)

data class StoreCatalogPage(
    val products: List<StoreCatalogProduct>,
    val categories: List<StoreCatalogCategory>,
    val hasMore: Boolean,
    val page: Int,
    val pageSize: Int,
    val activeTenantId: String,
)
