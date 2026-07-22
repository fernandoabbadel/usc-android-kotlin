package com.example.usc1.domain.model

data class MembershipCardConfig(
    val validity: String = DefaultValidity,
    val backgroundUrls: Map<String, String> = emptyMap(),
    val backgroundOpacity: Int = DefaultBackgroundOpacity,
) {
    companion object {
        const val DefaultValidity = "DEZ/2026"
        const val DefaultBackgroundOpacity = 60
    }
}
