package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DesignTokens

data class MenuAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false
)

data class MenuSection(
    val title: String? = null,
    val actions: List<MenuAction>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTopMenu(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onShare: () -> Unit,
    onDesktopModeToggle: () -> Unit,
    onClearData: () -> Unit,
    isDesktopMode: Boolean = false,
    activeIdentityName: String,
    identities: List<com.example.domain.models.BrowserIdentity>,
    onSwitchIdentity: (String) -> Unit
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showIdentityDropdown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = DesignTokens.ElevationHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            // Header / Current Profile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing24, vertical = DesignTokens.Spacing12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Identity",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(DesignTokens.Spacing12))
                    Column {
                        Text("Active Identity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(activeIdentityName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                Box {
                    FilledTonalButton(onClick = { showIdentityDropdown = true }) {
                        Text("Switch")
                    }
                    DropdownMenu(
                        expanded = showIdentityDropdown,
                        onDismissRequest = { showIdentityDropdown = false }
                    ) {
                        identities.forEach { identity ->
                            DropdownMenuItem(
                                text = { Text(identity.name) },
                                onClick = {
                                    showIdentityDropdown = false
                                    if (identity.name != activeIdentityName) {
                                        onSwitchIdentity(identity.id)
                                        onDismiss()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing8))

            val sections = listOf(
                MenuSection(
                    actions = listOf(
                        MenuAction("New tab", Icons.Default.Add, { onNewTab(); onDismiss() }),
                        MenuAction("New incognito tab", Icons.Default.VisibilityOff, { onNewIncognitoTab(); onDismiss() })
                    )
                ),
                MenuSection(
                    actions = listOf(
                        MenuAction("History", Icons.Default.History, { onHistory(); onDismiss() }),
                        MenuAction("Bookmarks", Icons.Default.Star, { onBookmarks(); onDismiss() }),
                        MenuAction("Share", Icons.Default.Share, { onShare(); onDismiss() })
                    )
                ),
                MenuSection(
                    actions = listOf(
                        MenuAction(
                            if (isDesktopMode) "Mobile site" else "Desktop site",
                            if (isDesktopMode) Icons.Default.Smartphone else Icons.Default.DesktopMac,
                            { onDesktopModeToggle(); onDismiss() }
                        )
                    )
                ),
                MenuSection(
                    actions = listOf(
                        MenuAction("Clear browsing data", Icons.Default.DeleteSweep, { onClearData(); onDismiss() }, isDestructive = true),
                        MenuAction("Settings", Icons.Default.Settings, { onSettings(); onDismiss() })
                    )
                )
            )

            LazyColumn {
                sections.forEachIndexed { index, section ->
                    if (section.title != null) {
                        item {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = DesignTokens.Spacing24, vertical = DesignTokens.Spacing8)
                            )
                        }
                    }
                    items(section.actions) { action ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = action.title, 
                                    color = if (action.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge
                                ) 
                            },
                            onClick = action.onClick,
                            leadingIcon = {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.title,
                                    tint = if (action.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            contentPadding = PaddingValues(horizontal = DesignTokens.Spacing24, vertical = DesignTokens.Spacing12)
                        )
                    }
                    if (index < sections.lastIndex) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing4))
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing16))
                }
            }
        }
    }
}
