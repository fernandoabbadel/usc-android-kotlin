package com.example.usc1.domain.model

data class AdminDashboardBundle(
    val stats: AdminDashboardStats,
    val recentUsers: List<AdminDashboardRecentUser>,
    val recentActivity: List<AdminDashboardActivityLog>,
)

data class AdminDashboardStats(
    val totalUsers: Long,
    val totalEvents: Long,
    val totalSales: Long?,
    val activeChamps: Long?,
)

data class AdminDashboardRecentUser(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val className: String,
    val role: String,
    val createdAt: String?,
)

data class AdminDashboardActivityLog(
    val id: String,
    val userName: String,
    val action: String,
    val resource: String,
    val timestamp: String?,
)
