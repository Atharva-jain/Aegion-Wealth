package com.teapink.waste_samaritan.aegionwealth.ui.features.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.ripple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.teapink.waste_samaritan.aegionwealth.utils.GreetingUtils.getTimeBasedGreeting
import com.teapink.waste_samaritan.aegionwealth.utils.user_manager.UserManager
import org.koin.compose.koinInject
import org.koin.java.KoinJavaComponent.inject


@Composable
fun HomeScreen(
    onCreateStocksOnly: () -> Unit, onCreateBalanced: () -> Unit, onSearchPortfolio: () -> Unit
) {

    val userManager: UserManager = koinInject()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp), // Slightly wider padding for a premium feel
        contentPadding = PaddingValues(top = 32.dp, bottom = 120.dp), // Bottom padding for Nav Bar
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Sleek Header Section ---
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "${getTimeBasedGreeting()}, ${userManager.getUserProfile().displayName}", // Personalized greeting
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Build Your Wealth",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp // Fixed: Changed from .dp to .sp
                )
            }
        }

        item {
            // search portfolio card
            SearchPortfolioCtaCard(
                onClick = onSearchPortfolio
            )
        }

        // --- 100% Equity Card (Aggressive/Growth Theme) ---
        item {

            PremiumPortfolioCard(
                title = "100% Equity Portfolio",
                description = "High-growth potential utilizing strictly stocks. Ideal for maximizing long-term market returns.",
                icon = Icons.Rounded.TrendingUp,
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    MaterialTheme.colorScheme.surface
                ),
                iconColor = MaterialTheme.colorScheme.primary,
                onClick = onCreateStocksOnly
            )
        }

        // --- Balanced Card (Stable/Diversified Theme) ---
        item {
            PremiumPortfolioCard(
                title = "Balanced Portfolio",
                description = "A diversified mix of stocks and bonds to manage risk while maintaining steady, compounding growth.",
                icon = Icons.Rounded.PieChart,
                gradientColors = listOf(
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    MaterialTheme.colorScheme.surface
                ),
                iconColor = MaterialTheme.colorScheme.tertiary,
                onClick = onCreateBalanced
            )
        }
    }
}

@Composable
fun PremiumPortfolioCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    iconColor: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    // Snappier spring-like animation for premium feel
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "CardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                while (true) {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = iconColor, bounded = true), // New optimized ripple
                onClick = onClick
            ), shape = RoundedCornerShape(24.dp), // Softer, more modern corners
        border = BorderStroke(
            width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ), elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp, // Flat aesthetic relies on the border and gradient
            pressedElevation = 0.dp
        ), colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        // Custom Gradient Background inside the card
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(colors = gradientColors))
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Texts
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 20.dp)
                ) {
                    Text(
                        text = title, style = MaterialTheme.typography.titleLarge, // Montserrat
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium, // Inter
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f // Increases readability
                    )
                }

                // Right Side: Elevated Icon Container
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface) // Solid surface over gradient
                        .padding(12.dp), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = iconColor
                    )
                }
            }
        }
    }
}

// Reusable Placeholder for History and Profile
@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SearchPortfolioCtaCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Premium spring-like scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "CardScale"
    )

    // Switched to Secondary Theme for a completely distinct color
    val gradientColors = listOf(
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    )
    val iconColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                while (true) {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = iconColor, bounded = true),
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp), // Tighter corners for the compact design
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        // Gradient Background
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(colors = gradientColors))
                .fillMaxWidth()
                .padding(16.dp) // Keeps the card short and compact
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- Left: Icon ---
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface) // Pops against the secondary gradient
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ManageSearch,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = iconColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // --- Middle: Text Content ---
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Find Your Portfolios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Search past equity & multi-asset strategies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, // Forces it to stay compact
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // --- Right: Subtle Arrow ---
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Go to Search",
                    tint = iconColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}