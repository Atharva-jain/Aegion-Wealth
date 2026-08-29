package com.teapink.waste_samaritan.aegionwealth.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teapink.waste_samaritan.aegionwealth.ui.features.profile.ProfileViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.navigation.stock_allocation.PortfolioNavGraph
import com.teapink.waste_samaritan.aegionwealth.ui.theme.AegionWealthTheme
import com.teapink.waste_samaritan.aegionwealth.utils.theme.AppThemeMode
import org.koin.compose.viewmodel.koinViewModel

class CreatePortfolioActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                PortfolioNavGraph(onFinish = { finish() })
            }
        }
    }
}

