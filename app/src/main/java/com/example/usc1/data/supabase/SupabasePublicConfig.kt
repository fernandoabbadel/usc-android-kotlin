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

    val isConfigured: Boolean
        get() = url.isNotBlank() && anonKey.isNotBlank()

    fun requireConfigured(): SupabasePublicConfig {
        require(isConfigured) {
            "Configure SUPABASE_URL e SUPABASE_ANON_KEY em local.properties ou nas variáveis de ambiente."
        }
        require(url.startsWith("https://") && url.contains(".supabase.")) {
            "SUPABASE_URL inválida para o projeto Android."
        }
        return this
    }
}
