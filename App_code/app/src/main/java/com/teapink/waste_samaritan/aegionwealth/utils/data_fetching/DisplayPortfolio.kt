package com.teapink.waste_samaritan.aegionwealth.utils.data_fetching

import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioMultiRecord
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioRecord

// 1. Unified Wrapper for both Portfolio Types
sealed class DisplayPortfolio {
    abstract val id: String
    abstract val timestamp: Long
    abstract val investment: Double
    abstract val healthScore: Double

    data class Equity(val record: PortfolioRecord) : DisplayPortfolio() {
        override val id = record.documentId
        override val timestamp = record.timestamp
        override val investment = record.request.initialInvestmentAmount
        override val healthScore = record.response.portfolioGrade.healthScore
    }

    data class MultiAsset(val record: PortfolioMultiRecord) : DisplayPortfolio() {
        override val id = record.documentId
        override val timestamp = record.timestamp
        override val investment = record.request.initial_investment_amount
        override val healthScore = record.response.portfolio_grade.healthScore
    }
}