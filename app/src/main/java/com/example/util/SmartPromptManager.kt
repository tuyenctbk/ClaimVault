package com.example.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SmartPromptManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("claim_vault_smart_prompts", Context.MODE_PRIVATE)

    private val _showRateDialog = MutableStateFlow(false)
    val showRateDialog: StateFlow<Boolean> = _showRateDialog.asStateFlow()

    private val _showShareDialog = MutableStateFlow(false)
    val showShareDialog: StateFlow<Boolean> = _showShareDialog.asStateFlow()

    init {
        incrementLaunchCount()
    }

    private fun incrementLaunchCount() {
        val launches = prefs.getInt("app_launch_count", 0) + 1
        prefs.edit().putInt("app_launch_count", launches).apply()
    }

    fun onWarrantyOrReceiptSaved() {
        val count = prefs.getInt("saved_items_count", 0) + 1
        prefs.edit().putInt("saved_items_count", count).apply()

        FirebaseManager.logEvent("item_saved_count", mapOf("count" to count))

        // Check if smart prompts should trigger
        checkDynamicTriggers()
    }

    fun onOcrSuccess() {
        val count = prefs.getInt("ocr_success_count", 0) + 1
        prefs.edit().putInt("ocr_success_count", count).apply()
        checkDynamicTriggers()
    }

    fun onPdfExportSuccess() {
        val count = prefs.getInt("pdf_export_count", 0) + 1
        prefs.edit().putInt("pdf_export_count", count).apply()
        checkDynamicTriggers()
    }

    private fun checkDynamicTriggers() {
        if (shouldAutoShowRating()) {
            _showRateDialog.value = true
        } else if (shouldAutoShowShare()) {
            _showShareDialog.value = true
        }
    }

    private fun getSatisfactionScore(): Int {
        val savedItems = prefs.getInt("saved_items_count", 0)
        val ocrSuccess = prefs.getInt("ocr_success_count", 0)
        val pdfExports = prefs.getInt("pdf_export_count", 0)

        // Calculate score: 1pt per save, 2pt per OCR, 5pt per PDF export
        return (savedItems * 1) + (ocrSuccess * 2) + (pdfExports * 5)
    }

    fun shouldAutoShowRating(): Boolean {
        val hasRated = prefs.getBoolean("has_rated_app", false)
        val neverShow = prefs.getBoolean("never_show_rating", false)
        val launches = prefs.getInt("app_launch_count", 0)
        val score = getSatisfactionScore()

        // Trigger when satisfaction score is high (>= 10) and user has explored the app (>= 3 launches)
        return !hasRated && !neverShow && score >= 10 && launches >= 3
    }

    fun shouldAutoShowShare(): Boolean {
        val hasShared = prefs.getBoolean("has_shared_app", false)
        val score = getSatisfactionScore()
        // Trigger share suggestion at higher satisfaction
        return !hasShared && score >= 15
    }

    fun triggerRateDialog() {
        _showRateDialog.value = true
    }

    fun dismissRateDialog(neverShowAgain: Boolean = false) {
        _showRateDialog.value = false
        if (neverShowAgain) {
            prefs.edit().putBoolean("never_show_rating", true).apply()
        }
    }

    fun markRated() {
        prefs.edit().putBoolean("has_rated_app", true).apply()
        _showRateDialog.value = false
        FirebaseManager.logEvent("rate_app_5_stars_completed")
    }

    fun triggerShareDialog() {
        _showShareDialog.value = true
    }

    fun dismissShareDialog() {
        _showShareDialog.value = false
    }

    fun markShared() {
        prefs.edit().putBoolean("has_shared_app", true).apply()
        _showShareDialog.value = false
        FirebaseManager.logEvent("share_app_completed")
    }

    fun shareApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "ClaimVault - Warranty & Receipt Manager")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out ClaimVault! It's a 100% offline, privacy-first Android app to organize product warranties, store receipts, and export PDF claim packages easily."
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ClaimVault via").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            markShared()
        } catch (e: Exception) {
            FirebaseManager.logException(e)
        }
    }
}
