package com.example.usc1.domain.repository

import com.example.usc1.core.session.UserSession
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val session: StateFlow<UserSession>

    suspend fun refreshSession()
    suspend fun signOut()
}
