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

        // Check if smart rate dialog should trigger automatically (e.g. at 2nd saved item)
        if (shouldAutoShowRating()) {
            _showRateDialog.value = true
        } else if (shouldAutoShowShare() && count == 3) {
            _showShareDialog.value = true
        }
    }

    fun shouldAutoShowRating(): Boolean {
        val hasRated = prefs.getBoolean("has_rated_app", false)
        val neverShow = prefs.getBoolean("never_show_rating", false)
        val count = prefs.getInt("saved_items_count", 0)
        val launches = prefs.getInt("app_launch_count", 0)

        return !hasRated && !neverShow && (count >= 2 || launches >= 4)
    }

    fun shouldAutoShowShare(): Boolean {
        val hasShared = prefs.getBoolean("has_shared_app", false)
        val count = prefs.getInt("saved_items_count", 0)
        return !hasShared && count >= 3
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
