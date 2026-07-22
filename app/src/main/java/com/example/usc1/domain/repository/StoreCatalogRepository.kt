package com.example.usc1.domain.repository

import com.example.usc1.domain.model.StoreCatalogPage
import com.example.usc1.domain.model.StoreCatalogProduct

interface StoreCatalogRepository {
    suspend fun getProductsPage(
        category: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
        forceRefresh: Boolean = false,
    ): StoreCatalogPage

    suspend fun getProductById(productId: String): StoreCatalogProduct?
}
