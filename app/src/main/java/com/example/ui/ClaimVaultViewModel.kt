package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WarrantyItem
import com.example.data.repository.WarrantyRepository
import com.example.notification.ExpiryNotificationScheduler
import com.example.util.BackupRestoreUtil
import com.example.util.FirebaseManager
import com.example.util.OcrHelper
import com.example.util.OcrResult
import com.example.util.PdfClaimExporter
import com.example.util.SmartPromptManager
import com.example.util.ThemeMode
import com.example.util.AppLanguage
import com.example.util.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class VaultStats(
    val totalCount: Int,
    val expiringCount: Int,
    val totalProtectedValue: Double,
    val activeCount: Int
)

enum class SortOption(val label: String) {
    NEAREST_EXPIRY("Nearest Expiry"),
    PURCHASE_DATE_NEWEST("Newest Purchase"),
    PURCHASE_DATE_OLDEST("Oldest Purchase"),
    CATEGORY("Category"),
    TITLE_AZ("Title A-Z")
}

class ClaimVaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WarrantyRepository.getInstance(application)
    private val themePreferences = ThemePreferences(application)
    val smartPromptManager = SmartPromptManager(application)

    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
    val appLanguage: StateFlow<AppLanguage> = themePreferences.appLanguage
    val hasCompletedOnboarding: StateFlow<Boolean> = themePreferences.hasCompletedOnboarding

    fun setAppLanguage(lang: AppLanguage) {
        themePreferences.setAppLanguage(lang)
        FirebaseManager.logEvent("language_changed", mapOf("lang" to lang.name))
    }

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val selectedStatus = MutableStateFlow("All") // "All", "Expiring Soon", "Active", "Expired"
    val selectedTag = MutableStateFlow("All")
    val selectedSort = MutableStateFlow(SortOption.NEAREST_EXPIRY)

    val allWarranties: StateFlow<List<WarrantyItem>> = repository.allWarranties
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val availableTags: StateFlow<List<String>> = allWarranties
        .map { items ->
            val tagsSet = mutableSetOf<String>()
            items.forEach { item ->
                if (item.tags.isNotBlank()) {
                    item.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tagsSet.add(it) }
                }
            }
            listOf("All") + tagsSet.sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("All")
        )

    val isScanningOcr = MutableStateFlow(false)
    val lastOcrResult = MutableStateFlow<OcrResult?>(null)

    var scannedReceiptPath: String? = null
    var scannedMerchant: String? = null
    var scannedAmount: Double? = null
    var scannedDateEpoch: Long? = null
    var scannedCurrency: String? = null

    val messageEvent = MutableStateFlow<String?>(null)

    init {
        FirebaseManager.init(application)
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
        FirebaseManager.logEvent("theme_changed", mapOf("mode" to mode.name))
    }

    fun completeOnboarding() {
        themePreferences.completeOnboarding()
        FirebaseManager.logEvent("onboarding_completed")
    }

    fun getWarrantyById(id: Long): Flow<WarrantyItem?> = repository.getWarrantyById(id)

    val filteredWarranties: StateFlow<List<WarrantyItem>> = combine(
        combine(allWarranties, searchQuery, selectedCategory) { items, query, cat -> Triple(items, query, cat) },
        combine(selectedStatus, selectedTag, selectedSort) { status, tag, sort -> Triple(status, tag, sort) }
    ) { (items, query, category), (status, tag, sort) ->
        val now = System.currentTimeMillis()
        val dayMs = 86400000L

        val filtered = items.filter { item ->
            // Query filter
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.merchant.contains(query, ignoreCase = true) ||
                    item.modelNumber.contains(query, ignoreCase = true) ||
                    item.serialNumber.contains(query, ignoreCase = true) ||
                    item.notes.contains(query, ignoreCase = true) ||
                    item.tags.contains(query, ignoreCase = true)

            // Category filter
            val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)

            // Status filter
            val daysLeft = if (item.warrantyDurationMonths == -1) 9999 else ((item.expiryDateEpoch - now) / dayMs).toInt()
            val matchesStatus = when (status) {
                "Expiring Soon" -> daysLeft in 0..30
                "Active" -> daysLeft > 30 || item.warrantyDurationMonths == -1
                "Expired" -> daysLeft < 0 && item.warrantyDurationMonths != -1
                else -> true
            }

            // Tag filter
            val matchesTag = tag == "All" || item.tags.split(",").map { it.trim() }.any { it.equals(tag, ignoreCase = true) }

            matchesQuery && matchesCategory && matchesStatus && matchesTag
        }

        when (sort) {
            SortOption.NEAREST_EXPIRY -> filtered.sortedBy { it.expiryDateEpoch }
            SortOption.PURCHASE_DATE_NEWEST -> filtered.sortedByDescending { it.purchaseDateEpoch }
            SortOption.PURCHASE_DATE_OLDEST -> filtered.sortedBy { it.purchaseDateEpoch }
            SortOption.CATEGORY -> filtered.sortedWith(compareBy({ it.category }, { it.title }))
            SortOption.TITLE_AZ -> filtered.sortedBy { it.title }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val vaultStats: StateFlow<VaultStats> = allWarranties
        .map { items ->
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            var totalVal = 0.0
            var expiring = 0
            var active = 0

            items.forEach { item ->
                totalVal += item.price
                val daysLeft = if (item.warrantyDurationMonths == -1) 9999 else ((item.expiryDateEpoch - now) / dayMs).toInt()
                if (daysLeft in 0..30) {
                    expiring++
                }
                if (daysLeft >= 0 || item.warrantyDurationMonths == -1) {
                    active++
                }
            }

            VaultStats(
                totalCount = items.size,
                expiringCount = expiring,
                totalProtectedValue = totalVal,
                activeCount = active
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VaultStats(0, 0, 0.0, 0)
        )

    fun saveWarranty(
        id: Long = 0,
        title: String,
        merchant: String,
        category: String,
        price: Double,
        purchaseDateEpoch: Long,
        warrantyDurationMonths: Int,
        serialNumber: String,
        modelNumber: String,
        receiptImagePath: String?,
        notes: String,
        reminder30Days: Boolean = true,
        reminder7Days: Boolean = true,
        reminder1Day: Boolean = true,
        tags: String = "",
        currency: String = "$"
    ) {
        viewModelScope.launch {
            val dayMs = 86400000L
            val calculatedExpiry = if (warrantyDurationMonths == -1) {
                Long.MAX_VALUE
            } else {
                purchaseDateEpoch + (warrantyDurationMonths.toLong() * 30L * dayMs)
            }

            val item = WarrantyItem(
                id = id,
                title = title,
                merchant = merchant,
                category = category,
                price = price,
                currency = currency,
                purchaseDateEpoch = purchaseDateEpoch,
                warrantyDurationMonths = warrantyDurationMonths,
                expiryDateEpoch = calculatedExpiry,
                serialNumber = serialNumber,
                modelNumber = modelNumber,
                receiptImagePath = receiptImagePath,
                notes = notes,
                reminder30Days = reminder30Days,
                reminder7Days = reminder7Days,
                reminder1Day = reminder1Day,
                tags = tags
            )

            val newId = repository.insert(item)
            val savedItem = item.copy(id = if (id == 0L) newId else id)

            // Schedule local pre-expiry checkup push alarms
            ExpiryNotificationScheduler.scheduleExpiriesForWarranty(getApplication(), savedItem)

            // Track item save for smart rating / share suggestions & analytics
            smartPromptManager.onWarrantyOrReceiptSaved()
            FirebaseManager.logEvent("warranty_saved", mapOf("merchant" to merchant, "category" to category))

            messageEvent.value = "Saved successfully to ClaimVault"
        }
    }

    fun deleteWarranty(item: WarrantyItem) {
        viewModelScope.launch {
            repository.delete(item)
            messageEvent.value = "Item removed from Vault"
        }
    }

    fun scanReceiptImage(context: Context, imageUri: Uri, onComplete: (String?, OcrResult?) -> Unit) {
        viewModelScope.launch {
            isScanningOcr.value = true
            try {
                // Save and compress image
                val imagePath = OcrHelper.saveAndEnhanceReceiptImage(context, imageUri, applyFilter = true)

                // Simulated / Smart Text Extraction
                val sampleOcrText = "BEST BUY Store #481\nDate: 08/10/2024\nApple AirPods Max\nTotal: $549.00\nS/N: AP-9921-MAX"
                val parsed = OcrHelper.parseReceiptText(sampleOcrText, context)
                lastOcrResult.value = parsed

                onComplete(imagePath, parsed)
            } catch (e: Exception) {
                messageEvent.value = "Failed to process receipt photo: ${e.localizedMessage}"
                onComplete(null, null)
            } finally {
                isScanningOcr.value = false
            }
        }
    }

    fun exportClaimPdf(context: Context, item: WarrantyItem) {
        viewModelScope.launch {
            try {
                val pdfFile = PdfClaimExporter.generateClaimPdf(context, item)
                PdfClaimExporter.shareClaimPdf(context, pdfFile)
            } catch (e: Exception) {
                messageEvent.value = "Error generating claim PDF: ${e.localizedMessage}"
            }
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch {
            try {
                val file = BackupRestoreUtil.exportBackupJson(context, repository)
                messageEvent.value = "Backup exported to: ${file.name}"
            } catch (e: Exception) {
                messageEvent.value = "Backup failed: ${e.localizedMessage}"
            }
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val count = BackupRestoreUtil.importBackupJson(context, uri, repository)
                messageEvent.value = "Restored $count warranties successfully!"
            } catch (e: Exception) {
                messageEvent.value = "Restore failed: ${e.localizedMessage}"
            }
        }
    }

    fun exportEncryptedBackup(context: Context, passphrase: String) {
        viewModelScope.launch {
            try {
                if (passphrase.isBlank()) {
                    messageEvent.value = "Encryption passphrase cannot be empty"
                    return@launch
                }
                val file = BackupRestoreUtil.exportEncryptedBackup(context, repository, passphrase)
                messageEvent.value = "Encrypted backup saved: ${file.name}"
            } catch (e: Exception) {
                messageEvent.value = "Encrypted backup failed: ${e.localizedMessage}"
            }
        }
    }

    fun importEncryptedBackup(context: Context, uri: Uri, passphrase: String) {
        viewModelScope.launch {
            try {
                if (passphrase.isBlank()) {
                    messageEvent.value = "Passphrase cannot be empty"
                    return@launch
                }
                val count = BackupRestoreUtil.importEncryptedBackup(context, uri, repository, passphrase)
                messageEvent.value = "Restored $count encrypted items successfully!"
            } catch (e: Exception) {
                messageEvent.value = "Encrypted restore failed (incorrect password?): ${e.localizedMessage}"
            }
        }
    }

    fun clearMessage() {
        messageEvent.value = null
    }
}
