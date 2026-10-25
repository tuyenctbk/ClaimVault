package com.example.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    var latestVersionName: String = "1.5.0"
        private set
    var latestVersionHighlights: String = "UPC Barcode Lookup, Historical Exchange Rates & Storage Optimizations"
        private set

    init {
        incrementLaunchCount()
    }

    private fun incrementLaunchCount() {
        val launches = prefs.getInt("app_launch_count", 0) + 1
        prefs.edit().putInt("app_launch_count", launches).apply()
    }

    // Cooldown check: enforce minimum 7 days (604800000 ms) between any auto prompts
    private fun isPromptInCooldown(): Boolean {
        val lastPromptTime = prefs.getLong("last_prompt_timestamp_ms", 0L)
        val now = System.currentTimeMillis()
        val cooldownPeriod = 7 * 24 * 60 * 60 * 1000L // 7 days in ms
        return (now - lastPromptTime) < cooldownPeriod
    }

    private fun markPromptShown() {
        prefs.edit().putLong("last_prompt_timestamp_ms", System.currentTimeMillis()).apply()
    }

    fun onWarrantyOrReceiptSaved() {
        val count = prefs.getInt("saved_items_count", 0) + 1
        prefs.edit().putInt("saved_items_count", count).apply()

        FirebaseManager.logEvent("item_saved_count", mapOf("count" to count))

        // Check if smart rate dialog should trigger automatically (e.g. at 3rd saved item)
        if (shouldAutoShowRating()) {
            triggerRateDialog(isAuto = true)
        } else if (shouldAutoShowShare()) {
            triggerShareDialog(isAuto = true)
        }
    }

    fun onClaimPdfExported() {
        val exports = prefs.getInt("pdf_export_count", 0) + 1
        prefs.edit().putInt("pdf_export_count", exports).apply()

        if (shouldAutoShowRating()) {
            triggerRateDialog(isAuto = true)
        }
    }

    private fun shouldAutoShowRating(): Boolean {
        if (isPromptInCooldown()) return false
        val hasRated = prefs.getBoolean("has_rated_app", false)
        val neverShow = prefs.getBoolean("never_show_rating", false)
        val dismissCount = prefs.getInt("rate_dismiss_count", 0)
        val count = prefs.getInt("saved_items_count", 0)

        // Don't show if user dismissed twice already or marked never show
        return !hasRated && !neverShow && dismissCount < 2 && count >= 3
    }

    private fun shouldAutoShowShare(): Boolean {
        if (isPromptInCooldown()) return false
        val hasShared = prefs.getBoolean("has_shared_app", false)
        val dismissCount = prefs.getInt("share_dismiss_count", 0)
        val count = prefs.getInt("saved_items_count", 0)

        return !hasShared && dismissCount < 2 && count >= 5
    }

    private val appCurrentVersionCode: Int
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            com.example.BuildConfig.VERSION_CODE
        }

    private var pendingUpdateVersionCode: Int = 0

    fun checkForAppUpdate(
        currentVersionCode: Int = appCurrentVersionCode,
        newVersionCode: Int = appCurrentVersionCode,
        versionName: String = "",
        highlights: String = ""
    ) {
        if (newVersionCode <= currentVersionCode) return
        pendingUpdateVersionCode = newVersionCode
        if (versionName.isNotBlank()) latestVersionName = versionName
        if (highlights.isNotBlank()) latestVersionHighlights = highlights

        val ignoredVersion = prefs.getInt("ignored_update_version_code", 0)
        val lastUpdatePrompt = prefs.getLong("last_update_prompt_ms", 0L)
        val now = System.currentTimeMillis()
        val updateCooldown = 14 * 24 * 60 * 60 * 1000L // 14 days cooldown for update check

        if (newVersionCode > ignoredVersion) {
            if ((now - lastUpdatePrompt) > updateCooldown && !isPromptInCooldown()) {
                _showUpdateDialog.value = true
                prefs.edit().putLong("last_update_prompt_ms", now).apply()
                markPromptShown()
            }
        }
    }

    fun triggerRateDialog(isAuto: Boolean = false) {
        if (_showShareDialog.value || _showUpdateDialog.value) return // Mutex
        if (isAuto && isPromptInCooldown()) return

        _showRateDialog.value = true
        if (isAuto) markPromptShown()
    }

    fun dismissRateDialog(neverShowAgain: Boolean = false) {
        _showRateDialog.value = false
        val count = prefs.getInt("rate_dismiss_count", 0) + 1
        prefs.edit().putInt("rate_dismiss_count", count).apply()
        if (neverShowAgain || count >= 2) {
            prefs.edit().putBoolean("never_show_rating", true).apply()
        }
    }

    fun markRated() {
        prefs.edit().putBoolean("has_rated_app", true).apply()
        _showRateDialog.value = false
        FirebaseManager.logEvent("rate_app_5_stars_completed")
    }

    fun triggerShareDialog(isAuto: Boolean = false) {
        if (_showRateDialog.value || _showUpdateDialog.value) return // Mutex
        if (isAuto && isPromptInCooldown()) return

        _showShareDialog.value = true
        if (isAuto) markPromptShown()
    }

    fun dismissShareDialog() {
        _showShareDialog.value = false
        val count = prefs.getInt("share_dismiss_count", 0) + 1
        prefs.edit().putInt("share_dismiss_count", count).apply()
    }

    fun markShared() {
        prefs.edit().putBoolean("has_shared_app", true).apply()
        _showShareDialog.value = false
        FirebaseManager.logEvent("share_app_completed")
    }

    fun dismissUpdateDialog(remindLater: Boolean = true, ignoreVersion: Boolean = false) {
        _showUpdateDialog.value = false
        if (ignoreVersion && pendingUpdateVersionCode > 0) {
            prefs.edit().putInt("ignored_update_version_code", pendingUpdateVersionCode).apply()
        }
    }

    fun openAppUpdateLink() {
        _showUpdateDialog.value = false
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun shareApp() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "ClaimVault - Warranty & Receipt Manager")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out ClaimVault! It's a 100% privacy-first Android app with barcode scanning, currency conversion, and warranty tracking."
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
