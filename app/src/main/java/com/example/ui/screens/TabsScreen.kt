package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.browser.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsScreen(
    viewModel: BrowserViewModel,
    onTabSelected: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val tabs by viewModel.tabManager.tabs.collectAsState()
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${tabs.size} Tabs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = {
                        viewModel.tabManager.addNewTab("about:blank", isIncognito = true)
                        onTabSelected()
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    // Using basic Add icon as placeholder for incognito style
                    Icon(Icons.Default.Add, contentDescription = "New Incognito Tab", tint = MaterialTheme.colorScheme.error)
                }
                FloatingActionButton(onClick = {
                    viewModel.tabManager.addNewTab()
                    onTabSelected()
                }) {
                    Icon(Icons.Default.Add, contentDescription = "New Tab")
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs) { tab ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f)
                        .clickable {
                            viewModel.tabManager.selectTab(tab.id)
                            onTabSelected()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (tab.id == activeTabId && tab.state.isIncognito) Color.DarkGray
                        else if (tab.id == activeTabId) MaterialTheme.colorScheme.primaryContainer
                        else if (tab.state.isIncognito) Color.Gray
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.state.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.tabManager.closeTab(tab.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close tab", modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        HorizontalDivider()
                        
                        Text(
                            text = tab.state.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(8.dp)
                        )
                        // A placeholder for actual thumbnail. In a real app we'd capture bitmap 
                        // from webview and show it here.
                    }
                }
            }
        }
    }
}
