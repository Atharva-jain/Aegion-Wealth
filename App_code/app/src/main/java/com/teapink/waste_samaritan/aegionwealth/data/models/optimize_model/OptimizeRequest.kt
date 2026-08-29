package com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model

import com.google.gson.annotations.SerializedName

// --- 1. REQUEST MODEL ---
data class OptimizeRequest(
    @SerializedName("initial_investment_amount") val initialInvestmentAmount: Double = 0.0,
    @SerializedName("user_risk_profile") val userRiskProfile: String = "",
    @SerializedName("time_horizon_years") val timeHorizonYears: Int = 0,
    @SerializedName("stock_tickers") val tickers: List<String> = emptyList()
)