package com.teapink.waste_samaritan.aegionwealth.ui.features.authentication

import com.teapink.waste_samaritan.aegionwealth.data.models.UserProfile

// Auth State
sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
}