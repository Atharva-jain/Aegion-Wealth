package com.teapink.waste_samaritan.aegionwealth.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.Gson
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.view_model.DatabaseViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.profile.ProfileViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.PortfolioResultScreen
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.PortfolioResultViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.theme.AegionWealthTheme
import com.teapink.waste_samaritan.aegionwealth.utils.theme.AppThemeMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.getValue

class StockPortfolioAllocationResultActivity : ComponentActivity() {

    // Koin injection
    private val viewModel: PortfolioResultViewModel by viewModel()
    private val databaseViewModel: DatabaseViewModel by viewModel()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Triggers the API instantly on creation
        val requestJson = Gson().toJson(optimizeRequest)
        viewModel.generatePortfolio(requestJson)

        enableEdgeToEdge()
        setContent {
            // We can grab the ViewModel right at the root to observe the theme
            val profileViewModel: ProfileViewModel = koinViewModel()


            val themeMode by profileViewModel.themeMode.collectAsStateWithLifecycle()

            // Determine actual dark/light boolean based on the enum and system state
            val isDarkTheme = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            AegionWealthTheme(darkTheme = isDarkTheme) {
                PortfolioResultScreen(
                    viewModel = viewModel,
                    databaseViewModel = databaseViewModel,
                    optimizeRequest = optimizeRequest,
                    onClose = { finish() })
            }
        }
    }

    companion object {
        private val TAG = StockPortfolioAllocationResultActivity::class.simpleName
        private var onBackPress: ((verified: Boolean) -> Unit)? = null
        lateinit var optimizeRequest: OptimizeRequest
        fun startActivity(
            context: Context,
            optimizeRequestResponse: OptimizeRequest,
            onBackPress: ((verified: Boolean) -> Unit)
        ) {
            this.onBackPress = onBackPress
            this.optimizeRequest = optimizeRequestResponse
            Intent(context, StockPortfolioAllocationResultActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }

}

