package com.teapink.waste_samaritan.aegionwealth.data.repository

import android.util.Log
import com.teapink.waste_samaritan.aegionwealth.data.api.AegionWealthApi
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeResponse
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetOptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetResponse
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class OptimizationRepository(private val api: AegionWealthApi) {

    fun generateOptimizedPortfolio(request: OptimizeRequest): Flow<Resource<OptimizeResponse>> =
        flow {
            emit(Resource.Loading())
            try {
                val response = api.optimizePortfolio(request)
                if (response.isSuccessful && response.body() != null) {
                    emit(Resource.Success(response.body()!!))
                    Log.d("OptimizationRepository", "Optimization successful ${response.body()}")
                } else {
                    emit(Resource.Error("Optimization failed: Error ${response.code()}"))
                }
            } catch (e: Exception) {
                emit(Resource.Error("Network error: Please check your connection."))
                Log.e("OptimizationRepository", "Network error: ${e.message}", e)
            }
        }

    fun optimizePortfolio(request: MultiAssetOptimizeRequest): Flow<Resource<MultiAssetResponse>> =
        flow {
            emit(Resource.Loading())
            try {
                val response = api.optimizeMultiAsset(request)
                if (response.isSuccessful) {
                    response.body()?.let {
                        emit(Resource.Success(it))
                    } ?: emit(Resource.Error("Empty response body"))
                } else {
                    emit(Resource.Error("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                emit(Resource.Error(e.localizedMessage ?: "Network Error"))
            }
        }.flowOn(Dispatchers.IO)
}