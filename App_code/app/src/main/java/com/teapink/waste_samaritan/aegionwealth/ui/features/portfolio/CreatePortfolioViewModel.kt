package com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teapink.waste_samaritan.aegionwealth.data.models.Quote
import com.teapink.waste_samaritan.aegionwealth.data.repository.MarketRepository
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*



class CreatePortfolioViewModel (
    private val repository: MarketRepository
) : ViewModel() {

    // --- Search & Selection State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStocks = MutableStateFlow<List<Quote>>(emptyList())
    val selectedStocks: StateFlow<List<Quote>> = _selectedStocks.asStateFlow()

    // --- Investment Strategy State ---
    private val _investmentCapital = MutableStateFlow("")
    val investmentCapital: StateFlow<String> = _investmentCapital.asStateFlow()

    private val _riskProfile = MutableStateFlow(RiskProfile.MODERATE) // Default to moderate
    val riskProfile: StateFlow<RiskProfile> = _riskProfile.asStateFlow()

    private val _timeHorizon = MutableStateFlow("")
    val timeHorizon: StateFlow<String> = _timeHorizon.asStateFlow()

    private val _timeHorizonUnit = MutableStateFlow(TimeHorizonUnit.YEARS)
    val timeHorizonUnit: StateFlow<TimeHorizonUnit> = _timeHorizonUnit.asStateFlow()

    // --- Search Logic ---
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<Resource<List<Quote>>> = _searchQuery
        .debounce(300L) // Wait 300ms after user stops typing
        .distinctUntilChanged() // Don't trigger if the query is exactly the same
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(Resource.Success(emptyList()))
            } else {
                repository.searchMarketData(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Success(emptyList())
        )

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun addStockToPortfolio(quote: Quote) {
        val current = _selectedStocks.value
        // Limit to 10 stocks maximum to prevent overloading the AI (Optional, but good practice)
        if (current.size < 10 && !current.any { it.symbol == quote.symbol }) {
            _selectedStocks.value = current + quote
        }
        _searchQuery.value = "" // Clear search bar for great UX
    }

    fun removeStockFromPortfolio(quote: Quote) {
        _selectedStocks.value = _selectedStocks.value.filter { it.symbol != quote.symbol }
    }

    // --- Form Validation ---
    val isStrategyValid: StateFlow<Boolean> = combine(
        _investmentCapital, _timeHorizon, _selectedStocks
    ) { capital, horizon, stocks ->
        val capValue = capital.toDoubleOrNull() ?: 0.0
        val horValue = horizon.toDoubleOrNull() ?: 0.0 // Changed to double to allow "1.5" years

        // Valid if capital > 0, time > 0, and they have picked at least 3 stocks
        capValue > 0 && horValue > 0 && stocks.size >= 3
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- Update Inputs Safely ---
    fun updateCapital(amount: String) {
        // UX FIX: Allow decimals for currency, but prevent multiple decimals (e.g., "10.50.2")
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*\$"))) {
            _investmentCapital.value = amount
        }
    }

    fun updateRiskProfile(profile: RiskProfile) {
        _riskProfile.value = profile
    }

    fun updateTimeHorizon(time: String) {
        // UX FIX: Allow decimals for time (e.g., "1.5" years). Limit total length to prevent crazy inputs.
        if (time.isEmpty() || (time.matches(Regex("^\\d*\\.?\\d*\$")) && time.length <= 5)) {
            _timeHorizon.value = time
        }
    }

    fun updateTimeHorizonUnit(unit: TimeHorizonUnit) {
        _timeHorizonUnit.value = unit
    }

    // --- Data Preparation Helpers ---

    /**
     * CRITICAL BUG FIX: Converts the UI input into normalized years for the API.
     * If UI is "6" and "MONTHS", this returns 0.5 (or 1 if you strictly need integers).
     */
    fun getNormalizedTimeHorizonInYears(): Int {
        val inputValue = _timeHorizon.value.toDoubleOrNull() ?: 1.0

        val yearsAsDouble = if (_timeHorizonUnit.value == TimeHorizonUnit.MONTHS) {
            inputValue / 12.0
        } else {
            inputValue
        }

        // If your API requires an Int, round up so 6 months = 1 year minimum.
        // If your API accepts Doubles, just return `yearsAsDouble` directly.
        return kotlin.math.max(1, kotlin.math.ceil(yearsAsDouble).toInt())
    }

    // Inside CreatePortfolioViewModel
    // this section includes multi asset code
    private val _selectedHedges = MutableStateFlow<Set<String>>(emptySet())
    val selectedHedges: StateFlow<Set<String>> = _selectedHedges.asStateFlow()

    fun toggleHedgeAsset(ticker: String) {
        val current = _selectedHedges.value.toMutableSet()
        if (current.contains(ticker)) {
            current.remove(ticker)
        } else {
            current.add(ticker)
        }
        _selectedHedges.value = current
    }

}