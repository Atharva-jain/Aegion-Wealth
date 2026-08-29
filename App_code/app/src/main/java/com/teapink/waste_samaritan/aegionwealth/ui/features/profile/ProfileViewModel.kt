package com.teapink.waste_samaritan.aegionwealth.ui.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.teapink.waste_samaritan.aegionwealth.data.models.UserProfile
import com.teapink.waste_samaritan.aegionwealth.ui.features.authentication.AuthState
import com.teapink.waste_samaritan.aegionwealth.utils.theme.AppThemeMode
import com.teapink.waste_samaritan.aegionwealth.utils.theme.ThemePreferences
import com.teapink.waste_samaritan.aegionwealth.utils.theme.ThemePreferencesManager
import com.teapink.waste_samaritan.aegionwealth.utils.user_manager.UserManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val themePreferencesManager: ThemePreferencesManager,
    private val userManager: UserManager
) : ViewModel() {
    // In a real app, you would inject Firebase Auth and DataStore here.

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = themePreferencesManager.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppThemeMode.SYSTEM // Default while loading
    )


    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            themePreferencesManager.saveThemeMode(mode)
        }

    }

    // Inside your ViewModel
    fun onUserAuthenticated(user: UserProfile) {
        userManager.saveUserProfile(user)
    }


    // 2. Real-Time Firebase Auth State
    val authState: StateFlow<AuthState> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                // User is logged in! Map Firebase data to our model
                trySend(
                    AuthState.Authenticated(
                        user = UserProfile(
                            uid = currentUser.uid,
                            displayName = currentUser.displayName ?: "Investor",
                            email = currentUser.email ?: "No email provided",
                            photoUrl = currentUser.photoUrl.toString()
                        )
                    )
                )
            } else {
                // User is logged out
                trySend(AuthState.Unauthenticated)
            }
        }

        // Attach listener
        auth.addAuthStateListener(authStateListener)

        // Clean up when the flow stops being collected
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthState.Loading // Start loading while Firebase checks session
    )

    fun logout() {
        userManager.clearUserData()
        auth.signOut() // This will automatically trigger the authStateListener above
    }

}