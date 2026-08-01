package com.example.worldlensai.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Registration : Screen("registration")
    object Home : Screen("home")
    object Camera : Screen("camera")
    object Analysis : Screen("analysis")
    object Lesson : Screen("lesson")
    object Quiz : Screen("quiz")
    object Dashboard : Screen("dashboard")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
}