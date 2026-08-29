package com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeResponse
import com.teapink.waste_samaritan.aegionwealth.data.repository.OptimizationRepository
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PortfolioResultViewModel(
    private val repository: OptimizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Resource<OptimizeResponse>>(Resource.Idle())
    val uiState: StateFlow<Resource<OptimizeResponse>> = _uiState.asStateFlow()

    // We store the initial capital to calculate INR amounts in the UI later
    var initialCapital: Double = 0.0
        private set

    fun generatePortfolio(requestJson: String?) {
        if (requestJson.isNullOrBlank()) {
            _uiState.value = Resource.Error("Invalid request data.")
            return
        }

        try {
            val request = Gson().fromJson(requestJson, OptimizeRequest::class.java)
            initialCapital = request.initialInvestmentAmount // Save for UI calculations

            repository.generateOptimizedPortfolio(request).onEach { result ->
                _uiState.value = result
            }.launchIn(viewModelScope)

        } catch (e: Exception) {
            _uiState.value = Resource.Error("Failed to parse request data.")
        }
    }
}