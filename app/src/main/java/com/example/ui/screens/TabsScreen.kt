package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.browser.BrowserViewModel
import com.example.ui.theme.DesignTokens

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
                },
                windowInsets = WindowInsets.statusBars
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
                SmallFloatingActionButton(
                    onClick = {
                        viewModel.tabManager.addNewTab("about:blank", isIncognito = true)
                        onTabSelected()
                    },
                    modifier = Modifier.padding(bottom = DesignTokens.Spacing8)
                ) {
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
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(
                start = DesignTokens.Spacing16,
                end = DesignTokens.Spacing16,
                top = DesignTokens.Spacing16,
                bottom = DesignTokens.Spacing16 + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing16),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing16)
        ) {
            items(tabs) { tab ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.65f) // Closer to phone screen aspect ratio
                        .clickable {
                            viewModel.tabManager.selectTab(tab.id)
                            onTabSelected()
                        },
                    shape = RoundedCornerShape(DesignTokens.CornerRadiusMedium),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (tab.id == activeTabId) DesignTokens.ElevationHigh else DesignTokens.ElevationLow
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (tab.id == activeTabId && tab.state.isIncognito) Color.DarkGray
                        else if (tab.id == activeTabId) MaterialTheme.colorScheme.primaryContainer
                        else if (tab.state.isIncognito) Color.Gray
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing8, vertical = DesignTokens.Spacing4),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tab.state.favicon != null) {
                                Image(
                                    bitmap = tab.state.favicon!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).padding(end = DesignTokens.Spacing4)
                                )
                            }
                            Text(
                                text = tab.state.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            IconButton(
                                onClick = { viewModel.tabManager.closeTab(tab.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close tab", modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                            // Thumbnail Preview Setup
                            if (tab.state.thumbnail != null) {
                                Image(
                                    bitmap = tab.state.thumbnail!!.asImageBitmap(),
                                    contentDescription = "Tab preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = tab.state.url.takeIf { it.isNotEmpty() && it != "about:blank" } ?: "New Tab",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(DesignTokens.Spacing16)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
