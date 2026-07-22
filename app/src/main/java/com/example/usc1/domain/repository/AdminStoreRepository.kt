package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminStoreBundle
import com.example.usc1.domain.model.AdminStoreCategoriesBundle
import com.example.usc1.domain.model.AdminStoreCategoryForm
import com.example.usc1.domain.model.AdminStoreFinanceConfig
import com.example.usc1.domain.model.AdminStoreOrderStatus
import com.example.usc1.domain.model.AdminStoreOrdersMode
import com.example.usc1.domain.model.AdminStoreOrdersPage
import com.example.usc1.domain.model.AdminStoreProductForm
import com.example.usc1.domain.model.AdminStoreProductsPage
import com.example.usc1.domain.model.AdminStoreReview
import com.example.usc1.domain.model.AdminStoreReviewStatus

interface AdminStoreRepository {
    suspend fun getStoreBundle(forceRefresh: Boolean): AdminStoreBundle

    suspend fun saveFinanceConfig(config: AdminStoreFinanceConfig)

    suspend fun getCategories(
        tenantLogoUrl: String?,
        defaultButtonColor: String,
        forceRefresh: Boolean,
    ): AdminStoreCategoriesBundle

    suspend fun saveCategory(form: AdminStoreCategoryForm, tenantLogoUrl: String?)

    suspend fun saveCategoryDisplayOrder(categoryIds: List<String>)

    suspend fun setCategoryVisibility(categoryId: String, visible: Boolean)

    suspend fun setMiniVendorCategoryVisibility(miniVendorId: String, visible: Boolean)

    suspend fun getProductsPage(
        categoryLabel: String?,
        inactiveOnly: Boolean,
        forceRefresh: Boolean,
    ): AdminStoreProductsPage

    suspend fun saveProduct(
        form: AdminStoreProductForm,
        tenantName: String?,
        tenantLogoUrl: String?,
    )

    suspend fun setProductActive(productId: String, active: Boolean)

    suspend fun getReviews(limit: Int, forceRefresh: Boolean): List<AdminStoreReview>

    suspend fun setReviewStatus(reviewId: String, status: AdminStoreReviewStatus)

    suspend fun getOrdersPage(
        mode: AdminStoreOrdersMode,
        page: Int,
        pageSize: Int,
        categoryLabel: String?,
    ): AdminStoreOrdersPage

    suspend fun approveOrder(orderId: String, approvedBy: String)

    suspend fun setOrderStatus(orderId: String, status: AdminStoreOrderStatus)
}
