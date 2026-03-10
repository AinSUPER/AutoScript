package com.autoscript.di

import android.content.Context
import com.autoscript.data.local.database.ScriptDatabase
import com.autoscript.data.repository.ScriptRepository
import com.autoscript.engine.ScriptEngine
import com.autoscript.service.AccessibilityServiceManager
import com.autoscript.utils.ImageProcessor
import com.autoscript.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object AppModule {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var database: ScriptDatabase? = null

    @Volatile
    private var scriptRepository: ScriptRepository? = null

    @Volatile
    private var scriptEngine: ScriptEngine? = null

    @Volatile
    private var accessibilityManager: AccessibilityServiceManager? = null

    @Volatile
    private var imageProcessor: ImageProcessor? = null

    fun provideContext(): Context {
        return com.autoscript.AutoScriptApp.getAppContext()
    }

    fun provideApplicationScope(): CoroutineScope = applicationScope

    fun provideDatabase(context: Context): ScriptDatabase {
        return database ?: synchronized(this) {
            database ?: ScriptDatabase.getInstance(context).also { database = it }
        }
    }

    fun provideScriptRepository(context: Context): ScriptRepository {
        return scriptRepository ?: synchronized(this) {
            scriptRepository ?: ScriptRepository(provideDatabase(context)).also { scriptRepository = it }
        }
    }

    fun provideScriptEngine(context: Context): ScriptEngine {
        return scriptEngine ?: synchronized(this) {
            scriptEngine ?: ScriptEngine(
                context = context,
                repository = provideScriptRepository(context),
                scope = applicationScope
            ).also { scriptEngine = it }
        }
    }

    fun provideAccessibilityManager(context: Context): AccessibilityServiceManager {
        return accessibilityManager ?: synchronized(this) {
            accessibilityManager ?: AccessibilityServiceManager(context).also { accessibilityManager = it }
        }
    }

    fun provideImageProcessor(context: Context): ImageProcessor {
        return imageProcessor ?: synchronized(this) {
            imageProcessor ?: ImageProcessor(context).also { imageProcessor = it }
        }
    }

    fun providePreferenceManager(): PreferenceManager {
        return PreferenceManager
    }

    fun cleanup() {
        database?.close()
        database = null
        scriptRepository = null
        scriptEngine = null
        accessibilityManager = null
        imageProcessor = null
    }
}
