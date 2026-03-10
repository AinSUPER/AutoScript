package com.autoscript.advanced.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Environment
import android.view.View
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 截图管理器
 * 支持全屏截图和区域截图
 */
class ScreenshotManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * 截图结果
     */
    data class ScreenshotResult(
        val success: Boolean,
        val bitmap: Bitmap? = null,
        val filePath: String? = null,
        val error: String? = null
    )

    /**
     * 截图配置
     */
    data class ScreenshotConfig(
        val saveToGallery: Boolean = true,
        val quality: Int = 100,
        val format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        val fileName: String? = null
    )

    /**
     * 获取屏幕尺寸
     */
    fun getScreenSize(): Pair<Int, Int> {
        val displayMetrics = android.util.DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    /**
     * 获取全屏截图
     * @param rootView 根视图
     * @param config 截图配置
     * @return 截图结果
     */
    fun takeFullscreenScreenshot(rootView: View, config: ScreenshotConfig = ScreenshotConfig()): ScreenshotResult {
        return try {
            val (width, height) = getScreenSize()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            rootView.draw(canvas)

            if (config.saveToGallery) {
                val filePath = saveBitmap(bitmap, config)
                ScreenshotResult(true, bitmap, filePath)
            } else {
                ScreenshotResult(true, bitmap)
            }
        } catch (e: Exception) {
            ScreenshotResult(false, error = e.message)
        }
    }

    /**
     * 获取区域截图
     * @param rootView 根视图
     * @param rect 截图区域
     * @param config 截图配置
     * @return 截图结果
     */
    fun takeRegionScreenshot(rootView: View, rect: Rect, config: ScreenshotConfig = ScreenshotConfig()): ScreenshotResult {
        return try {
            val (width, height) = getScreenSize()
            val fullBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(fullBitmap)
            rootView.draw(fullBitmap)

            val regionBitmap = Bitmap.createBitmap(
                fullBitmap,
                rect.left.coerceAtLeast(0),
                rect.top.coerceAtLeast(0),
                rect.width().coerceAtMost(width - rect.left),
                rect.height().coerceAtMost(height - rect.top)
            )
            fullBitmap.recycle()

            if (config.saveToGallery) {
                val filePath = saveBitmap(regionBitmap, config)
                ScreenshotResult(true, regionBitmap, filePath)
            } else {
                ScreenshotResult(true, regionBitmap)
            }
        } catch (e: Exception) {
            ScreenshotResult(false, error = e.message)
        }
    }

    /**
     * 获取指定视图的截图
     * @param view 目标视图
     * @param config 截图配置
     * @return 截图结果
     */
    fun takeViewScreenshot(view: View, config: ScreenshotConfig = ScreenshotConfig()): ScreenshotResult {
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            if (config.saveToGallery) {
                val filePath = saveBitmap(bitmap, config)
                ScreenshotResult(true, bitmap, filePath)
            } else {
                ScreenshotResult(true, bitmap)
            }
        } catch (e: Exception) {
            ScreenshotResult(false, error = e.message)
        }
    }

    /**
     * 从Bitmap创建区域截图
     * @param sourceBitmap 源位图
     * @param rect 截图区域
     * @return 裁剪后的位图
     */
    fun cropBitmap(sourceBitmap: Bitmap, rect: Rect): Bitmap? {
        return try {
            Bitmap.createBitmap(
                sourceBitmap,
                rect.left.coerceAtLeast(0),
                rect.top.coerceAtLeast(0),
                rect.width().coerceAtMost(sourceBitmap.width - rect.left),
                rect.height().coerceAtMost(sourceBitmap.height - rect.top)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 保存位图到文件
     * @param bitmap 位图
     * @param config 截图配置
     * @return 文件路径
     */
    private fun saveBitmap(bitmap: Bitmap, config: ScreenshotConfig): String {
        val fileName = config.fileName ?: "screenshot_${dateFormat.format(Date())}"
        val extension = when (config.format) {
            Bitmap.CompressFormat.PNG -> ".png"
            Bitmap.CompressFormat.JPEG -> ".jpg"
            Bitmap.CompressFormat.WEBP -> ".webp"
            else -> ".png"
        }

        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "AutoScript"
        )
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, fileName + extension)
        FileOutputStream(file).use { out ->
            bitmap.compress(config.format, config.quality, out)
        }

        return file.absolutePath
    }

    /**
     * 截图并返回Base64字符串
     * @param rootView 根视图
     * @return Base64编码的图片字符串
     */
    fun takeScreenshotAsBase64(rootView: View): String? {
        return try {
            val (width, height) = getScreenSize()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            rootView.draw(canvas)

            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            bitmap.recycle()

            android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 缩放截图
     * @param bitmap 源位图
     * @param scale 缩放比例
     * @return 缩放后的位图
     */
    fun scaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 释放位图资源
     * @param bitmap 要释放的位图
     */
    fun recycleBitmap(bitmap: Bitmap?) {
        bitmap?.recycle()
    }
}
