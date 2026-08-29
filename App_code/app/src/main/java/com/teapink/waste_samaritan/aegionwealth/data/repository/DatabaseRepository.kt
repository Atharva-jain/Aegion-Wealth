package com.teapink.waste_samaritan.aegionwealth.data.repository

import com.teapink.waste_samaritan.aegionwealth.data.models.UserProfile
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioMultiRecord
import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.PortfolioRecord
import com.teapink.waste_samaritan.aegionwealth.data.services.DatabaseServices
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import com.teapink.waste_samaritan.aegionwealth.utils.data_fetching.PortfolioHistoryItem
import kotlinx.coroutines.flow.Flow

class DatabaseRepository(val databaseServices: DatabaseServices) {

    suspend fun saveUserRecord(user: UserProfile): Resource<Unit> {
        return databaseServices.saveUserRecord(user)
    }

    suspend fun savePortfolioRecord(record: PortfolioRecord): Resource<Unit> {
        return databaseServices.savePortfolioRecord(record)
    }

    suspend fun savePortfolioMultiRecord(record: PortfolioMultiRecord): Resource<Unit> {
        return databaseServices.savePortfolioMultiRecord(record)
    }

    suspend fun deletePortfolioRecord(record: PortfolioRecord): Resource<Unit> {
        return databaseServices.deletePortfolioRecord(record)
    }

    suspend fun deletePortfolioMultiRecord(record: PortfolioMultiRecord): Resource<Unit> {
        return databaseServices.deletePortfolioMultiRecord(record)
    }


    suspend fun getUserPortfolioHistory(userId: String): Result<List<PortfolioHistoryItem>> {
        return databaseServices.getUserPortfolioHistory(userId)
    }

    suspend fun getEquityHistory(userId: String): Result<List<PortfolioHistoryItem.Equity>> {
        return databaseServices.getEquityHistory(userId)
    }

    suspend fun getMultiAssetHistory(userId: String): Result<List<PortfolioHistoryItem.MultiAsset>> {
        return databaseServices.getMultiAssetHistory(userId)
    }

    fun getEquityHistoryFlow(userId: String): Flow<Result<List<PortfolioHistoryItem.Equity>>> {
        return databaseServices.getEquityHistoryFlow(userId)
    }

    fun getMultiAssetHistoryFlow(userId: String): Flow<Result<List<PortfolioHistoryItem.MultiAsset>>> {
        return databaseServices.getMultiAssetHistoryFlow(userId)
    }

}