package com.example.project_phoenix.data

import android.content.Context
import androidx.core.content.edit

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND, true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SOUND, enabled) }
    }

    fun isNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS, false)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
    }

    companion object {
        private const val PREFS_NAME = "settings_prefs"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
    }
}