package com.example.data.repository

import com.example.data.local.BrowserDao
import com.example.domain.models.BookmarkItem
import com.example.domain.models.HistoryItem
import kotlinx.coroutines.flow.Flow
class BrowserRepository(
    private val dao: BrowserDao
) {
    suspend fun insertHistory(item: HistoryItem) = dao.insertHistory(item)
    fun getAllHistory(identityId: String): Flow<List<HistoryItem>> = dao.getAllHistory(identityId)
    fun searchHistory(identityId: String, query: String): Flow<List<HistoryItem>> = dao.searchHistory(identityId, query)
    suspend fun deleteHistory(item: HistoryItem) = dao.deleteHistory(item)
    suspend fun clearHistory(identityId: String) = dao.clearHistory(identityId)

    suspend fun insertBookmark(item: BookmarkItem) = dao.insertBookmark(item)
    suspend fun updateBookmark(item: BookmarkItem) = dao.updateBookmark(item)
    fun getAllBookmarks(identityId: String): Flow<List<BookmarkItem>> = dao.getAllBookmarks(identityId)
    fun searchBookmarks(identityId: String, query: String): Flow<List<BookmarkItem>> = dao.searchBookmarks(identityId, query)
    suspend fun deleteBookmark(item: BookmarkItem) = dao.deleteBookmark(item)
    suspend fun isBookmarked(url: String, identityId: String): Boolean = dao.isBookmarked(url, identityId) > 0
}
