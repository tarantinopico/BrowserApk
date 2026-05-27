package com.example.data.repository

import com.example.data.local.BrowserDao
import com.example.domain.models.TabGroup
import com.example.domain.models.SavedTab
import kotlinx.coroutines.flow.Flow

class TabGroupRepository(private val dao: BrowserDao) {
    fun getTabGroupsForIdentity(identityId: String): Flow<List<TabGroup>> = dao.getTabGroupsForIdentity(identityId)
    suspend fun insertTabGroup(group: TabGroup) = dao.insertTabGroup(group)
    suspend fun deleteTabGroup(group: TabGroup) = dao.deleteTabGroup(group)
    
    fun getSavedTabsForIdentity(identityId: String): Flow<List<SavedTab>> = dao.getSavedTabsForIdentity(identityId)
    suspend fun insertSavedTab(tab: SavedTab) = dao.insertSavedTab(tab)
    suspend fun deleteSavedTab(tab: SavedTab) = dao.deleteSavedTab(tab)
    suspend fun deleteSavedTabById(id: String) = dao.deleteSavedTabById(id)
}
