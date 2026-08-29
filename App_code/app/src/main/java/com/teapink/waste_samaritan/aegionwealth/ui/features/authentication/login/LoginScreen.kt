package com.teapink.waste_samaritan.aegionwealth.ui.features.authentication.login

import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp // Fixed: sp for typography kerning
import com.teapink.waste_samaritan.aegionwealth.R
import androidx.compose.ui.text.style.TextOverflow


@Composable
fun LoginScreenUI(
    state: LoginState,
    onGoogleSignInClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // --- 1. Immersive Mesh Gradient Background ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-50).dp)
                    .graphicsLayer(alpha = 0.3f)
                    .blur(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-100).dp, y = 100.dp)
                    .graphicsLayer(alpha = 0.3f)
                    .blur(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
            )
        }

        // --- 2. Main Content Container ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // --- 3. Glassmorphic Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                // Tighter padding to prevent text wrapping
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // --- 4. FIXED LOGO: Removed circular background, added soft corners ---
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Wealth App Logo",
                        modifier = Modifier
                            .height(80.dp)
                            // Adds a soft corner to your white asset so it looks intentional
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // --- 5. FIXED TYPOGRAPHY: Strict centering and sizing ---
                    Text(
                        text = "Good to see you",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary, // Using primary color to make it an accent
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Aegion Wealth",
                        // Swapped to headlineLarge to ensure it fits on one line better
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Sign in securely with Google to sync your portfolio and access your personalized wealth tools.",
                        style = MaterialTheme.typography.bodyMedium, // Swapped to bodyMedium to prevent overcrowding
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // --- 6. The Confident, Premium Button ---
                    PremiumGoogleSignInButton(
                        state = state,
                        onClick = onGoogleSignInClick
                    )
                }
            }

            Text(
                text = "Version 1.0.1 • Powered by Secure Sync",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        // --- 3. Floating Glassmorphic Back Button ---
        // Placed at the end of the Box so it draws on top of everything else
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding() // Ensures it respects the notch/status bar
                .padding(start = 16.dp, top = 16.dp)
                // Frosted glass effect for the button background
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Navigate Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

    }
}

@Composable
fun PremiumGoogleSignInButton(
    state: LoginState,
    onClick: () -> Unit
) {
    val isLoading = state is LoginState.Loading

    Crossfade(targetState = state, label = "Button State") { currentState ->
        when (currentState) {
            is LoginState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }
            is LoginState.Error -> {
                Text(
                    text = currentState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                // FIXED BUTTON: Strict row layout, no text wrapping
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !isLoading, onClick = onClick),
                    color = MaterialTheme.colorScheme.onSurface, // Dark background
                    contentColor = MaterialTheme.colorScheme.surface, // Light text
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center // Strictly centers the content block
                    ) {
                        Text(
                            text = "G",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, // CRITICAL: Prevents the 2-line wrap
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}