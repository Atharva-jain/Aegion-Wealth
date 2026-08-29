package com.teapink.waste_samaritan.aegionwealth.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.Gson
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.view_model.DatabaseViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.PortfolioResultViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.profile.ProfileViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.navigation.display.DisplayPortfolioNavGraph
import com.teapink.waste_samaritan.aegionwealth.ui.theme.AegionWealthTheme
import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.PortfolioHistoryItem
import com.teapink.waste_samaritan.aegionwealth.utils.theme.AppThemeMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.getValue

class PortfolioViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Triggers the API instantly on creation

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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DisplayPortfolioNavGraph(innerPadding = innerPadding, onDelete = {})
                }
            }
        }
    }

    companion object {
        private val TAG = PortfolioViewerActivity::class.simpleName
        private var onBackPress: ((verified: Boolean) -> Unit)? = null
        private var onDeletePortfolio: ((verified: Boolean) -> Unit)? = null

        fun startActivity(
            context: Context,
            onBackPress: ((verified: Boolean) -> Unit),

        ) {
            this.onBackPress = onBackPress

            Intent(context, PortfolioViewerActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }


}

