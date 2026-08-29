package com.teapink.waste_samaritan.aegionwealth

import android.app.Application
import com.teapink.waste_samaritan.aegionwealth.di.firebaseModule
import com.teapink.waste_samaritan.aegionwealth.di.networkModule
import com.teapink.waste_samaritan.aegionwealth.di.preferencesModule
import com.teapink.waste_samaritan.aegionwealth.di.repositoryModule
import com.teapink.waste_samaritan.aegionwealth.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class AegionWealthApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            // Pass the Android context
            androidContext(this@AegionWealthApplication)
            // Load your modules
            modules(
                networkModule, repositoryModule, preferencesModule, firebaseModule, viewModelModule
            )
        }
    }
}