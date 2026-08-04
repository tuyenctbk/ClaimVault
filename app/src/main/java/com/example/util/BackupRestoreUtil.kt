package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.WarrantyItem
import com.example.data.repository.WarrantyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object BackupRestoreUtil {

    suspend fun exportBackupJson(context: Context, repository: WarrantyRepository): File = withContext(Dispatchers.IO) {
        val items = repository.allWarranties.first()
        val jsonArray = JSONArray()

        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("merchant", item.merchant)
                put("category", item.category)
                put("price", item.price)
                put("currency", item.currency)
                put("purchaseDateEpoch", item.purchaseDateEpoch)
                put("warrantyDurationMonths", item.warrantyDurationMonths)
                put("expiryDateEpoch", item.expiryDateEpoch)
                put("serialNumber", item.serialNumber)
                put("modelNumber", item.modelNumber)
                put("notes", item.notes)
                put("reminder30Days", item.reminder30Days)
                put("reminder7Days", item.reminder7Days)
                put("reminder1Day", item.reminder1Day)
            }
            jsonArray.put(obj)
        }

        val backupsDir = File(context.cacheDir, "backups")
        if (!backupsDir.exists()) backupsDir.mkdirs()

        val backupFile = File(backupsDir, "ClaimVault_Backup_${System.currentTimeMillis()}.json")
        FileOutputStream(backupFile).use { out ->
            out.write(jsonArray.toString(2).toByteArray(Charsets.UTF_8))
        }

        backupFile
    }

    suspend fun importBackupJson(context: Context, uri: Uri, repository: WarrantyRepository): Int = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val jsonStr = inputStream?.bufferedReader()?.use { it.readText() } ?: return@withContext 0

        val jsonArray = JSONArray(jsonStr)
        var importedCount = 0

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val item = WarrantyItem(
                title = obj.optString("title", "Imported Item"),
                merchant = obj.optString("merchant", "Unknown Store"),
                category = obj.optString("category", "Electronics"),
                price = obj.optDouble("price", 0.0),
                currency = obj.optString("currency", "$"),
                purchaseDateEpoch = obj.optLong("purchaseDateEpoch", System.currentTimeMillis()),
                warrantyDurationMonths = obj.optInt("warrantyDurationMonths", 12),
                expiryDateEpoch = obj.optLong("expiryDateEpoch", System.currentTimeMillis() + 31536000000L),
                serialNumber = obj.optString("serialNumber", ""),
                modelNumber = obj.optString("modelNumber", ""),
                notes = obj.optString("notes", ""),
                reminder30Days = obj.optBoolean("reminder30Days", true),
                reminder7Days = obj.optBoolean("reminder7Days", true),
                reminder1Day = obj.optBoolean("reminder1Day", true)
            )
            repository.insert(item)
            importedCount++
        }

        importedCount
    }
}
