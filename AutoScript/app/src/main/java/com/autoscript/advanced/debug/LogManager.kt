package com.autoscript.advanced.debug

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 日志管理器
 * 提供日志记录、过滤、导出等功能
 */
class LogManager(private val context: Context) {

    /**
     * 日志级别
     */
    enum class LogLevel(val priority: Int) {
        VERBOSE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        FATAL(5)
    }

    /**
     * 日志条目
     */
    data class LogEntry(
        val id: Long,
        val timestamp: Long,
        val level: LogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null,
        val threadName: String = Thread.currentThread().name,
        val extra: Map<String, Any?> = emptyMap()
    )

    /**
     * 日志配置
     */
    data class LogConfig(
        val minLevel: LogLevel = LogLevel.DEBUG,
        val maxEntries: Int = 10000,
        val enableFileLog: Boolean = true,
        val logDirectory: String = "logs",
        val logFileName: String = "autoscript.log",
        val maxFileSize: Long = 5 * 1024 * 1024,
        val maxBackupFiles: Int = 5,
        val dateFormat: String = "yyyy-MM-dd HH:mm:ss.SSS"
    )

    /**
     * 日志过滤器
     */
    data class LogFilter(
        val minLevel: LogLevel? = null,
        val maxLevel: LogLevel? = null,
        val tags: List<String>? = null,
        val messageContains: String? = null,
        val startTime: Long? = null,
        val endTime: Long? = null
    )

    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val listeners = mutableListOf<(LogEntry) -> Unit>()
    private val executor = Executors.newSingleThreadScheduledExecutor()

    private var config = LogConfig()
    private var currentId = 0L
    private var logFile: File? = null
    private var fileWriter: FileWriter? = null
    private val dateFormat = SimpleDateFormat(config.dateFormat, Locale.getDefault())

    init {
        if (config.enableFileLog) {
            initLogFile()
        }

        executor.scheduleAtFixedRate({
            trimLogs()
        }, 1, 1, TimeUnit.MINUTES)
    }

    /**
     * 设置日志配置
     */
    fun setConfig(config: LogConfig) {
        this.config = config
        dateFormat.applyPattern(config.dateFormat)

        if (config.enableFileLog) {
            initLogFile()
        }
    }

