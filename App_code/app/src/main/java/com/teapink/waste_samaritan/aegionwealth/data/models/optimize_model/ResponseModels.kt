package com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model

import com.google.gson.annotations.SerializedName

data class OptimizeResponse(
    @SerializedName("system_overrides") val systemOverrides: List<String> = emptyList(),
    @SerializedName("market_regime") val marketRegime: MarketRegime = MarketRegime(),
    @SerializedName("asset_class_breakdown") val assetClassBreakdown: Map<String, Double> = emptyMap(), // Add this!
    @SerializedName("allocation") val allocation: Allocation = Allocation(),
    @SerializedName("risk_analytics") val riskAnalytics: RiskAnalytics = RiskAnalytics(),
    @SerializedName("portfolio_xray") val portfolioXray: Map<String, Double> = emptyMap(),
    @SerializedName("system_alerts") val systemAlerts: List<String> = emptyList(),
    @SerializedName("portfolio_grade") val portfolioGrade: PortfolioGrade = PortfolioGrade()
)

data class MarketRegime(
    @SerializedName("regime") val regime: String = "",
    @SerializedName("status") val status: String = ""
)

data class Allocation(
    @SerializedName("weights") val weights: Map<String, Double> = emptyMap(),
    @SerializedName("expected_annual_return_pct") val expectedAnnualReturnPct: Double = 0.0,
    @SerializedName("portfolio_volatility_pct") val portfolioVolatilityPct: Double = 0.0
)

data class RiskAnalytics(
    @SerializedName("cornish_fisher_var_21d") val cornishFisherVar21d: Double = 0.0,
    @SerializedName("expected_shortfall_cvar") val expectedShortfallCvar: Double = 0.0,
    @SerializedName("stress_test_loss_exposure") val stressTestLossExposure: Double = 0.0
)

data class PortfolioGrade(
    @SerializedName("health_score") val healthScore: Double = 0.0,
    @SerializedName("verdict") val verdict: String = "",
    @SerializedName("sharpe_ratio") val sharpeRatio: Double = 0.0,
    @SerializedName("advice") val advice: String = "",
    @SerializedName("score_breakdown") val scoreBreakdown: ScoreBreakdown = ScoreBreakdown()
)

data class ScoreBreakdown(
    @SerializedName("base_score") val baseScore: Double = 0.0,
    @SerializedName("risk_adjusted_points") val riskAdjustedPoints: Double = 0.0,
    @SerializedName("concentration_penalty") val concentrationPenalty: Double = 0.0,
    @SerializedName("sentiment_impact") val sentimentImpact: Double = 0.0
)