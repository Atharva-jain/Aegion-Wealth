package com.teapink.waste_samaritan.aegionwealth.ui.features.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teapink.waste_samaritan.aegionwealth.data.models.Quote
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockSearchScreen(
    // Injecting ViewModel via Koin directly in Compose
    viewModel: MarketSearchViewModel = koinViewModel(), modifier: Modifier
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var active by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(if (active) 1f else 0.9f),
            query = query,
            onQueryChange = viewModel::onQueryChanged,
            onSearch = {
                keyboardController?.hide()
                active = false
            },
            active = active,
            onActiveChange = { active = it },
            placeholder = { Text("Search stocks, e.g., RELIANCE") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                AnimatedVisibility(
                    visible = active && query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()
                ) {
                    IconButton(onClick = { viewModel.onQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Search")
                    }
                }
            }) {
            // Search Bar Content (Dropdown Results)
            when (val currentState = state) {
                is Resource.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is Resource.Error -> {
                    Text(
                        text = currentState.message ?: "Unknown Error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }


                is Resource.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = currentState.data ?: emptyList(),
                            key = { it.symbol }) { quote ->
                            SearchItem(
                                quote = quote, onClick = {
                                    // Handle selection, hide keyboard, close search
                                    keyboardController?.hide()
                                    active = false
                                    viewModel.onQueryChanged(quote.symbol)
                                })
                        }
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
fun SearchItem(quote: Quote, onClick: () -> Unit) {
    // Visually distinguish local Indian exchanges if desired
    val isLocalMarket = quote.exchange == "NSI" || quote.exchange == "BSE"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quote.symbol,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isLocalMarket) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = quote.longName ?: quote.shortName ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = quote.exchange,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}