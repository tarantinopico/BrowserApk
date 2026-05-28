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

import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import com.example.data.repository.IdentityRepository
import com.example.data.repository.TabGroupRepository
import com.example.domain.models.BrowserIdentity
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModel(
    private val context: Context,
    private val repository: BrowserRepository,
    val settings: SettingsRepository,
    val identityRepository: IdentityRepository,
    val tabGroupRepository: TabGroupRepository
) : ViewModel() {

    companion object {
        fun provideFactory(
            context: Context,
            repository: BrowserRepository,
            settings: SettingsRepository,
            identityRepository: IdentityRepository,
            tabGroupRepository: TabGroupRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
                    return BrowserViewModel(context, repository, settings, identityRepository, tabGroupRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    val tabManager = TabManager(context)

    val identities = identityRepository.getAllIdentities()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeIdentityId = settings.activeIdentityIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "default")

    val activeIdentity = kotlinx.coroutines.flow.combine(identities, activeIdentityId) { ids, activeId ->
        ids.find { it.id == activeId } ?: ids.firstOrNull() ?: BrowserIdentity("default", "Personal", 0xFF4285F4, false, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.Lazily, BrowserIdentity("default", "Personal", 0xFF4285F4, false, System.currentTimeMillis()))

    val history = activeIdentityId.flatMapLatest { id -> repository.getAllHistory(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val bookmarks = activeIdentityId.flatMapLatest { id -> repository.getAllBookmarks(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val searchEngine = settings.searchEngineFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "https://www.google.com/search?q=")

    val newTabPage = settings.newTabPageFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "speed_dial")

    val homepageUrl = settings.homepageUrlFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "https://www.google.com")

    private var currentIdentityJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            identityRepository.initializeDefaultIdentityIfNeeded()
        }
        
        // Listen for identity changes
        viewModelScope.launch {
            activeIdentityId.collectLatest { identityId ->
                loadTabsForIdentity(identityId)
            }
        }
        
        // Listen to tab changes to sync to database periodically or reactively
        viewModelScope.launch {
            tabManager.tabs.collectLatest { tabs ->
                kotlinx.coroutines.delay(2000) // Debounce 2 seconds
                syncTabsToDatabase()
            }
        }
    }

    private fun loadTabsForIdentity(identityId: String) {
        viewModelScope.launch {
            val savedTabs = tabGroupRepository.getSavedTabsForIdentity(identityId).first()
            tabManager.clear()
            if (savedTabs.isEmpty()) {
                tabManager.addNewTab()
            } else {
                savedTabs.forEach { savedTab ->
                    tabManager.addNewTab(url = savedTab.url, isIncognito = savedTab.isIncognito)
                }
                // Try to restore active tab
                savedTabs.maxByOrNull { it.lastAccessed }?.id?.let { activeId ->
                    val newActiveId = tabManager.tabs.value.find { tab -> tab.state.url == savedTabs.first { it.id == activeId }.url }?.id
                    if (newActiveId != null) {
                        tabManager.selectTab(newActiveId)
                    }
                }
            }
        }
    }

    fun syncTabsToDatabase() {
        val currentIdentity = activeIdentityId.value
        val tabs = tabManager.tabs.value.toList()
        viewModelScope.launch {
            // Because we only sync one identity at a time and don't want to mess up IDs,
            // we delete all previous and re-insert or just update...
            // the safest is to fetch current, delete those that are gone, insert new/updated.
            // But since this is a debounce, let's keep it simple:
            val existing = tabGroupRepository.getSavedTabsForIdentity(currentIdentity).first()
            existing.forEach { tabGroupRepository.deleteSavedTab(it) }
            
            tabs.forEachIndexed { index, tab ->
                val savedTab = com.example.domain.models.SavedTab(
                    id = tab.id,
                    identityId = currentIdentity,
                    groupId = null,
                    url = tab.state.url,
                    title = tab.state.title,
                    isIncognito = tab.state.isIncognito,
                    position = index,
                    lastAccessed = if (tabManager.activeTabId.value == tab.id) System.currentTimeMillis() else 0L
                )
                tabGroupRepository.insertSavedTab(savedTab)
            }
        }
    }

    fun switchIdentity(id: String) {
        viewModelScope.launch {
            syncTabsToDatabase() // Force sync before switching
            settings.updateActiveIdentityId(id)
            // The activeIdentityId listener will trigger loadTabsForIdentity
        }
    }

    fun addHistory(url: String, title: String, faviconUrl: String?) {
        // Prevent recording incognito tabs
        val activeTab = tabManager.getActiveTab()
        if (activeTab?.state?.isIncognito == true) return
        
        viewModelScope.launch {
            repository.insertHistory(HistoryItem(identityId = activeIdentityId.value, url = url, title = title, faviconUrl = faviconUrl, visitedAt = System.currentTimeMillis()))
        }
    }

    fun clearBrowsingData(history: Boolean, cookies: Boolean, cache: Boolean) {
        viewModelScope.launch {
            if (history) repository.clearHistory(activeIdentityId.value)
            if (cookies) CookieManager.getInstance().removeAllCookies(null) // WebView cookies apply to whole app unless separated
            if (cache) {
                WebStorage.getInstance().deleteAllData()
                WebView(context).clearCache(true)
            }
        }
    }

    fun addBookmark(url: String, title: String) {
        viewModelScope.launch {
            repository.insertBookmark(BookmarkItem(identityId = activeIdentityId.value, url = url, title = title, createdAt = System.currentTimeMillis()))
        }
    }
    
    fun removeBookmark(item: BookmarkItem) {
        viewModelScope.launch {
            repository.deleteBookmark(item)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tabManager.detachActivityContext()
    }
}
