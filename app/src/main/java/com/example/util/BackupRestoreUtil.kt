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

    private fun encryptData(data: ByteArray, passphrase: String): ByteArray {
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

    private fun decryptData(encryptedData: ByteArray, passphrase: String): ByteArray {
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

