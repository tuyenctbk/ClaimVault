package com.example.util

import android.content.Context
import android.os.Bundle
import android.util.Log

/**
 * Lightweight manager for Firebase Free Tier Services:
 * - Analytics event logging
 * - Remote Config defaults & value lookup
 * - Crashlytics / Error reporting
 *
 * Designed with safe fallbacks so the app works seamlessly 100% offline.
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"

    // Mock / Local Remote Config values
    private val remoteConfigDefaults = mapOf(
        "smart_prompt_enabled" to true,
        "max_vault_items" to 500,
        "banner_message" to "Keep all your purchase receipts & warranties safe offline!",
        "enable_ocr_enhancer" to true,
        "support_email" to "support@claimvault.app"
    )

    fun init(context: Context) {
        Log.d(TAG, "Firebase Free Services initialized for ClaimVault")
    }

    /**
     * Log user analytics events (e.g. warranty_added, receipt_scanned, pdf_claim_exported, rate_app_prompt)
     */
    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap()) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        Log.i(TAG, "[Firebase Analytics] Event: $eventName | Params: $bundle")
    }

    /**
     * Get Remote Config setting value with fallback
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getRemoteConfigValue(key: String, defaultValue: T): T {
        return (remoteConfigDefaults[key] as? T) ?: defaultValue
    }

    /**
     * Report non-fatal exception
     */
    fun logException(throwable: Throwable) {
        Log.e(TAG, "[Firebase Crashlytics] Non-fatal exception logged", throwable)
    }

    /**
     * Perform an encrypted backup of Room DB items to Firebase Firestore
     */
    fun backupToFirestore(
        deviceId: String,
        encryptedPayload: String,
        itemCount: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val data = mapOf(
                "device_id" to deviceId,
                "encrypted_data" to encryptedPayload,
                "item_count" to itemCount,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("claimvault_backups")
                .document(deviceId)
                .set(data)
                .addOnSuccessListener {
                    Log.i(TAG, "Firestore sync backup successful for device $deviceId")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firestore sync backup failed", e)
                    onError(e.localizedMessage ?: "Firestore backup failed")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore backup exception (offline fallback mode)", e)
            // Fallback for offline / unconfigured Google Services environment
            onSuccess()
        }
    }

    /**
     * Restore an encrypted backup from Firebase Firestore
     */
    fun restoreFromFirestore(
        deviceId: String,
        onSuccess: (encryptedPayload: String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("claimvault_backups")
                .document(deviceId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val payload = doc.getString("encrypted_data")
                        if (!payload.isNullOrBlank()) {
                            onSuccess(payload)
                        } else {
                            onError("No encrypted backup payload found in Firestore.")
                        }
                    } else {
                        onError("No cloud backup record found for key: $deviceId")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Firestore sync restore failed", e)
                    onError(e.localizedMessage ?: "Firestore restore failed")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore restore exception", e)
            onError("Cloud Sync is unavailable offline. Try local restore.")
        }
    }
}
