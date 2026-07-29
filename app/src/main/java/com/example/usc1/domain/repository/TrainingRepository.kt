package com.example.usc1.domain.repository

import com.example.usc1.ui.training.TrainingUiState

interface TrainingRepository {
    suspend fun getTrainingHub(
        tenantId: String,
        userId: String,
        userName: String,
    ): TrainingUiState
}
