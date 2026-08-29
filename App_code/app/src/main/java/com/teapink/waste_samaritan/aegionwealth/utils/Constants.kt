package com.teapink.waste_samaritan.aegionwealth.utils

import com.teapink.waste_samaritan.aegionwealth.data.models.optimize_model.HedgeAsset

import java.util.Calendar

object GreetingUtils {

    fun getTimeBasedGreeting(): String {
        // Use the legacy Calendar API which works on all Android versions
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when (currentHour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night" // Covers 21..23 and 0..4
        }
    }
}

object Constants {

    // collections
    const val USER_COLLECTION = "user"
    const val PORTFOLIO_COLLECTION = "portfolio"
    const val MULTI_PORTFOLIO_COLLECTION = "multi-asset-portfolio"

    // rest api link
    const val YAHOO_BASE_URL = "https://query2.finance.yahoo.com/"
    const val PORTFOLIO_ALLOCATION_BASE_URL = "Your Api Key"


    // screens
    const val HOME_SCREEN = "Home"
    const val HISTORY_SCREEN = "History"
    const val PROFILE_SCREEN = "Profile"

    // The translated list from your Python dictionary
    val availableHedgeAssets = listOf(
        HedgeAsset(
            "LIQUIDBEES.NS", "Cash & Equivalents", "Liquid ETF", "Nippon India ETF Liquid Bees"
        ),
        HedgeAsset(
            "LICNETFGSC.NS", "Government Bonds", "G-Sec ETF", "LIC MF Government Securities ETF"
        ),
        HedgeAsset("EBBETF0425.NS", "Corporate Bonds", "Bond ETF", "Bharat Bond ETF April 2025"),
        HedgeAsset("EBBETF0430.NS", "Corporate Bonds", "Bond ETF", "Bharat Bond ETF April 2030"),
        HedgeAsset("GOLDBEES.NS", "Commodities", "Gold ETF", "Nippon India ETF Gold Bees"),
        HedgeAsset("SETFGOLD.NS", "Commodities", "Gold ETF", "SBI ETF Gold"),
        HedgeAsset("KOTAKGOLD.NS", "Commodities", "Gold ETF", "Kotak Gold ETF"),
        HedgeAsset("MON100.NS", "International Equity", "US Tech", "Motilal Oswal NASDAQ 100 ETF")
    )

}