package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warranties")
data class WarrantyItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val merchant: String,
    val category: String, // e.g. "Electronics", "Home & Kitchen", "Fashion", "Automotive", "Tools", "Other"
    val price: Double,
    val currency: String = "$",
    val purchaseDateEpoch: Long, // timestamp in ms
    val warrantyDurationMonths: Int, // e.g. 12, 24, 36, -1 for Lifetime
    val expiryDateEpoch: Long, // calculated expiry timestamp
    val serialNumber: String = "",
    val modelNumber: String = "",
    val receiptImagePath: String? = null,
    val notes: String = "",
    val reminder30Days: Boolean = true,
    val reminder7Days: Boolean = true,
    val reminder1Day: Boolean = true,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val tags: String = ""
)
