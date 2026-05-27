package com.example.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks_table")
data class BookmarkItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long
)
