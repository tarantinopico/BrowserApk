package com.example.ui.screens

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.models.BrowserTab
import com.example.ui.browser.BrowserViewModel
import kotlinx.coroutines.launch
import android.webkit.URLUtil

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

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
    
    val tabs by viewModel.tabManager.tabs.collectAsState()
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId }
    val customView by viewModel.tabManager.customView.collectAsState()
    val isIncognitoSession by viewModel.tabManager.isIncognitoSession.collectAsState()
    val searchEngineUrl by viewModel.searchEngine.collectAsState()
    
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

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
        urlInput = activeTab?.state?.url ?: ""
    }

    LaunchedEffect(activeTab?.state?.isLoading) {
        if (activeTab?.state?.isLoading == false) {
            isRefreshing = false
        }
    }

    if (customView != null) {
        // Fullscreen mode (e.g. for videos)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { customView!! }, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = { viewModel.tabManager.hideCustomView() },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Fullscreen", tint = Color.White)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val isSecure = activeTab?.state?.isSecure == true
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        placeholder = { Text("Search or type web address") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Info,
                                contentDescription = "Security",
                                tint = if (isSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        },
                        trailingIcon = {
                            if (activeTab?.state?.isLoading == true) {
                                IconButton(onClick = { activeTab.webView?.stopLoading() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Stop")
                                }
                            } else {
                                IconButton(onClick = { activeTab?.webView?.reload() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            var targetUrl = urlInput.trim()
                            if (URLUtil.isValidUrl(targetUrl)) {
                                activeTab?.webView?.loadUrl(targetUrl)
                            } else if (targetUrl.contains(".") && !targetUrl.contains(" ")) {
                                activeTab?.webView?.loadUrl("https://$targetUrl")
                            } else {
                                activeTab?.webView?.loadUrl(searchEngineUrl + targetUrl)
                            }
                        }),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        val isDesktop = activeTab?.state?.isDesktopMode == true
                        DropdownMenuItem(
                            text = { Text(if (isDesktop) "Mobile Site" else "Desktop Site") },
                            onClick = {
                                menuExpanded = false
                                activeTab?.id?.let { viewModel.tabManager.toggleDesktopMode(it) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                menuExpanded = false
                                onNavigateToSettings()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (activeTab?.state?.isIncognito == true) Color.DarkGray else MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = if (activeTab?.state?.isIncognito == true) Color.DarkGray else MaterialTheme.colorScheme.surface
            ) {
                IconButton(
                    onClick = { activeTab?.webView?.goBack() },
                    enabled = activeTab?.state?.canGoBack == true
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = if (activeTab?.state?.isIncognito == true) Color.White else LocalContentColor.current)
                }
                IconButton(
                    onClick = { activeTab?.webView?.goForward() },
                    enabled = activeTab?.state?.canGoForward == true
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = if (activeTab?.state?.isIncognito == true) Color.White else LocalContentColor.current)
                }
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = { 
                    activeTab?.let {
                        viewModel.addBookmark(it.state.url, it.state.title)
                    }
                }) {
                    Icon(Icons.Default.StarBorder, contentDescription = "Bookmark", tint = if (activeTab?.state?.isIncognito == true) Color.White else LocalContentColor.current)
                }

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onNavigateToTabs() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${tabs.size}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                
                IconButton(onClick = onNavigateToHistory) {
                    Icon(Icons.Default.History, contentDescription = "History", tint = if (activeTab?.state?.isIncognito == true) Color.White else LocalContentColor.current)
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = { 
                isRefreshing = true
                activeTab?.webView?.reload() 
            }
        ) {
            if (activeTab?.webView != null) {
                val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
                key(activeTabId) {
                    AndroidView(
                        factory = { activeTab.webView!! },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            val currentUrl = activeTab.state.url
                            if (currentUrl != "about:blank" && currentUrl.isNotEmpty()) {
                                viewModel.addHistory(currentUrl, activeTab.state.title, null)
                            }
                            viewModel.tabManager.applyDarkTheme(activeTabId ?: "", isSystemInDarkTheme)
                        }
                    )
                }
                
                if (activeTab.state.isLoading && activeTab.state.loadingProgress < 100) {
                    LinearProgressIndicator(
                        progress = { activeTab.state.loadingProgress / 100f },
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    )
                }
            } else {
                Text("No active tab", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
