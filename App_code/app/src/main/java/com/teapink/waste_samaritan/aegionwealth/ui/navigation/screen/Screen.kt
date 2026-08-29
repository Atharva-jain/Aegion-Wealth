package com.teapink.waste_samaritan.aegionwealth.ui.navigation.screen

import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Person

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", androidx.compose.material.icons.Icons.Rounded.Home)
    object History : Screen("history", "History", androidx.compose.material.icons.Icons.Rounded.List)
    object Profile : Screen("profile", "Profile", androidx.compose.material.icons.Icons.Rounded.Person)
}