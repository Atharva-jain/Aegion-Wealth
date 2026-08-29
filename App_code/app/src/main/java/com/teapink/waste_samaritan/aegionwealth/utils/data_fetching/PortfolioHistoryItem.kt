package com.teapink.waste_samaritan.aegionwealth.utils.data_fetching

import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioRecord
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioMultiRecord

sealed class PortfolioHistoryItem {
    abstract val timestamp: Long
    abstract val documentId: String

    data class Equity(val record: PortfolioRecord) : PortfolioHistoryItem() {
        override val timestamp: Long = record.timestamp
        override val documentId: String = record.documentId
    }

    data class MultiAsset(val record: PortfolioMultiRecord) : PortfolioHistoryItem() {
        override val timestamp: Long = record.timestamp
        override val documentId: String = record.documentId
    }
}