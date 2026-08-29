package com.teapink.waste_samaritan.aegionwealth.ui.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teapink.waste_samaritan.aegionwealth.data.repository.DatabaseRepository
import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.PortfolioHistoryItem
import com.teapink.waste_samaritan.aegionwealth.utils.user_manager.UserManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// 1. The State Data Class tailored perfectly for your Dual-Dashboard UI
data class HistoryUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val latestEquity: PortfolioHistoryItem.Equity? = null,
    val latestMultiAsset: PortfolioHistoryItem.MultiAsset? = null,
    val fullHistory: List<PortfolioHistoryItem> = emptyList()
)

class HistoryViewModel(
    private val repository: DatabaseRepository, userManager: UserManager
) : ViewModel() {

    private val _historyState = MutableStateFlow(HistoryUiState())
    val historyState: StateFlow<HistoryUiState> = _historyState.asStateFlow()

    private var historyObservationJob: Job? = null

    init {
        // Start observing based on the current user
        observeUserHistory(userManager.getUserProfile().uid)
    }

    // Call this explicitly when the user signs in or out to swap data streams
    fun observeUserHistory(userId: String) {
        // 1. Cancel any existing listener if the user changes
        historyObservationJob?.cancel()

        if (userId.isBlank()) {
            _historyState.value = HistoryUiState(isLoading = false, error = "User not logged in")
            return
        }

        _historyState.value = _historyState.value.copy(isLoading = true, error = null)

        historyObservationJob = viewModelScope.launch {
            // 2. Combine both live streams
            combine(
                repository.getEquityHistoryFlow(userId), repository.getMultiAssetHistoryFlow(userId)
            ) { equityResult, multiAssetResult ->

                val equityList = equityResult.getOrElse { emptyList() }
                val multiAssetList = multiAssetResult.getOrElse { emptyList() }

                val combinedHistory =
                    (equityList + multiAssetList).sortedByDescending { it.timestamp }

                // 3. Handle specific combined error states gracefully
                if (combinedHistory.isEmpty() && equityResult.isFailure && multiAssetResult.isFailure) {
                    HistoryUiState(
                        isLoading = false,
                        error = "Failed to sync live data. Please check your connection."
                    )
                } else {
                    HistoryUiState(
                        isLoading = false,
                        latestEquity = equityList.firstOrNull(),
                        latestMultiAsset = multiAssetList.firstOrNull(),
                        fullHistory = combinedHistory
                    )
                }
            }.catch { e ->
                    // Catch any catastrophic flow errors
                    _historyState.value =
                        HistoryUiState(isLoading = false, error = e.localizedMessage)
                }.collect { newState ->
                    // 4. Push live updates to UI
                    _historyState.value = newState
                }
        }
    }
}