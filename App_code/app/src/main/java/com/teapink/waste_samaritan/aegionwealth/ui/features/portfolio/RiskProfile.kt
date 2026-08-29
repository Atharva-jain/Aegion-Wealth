package com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio

enum class RiskProfile(val title: String, val description: String) {
    CONSERVATIVE("Conservative", "Focus on capital preservation and minimal volatility."),
    MODERATE("Moderate", "Balanced approach aiming for steady growth and manageable risk."),
    AGGRESSIVE("Aggressive", "Focus on maximum alpha and high returns, accepting higher volatility.")
}

enum class TimeHorizonUnit { MONTHS, YEARS }