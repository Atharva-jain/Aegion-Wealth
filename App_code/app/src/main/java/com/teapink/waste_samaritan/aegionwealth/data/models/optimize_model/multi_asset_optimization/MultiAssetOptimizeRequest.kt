package com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.multi_asset_optimization

import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.Allocation
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.MarketRegime
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioGrade
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.RiskAnalytics

data class MultiAssetOptimizeRequest(
    val initial_investment_amount: Double = 0.0,
    val user_risk_profile: String = "",
    val time_horizon_years: Int = 0,
    val stock_tickers: List<String> = emptyList(),
    val bond_tickers: List<String> = emptyList(),
)

data class MultiAssetResponse(
    val system_overrides: List<String> = emptyList(),
    val market_regime: MarketRegime = MarketRegime(),
    val asset_class_breakdown: Map<String, Double> = emptyMap(), // New field
    val allocation: Allocation = Allocation(),
    val risk_analytics: RiskAnalytics = RiskAnalytics(),
    val portfolio_xray: Map<String, Double> = emptyMap(),
    val system_alerts: List<String> = emptyList(),
    val portfolio_grade: PortfolioGrade = PortfolioGrade()
)


