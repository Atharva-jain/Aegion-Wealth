package com.teapink.waste_samaritan.aegionwealth.ui.navigation.stock_allocation

sealed class PortfolioRoute(val route: String) {
    object Select : PortfolioRoute("select")
    object Allocate : PortfolioRoute("allocate")
    object Strategy : PortfolioRoute("strategy")
    object Hedge : PortfolioRoute("hedge")
}