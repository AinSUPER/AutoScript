package com.autoscript

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.autoscript.data.local.database.ScriptDatabase
import com.autoscript.di.AppModule
import com.autoscript.utils.PreferenceManager
import com.autoscript.utils.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoScriptApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        @Volatile
        private var instance: AutoScriptApp? = null

        fun getInstance(): AutoScriptApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }

        fun getAppContext(): Context = getInstance().applicationContext

        const val CHANNEL_SERVICE = "service_channel"
        const val CHANNEL_SCRIPT = "script_channel"
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        initApp()
    }

    private fun initApp() {
        applicationScope.launch {
            try {
                initPreferences()
                initTheme()
                initDatabase()
                initNotificationChannels()
                LogUtils.d("AutoScriptApp", "Application initialized successfully")
            } catch (e: Exception) {
                LogUtils.e("AutoScriptApp", "Application initialization failed", e)
            }
        }
    }

    private fun initPreferences() {
        PreferenceManager.init(this)
    }

    private fun initTheme() {
        val nightMode = PreferenceManager.getNightMode()
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun initDatabase() {
        ScriptDatabase.getInstance(this)
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AutoScript服务运行状态通知"
                setShowBadge(false)
            }

            val scriptChannel = NotificationChannel(
                CHANNEL_SCRIPT,
                getString(R.string.notification_channel_script),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "脚本执行状态通知"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(serviceChannel, scriptChannel))
        }
    }

    fun getAppScope(): CoroutineScope = applicationScope
}
