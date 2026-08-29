package com.teapink.waste_samaritan.aegionwealth.data.models

import com.google.gson.annotations.SerializedName

data class YahooSearchResponse(
    @SerializedName("quotes") val quotes: List<Quote> = emptyList(),
    @SerializedName("news") val news: List<NewsItem> = emptyList()
)

data class Quote(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("shortname") val shortName: String?,
    @SerializedName("longname") val longName: String?,
    @SerializedName("exchange") val exchange: String,
    @SerializedName("quoteType") val quoteType: String,
    @SerializedName("sector") val sector: String?,
    @SerializedName("industry") val industry: String?
)

data class NewsItem(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("title") val title: String,
    @SerializedName("publisher") val publisher: String,
    @SerializedName("link") val link: String,
    @SerializedName("providerPublishTime") val publishTime: Long
)