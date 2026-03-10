package com.autoscript.advanced.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 文件下载器
 * 支持断点续传、多任务下载、进度回调
 */
class FileDownloader(private val context: Context) {

    /**
     * 下载配置
     */
    data class DownloadConfig(
        val connectTimeout: Long = 30000,
        val readTimeout: Long = 30000,
        val bufferSize: Int = 8192,
        val enableResume: Boolean = true,
        val maxRetries: Int = 3,
        val headers: Map<String, String> = emptyMap()
    )

    /**
     * 下载结果
     */
    data class DownloadResult(
        val success: Boolean,
        val filePath: String? = null,
        val fileSize: Long = 0,
        val error: String? = null
    )

    /**
     * 下载进度
     */
    data class DownloadProgress(
        val downloadId: String,
        val url: String,
        val totalBytes: Long,
        val downloadedBytes: Long,
        val progress: Float,
        val speed: Long,
        val isCompleted: Boolean
    )

    /**
     * 下载任务
     */
    data class DownloadTask(
        val id: String,
        val url: String,
        val filePath: String,
        val totalBytes: Long = 0,
        val downloadedBytes: Long = 0,
        val status: Status = Status.PENDING,
        val startTime: Long = System.currentTimeMillis()
    ) {
        enum class Status {
            PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
        }
    }

    private var defaultConfig = DownloadConfig()
    private val downloadTasks = mutableMapOf<String, DownloadTask>()
    private val progressCallbacks = mutableMapOf<String, (DownloadProgress) -> Unit>()
    private val cancelledTasks = mutableSetOf<String>()

    /**
     * 设置默认配置
     * @param config 默认配置
     */
    fun setDefaultConfig(config: DownloadConfig) {
        defaultConfig = config
    }

    /**
     * 下载文件
     * @param url 下载URL
     * @param destPath 目标路径
     * @param config 下载配置
     * @param progressCallback 进度回调
     * @return 下载结果
     */
    suspend fun download(
        url: String,
        destPath: String,
        config: DownloadConfig = defaultConfig,
        progressCallback: ((DownloadProgress) -> Unit)? = null
    ): DownloadResult = withContext(Dispatchers.IO) {
        val downloadId = generateDownloadId(url)

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        return@withContext try {
            val destFile = File(destPath)
            val tempFile = File(destPath + ".tmp")

            val downloadedBytes = if (config.enableResume && tempFile.exists()) {
                tempFile.length()
            } else {
                if (tempFile.exists()) tempFile.delete()
                0L
            }

            val downloadUrl = URL(url)
            connection = downloadUrl.openConnection() as HttpURLConnection

            connection.apply {
                connectTimeout = config.connectTimeout.toInt()
                readTimeout = config.readTimeout.toInt()
                requestMethod = "GET"

                for ((key, value) in config.headers) {
                    setRequestProperty(key, value)
                }

                if (config.enableResume && downloadedBytes > 0) {
                    setRequestProperty("Range", "bytes=$downloadedBytes-")
                }
            }

            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK &&
                responseCode != HttpURLConnection.HTTP_PARTIAL
            ) {
                return@withContext DownloadResult(
                    success = false,
                    error = "HTTP错误: $responseCode"
                )
            }

            val totalBytes = if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                downloadedBytes + (connection.contentLengthLong)
            } else {
                connection.contentLengthLong
            }

            val task = DownloadTask(
                id = downloadId,
                url = url,
                filePath = destPath,
                totalBytes = totalBytes,
                downloadedBytes = downloadedBytes,
                status = DownloadTask.Status.DOWNLOADING
            )
            downloadTasks[downloadId] = task

            if (progressCallback != null) {
                progressCallbacks[downloadId] = progressCallback
            }

            inputStream = connection.inputStream
            outputStream = FileOutputStream(tempFile, config.enableResume && downloadedBytes > 0)

            val buffer = ByteArray(config.bufferSize)
            var bytesRead: Int
            var lastProgressTime = System.currentTimeMillis()
            var lastBytes = downloadedBytes
            var currentSpeed = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (cancelledTasks.contains(downloadId)) {
                    cancelledTasks.remove(downloadId)
                    downloadTasks[downloadId] = task.copy(status = DownloadTask.Status.CANCELLED)
                    return@withContext DownloadResult(
                        success = false,
                        error = "下载已取消"
                    )
                }

                outputStream.write(buffer, 0, bytesRead)

