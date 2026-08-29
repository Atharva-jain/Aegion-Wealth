package com.teapink.waste_samaritan.aegionwealth.ui.features.authentication.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun signInWithFirebase(credential: AuthCredential) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                // Authenticate with Firebase using the Google Credential
                auth.signInWithCredential(credential).await()
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.localizedMessage ?: "Authentication failed")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}