package com.example.usc1.ui.admin

import com.example.usc1.domain.model.AdminMentorshipLabelsConfig

data class AdminMentorshipUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val labels: AdminMentorshipLabelsConfig = AdminMentorshipLabelsConfig(),
)
