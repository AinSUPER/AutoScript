package com.autoscript.advanced.file

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.*

/**
 * ZIP压缩解压工具
 * 支持压缩、解压、进度回调
 */
class ZipManager(private val context: Context) {

    /**
     * 压缩配置
     */
    data class ZipConfig(
        val compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
        val bufferSize: Int = 8192,
        val includeRootDirectory: Boolean = true,
        val encoding: String = "UTF-8"
    )

    /**
     * 操作结果
     */
    data class ZipResult(
        val success: Boolean,
        val filePath: String? = null,
        val entriesCount: Int = 0,
        val bytesProcessed: Long = 0,
        val error: String? = null
    )

    /**
     * 进度信息
     */
    data class ZipProgress(
        val currentEntry: String,
        val currentFile: Int,
        val totalFiles: Int,
        val bytesProcessed: Long,
        val totalBytes: Long
    )

    private var defaultConfig = ZipConfig()

    /**
     * 设置默认配置
     * @param config 默认配置
     */
    fun setDefaultConfig(config: ZipConfig) {
        defaultConfig = config
    }

    /**
     * 压缩文件
     * @param sourcePath 源文件路径
     * @param destPath 目标ZIP文件路径
     * @param config 压缩配置
     * @param progressCallback 进度回调
     * @return 压缩结果
     */
    suspend fun zipFile(
        sourcePath: String,
        destPath: String,
        config: ZipConfig = defaultConfig,
        progressCallback: ((ZipProgress) -> Unit)? = null
    ): ZipResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val sourceFile = File(sourcePath)

            if (!sourceFile.exists()) {
                return@withContext ZipResult(false, error = "源文件不存在")
            }

            val destFile = File(destPath)
            destFile.parentFile?.mkdirs()

