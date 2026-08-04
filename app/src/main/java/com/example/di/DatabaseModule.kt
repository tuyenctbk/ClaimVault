package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ReceiptDao
import com.example.data.local.WarrantyDao

/**
 * Singleton provider module for AppDatabase and DAOs.
 */
object DatabaseModule {
    @Volatile
    private var database: AppDatabase? = null

    fun provideDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: AppDatabase.getDatabase(context).also { database = it }
        }
    }

    fun provideWarrantyDao(context: Context): WarrantyDao {
        return provideDatabase(context).warrantyDao()
    }

    fun provideReceiptDao(context: Context): ReceiptDao {
        return provideDatabase(context).receiptDao()
    }
}
