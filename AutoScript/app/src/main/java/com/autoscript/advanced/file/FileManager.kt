package com.autoscript.advanced.file

import android.content.Context
import android.os.Environment
import java.io.*
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件管理器
 * 提供文件读写、目录管理、文件搜索等功能
 */
class FileManager(private val context: Context) {

    /**
     * 文件信息
     */
    data class FileInfo(
        val path: String,
        val name: String,
        val extension: String,
        val size: Long,
        val lastModified: Long,
        val isDirectory: Boolean,
        val isFile: Boolean,
        val isHidden: Boolean,
        val canRead: Boolean,
        val canWrite: Boolean
    )

    /**
     * 文件操作结果
     */
    data class FileResult(
        val success: Boolean,
        val data: Any? = null,
        val error: String? = null
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 获取应用私有文件目录
     * @return 文件目录路径
     */
    fun getPrivateFilesDir(): String {
        return context.filesDir.absolutePath
    }

    /**
     * 获取应用私有缓存目录
     * @return 缓存目录路径
     */
    fun getCacheDir(): String {
        return context.cacheDir.absolutePath
    }

    /**
     * 获取外部存储目录
     * @return 外部存储目录路径
     */
    fun getExternalStorageDir(): String? {
        return if (isExternalStorageAvailable()) {
            Environment.getExternalStorageDirectory().absolutePath
        } else {
            null
        }
    }

    /**
     * 检查外部存储是否可用
     * @return 是否可用
     */
    fun isExternalStorageAvailable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED
    }

