package com.example.data.repository

import com.example.data.local.BrowserDao
import com.example.domain.models.BrowserIdentity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class IdentityRepository(private val dao: BrowserDao) {
    fun getAllIdentities(): Flow<List<BrowserIdentity>> = dao.getAllIdentities()
    
    suspend fun insertIdentity(identity: BrowserIdentity) = dao.insertIdentity(identity)
    
    suspend fun deleteIdentity(identity: BrowserIdentity) = dao.deleteIdentity(identity)

    suspend fun initializeDefaultIdentityIfNeeded() {
        val identities = dao.getAllIdentities().firstOrNull()
        if (identities.isNullOrEmpty()) {
            insertIdentity(
                BrowserIdentity(
                    id = "default",
                    name = "Personal",
                    colorAccent = 0xFF4285F4,
                    isIncognito = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}
