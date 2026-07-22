package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminPlanSubscription
import com.example.usc1.domain.model.AdminPlanSubscriptionListKind

data class AdminPlanSubscriptionsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val kind: AdminPlanSubscriptionListKind = AdminPlanSubscriptionListKind.BichoSolto,
    val rows: List<AdminPlanSubscription> = emptyList(),
    val page: Int = 1,
) {
    val totalPages: Int
        get() = kotlin.math.max(1, kotlin.math.ceil(rows.size / PageSize.toDouble()).toInt())

    val pagedRows: List<AdminPlanSubscription>
        get() = rows.drop((page - 1) * PageSize).take(PageSize)

    val canGoPrevious: Boolean
        get() = page > 1

    val canGoNext: Boolean
        get() = page < totalPages

    companion object {
        const val PageSize = 20
    }
}
