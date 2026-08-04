package com.example.data.repository

import android.content.Context
import com.example.data.local.ClaimVaultDatabase
import com.example.data.local.WarrantyDao
import com.example.data.local.WarrantyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

class WarrantyRepository(private val warrantyDao: WarrantyDao) {

    val allWarranties: Flow<List<WarrantyItem>> = warrantyDao.getAllWarranties()

    fun getWarrantyById(id: Long): Flow<WarrantyItem?> = warrantyDao.getWarrantyById(id)

    suspend fun insert(item: WarrantyItem): Long = withContext(Dispatchers.IO) {
        warrantyDao.insertWarranty(item)
    }

    suspend fun update(item: WarrantyItem) = withContext(Dispatchers.IO) {
        warrantyDao.updateWarranty(item)
    }

    suspend fun delete(item: WarrantyItem) = withContext(Dispatchers.IO) {
        warrantyDao.deleteWarranty(item)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        warrantyDao.deleteWarrantyById(id)
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        // No-op: Sample data removed for production.
    }

    companion object {
        @Volatile
        private var INSTANCE: WarrantyRepository? = null

        fun getInstance(context: Context): WarrantyRepository {
            return INSTANCE ?: synchronized(this) {
                val db = ClaimVaultDatabase.getDatabase(context)
                val instance = WarrantyRepository(db.warrantyDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
