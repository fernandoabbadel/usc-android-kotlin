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
import com.example.usc1.domain.model.StorePaymentRecipient
import com.example.usc1.domain.model.StorePlanScopeRow
import com.example.usc1.domain.model.StoreApprovalOutcome

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

    /**
     * Recebedores já salvos no escopo "produtos" (`paymentRecipients.ts` 115-139). É o que a tela
     * carrega junto com a lista, como o web faz em `produtos/page.tsx` 338-365.
     */
    suspend fun getProductPaymentRecipients(forceRefresh: Boolean): List<StorePaymentRecipient>

    /**
     * Diretório de membros aprovados que podem virar recebedores
     * (`paymentRecipients.ts` 141-210, teto de 400).
     *
     * Consulta **preguiçosa**: no web ela mora dentro do `CommerceReceiversManager`
     * (`produtos/page.tsx` 1835) e só roda quando o gerenciador abre. São duas leituras de até
     * 400 linhas; carregá-las a cada abertura da tela de produtos seria custo puro.
     */
    suspend fun getRecipientDirectory(): List<StorePaymentRecipient>

    /** `paymentRecipients.ts` 212-255. */
    suspend fun saveProductPaymentRecipients(
        recipients: List<StorePaymentRecipient>,
    ): List<StorePaymentRecipient>

    suspend fun getReviews(limit: Int, forceRefresh: Boolean): List<AdminStoreReview>

    suspend fun setReviewStatus(reviewId: String, status: AdminStoreReviewStatus)

    suspend fun getOrdersPage(
        mode: AdminStoreOrdersMode,
        page: Int,
        pageSize: Int,
        categoryLabel: String?,
    ): AdminStoreOrdersPage

    /**
     * Aprova o pedido no mesmo caminho direto que o web executa
     * (`storeService.ts` 1128-1345), relatando as etapas secundárias que falharem em vez de
     * silenciá-las como o `console.warn` do web.
     */
    suspend fun approveOrder(orderId: String, approvedBy: String): StoreApprovalOutcome

    suspend fun setOrderStatus(orderId: String, status: AdminStoreOrderStatus)
}
