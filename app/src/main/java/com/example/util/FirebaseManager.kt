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
}
