package com.autoscript.advanced.file

import android.content.Context
import java.io.*
import java.nio.channels.FileChannel

/**
 * 文件工具类
 * 提供复制、移动、删除等操作
 */
class FileUtils(private val context: Context) {

    /**
     * 操作结果
     */
    data class OperationResult(
        val success: Boolean,
        val message: String? = null,
        val bytesProcessed: Long = 0
    )

    /**
     * 复制文件
     * @param sourcePath 源文件路径
     * @param destPath 目标文件路径
     * @param overwrite 是否覆盖已存在的文件
     * @return 操作结果
     */
    fun copyFile(sourcePath: String, destPath: String, overwrite: Boolean = true): OperationResult {
        return try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)

            if (!sourceFile.exists()) {
                return OperationResult(false, "源文件不存在")
            }

            if (!sourceFile.isFile) {
                return OperationResult(false, "源路径不是文件")
            }

            if (destFile.exists() && !overwrite) {
                return OperationResult(false, "目标文件已存在")
            }

            destFile.parentFile?.mkdirs()

            var bytesCopied = 0L

            FileInputStream(sourceFile).use { source ->
                FileOutputStream(destFile).use { dest ->
                    val channelSource = source.channel
                    val channelDest = dest.channel

                    bytesCopied = channelDest.transferFrom(channelSource, 0, channelSource.size())
                }
            }

