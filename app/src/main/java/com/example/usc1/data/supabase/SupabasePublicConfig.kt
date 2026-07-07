package com.example.usc1.data.supabase

data class SupabasePublicConfig(
    val url: String,
    val anonKey: String,
) {
    init {
        require(!anonKey.contains("service_role", ignoreCase = true)) {
            "O app Android não pode receber service_role."
        }
    }
}
