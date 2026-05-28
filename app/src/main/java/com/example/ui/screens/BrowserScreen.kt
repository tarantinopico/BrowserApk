package com.example.ui.screens

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.URLUtil
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import com.example.domain.models.BrowserTab
import com.example.ui.browser.BrowserViewModel
import com.example.ui.theme.DesignTokens
import kotlinx.coroutines.launch

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import com.example.ui.components.TopPullDownPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onNavigateToTabs: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    var isOmniboxFocused by remember { mutableStateOf(false) }
    
    val tabs by viewModel.tabManager.tabs.collectAsState()
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId }
    val customView by viewModel.tabManager.customView.collectAsState()
    val isIncognitoSession by viewModel.tabManager.isIncognitoSession.collectAsState()
    val searchEngineUrl by viewModel.searchEngine.collectAsState()
    val newTabPage by viewModel.newTabPage.collectAsState()
    val homepageUrl by viewModel.homepageUrl.collectAsState()
    
    val activeIdentity by viewModel.activeIdentity.collectAsState()
    val identities by viewModel.identities.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var showTopPanel by remember { mutableStateOf(false) }
    var panelDragOffset by remember { mutableStateOf(0f) }

    // File chooser launcher
    val fileChooserLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val result = if (uris.isNotEmpty()) uris.toTypedArray() else null
        viewModel.tabManager.fileChooserCallback?.onReceiveValue(result)
        viewModel.tabManager.fileChooserCallback = null
    }

    DisposableEffect(context) {
        viewModel.tabManager.attachActivityContext(context)
        viewModel.tabManager.setFileLaunchHandler { callback, params ->
            val acceptTypes = params?.acceptTypes?.joinToString(",") ?: "*/*"
            val type = if (acceptTypes.isEmpty()) "*/*" else acceptTypes
            fileChooserLauncher.launch(type)
        }
        onDispose {
            viewModel.tabManager.detachActivityContext()
            viewModel.tabManager.setFileLaunchHandler { _, _ -> }
        }
    }

    LaunchedEffect(activeTab?.state?.url) {
        if (!isOmniboxFocused) {
            urlInput = activeTab?.state?.url?.let { if (it == "about:blank") "" else it } ?: ""
        }
    }

    LaunchedEffect(activeTab?.state?.isLoading) {
        if (activeTab?.state?.isLoading == false) {
            isRefreshing = false
        }
    }
    
    BackHandler(enabled = showTopPanel) {
        showTopPanel = false
    }

    if (customView != null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { customView!! }, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = { viewModel.tabManager.hideCustomView() },
                modifier = Modifier.align(Alignment.TopEnd).padding(DesignTokens.Spacing16).statusBarsPadding()
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Fullscreen", tint = Color.White)
            }
        }
        return
    }

    val onUrlSubmit: (String) -> Unit = { targetUrl ->
        var cleanUrl = targetUrl.trim()
        if (cleanUrl.isNotEmpty()) {
            if (URLUtil.isValidUrl(cleanUrl)) {
                activeTab?.webView?.loadUrl(cleanUrl)
            } else if (cleanUrl.contains(".") && !cleanUrl.contains(" ")) {
                activeTab?.webView?.loadUrl("https://$cleanUrl")
            } else {
                activeTab?.webView?.loadUrl(searchEngineUrl + cleanUrl)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (panelDragOffset > 150f) {
                                        showTopPanel = true
                                    }
                                    panelDragOffset = 0f
                                }
                            ) { change, dragAmount ->
                                if (!showTopPanel && dragAmount > 0) {
                                    panelDragOffset += dragAmount
                                    change.consume()
                                }
                            }
                        }
                ) {
                    BrowserTopBar(
                        urlInput = urlInput,
                        onUrlInputChange = { urlInput = it },
                        onUrlSubmit = onUrlSubmit,
                        activeTab = activeTab,
                        isIncognito = isIncognitoSession,
                        onGoBack = { activeTab?.webView?.goBack() },
                        onReloadStop = {
                            if (activeTab?.state?.isLoading == true) activeTab.webView?.stopLoading()
                            else activeTab?.webView?.reload()
                        },
                        isOmniboxFocused = isOmniboxFocused,
                        onOmniboxFocusChanged = { isOmniboxFocused = it }
                    )
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    val currentUrl = activeTab?.state?.url ?: ""
                    if (currentUrl == "about:blank" || currentUrl.isEmpty()) {
                        if (newTabPage == "speed_dial") {
                            SpeedDialContent(onUrlSubmit = { url -> activeTab?.webView?.loadUrl(url) })
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                        }
                    } else {
                        PullToRefreshBox(
                            modifier = Modifier.fillMaxSize(),
                            state = pullToRefreshState,
                            isRefreshing = isRefreshing,
                            onRefresh = { 
                                isRefreshing = true
                                activeTab?.webView?.reload() 
                            }
                        ) {
                            if (activeTab?.webView != null) {
                                val isSystemInDarkTheme = isSystemInDarkTheme()
                                key(activeTabId) {
                                    AndroidView(
                                        factory = { activeTab.webView!! },
                                        modifier = Modifier.fillMaxSize(),
                                        update = { view ->
                                            val url = activeTab.state.url
                                            if (url != "about:blank" && url.isNotEmpty()) {
                                                viewModel.addHistory(url, activeTab.state.title, null)
                                            }
                                            viewModel.tabManager.applyDarkTheme(activeTabId ?: "", isSystemInDarkTheme)
                                        }
                                    )
                                }
                            } else {
                                Text("No active tab", modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
            
            TopPullDownPanel(
                isVisible = showTopPanel,
                onDismiss = { showTopPanel = false },
                tabs = tabs,
                activeTabId = activeTabId,
                onTabSelected = { 
                    viewModel.tabManager.selectTab(it)
                    showTopPanel = false
                },
                onCloseTab = { viewModel.tabManager.closeTab(it) },
                onNewTab = { 
                    viewModel.tabManager.addNewTab()
                    showTopPanel = false
                },
                onNavigateToSettings = {
                    showTopPanel = false
                    onNavigateToSettings()
                },
                onNavigateToBookmarks = {
                    showTopPanel = false
                    onNavigateToBookmarks()
                },
                onNavigateToHistory = {
                    showTopPanel = false
                    onNavigateToHistory()
                },
                dragOffset = panelDragOffset,
                isIncognito = isIncognitoSession,
                activeIdentity = activeIdentity,
                identities = identities,
                onSwitchIdentity = { viewModel.switchIdentity(it) }
            )
        }
    }
}

@Composable
fun SpeedDialContent(onUrlSubmit: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing24),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text("Good Morning", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(DesignTokens.Spacing32))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing16),
            modifier = Modifier.fillMaxWidth()
        ) {
            val topSites = listOf("google.com", "youtube.com", "wikipedia.org", "reddit.com", "amazon.com", "twitter.com", "github.com", "stackoverflow.com")
            items(topSites.size) { i ->
                val site = topSites[i]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onUrlSubmit("https://www.$site") }.padding(DesignTokens.Spacing4)
                ) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(DesignTokens.CornerRadiusMedium)).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(site.first().uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing8))
                    Text(site.substringBefore("."), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(modifier = Modifier.weight(2f))
    }
}

