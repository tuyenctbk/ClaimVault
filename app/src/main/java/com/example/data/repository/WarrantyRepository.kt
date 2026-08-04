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
        val currentList = warrantyDao.getAllWarranties().first()
        if (currentList.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            val sampleItems = listOf(
                WarrantyItem(
                    title = "Sony WH-1000XM5",
                    merchant = "Sony Direct / Amazon",
                    category = "Electronics",
                    price = 399.99,
                    currency = "$",
                    purchaseDateEpoch = now - (353L * dayMs),
                    warrantyDurationMonths = 12,
                    expiryDateEpoch = now + (12L * dayMs), // Expires in 12 days!
                    serialNumber = "SN-982140-XM5",
                    modelNumber = "WH1000XM5/B",
                    notes = "Noise canceling headphones. Right ear cup slider replaced under warranty checkup.",
                    reminder30Days = true,
                    reminder7Days = true,
                    reminder1Day = true
                ),
                WarrantyItem(
                    title = "iPhone 15 Pro",
                    merchant = "Apple Store",
                    category = "Electronics",
                    price = 999.00,
                    currency = "$",
                    purchaseDateEpoch = now - (120L * dayMs),
                    warrantyDurationMonths = 12,
                    expiryDateEpoch = now + (245L * dayMs),
                    serialNumber = "F2LX7892K9",
                    modelNumber = "A2848",
                    notes = "AppleCare+ active until expiry. Includes liquid damage protection.",
                    reminder30Days = true,
                    reminder7Days = true,
                    reminder1Day = true
                ),
                WarrantyItem(
                    title = "Dyson V15 Vacuum",
                    merchant = "Dyson Store",
                    category = "Home & Kitchen",
                    price = 749.99,
                    currency = "$",
                    purchaseDateEpoch = now - (600L * dayMs),
                    warrantyDurationMonths = 24,
                    expiryDateEpoch = now + (30L * dayMs), // Expires in 30 days!
                    serialNumber = "DY-882-9910",
                    modelNumber = "V15-DETECT",
                    notes = "Laser Fluffy head & HEPA filter included.",
                    reminder30Days = true,
                    reminder7Days = true,
                    reminder1Day = true
                ),
                WarrantyItem(
                    title = "LG OLED 65\" TV",
                    merchant = "Best Buy",
                    category = "Electronics",
                    price = 1799.00,
                    currency = "$",
                    purchaseDateEpoch = now - (180L * dayMs),
                    warrantyDurationMonths = 24,
                    expiryDateEpoch = now + (550L * dayMs),
                    serialNumber = "402RMBY12093",
                    modelNumber = "OLED65C3PUA",
                    notes = "Includes Geek Squad 2-Year Panel Protection.",
                    reminder30Days = true,
                    reminder7Days = true,
                    reminder1Day = true
                ),
                WarrantyItem(
                    title = "Breville Espresso Machine",
                    merchant = "Bed Bath & Beyond",
                    category = "Home & Kitchen",
                    price = 699.95,
                    currency = "$",
                    purchaseDateEpoch = now - (90L * dayMs),
                    warrantyDurationMonths = 24,
                    expiryDateEpoch = now + (640L * dayMs),
                    serialNumber = "BR-99012-BES",
                    modelNumber = "BES870XL",
                    notes = "Descaling instructions saved. Keep receipt for group head seal warranty.",
                    reminder30Days = true,
                    reminder7Days = true,
                    reminder1Day = true
                ),
                WarrantyItem(
                    title = "DeWalt Cordless Drill Kit",
                    merchant = "Home Depot",
                    category = "Tools",
                    price = 199.00,
                    currency = "$",
                    purchaseDateEpoch = now - (300L * dayMs),
                    warrantyDurationMonths = 36,
                    expiryDateEpoch = now + (795L * dayMs),
                    serialNumber = "DW-2023-7712",
                    modelNumber = "DCD771C2",
                    notes = "3-Year Limited Warranty with 1-Year Free Service.",
                    reminder30Days = true,
                    reminder7Days = true,
                    reminder1Day = true
                )
            )

            sampleItems.forEach { item ->
                warrantyDao.insertWarranty(item)
            }
        }
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
