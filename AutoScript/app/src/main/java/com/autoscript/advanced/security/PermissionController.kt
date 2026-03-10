package com.autoscript.advanced.security

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限控制器
 * 管理应用权限检查和请求
 */
class PermissionController(private val context: Context) {

    /**
     * 权限状态
     */
    enum class PermissionStatus {
        GRANTED, DENIED, DENIED_DO_NOT_ASK_AGAIN
    }

    /**
     * 权限检查结果
     */
    data class PermissionCheckResult(
        val permission: String,
        val status: PermissionStatus,
        val shouldShowRationale: Boolean
    )

    /**
     * 权限请求结果
     */
    data class PermissionRequestResult(
        val requestCode: Int,
        val results: Map<String, Boolean>,
        val allGranted: Boolean
    )

    /**
     * 权限配置
     */
    data class PermissionConfig(
        val requestCode: Int = 1000,
        val showRationaleBeforeRequest: Boolean = true,
        val openSettingsOnPermanentlyDenied: Boolean = true
    )

    private var config = PermissionConfig()
    private val pendingRequests = mutableMapOf<Int, (PermissionRequestResult) -> Unit>()

    companion object {
        val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val CAMERA_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val PHONE_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )

        val CONTACTS_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )

        val RECORD_AUDIO_PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO)

        val DANGEROUS_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS
        )
    }

    /**
     * 设置权限配置
     */
    fun setConfig(config: PermissionConfig) {
        this.config = config
    }

    /**
     * 检查单个权限
     * @param permission 权限名称
     * @return 权限状态
     */
    fun checkPermission(permission: String): PermissionStatus {
        return when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                PermissionStatus.GRANTED
            }
            ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission) -> {
                PermissionStatus.DENIED
            }
            else -> {
                PermissionStatus.DENIED_DO_NOT_ASK_AGAIN
            }
        }
    }

    /**
     * 检查多个权限
     * @param permissions 权限列表
     * @return 权限检查结果列表
     */
    fun checkPermissions(permissions: Array<String>): List<PermissionCheckResult> {
        return permissions.map { permission ->
            PermissionCheckResult(
                permission = permission,
                status = checkPermission(permission),
                shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity,
                    permission
                )
            )
        }
    }

    /**
     * 检查是否所有权限已授予
     * @param permissions 权限列表
     * @return 是否全部授予
     */
    fun areAllPermissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 获取未授予的权限
     * @param permissions 权限列表
     * @return 未授予的权限列表
     */
    fun getDeniedPermissions(permissions: Array<String>): List<String> {
        return permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 请求权限
     * @param activity Activity
     * @param permissions 权限列表
     * @param requestCode 请求码
     * @param callback 回调
     */
    fun requestPermissions(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int = config.requestCode,
        callback: (PermissionRequestResult) -> Unit
    ) {
        pendingRequests[requestCode] = callback

        val deniedPermissions = getDeniedPermissions(permissions)

        if (deniedPermissions.isEmpty()) {
            val results = permissions.associateWith { true }
            callback(PermissionRequestResult(requestCode, results, true))
            pendingRequests.remove(requestCode)
            return
        }

        ActivityCompat.requestPermissions(activity, deniedPermissions.toTypedArray(), requestCode)
    }

    /**
     * 处理权限请求结果
     * @param requestCode 请求码
     * @param permissions 权限列表
     * @param grantResults 授权结果
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val callback = pendingRequests.remove(requestCode) ?: return

        val results = mutableMapOf<String, Boolean>()
        for (i in permissions.indices) {
            results[permissions[i]] = grantResults.getOrNull(i) == PackageManager.PERMISSION_GRANTED
        }

        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        callback(PermissionRequestResult(requestCode, results, allGranted))
    }

    /**
     * 请求存储权限
     */
    fun requestStoragePermission(
        activity: Activity,
        requestCode: Int = config.requestCode,
        callback: (PermissionRequestResult) -> Unit
    ) {
        requestPermissions(activity, STORAGE_PERMISSIONS, requestCode, callback)
    }

    /**
     * 请求相机权限
     */
    fun requestCameraPermission(
        activity: Activity,
        requestCode: Int = config.requestCode,
        callback: (PermissionRequestResult) -> Unit
    ) {
        requestPermissions(activity, CAMERA_PERMISSIONS, requestCode, callback)
    }

    /**
     * 请求位置权限
     */
    fun requestLocationPermission(
        activity: Activity,
        requestCode: Int = config.requestCode,
        callback: (PermissionRequestResult) -> Unit
    ) {
        requestPermissions(activity, LOCATION_PERMISSIONS, requestCode, callback)
    }

    /**
     * 请求录音权限
     */
    fun requestRecordAudioPermission(
        activity: Activity,
        requestCode: Int = config.requestCode,
        callback: (PermissionRequestResult) -> Unit
    ) {
        requestPermissions(activity, RECORD_AUDIO_PERMISSIONS, requestCode, callback)
    }

    /**
     * 检查存储权限
     */
    fun hasStoragePermission(): Boolean {
        return areAllPermissionsGranted(STORAGE_PERMISSIONS)
    }

    /**
     * 检查相机权限
     */
    fun hasCameraPermission(): Boolean {
        return areAllPermissionsGranted(CAMERA_PERMISSIONS)
    }

    /**
     * 检查位置权限
     */
    fun hasLocationPermission(): Boolean {
        return areAllPermissionsGranted(LOCATION_PERMISSIONS)
    }

    /**
     * 检查录音权限
     */
    fun hasRecordAudioPermission(): Boolean {
        return areAllPermissionsGranted(RECORD_AUDIO_PERMISSIONS)
    }

    /**
     * 获取所有危险权限状态
     */
    fun getAllDangerousPermissionsStatus(): Map<String, PermissionStatus> {
        return DANGEROUS_PERMISSIONS.associateWith { checkPermission(it) }
    }

    /**
     * 获取已授予的危险权限
     */
    fun getGrantedDangerousPermissions(): List<String> {
        return DANGEROUS_PERMISSIONS.filter {
            checkPermission(it) == PermissionStatus.GRANTED
        }
    }

    /**
     * 获取未授予的危险权限
     */
    fun getDeniedDangerousPermissions(): List<String> {
        return DANGEROUS_PERMISSIONS.filter {
            checkPermission(it) != PermissionStatus.GRANTED
        }
    }

    /**
     * 打开应用设置页面
     */
    fun openAppSettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = android.net.Uri.parse("package:${context.packageName}")
        context.startActivity(intent)
    }

    /**
     * 检查是否有悬浮窗权限
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestOverlayPermission(activity: Activity, requestCode: Int = 1001) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            activity.startActivityForResult(intent, requestCode)
        }
    }

    /**
     * 检查是否有无障碍服务权限
     */
    fun hasAccessibilityPermission(): Boolean {
        val accessibilityEnabled = try {
            android.provider.Settings.Secure.getInt(
                context.contentResolver,
                android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Exception) {
            0
        }

        if (accessibilityEnabled == 1) {
            val service = "${context.packageName}/${context.packageName}.service.AccessibilityServiceImpl"
            val settingValue = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return settingValue.contains(service)
        }

        return false
    }

    /**
     * 打开无障碍服务设置
     */
    fun openAccessibilitySettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 检查是否有通知权限
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 请求通知权限
     */
    fun requestNotificationPermission(
        activity: Activity,
        requestCode: Int = config.requestCode,
        callback: (PermissionRequestResult) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                requestCode,
                callback
            )
        } else {
            callback(PermissionRequestResult(requestCode, mapOf("notification" to true), true))
        }
    }

    /**
     * 获取权限说明
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "需要存储权限来读取和保存文件"

            Manifest.permission.CAMERA -> "需要相机权限来拍摄照片和视频"

            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> "需要位置权限来获取您的位置信息"

            Manifest.permission.READ_PHONE_STATE -> "需要电话权限来获取设备信息"

            Manifest.permission.CALL_PHONE -> "需要电话权限来拨打电话"

            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS -> "需要通讯录权限来管理联系人"

            Manifest.permission.RECORD_AUDIO -> "需要录音权限来录制音频"

            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS -> "需要短信权限来发送和接收短信"

            else -> "需要此权限才能正常使用功能"
        }
    }
}
