package com.teapink.waste_samaritan.aegionwealth.ui.features.history


import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.search.CreatePortfolioEmptyState
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.ErrorState
import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.PortfolioHistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioMultiRecord
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioRecord
import com.teapink.waste_samaritan.aegionwealth.utils.user_manager.UserManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onEquityPortfolioClick: (PortfolioHistoryItem.Equity) -> Unit,
    onMultiAssetPortfolioClick: (PortfolioHistoryItem.MultiAsset) -> Unit
) {

    // implement user manager
    val userManager: UserManager = koinInject()

    // 1. Lifecycle-aware state collection
    val state by viewModel.historyState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Fetch the newest user ID from your manager
                val currentUserId = userManager.getUserProfile().uid
                // Re-bind the Firebase flow to the correct user
                viewModel.observeUserHistory(currentUserId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 2. Swipeable Pager Setup
    val tabs = listOf("Equity", "Multi-Asset")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Custom Animated TabRow ---
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                // Smooth indicator animation matching swipe physics
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            }) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = pagerState.currentPage == index, onClick = {
                    // Smooth scroll to page when tab is clicked
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }, text = {
                    Text(
                        text = title,
                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium,
                        color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- State Transition Wrapper ---
        AnimatedContent(
            targetState = state, transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            }, label = "HistoryStateTransition"
        ) { currentState ->
            when {
                currentState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                currentState.error != null -> {
                    // Assuming you have an ErrorState composable
                    ErrorState(
                        message = currentState.error!!,
                        bottonText = "Refresh",
                        title = "No User Found"
                    ) {
                        // To retry, we re-trigger the observer
                        viewModel.observeUserHistory(userManager.getUserProfile().uid)
                    }
                }

                else -> {
                    // --- Swipeable Content Area ---
                    HorizontalPager(
                        state = pagerState, modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> EquityHistoryList(
                                latest = currentState.latestEquity,
                                history = currentState.fullHistory.filterIsInstance<PortfolioHistoryItem.Equity>(),
                                onClick = onEquityPortfolioClick
                            )

                            1 -> MultiAssetHistoryList(
                                latest = currentState.latestMultiAsset,
                                history = currentState.fullHistory.filterIsInstance<PortfolioHistoryItem.MultiAsset>(),
                                onClick = onMultiAssetPortfolioClick
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// LIST: EQUITY (With Item Animations)
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EquityHistoryList(
    latest: PortfolioHistoryItem.Equity?,
    history: List<PortfolioHistoryItem.Equity>,
    onClick: (PortfolioHistoryItem.Equity) -> Unit
) {
    if (history.isEmpty()) {
        EmptyHistoryState("No Equity History", "Generate a pure stock portfolio to see it here.")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 80.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (latest != null) {
            item {
                Text(
                    text = "LATEST EQUITY PORTFOLIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                EquityCard(item = latest, isLatest = true, onClick = { onClick(latest) })
            }
        }

        val olderHistory = history.filter { it.documentId != latest?.documentId }

        if (olderHistory.isNotEmpty()) {
            item {
                Text(
                    text = "PREVIOUS PORTFOLIOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            // NOTE: The key is critical for animations to work!
            items(items = olderHistory, key = { it.documentId }) { item ->
                Box(modifier = Modifier.animateItem()) {
                    EquityCard(item = item, isLatest = false, onClick = { onClick(item) })
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryState(title: String, description: String) {
    // Basic implementation for the empty state
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ... MultiAssetHistoryList and MultiAssetCard follow the exact same pattern as above.

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiAssetHistoryList(
    latest: PortfolioHistoryItem.MultiAsset?,
    history: List<PortfolioHistoryItem.MultiAsset>,
    onClick: (PortfolioHistoryItem.MultiAsset) -> Unit
) {
    if (history.isEmpty()) {
        CreatePortfolioEmptyState(
            icon = Icons.Rounded.PieChart,
            title = "No Multi-Asset History",
            description = "Diversify with bonds and gold to see your history here.",
            iconTint = MaterialTheme.colorScheme.primary
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 80.dp,top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
        /*
        * contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 80.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
        * */
    ) {
        if (latest != null) {
            item {
                Text(
                    text = "LATEST MULTI-ASSET PORTFOLIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                MultiAssetCard(item = latest, isLatest = true, onClick = { onClick(latest) })
            }
        }

        val olderHistory = history.filter { it.documentId != latest?.documentId }

        if (olderHistory.isNotEmpty()) {
            item {
                Text(
                    text = "PREVIOUS MULTI-ASSET PORTFOLIOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            items(items = olderHistory, key = { it.documentId }) { item ->
                MultiAssetCard(item = item, isLatest = false, onClick = { onClick(item) })
            }
        }
    }
}

@Composable
fun EquityCard(
    item: PortfolioHistoryItem.Equity, isLatest: Boolean, onClick: (PortfolioRecord) -> Unit
) {
    val dateString = remember(item.timestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.timestamp))
    }
    // Added safety fallbacks in case data hasn't fully synced
    val capital = item.record.request.initialInvestmentAmount ?: 0.0
    val score = item.record.response.portfolioGrade.healthScore ?: 0

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)) // Restricts ripple to the card boundaries
            .clickable(onClick = { onClick(item.record) }),   // Enables the interactive click
        shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(
            containerColor = if (isLatest) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest
        ), elevation = CardDefaults.elevatedCardElevation(if (isLatest) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Icon Background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Main Body (Capital & Date)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "₹${String.format("%,.0f", capital)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLatest) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generated on $dateString",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLatest) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3. Health Score
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Health",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLatest) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLatest) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun MultiAssetCard(
    item: PortfolioHistoryItem.MultiAsset,
    isLatest: Boolean,
    onClick: (PortfolioMultiRecord) -> Unit
) {
    val dateString = remember(item.timestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.timestamp))
    }
    // Match the exact variable names from your MultiAsset Optimize Request/Response
    val capital = item.record.request.initial_investment_amount ?: 0.0
    val score = item.record.response.portfolio_grade.healthScore ?: 0

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = { onClick(item.record) }),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isLatest) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.elevatedCardElevation(if (isLatest) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Icon Background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Main Body (Capital & Date)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "₹${String.format("%,.0f", capital)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLatest) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generated on $dateString",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLatest) MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3. Health Score
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Health",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLatest) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isLatest) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}