package com.teapink.waste_samaritan.aegionwealth.data.api

import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.OptimizeResponse
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetOptimizeRequest
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization.MultiAssetResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AegionWealthApi {
    @POST("optimize/stocks")
    suspend fun optimizePortfolio(
        @Body request: OptimizeRequest
    ): Response<OptimizeResponse>

    @POST("optimize/multi-asset")
    suspend fun optimizeMultiAsset(
        @Body request: MultiAssetOptimizeRequest
    ): Response<MultiAssetResponse>

}

// In your Koin network module:
// single {
//     Retrofit.Builder()
//         .baseUrl("https://aegion-api-864827383476.asia-south1.run.app/")
//         .addConverterFactory(GsonConverterFactory.create())
//         .build()
//         .create(AegionWealthApi::class.java)
// }