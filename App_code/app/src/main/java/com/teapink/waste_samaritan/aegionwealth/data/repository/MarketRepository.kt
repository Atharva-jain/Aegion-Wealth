package com.teapink.waste_samaritan.aegionwealth.data.repository

import android.net.http.HttpException
import android.os.Build
import androidx.annotation.RequiresExtension
import com.teapink.waste_samaritan.aegionwealth.data.api.YahooFinanceApi
import com.teapink.waste_samaritan.aegionwealth.data.models.Quote
import com.teapink.waste_samaritan.aegionwealth.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException


class MarketRepository  constructor(
    private val api: YahooFinanceApi
) {
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    fun searchMarketData(query: String?): Flow<Resource<List<Quote>>> = flow {

        val cleanQuery = query?.trim() ?: ""

        if (cleanQuery.isBlank()) {
            emit(Resource.Success(emptyList()))
            return@flow
        }

        emit(Resource.Loading())

        try {
            // FIX 1: Removed the `.NS` append trick.
            // We now send EXACTLY what the user typed to Yahoo so it can search names properly.
            val response = api.searchSymbol(query = cleanQuery)

            if (response.isSuccessful) {
                val rawQuotes = response.body()?.quotes ?: emptyList()

                val indianStocksOnly = rawQuotes.filter { quote ->
                    val isIndianExchange = quote.exchange == "NSI" || quote.exchange == "BSE"
                    val isEquity = quote.quoteType == "EQUITY"
                    val isValid = !quote.symbol.isNullOrBlank()

                    // FIX 2: This logic already perfectly handles Name searches!
                    // It checks if the user's query exists in the ticker OR the company name.
                    val matchesSearchTerm =
                        quote.symbol?.contains(cleanQuery, ignoreCase = true) == true ||
                                quote.shortName?.contains(cleanQuery, ignoreCase = true) == true ||
                                quote.longName?.contains(cleanQuery, ignoreCase = true) == true

                    isIndianExchange && isEquity && isValid && matchesSearchTerm
                }

                emit(Resource.Success(indianStocksOnly))
            } else {
                emit(Resource.Error("API Error: ${response.code()} - ${response.message()}"))
            }

        } catch (e: IOException) {
            emit(Resource.Error("Network error. Please check your internet connection."))
        } catch (e: HttpException) {
            emit(Resource.Error("Server error occurred."))
        } catch (e: Exception) {
            emit(Resource.Error("An unexpected error occurred."))
        }
    }.flowOn(Dispatchers.IO)
}