package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.domain.models.BrowserTab
import com.example.ui.theme.DesignTokens
import kotlin.math.roundToInt

@Composable
fun TopPullDownPanel(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onTabSelected: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    dragOffset: Float,
    isIncognito: Boolean,
    activeIdentity: com.example.domain.models.BrowserIdentity,
    identities: List<com.example.domain.models.BrowserIdentity>,
    onSwitchIdentity: (String) -> Unit
) {
    var showIdentityDropdown by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible || dragOffset > 0,
        enter = expandVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            expandFrom = Alignment.Top
        ) + fadeIn(),
        exit = shrinkVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            shrinkTowards = Alignment.Top
        ) + fadeOut(),
        modifier = Modifier.zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                    onDismiss()
                }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .offset { IntOffset(0, dragOffset.roundToInt().coerceAtLeast(0)) }
                    .clip(
                        RoundedCornerShape(
                            bottomStart = DesignTokens.CornerRadiusExtraLarge,
                            bottomEnd = DesignTokens.CornerRadiusExtraLarge
                        )
                    )
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {},
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 0.dp,
                shadowElevation = DesignTokens.ElevationHigh
            ) {
                if (isIncognito) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)))
                }
                
                Column(
                    modifier = Modifier.fillMaxSize().padding(WindowInsets.statusBars.asPaddingValues())
                ) {
                    // Actions Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DesignTokens.Spacing24),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Menu & Tabs", style = MaterialTheme.typography.headlineMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = onDismiss, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Close Panel", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Identity switch row
                    Box(modifier = Modifier.padding(horizontal = DesignTokens.Spacing24, vertical = DesignTokens.Spacing8)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(DesignTokens.CornerRadiusLarge))
                                .padding(DesignTokens.Spacing16),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Identity", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(DesignTokens.Spacing12))
                                Text(activeIdentity.name, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            }
                            Box {
                                TextButton(onClick = { showIdentityDropdown = true }) {
                                    Text("SWITCH", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                                DropdownMenu(
                                    expanded = showIdentityDropdown,
                                    onDismissRequest = { showIdentityDropdown = false }
                                ) {
                                    identities.forEach { identity ->
                                        DropdownMenuItem(
                                            text = { Text(identity.name, fontWeight = if (identity.id == activeIdentity.id) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) },
                                            onClick = {
                                                showIdentityDropdown = false
                                                if (identity.id != activeIdentity.id) {
                                                    onSwitchIdentity(identity.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing16))
                    // Main Browser Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing32),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickAction(Icons.Default.Add, "New Tab", onNewTab)
                        QuickAction(Icons.Default.Bookmarks, "Bookmarks", onNavigateToBookmarks)
                        QuickAction(Icons.Default.History, "History", onNavigateToHistory)
                        QuickAction(Icons.Default.Settings, "Settings", onNavigateToSettings)
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing24))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Tabs Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(140.dp),
                        contentPadding = PaddingValues(DesignTokens.Spacing24),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing16),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing16)
                    ) {
                        items(tabs) { tab ->
                            TabCard(
                                tab = tab,
                                isActive = tab.id == activeTabId,
                                onClick = { onTabSelected(tab.id) },
                                onClose = { onCloseTab(tab.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(DesignTokens.Spacing8)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(DesignTokens.CornerRadiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(DesignTokens.Spacing4))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun TabCard(
    tab: BrowserTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(DesignTokens.CornerRadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive && tab.state.isIncognito) Color.DarkGray
            else if (isActive) MaterialTheme.colorScheme.primaryContainer
            else if (tab.state.isIncognito) Color.Gray
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) DesignTokens.ElevationHigh else DesignTokens.ElevationLow)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing8, vertical = DesignTokens.Spacing4),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tab.state.title,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }
            HorizontalDivider()
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (tab.state.thumbnail != null) {
                    androidx.compose.foundation.Image(
                        bitmap = tab.state.thumbnail!!.asImageBitmap(),
                        contentDescription = "Thumbnail for ${tab.state.title}",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = tab.state.url.takeIf { it.isNotEmpty() && it != "about:blank" } ?: "New Tab",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(DesignTokens.Spacing8)
                    )
                }
            }
        }
    }
}
