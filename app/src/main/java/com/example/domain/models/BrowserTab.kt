package com.example.domain.models

import android.graphics.Bitmap
import android.webkit.WebView

data class BrowserTab(
    val id: String,
    var webView: WebView? = null,
    var state: TabState = TabState()
)

data class TabState(
    val url: String = "about:blank",
    val title: String = "New Tab",
    val favicon: Bitmap? = null,
    val thumbnail: Bitmap? = null,
    val isLoading: Boolean = false,
    val loadingProgress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = true,
    val isDesktopMode: Boolean = false,
    val isIncognito: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)
