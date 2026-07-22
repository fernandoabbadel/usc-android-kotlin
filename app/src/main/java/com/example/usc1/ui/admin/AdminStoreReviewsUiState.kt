package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminStoreReview
import com.example.usc1.domain.model.AdminStoreReviewStatus

data class AdminStoreReviewsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val reviews: List<AdminStoreReview> = emptyList(),
    val page: Int = 1,
    val mutatingReviewId: String? = null,
) {
    val pendingReviews: List<AdminStoreReview>
        get() = reviews.filter { it.status == AdminStoreReviewStatus.Pending }

    val totalPages: Int
        get() = maxOf(1, ((pendingReviews.size + PageSize - 1) / PageSize))

    val pagedReviews: List<AdminStoreReview>
        get() {
            val safePage = page.coerceIn(1, totalPages)
            val start = (safePage - 1) * PageSize
            return pendingReviews.drop(start).take(PageSize)
        }

    companion object {
        const val PageSize = 20
    }
}