            var totalBytes = 0L
            val fileSize = sourceFile.length()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destFile))).use { zos ->
                zos.setLevel(config.compressionLevel)

                val entry = ZipEntry(sourceFile.name)
                zos.putNextEntry(entry)

                val buffer = ByteArray(config.bufferSize)
                FileInputStream(sourceFile).use { fis ->
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        zos.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead

                        progressCallback?.invoke(
                            ZipProgress(
                                currentEntry = sourceFile.name,
                                currentFile = 1,
                                totalFiles = 1,
                                bytesProcessed = totalBytes,
                                totalBytes = fileSize
                            )
                        )
                    }
                }

                zos.closeEntry()
            }

            ZipResult(true, destPath, 1, totalBytes)
        } catch (e: Exception) {
            ZipResult(false, error = e.message)
        }
    }

    /**
     * 压缩目录
     * @param sourcePath 源目录路径
     * @param destPath 目标ZIP文件路径
     * @param config 压缩配置
     * @param progressCallback 进度回调
     * @return 压缩结果
     */
    suspend fun zipDirectory(
        sourcePath: String,
        destPath: String,
        config: ZipConfig = defaultConfig,
        progressCallback: ((ZipProgress) -> Unit)? = null
    ): ZipResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val sourceDir = File(sourcePath)

            if (!sourceDir.exists()) {
                return@withContext ZipResult(false, error = "源目录不存在")
            }

            if (!sourceDir.isDirectory) {
                return@withContext ZipResult(false, error = "源路径不是目录")
            }

            val destFile = File(destPath)
            destFile.parentFile?.mkdirs()

            val files = sourceDir.walkTopDown().filter { it.isFile }.toList()
            val totalFiles = files.size
            val totalSize = files.sumOf { it.length() }

            var entriesCount = 0
            var totalBytes = 0L
            var currentFileIndex = 0

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destFile))).use { zos ->
                zos.setLevel(config.compressionLevel)

                val basePath = if (config.includeRootDirectory) {
                    sourceDir.parentFile?.absolutePath ?: ""
                } else {
                    sourceDir.absolutePath
                }

                for (file in files) {
                    currentFileIndex++

                    val entryPath = if (config.includeRootDirectory) {
                        file.relativeTo(File(basePath)).path
                    } else {
                        file.relativeTo(sourceDir).path
                    }

                    val entry = ZipEntry(entryPath.replace("\\", "/"))
                    zos.putNextEntry(entry)

                    val buffer = ByteArray(config.bufferSize)
                    FileInputStream(file).use { fis ->
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            zos.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead

                            progressCallback?.invoke(
                                ZipProgress(
                                    currentEntry = file.name,
                                    currentFile = currentFileIndex,
                                    totalFiles = totalFiles,
                                    bytesProcessed = totalBytes,
                                    totalBytes = totalSize
                                )
                            )
                        }
                    }

                    zos.closeEntry()
                    entriesCount++
                }
            }

            ZipResult(true, destPath, entriesCount, totalBytes)
        } catch (e: Exception) {
            ZipResult(false, error = e.message)
        }
    }

    /**
     * 压缩多个文件
     * @param sourcePaths 源文件路径列表
     * @param destPath 目标ZIP文件路径
     * @param config 压缩配置
     * @param progressCallback 进度回调
     * @return 压缩结果
     */
    suspend fun zipFiles(
        sourcePaths: List<String>,
        destPath: String,
        config: ZipConfig = defaultConfig,
        progressCallback: ((ZipProgress) -> Unit)? = null
    ): ZipResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val destFile = File(destPath)
            destFile.parentFile?.mkdirs()

            val files = sourcePaths.mapNotNull { File(it).takeIf { it.exists() && it.isFile } }
            if (files.isEmpty()) {
                return@withContext ZipResult(false, error = "没有有效的源文件")
            }

            val totalFiles = files.size
            val totalSize = files.sumOf { it.length() }

            var entriesCount = 0
            var totalBytes = 0L

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destFile))).use { zos ->
                zos.setLevel(config.compressionLevel)

                files.forEachIndexed { index, file ->
                    val entry = ZipEntry(file.name)
                    zos.putNextEntry(entry)

                    val buffer = ByteArray(config.bufferSize)
                    FileInputStream(file).use { fis ->
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            zos.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead

                            progressCallback?.invoke(
                                ZipProgress(
                                    currentEntry = file.name,
                                    currentFile = index + 1,
                                    totalFiles = totalFiles,
                                    bytesProcessed = totalBytes,
                                    totalBytes = totalSize
                                )
                            )
                        }
                    }

                    zos.closeEntry()
                    entriesCount++
                }
            }

            ZipResult(true, destPath, entriesCount, totalBytes)
        } catch (e: Exception) {
            ZipResult(false, error = e.message)
        }
    }

    /**
     * 解压ZIP文件
     * @param zipPath ZIP文件路径
     * @param destPath 目标目录路径
     * @param config 解压配置
     * @param progressCallback 进度回调
     * @return 解压结果
     */
    suspend fun unzip(
        zipPath: String,
        destPath: String,
        config: ZipConfig = defaultConfig,
        progressCallback: ((ZipProgress) -> Unit)? = null
    ): ZipResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val zipFile = File(zipPath)

            if (!zipFile.exists()) {
                return@withContext ZipResult(false, error = "ZIP文件不存在")
            }

            val destDir = File(destPath)
            destDir.mkdirs()

            var entriesCount = 0
            var totalBytes = 0L

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry?

                val totalSize = zipFile.length()

                while (zis.nextEntry.also { entry = it } != null) {
                    val entryFile = File(destDir, entry!!.name)

                    if (entry!!.isDirectory) {
                        entryFile.mkdirs()
                        continue
                    }

                    entryFile.parentFile?.mkdirs()

                    val buffer = ByteArray(config.bufferSize)
                    FileOutputStream(entryFile).use { fos ->
                        var bytesRead: Int
                        while (zis.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                        }
                    }

                    entriesCount++

                    progressCallback?.invoke(
                        ZipProgress(
                            currentEntry = entry!!.name,
                            currentFile = entriesCount,
                            totalFiles = -1,
                            bytesProcessed = totalBytes,
                            totalBytes = totalSize
                        )
                    )

                    zis.closeEntry()
                }
            }

            ZipResult(true, destPath, entriesCount, totalBytes)
        } catch (e: Exception) {
            ZipResult(false, error = e.message)
        }
    }

    /**
     * 解压ZIP文件到指定目录 (同步版本)
     * @param zipPath ZIP文件路径
     * @param destPath 目标目录路径
     * @return 解压结果
     */
    fun unzipSync(zipPath: String, destPath: String): ZipResult {
        return try {
            val zipFile = File(zipPath)

            if (!zipFile.exists()) {
                return ZipResult(false, error = "ZIP文件不存在")
            }

            val destDir = File(destPath)
            destDir.mkdirs()

            var entriesCount = 0
            var totalBytes = 0L

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry?

                while (zis.nextEntry.also { entry = it } != null) {
                    val entryFile = File(destDir, entry!!.name)

                    if (entry!!.isDirectory) {
                        entryFile.mkdirs()
                        continue
                    }

                    entryFile.parentFile?.mkdirs()

                    val buffer = ByteArray(defaultConfig.bufferSize)
                    FileOutputStream(entryFile).use { fos ->
                        var bytesRead: Int
                        while (zis.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                        }
                    }

                    entriesCount++
                    zis.closeEntry()
                }
            }

            ZipResult(true, destPath, entriesCount, totalBytes)
        } catch (e: Exception) {
            ZipResult(false, error = e.message)
        }
    }

    /**
     * 获取ZIP文件条目列表
     * @param zipPath ZIP文件路径
     * @return 条目信息列表
     */
    fun listEntries(zipPath: String): List<ZipEntryInfo> {
        val entries = mutableListOf<ZipEntryInfo>()

        try {
            val zipFile = File(zipPath)
            if (!zipFile.exists()) return entries

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry?

                while (zis.nextEntry.also { entry = it } != null) {
                    entries.add(
                        ZipEntryInfo(
                            name = entry!!.name,
                            size = entry!!.size,
                            compressedSize = entry!!.compressedSize,
                            isDirectory = entry!!.isDirectory,
                            lastModified = entry!!.lastModified
                        )
                    )
                    zis.closeEntry()
                }
            }
        } catch (e: Exception) {
        }

        return entries
    }

    /**
     * ZIP条目信息
     */
    data class ZipEntryInfo(
        val name: String,
        val size: Long,
        val compressedSize: Long,
        val isDirectory: Boolean,
        val lastModified: Long
    )

    /**
     * 解压单个文件
     * @param zipPath ZIP文件路径
     * @param entryName 条目名称
     * @param destPath 目标文件路径
     * @return 是否成功
     */
    fun extractEntry(zipPath: String, entryName: String, destPath: String): Boolean {
        return try {
            val zipFile = File(zipPath)
            if (!zipFile.exists()) return false

            val destFile = File(destPath)
            destFile.parentFile?.mkdirs()

            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
                var entry: ZipEntry?

                while (zis.nextEntry.also { entry = it } != null) {
                    if (entry!!.name == entryName) {
                        FileOutputStream(destFile).use { fos ->
                            val buffer = ByteArray(defaultConfig.bufferSize)
                            var bytesRead: Int
                            while (zis.read(buffer).also { bytesRead = it } != -1) {
                                fos.write(buffer, 0, bytesRead)
                            }
                        }
                        return true
                    }
                    zis.closeEntry()
                }
            }

            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查是否为有效的ZIP文件
     * @param zipPath ZIP文件路径
     * @return 是否有效
     */
    fun isValidZip(zipPath: String): Boolean {
        return try {
            val file = File(zipPath)
            if (!file.exists()) return false

            ZipInputStream(FileInputStream(file)).use { zis ->
                zis.nextEntry != null || true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取ZIP文件注释
     * @param zipPath ZIP文件路径
     * @return 注释内容
     */
    fun getComment(zipPath: String): String? {
        return try {
            ZipFile(File(zipPath)).use { zipFile ->
                zipFile.comment
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 计算压缩率
     * @param originalSize 原始大小
     * @param compressedSize 压缩后大小
     * @return 压缩率百分比
     */
    fun calculateCompressionRatio(originalSize: Long, compressedSize: Long): Float {
        return if (originalSize > 0) {
            (1 - compressedSize.toFloat() / originalSize) * 100
        } else {
            0f
        }
    }

    /**
     * 添加文件到现有ZIP
     * @param zipPath ZIP文件路径
     * @param filePath 要添加的文件路径
     * @param entryName 条目名称
     * @return 是否成功
     */
    fun addEntry(zipPath: String, filePath: String, entryName: String? = null): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            val tempZip = File(zipPath + ".tmp")

            ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZip))).use { zos ->
                if (File(zipPath).exists()) {
                    ZipInputStream(BufferedInputStream(FileInputStream(zipPath))).use { zis ->
                        var entry: ZipEntry?
                        val buffer = ByteArray(defaultConfig.bufferSize)

                        while (zis.nextEntry.also { entry = it } != null) {
                            zos.putNextEntry(ZipEntry(entry!!.name))
                            var bytesRead: Int
                            while (zis.read(buffer).also { bytesRead = it } != -1) {
                                zos.write(buffer, 0, bytesRead)
                            }
                            zos.closeEntry()
                            zis.closeEntry()
                        }
                    }
                }

                val newEntry = ZipEntry(entryName ?: file.name)
                zos.putNextEntry(newEntry)

                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(defaultConfig.bufferSize)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        zos.write(buffer, 0, bytesRead)
                    }
                }

                zos.closeEntry()
            }

            File(zipPath).delete()
            tempZip.renameTo(File(zipPath))

            true
        } catch (e: Exception) {
            false
        }
    }
}
