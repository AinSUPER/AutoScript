package com.autoscript.service

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.autoscript.utils.LogUtils

class AccessibilityServiceManager(private val context: Context) {

    companion object {
        private const val TAG = "AccessibilityServiceManager"
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        var accessibilityEnabled = 0
        val service = "${context.packageName}/${AccessibilityServiceImpl::class.java.canonicalName}"
        
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            LogUtils.e(TAG, "Error finding setting, default accessibility to not found", e)
        }

        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (!settingValue.isNullOrEmpty()) {
                val splitter = TextUtils.SimpleStringSplitter(':')
                splitter.setString(settingValue)
                while (splitter.hasNext()) {
                    val accessibilityService = splitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }

        return AccessibilityServiceImpl.isServiceEnabled()
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getServiceInstance(): AccessibilityServiceImpl? {
        return AccessibilityServiceImpl.getInstance()
    }

    suspend fun click(x: Int, y: Int): Boolean {
        val service = getServiceInstance() ?: return false
        return service.click(x, y)
    }

    suspend fun longClick(x: Int, y: Int, duration: Long = 500): Boolean {
        val service = getServiceInstance() ?: return false
        return service.longClick(x, y, duration)
    }

    suspend fun swipe(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        duration: Long = 300
    ): Boolean {
        val service = getServiceInstance() ?: return false
        return service.swipe(startX, startY, endX, endY, duration)
    }

    fun inputText(text: String): Boolean {
        val service = getServiceInstance() ?: return false
        return service.inputText(text)
    }

    fun getRootNode() = getServiceInstance()?.getRootNode()

    fun findNodeByText(text: String) = getServiceInstance()?.findNodeByText(text)

    fun findNodeById(id: String) = getServiceInstance()?.findNodeById(id)

    fun clickNode(node: android.view.accessibility.AccessibilityNodeInfo): Boolean {
        val service = getServiceInstance() ?: return false
        return service.clickNode(node)
    }
}