            OperationResult(true, "复制成功", bytesCopied)
        } catch (e: Exception) {
            OperationResult(false, e.message)
        }
    }

    /**
     * 复制目录
     * @param sourcePath 源目录路径
     * @param destPath 目标目录路径
     * @param overwrite 是否覆盖已存在的文件
     * @return 操作结果
     */
    fun copyDirectory(sourcePath: String, destPath: String, overwrite: Boolean = true): OperationResult {
        return try {
            val sourceDir = File(sourcePath)
            val destDir = File(destPath)

            if (!sourceDir.exists()) {
                return OperationResult(false, "源目录不存在")
            }

            if (!sourceDir.isDirectory) {
                return OperationResult(false, "源路径不是目录")
            }

            destDir.mkdirs()

            var totalBytes = 0L

            sourceDir.walkTopDown().forEach { file ->
                val relativePath = file.relativeTo(sourceDir).path
                val destFile = File(destDir, relativePath)

                if (file.isDirectory) {
                    destFile.mkdirs()
                } else {
                    val result = copyFile(file.absolutePath, destFile.absolutePath, overwrite)
                    if (!result.success) {
                        return OperationResult(false, "复制文件失败: ${file.name}")
                    }
                    totalBytes += result.bytesProcessed
                }
            }

            OperationResult(true, "复制成功", totalBytes)
        } catch (e: Exception) {
            OperationResult(false, e.message)
        }
    }

    /**
     * 移动文件
     * @param sourcePath 源文件路径
     * @param destPath 目标文件路径
     * @param overwrite 是否覆盖已存在的文件
     * @return 操作结果
     */
    fun moveFile(sourcePath: String, destPath: String, overwrite: Boolean = true): OperationResult {
        return try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)

            if (!sourceFile.exists()) {
                return OperationResult(false, "源文件不存在")
            }

            if (!sourceFile.isFile) {
                return OperationResult(false, "源路径不是文件")
            }

            if (destFile.exists() && !overwrite) {
                return OperationResult(false, "目标文件已存在")
            }

            destFile.parentFile?.mkdirs()

            val fileSize = sourceFile.length()

            val moved = sourceFile.renameTo(destFile)

            if (moved) {
                OperationResult(true, "移动成功", fileSize)
            } else {
                val copyResult = copyFile(sourcePath, destPath, overwrite)
                if (copyResult.success) {
                    sourceFile.delete()
                    OperationResult(true, "移动成功", copyResult.bytesProcessed)
                } else {
                    OperationResult(false, "移动失败")
                }
            }
        } catch (e: Exception) {
            OperationResult(false, e.message)
        }
    }

    /**
     * 移动目录
     * @param sourcePath 源目录路径
     * @param destPath 目标目录路径
     * @param overwrite 是否覆盖已存在的文件
     * @return 操作结果
     */
    fun moveDirectory(sourcePath: String, destPath: String, overwrite: Boolean = true): OperationResult {
        return try {
            val sourceDir = File(sourcePath)
            val destDir = File(destPath)

            if (!sourceDir.exists()) {
                return OperationResult(false, "源目录不存在")
            }

            if (!sourceDir.isDirectory) {
                return OperationResult(false, "源路径不是目录")
            }

            val moved = sourceDir.renameTo(destDir)

            if (moved) {
                OperationResult(true, "移动成功")
            } else {
                val copyResult = copyDirectory(sourcePath, destPath, overwrite)
                if (copyResult.success) {
                    deleteDirectory(sourcePath)
                    OperationResult(true, "移动成功", copyResult.bytesProcessed)
                } else {
                    OperationResult(false, "移动失败")
                }
            }
        } catch (e: Exception) {
            OperationResult(false, e.message)
        }
    }

    /**
     * 删除文件
     * @param filePath 文件路径
     * @return 操作结果
     */
    fun deleteFile(filePath: String): OperationResult {
        return try {
            val file = File(filePath)

            if (!file.exists()) {
                return OperationResult(false, "文件不存在")
            }

            if (!file.isFile) {
                return OperationResult(false, "路径不是文件")
            }

            val deleted = file.delete()

            if (deleted) {
                OperationResult(true, "删除成功")
            } else {
                OperationResult(false, "删除失败")
            }
        } catch (e: Exception) {
            OperationResult(false, e.message)
        }
    }

    /**
     * 删除目录
     * @param dirPath 目录路径
     * @return 操作结果
     */
    fun deleteDirectory(dirPath: String): OperationResult {
        return try {
            val dir = File(dirPath)

            if (!dir.exists()) {
                return OperationResult(false, "目录不存在")
            }

            if (!dir.isDirectory) {
                return OperationResult(false, "路径不是目录")
            }

            val deleted = dir.deleteRecursively()

            if (deleted) {
                OperationResult(true, "删除成功")
            } else {
                OperationResult(false, "删除失败")
            }
        } catch (e: Exception) {
            OperationResult(false, e.message)
        }
    }

    /**
     * 批量删除文件
     * @param filePaths 文件路径列表
     * @return 操作结果
     */
    fun deleteFiles(filePaths: List<String>): OperationResult {
        var successCount = 0
        var failCount = 0

        for (path in filePaths) {
            val file = File(path)
            if (file.exists()) {
                if (file.deleteRecursively()) {
                    successCount++
                } else {
                    failCount++
                }
            }
        }

        return OperationResult(
            success = failCount == 0,
            message = "成功: $successCount, 失败: $failCount"
        )
    }

    /**
     * 创建快捷方式 (复制文件并添加后缀)
     * @param filePath 文件路径
     * @param suffix 后缀
     * @return 操作结果
     */
    fun createBackup(filePath: String, suffix: String = ".bak"): OperationResult {
        return try {
            val file = File(filePath)

            if (!file.exists()) {
                return OperationResult(false, "文件不存在")
            }

            val backupPath = filePath + suffix
            copyFile(filePath, backupPath)
        } catch (e: Exception) {
            OperationResult(false, e.message)
        }
    }

    /**
     * 比较两个文件内容
     * @param file1Path 文件1路径
     * @param file2Path 文件2路径
     * @return 是否相同
     */
    fun compareFiles(file1Path: String, file2Path: String): Boolean {
        return try {
            val file1 = File(file1Path)
            val file2 = File(file2Path)

            if (!file1.exists() || !file2.exists()) {
                return false
            }

            if (file1.length() != file2.length()) {
                return false
            }

            FileInputStream(file1).use { fis1 ->
                FileInputStream(file2).use { fis2 ->
                    val buffer1 = ByteArray(8192)
                    val buffer2 = ByteArray(8192)

                    while (true) {
                        val read1 = fis1.read(buffer1)
                        val read2 = fis2.read(buffer2)

                        if (read1 != read2) return false
                        if (read1 == -1) break

                        if (!buffer1.contentEquals(buffer2)) return false
                    }
                }
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 合并文件
     * @param sourceFiles 源文件列表
     * @param destPath 目标文件路径
     * @return 操作结果
     */
    fun mergeFiles(sourceFiles: List<String>, destPath: String): OperationResult {
        return try {
            val destFile = File(destPath)
            destFile.parentFile?.mkdirs()

            var totalBytes = 0L

            FileOutputStream(destFile).use { fos ->
                for (sourcePath in sourceFiles) {
                    val sourceFile = File(sourcePath)
                    if (sourceFile.exists() && sourceFile.isFile) {
                        FileInputStream(sourceFile).use { fis ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int

                            while (fis.read(buffer).also { bytesRead = it } != -1) {
                                fos.write(buffer, 0, bytesRead)
                                totalBytes += bytesRead
                            }
                        }
                    }
                }
            }

            OperationResult(true, "合并成功", totalBytes)
        } catch (e: Exception) {
            OperationResult(false, e.message)
        }
    }

    /**
     * 分割文件
     * @param filePath 文件路径
     * @param partSize 每部分大小 (字节)
     * @param outputDir 输出目录
     * @return 操作结果
     */
    fun splitFile(filePath: String, partSize: Long, outputDir: String? = null): OperationResult {
        return try {
            val sourceFile = File(filePath)

            if (!sourceFile.exists()) {
                return OperationResult(false, "文件不存在")
            }

            val dir = outputDir?.let { File(it) } ?: sourceFile.parentFile
            dir?.mkdirs()

            val totalSize = sourceFile.length()
            var bytesProcessed = 0L
            var partIndex = 0

            FileInputStream(sourceFile).use { fis ->
                val buffer = ByteArray(minOf(partSize.toInt(), 8192 * 1024))

                while (bytesProcessed < totalSize) {
                    val partFile = File(dir, "${sourceFile.nameWithoutExtension}.part$partIndex")
                    FileOutputStream(partFile).use { fos ->
                        var bytesWritten = 0L

                        while (bytesWritten < partSize) {
                            val toRead = minOf(buffer.size.toLong(), partSize - bytesWritten).toInt()
                            val bytesRead = fis.read(buffer, 0, toRead)

                            if (bytesRead == -1) break

                            fos.write(buffer, 0, bytesRead)
                            bytesWritten += bytesRead
                            bytesProcessed += bytesRead
                        }
                    }
                    partIndex++
                }
            }

            OperationResult(true, "分割成功，共 $partIndex 个文件", bytesProcessed)
        } catch (e: Exception) {
            OperationResult(false, e.message)
        }
    }

    /**
     * 获取文件MD5值
     * @param filePath 文件路径
     * @return MD5字符串
     */
    fun getMD5(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val md = java.security.MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }

            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取文件SHA256值
     * @param filePath 文件路径
     * @return SHA256字符串
     */
    fun getSHA256(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val md = java.security.MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }

            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 查找重复文件
     * @param dirPath 目录路径
     * @return 重复文件列表 (MD5 -> 文件路径列表)
     */
    fun findDuplicateFiles(dirPath: String): Map<String, List<String>> {
        val fileHashes = mutableMapOf<String, MutableList<String>>()

        try {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) return emptyMap()

            dir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    getMD5(file.absolutePath)?.let { md5 ->
                        fileHashes.getOrPut(md5) { mutableListOf() }
                            .add(file.absolutePath)
                    }
                }
        } catch (e: Exception) {
        }

        return fileHashes.filter { it.value.size > 1 }
    }

    /**
     * 同步文件内容
     * @param sourcePath 源文件路径
     * @param destPath 目标文件路径
     * @return 操作结果
     */
    fun syncFile(sourcePath: String, destPath: String): OperationResult {
        return try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)

            if (!sourceFile.exists()) {
                return OperationResult(false, "源文件不存在")
            }

            if (destFile.exists()) {
                if (sourceFile.lastModified() <= destFile.lastModified()) {
                    return OperationResult(true, "目标文件已是最新")
                }
            }

            copyFile(sourcePath, destPath, true)
        } catch (e: Exception) {
            OperationResult(false, e.message)
        }
    }

    /**
     * 清理空目录
     * @param dirPath 目录路径
     * @return 清理的目录数量
     */
    fun cleanEmptyDirectories(dirPath: String): Int {
        var count = 0

        try {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) return 0

            dir.walkBottomUp()
                .filter { it.isDirectory && it.listFiles()?.isEmpty() == true }
                .forEach { emptyDir ->
                    if (emptyDir.delete()) {
                        count++
                    }
                }
        } catch (e: Exception) {
        }

        return count
    }
}
