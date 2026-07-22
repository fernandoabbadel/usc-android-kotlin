package com.example.usc1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.usc1.data.supabase.SupabaseClientProvider
import com.example.usc1.navigation.UscNavGraph
import com.example.usc1.ui.theme.UscTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseClientProvider.handleAuthDeeplink(intent)
        enableEdgeToEdge()
        setContent {
            UscTheme {
                UscNavGraph()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SupabaseClientProvider.handleAuthDeeplink(intent)
    }
}
