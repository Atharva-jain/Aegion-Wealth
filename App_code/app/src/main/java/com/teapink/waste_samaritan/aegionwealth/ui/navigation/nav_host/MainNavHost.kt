package com.teapink.waste_samaritan.aegionwealth.ui.navigation.nav_host

import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.teapink.waste_samaritan.aegionwealth.data.view_model.DatabaseViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.activity.CreateMultiAssetPortfolioActivity
import com.teapink.waste_samaritan.aegionwealth.ui.activity.CreatePortfolioActivity
import com.teapink.waste_samaritan.aegionwealth.ui.activity.DisplayPortfolioActivity
import com.teapink.waste_samaritan.aegionwealth.ui.activity.PortfolioViewerActivity
import com.teapink.waste_samaritan.aegionwealth.ui.features.history.HistoryScreen
import com.teapink.waste_samaritan.aegionwealth.ui.features.history.HistoryViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.home.HomeScreen
import com.teapink.waste_samaritan.aegionwealth.ui.features.profile.ProfileScreen
import com.teapink.waste_samaritan.aegionwealth.ui.navigation.screen.Screen
import com.teapink.waste_samaritan.aegionwealth.ui.theme.onSurfaceDark
import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.PortfolioHistoryItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainNavHost(
    modifier: Modifier,
    navController: NavHostController,
    databaseViewModel: DatabaseViewModel,
    innerPadding: PaddingValues
) {

    val historyViewModel: HistoryViewModel = koinViewModel()
    val context = LocalContext.current

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screen.Home.route,
        // Smooth crossfade transitions between bottom nav tabs
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }) {

        composable(Screen.Home.route) {
            HomeScreen(onCreateStocksOnly = {
                context.startActivity(Intent(context, CreatePortfolioActivity::class.java))
            }, onCreateBalanced = {
                context.startActivity(
                    Intent(
                        context, CreateMultiAssetPortfolioActivity::class.java
                    )
                )
            }, onSearchPortfolio = {
                PortfolioViewerActivity.startActivity(
                    context, onBackPress = {
                        // This pops the entire Activity and goes back to whatever screen launched it
                        (context as? PortfolioViewerActivity)?.finish()
                    })
            })
        }

        composable(Screen.History.route) {
            HistoryScreen(viewModel = historyViewModel, onEquityPortfolioClick = { record ->
                DisplayPortfolioActivity.startActivity(context, record, onBackPress = {
                    (context as? DisplayPortfolioActivity)?.finish()
                }, onDeletePortfolio = {
                    databaseViewModel.deletePortfolioRecord(record.record)
                    (context as? DisplayPortfolioActivity)?.finish()
                })
            }, onMultiAssetPortfolioClick = { record ->
                DisplayPortfolioActivity.startActivity(context, record, onBackPress = {
                    (context as? DisplayPortfolioActivity)?.finish()
                }, onDeletePortfolio = {
                    databaseViewModel.deletePortfolioMultiRecord(record.record)
                    (context as? DisplayPortfolioActivity)?.finish()
                })
            })
        }

        composable(Screen.Profile.route) {
            ProfileScreen(databaseViewModel = databaseViewModel, modifier = modifier)
        }
    }


}

