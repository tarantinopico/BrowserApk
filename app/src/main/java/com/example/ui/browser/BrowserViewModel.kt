package com.example.ui.browser

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BrowserRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.models.BookmarkItem
import com.example.domain.models.HistoryItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrowserViewModel(
    private val context: Context,
    private val repository: BrowserRepository,
    val settings: SettingsRepository
) : ViewModel() {

    companion object {
        fun provideFactory(
            context: Context,
            repository: BrowserRepository,
            settings: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
                    return BrowserViewModel(context, repository, settings) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    val tabManager = TabManager(context)

    val history = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val bookmarks = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val searchEngine = settings.searchEngineFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "https://www.google.com/search?q=")

    init {
        if (tabManager.tabs.value.isEmpty()) {
            tabManager.addNewTab()
        }
    }

    fun addHistory(url: String, title: String, faviconUrl: String?) {
        // Prevent recording incognito tabs
        val activeTab = tabManager.getActiveTab()
        if (activeTab?.state?.isIncognito == true) return
        
        viewModelScope.launch {
            repository.insertHistory(HistoryItem(url = url, title = title, faviconUrl = faviconUrl, visitedAt = System.currentTimeMillis()))
        }
    }

    fun clearBrowsingData(history: Boolean, cookies: Boolean, cache: Boolean) {
        viewModelScope.launch {
            if (history) repository.clearHistory()
            if (cookies) CookieManager.getInstance().removeAllCookies(null)
            if (cache) {
                WebStorage.getInstance().deleteAllData()
                WebView(context).clearCache(true)
            }
        }
    }

    fun addBookmark(url: String, title: String) {
        viewModelScope.launch {
            repository.insertBookmark(BookmarkItem(url = url, title = title, createdAt = System.currentTimeMillis()))
        }
    }
    
    fun removeBookmark(item: BookmarkItem) {
        viewModelScope.launch {
            repository.deleteBookmark(item)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // We only clear if this ViewModel is actually completely destroyed (process death handles itself).
        // Since it's tied to MainActivity, it shouldn't clear on rotation (ViewModel survives).
        tabManager.detachActivityContext()
    }
}
