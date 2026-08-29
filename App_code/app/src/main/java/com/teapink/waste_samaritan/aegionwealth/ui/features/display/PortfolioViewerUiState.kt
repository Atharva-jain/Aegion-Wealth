package com.teapink.waste_samaritan.aegionwealth.ui.features.display

import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.DisplayPortfolio

// --- State Contracts ---
enum class PortfolioTypeFilter(val displayName: String) {
    ALL("All"), EQUITY("Equity"), MULTI_ASSET("Multi-Asset")
}

data class PortfolioViewerUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val allPortfolios: List<DisplayPortfolio> = emptyList(), // The raw, unfiltered data
    val filteredList: List<DisplayPortfolio> = emptyList(),  // The data currently shown on screen
    val isDateFilterApplied: Boolean = false,
    val selectedDateMillis: Long? = null
)