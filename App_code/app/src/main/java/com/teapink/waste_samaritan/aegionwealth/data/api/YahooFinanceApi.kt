package com.teapink.waste_samaritan.aegionwealth.data.api

import com.teapink.waste_samaritan.aegionwealth.data.models.YahooSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface YahooFinanceApi {
    @GET("v1/finance/search")
    suspend fun searchSymbol(
        @Query("q") query: String,
        // BUMP THIS UP: Fetch 100 items so we have a deep enough pool to filter from
        @Query("quotesCount") quotesCount: Int = 100,
        @Query("newsCount") newsCount: Int = 0,
        @Query("region") region: String = "IN",
        @Query("lang") lang: String = "en-IN",
        @Query("enableFuzzyQuery") enableFuzzyQuery: Boolean = false,
        @Query("enableCb") enableCb: Boolean = true,
        @Query("enableNavLinks") enableNavLinks: Boolean = false
    ): Response<YahooSearchResponse>
}