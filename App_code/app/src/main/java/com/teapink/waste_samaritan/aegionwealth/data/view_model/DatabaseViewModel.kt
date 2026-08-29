package com.teapink.waste_samaritan.aegionwealth.data.view_model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teapink.waste_samaritan.aegionwealth.data.models.UserProfile
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeResponse
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioMultiRecord
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioRecord
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetOptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetResponse
import com.teapink.waste_samaritan.aegionwealth.data.repository.DatabaseRepository
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.PortfolioHistoryItem
import com.teapink.waste_samaritan.aegionwealth.utils.user_manager.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DatabaseViewModel(
    val databaseRepository: DatabaseRepository, private val userManager: UserManager
) : ViewModel() {

    // State for the save operation (Optional: useful for showing a "Saved" toast)
    private val _saveSyncState = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val saveSyncState: StateFlow<Resource<Unit>> = _saveSyncState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    fun setUser(user: UserProfile) {
        _currentUser.value = user
    }

    fun clearUser() {
        _currentUser.value = null
    }

    // Call this function when you successfully receive the response from your Python/Backend API
    fun syncToFirebase(request: OptimizeRequest, response: OptimizeResponse) {

        val activeUser = userManager.getUserProfile()
        Log.d("FirebaseLogged", "user: $activeUser")

        if (activeUser.uid == "") {
            Log.d("FirebaseLogged", "Must be logged in to save portfolios.")
            _saveSyncState.value = Resource.Error("Must be logged in to save portfolios.")
            return
        }

        viewModelScope.launch {
            _saveSyncState.value = Resource.Loading()

            val record = PortfolioRecord(
                request = request,
                response = response,
                timestamp = System.currentTimeMillis(),
                user = activeUser,
                userId = activeUser.uid
            )

            val result = databaseRepository.savePortfolioRecord(record)
            _saveSyncState.value = result
        }
    }

    fun syncMultiAssetToFirebase(request: MultiAssetOptimizeRequest, response: MultiAssetResponse) {

        val activeUser = userManager.getUserProfile()
        Log.d("FirebaseLogged", "user: $activeUser")

        if (activeUser.uid == "") {
            Log.d("FirebaseLogged", "Must be logged in to save portfolios.")
            _saveSyncState.value = Resource.Error("Must be logged in to save portfolios.")
            return
        }

        viewModelScope.launch {
            val record = PortfolioMultiRecord(
                userId = activeUser.uid, user = activeUser, timestamp = System.currentTimeMillis(),
                // Ensure your PortfolioRecord document fields match these new data classes
                request = request, response = response
            )
            databaseRepository.savePortfolioMultiRecord(record)
        }
    }

    fun saveUserRecord(user: UserProfile) {
        viewModelScope.launch {
            _saveSyncState.value = Resource.Loading()

            val result = databaseRepository.saveUserRecord(user)

            _saveSyncState.value = result

        }
    }

    fun deletePortfolioRecord(record: PortfolioRecord) {
        viewModelScope.launch {
            databaseRepository.deletePortfolioRecord(record)
        }
    }

    fun deletePortfolioMultiRecord(record: PortfolioMultiRecord) {
        viewModelScope.launch {
            databaseRepository.deletePortfolioMultiRecord(record)
        }
    }


}