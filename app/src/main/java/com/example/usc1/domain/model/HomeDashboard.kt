package com.example.usc1.domain.model

data class HomeDashboardBundle(
    val events: List<HomeDashboardEvent> = emptyList(),
    val products: List<HomeDashboardProduct> = emptyList(),
    val partners: List<HomeDashboardPartner> = emptyList(),
    val leagues: List<HomeDashboardLeague> = emptyList(),
    val posts: List<HomeDashboardPost> = emptyList(),
    val trainingImageUrls: List<String> = emptyList(),
    val capturedFreshmen: Int = 0,
    val totalMembers: Int = 0,
    val activeSalesEvent: HomeDashboardSalesEvent? = null,
    val profile: HomeDashboardProfile = HomeDashboardProfile(),
    val moduleVisibility: Map<String, Boolean> = emptyMap(),
) {
    fun isModuleVisible(key: String): Boolean = moduleVisibility[key] != false
}

data class HomeDashboardProfile(
    val avatarUrl: String? = null,
    val className: String = "",
    val planName: String = "",
    val level: Int = 1,
)

data class HomeDashboardSalesEvent(
    val id: String,
    val title: String,
    val menuTitle: String,
    val imageUrl: String? = null,
)

data class HomeDashboardEvent(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val imageUrl: String? = null,
    val type: String,
    val status: String,
    val likesCount: Int,
    val interestedCount: Int,
    val viewerHasLiked: Boolean,
    val viewerIsInterested: Boolean = false,
    val imagePositionY: Double? = null,
)

data class HomeDashboardProduct(
    val id: String,
    val name: String,
    val price: Double,
    val imageUrl: String? = null,
    val likesCount: Int,
    val viewerHasLiked: Boolean,
    val topClasses: List<HomeDashboardClassStat> = emptyList(),
)

data class HomeDashboardClassStat(
    val className: String,
    val count: Int,
)

data class HomeDashboardPartner(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
    val coverUrl: String? = null,
    val category: String = "",
    val tier: String = "standard",
)

data class HomeDashboardLeague(
    val id: String,
    val name: String,
    val acronym: String,
    val logoUrl: String? = null,
    val description: String = "",
    val weeklyTip: String = "",
)

data class HomeDashboardPost(
    val id: String,
    val userName: String,
    val avatarUrl: String? = null,
    val text: String,
    val createdAt: String = "",
    val likesCount: Int,
    val viewerHasLiked: Boolean,
)
