package com.teapink.waste_samaritan.aegionwealth.ui.features.history.display

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.RiskAnalytics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.PortfolioHistoryItem
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

// 1. Unified Data Class for the UI
private data class ExtractedPortfolioData(
    val investment: Double,
    val healthScore: Double,
    val verdict: String,
    val weights: Map<String, Double>,
    val risk: RiskAnalytics,
    val overrides: List<String>,
    val alerts: List<String>,
    val regimeStatus: String,
    val xray: Map<String, Double>,
    val advice: String,
    val assetClassData: Map<String, Double>?,
    val userRiskProfile: String,
    val timeHorizonYears: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioDetailContentScreen(
    portfolio: PortfolioHistoryItem,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 2. Extract Data based on the Sealed Class
    val data = remember(portfolio) {
        when (portfolio) {
            is PortfolioHistoryItem.Equity -> {
                val req = portfolio.record.request
                val res = portfolio.record.response
                ExtractedPortfolioData(
                    investment = req.initialInvestmentAmount,
                    healthScore = res.portfolioGrade.healthScore,
                    verdict = res.portfolioGrade.verdict,
                    weights = res.allocation.weights,
                    risk = res.riskAnalytics,
                    overrides = res.systemOverrides,
                    alerts = res.systemAlerts,
                    regimeStatus = res.marketRegime.status,
                    xray = res.portfolioXray,
                    advice = res.portfolioGrade.advice,
                    assetClassData = null,
                    userRiskProfile = req.userRiskProfile,
                    timeHorizonYears = req.timeHorizonYears
                )
            }

            is PortfolioHistoryItem.MultiAsset -> {
                val req = portfolio.record.request
                val res = portfolio.record.response
                ExtractedPortfolioData(
                    investment = req.initial_investment_amount,
                    healthScore = res.portfolio_grade.healthScore,
                    verdict = res.portfolio_grade.verdict,
                    weights = res.allocation.weights,
                    risk = res.risk_analytics,
                    overrides = res.system_overrides,
                    alerts = res.system_alerts,
                    regimeStatus = res.market_regime.status,
                    xray = res.portfolio_xray,
                    advice = res.portfolio_grade.advice,
                    assetClassData = res.asset_class_breakdown,
                    userRiskProfile = req.user_risk_profile,
                    timeHorizonYears = req.time_horizon_years
                )
            }
        }
    }

    // Process Allocations & Alerts
    val activeAllocations = remember(data.weights) {
        data.weights.filter { it.value > 0.0 }.toList().sortedByDescending { it.second }
    }
    val rejectedAssets = remember(data.weights) {
        data.weights.filter { it.value == 0.0 }.keys.toList().sorted()
    }
    val allAlerts = remember(data) { data.overrides + data.alerts }

    // 3. Staggered Animation State
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // 4. BOTTOM SHEET STATE SETUP
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    // --- Delete Confirmation Dialog ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Portfolio") },
            text = { Text("Are you sure you want to permanently delete this portfolio? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                        onBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (portfolio is PortfolioHistoryItem.Equity) "Equity Details" else "Multi-Asset Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete Portfolio",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, delayMillis = 800)) + slideInVertically(tween(500, delayMillis = 800)) { it / 2 }
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("Strategy Parameters") },
                    icon = { Icon(Icons.Rounded.Tune, contentDescription = "View Parameters") },
                    onClick = { showBottomSheet = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            AnimatedDashboardItem(visible = visible, delay = 0) {
                HeaderSection(
                    investment = data.investment,
                    healthScore = data.healthScore,
                    verdict = data.verdict,
                    timestamp = portfolio.timestamp
                )
            }

            AnimatedDashboardItem(visible = visible, delay = 100) {
                SystemIntelligenceSection(regimeStatus = data.regimeStatus, advice = data.advice)
            }

            if (data.assetClassData != null && data.assetClassData.isNotEmpty()) {
                AnimatedDashboardItem(visible = visible, delay = 200) {
                    AssetClassBreakdownCard(breakdown = data.assetClassData)
                }
            }

            AnimatedDashboardItem(visible = visible, delay = 300) {
                AllocationCardV2(allocations = activeAllocations, initialCapital = data.investment)
            }

            if (rejectedAssets.isNotEmpty()) {
                AnimatedDashboardItem(visible = visible, delay = 400) {
                    RejectedAssetsCard(rejectedAssets = rejectedAssets)
                }
            }

            if (data.xray.isNotEmpty()) {
                AnimatedDashboardItem(visible = visible, delay = 500) {
                    HorizontalBarChartCard(title = "Diversification X-Ray", icon = Icons.Rounded.AutoGraph, data = data.xray)
                }
            }

            AnimatedDashboardItem(visible = visible, delay = 600) {
                RiskEngineCard(riskAnalytics = data.risk)
            }

            if (allAlerts.isNotEmpty()) {
                AnimatedDashboardItem(visible = visible, delay = 700) {
                    SystemAlertsCard(alerts = allAlerts)
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Safe space for the FAB
        }
    }

    // 5. BOTTOM SHEET IMPLEMENTATION
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Strategy Parameters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Base inputs provided to the AI.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Target Risk Profile", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(data.userRiskProfile.ifBlank { "Unspecified" }.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Time Horizon", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (data.timeHorizonYears > 0) "${data.timeHorizonYears} Years" else "Not Set", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showBottomSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Close", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
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
        enter = fadeIn(tween(500, delayMillis = delay)) + slideInVertically(tween(500, delayMillis = delay, easing = FastOutSlowInEasing)) { it / 4 }
    ) {
        content()
    }
}

// ==========================================
// ALLOCATION DONUT & ROWS (With ₹ Amounts)
// ==========================================
@Composable
fun AllocationCardV2(allocations: List<Pair<String, Double>>, initialCapital: Double) {
    val chartColors = listOf(
        MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primaryContainer, Color(0xFF8E24AA), Color(0xFF00897B)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Capital Allocation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedDonutChartV2(weights = allocations, colors = chartColors)
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
fun AnimatedDonutChartV2(weights: List<Pair<String, Double>>, colors: List<Color>) {
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
    val isCash = ticker.contains("CASH", ignoreCase = true) || ticker.contains("LIQUID", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(if(isCash) Color(0xFF388E3C).copy(alpha=0.1f) else dotColor.copy(alpha = 0.1f))
                .border(1.5.dp, if(isCash) Color(0xFF388E3C).copy(alpha=0.4f) else dotColor.copy(alpha = 0.4f), CircleShape),
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

// ==========================================
// AI EXCLUSIONS (Rejected Assets)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RejectedAssetsCard(rejectedAssets: List<String>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
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
// MACRO ASSET CLASS BREAKDOWN
// ==========================================
@Composable
fun AssetClassBreakdownCard(breakdown: Map<String, Double>) {
    val activeClasses = breakdown.filter { it.value > 0.0 }.entries.sortedByDescending { it.value }
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary, Color(0xFFF57C00))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Macro Strategy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("High-level asset class distribution", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            var triggerAnimation by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { triggerAnimation = true }

            Row(modifier = Modifier.fillMaxWidth().height(16.dp).clip(CircleShape)) {
                activeClasses.forEachIndexed { index, entry ->
                    val animatedWeight by animateFloatAsState(
                        targetValue = if (triggerAnimation) entry.value.toFloat() / 100f else 0f,
                        animationSpec = tween(1200, easing = FastOutSlowInEasing),
                        label = "AssetAnim"
                    )
                    Box(modifier = Modifier.fillMaxHeight().weight(animatedWeight.coerceAtLeast(0.001f)).background(colors[index % colors.size]))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            activeClasses.forEachIndexed { index, (assetClass, percent) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(colors[index % colors.size]))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = assetClass.replace("_", " "), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(text = "$percent%", style = MaterialTheme.typography.titleMedium, color = colors[index % colors.size], fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// ======================================================================
// REFACTORED COMPONENT: Header Section
// ======================================================================
// ======================================================================
// REFACTORED COMPONENT: Header Section (Alignment Fixed)
// ======================================================================
@Composable
fun HeaderSection(investment: Double, healthScore: Double, verdict: String, timestamp: Long) {
    val dateString = remember(timestamp) {
        SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    val isRed = verdict.contains("RED", ignoreCase = true) || verdict.contains("SUBOPTIMAL", ignoreCase = true)
    val isYellow = verdict.contains("YELLOW", ignoreCase = true)
    val verdictColor = when {
        isRed -> MaterialTheme.colorScheme.error
        isYellow -> Color(0xFFFBC02D)
        else -> Color(0xFF388E3C)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Initial Investment", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("₹${String.format("%,.0f", investment)}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(20.dp))

            // --- THE FIX IS HERE ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top // Fixed: Aligns the labels at the top even if values wrap below
            ) {
                // Left Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("Health Score", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${healthScore.toInt()} / 100",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = verdictColor
                    )
                }

                // Right Column
                Column(
                    modifier = Modifier.weight(1f), // Added weight to constrain width
                    horizontalAlignment = Alignment.End
                ) {
                    Text("System Verdict", style = MaterialTheme.typography.labelMedium)
                    Text(
                        verdict.replace(Regex("[^A-Za-z ]"), "").trim(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = verdictColor,
                        textAlign = TextAlign.End // Ensures that when it wraps to two lines, it stays aligned to the right edge
                    )
                }
            }
            // -----------------------

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Generated on $dateString", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
// ======================================================================
// COMPONENT: System Intelligence
// ======================================================================
@Composable
fun SystemIntelligenceSection(regimeStatus: String, advice: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = advice, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, lineHeight = 20.sp)
            }
            if (regimeStatus.isNotBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = regimeStatus.replace(Regex("[^A-Za-z0-9: .%\\-]"), "").trim(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (regimeStatus.contains("BEAR", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ==========================================
// SYSTEM ALERTS CARD
// ==========================================
@Composable
fun SystemAlertsCard(alerts: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)).border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).padding(20.dp)
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

// ======================================================================
// COMPONENT: Horizontal Bar Chart
// ======================================================================
@Composable
fun HorizontalBarChartCard(title: String, icon: ImageVector, data: Map<String, Double>) {
    var animationPlayed by remember { mutableStateOf(false) }
    val progressMultiplier by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "BarChartAnim"
    )

    LaunchedEffect(Unit) { animationPlayed = true }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        //border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            data.entries.sortedByDescending { it.value }.forEach { (label, value) ->
                if (value > 0) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = label.replace("_", " "), style = MaterialTheme.typography.labelMedium)
                            Text(text = String.format("%.1f%%", value), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Box(modifier = Modifier.fillMaxWidth(fraction = (value.toFloat() / 100f) * progressMultiplier).fillMaxHeight().clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            }
        }
    }
}

// ======================================================================
// COMPONENT: Risk Engine Card (Replacing RiskAnalyticsCard)
// ======================================================================
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