package com.teapink.waste_samaritan.aegionwealth.ui.features.display

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.DisplayPortfolio
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.items // CRITICAL: This fixes the Int/id/items errors
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchPortfolioScreen(
    viewModel: PortfolioViewerViewModel,
    onPortfolioClick: (String) -> Unit,
    onBack: () -> Unit // Added back navigation callback
) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") } // All, Equity, Multi-Asset

    // ADDED: State to control the visibility of the Date Picker Dialog
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Search Bar ---
        SearchBar(
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
                // FIXED: Changed parameter name to 'typeString' to match ViewModel
                viewModel.filterData(query = it, typeString = selectedType)
            },
            onSearch = { },
            active = false,
            onActiveChange = { },
            placeholder = { Text("Search by ID or Ticker...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Go Back")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {}

        // --- 2. Interactive Filters ---
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Equity", "Multi-Asset").forEach { type ->
                FilterChip(
                    selected = selectedType == type, onClick = {
                        selectedType = type
                        // FIXED: Changed parameter name to 'typeString'
                        viewModel.filterData(query = searchQuery, typeString = type)
                    }, label = { Text(type) }, leadingIcon = if (selectedType == type) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else null)
            }

            // Date Filter Innovation: A specialized chip
            // --- NEW: Fully Functional Date Filter Chip ---
            ElevatedFilterChip(
                selected = state.isDateFilterApplied,
                onClick = {
                    if (state.isDateFilterApplied) {
                        // If already applied, clicking it clears the filter
                        viewModel.setDateFilter(null)
                    } else {
                        // If not applied, clicking opens the picker
                        showDatePicker = true
                    }
                },
                label = {
                    // Show the actual formatted date if one is selected
                    val dateText = state.selectedDateMillis?.let {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "Any Date"
                    Text(dateText)
                },
                leadingIcon = { Icon(Icons.Rounded.DateRange, null) },
                trailingIcon = if (state.isDateFilterApplied) {
                    // Show a little 'X' icon if a date is selected so users know it clears it
                    {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear Date",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 3. Animated List ---
        LazyColumn(
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FIXED: The imported `items` function now correctly recognizes 'state.filteredList'
            items(items = state.filteredList, key = { it.id }) { portfolio ->
                PortfolioListItemCard(
                    portfolio = portfolio,
                    modifier = Modifier
                        .animateItem()
                        .clickable { onPortfolioClick(portfolio.id) })
            }
        }
    }

    // --- NEW: Material 3 Date Picker Dialog Implementation ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDateMillis ?: System.currentTimeMillis()
        )

        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(
                onClick = {
                    // Pass the selected time back to the ViewModel
                    viewModel.setDateFilter(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) {
                Text("Apply")
            }
        }, dismissButton = {
            TextButton(onClick = { showDatePicker = false }) {
                Text("Cancel")
            }
        }) {
            // The actual calendar UI
            DatePicker(state = datePickerState)
        }
    }

}

@Composable
fun PortfolioListItemCard(
    portfolio: DisplayPortfolio, modifier: Modifier = Modifier
) {
    // 1. Determine Type-Specific Styling
    val isEquity = portfolio is DisplayPortfolio.Equity
    val icon = if (isEquity) Icons.Rounded.ShowChart else Icons.Rounded.PieChart
    val typeColor =
        if (isEquity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val typeName = if (isEquity) "Equity" else "Multi-Asset"

    // 2. Format Date
    val dateString = remember(portfolio.timestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(portfolio.timestamp))
    }

    // 3. Extract a preview of the tickers
    val tickersSummary = remember(portfolio) {
        when (portfolio) {
            is DisplayPortfolio.Equity -> {
                val tickers = portfolio.record.request.tickers
                tickers.take(3).joinToString(", ") + if (tickers.size > 3) "..." else ""
            }

            is DisplayPortfolio.MultiAsset -> {
                val allTickers =
                    portfolio.record.request.stock_tickers + portfolio.record.request.bond_tickers
                allTickers.take(3).joinToString(", ") + if (allTickers.size > 3) "..." else ""
            }
        }
    }

    // 4. Dynamic Health Color
    val healthColor = when {
        portfolio.healthScore >= 80 -> Color(0xFF4CAF50) // Green for excellent
        portfolio.healthScore >= 50 -> Color(0xFFFF9800) // Orange for average
        else -> MaterialTheme.colorScheme.error          // Red for poor
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Icon Block ---
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = typeColor)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- Main Content Block ---
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "₹${String.format("%,.0f", portfolio.investment)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = typeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " • $dateString",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (tickersSummary.isNotBlank()) {
                    Text(
                        text = tickersSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // --- Health Score Block ---
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Score",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = portfolio.healthScore.toInt().toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = healthColor
                )
            }
        }
    }
}