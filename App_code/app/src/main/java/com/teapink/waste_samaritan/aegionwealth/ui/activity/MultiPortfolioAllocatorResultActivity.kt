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
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetOptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.view_model.DatabaseViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.multi_asset.MultiAssetResultScreen
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.multi_asset.MultiAssetResultViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.PortfolioResultViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.profile.ProfileViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.theme.AegionWealthTheme
import com.teapink.waste_samaritan.aegionwealth.utils.theme.AppThemeMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.getValue

class MultiPortfolioAllocatorResultActivity : ComponentActivity() {

    private val viewModel: MultiAssetResultViewModel by viewModel()
    private val databaseViewModel: DatabaseViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Triggers the API instantly on creation
        val requestJson = Gson().toJson(optimizeRequest)
        viewModel.fetchOptimization(optimizeRequest)

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
                MultiAssetResultScreen(
                    viewModel = viewModel,
                    databaseViewModel = databaseViewModel,
                    optimizeRequest = optimizeRequest,
                    onClose = { finish() })
            }
        }
    }

    companion object {
        private val TAG = MultiPortfolioAllocatorResultActivity::class.simpleName
        private var onBackPress: ((verified: Boolean) -> Unit)? = null
        lateinit var optimizeRequest: MultiAssetOptimizeRequest
        fun startActivity(
            context: Context,
            optimizeRequestResponse: MultiAssetOptimizeRequest,
            onBackPress: ((verified: Boolean) -> Unit)
        ) {
            this.onBackPress = onBackPress
            this.optimizeRequest = optimizeRequestResponse
            Intent(context, MultiPortfolioAllocatorResultActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }

}
