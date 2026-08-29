package com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock


import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeResponse
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioGrade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.MarketRegime
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.RiskAnalytics
import kotlin.math.absoluteValue
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.view_model.DatabaseViewModel
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PortfolioResultScreen(
    viewModel: PortfolioResultViewModel,
    databaseViewModel: DatabaseViewModel,
    optimizeRequest: OptimizeRequest,
    onClose: () -> Unit
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
                label = "ScreenStateTransition",
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            ) { currentState ->
                when (currentState) {
                    is Resource.Idle, is Resource.Loading -> InnovativeLoadingState()
                    is Resource.Error -> ErrorState(
                        "Optimization Failed",
                        "Go Back",
                        message = currentState.message ?: "Unknown Error",
                        onRetry = onClose
                    )

                    is Resource.Success -> {
                        currentState.data?.let { response ->
                            LaunchedEffect(response) {
                                databaseViewModel.syncToFirebase(
                                    request = optimizeRequest, response = response
                                )
                            }
                            PortfolioDashboardV2(
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
fun AttractiveTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Aegion Analysis",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ErrorState(title: String, bottonText: String, message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(50.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)) {
            Text(bottonText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InnovativeLoadingState() {
    val infiniteTransition = rememberInfiniteTransition(label = "LoaderTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "DataRotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.25f, animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "CorePulse"
    )
    val connectionAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.7f, animationSpec = infiniteRepeatable(animation = tween(1500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "ConnectionAlpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            Box(
                modifier = Modifier.size(140.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    .background(brush = Brush.radialGradient(colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), Color.Transparent)), shape = CircleShape)
            )

            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val orbitRadius = size.width / 3.2f

                for (i in 0 until 6) {
                    val angleOffset = i * 60f
                    val angle = Math.toRadians((rotation + angleOffset).toDouble())
                    val x = center.x + orbitRadius * cos(angle).toFloat()
                    val y = center.y + orbitRadius * sin(angle).toFloat()
                    val color = if (i % 2 == 0) primaryColor else tertiaryColor

                    drawLine(color = color, start = center, end = Offset(x, y), strokeWidth = 1.5.dp.toPx(), alpha = connectionAlpha * 0.8f)
                    drawCircle(color = color, radius = if (i % 3 == 0) 7.dp.toPx() else 5.dp.toPx(), center = Offset(x, y), alpha = if (i % 2 == 0) connectionAlpha else 1f)
                }
            }
            Box(
                modifier = Modifier.size(60.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    .clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        var statusIndex by remember { mutableStateOf(0) }
        val statuses = listOf("Fetching real-time variables...", "Analyzing market regime...", "Structuring allocation...", "Running stress tests...", "Finalizing portfolio...")

        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(2300)
                statusIndex = (statusIndex + 1) % statuses.size
            }
        }
        AnimatedContent(
            targetState = statuses[statusIndex],
            transitionSpec = { fadeIn() + slideInVertically { it } togetherWith fadeOut() + slideOutVertically { -it } },
            label = "StatusText"
        ) { text ->
            Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}

// ==========================================
// THE DASHBOARD
// ==========================================
@Composable
fun PortfolioDashboardV2(response: OptimizeResponse, initialCapital: Double) {
    val scrollState = rememberScrollState()

    // 1. Data Separation
    val allAlerts = remember(response) { response.systemOverrides + response.systemAlerts }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        AnimatedDashboardItem(visible = visible, delay = 0) {
            HealthScoreCard(grade = response.portfolioGrade)
        }

        AnimatedDashboardItem(visible = visible, delay = 100) {
            MarketRegimeAndMetrics(
                regime = response.marketRegime,
                expectedReturn = response.allocation.expectedAnnualReturnPct,
                volatility = response.allocation.portfolioVolatilityPct,
                sharpeRatio = response.portfolioGrade.sharpeRatio
            )
        }

        if (response.assetClassBreakdown.isNotEmpty()) {
            AnimatedDashboardItem(visible = visible, delay = 200) {
                HorizontalBarChartCard(
                    title = "Asset Class Breakdown",
                    icon = Icons.Rounded.PieChart,
                    data = response.assetClassBreakdown
                )
            }
        }

        AnimatedDashboardItem(visible = visible, delay = 300) {
            AllocationCardV2(allocations = activeAllocations, initialCapital = initialCapital)
        }

        if (rejectedAssets.isNotEmpty()) {
            AnimatedDashboardItem(visible = visible, delay = 400) {
                RejectedAssetsCard(rejectedAssets = rejectedAssets)
            }
        }

        if (response.portfolioXray.isNotEmpty()) {
            AnimatedDashboardItem(visible = visible, delay = 500) {
                HorizontalBarChartCard(
                    title = "Sector Exposure",
                    icon = Icons.Rounded.AutoGraph,
                    data = response.portfolioXray
                )
            }
        }

        AnimatedDashboardItem(visible = visible, delay = 600) {
            RiskEngineCard(riskAnalytics = response.riskAnalytics)
        }

        if (allAlerts.isNotEmpty()) {
            AnimatedDashboardItem(visible = visible, delay = 700) {
                SystemAlertsCard(alerts = allAlerts)
            }
        }
    }
}

// Reusable Staggered Animation Wrapper
@Composable
fun AnimatedDashboardItem(visible: Boolean, delay: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500, delayMillis = delay)) + slideInVertically(tween(500, delayMillis = delay, easing = FastOutSlowInEasing)) { it / 4 }
    ) {
        content()
    }
}

// ==========================================
// NEW: AI EXCLUSIONS CARD (Rejected Stocks)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RejectedAssetsCard(rejectedAssets: List<String>) {
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
// NEW: REUSABLE HORIZONTAL BAR CHART
// (Used for Asset Class & XRay)
// ==========================================
@Composable
fun HorizontalBarChartCard(title: String, icon: ImageVector, data: Map<String, Double>) {
    var animationPlayed by remember { mutableStateOf(false) }
    val progressMultiplier by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "BarChartAnim"
    )

    LaunchedEffect(Unit) { animationPlayed = true }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(20.dp))

            data.entries.sortedByDescending { it.value }.forEach { (label, value) ->
                if (value > 0) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label.replace("_", " "), style = MaterialTheme.typography.labelMedium)
                            Text(String.format("%.1f%%", value), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((value.toFloat() / 100f) * progressMultiplier)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// EXISTING COMPONENTS (Alerts, Health, Regime, Allocation, Risk)
// ==========================================

@Composable
fun SystemAlertsCard(alerts: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("System Interventions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        alerts.forEach { alert ->
            Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.Top) {
                Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(alert.replace("🚨", "").replace("⚠️", "").trim(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun HealthScoreCard(grade: PortfolioGrade) {
    var isExpanded by remember { mutableStateOf(false) }
    val isRed = grade.verdict.contains("RED", ignoreCase = true) || grade.verdict.contains("SUBOPTIMAL", ignoreCase = true)
    val isYellow = grade.verdict.contains("YELLOW", ignoreCase = true)
    val verdictColor = when {
        isRed -> MaterialTheme.colorScheme.error
        isYellow -> Color(0xFFFBC02D)
        else -> Color(0xFF388E3C)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(grade.healthScore.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = verdictColor)
                Text("/ 100", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                Spacer(modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = verdictColor.copy(alpha = 0.15f), contentColor = verdictColor) {
                    Text(grade.verdict.replace(Regex("[^A-Za-z ]"), "").trim(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("AI Verdict Impact", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(grade.advice, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp, modifier = Modifier.padding(top = 4.dp))

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Score Calculation:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    grade.scoreBreakdown.let { breakdown ->
                        BreakdownRow("Base Score", breakdown.baseScore, isPositive = true)
                        BreakdownRow("Risk-Adjusted Bonus", breakdown.riskAdjustedPoints, isPositive = true)
                        BreakdownRow("Concentration Penalty", breakdown.concentrationPenalty, isPositive = false)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(if (isExpanded) "Hide Breakdown" else "See Breakdown", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: Double, isPositive: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = if (value > 0) "+$value" else "$value", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (isPositive) Color(0xFF388E3C) else MaterialTheme.colorScheme.error)
    }
}

@Composable
fun MarketRegimeAndMetrics(regime: MarketRegime, expectedReturn: Double, volatility: Double, sharpeRatio: Double) {
    val isBear = regime.regime.equals("BEAR", ignoreCase = true)
    val regimeColor = if (isBear) MaterialTheme.colorScheme.error else Color(0xFF388E3C)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(regimeColor.copy(alpha = 0.1f)).border(1.dp, regimeColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (isBear) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp, contentDescription = null, tint = regimeColor, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("${regime.regime} MARKET DETECTED", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = regimeColor)
                Text(regime.status.replace(Regex("[^A-Za-z0-9: .%\\-]"), "").trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricBoxV2(Modifier.weight(1f), "Exp. Return", "$expectedReturn%", Icons.Rounded.ShowChart, MaterialTheme.colorScheme.primary)
            MetricBoxV2(Modifier.weight(1f), "Volatility", "$volatility%", Icons.Rounded.QueryStats, MaterialTheme.colorScheme.tertiary)
            MetricBoxV2(Modifier.weight(1f), "Sharpe Ratio", sharpeRatio.toString(), Icons.Rounded.Balance, MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun MetricBoxV2(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun AllocationCardV2(allocations: List<Pair<String, Double>>, initialCapital: Double) {
    val chartColors = listOf(
        MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primaryContainer, Color(0xFF8E24AA), Color(0xFF00897B)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Capital Allocation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedDonutChart(weights = allocations, colors = chartColors)
            Spacer(modifier = Modifier.height(32.dp))

            allocations.forEachIndexed { index, (ticker, weight) ->
                val color = chartColors[index % chartColors.size]
                val amountInr = (weight / 100.0) * initialCapital
                DeploymentRowV2(ticker = ticker, weightPct = weight, amountInr = amountInr, dotColor = color)
                if (index < allocations.size - 1) Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun AnimatedDonutChart(weights: List<Pair<String, Double>>, colors: List<Color>) {
    val totalWeight = weights.sumOf { it.second }.toFloat()
    val sweepProgress = remember { Animatable(0f) }

    LaunchedEffect(weights) {
        sweepProgress.animateTo(targetValue = 1f, animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }

    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            weights.forEachIndexed { index, entry ->
                val sweepAngle = (entry.second.toFloat() / totalWeight) * 360f
                val color = colors[index % colors.size]
                drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle * sweepProgress.value, useCenter = false, style = Stroke(width = 36.dp.toPx(), cap = StrokeCap.Butt), size = Size(size.width, size.height))
                startAngle += sweepAngle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("100%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Allocated", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DeploymentRowV2(ticker: String, weightPct: Double, amountInr: Double, dotColor: Color) {
    val isCash = ticker.contains("CASH", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(if(isCash) Color(0xFF388E3C).copy(alpha=0.1f) else dotColor.copy(alpha = 0.1f)).border(1.5.dp, if(isCash) Color(0xFF388E3C).copy(alpha=0.4f) else dotColor.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCash) {
                Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF388E3C), modifier = Modifier.size(20.dp))
            } else {
                Text(ticker.take(1), fontWeight = FontWeight.ExtraBold, color = dotColor, style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(ticker.replace(".NS", "").replace("_", " "), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("${String.format("%.1f", weightPct)}% of Portfolio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("₹${String.format("%,.0f", amountInr)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text("Capital", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RiskEngineCard(riskAnalytics: RiskAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("What are my actual risks?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("Translated from complex quants to plain English.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            RiskItem(
                title = "Value at Risk (VaR)",
                description = "In normal conditions over 21 days, you are expected to lose no more than this amount.",
                value = "₹${String.format("%,.0f", riskAnalytics.cornishFisherVar21d.absoluteValue)}",
                icon = Icons.Rounded.Shield
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 16.dp))
            RiskItem(
                title = "Crash Exposure (CVaR)",
                description = "If a severe black-swan crash occurs, this is the average projected loss.",
                value = "₹${String.format("%,.0f", riskAnalytics.expectedShortfallCvar.absoluteValue)}",
                icon = Icons.Rounded.WarningAmber
            )
        }
    }
}

@Composable
fun RiskItem(title: String, description: String, value: String, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
    }
}

