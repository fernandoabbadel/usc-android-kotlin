package com.example.usc1.navigation

sealed interface AppRoute {
    val route: String

    data object Home : AppRoute {
        override val route = "home"
    }
}
