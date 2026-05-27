package com.example.data.local

import androidx.room.*
import com.example.domain.models.BookmarkItem
import com.example.domain.models.HistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem)

    @Query("SELECT * FROM history_table ORDER BY visitedAt DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Query("SELECT * FROM history_table WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC")
    fun searchHistory(query: String): Flow<List<HistoryItem>>

    @Delete
    suspend fun deleteHistory(item: HistoryItem)
    
    @Query("DELETE FROM history_table")
    suspend fun clearHistory()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(item: BookmarkItem)
    
    @Update
    suspend fun updateBookmark(item: BookmarkItem)

    @Query("SELECT * FROM bookmarks_table ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkItem>>

    @Query("SELECT * FROM bookmarks_table WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchBookmarks(query: String): Flow<List<BookmarkItem>>

    @Delete
    suspend fun deleteBookmark(item: BookmarkItem)
    
    @Query("SELECT COUNT(*) FROM bookmarks_table WHERE url = :url")
    suspend fun isBookmarked(url: String): Int
}
