package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "browser_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val DESKTOP_MODE_DEFAULT = booleanPreferencesKey("desktop_mode_default")
        val DARK_MODE_PREFERENCE = stringPreferencesKey("dark_mode_preference")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val COOKIES_ENABLED = booleanPreferencesKey("cookies_enabled")
        val NEW_TAB_PAGE = stringPreferencesKey("new_tab_page")
        val HOMEPAGE_URL = stringPreferencesKey("homepage_url")
    }

    val searchEngineFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SEARCH_ENGINE] ?: "https://www.google.com/search?q="
    }

    val desktopModeDefaultFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DESKTOP_MODE_DEFAULT] ?: false
    }

    val darkModePreferenceFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_PREFERENCE] ?: "system"
    }

    val javascriptEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[JAVASCRIPT_ENABLED] ?: true
    }

    val cookiesEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[COOKIES_ENABLED] ?: true
    }

    val newTabPageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[NEW_TAB_PAGE] ?: "speed_dial" // other options: "blank", "homepage", "custom_url"
    }

    val homepageUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HOMEPAGE_URL] ?: "https://www.google.com"
    }

    suspend fun updateSearchEngine(url: String) {
        context.dataStore.edit { preferences -> preferences[SEARCH_ENGINE] = url }
    }

    suspend fun updateDesktopModeDefault(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[DESKTOP_MODE_DEFAULT] = enabled }
    }

    suspend fun updateDarkModePreference(mode: String) {
        context.dataStore.edit { preferences -> preferences[DARK_MODE_PREFERENCE] = mode }
    }

    suspend fun updateJavascriptEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[JAVASCRIPT_ENABLED] = enabled }
    }

    suspend fun updateCookiesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[COOKIES_ENABLED] = enabled }
    }

    suspend fun updateNewTabPage(pageLayout: String) {
        context.dataStore.edit { preferences -> preferences[NEW_TAB_PAGE] = pageLayout }
    }

    suspend fun updateHomepageUrl(url: String) {
        context.dataStore.edit { preferences -> preferences[HOMEPAGE_URL] = url }
    }
}
