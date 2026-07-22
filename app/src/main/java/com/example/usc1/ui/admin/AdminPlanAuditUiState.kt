package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminPlanRequestStatus
import com.example.usc1.domain.model.AdminPlanSubscriptionStatus

data class AdminPlanAuditUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val pendingRequests: Int = 0,
    val approvedRequests: Int = 0,
    val rejectedRequests: Int = 0,
    val activeSubscriptions: Int = 0,
    val pendingSubscriptions: Int = 0,
) {
    companion object {
        fun fromRows(
            requests: List<com.example.usc1.domain.model.AdminPlanRequest>,
            subscriptions: List<com.example.usc1.domain.model.AdminPlanSubscription>,
        ): AdminPlanAuditUiState {
            return AdminPlanAuditUiState(
                isLoading = false,
                pendingRequests = requests.count { it.status == AdminPlanRequestStatus.Pendente },
                approvedRequests = requests.count { it.status == AdminPlanRequestStatus.Aprovado },
                rejectedRequests = requests.count { it.status == AdminPlanRequestStatus.Rejeitado },
                activeSubscriptions = subscriptions.count { it.status == AdminPlanSubscriptionStatus.Ativo },
                pendingSubscriptions = subscriptions.count { it.status == AdminPlanSubscriptionStatus.Pendente },
            )
        }
    }
}
