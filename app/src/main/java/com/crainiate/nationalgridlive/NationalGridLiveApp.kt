package com.crainiate.nationalgridlive

import android.app.Application
import com.crainiate.nationalgridlive.data.repository.DataModule
import com.crainiate.nationalgridlive.data.settings.SettingsRepository

class NationalGridLiveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(this)
        DataModule.init(this)
    }
}
