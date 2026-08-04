package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storeName: String,
    val date: Long,
    val totalAmount: Double,
    val imagePath: String? = null,
    val warrantyExpiryDate: Long,
    val tags: String = ""
)
