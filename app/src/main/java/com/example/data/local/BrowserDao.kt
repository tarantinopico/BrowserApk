package com.example.data.local

import androidx.room.*
import com.example.domain.models.BookmarkItem
import com.example.domain.models.HistoryItem
import com.example.domain.models.BrowserIdentity
import com.example.domain.models.TabGroup
import com.example.domain.models.SavedTab
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdentity(item: BrowserIdentity)
    
    @Query("SELECT * FROM identities_table ORDER BY createdAt ASC")
    fun getAllIdentities(): Flow<List<BrowserIdentity>>
    
    @Delete
    suspend fun deleteIdentity(item: BrowserIdentity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabGroup(item: TabGroup)
    
    @Query("SELECT * FROM tab_groups_table WHERE identityId = :identityId ORDER BY createdAt ASC")
    fun getTabGroupsForIdentity(identityId: String): Flow<List<TabGroup>>
    
    @Delete
    suspend fun deleteTabGroup(item: TabGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedTab(item: SavedTab)
    
    @Query("SELECT * FROM saved_tabs_table WHERE identityId = :identityId ORDER BY position ASC")
    fun getSavedTabsForIdentity(identityId: String): Flow<List<SavedTab>>
    
    @Delete
    suspend fun deleteSavedTab(item: SavedTab)
    
    @Query("DELETE FROM saved_tabs_table WHERE id = :id")
    suspend fun deleteSavedTabById(id: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem)

    @Query("SELECT * FROM history_table WHERE identityId = :identityId ORDER BY visitedAt DESC")
    fun getAllHistory(identityId: String): Flow<List<HistoryItem>>

    @Query("SELECT * FROM history_table WHERE identityId = :identityId AND (title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%') ORDER BY visitedAt DESC")
    fun searchHistory(identityId: String, query: String): Flow<List<HistoryItem>>

    @Delete
    suspend fun deleteHistory(item: HistoryItem)
    
    @Query("DELETE FROM history_table WHERE identityId = :identityId")
    suspend fun clearHistory(identityId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(item: BookmarkItem)
    
    @Update
    suspend fun updateBookmark(item: BookmarkItem)

    @Query("SELECT * FROM bookmarks_table WHERE identityId = :identityId ORDER BY createdAt DESC")
    fun getAllBookmarks(identityId: String): Flow<List<BookmarkItem>>

    @Query("SELECT * FROM bookmarks_table WHERE identityId = :identityId AND (title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchBookmarks(identityId: String, query: String): Flow<List<BookmarkItem>>

    @Delete
    suspend fun deleteBookmark(item: BookmarkItem)
    
    @Query("SELECT COUNT(*) FROM bookmarks_table WHERE url = :url AND identityId = :identityId")
    suspend fun isBookmarked(url: String, identityId: String): Int
}
