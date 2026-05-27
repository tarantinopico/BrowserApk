package com.example.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "identities_table")
data class BrowserIdentity(
    @PrimaryKey val id: String,
    val name: String,
    val colorAccent: Long,
    val isIncognito: Boolean,
    val createdAt: Long
)

@Entity(tableName = "tab_groups_table")
data class TabGroup(
    @PrimaryKey val id: String,
    val identityId: String,
    val name: String,
    val colorAccent: Long,
    val createdAt: Long,
    val isCollapsed: Boolean = false
)

@Entity(tableName = "saved_tabs_table")
data class SavedTab(
    @PrimaryKey val id: String,
    val identityId: String,
    val groupId: String?,
    val url: String,
    val title: String,
    val isIncognito: Boolean,
    val position: Int,
    val lastAccessed: Long
)
