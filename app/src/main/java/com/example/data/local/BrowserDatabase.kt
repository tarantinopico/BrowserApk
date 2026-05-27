package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.domain.models.BookmarkItem
import com.example.domain.models.HistoryItem
import com.example.domain.models.BrowserIdentity
import com.example.domain.models.TabGroup
import com.example.domain.models.SavedTab

@Database(
    entities = [
        HistoryItem::class, 
        BookmarkItem::class,
        BrowserIdentity::class,
        TabGroup::class,
        SavedTab::class
    ], 
    version = 2, 
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao
}
