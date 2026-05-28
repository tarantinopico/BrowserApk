package com.example.ui.browser

import android.content.Context
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebStorage
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.ProfileStore
import com.example.domain.models.BrowserTab
import com.example.domain.models.TabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class TabManager(private val applicationContext: Context) {
    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId
    
    private val savedStates = mutableMapOf<String, Bundle>()
    private var baseContextWrapper: MutableContextWrapper? = null

    // For full screen videos
    val _customView = MutableStateFlow<View?>(null)
    val customView: StateFlow<View?> = _customView
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val _isIncognitoSession = MutableStateFlow<Boolean>(false)
    val isIncognitoSession: StateFlow<Boolean> = _isIncognitoSession

    // For file choosers
    var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    var requestFileLaunchHandler: ((ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Unit)? = null

    private val defaultUserAgent = WebSettings.getDefaultUserAgent(applicationContext)
    private val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun attachActivityContext(context: Context) {
        if (baseContextWrapper == null) {
            baseContextWrapper = MutableContextWrapper(context)
        } else {
            baseContextWrapper?.baseContext = context
        }
        _tabs.value.forEach { tab ->
            tab.webView?.let { swapContext(it, context) }
        }
    }
    
    private fun swapContext(webView: WebView, newContext: Context) {
        val contextWrapper = webView.context as? MutableContextWrapper
        contextWrapper?.baseContext = newContext
    }

    fun detachActivityContext() {
        baseContextWrapper?.baseContext = applicationContext
        _tabs.value.forEach { tab ->
            tab.webView?.let { swapContext(it, applicationContext) }
        }
    }

    fun setFileLaunchHandler(handler: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Unit) {
        requestFileLaunchHandler = handler
    }

    fun applyDarkTheme(id: String, isDark: Boolean) {
        val tab = _tabs.value.find { it.id == id } ?: return
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(tab.webView?.settings!!, isDark)
        }
    }

    fun addNewTab(url: String = "about:blank", isIncognito: Boolean = false): String {
        // If switching mode, we could isolate or clear cookies for incognito
        _isIncognitoSession.value = _isIncognitoSession.value || isIncognito
        val newId = UUID.randomUUID().toString()
        val webView = createWebView(newId, url, isIncognito)
        
        val newTab = BrowserTab(
            id = newId,
            webView = webView,
            state = TabState(url = url, isIncognito = isIncognito)
        )
        _tabs.update { it + newTab }
        _activeTabId.value = newId
        return newId
    }

    fun toggleDesktopMode(id: String) {
        val tab = _tabs.value.find { it.id == id } ?: return
        val currentDesktopMode = tab.state.isDesktopMode
        val newDesktopMode = !currentDesktopMode
        
        tab.webView?.settings?.apply {
            userAgentString = if (newDesktopMode) desktopUserAgent else defaultUserAgent
            useWideViewPort = newDesktopMode
            loadWithOverviewMode = newDesktopMode
        }
        
        updateTabState(id) { it.copy(isDesktopMode = newDesktopMode) }
        tab.webView?.reload()
    }

    fun selectTab(id: String) {
        if (_tabs.value.any { it.id == id }) {
            _activeTabId.value = id
        }
    }

    fun trimMemory(level: Int) {
        // When memory is low, pause timers and clean up
        _tabs.value.forEach { tab ->
            if (tab.id != _activeTabId.value) {
                // Free up some memory for inactive tabs
                tab.webView?.freeMemory() // Deprecated but helps in some Android versions
            }
        }
    }

    fun closeTab(id: String) {
        val currentTabs = _tabs.value.toMutableList()
        val index = currentTabs.indexOfFirst { it.id == id }
        if (index != -1) {
            val tab = currentTabs[index]
            val wasIncognito = tab.state.isIncognito
            tab.webView?.destroy()
            currentTabs.removeAt(index)
            savedStates.remove(id)
            _tabs.value = currentTabs

            if (_activeTabId.value == id) {
                _activeTabId.value = if (currentTabs.isNotEmpty()) {
                    currentTabs[maxOf(0, index - 1)].id
                } else {
                    addNewTab("about:blank")
                }
            }

            // If we closed the last incognito tab, clean up session cookies & cache
            if (wasIncognito && currentTabs.none { it.state.isIncognito }) {
                _isIncognitoSession.value = false
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeSessionCookies(null)
                WebStorage.getInstance().deleteAllData()
            }
        }
    }
    
    private fun createWebView(tabId: String, initialUrl: String, isIncognito: Boolean, identityId: String = "default"): WebView {
        val context = baseContextWrapper ?: MutableContextWrapper(applicationContext)
        val webView = WebView(context)
        
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                val profileName = if (isIncognito) "incognito_$identityId" else "identity_$identityId"
                androidx.webkit.WebViewCompat.setProfile(webView, profileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = !isIncognito
            settings.defaultTextEncodingName = "utf-8"
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW // strict
            settings.safeBrowsingEnabled = true

            // Security Hardening
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, !isIncognito)
            if (isIncognito) {
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
            } else {
                settings.cacheMode = WebSettings.LOAD_DEFAULT
            }
            
            webViewClient = CustomWebViewClient(
                context = context,
                tabId = tabId,
                onPageStartedUpdate = { url, favicon, isSecure ->
                    updateTabState(tabId) { 
                        it.copy(isLoading = true, url = url ?: it.url, isSecure = isSecure) 
                    }
                },
                onPageFinishedUpdate = { url, title, canGoBack, canGoForward ->
                    var thumbnailBitmap: android.graphics.Bitmap? = null
                    val scope = this@apply.handler
                    scope?.postDelayed({
                        try {
                            if (this@apply.width > 0 && this@apply.height > 0) {
                                val scale = 0.25f // 1/4 size
                                val width = (this@apply.width * scale).toInt()
                                val height = (this@apply.height * scale).toInt()
                                if (width > 0 && height > 0) {
                                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(bitmap)
                                    canvas.scale(scale, scale)
                                    this@apply.draw(canvas)
                                    thumbnailBitmap = bitmap
                                    
                                    updateTabState(tabId) { 
                                        it.copy(thumbnail = bitmap)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 500) // Small delay to let rendering finish

                    updateTabState(tabId) { 
                        it.copy(
                            isLoading = false,
                            url = url ?: it.url,
                            title = title ?: it.title,
                            canGoBack = canGoBack,
                            canGoForward = canGoForward
                        )
                    }
                }
            )

            webChromeClient = CustomWebChromeClient(
                tabId = tabId,
                onProgressChanged = { newProgress ->
                    updateTabState(tabId) { it.copy(loadingProgress = newProgress) }
                },
                onReceivedTitle = { title ->
                    updateTabState(tabId) { it.copy(title = title ?: it.title) }
                },
                onReceivedIcon = { icon ->
                    updateTabState(tabId) { it.copy(favicon = icon) }
                },
                onShowCustomView = { view, callback ->
                    _customView.value = view
                    customViewCallback = callback
                },
                onHideCustomView = {
                    hideCustomView()
                },
                requestFileLaunch = { callback, params ->
                    fileChooserCallback = callback
                    requestFileLaunchHandler?.invoke(callback, params)
                }
            )
            
            setDownloadListener(CustomDownloadListener(context))
        }
        
        savedStates[tabId]?.let { bundle ->
            webView.restoreState(bundle)
        } ?: run {
            if (initialUrl != "about:blank") {
                webView.loadUrl(initialUrl)
            }
        }
        
        return webView
    }

    fun hideCustomView() {
        _customView.value = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
    }

    private fun updateTabState(id: String, update: (TabState) -> TabState) {
        _tabs.update { currentTabs ->
            currentTabs.map { tab ->
                if (tab.id == id) {
                    tab.copy(state = update(tab.state))
                } else {
                    tab
                }
            }
        }
    }
    
    fun getActiveTab(): BrowserTab? {
        return _tabs.value.find { it.id == _activeTabId.value }
    }

    fun saveState(outState: Bundle) {
        val tabIds = _tabs.value.map { it.id }
        outState.putStringArrayList("tabIds", ArrayList(tabIds))
        outState.putString("activeTabId", _activeTabId.value)
        outState.putBoolean("isIncognitoSession", _isIncognitoSession.value)
        
        _tabs.value.forEach { tab ->
            val bundle = Bundle()
            tab.webView?.saveState(bundle)
            bundle.putBoolean("isDesktopMode", tab.state.isDesktopMode)
            bundle.putBoolean("isIncognito", tab.state.isIncognito)
            outState.putBundle("tab_state_${tab.id}", bundle)
        }
    }

    fun restoreState(savedState: Bundle) {
        val tabIds = savedState.getStringArrayList("tabIds") ?: return
        val activeId = savedState.getString("activeTabId")
        _isIncognitoSession.value = savedState.getBoolean("isIncognitoSession", false)
        
        tabIds.forEach { id ->
            val bundle = savedState.getBundle("tab_state_$id")
            if (bundle != null) {
                savedStates[id] = bundle
                val isDesktopMode = bundle.getBoolean("isDesktopMode", false)
                val isIncognito = bundle.getBoolean("isIncognito", false)
                val webView = createWebView(id, "about:blank", isIncognito)
                
                if (isDesktopMode) {
                    webView.settings.userAgentString = desktopUserAgent
                    webView.settings.useWideViewPort = true
                    webView.settings.loadWithOverviewMode = true
                }

                val tab = BrowserTab(id = id, webView = webView, state = TabState(isDesktopMode = isDesktopMode, isIncognito = isIncognito))
                _tabs.update { it + tab }
                
                webView.title?.let { title -> updateTabState(id) { it.copy(title = title) } }
                webView.url?.let { url -> updateTabState(id) { it.copy(url = url) } }
            }
        }
        if (tabIds.isNotEmpty()) {
            _activeTabId.value = activeId ?: tabIds.first()
        } else {
            addNewTab("https://www.google.com")
        }
    }
    
    fun clear() {
        _tabs.value.forEach { it.webView?.destroy() }
        _tabs.value = emptyList()
        savedStates.clear()
        _activeTabId.value = null
    }
}