@Composable
fun BrowserTopBar(
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    onUrlSubmit: (String) -> Unit,
    activeTab: BrowserTab?,
    isIncognito: Boolean,
    onGoBack: () -> Unit,
    onReloadStop: () -> Unit,
    isOmniboxFocused: Boolean,
    onOmniboxFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing12, vertical = DesignTokens.Spacing8),
        shape = RoundedCornerShape(DesignTokens.CornerRadiusExtraLarge),
        color = if (isIncognito) Color.DarkGray else MaterialTheme.colorScheme.surfaceColorAtElevation(DesignTokens.ElevationLow),
        tonalElevation = DesignTokens.ElevationLow,
        shadowElevation = DesignTokens.ElevationLow
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing8, vertical = DesignTokens.Spacing8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(visible = !isOmniboxFocused) {
                    IconButton(onClick = onGoBack, enabled = activeTab?.state?.canGoBack == true, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(DesignTokens.CornerRadiusLarge))
                        .background(if (isIncognito) Color.Gray.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = DesignTokens.Spacing12),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val isSecure = activeTab?.state?.isSecure == true
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(visible = !isOmniboxFocused) {
                            Row {
                                Icon(
                                    imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Info,
                                    contentDescription = "Security Status",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(DesignTokens.Spacing8))
                            }
                        }
                        
                        BasicTextField(
                            value = urlInput,
                            onValueChange = onUrlInputChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { onOmniboxFocusChanged(it.isFocused) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                focusManager.clearFocus()
                                onUrlSubmit(urlInput)
                            }),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (urlInput.isEmpty()) {
                                        Text("Search or type URL", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        if (urlInput.isNotEmpty() && isOmniboxFocused) {
                            IconButton(onClick = { onUrlInputChange("") }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear text", modifier = Modifier.size(16.dp))
                            }
                        } else if (!isOmniboxFocused) {
                            val isLoading = activeTab?.state?.isLoading == true
                            IconButton(onClick = onReloadStop, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                    contentDescription = if (isLoading) "Stop loading" else "Reload page",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            val isLoading = activeTab?.state?.isLoading == true
            val progress = activeTab?.state?.loadingProgress ?: 0
            
            AnimatedVisibility(visible = isLoading) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

// Bottom bar removed for minimalist UI

