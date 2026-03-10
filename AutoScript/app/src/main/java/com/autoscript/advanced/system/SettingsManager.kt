package com.autoscript.advanced.system

import android.content.Context
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.view.WindowManager

/**
 * 系统设置管理
 * 提供系统设置读取和修改功能
 */
class SettingsManager(private val context: Context) {

    /**
     * 设置操作结果
     */
    data class SettingsResult(
        val success: Boolean,
        val value: Any? = null,
        val error: String? = null
    )

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * 获取屏幕亮度
     * @return 亮度值 (0-255)
     */
    fun getScreenBrightness(): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (e: Exception) {
            128
        }
    }

    /**
     * 设置屏幕亮度
     * @param brightness 亮度值 (0-255)
     * @return 是否成功
     */
    fun setScreenBrightness(brightness: Int): Boolean {
        return try {
            val value = brightness.coerceIn(0, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取屏幕亮度模式
     * @return 亮度模式 (0=手动, 1=自动)
     */
    fun getScreenBrightnessMode(): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
        } catch (e: Exception) {
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        }
    }

    /**
     * 设置屏幕亮度模式
     * @param auto 是否自动亮度
     * @return 是否成功
     */
    fun setScreenBrightnessMode(auto: Boolean): Boolean {
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取屏幕超时时间
     * @return 超时时间 (毫秒)
     */
    fun getScreenTimeout(): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT
            )
        } catch (e: Exception) {
            30000
        }
    }

    /**
     * 设置屏幕超时时间
     * @param timeoutMs 超时时间 (毫秒)
     * @return 是否成功
     */
    fun setScreenTimeout(timeoutMs: Int): Boolean {
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                timeoutMs
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取媒体音量
     * @return 音量值
     */
    fun getMediaVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    /**
     * 设置媒体音量
     * @param volume 音量值
     * @return 是否成功
     */
    fun setMediaVolume(volume: Int): Boolean {
        return try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val value = volume.coerceIn(0, maxVolume)
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                value,
                AudioManager.FLAG_SHOW_UI
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取铃声音量
     * @return 音量值
     */
    fun getRingVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_RING)
    }

    /**
     * 设置铃声音量
     * @param volume 音量值
     * @return 是否成功
     */
    fun setRingVolume(volume: Int): Boolean {
        return try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val value = volume.coerceIn(0, maxVolume)
            audioManager.setStreamVolume(
                AudioManager.STREAM_RING,
                value,
                AudioManager.FLAG_SHOW_UI
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取通知音量
     * @return 音量值
     */
    fun getNotificationVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
    }

    /**
     * 设置通知音量
     * @param volume 音量值
     * @return 是否成功
     */
    fun setNotificationVolume(volume: Int): Boolean {
        return try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            val value = volume.coerceIn(0, maxVolume)
            audioManager.setStreamVolume(
                AudioManager.STREAM_NOTIFICATION,
                value,
                AudioManager.FLAG_SHOW_UI
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取闹钟音量
     * @return 音量值
     */
    fun getAlarmVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
    }

    /**
     * 设置闹钟音量
     * @param volume 音量值
     * @return 是否成功
     */
    fun setAlarmVolume(volume: Int): Boolean {
        return try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val value = volume.coerceIn(0, maxVolume)
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                value,
                AudioManager.FLAG_SHOW_UI
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取最大媒体音量
     * @return 最大音量值
     */
    fun getMaxMediaVolume(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    /**
     * 设置静音模式
     * @param silent 是否静音
     * @return 是否成功
     */
    fun setSilentMode(silent: Boolean): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val mode = if (silent) {
                    AudioManager.RINGER_MODE_SILENT
                } else {
                    AudioManager.RINGER_MODE_NORMAL
                }
                audioManager.ringerMode = mode
            } else {
                @Suppress("DEPRECATION")
                audioManager.setStreamMute(AudioManager.STREAM_RING, silent)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取静音状态
     * @return 是否静音
     */
    fun isSilentMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
        } else {
            getRingVolume() == 0
        }
    }

    /**
     * 检查WiFi是否开启
     * @return 是否开启
     */
    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    /**
     * 设置WiFi开关
     * @param enabled 是否开启
     * @return 是否成功
     */
    fun setWifiEnabled(enabled: Boolean): Boolean {
        return try {
            wifiManager.isWifiEnabled = enabled
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取屏幕宽度
     * @return 屏幕宽度 (像素)
     */
    fun getScreenWidth(): Int {
        val displayMetrics = android.util.DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    /**
     * 获取屏幕高度
     * @return 屏幕高度 (像素)
     */
    fun getScreenHeight(): Int {
        val displayMetrics = android.util.DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    /**
     * 获取屏幕密度
     * @return 屏幕密度 (DPI)
     */
    fun getScreenDensity(): Int {
        val displayMetrics = android.util.DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.densityDpi
    }

    /**
     * 获取屏幕刷新率
     * @return 刷新率 (Hz)
     */
    fun getScreenRefreshRate(): Float {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.refreshRate ?: 60f
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate
        }
    }

    /**
     * 获取设备名称
     * @return 设备名称
     */
    fun getDeviceName(): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                "bluetooth_name"
            ) ?: Build.MODEL
        } catch (e: Exception) {
            Build.MODEL
        }
    }

    /**
     * 获取Android版本
     * @return Android版本
     */
    fun getAndroidVersion(): String {
        return Build.VERSION.RELEASE
    }

    /**
     * 获取SDK版本
     * @return SDK版本号
     */
    fun getSdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }

    /**
     * 获取设备型号
     * @return 设备型号
     */
    fun getDeviceModel(): String {
        return Build.MODEL
    }

    /**
     * 获取设备制造商
     * @return 制造商
     */
    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER
    }

    /**
     * 获取设备品牌
     * @return 品牌
     */
    fun getDeviceBrand(): String {
        return Build.BRAND
    }

    /**
     * 获取设备ID
     * @return 设备ID
     */
    fun getDeviceId(): String? {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查是否为平板
     * @return 是否为平板
     */
    fun isTablet(): Boolean {
        return context.resources.configuration.screenLayout and
                android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK >=
                android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    /**
     * 检查是否为横屏
     * @return 是否为横屏
     */
    fun isLandscape(): Boolean {
        return context.resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * 获取当前语言
     * @return 语言代码
     */
    fun getCurrentLanguage(): String {
        return context.resources.configuration.locale.language
    }

    /**
     * 获取当前地区
     * @return 地区代码
     */
    fun getCurrentCountry(): String {
        return context.resources.configuration.locale.country
    }

    /**
     * 获取时区
     * @return 时区ID
     */
    fun getTimeZone(): String {
        return java.util.TimeZone.getDefault().id
    }

    /**
     * 获取系统设置值
     * @param name 设置名称
     * @return 设置值
     */
    fun getSystemSetting(name: String): String? {
        return try {
            Settings.System.getString(context.contentResolver, name)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 设置系统设置值
     * @param name 设置名称
     * @param value 设置值
     * @return 是否成功
     */
    fun setSystemSetting(name: String, value: String): Boolean {
        return try {
            Settings.System.putString(context.contentResolver, name, value)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取安全设置值
     * @param name 设置名称
     * @return 设置值
     */
    fun getSecureSetting(name: String): String? {
        return try {
            Settings.Secure.getString(context.contentResolver, name)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取全局设置值
     * @param name 设置名称
     * @return 设置值
     */
    fun getGlobalSetting(name: String): String? {
        return try {
            Settings.Global.getString(context.contentResolver, name)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取电池电量
     * @return 电量百分比 (0-100)
     */
    fun getBatteryLevel(): Int {
        return try {
            val batteryIntent = context.registerReceiver(
                null,
                android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level >= 0 && scale > 0) {
                (level * 100) / scale
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 检查是否正在充电
     * @return 是否正在充电
     */
    fun isCharging(): Boolean {
        return try {
            val batteryIntent = context.registerReceiver(
                null,
                android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            )
            val status = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == android.os.BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            false
        }
    }
}
