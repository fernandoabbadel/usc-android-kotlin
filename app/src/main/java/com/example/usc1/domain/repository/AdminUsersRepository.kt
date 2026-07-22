package com.example.usc1.domain.repository

import com.example.usc1.domain.model.AdminUserStatus
import com.example.usc1.domain.model.AdminUserProfile
import com.example.usc1.domain.model.AdminUserRoleUpdate
import com.example.usc1.domain.model.AdminUserTurmaLeaderUpdate
import com.example.usc1.domain.model.AdminUserUpdate
import com.example.usc1.domain.model.AdminUsersFilters
import com.example.usc1.domain.model.AdminUsersPage

interface AdminUsersRepository {
    suspend fun getUsersPage(
        pageSize: Int,
        cursorId: String?,
        filters: AdminUsersFilters,
        forceRefresh: Boolean,
    ): AdminUsersPage

    suspend fun getUserProfile(userId: String, forceRefresh: Boolean): AdminUserProfile?

    suspend fun updateUser(payload: AdminUserUpdate)

    suspend fun setUserStatus(userId: String, status: AdminUserStatus)

    suspend fun updateUserRole(payload: AdminUserRoleUpdate)

    suspend fun setUserTurmaLeader(payload: AdminUserTurmaLeaderUpdate)

    suspend fun deleteUser(userId: String)
}
