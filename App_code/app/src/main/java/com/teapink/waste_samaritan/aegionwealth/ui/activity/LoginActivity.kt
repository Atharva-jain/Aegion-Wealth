package com.teapink.waste_samaritan.aegionwealth.ui.activity


import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.teapink.waste_samaritan.aegionwealth.R
import com.teapink.waste_samaritan.aegionwealth.ui.features.authentication.login.LoginScreenUI
import com.teapink.waste_samaritan.aegionwealth.ui.features.authentication.login.LoginState
import com.teapink.waste_samaritan.aegionwealth.ui.features.authentication.login.LoginViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.profile.ProfileViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.theme.AegionWealthTheme
import com.teapink.waste_samaritan.aegionwealth.utils.theme.AppThemeMode
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel

class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inject ViewModel using Koin


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
                val state by viewModel.loginState.collectAsStateWithLifecycle()

                // Listen for success and close activity
                if (state is LoginState.Success) {
                    finish() // Returns user to the Profile/Home screen
                }

                LoginScreenUI(
                    state = state,
                    onGoogleSignInClick = { triggerGoogleSignIn() },
                    onBackClick = { finish() })
            }
        }
    }

    private fun triggerGoogleSignIn() {
        // Must use lifecycleScope because CredentialManager requires a Coroutine
        lifecycleScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@LoginActivity)

                // 1. Configure the Google ID Option
                // IMPORTANT: Use your Web Client ID from Firebase Console (NOT the Android Client ID)
                val googleIdOption =
                    GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .setAutoSelectEnabled(true).build()

                // 2. Build the request
                val request =
                    GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

                // 3. Launch the native Android bottom sheet for 1-tap sign in
                val result = credentialManager.getCredential(
                    context = this@LoginActivity, request = request
                )

                // 4. Handle the result
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    // 5. Convert to Firebase Auth Credential and pass to ViewModel
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    viewModel.signInWithFirebase(firebaseCredential)

                } else {
                    Log.e("Auth", "Unexpected type of credential")
                    // Handle error state via ViewModel
                }

            } catch (e: GetCredentialException) {
                Log.e("Auth", "GetCredentialException: ${e.message}")
                // Handle cancellation or API errors cleanly
                viewModel.resetState()
            }
        }
    }

}

