package com.example.testaudio

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.testaudio.repositories.AppPreferencesRepository
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

private const val APP_PREFERENCES_NAME = "app_preferences"
private val Context.dataStore by preferencesDataStore(name = APP_PREFERENCES_NAME)
class TestAudioApplication: Application() {
    lateinit var appPreferencesRepository: AppPreferencesRepository

    companion object {
        init {
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())
        }
    }

    override fun onCreate() {
        super.onCreate()
        appPreferencesRepository = AppPreferencesRepository(dataStore)
    }
}