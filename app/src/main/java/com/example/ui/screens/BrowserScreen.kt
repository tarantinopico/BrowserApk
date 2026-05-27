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
import com.example.domain.models.BrowserTab
import com.example.ui.browser.BrowserViewModel
import com.example.ui.theme.DesignTokens
import kotlinx.coroutines.launch

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
    
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

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
    
    BackHandler(enabled = showBottomSheet) {
        showBottomSheet = false
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

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    val isSecure = activeTab?.state?.isSecure == true
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .focusRequester(focusRequester),
                        placeholder = { Text("Search or type web address", maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Info,
                                contentDescription = "Security",
                                modifier = Modifier.size(20.dp),
                                tint = if (isSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        },
                        trailingIcon = {
                            if (activeTab?.state?.isLoading == true) {
                                IconButton(onClick = { activeTab.webView?.stopLoading() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Stop", modifier = Modifier.size(20.dp))
                                }
                            } else {
                                IconButton(onClick = { activeTab?.webView?.reload() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload", modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            focusManager.clearFocus()
                            var targetUrl = urlInput.trim()
                            if (targetUrl.isEmpty()) return@KeyboardActions
                            if (URLUtil.isValidUrl(targetUrl)) {
                                activeTab?.webView?.loadUrl(targetUrl)
                            } else if (targetUrl.contains(".") && !targetUrl.contains(" ")) {
                                activeTab?.webView?.loadUrl("https://$targetUrl")
                            } else {
                                activeTab?.webView?.loadUrl(searchEngineUrl + targetUrl)
                            }
                        }),
                        shape = RoundedCornerShape(DesignTokens.CornerRadiusLarge),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isIncognitoSession) Color.DarkGray else MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = if (isIncognitoSession) Color.DarkGray else MaterialTheme.colorScheme.surface,
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                tonalElevation = DesignTokens.ElevationMedium,
                modifier = Modifier.height(DesignTokens.BottomBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing8),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { activeTab?.webView?.goBack() },
                        enabled = activeTab?.state?.canGoBack == true
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(
                        onClick = { activeTab?.webView?.goForward() },
                        enabled = activeTab?.state?.canGoForward == true
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(DesignTokens.CornerRadiusMedium))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onNavigateToTabs() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${tabs.size}", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { 
                        var targetUrl = homepageUrl
                        if (targetUrl.startsWith("http")) activeTab?.webView?.loadUrl(targetUrl)
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 50) {
                            // swipe right
                        } else if (dragAmount < -50) {
                            // swipe left
                        }
                    }
                }
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

            if (activeTab?.state?.isLoading == true && activeTab.state.loadingProgress < 100) {
                LinearProgressIndicator(
                    progress = { activeTab.state.loadingProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing16, vertical = DesignTokens.Spacing8)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BottomSheetActionIcon(icon = Icons.Default.StarBorder, label = "Bookmark") {
                        showBottomSheet = false
                        activeTab?.let { viewModel.addBookmark(it.state.url, it.state.title) }
                    }
                    BottomSheetActionIcon(icon = Icons.Default.History, label = "History") {
                        showBottomSheet = false
                        onNavigateToHistory()
                    }
                    BottomSheetActionIcon(icon = Icons.Default.Bookmarks, label = "Bookmarks") {
                        showBottomSheet = false
                        onNavigateToBookmarks()
                    }
                    val isDesktop = activeTab?.state?.isDesktopMode == true
                    BottomSheetActionIcon(icon = if (isDesktop) Icons.Default.PhoneAndroid else Icons.Default.DesktopMac, label = "Desktop") {
                        showBottomSheet = false
                        activeTab?.id?.let { viewModel.tabManager.toggleDesktopMode(it) }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing16))
                
                ListItem(
                    headlineContent = { Text("Settings") },
                    leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showBottomSheet = false
                        onNavigateToSettings()
                    }
                )
                Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
            }
        }
    }
}

@Composable
fun BottomSheetActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(DesignTokens.Spacing8)
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(DesignTokens.CornerRadiusMedium)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(DesignTokens.Spacing4))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SpeedDialContent(onUrlSubmit: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text("Good Morning", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(DesignTokens.Spacing24))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing12),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing12),
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
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(DesignTokens.CornerRadiusMedium)).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(site.first().uppercase(), style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing4))
                    Text(site.substringBefore("."), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(modifier = Modifier.weight(2f))
    }
}
