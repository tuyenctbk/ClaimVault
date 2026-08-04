package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.Receipt
import com.example.data.local.ReceiptDao
import com.example.di.DatabaseModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReceiptViewModel(
    application: Application,
    private val receiptDao: ReceiptDao = DatabaseModule.provideReceiptDao(application)
) : AndroidViewModel(application) {

    val searchQuery = MutableStateFlow("")

    val allReceipts: StateFlow<List<Receipt>> = receiptDao.getAllReceipts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredReceipts: StateFlow<List<Receipt>> = combine(allReceipts, searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { receipt ->
                receipt.storeName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getReceiptById(id: Long): Flow<Receipt?> = receiptDao.getReceiptById(id)

    fun getReceiptsByExpiryDate(maxExpiryDate: Long): Flow<List<Receipt>> =
        receiptDao.getReceiptsByExpiryDate(maxExpiryDate)

    fun saveReceipt(
        id: Long = 0,
        storeName: String,
        date: Long,
        totalAmount: Double,
        imagePath: String? = null,
        warrantyExpiryDate: Long
    ) {
        viewModelScope.launch {
            val receipt = Receipt(
                id = id,
                storeName = storeName,
                date = date,
                totalAmount = totalAmount,
                imagePath = imagePath,
                warrantyExpiryDate = warrantyExpiryDate
            )
            if (id == 0L) {
                receiptDao.insertReceipt(receipt)
            } else {
                receiptDao.updateReceipt(receipt)
            }
        }
    }

    fun addReceipt(receipt: Receipt) {
        viewModelScope.launch {
            receiptDao.insertReceipt(receipt)
        }
    }

    fun updateReceipt(receipt: Receipt) {
        viewModelScope.launch {
            receiptDao.updateReceipt(receipt)
        }
    }

    fun deleteReceipt(receipt: Receipt) {
        viewModelScope.launch {
            receiptDao.deleteReceipt(receipt)
        }
    }

    fun deleteReceiptById(id: Long) {
        viewModelScope.launch {
            receiptDao.deleteReceiptById(id)
        }
    }
}
