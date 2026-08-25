package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val displayName: String) {
    EN("English"),
    ES("Español"),
    FR("Français"),
    DE("Deutsch"),
    VI("Tiếng Việt")
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("claim_vault_theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(getSavedLanguage())
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean("has_completed_onboarding", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _hasCompletedCameraOnboarding = MutableStateFlow(prefs.getBoolean("has_completed_camera_onboarding", false))
    val hasCompletedCameraOnboarding: StateFlow<Boolean> = _hasCompletedCameraOnboarding.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("is_biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun getSavedThemeMode(): ThemeMode {
        val modeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun getSavedLanguage(): AppLanguage {
        val langStr = prefs.getString("app_language", AppLanguage.EN.name) ?: AppLanguage.EN.name
        return try {
            AppLanguage.valueOf(langStr)
        } catch (e: Exception) {
            AppLanguage.EN
        }
    }

    fun setAppLanguage(lang: AppLanguage) {
        prefs.edit().putString("app_language", lang.name).apply()
        _appLanguage.value = lang
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
        _hasCompletedOnboarding.value = true
    }

    fun completeCameraOnboarding() {
        prefs.edit().putBoolean("has_completed_camera_onboarding", true).apply()
        _hasCompletedCameraOnboarding.value = true
    }

    fun resetCameraOnboarding() {
        prefs.edit().putBoolean("has_completed_camera_onboarding", false).apply()
        _hasCompletedCameraOnboarding.value = false
    }
}
