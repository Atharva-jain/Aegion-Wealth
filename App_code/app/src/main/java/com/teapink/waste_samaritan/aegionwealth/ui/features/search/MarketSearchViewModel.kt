package com.teapink.waste_samaritan.aegionwealth.ui.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teapink.waste_samaritan.aegionwealth.data.repository.MarketRepository
import com.teapink.waste_samaritan.aegionwealth.utils.Resource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.teapink.waste_samaritan.aegionwealth.data.models.Quote
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MarketSearchViewModel(
    private val repository: MarketRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // The UI State is now directly derived from the search query flow
    val uiState: StateFlow<Resource<List<Quote>>> = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(Resource.Success(emptyList())) // Clear list if query is empty
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
}