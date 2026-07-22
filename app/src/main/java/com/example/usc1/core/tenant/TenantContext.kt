package com.example.usc1.core.tenant

data class TenantContext(
    val id: String,
    val slug: String,
    val name: String,
    val acronym: String = "",
    val course: String = "",
    val logoUrl: String? = null,
    val palette: TenantPalette = TenantPalette.Green,
    val membershipStatus: TenantMembershipStatus = TenantMembershipStatus.Unlinked,
)

enum class TenantMembershipStatus(val remoteValue: String) {
    Unlinked("unlinked"),
    Pending("pending"),
    Approved("approved"),
    Rejected("rejected"),
    Disabled("disabled");

    val isPending: Boolean
        get() = this == Pending

    companion object {
        fun fromRemote(value: String?): TenantMembershipStatus {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.remoteValue == normalized } ?: Unlinked
        }
    }
}

enum class TenantPalette(val remoteValue: String) {
    Green("green"),
    Yellow("yellow"),
    Red("red"),
    Blue("blue"),
    Orange("orange"),
    Purple("purple"),
    Pink("pink");
}