    /**
     * 读取文件内容为字符串
     * @param filePath 文件路径
     * @param charset 字符编码
     * @return 文件内容
     */
    fun readText(filePath: String, charset: Charset = Charsets.UTF_8): String? {
        return try {
            File(filePath).readText(charset)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 读取文件内容为字节数组
     * @param filePath 文件路径
     * @return 字节数组
     */
    fun readBytes(filePath: String): ByteArray? {
        return try {
            File(filePath).readBytes()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 读取文件行列表
     * @param filePath 文件路径
     * @param charset 字符编码
     * @return 行列表
     */
    fun readLines(filePath: String, charset: Charset = Charsets.UTF_8): List<String>? {
        return try {
            File(filePath).readLines(charset)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 写入字符串到文件
     * @param filePath 文件路径
     * @param content 内容
     * @param append 是否追加
     * @param charset 字符编码
     * @return 是否成功
     */
    fun writeText(
        filePath: String,
        content: String,
        append: Boolean = false,
        charset: Charset = Charsets.UTF_8
    ): Boolean {
        return try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.writeText(content, charset)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 写入字节数组到文件
     * @param filePath 文件路径
     * @param bytes 字节数组
     * @param append 是否追加
     * @return 是否成功
     */
    fun writeBytes(filePath: String, bytes: ByteArray, append: Boolean = false): Boolean {
        return try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            if (append) {
                FileOutputStream(file, true).use { it.write(bytes) }
            } else {
                file.writeBytes(bytes)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 追加字符串到文件
     * @param filePath 文件路径
     * @param content 内容
     * @param charset 字符编码
     * @return 是否成功
     */
    fun appendText(filePath: String, content: String, charset: Charset = Charsets.UTF_8): Boolean {
        return writeText(filePath, content, true, charset)
    }

    /**
     * 创建文件
     * @param filePath 文件路径
     * @return 是否成功
     */
    fun createFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            file.createNewFile()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 创建目录
     * @param dirPath 目录路径
     * @return 是否成功
     */
    fun createDirectory(dirPath: String): Boolean {
        return try {
            val dir = File(dirPath)
            if (!dir.exists()) {
                dir.mkdirs()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除文件
     * @param filePath 文件路径
     * @return 是否成功
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除目录及其内容
     * @param dirPath 目录路径
     * @return 是否成功
     */
    fun deleteDirectory(dirPath: String): Boolean {
        return try {
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) {
                deleteRecursively(dir)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 递归删除
     */
    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        return file.delete()
    }

    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return 是否存在
     */
    fun exists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    /**
     * 检查是否为目录
     * @param path 路径
     * @return 是否为目录
     */
    fun isDirectory(path: String): Boolean {
        return File(path).isDirectory
    }

    /**
     * 检查是否为文件
     * @param path 路径
     * @return 是否为文件
     */
    fun isFile(path: String): Boolean {
        return File(path).isFile
    }

    /**
     * 获取文件信息
     * @param filePath 文件路径
     * @return 文件信息
     */
    fun getFileInfo(filePath: String): FileInfo? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            FileInfo(
                path = file.absolutePath,
                name = file.name,
                extension = file.extension,
                size = if (file.isFile) file.length() else 0,
                lastModified = file.lastModified(),
                isDirectory = file.isDirectory,
                isFile = file.isFile,
                isHidden = file.isHidden,
                canRead = file.canRead(),
                canWrite = file.canWrite()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取目录下的文件列表
     * @param dirPath 目录路径
     * @param recursive 是否递归
     * @return 文件列表
     */
    fun listFiles(dirPath: String, recursive: Boolean = false): List<FileInfo> {
        val result = mutableListOf<FileInfo>()
        try {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) return result

            dir.listFiles()?.forEach { file ->
                getFileInfo(file.absolutePath)?.let { result.add(it) }

                if (recursive && file.isDirectory) {
                    result.addAll(listFiles(file.absolutePath, true))
                }
            }
        } catch (e: Exception) {
        }

        return result
    }

    /**
     * 搜索文件
     * @param dirPath 目录路径
     * @param query 搜索关键词
     * @param recursive 是否递归搜索
     * @return 匹配的文件列表
     */
    fun searchFiles(dirPath: String, query: String, recursive: Boolean = true): List<FileInfo> {
        val result = mutableListOf<FileInfo>()
        try {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) return result

            dir.listFiles()?.forEach { file ->
                if (file.name.contains(query, ignoreCase = true)) {
                    getFileInfo(file.absolutePath)?.let { result.add(it) }
                }

                if (recursive && file.isDirectory) {
                    result.addAll(searchFiles(file.absolutePath, query, true))
                }
            }
        } catch (e: Exception) {
        }

        return result
    }

    /**
     * 按扩展名过滤文件
     * @param dirPath 目录路径
     * @param extensions 扩展名列表
     * @param recursive 是否递归
     * @return 匹配的文件列表
     */
    fun filterByExtension(
        dirPath: String,
        extensions: List<String>,
        recursive: Boolean = false
    ): List<FileInfo> {
        return listFiles(dirPath, recursive).filter { file ->
            extensions.any { ext -> file.extension.equals(ext, ignoreCase = true) }
        }
    }

    /**
     * 获取文件大小
     * @param filePath 文件路径
     * @return 文件大小 (字节)
     */
    fun getFileSize(filePath: String): Long {
        return try {
            val file = File(filePath)
            if (file.isFile) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 获取目录大小
     * @param dirPath 目录路径
     * @return 目录大小 (字节)
     */
    fun getDirectorySize(dirPath: String): Long {
        var size = 0L
        try {
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        size += file.length()
                    }
                }
            }
        } catch (e: Exception) {
        }

        return size
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
     * 重命名文件
     * @param oldPath 原路径
     * @param newName 新名称
     * @return 是否成功
     */
    fun rename(oldPath: String, newName: String): Boolean {
        return try {
            val oldFile = File(oldPath)
            val newFile = File(oldFile.parentFile, newName)
            oldFile.renameTo(newFile)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取文件扩展名
     * @param filePath 文件路径
     * @return 扩展名
     */
    fun getExtension(filePath: String): String {
        return File(filePath).extension
    }

    /**
     * 获取文件名 (不含扩展名)
     * @param filePath 文件路径
     * @return 文件名
     */
    fun getFileNameWithoutExtension(filePath: String): String {
        return File(filePath).nameWithoutExtension
    }

    /**
     * 获取文件名
     * @param filePath 文件路径
     * @return 文件名
     */
    fun getFileName(filePath: String): String {
        return File(filePath).name
    }

    /**
     * 获取父目录路径
     * @param filePath 文件路径
     * @return 父目录路径
     */
    fun getParentPath(filePath: String): String? {
        return File(filePath).parent
    }

    /**
     * 获取文件最后修改时间
     * @param filePath 文件路径
     * @return 格式化的时间字符串
     */
    fun getLastModified(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                dateFormat.format(Date(file.lastModified()))
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 设置文件最后修改时间
     * @param filePath 文件路径
     * @param time 时间戳
     * @return 是否成功
     */
    fun setLastModified(filePath: String, time: Long): Boolean {
        return try {
            File(filePath).setLastModified(time)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 清空目录内容
     * @param dirPath 目录路径
     * @return 是否成功
     */
    fun clearDirectory(dirPath: String): Boolean {
        return try {
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { it.deleteRecursively() }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取可用存储空间
     * @param path 路径
     * @return 可用空间 (字节)
     */
    fun getAvailableSpace(path: String): Long {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.freeSpace
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 获取总存储空间
     * @param path 路径
     * @return 总空间 (字节)
     */
    fun getTotalSpace(path: String): Long {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.totalSpace
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
}
