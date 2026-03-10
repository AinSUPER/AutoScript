package com.autoscript.advanced.debug

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * 性能监控器
 * 监控CPU、内存、帧率等性能指标
 */
class PerformanceMonitor(private val context: Context) {

    /**
     * 性能指标
     */
    data class PerformanceMetrics(
        val timestamp: Long,
        val cpuUsage: Float,
        val memoryUsed: Long,
        val memoryTotal: Long,
        val memoryPercent: Float,
        val threadCount: Int,
        val fps: Float,
        val frameTime: Float,
        val networkLatency: Long? = null,
        val diskIO: DiskIO? = null
    )

    /**
     * 磁盘IO统计
     */
    data class DiskIO(
        val readBytes: Long,
        val writeBytes: Long,
        val readOps: Long,
        val writeOps: Long
    )

    /**
     * 方法执行统计
     */
    data class MethodStats(
        val name: String,
        val callCount: Long,
        val totalTimeMs: Long,
        val avgTimeMs: Float,
        val maxTimeMs: Long,
        val minTimeMs: Long
    )

    /**
     * 监控配置
     */
    data class MonitorConfig(
        val sampleIntervalMs: Long = 1000,
        val historySize: Int = 300,
        val enableMethodTracing: Boolean = false,
        val enableFrameMonitoring: Boolean = true,
        val warnCpuThreshold: Float = 80f,
        val warnMemoryThreshold: Float = 80f,
        val warnFpsThreshold: Float = 30f
    )

    /**
     * 性能警告
     */
    data class PerformanceWarning(
        val type: WarningType,
        val value: Any,
        val threshold: Any,
        val timestamp: Long
    )

    enum class WarningType {
        HIGH_CPU, HIGH_MEMORY, LOW_FPS, SLOW_METHOD
    }

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val executor = Executors.newScheduledThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var config = MonitorConfig()
    private val metricsHistory = CopyOnWriteArrayList<PerformanceMetrics>()
    private val methodStats = ConcurrentHashMap<String, MethodStats>()
    private val warnings = CopyOnWriteArrayList<PerformanceWarning>()
    private val listeners = mutableListOf<(PerformanceMetrics) -> Unit>()
    private val warningListeners = mutableListOf<(PerformanceWarning) -> Unit>()

    private var isMonitoring = false
    private var lastCpuTime = 0L
    private var lastAppCpuTime = 0L
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var currentFps = 60f

    private val methodStartTimes = ConcurrentHashMap<String, Long>()
    private val totalBytesRead = AtomicLong(0)
    private val totalBytesWritten = AtomicLong(0)

    /**
     * 设置监控配置
     */
    fun setConfig(config: MonitorConfig) {
        this.config = config
    }

    /**
     * 开始监控
     */
    fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        lastCpuTime = getTotalCpuTime()
        lastAppCpuTime = getAppCpuTime()

        executor.scheduleAtFixedRate({
            collectMetrics()
        }, 0, config.sampleIntervalMs, TimeUnit.MILLISECONDS)

