package com.autoscript.advanced.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import java.io.File

/**
 * 应用管理
 * 提供应用启动、关闭、卸载等功能
 */
class AppManager(private val context: Context) {

    private val packageManager = context.packageManager

    /**
     * 应用信息
     */
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val versionName: String,
        val versionCode: Long,
        val installTime: Long,
        val updateTime: Long,
        val appSize: Long = 0,
        val isSystemApp: Boolean,
        val isEnabled: Boolean,
        val targetSdkVersion: Int,
        val sourceDir: String,
        val dataDir: String
    )

    /**
     * 应用操作结果
     */
    data class AppOperationResult(
        val success: Boolean,
        val message: String? = null
    )

    /**
     * 获取已安装应用列表
     * @param includeSystemApps 是否包含系统应用
     * @return 应用列表
     */
    fun getInstalledApps(includeSystemApps: Boolean = false): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()

        try {
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(0)
            }

            for (packageInfo in packages) {
                val isSystemApp = (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                if (!includeSystemApps && isSystemApp) {
                    continue
                }

                apps.add(packageInfoToAppInfo(packageInfo))
            }
        } catch (e: Exception) {
        }

        return apps.sortedBy { it.appName.lowercase() }
    }

    /**
     * 获取应用信息
     * @param packageName 包名
     * @return 应用信息
     */
    fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfoToAppInfo(packageInfo)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * PackageInfo转AppInfo
     */
    private fun packageInfoToAppInfo(packageInfo: PackageInfo): AppInfo {
        val appInfo = packageInfo.applicationInfo

        return AppInfo(
            packageName = packageInfo.packageName,
            appName = appInfo.loadLabel(packageManager).toString(),
            versionName = packageInfo.versionName ?: "",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            },
            installTime = packageInfo.firstInstallTime,
            updateTime = packageInfo.lastUpdateTime,
            appSize = try {
                File(appInfo.sourceDir).length()
            } catch (e: Exception) {
                0L
            },
            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            isEnabled = appInfo.enabled,
            targetSdkVersion = appInfo.targetSdkVersion,
            sourceDir = appInfo.sourceDir,
            dataDir = appInfo.dataDir
        )
    }

    /**
     * 启动应用
     * @param packageName 包名
     * @return 操作结果
     */
    fun launchApp(packageName: String): AppOperationResult {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                AppOperationResult(true, "应用已启动")
            } else {
                AppOperationResult(false, "无法启动应用")
            }
        } catch (e: Exception) {
            AppOperationResult(false, e.message)
        }
    }

    /**
     * 启动应用的指定Activity
     * @param packageName 包名
     * @param activityClassName Activity类名
     * @return 操作结果
     */
    fun launchActivity(packageName: String, activityClassName: String): AppOperationResult {
        return try {
            val intent = Intent().apply {
                setClassName(packageName, activityClassName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AppOperationResult(true, "Activity已启动")
        } catch (e: Exception) {
            AppOperationResult(false, e.message)
        }
    }

    /**
     * 打开应用详情页
     * @param packageName 包名
     * @return 操作结果
     */
    fun openAppDetails(packageName: String): AppOperationResult {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AppOperationResult(true, "已打开应用详情")
        } catch (e: Exception) {
            AppOperationResult(false, e.message)
        }
    }

    /**
     * 卸载应用
     * @param packageName 包名
     * @return 操作结果
     */
    fun uninstallApp(packageName: String): AppOperationResult {
        return try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AppOperationResult(true, "已打开卸载界面")
        } catch (e: Exception) {
            AppOperationResult(false, e.message)
        }
    }

    /**
     * 检查应用是否已安装
     * @param packageName 包名
     * @return 是否已安装
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 获取应用图标
     * @param packageName 包名
     * @return 图标Drawable
     */
    fun getAppIcon(packageName: String): Drawable? {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            appInfo.loadIcon(packageManager)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取应用版本名
     * @param packageName 包名
     * @return 版本名
     */
    fun getVersionName(packageName: String): String? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取应用版本号
     * @param packageName 包名
     * @return 版本号
     */
    fun getVersionCode(packageName: String): Long? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取应用签名
     * @param packageName 包名
     * @return 签名字符串
     */
    fun getAppSignature(packageName: String): String? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.firstOrNull()?.let { signature ->
                val md = java.security.MessageDigest.getInstance("SHA-256")
                md.update(signature.toByteArray())
                md.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 搜索应用
     * @param query 搜索关键词
     * @return 匹配的应用列表
     */
    fun searchApps(query: String): List<AppInfo> {
        return getInstalledApps().filter { app ->
            app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
        }
    }

    /**
     * 获取可以处理指定Intent的应用列表
     * @param intent Intent
     * @return 应用列表
     */
    fun queryIntentActivities(intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
    }

    /**
     * 获取默认启动器包名
     * @return 启动器包名
     */
    fun getDefaultLauncherPackage(): String? {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(intent, 0)
            }
            resolveInfo?.activityInfo?.packageName
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取应用权限列表
     * @param packageName 包名
     * @return 权限列表
     */
    fun getAppPermissions(packageName: String): List<String> {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            packageInfo.requestedPermissions?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 清除应用数据
     * @param packageName 包名
     * @return 操作结果
     */
    fun clearAppData(packageName: String): AppOperationResult {
        return try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AppOperationResult(true, "请手动清除应用数据")
        } catch (e: Exception) {
            AppOperationResult(false, e.message)
        }
    }

    /**
     * 检查应用是否为系统应用
     * @param packageName 包名
     * @return 是否为系统应用
     */
    fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取当前应用版本名
     * @return 版本名
     */
    fun getCurrentAppVersionName(): String? {
        return getVersionName(context.packageName)
    }

    /**
     * 获取当前应用版本号
     * @return 版本号
     */
    fun getCurrentAppVersionCode(): Long? {
        return getVersionCode(context.packageName)
    }
}
