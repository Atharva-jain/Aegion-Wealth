package com.teapink.waste_samaritan.aegionwealth.utils.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// 1. Extension property to ensure a single instance of DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class ThemePreferencesManager(private val dataStore: DataStore<Preferences>) {

    private companion object {
        val THEME_KEY = stringPreferencesKey("app_theme_mode")
    }

    // 2. Read the Flow and map the String back to our Enum
    val themeModeFlow: Flow<AppThemeMode> = dataStore.data
        .catch { exception ->
            // Handle IOExceptions cleanly
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: AppThemeMode.SYSTEM.name
            // Safely convert string back to enum, defaulting to SYSTEM
            runCatching { AppThemeMode.valueOf(themeName) }.getOrDefault(AppThemeMode.SYSTEM)
        }

    // 3. Write the Enum as a String to DataStore
    suspend fun saveThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.name
        }
    }
}