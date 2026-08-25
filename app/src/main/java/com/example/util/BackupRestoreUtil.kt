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
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupRestoreUtil {

    fun encryptData(data: ByteArray, passphrase: String): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val ciphertext = cipher.doFinal(data)

        val result = ByteArray(16 + 12 + ciphertext.size)
        System.arraycopy(salt, 0, result, 0, 16)
        System.arraycopy(iv, 0, result, 16, 12)
        System.arraycopy(ciphertext, 0, result, 28, ciphertext.size)
        return result
    }

    fun decryptData(encryptedData: ByteArray, passphrase: String): ByteArray {
        val salt = ByteArray(16)
        val iv = ByteArray(12)
        val ciphertext = ByteArray(encryptedData.size - 28)

        System.arraycopy(encryptedData, 0, salt, 0, 16)
        System.arraycopy(encryptedData, 16, iv, 0, 12)
        System.arraycopy(encryptedData, 28, ciphertext, 0, ciphertext.size)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(ciphertext)
    }

    fun serializeItemsToJson(items: List<WarrantyItem>): String {
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
                put("tags", item.tags)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    fun deserializeItemsFromJson(jsonStr: String): List<WarrantyItem> {
        val jsonArray = JSONArray(jsonStr)
        val list = mutableListOf<WarrantyItem>()
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
                reminder1Day = obj.optBoolean("reminder1Day", true),
                tags = obj.optString("tags", "")
            )
            list.add(item)
        }
        return list
    }

    suspend fun deserializeItemsFromJson(jsonStr: String, repository: WarrantyRepository): Int = withContext(Dispatchers.IO) {
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
                reminder1Day = obj.optBoolean("reminder1Day", true),
                tags = obj.optString("tags", "")
            )
            repository.insert(item)
            importedCount++
        }
        importedCount
    }

    suspend fun exportEncryptedBackup(context: Context, repository: WarrantyRepository, passphrase: String): File = withContext(Dispatchers.IO) {
        val jsonFile = exportBackupJson(context, repository)
        val jsonBytes = jsonFile.readBytes()
        val encryptedBytes = encryptData(jsonBytes, passphrase)

        val backupsDir = File(context.cacheDir, "backups")
        if (!backupsDir.exists()) backupsDir.mkdirs()

        val encryptedFile = File(backupsDir, "ClaimVault_Encrypted_Backup_${System.currentTimeMillis()}.enc")
        FileOutputStream(encryptedFile).use { out ->
            out.write(encryptedBytes)
        }
        jsonFile.delete()
        encryptedFile
    }

    suspend fun importEncryptedBackup(context: Context, uri: Uri, repository: WarrantyRepository, passphrase: String): Int = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val encryptedBytes = inputStream?.readBytes() ?: return@withContext 0

        val decryptedBytes = decryptData(encryptedBytes, passphrase)
        val jsonStr = String(decryptedBytes, Charsets.UTF_8)

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
                reminder1Day = obj.optBoolean("reminder1Day", true),
                tags = obj.optString("tags", "")
            )
            repository.insert(item)
            importedCount++
        }

        importedCount
    }

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
                put("tags", item.tags)
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

    suspend fun exportBackupCsv(context: Context, repository: WarrantyRepository): File = withContext(Dispatchers.IO) {
        val items = repository.allWarranties.first()
        val sb = StringBuilder()
        // CSV Header
        sb.append("ID,Title,Merchant,Category,Price,Currency,PurchaseDateEpoch,WarrantyDurationMonths,ExpiryDateEpoch,SerialNumber,ModelNumber,Notes,Reminder30Days,Reminder7Days,Reminder1Day,Tags\n")

        for (item in items) {
            sb.append(item.id).append(",")
            sb.append(escapeCsv(item.title)).append(",")
            sb.append(escapeCsv(item.merchant)).append(",")
            sb.append(escapeCsv(item.category)).append(",")
            sb.append(item.price).append(",")
            sb.append(escapeCsv(item.currency)).append(",")
            sb.append(item.purchaseDateEpoch).append(",")
            sb.append(item.warrantyDurationMonths).append(",")
            sb.append(item.expiryDateEpoch).append(",")
            sb.append(escapeCsv(item.serialNumber)).append(",")
            sb.append(escapeCsv(item.modelNumber)).append(",")
            sb.append(escapeCsv(item.notes)).append(",")
            sb.append(item.reminder30Days).append(",")
            sb.append(item.reminder7Days).append(",")
            sb.append(item.reminder1Day).append(",")
            sb.append(escapeCsv(item.tags))
            sb.append("\n")
        }

        val backupsDir = File(context.cacheDir, "backups")
        if (!backupsDir.exists()) backupsDir.mkdirs()

        val csvFile = File(backupsDir, "ClaimVault_Receipts_${System.currentTimeMillis()}.csv")
        FileOutputStream(csvFile).use { out ->
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        }

        csvFile
    }

    suspend fun importBackupCsv(context: Context, uri: Uri, repository: WarrantyRepository): Int = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val lines = inputStream?.bufferedReader()?.readLines() ?: return@withContext 0

        if (lines.isEmpty()) return@withContext 0

        var importedCount = 0
        // Skip header line
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue

            val tokens = parseCsvLine(line)
            if (tokens.size >= 6) {
                val title = tokens.getOrNull(1) ?: "Imported Item"
                val merchant = tokens.getOrNull(2) ?: "Unknown Store"
                val category = tokens.getOrNull(3) ?: "Electronics"
                val price = tokens.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                val currency = tokens.getOrNull(5) ?: "$"
                val purchaseDate = tokens.getOrNull(6)?.toLongOrNull() ?: System.currentTimeMillis()
                val duration = tokens.getOrNull(7)?.toIntOrNull() ?: 12
                val expiryDate = tokens.getOrNull(8)?.toLongOrNull() ?: (System.currentTimeMillis() + 31536000000L)
                val serial = tokens.getOrNull(9) ?: ""
                val model = tokens.getOrNull(10) ?: ""
                val notes = tokens.getOrNull(11) ?: ""
                val rem30 = tokens.getOrNull(12)?.toBooleanStrictOrNull() ?: true
                val rem7 = tokens.getOrNull(13)?.toBooleanStrictOrNull() ?: true
                val rem1 = tokens.getOrNull(14)?.toBooleanStrictOrNull() ?: true
                val tags = tokens.getOrNull(15) ?: ""

                val item = WarrantyItem(
                    title = title,
                    merchant = merchant,
                    category = category,
                    price = price,
                    currency = currency,
                    purchaseDateEpoch = purchaseDate,
                    warrantyDurationMonths = duration,
                    expiryDateEpoch = expiryDate,
                    serialNumber = serial,
                    modelNumber = model,
                    notes = notes,
                    reminder30Days = rem30,
                    reminder7Days = rem7,
                    reminder1Day = rem1,
                    tags = tags
                )
                repository.insert(item)
                importedCount++
            }
        }

        importedCount
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(c)
                }
            } else {
                if (c == '\"') {
                    inQuotes = true
                } else if (c == ',') {
                    result.add(curVal.toString())
                    curVal = StringBuilder()
                } else {
                    curVal.append(c)
                }
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }

    fun shareExportedFile(context: Context, file: File, mimeType: String, subject: String) {
        val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
            putExtra(android.content.Intent.EXTRA_TEXT, "Exported ClaimVault receipt & warranty backup file.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = android.content.Intent.createChooser(intent, "Share / Save Export File")
        context.startActivity(chooser)
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
                reminder1Day = obj.optBoolean("reminder1Day", true),
                tags = obj.optString("tags", "")
            )
            repository.insert(item)
            importedCount++
        }

        importedCount
    }
}

