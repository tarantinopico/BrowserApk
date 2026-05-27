package com.example.data.repository

import com.example.data.local.BrowserDao
import com.example.domain.models.BookmarkItem
import com.example.domain.models.HistoryItem
import kotlinx.coroutines.flow.Flow
class BrowserRepository(
    private val dao: BrowserDao
) {
    suspend fun insertHistory(item: HistoryItem) = dao.insertHistory(item)
    fun getAllHistory(): Flow<List<HistoryItem>> = dao.getAllHistory()
    fun searchHistory(query: String): Flow<List<HistoryItem>> = dao.searchHistory(query)
    suspend fun deleteHistory(item: HistoryItem) = dao.deleteHistory(item)
    suspend fun clearHistory() = dao.clearHistory()

    suspend fun insertBookmark(item: BookmarkItem) = dao.insertBookmark(item)
    suspend fun updateBookmark(item: BookmarkItem) = dao.updateBookmark(item)
    fun getAllBookmarks(): Flow<List<BookmarkItem>> = dao.getAllBookmarks()
    fun searchBookmarks(query: String): Flow<List<BookmarkItem>> = dao.searchBookmarks(query)
    suspend fun deleteBookmark(item: BookmarkItem) = dao.deleteBookmark(item)
    suspend fun isBookmarked(url: String): Boolean = dao.isBookmarked(url) > 0
}
