package com.example.usc1.domain.model

data class AdminActivityLogRecord(
    val id: String,
    val userId: String,
    val userName: String,
    val action: String,
    val resource: String,
    val details: String,
    val timestamp: String,
)

data class AdminActivityLogsPage(
    val logs: List<AdminActivityLogRecord>,
    val page: Int,
    val pageSize: Int,
    val nextCursor: String?,
    val hasMore: Boolean,
)

object AdminActivityLogsCatalog {
    const val PageSize = 20
    const val MaxActivityLogResults = 260
}
