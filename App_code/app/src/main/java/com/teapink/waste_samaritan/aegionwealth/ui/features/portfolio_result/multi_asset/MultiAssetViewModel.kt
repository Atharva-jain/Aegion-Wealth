package com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.multi_asset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetOptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetResponse
import com.teapink.waste_samaritan.aegionwealth.data.repository.OptimizationRepository
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MultiAssetResultViewModel(
    private val repository: OptimizationRepository // Assuming injected via Koin or Hilt
) : ViewModel() {

    // --- UI State ---
    private val _uiState = MutableStateFlow<Resource<MultiAssetResponse>>(Resource.Idle())
    val uiState: StateFlow<Resource<MultiAssetResponse>> = _uiState.asStateFlow()

    // Store the initial capital so the UI can calculate absolute ₹ amounts in the charts
    var initialCapital: Double = 0.0
        private set

    // Store the request so we can retry easily if the network fails
    var initialRequest: MultiAssetOptimizeRequest? = null
        private set

    /**
     * Call this exactly once when the Result Screen opens.
     */
    fun fetchOptimization(request: MultiAssetOptimizeRequest) {
        // Prevent duplicate API calls if already loading or successfully loaded (e.g., on rotation)
        if (_uiState.value is Resource.Loading || _uiState.value is Resource.Success) return

        initialCapital = request.initial_investment_amount
        initialRequest = request

        executeOptimization(request)
    }

    /**
     * Called by the "Go Back / Retry" button on the Error State screen
     */
    fun retryOptimization() {
        initialRequest?.let { request ->
            _uiState.value = Resource.Idle()
            executeOptimization(request)
        } ?: run {
            _uiState.value = Resource.Error("Missing request data. Please go back and try again.")
        }
    }

    private fun executeOptimization(request: MultiAssetOptimizeRequest) {
        viewModelScope.launch {
            // Collect the Flow from the repository and push it directly to the UI state
            repository.optimizePortfolio(request).collect { result ->
                _uiState.value = result
            }
        }
    }
}