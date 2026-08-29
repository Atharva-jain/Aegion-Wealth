package com.teapink.waste_samaritan.aegionwealth.ui.navigation.display

import androidx.activity.ComponentActivity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.teapink.waste_samaritan.aegionwealth.ui.features.display.PortfolioDetailScreen
import com.teapink.waste_samaritan.aegionwealth.ui.features.display.PortfolioViewerViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.display.SearchPortfolioScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DisplayPortfolioNavGraph(
    sharedViewModel: PortfolioViewerViewModel = koinViewModel(),
    innerPadding: PaddingValues,
    onDelete: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "search_screen",
        enterTransition = { fadeIn() + slideInHorizontally { it / 2 } },
        exitTransition = { fadeOut() + slideOutHorizontally { -it / 2 } },
        popEnterTransition = { fadeIn() + slideInHorizontally { -it / 2 } },
        popExitTransition = { fadeOut() + slideOutHorizontally { it / 2 } }) {
        composable("search_screen") {
            SearchPortfolioScreen(viewModel = sharedViewModel, onBack = {
                // This pops the entire Activity and goes back to whatever screen launched it
                (navController.context as? ComponentActivity)?.finish()
            }, onPortfolioClick = { portfolioId ->
                navController.navigate("detail_screen/$portfolioId")
            })
        }

        composable(
            route = "detail_screen/{portfolioId}",
            arguments = listOf(navArgument("portfolioId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("portfolioId") ?: ""
            PortfolioDetailScreen(
                portfolioId = id,
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() },
                onDelete = onDelete
            )
        }
    }
}