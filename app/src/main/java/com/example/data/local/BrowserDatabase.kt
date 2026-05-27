package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.domain.models.BookmarkItem
import com.example.domain.models.HistoryItem

@Database(entities = [HistoryItem::class, BookmarkItem::class], version = 1, exportSchema = false)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao
}