        if (config.enableFrameMonitoring) {
            startFrameMonitoring()
        }
    }

    /**
     * 停止监控
     */
    fun stopMonitoring() {
        isMonitoring = false
        executor.shutdown()
    }

    /**
     * 收集性能指标
     */
    private fun collectMetrics() {
        try {
            val timestamp = System.currentTimeMillis()

            val cpuUsage = calculateCpuUsage()

            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)

            val memoryUsed = memoryInfo.totalPss * 1024L
            val memoryTotal = activityManager.memoryClass * 1024L * 1024L
            val memoryPercent = (memoryUsed.toFloat() / memoryTotal) * 100

            val threadCount = Thread.activeCount()

            val metrics = PerformanceMetrics(
                timestamp = timestamp,
                cpuUsage = cpuUsage,
                memoryUsed = memoryUsed,
                memoryTotal = memoryTotal,
                memoryPercent = memoryPercent,
                threadCount = threadCount,
                fps = currentFps,
                frameTime = if (currentFps > 0) 1000f / currentFps else 0f
            )

            metricsHistory.add(metrics)

            while (metricsHistory.size > config.historySize) {
                metricsHistory.removeAt(0)
            }

            checkWarnings(metrics)

            notifyListeners(metrics)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 计算CPU使用率
     */
    private fun calculateCpuUsage(): Float {
        val currentCpuTime = getTotalCpuTime()
        val currentAppCpuTime = getAppCpuTime()

        val cpuTimeDiff = currentCpuTime - lastCpuTime
        val appCpuTimeDiff = currentAppCpuTime - lastAppCpuTime

        lastCpuTime = currentCpuTime
        lastAppCpuTime = currentAppCpuTime

        return if (cpuTimeDiff > 0) {
            (appCpuTimeDiff.toFloat() / cpuTimeDiff) * 100f
        } else {
            0f
        }
    }

    /**
     * 获取总CPU时间
     */
    private fun getTotalCpuTime(): Long {
        return try {
            val statFile = java.io.File("/proc/stat")
            if (statFile.exists()) {
                val lines = statFile.readLines()
                if (lines.isNotEmpty()) {
                    val parts = lines[0].split("\\s+".toRegex())
                    if (parts.size > 8) {
                        var total = 0L
                        for (i in 1..8) {
                            total += parts[i].toLong()
                        }
                        total
                    } else {
                        0L
                    }
                } else {
                    0L
                }
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 获取应用CPU时间
     */
    private fun getAppCpuTime(): Long {
        return try {
            val pid = android.os.Process.myPid()
            val statFile = java.io.File("/proc/$pid/stat")
            if (statFile.exists()) {
                val line = statFile.readText()
                val parts = line.split("\\s+".toRegex())
                if (parts.size > 17) {
                    parts[13].toLong() + parts[14].toLong() + parts[15].toLong() + parts[16].toLong()
                } else {
                    0L
                }
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 开始帧率监控
     */
    private fun startFrameMonitoring() {
        mainHandler.post(object : Runnable {
            override fun run() {
                if (!isMonitoring) return

                val currentTime = System.currentTimeMillis()
                frameCount++

                if (lastFrameTime > 0) {
                    val elapsed = currentTime - lastFrameTime
                    if (elapsed >= 1000) {
                        currentFps = (frameCount * 1000f) / elapsed
                        frameCount = 0
                        lastFrameTime = currentTime
                    }
                } else {
                    lastFrameTime = currentTime
                }

                mainHandler.postDelayed(this, 16)
            }
        })
    }

    /**
     * 检查性能警告
     */
    private fun checkWarnings(metrics: PerformanceMetrics) {
        if (metrics.cpuUsage > config.warnCpuThreshold) {
            val warning = PerformanceWarning(
                type = WarningType.HIGH_CPU,
                value = metrics.cpuUsage,
                threshold = config.warnCpuThreshold,
                timestamp = metrics.timestamp
            )
            warnings.add(warning)
            notifyWarningListeners(warning)
        }

        if (metrics.memoryPercent > config.warnMemoryThreshold) {
            val warning = PerformanceWarning(
                type = WarningType.HIGH_MEMORY,
                value = metrics.memoryPercent,
                threshold = config.warnMemoryThreshold,
                timestamp = metrics.timestamp
            )
            warnings.add(warning)
            notifyWarningListeners(warning)
        }

        if (metrics.fps < config.warnFpsThreshold && metrics.fps > 0) {
            val warning = PerformanceWarning(
                type = WarningType.LOW_FPS,
                value = metrics.fps,
                threshold = config.warnFpsThreshold,
                timestamp = metrics.timestamp
            )
            warnings.add(warning)
            notifyWarningListeners(warning)
        }
    }

    /**
     * 开始方法追踪
     */
    fun beginMethod(methodName: String) {
        if (config.enableMethodTracing) {
            methodStartTimes[methodName] = System.currentTimeMillis()
        }
    }

    /**
     * 结束方法追踪
     */
    fun endMethod(methodName: String) {
        if (!config.enableMethodTracing) return

        val startTime = methodStartTimes.remove(methodName) ?: return
        val duration = System.currentTimeMillis() - startTime

        val stats = methodStats[methodName]
        if (stats != null) {
            methodStats[methodName] = stats.copy(
                callCount = stats.callCount + 1,
                totalTimeMs = stats.totalTimeMs + duration,
                avgTimeMs = (stats.totalTimeMs + duration).toFloat() / (stats.callCount + 1),
                maxTimeMs = maxOf(stats.maxTimeMs, duration),
                minTimeMs = minOf(stats.minTimeMs, duration)
            )
        } else {
            methodStats[methodName] = MethodStats(
                name = methodName,
                callCount = 1,
                totalTimeMs = duration,
                avgTimeMs = duration.toFloat(),
                maxTimeMs = duration,
                minTimeMs = duration
            )
        }
    }

    /**
     * 记录磁盘读取
     */
    fun recordDiskRead(bytes: Long) {
        totalBytesRead.addAndGet(bytes)
    }

    /**
     * 记录磁盘写入
     */
    fun recordDiskWrite(bytes: Long) {
        totalBytesWritten.addAndGet(bytes)
    }

    /**
     * 获取最新性能指标
     */
    fun getLatestMetrics(): PerformanceMetrics? {
        return metricsHistory.lastOrNull()
    }

    /**
     * 获取历史性能指标
     */
    fun getMetricsHistory(): List<PerformanceMetrics> {
        return metricsHistory.toList()
    }

    /**
     * 获取方法统计
     */
    fun getMethodStats(): List<MethodStats> {
        return methodStats.values.toList().sortedByDescending { it.totalTimeMs }
    }

    /**
     * 获取警告列表
     */
    fun getWarnings(): List<PerformanceWarning> {
        return warnings.toList()
    }

    /**
     * 清除警告
     */
    fun clearWarnings() {
        warnings.clear()
    }

    /**
     * 添加监听器
     */
    fun addListener(listener: (PerformanceMetrics) -> Unit) {
        listeners.add(listener)
    }

    /**
     * 移除监听器
     */
    fun removeListener(listener: (PerformanceMetrics) -> Unit) {
        listeners.remove(listener)
    }

    /**
     * 添加警告监听器
     */
    fun addWarningListener(listener: (PerformanceWarning) -> Unit) {
        warningListeners.add(listener)
    }

    /**
     * 移除警告监听器
     */
    fun removeWarningListener(listener: (PerformanceWarning) -> Unit) {
        warningListeners.remove(listener)
    }

    /**
     * 通知监听器
     */
    private fun notifyListeners(metrics: PerformanceMetrics) {
        listeners.forEach { it(metrics) }
    }

    /**
     * 通知警告监听器
     */
    private fun notifyWarningListeners(warning: PerformanceWarning) {
        warningListeners.forEach { it(warning) }
    }

    /**
     * 获取性能摘要
     */
    fun getSummary(): Map<String, Any> {
        val latest = getLatestMetrics() ?: return emptyMap()
        val history = getMetricsHistory()

        return mapOf(
            "currentCpuUsage" to latest.cpuUsage,
            "currentMemoryUsed" to latest.memoryUsed,
            "currentMemoryPercent" to latest.memoryPercent,
            "currentFps" to latest.fps,
            "avgCpuUsage" to (if (history.isNotEmpty()) history.map { it.cpuUsage }.average() else 0.0),
            "avgMemoryPercent" to (if (history.isNotEmpty()) history.map { it.memoryPercent }.average() else 0.0),
            "avgFps" to (if (history.isNotEmpty()) history.map { it.fps }.average() else 0.0),
            "warningCount" to warnings.size,
            "methodCount" to methodStats.size
        )
    }

    /**
     * 触发GC
     */
    fun triggerGC() {
        System.gc()
        System.runFinalization()
    }

    /**
     * 获取可用内存
     */
    fun getAvailableMemory(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem
    }

    /**
     * 检查是否低内存
     */
    fun isLowMemory(): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        stopMonitoring()
        listeners.clear()
        warningListeners.clear()
        metricsHistory.clear()
        methodStats.clear()
        warnings.clear()
    }
}
