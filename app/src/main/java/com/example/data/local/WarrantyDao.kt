package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WarrantyDao {
    @Query("SELECT * FROM warranties ORDER BY expiryDateEpoch ASC")
    fun getAllWarranties(): Flow<List<WarrantyItem>>

    @Query("SELECT * FROM warranties WHERE id = :id")
    fun getWarrantyById(id: Long): Flow<WarrantyItem?>

    @Query("SELECT * FROM warranties WHERE expiryDateEpoch <= :thresholdEpoch AND expiryDateEpoch >= :currentEpoch ORDER BY expiryDateEpoch ASC")
    fun getExpiringSoonWarranties(currentEpoch: Long, thresholdEpoch: Long): Flow<List<WarrantyItem>>

    @Query("SELECT * FROM warranties WHERE expiryDateEpoch >= :startDateEpoch AND expiryDateEpoch <= :endDateEpoch ORDER BY expiryDateEpoch ASC")
    fun getWarrantiesByExpiryDateRange(startDateEpoch: Long, endDateEpoch: Long): Flow<List<WarrantyItem>>

    @Query("SELECT * FROM warranties WHERE expiryDateEpoch <= :maxExpiryEpoch ORDER BY expiryDateEpoch ASC")
    fun getWarrantiesExpiringBefore(maxExpiryEpoch: Long): Flow<List<WarrantyItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarranty(item: WarrantyItem): Long

    @Update
    suspend fun updateWarranty(item: WarrantyItem)

    @Delete
    suspend fun deleteWarranty(item: WarrantyItem)

    @Query("DELETE FROM warranties WHERE id = :id")
    suspend fun deleteWarrantyById(id: Long)

    @Query("DELETE FROM warranties")
    suspend fun deleteAll()
}
