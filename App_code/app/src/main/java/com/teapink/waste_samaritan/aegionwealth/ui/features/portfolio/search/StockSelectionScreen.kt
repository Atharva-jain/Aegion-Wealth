package com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teapink.waste_samaritan.aegionwealth.data.models.Quote
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalFocusManager
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.CreatePortfolioViewModel
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StockSelectionScreen(
    viewModel: CreatePortfolioViewModel, // Assuming injected via Koin or Hilt
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchState by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedStocks by viewModel.selectedStocks.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) focusManager.clearFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // --- HEADER ---
        HeaderSection(
            selectedCount = selectedStocks.size,
            onBackClick = onBackClick,
            onNextClick = {
                focusManager.clearFocus()
                onNextClick()
            }
        )

        // --- SELECTED STOCKS TRAY & SEARCH ---
        Surface(
            color = MaterialTheme.colorScheme.background,
            shadowElevation = if (listState.canScrollBackward) 4.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
        ) {
            Column {
                AnimatedVisibility(
                    visible = selectedStocks.isNotEmpty(),
                    enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(250)) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Your Portfolio",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary, // Emphasize with Primary
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items = selectedStocks, key = { it.symbol }) { quote ->
                                SelectedStockChip(
                                    modifier = Modifier.animateItem(),
                                    quote = quote,
                                    onRemove = { viewModel.removeStockFromPortfolio(quote) }
                                )
                            }
                        }
                    }
                }

                StockSearchBar(
                    query = query,
                    onQueryChanged = viewModel::onQueryChanged,
                    isLoading = searchState is Resource.Loading && query.isNotEmpty()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // --- DYNAMIC CONTENT AREA ---
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                query.isBlank() -> {
                    CreatePortfolioEmptyState(
                        icon = Icons.Rounded.Search,
                        title = "Search Companies",
                        description = "Type ticker symbols (e.g., TCS, RELIANCE) to select assets for your diversified portfolio.",
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
                searchState is Resource.Error -> {
                    CreatePortfolioEmptyState(
                        icon = Icons.Rounded.WifiOff,
                        title = "Network Error",
                        description = searchState.message ?: "Please check your connection.",
                        iconTint = MaterialTheme.colorScheme.error
                    )
                }
                searchState is Resource.Success && searchState.data.isNullOrEmpty() -> {
                    CreatePortfolioEmptyState(
                        icon = Icons.Rounded.Block,
                        title = "No Assets Found",
                        description = "We couldn't find any assets matching '${query}'.",
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }
                else -> {
                    val results = searchState.data ?: emptyList()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = results, key = { it.symbol }) { quote ->
                            val isAlreadyAdded = selectedStocks.any { it.symbol == quote.symbol }
                            FlatStockCard(
                                modifier = Modifier.animateItem(),
                                quote = quote,
                                isAdded = isAlreadyAdded,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.addStockToPortfolio(quote)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// REFINED UI COMPONENTS
// ==========================================

@Composable
private fun HeaderSection(selectedCount: Int, onBackClick: () -> Unit, onNextClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant) // Pure M3 surface variant
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Navigate Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Select Assets",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        val hasEnoughStocks = selectedCount >= 3
        var isNextPressed by remember { mutableStateOf(false) }
        val nextScale by animateFloatAsState(if (isNextPressed) 0.94f else 1f, label = "NextScale")

        Button(
            onClick = onNextClick,
            enabled = hasEnoughStocks,
            modifier = Modifier.scale(nextScale),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            interactionSource = remember { MutableInteractionSource() }.also {
                isNextPressed = it.collectIsPressedAsState().value && hasEnoughStocks
            },
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            AnimatedContent(targetState = hasEnoughStocks, label = "ButtonTextAnim") { enough ->
                Text(
                    text = if (enough) "Next ($selectedCount)" else "Add ${3 - selectedCount} more",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StockSearchBar(query: String, onQueryChanged: (String) -> Unit, isLoading: Boolean) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        placeholder = {
            Text("Search by Ticker Symbol", color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            } else if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // Makes search bar distinct
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent // Clean look when not focused
        ),
        singleLine = true
    )
}

@Composable
fun FlatStockCard(
    modifier: Modifier = Modifier, quote: Quote, isAdded: Boolean, onClick: () -> Unit
) {
    // Pure M3 Semantic Colors without alpha manipulation
    val containerColor by animateColorAsState(
        targetValue = if (isAdded) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        label = "CardColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isAdded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
        label = "BorderColor"
    )
    val contentColor = if (isAdded) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isAdded, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Company Logo Placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isAdded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = quote.symbol.take(1),
                    fontWeight = FontWeight.Bold,
                    color = if (isAdded) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quote.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor
                )
                Text(
                    text = quote.longName ?: quote.shortName ?: "Unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAdded) contentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Checkmark / Add Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAdded) Icons.Rounded.Check else Icons.Rounded.Add,
                    contentDescription = null,
                    tint = if (isAdded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SelectedStockChip(modifier: Modifier = Modifier, quote: Quote, onRemove: () -> Unit) {
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onRemove),
        color = MaterialTheme.colorScheme.primaryContainer, // Stands out as selected
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = quote.symbol,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Rounded.Cancel,
                contentDescription = "Remove",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun CreatePortfolioEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = iconTint
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}