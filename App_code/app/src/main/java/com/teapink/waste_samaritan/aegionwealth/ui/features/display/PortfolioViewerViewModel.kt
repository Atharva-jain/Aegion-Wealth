package com.teapink.waste_samaritan.aegionwealth.ui.features.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teapink.waste_samaritan.aegionwealth.data.repository.DatabaseRepository
import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.DisplayPortfolio
import com.teapink.waste_samaritan.aegionwealth.utils.user_manager.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn


class PortfolioViewerViewModel (
    private val repository: DatabaseRepository,
    private val userManager: UserManager
) : ViewModel() {

    // --- 1. User Inputs (The "Intents") ---
    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow(PortfolioTypeFilter.ALL)
    private val _selectedDateMillis = MutableStateFlow<Long?>(null)

    // --- 2. The Reactive Data Pipeline ---
    // We combine the raw Firebase data with the user's search/filter criteria.
    // Whenever Firebase updates OR the user types/clicks, this block re-runs automatically.
    val uiState: StateFlow<PortfolioViewerUiState> = combine(
        repository.getEquityHistoryFlow(userManager.getUserProfile().uid),
        repository.getMultiAssetHistoryFlow(userManager.getUserProfile().uid),
        _searchQuery,
        _selectedType,
        _selectedDateMillis
    ) { equityResult, multiAssetResult, query, type, dateMillis ->

        // 1. Handle Loading/Errors
        if (equityResult.isFailure && multiAssetResult.isFailure) {
            return@combine PortfolioViewerUiState(
                isLoading = false,
                error = "Failed to load portfolios. Please check your connection."
            )
        }

        // 2. Map raw data into our unified sealed class
        val equities = equityResult.getOrDefault(emptyList()).map { DisplayPortfolio.Equity(it.record) }
        val multiAssets = multiAssetResult.getOrDefault(emptyList()).map { DisplayPortfolio.MultiAsset(it.record) }

        // Combine and sort by newest first
        val combinedRawList = (equities + multiAssets).sortedByDescending { it.timestamp }

        // 3. Apply Filters Reactively
        val processedList = combinedRawList.filter { portfolio ->

            // A. Type Filter
            val matchesType = when (type) {
                PortfolioTypeFilter.ALL -> true
                PortfolioTypeFilter.EQUITY -> portfolio is DisplayPortfolio.Equity
                PortfolioTypeFilter.MULTI_ASSET -> portfolio is DisplayPortfolio.MultiAsset
            }

            // B. Search Query Filter (Searches by ID, or checks the tickers inside the request)
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                val lowerQuery = query.lowercase()

                // Match ID
                val idMatch = portfolio.id.lowercase().contains(lowerQuery)

                // Match Tickers (Digging into the sealed class types)
                val tickerMatch = when (portfolio) {
                    is DisplayPortfolio.Equity -> portfolio.record.request.tickers.any { it.lowercase().contains(lowerQuery) }
                    is DisplayPortfolio.MultiAsset -> {
                        portfolio.record.request.stock_tickers.any { it.lowercase().contains(lowerQuery) } ||
                                portfolio.record.request.bond_tickers.any { it.lowercase().contains(lowerQuery) }
                    }
                }

                idMatch || tickerMatch
            }

            // C. Date Filter (If a date is selected, check if timestamps fall on the same day)
            val matchesDate = if (dateMillis == null) {
                true
            } else {
                isSameDay(portfolio.timestamp, dateMillis)
            }

            // Must match all active filters to be included in the list
            matchesType && matchesSearch && matchesDate
        }

        // 4. Emit the new State to the UI
        PortfolioViewerUiState(
            isLoading = false,
            allPortfolios = combinedRawList,
            filteredList = processedList,
            isDateFilterApplied = dateMillis != null,
            selectedDateMillis = dateMillis
        )
    }.stateIn(
        scope = viewModelScope,
        // Stops the pipeline 5 seconds after the user leaves the screen to save battery
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PortfolioViewerUiState(isLoading = true)
    )

    // --- 3. Public Functions for the UI to call ---

    fun filterData(query: String, typeString: String) {
        _searchQuery.value = query
        _selectedType.value = when (typeString) {
            "Equity" -> PortfolioTypeFilter.EQUITY
            "Multi-Asset" -> PortfolioTypeFilter.MULTI_ASSET
            else -> PortfolioTypeFilter.ALL
        }
    }

    // Mocking the Date Picker logic (You would tie this to a DatePickerDialog in Compose)
    fun setDateFilter(dateInMillis: Long?) {
        _selectedDateMillis.value = dateInMillis
    }

    fun showDatePicker() {
        // In a real app, this might trigger a UI event, but for now,
        // we'll just toggle it off if it's on, or you can implement a channel event to trigger the picker.
        if (_selectedDateMillis.value != null) {
            setDateFilter(null) // Clear the filter
        }
    }

    // Used by the Detail Screen to instantly grab the data without passing Parcelables
    fun getPortfolioById(id: String): DisplayPortfolio? {
        // We look in the `allPortfolios` list because they might have navigated to details,
        // and we want to ensure we find it even if a search filter was active.
        return uiState.value.allPortfolios.find { it.id == id }
    }

    // --- Helper Utilities ---
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        // Simple comparison: In production, use java.time.LocalDate or a Calendar instance
        // to properly account for TimeZones to prevent midnight-cutoff bugs.
        val day1 = timestamp1 / (1000 * 60 * 60 * 24)
        val day2 = timestamp2 / (1000 * 60 * 60 * 24)
        return day1 == day2
    }
}