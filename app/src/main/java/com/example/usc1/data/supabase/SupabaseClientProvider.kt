package com.example.usc1.data.supabase

import android.content.Intent
import android.util.Log
import com.example.usc1.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface AuthDeepLinkEvent {
    data object SessionImported : AuthDeepLinkEvent
    data class Error(val userMessage: String) : AuthDeepLinkEvent
}

object SupabaseClientProvider {
    private const val Tag = "USCAuth"

    private val _authDeepLinkEvents = MutableSharedFlow<AuthDeepLinkEvent>(
        replay = 1,
        extraBufferCapacity = 1,
    )
    val authDeepLinkEvents: SharedFlow<AuthDeepLinkEvent> = _authDeepLinkEvents.asSharedFlow()

    val config: SupabasePublicConfig
        get() = SupabasePublicConfig(
            url = BuildConfig.SUPABASE_URL.trim(),
            anonKey = BuildConfig.SUPABASE_ANON_KEY.trim(),
        )

    val client: SupabaseClient by lazy {
        val publicConfig = config.requireConfigured()
        createSupabaseClient(
            supabaseUrl = publicConfig.url,
            supabaseKey = publicConfig.anonKey,
        ) {
            install(Auth) {
                scheme = AndroidAuthScheme
                host = AndroidAuthHost
                defaultRedirectUrl = AndroidAuthRedirectUrl
                flowType = FlowType.PKCE
                autoLoadFromStorage = true
                autoSaveToStorage = true
            }
            install(Postgrest) {
                propertyConversionMethod = PropertyConversionMethod.SERIAL_NAME
            }
        }
    }

    fun handleAuthDeeplink(intent: Intent?): Boolean {
        val data = intent?.data
        val isAuthDeepLink = data?.scheme == AndroidAuthScheme && data.host == AndroidAuthHost
        Log.d(Tag, "deep link recebido: ${if (isAuthDeepLink) "sim" else "não"}")

        if (intent == null || !config.isConfigured || !isAuthDeepLink) return false

        val hasPkceCode = data.getQueryParameter("code").isNullOrBlank().not()
        val hasImplicitSession = data.fragment?.contains("access_token") == true
        val hasOAuthError = data.getQueryParameter("error").isNullOrBlank().not()

        if (hasOAuthError) {
            Log.w(Tag, "sessão criada: não; Supabase retornou erro OAuth")
            _authDeepLinkEvents.tryEmit(
                AuthDeepLinkEvent.Error("O login Google retornou erro antes de criar a sessão."),
            )
            return true
        }

        if (!hasPkceCode && !hasImplicitSession) {
            Log.w(Tag, "sessão criada: não; deep link sem código OAuth")
            _authDeepLinkEvents.tryEmit(
                AuthDeepLinkEvent.Error("O app recebeu o deep link, mas ele não trouxe código de login válido."),
            )
            return true
        }

        client.handleDeeplinks(
            intent = intent,
            onSessionSuccess = {
                Log.d(Tag, "sessão criada: sim")
                _authDeepLinkEvents.tryEmit(AuthDeepLinkEvent.SessionImported)
            },
            onError = { error ->
                Log.w(Tag, "sessão criada: não; erro=${error::class.java.simpleName}")
                _authDeepLinkEvents.tryEmit(
                    AuthDeepLinkEvent.Error("O app recebeu o retorno do Google, mas não conseguiu criar a sessão."),
                )
            },
        )
        return true
    }

    const val AndroidAuthScheme = "usc1"
    const val AndroidAuthHost = "auth"
    const val AndroidAuthRedirectUrl = "usc1://auth"
}