                val currentDownloaded = downloadTasks[downloadId]?.downloadedBytes?.plus(bytesBytes) ?: downloadedBytes + bytesRead
                downloadTasks[downloadId] = task.copy(downloadedBytes = currentDownloaded)

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastProgressTime >= 500) {
                    currentSpeed = ((currentDownloaded - lastBytes) * 1000 / (currentTime - lastProgressTime))
                    lastProgressTime = currentTime
                    lastBytes = currentDownloaded

                    progressCallbacks[downloadId]?.invoke(
                        DownloadProgress(
                            downloadId = downloadId,
                            url = url,
                            totalBytes = totalBytes,
                            downloadedBytes = currentDownloaded,
                            progress = if (totalBytes > 0) currentDownloaded.toFloat() / totalBytes else 0f,
                            speed = currentSpeed,
                            isCompleted = false
                        )
                    )
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            if (tempFile.exists()) {
                tempFile.renameTo(destFile)
            }

            downloadTasks[downloadId] = task.copy(
                status = DownloadTask.Status.COMPLETED,
                downloadedBytes = totalBytes
            )

            progressCallbacks[downloadId]?.invoke(
                DownloadProgress(
                    downloadId = downloadId,
                    url = url,
                    totalBytes = totalBytes,
                    downloadedBytes = totalBytes,
                    progress = 1f,
                    speed = currentSpeed,
                    isCompleted = true
                )
            )

            DownloadResult(
                success = true,
                filePath = destPath,
                fileSize = totalBytes
            )
        } catch (e: Exception) {
            val task = downloadTasks[downloadId]
            if (task != null) {
                downloadTasks[downloadId] = task.copy(status = DownloadTask.Status.FAILED)
            }
            DownloadResult(
                success = false,
                error = e.message
            )
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
                connection?.disconnect()
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 取消下载
     * @param downloadId 下载ID
     */
    fun cancel(downloadId: String) {
        cancelledTasks.add(downloadId)
    }

    /**
     * 取消所有下载
     */
    fun cancelAll() {
        downloadTasks.keys.forEach { cancelledTasks.add(it) }
    }

    /**
     * 获取下载任务
     * @param downloadId 下载ID
     * @return 下载任务
     */
    fun getTask(downloadId: String): DownloadTask? {
        return downloadTasks[downloadId]
    }

    /**
     * 获取所有下载任务
     * @return 下载任务列表
     */
    fun getAllTasks(): List<DownloadTask> {
        return downloadTasks.values.toList()
    }

    /**
     * 删除下载任务
     * @param downloadId 下载ID
     */
    fun removeTask(downloadId: String) {
        downloadTasks.remove(downloadId)
        progressCallbacks.remove(downloadId)
    }

    /**
     * 生成下载ID
     */
    private fun generateDownloadId(url: String): String {
        return url.hashCode().toString(16) + "_" + System.currentTimeMillis()
    }

    /**
     * 获取文件大小
     * @param url 文件URL
     * @return 文件大小 (字节)
     */
    suspend fun getFileSize(url: String): Long = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connect()

            val size = if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.contentLengthLong
            } else {
                -1L
            }

            connection.disconnect()
            size
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * 检查是否支持断点续传
     * @param url 文件URL
     * @return 是否支持
     */
    suspend fun supportsResume(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connect()

            val acceptRanges = connection.getHeaderField("Accept-Ranges")
            connection.disconnect()

            acceptRanges == "bytes"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 同步下载文件
     * @param url 下载URL
     * @param destPath 目标路径
     * @param config 下载配置
     * @return 下载结果
     */
    fun downloadSync(
        url: String,
        destPath: String,
        config: DownloadConfig = defaultConfig
    ): DownloadResult {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        return try {
            val destFile = File(destPath)
            val parentDir = destFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            val downloadUrl = URL(url)
            connection = downloadUrl.openConnection() as HttpURLConnection

            connection.apply {
                connectTimeout = config.connectTimeout.toInt()
                readTimeout = config.readTimeout.toInt()
                requestMethod = "GET"

                for ((key, value) in config.headers) {
                    setRequestProperty(key, value)
                }
            }

            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return DownloadResult(
                    success = false,
                    error = "HTTP错误: ${connection.responseCode}"
                )
            }

            val totalBytes = connection.contentLengthLong

            inputStream = connection.inputStream
            outputStream = FileOutputStream(destFile)

            val buffer = ByteArray(config.bufferSize)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()

            DownloadResult(
                success = true,
                filePath = destPath,
                fileSize = totalBytes
            )
        } catch (e: Exception) {
            DownloadResult(
                success = false,
                error = e.message
            )
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
                connection?.disconnect()
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 批量下载
     * @param downloads 下载列表 (URL, 目标路径)
     * @param config 下载配置
     * @param progressCallback 进度回调
     * @return 下载结果列表
     */
    suspend fun downloadBatch(
        downloads: List<Pair<String, String>>,
        config: DownloadConfig = defaultConfig,
        progressCallback: ((Int, Int, DownloadResult) -> Unit)? = null
    ): List<DownloadResult> {
        val results = mutableListOf<DownloadResult>()

        downloads.forEachIndexed { index, (url, destPath) ->
            val result = download(url, destPath, config)
            results.add(result)
            progressCallback?.invoke(index + 1, downloads.size, result)
        }

        return results
    }

    /**
     * 格式化文件大小
     * @param bytes 字节数
     * @return 格式化字符串
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    /**
     * 格式化下载速度
     * @param bytesPerSecond 每秒字节数
     * @return 格式化字符串
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatFileSize(bytesPerSecond)}/s"
    }
}
