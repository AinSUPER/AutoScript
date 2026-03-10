package com.autoscript.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.autoscript.AutoScriptApp

object PreferenceManager {

    private const val PREF_NAME = "autoscript_prefs"

    private const val KEY_NIGHT_MODE = "night_mode"
    private const val KEY_AUTO_START = "auto_start"
    private const val KEY_SCRIPT_DELAY = "script_delay"
    private const val KEY_RUNNING_SPEED = "running_speed"
    private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    private const val KEY_SHOW_FLOATING_WINDOW = "show_floating_window"
    private const val KEY_VIBRATE_ON_ACTION = "vibrate_on_action"
    private const val KEY_DEFAULT_SCRIPT_ID = "default_script_id"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getNightMode(): Int {
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    fun setNightMode(mode: Int) {
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply()
    }

    fun isAutoStart(): Boolean {
        return prefs.getBoolean(KEY_AUTO_START, false)
    }

    fun setAutoStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    fun getScriptDelay(): Long {
        return prefs.getLong(KEY_SCRIPT_DELAY, 500L)
    }

    fun setScriptDelay(delay: Long) {
        prefs.edit().putLong(KEY_SCRIPT_DELAY, delay).apply()
    }

    fun getRunningSpeed(): Int {
        return prefs.getInt(KEY_RUNNING_SPEED, 100)
    }

    fun setRunningSpeed(speed: Int) {
        prefs.edit().putInt(KEY_RUNNING_SPEED, speed).apply()
    }

    fun isKeepScreenOn(): Boolean {
        return prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
    }

    fun isShowFloatingWindow(): Boolean {
        return prefs.getBoolean(KEY_SHOW_FLOATING_WINDOW, true)
    }

    fun setShowFloatingWindow(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_FLOATING_WINDOW, enabled).apply()
    }

    fun isVibrateOnAction(): Boolean {
        return prefs.getBoolean(KEY_VIBRATE_ON_ACTION, true)
    }

    fun setVibrateOnAction(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE_ON_ACTION, enabled).apply()
    }

    fun getDefaultScriptId(): Long {
        return prefs.getLong(KEY_DEFAULT_SCRIPT_ID, -1L)
    }

    fun setDefaultScriptId(id: Long) {
        prefs.edit().putLong(KEY_DEFAULT_SCRIPT_ID, id).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