    /**
     * 初始化日志文件
     */
    private fun initLogFile() {
        try {
            val logDir = File(context.filesDir, config.logDirectory)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            logFile = File(logDir, config.logFileName)

            if (logFile!!.exists() && logFile!!.length() > config.maxFileSize) {
                rotateLogFiles()
            }

            fileWriter = FileWriter(logFile, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 轮转日志文件
     */
    private fun rotateLogFiles() {
        try {
            for (i in config.maxBackupFiles - 1 downTo 1) {
                val oldFile = File(logFile!!.parent, "${config.logFileName}.$i")
                val newFile = File(logFile!!.parent, "${config.logFileName}.${i + 1}")

                if (oldFile.exists()) {
                    if (i == config.maxBackupFiles - 1) {
                        oldFile.delete()
                    } else {
                        oldFile.renameTo(newFile)
                    }
                }
            }

            logFile!!.renameTo(File(logFile!!.parent, "${config.logFileName}.1"))
            logFile = File(logFile!!.parent, config.logFileName)
            fileWriter = FileWriter(logFile, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 记录日志
     */
    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null, extra: Map<String, Any?> = emptyMap()) {
        if (level.priority < config.minLevel.priority) {
            return
        }

        val entry = LogEntry(
            id = currentId++,
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            extra = extra
        )

        logQueue.offer(entry)

        if (config.enableFileLog) {
            writeToFile(entry)
        }

        notifyListeners(entry)
    }

    /**
     * 写入文件
     */
    private fun writeToFile(entry: LogEntry) {
        try {
            val writer = fileWriter ?: return

            val dateStr = dateFormat.format(Date(entry.timestamp))
            val levelStr = entry.level.name.padEnd(5)
            val tagStr = entry.tag.padEnd(20)

            writer.append("$dateStr $levelStr $tagStr ${entry.message}\n")

            if (entry.throwable != null) {
                val pw = PrintWriter(writer)
                entry.throwable.printStackTrace(pw)
                pw.flush()
            }

            writer.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 通知监听器
     */
    private fun notifyListeners(entry: LogEntry) {
        listeners.forEach { listener ->
            try {
                listener(entry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 记录VERBOSE级别日志
     */
    fun v(tag: String, message: String, extra: Map<String, Any?> = emptyMap()) {
        log(LogLevel.VERBOSE, tag, message, extra = extra)
    }

    /**
     * 记录DEBUG级别日志
     */
    fun d(tag: String, message: String, extra: Map<String, Any?> = emptyMap()) {
        log(LogLevel.DEBUG, tag, message, extra = extra)
    }

    /**
     * 记录INFO级别日志
     */
    fun i(tag: String, message: String, extra: Map<String, Any?> = emptyMap()) {
        log(LogLevel.INFO, tag, message, extra = extra)
    }

    /**
     * 记录WARN级别日志
     */
    fun w(tag: String, message: String, extra: Map<String, Any?> = emptyMap()) {
        log(LogLevel.WARN, tag, message, extra = extra)
    }

    /**
     * 记录ERROR级别日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null, extra: Map<String, Any?> = emptyMap()) {
        log(LogLevel.ERROR, tag, message, throwable, extra)
    }

    /**
     * 记录FATAL级别日志
     */
    fun f(tag: String, message: String, throwable: Throwable? = null, extra: Map<String, Any?> = emptyMap()) {
        log(LogLevel.FATAL, tag, message, throwable, extra)
    }

    /**
     * 添加日志监听器
     */
    fun addListener(listener: (LogEntry) -> Unit) {
        listeners.add(listener)
    }

    /**
     * 移除日志监听器
     */
    fun removeListener(listener: (LogEntry) -> Unit) {
        listeners.remove(listener)
    }

    /**
     * 获取所有日志
     */
    fun getAllLogs(): List<LogEntry> {
        return logQueue.toList()
    }

    /**
     * 获取日志 (带过滤)
     */
    fun getLogs(filter: LogFilter): List<LogEntry> {
        return logQueue.filter { entry ->
            var match = true

            filter.minLevel?.let { match = match && entry.level.priority >= it.priority }
            filter.maxLevel?.let { match = match && entry.level.priority <= it.priority }
            filter.tags?.let { match = match && entry.tag in it }
            filter.messageContains?.let { match = match && entry.message.contains(it, ignoreCase = true) }
            filter.startTime?.let { match = match && entry.timestamp >= it }
            filter.endTime?.let { match = match && entry.timestamp <= it }

            match
        }
    }

    /**
     * 按级别获取日志
     */
    fun getLogsByLevel(level: LogLevel): List<LogEntry> {
        return logQueue.filter { it.level == level }
    }

    /**
     * 按标签获取日志
     */
    fun getLogsByTag(tag: String): List<LogEntry> {
        return logQueue.filter { it.tag == tag }
    }

    /**
     * 搜索日志
     */
    fun searchLogs(query: String): List<LogEntry> {
        return logQueue.filter {
            it.message.contains(query, ignoreCase = true) ||
                    it.tag.contains(query, ignoreCase = true)
        }
    }

    /**
     * 清空日志
     */
    fun clearLogs() {
        logQueue.clear()
    }

    /**
     * 裁剪日志数量
     */
    private fun trimLogs() {
        while (logQueue.size > config.maxEntries) {
            logQueue.poll()
        }
    }

    /**
     * 导出日志到文件
     * @param filePath 文件路径
     * @param filter 日志过滤器
     * @return 是否成功
     */
    fun exportLogs(filePath: String, filter: LogFilter? = null): Boolean {
        return try {
            val logs = if (filter != null) getLogs(filter) else getAllLogs()
            val file = File(filePath)
            file.parentFile?.mkdirs()

            FileWriter(file).use { writer ->
                for (entry in logs) {
                    val dateStr = dateFormat.format(Date(entry.timestamp))
                    val levelStr = entry.level.name.padEnd(5)
                    val tagStr = entry.tag.padEnd(20)

                    writer.append("$dateStr $levelStr $tagStr ${entry.message}\n")

                    if (entry.throwable != null) {
                        val pw = PrintWriter(writer)
                        entry.throwable.printStackTrace(pw)
                        pw.flush()
                    }
                }
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取日志统计
     */
    fun getStatistics(): Map<String, Any> {
        val logs = logQueue.toList()

        return mapOf(
            "totalCount" to logs.size,
            "verboseCount" to logs.count { it.level == LogLevel.VERBOSE },
            "debugCount" to logs.count { it.level == LogLevel.DEBUG },
            "infoCount" to logs.count { it.level == LogLevel.INFO },
            "warnCount" to logs.count { it.level == LogLevel.WARN },
            "errorCount" to logs.count { it.level == LogLevel.ERROR },
            "fatalCount" to logs.count { it.level == LogLevel.FATAL },
            "oldestTimestamp" to logs.minByOrNull { it.timestamp }?.timestamp,
            "newestTimestamp" to logs.maxByOrNull { it.timestamp }?.timestamp
        )
    }

    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String? {
        return logFile?.absolutePath
    }

    /**
     * 获取日志文件大小
     */
    fun getLogFileSize(): Long {
        return logFile?.length() ?: 0L
    }

    /**
     * 清理日志文件
     */
    fun clearLogFiles() {
        try {
            fileWriter?.close()
            logFile?.delete()

            val logDir = File(context.filesDir, config.logDirectory)
            logDir.listFiles()?.forEach { it.delete() }

            initLogFile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 释放资源
     */
    fun cleanup() {
        executor.shutdown()
        fileWriter?.close()
        listeners.clear()
        logQueue.clear()
    }
}
