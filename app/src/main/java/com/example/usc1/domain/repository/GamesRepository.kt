package com.example.usc1.domain.repository

import com.example.usc1.ui.games.GamesUiState

interface GamesRepository {
    suspend fun getGamesHub(
        tenantId: String,
        userId: String,
        userName: String,
    ): GamesUiState
}
