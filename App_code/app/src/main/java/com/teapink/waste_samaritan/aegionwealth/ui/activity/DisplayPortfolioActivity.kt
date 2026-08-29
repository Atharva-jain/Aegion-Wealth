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

import com.teapink.waste_samaritan.aegionwealth.ui.features.history.display.PortfolioDetailContentScreen
import com.teapink.waste_samaritan.aegionwealth.ui.features.profile.ProfileViewModel

import com.teapink.waste_samaritan.aegionwealth.ui.theme.AegionWealthTheme
import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.PortfolioHistoryItem
import com.teapink.waste_samaritan.aegionwealth.utils.theme.AppThemeMode
import org.koin.compose.viewmodel.koinViewModel

class DisplayPortfolioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val profileViewModel: ProfileViewModel = koinViewModel()


            val themeMode by profileViewModel.themeMode.collectAsStateWithLifecycle()

            // Determine actual dark/light boolean based on the enum and system state
            val isDarkTheme = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            AegionWealthTheme(darkTheme = isDarkTheme) {
                PortfolioDetailContentScreen(
                    portfolio = optimizeRequest,
                    onBack = { finish() },
                    onDelete = {
                        onDeletePortfolio?.invoke(true)
                    })
            }
        }
    }

    companion object {
        private val TAG = DisplayPortfolioActivity::class.simpleName
        private var onBackPress: ((verified: Boolean) -> Unit)? = null
        private var onDeletePortfolio: ((verified: Boolean) -> Unit)? = null
        lateinit var optimizeRequest: PortfolioHistoryItem
        fun startActivity(
            context: Context,
            optimizeRequestResponse: PortfolioHistoryItem,
            onBackPress: ((verified: Boolean) -> Unit),
            onDeletePortfolio: ((verified: Boolean) -> Unit)
        ) {
            this.onBackPress = onBackPress
            this.onDeletePortfolio = onDeletePortfolio
            this.optimizeRequest = optimizeRequestResponse
            Intent(context, DisplayPortfolioActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }

}

