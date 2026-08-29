package com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.multi_asset

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetOptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetResponse
import com.teapink.waste_samaritan.aegionwealth.data.view_model.DatabaseViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.AllocationCardV2
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.AttractiveTopBar
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.ErrorState
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.HealthScoreCard
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.InnovativeLoadingState
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.MarketRegimeAndMetrics
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.RiskEngineCard
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.SystemAlertsCard
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.ui.unit.sp
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.HorizontalBarChartCard



@Composable
fun MultiAssetResultScreen(
    viewModel: MultiAssetResultViewModel,
    onClose: () -> Unit,
    databaseViewModel: DatabaseViewModel,
    optimizeRequest: MultiAssetOptimizeRequest
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { AttractiveTopBar(onClose) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(
                targetState = state,
                label = "ScreenTransition",
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            ) { currentState ->
                when (currentState) {
                    is Resource.Idle, is Resource.Loading -> InnovativeLoadingState()
                    is Resource.Error -> ErrorState(
                        "Optimization Failed", "Go Back",
                        currentState.message ?: "Optimization Failed", onClose
                    )

                    is Resource.Success -> {
                        currentState.data?.let { response ->

                            // Fire and forget: Sync to Firebase silently in the background
                            LaunchedEffect(response) {
                                Log.d("FirebaseLogged", "Syncing Multi-Asset to Firebase")
                                databaseViewModel.syncMultiAssetToFirebase(
                                    request = optimizeRequest, response = response
                                )
                            }

                            MultiAssetDashboard(
                                response = response, initialCapital = viewModel.initialCapital
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiAssetDashboard(response: MultiAssetResponse, initialCapital: Double) {
    // 1. Data Separation
    val allAlerts = remember(response) { response.system_overrides + response.system_alerts }

    val allWeights = response.allocation.weights
    val activeAllocations = remember(allWeights) {
        allWeights.filter { it.value > 0.0 }.toList().sortedByDescending { it.second }
    }
    val rejectedAssets = remember(allWeights) {
        allWeights.filter { it.value == 0.0 }.keys.toList().sorted()
    }

    // 2. Staggered Animation State
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Smooth Native Scrolling Dashboard
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        AnimatedDashboardItem(visible = visible, delay = 0) {
            HealthScoreCard(grade = response.portfolio_grade)
        }

        AnimatedDashboardItem(visible = visible, delay = 100) {
            MarketRegimeAndMetrics(
                regime = response.market_regime,
                expectedReturn = response.allocation.expectedAnnualReturnPct,
                volatility = response.allocation.portfolioVolatilityPct,
                sharpeRatio = response.portfolio_grade.sharpeRatio
            )
        }

        if (response.asset_class_breakdown.isNotEmpty()) {
            AnimatedDashboardItem(visible = visible, delay = 200) {
                AssetClassBreakdownCard(breakdown = response.asset_class_breakdown)
            }
        }

        AnimatedDashboardItem(visible = visible, delay = 300) {
            AllocationCardV2(
                allocations = activeAllocations, initialCapital = initialCapital
            )
        }

        if (rejectedAssets.isNotEmpty()) {
            AnimatedDashboardItem(visible = visible, delay = 400) {
                MultiAssetRejectedAssetsCard(rejectedAssets = rejectedAssets)
            }
        }

        if (response.portfolio_xray.isNotEmpty()) {
            AnimatedDashboardItem(visible = visible, delay = 500) {
                HorizontalBarChartCard(
                    title = "Diversification X-Ray",
                    icon = Icons.Rounded.AutoGraph,
                    data = response.portfolio_xray
                )
            }
        }

        AnimatedDashboardItem(visible = visible, delay = 600) {
            RiskEngineCard(riskAnalytics = response.risk_analytics)
        }

        if (allAlerts.isNotEmpty()) {
            AnimatedDashboardItem(visible = visible, delay = 700) {
                SystemAlertsCard(alerts = allAlerts)
            }
        }
    }
}

// ==========================================
// STAGGERED ANIMATION WRAPPER
// ==========================================
@Composable
fun AnimatedDashboardItem(visible: Boolean, delay: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500, delayMillis = delay)) + slideInVertically(
            tween(500, delayMillis = delay, easing = FastOutSlowInEasing)
        ) { it / 4 }
    ) {
        content()
    }
}

// ==========================================
// COMPONENT: AI EXCLUSIONS (Rejected Assets)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiAssetRejectedAssetsCard(rejectedAssets: List<String>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("AI Exclusions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${rejectedAssets.size} assets rejected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "The optimization engine evaluated these requested assets but assigned them a 0% weight because they failed to improve the portfolio's risk-adjusted return under current market conditions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rejectedAssets.forEach { ticker ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(ticker.replace(".NS", ""), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// COMPONENT: Macro Asset Class Breakdown
// ==========================================
@Composable
fun AssetClassBreakdownCard(breakdown: Map<String, Double>) {
    // Filter out 0% asset classes and sort descending
    val activeClasses = breakdown.filter { it.value > 0.0 }.entries.sortedByDescending { it.value }

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFFF57C00) // Orange for commodities/gold if present
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Macro Strategy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "High-level asset class distribution",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Animated Stacked Progress Bar
            var triggerAnimation by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { triggerAnimation = true }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(CircleShape)
            ) {
                activeClasses.forEachIndexed { index, entry ->
                    val animatedWeight by animateFloatAsState(
                        targetValue = if (triggerAnimation) entry.value.toFloat() / 100f else 0f,
                        animationSpec = tween(1200, easing = FastOutSlowInEasing),
                        label = "AssetAnim"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(animatedWeight.coerceAtLeast(0.001f)) // Prevents crash if 0
                            .background(colors[index % colors.size])
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Legend
            activeClasses.forEachIndexed { index, (assetClass, percent) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(colors[index % colors.size])
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = assetClass.replace("_", " "), // Clean up "Fixed_Income_Bonds"
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors[index % colors.size],
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}