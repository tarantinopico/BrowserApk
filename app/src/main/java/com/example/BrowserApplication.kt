package com.example

import android.app.Application
import android.content.Context
import android.webkit.WebView
import androidx.room.Room
import com.example.data.local.BrowserDatabase
import com.example.data.repository.BrowserRepository
import com.example.data.repository.IdentityRepository
import com.example.data.repository.TabGroupRepository
import com.example.data.repository.SettingsRepository

class BrowserApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WebView.startSafeBrowsing(this) { success ->
            // Safe browsing initialized
        }
    }
}

class AppContainer(private val context: Context) {
    val database: BrowserDatabase by lazy {
        Room.databaseBuilder(
            context,
            BrowserDatabase::class.java,
            "browser_database"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
    
    val browserRepository: BrowserRepository by lazy {
        BrowserRepository(database.browserDao())
    }
    
    val identityRepository: IdentityRepository by lazy {
        IdentityRepository(database.browserDao())
    }
    
    val tabGroupRepository: TabGroupRepository by lazy {
        TabGroupRepository(database.browserDao())
    }
    
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }
}
