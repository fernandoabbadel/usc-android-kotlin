package com.example.usc1.domain.model

import com.example.usc1.core.tenant.TenantContext
import com.example.usc1.core.tenant.TenantMembershipStatus
import com.example.usc1.core.tenant.TenantPalette

data class PublicTenant(
    val id: String,
    val slug: String,
    val name: String,
    val acronym: String = "",
    val institution: String = "",
    val course: String = "",
    val city: String = "",
    val logoUrl: String? = null,
    val palette: TenantPalette = TenantPalette.Green,
) {
    val subtitle: String
        get() = listOf(institution, course, city)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(" • ")
            .ifBlank { acronym.ifBlank { slug.uppercase() } }

    fun toGuestTenantContext(): TenantContext = TenantContext(
        id = id,
        slug = slug,
        name = name,
        acronym = acronym,
        course = course,
        logoUrl = logoUrl,
        palette = palette,
        membershipStatus = TenantMembershipStatus.Unlinked,
    )
}
