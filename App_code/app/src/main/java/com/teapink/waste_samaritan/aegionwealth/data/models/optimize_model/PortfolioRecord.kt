package com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model

import com.teapink.waste_samaritan.aegionwealth.data.models.UserProfile
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetOptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetResponse
import java.util.UUID

// This is the actual document that will be saved in the "portfolios" collection
data class PortfolioRecord(
    var documentId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val user: UserProfile = UserProfile(),
    val timestamp: Long = System.currentTimeMillis(),
    val request: OptimizeRequest = OptimizeRequest(), // Ensure your original model has defaults
    val response: OptimizeResponse = OptimizeResponse() // Ensure your original model has defaults
)

data class PortfolioMultiRecord(
    var documentId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val user: UserProfile = UserProfile(),
    val timestamp: Long = System.currentTimeMillis(),
    val request: MultiAssetOptimizeRequest = MultiAssetOptimizeRequest(), // Ensure your original model has defaults
    val response: MultiAssetResponse = MultiAssetResponse() // Ensure your original model has defaults
)

data class HedgeAsset(
    val ticker: String,
    val sector: String,
    val industry: String,
    val description: String // A user-friendly name
)