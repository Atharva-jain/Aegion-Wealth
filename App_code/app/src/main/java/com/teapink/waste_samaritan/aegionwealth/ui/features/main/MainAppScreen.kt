package com.teapink.waste_samaritan.aegionwealth.ui.features.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.teapink.waste_samaritan.aegionwealth.data.view_model.DatabaseViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.home.BottomNavBar
import com.teapink.waste_samaritan.aegionwealth.ui.navigation.nav_host.MainNavHost

@Composable
fun MainAppScreen(databaseViewModel: DatabaseViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }) { innerPadding ->
        MainNavHost(
            databaseViewModel = databaseViewModel,
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            innerPadding = innerPadding
        )
    }
}

