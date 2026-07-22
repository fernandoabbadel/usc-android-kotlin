package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminArenaUser

interface AdminGamesRepository {
    suspend fun getArenaUsers(forceRefresh: Boolean = false): List<AdminArenaUser>
}
