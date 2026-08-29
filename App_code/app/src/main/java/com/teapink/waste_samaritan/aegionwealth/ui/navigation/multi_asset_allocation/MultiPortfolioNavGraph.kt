package com.teapink.waste_samaritan.aegionwealth.ui.navigation.multi_asset_allocation

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetOptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.ui.activity.MultiPortfolioAllocatorResultActivity
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.CreatePortfolioViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.investment_strategy.InvestmentStrategyScreen
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.multi_asset_portfolio.HedgeSelectionScreen
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.search.StockSelectionScreen
//import com.teapink.waste_samaritan.aegionwealth.ui.features.stock_portfolio.user_allocation.AllocationScreen
import org.koin.compose.viewmodel.koinViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.navigation.stock_allocation.PortfolioRoute


@Composable
fun MultiPortfolioNavGraph(onFinish: () -> Unit) {
    val navController = rememberNavController()

    // Shared ViewModel tied to this graph's lifecycle
    val sharedViewModel: CreatePortfolioViewModel = koinViewModel()
    val context = LocalContext.current


    NavHost(
        navController = navController,
        startDestination = PortfolioRoute.Select.route,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }) {

        // --- STEP 1: Select Assets ---
        composable(PortfolioRoute.Select.route) {
            StockSelectionScreen(
                viewModel = sharedViewModel, onBackClick = onFinish, // Exits the entire flow
                onNextClick = {
                    navController.navigate(PortfolioRoute.Hedge.route)
                })
        }

        // Step 2: Hedges & Bonds (The new screen)
        composable(PortfolioRoute.Hedge.route) { // Create this route in your sealed class
            HedgeSelectionScreen(
                viewModel = sharedViewModel,
                onBackClick = { navController.popBackStack() },
                onNextClick = {
                    navController.navigate(PortfolioRoute.Strategy.route) // Go to Step 3
                })
        }

        // --- STEP 2: Investment Strategy ---
        composable(PortfolioRoute.Strategy.route) {
            InvestmentStrategyScreen(
                viewModel = sharedViewModel,
                onBackClick = { navController.popBackStack() },
                onGenerateClick = {
                    val request = MultiAssetOptimizeRequest(
                        initial_investment_amount = sharedViewModel.investmentCapital.value.toDoubleOrNull()
                            ?: 0.0,
                        user_risk_profile = sharedViewModel.riskProfile.value.name.lowercase(),
                        time_horizon_years = sharedViewModel.getNormalizedTimeHorizonInYears(),
                        stock_tickers = sharedViewModel.selectedStocks.value.map { it.symbol },
                        bond_tickers = sharedViewModel.selectedHedges.value.toList() // Passed securely to your new Python API
                    )

                    Log.d("Hedge", "Full request $request")

                    MultiPortfolioAllocatorResultActivity.startActivity(
                        context = context, optimizeRequestResponse = request
                    ) {
                        // Action to perform after the result activity launches
                        onFinish()
                    }
                })
        }
    }
}