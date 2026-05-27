package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.browser.BrowserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val searchEngine by viewModel.settings.searchEngineFlow.collectAsState(initial = "https://www.google.com/search?q=")
    val isDesktopDefault by viewModel.settings.desktopModeDefaultFlow.collectAsState(initial = false)
    val darkModePref by viewModel.settings.darkModePreferenceFlow.collectAsState(initial = "system")
    val javascriptEnabled by viewModel.settings.javascriptEnabledFlow.collectAsState(initial = true)
    
    val scope = rememberCoroutineScope()
    var showClearDataDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("General", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                ListItem(
                    headlineContent = { Text("Search Engine") },
                    supportingContent = { Text(if (searchEngine.contains("google")) "Google" else if (searchEngine.contains("duckduckgo")) "DuckDuckGo" else "Custom") },
                    modifier = Modifier.clickable {
                        scope.launch {
                            val newEngine = if (searchEngine.contains("google")) "https://duckduckgo.com/?q=" else "https://www.google.com/search?q="
                            viewModel.settings.updateSearchEngine(newEngine)
                        }
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Desktop Mode Default") },
                    trailingContent = {
                        Switch(
                            checked = isDesktopDefault,
                            onCheckedChange = { scope.launch { viewModel.settings.updateDesktopModeDefault(it) } }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text("Enable JavaScript") },
                    trailingContent = {
                        Switch(
                            checked = javascriptEnabled,
                            onCheckedChange = { scope.launch { viewModel.settings.updateJavascriptEnabled(it) } }
                        )
                    }
                )
            }
            
            item {
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Privacy & Security", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                ListItem(
                    headlineContent = { Text("Clear Browsing Data") },
                    supportingContent = { Text("Clear history, cookies, and cache") },
                    modifier = Modifier.clickable { showClearDataDialog = true }
                )
            }
        }
        
        if (showClearDataDialog) {
            var clearHistory by remember { mutableStateOf(true) }
            var clearCookies by remember { mutableStateOf(false) }
            var clearCache by remember { mutableStateOf(true) }
            
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Clear Browsing Data") },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = clearHistory, onCheckedChange = { clearHistory = it })
                            Text("Browsing History")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = clearCookies, onCheckedChange = { clearCookies = it })
                            Text("Cookies and site data")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = clearCache, onCheckedChange = { clearCache = it })
                            Text("Cached images and files")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearBrowsingData(clearHistory, clearCookies, clearCache)
                        showClearDataDialog = false
                    }) {
                        Text("Clear Data")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
