package com.teapink.waste_samaritan.aegionwealth.utils.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

class ThemePreferences constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    // 1. Create a live state that Compose can observe
    private val _themeFlow = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val themeFlow: StateFlow<Boolean> = _themeFlow.asStateFlow()

    fun saveTheme(isDark: Boolean) {
        prefs.edit { putBoolean("is_dark_mode", isDark) }
        // 2. Update the live state instantly!
        _themeFlow.value = isDark
    }

    companion object {
        @Volatile
        private var INSTANCE: ThemePreferences? = null

        // 3. Ensure the whole app uses the EXACT same preferences object
        fun getInstance(context: Context): ThemePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}