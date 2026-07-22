package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminStoreCatalog
import com.example.usc1.domain.model.AdminStoreCategoriesBundle
import com.example.usc1.domain.model.AdminStoreCategory
import com.example.usc1.domain.model.AdminStoreCategoryForm
import com.example.usc1.domain.model.AdminStoreSellerType

data class AdminStoreCategoriesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSavingOrder: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val tenantId: String = "",
    val tenantLogoUrl: String = "",
    val defaultButtonColor: String = AdminStoreCatalog.CategoryColorDefault,
    val categories: List<AdminStoreCategory> = emptyList(),
    val form: AdminStoreCategoryForm = AdminStoreCategoryForm(),
    val visibilityActionKey: String = "",
    val orderedCategoryKeys: List<String> = emptyList(),
    val isOrderPanelOpen: Boolean = true,
) {
    val tenantCategories: List<AdminStoreCategory>
        get() = categories.filter { it.sellerType == AdminStoreSellerType.Tenant }

    val miniVendorCategories: List<AdminStoreCategory>
        get() = categories.filter { it.sellerType == AdminStoreSellerType.MiniVendor }

    val leagueCategories: List<AdminStoreCategory>
        get() = categories.filter { it.sellerType == AdminStoreSellerType.League }

    val editingCategory: AdminStoreCategory?
        get() = form.categoryId?.let { categoryId ->
            categories.firstOrNull { it.categoryId == categoryId }
        } ?: categories.firstOrNull { it.key == form.previousName }

    val orderableCategories: List<AdminStoreCategory>
        get() {
            val map = categories.filter { it.categoryId != null }.associateBy { it.key }
            return orderedCategoryKeys.mapNotNull(map::get)
        }

    val defaultOrderedKeys: List<String>
        get() = categories.filter { it.categoryId != null }.map { it.key }

    val isOrderDirty: Boolean
        get() = orderedCategoryKeys != defaultOrderedKeys

    val nonOrderableCategoriesCount: Int
        get() = categories.count { it.categoryId == null }
}

fun AdminStoreCategoriesBundle.toUiState(
    current: AdminStoreCategoriesUiState,
    tenantLogoUrl: String,
): AdminStoreCategoriesUiState {
    val defaultKeys = categories.filter { it.categoryId != null }.map { it.key }
    val mergedOrder = if (current.orderedCategoryKeys.isEmpty()) {
        defaultKeys
    } else {
        val previous = current.orderedCategoryKeys.filter(defaultKeys::contains)
        previous + defaultKeys.filterNot(previous::contains)
    }
    return current.copy(
        isLoading = false,
        errorMessage = null,
        tenantId = tenantId,
        tenantLogoUrl = tenantLogoUrl,
        defaultButtonColor = defaultButtonColor,
        categories = categories,
        orderedCategoryKeys = mergedOrder,
        form = if (current.form.nome.isBlank() && current.form.coverImg.isBlank() && current.form.categoryId == null) {
            current.form.copy(buttonColor = defaultButtonColor)
        } else {
            current.form
        },
    )
}
