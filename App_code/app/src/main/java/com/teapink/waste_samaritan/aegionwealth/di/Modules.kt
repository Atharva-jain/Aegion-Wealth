package com.teapink.waste_samaritan.aegionwealth.di

import com.google.firebase.firestore.FirebaseFirestore
import com.teapink.waste_samaritan.aegionwealth.data.api.AegionWealthApi
import com.teapink.waste_samaritan.aegionwealth.data.api.YahooFinanceApi
import com.teapink.waste_samaritan.aegionwealth.data.repository.DatabaseRepository
import com.teapink.waste_samaritan.aegionwealth.data.repository.MarketRepository
import com.teapink.waste_samaritan.aegionwealth.data.repository.OptimizationRepository
import com.teapink.waste_samaritan.aegionwealth.data.services.DatabaseServices
import com.teapink.waste_samaritan.aegionwealth.data.view_model.DatabaseViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.authentication.login.LoginViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.display.PortfolioViewerViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.history.HistoryViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.profile.ProfileViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.stock.PortfolioResultViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.search.MarketSearchViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio.CreatePortfolioViewModel
import com.teapink.waste_samaritan.aegionwealth.ui.features.portfolio_result.multi_asset.MultiAssetResultViewModel
import com.teapink.waste_samaritan.aegionwealth.utils.Constants.PORTFOLIO_ALLOCATION_BASE_URL
import com.teapink.waste_samaritan.aegionwealth.utils.Constants.YAHOO_BASE_URL
import com.teapink.waste_samaritan.aegionwealth.utils.theme.ThemePreferencesManager
import com.teapink.waste_samaritan.aegionwealth.utils.theme.dataStore
import com.teapink.waste_samaritan.aegionwealth.utils.user_manager.UserManager
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {
    single {
        Retrofit.Builder().baseUrl(YAHOO_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()
    }

    single { get<Retrofit>().create(YahooFinanceApi::class.java) }

    single {
        OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
    }

    // We build this Retrofit instance inline so it doesn't conflict with Yahoo's Base URL
    single<AegionWealthApi> {
        Retrofit.Builder().baseUrl(PORTFOLIO_ALLOCATION_BASE_URL).client(get())
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(AegionWealthApi::class.java)
    }
}

val repositoryModule = module {
    single { MarketRepository(get()) }
    single { OptimizationRepository(get()) }
}

val viewModelModule = module {
    viewModel { MarketSearchViewModel(get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { LoginViewModel() }
    viewModel { CreatePortfolioViewModel(get()) }
    viewModel { PortfolioResultViewModel(get()) }
    viewModel { DatabaseViewModel(get(), get()) }
    viewModel { MultiAssetResultViewModel(get()) }
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { PortfolioViewerViewModel(get(), get()) }
}

val preferencesModule = module {
    // Provide the DataStore instance using the Android context
    single { androidContext().dataStore }

    // Provide our Manager
    single { ThemePreferencesManager(get()) }

    single { UserManager(get()) }

}

val firebaseModule = module {
    // Provide Firestore instance
    single { FirebaseFirestore.getInstance() }
    // Provide Repository
    single<DatabaseServices> { DatabaseServices(get()) }
    single { DatabaseRepository(get()) }
}
