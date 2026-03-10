package com.autoscript.advanced.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import java.io.File
import java.io.FileOutputStream

/**
 * 图片裁剪工具
 * 提供图片裁剪、旋转、缩放等功能
 */
class ImageCropper(private val context: Context) {

    /**
     * 裁剪配置
     */
    data class CropConfig(
        val x: Int = 0,
        val y: Int = 0,
        val width: Int = 0,
        val height: Int = 0,
        val keepAspectRatio: Boolean = false,
        val aspectRatio: Pair<Float, Float> = Pair(1f, 1f)
    )

    /**
     * 裁剪结果
     */
    data class CropResult(
        val success: Boolean,
        val bitmap: Bitmap? = null,
        val filePath: String? = null,
        val error: String? = null
    )

    /**
     * 从文件加载图片
     * @param filePath 文件路径
     * @return 位图对象
     */
    fun loadFromFile(filePath: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(filePath)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从资源加载图片
     * @param resId 资源ID
     * @return 位图对象
     */
    fun loadFromResource(resId: Int): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, resId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 按矩形区域裁剪图片
     * @param bitmap 源位图
     * @param rect 裁剪区域
     * @return 裁剪后的位图
     */
    fun cropByRect(bitmap: Bitmap, rect: Rect): Bitmap? {
        return try {
            val left = rect.left.coerceAtLeast(0)
            val top = rect.top.coerceAtLeast(0)
            val right = rect.right.coerceAtMost(bitmap.width)
            val bottom = rect.bottom.coerceAtMost(bitmap.height)

            Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 按配置裁剪图片
     * @param bitmap 源位图
     * @param config 裁剪配置
     * @return 裁剪结果
     */
    fun crop(bitmap: Bitmap, config: CropConfig): CropResult {
        return try {
            var width = config.width
            var height = config.height

            if (config.keepAspectRatio) {
                val (ratioW, ratioH) = config.aspectRatio
                val targetRatio = ratioW / ratioH
                val currentRatio = width.toFloat() / height.toFloat()

                if (currentRatio > targetRatio) {
                    width = (height * targetRatio).toInt()
                } else {
                    height = (width / targetRatio).toInt()
                }
            }

            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                config.x.coerceAtLeast(0),
                config.y.coerceAtLeast(0),
                width.coerceAtMost(bitmap.width - config.x),
                height.coerceAtMost(bitmap.height - config.y)
            )

            CropResult(true, croppedBitmap)
        } catch (e: Exception) {
            CropResult(false, error = e.message)
        }
    }

    /**
     * 按中心点裁剪正方形
     * @param bitmap 源位图
     * @param size 正方形边长
     * @return 裁剪后的位图
     */
    fun cropCenterSquare(bitmap: Bitmap, size: Int): Bitmap? {
        return try {
            val startX = (bitmap.width - size) / 2
            val startY = (bitmap.height - size) / 2

            Bitmap.createBitmap(
                bitmap,
                startX.coerceAtLeast(0),
                startY.coerceAtLeast(0),
                size.coerceAtMost(bitmap.width),
                size.coerceAtMost(bitmap.height)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 按比例裁剪
     * @param bitmap 源位图
     * @param widthRatio 宽度比例
     * @param heightRatio 高度比例
     * @return 裁剪后的位图
     */
    fun cropByRatio(bitmap: Bitmap, widthRatio: Float, heightRatio: Float): Bitmap? {
        return try {
            val targetRatio = widthRatio / heightRatio
            val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

            val (cropWidth, cropHeight) = if (currentRatio > targetRatio) {
                val h = bitmap.height
                val w = (h * targetRatio).toInt()
                Pair(w, h)
            } else {
                val w = bitmap.width
                val h = (w / targetRatio).toInt()
                Pair(w, h)
            }

            val startX = (bitmap.width - cropWidth) / 2
            val startY = (bitmap.height - cropHeight) / 2

            Bitmap.createBitmap(bitmap, startX, startY, cropWidth, cropHeight)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 旋转图片
     * @param bitmap 源位图
     * @param degrees 旋转角度
     * @return 旋转后的位图
     */
    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap? {
        return try {
            val matrix = Matrix()
            matrix.postRotate(degrees)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 翻转图片
     * @param bitmap 源位图
     * @param horizontal 是否水平翻转
     * @param vertical 是否垂直翻转
     * @return 翻转后的位图
     */
    fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap? {
        return try {
            val matrix = Matrix()
            matrix.postScale(
                if (horizontal) -1f else 1f,
                if (vertical) -1f else 1f,
                bitmap.width / 2f,
                bitmap.height / 2f
            )
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 缩放图片
     * @param bitmap 源位图
     * @param newWidth 新宽度
     * @param newHeight 新高度
     * @return 缩放后的位图
     */
    fun resize(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap? {
        return try {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 按比例缩放图片
     * @param bitmap 源位图
     * @param scale 缩放比例
     * @return 缩放后的位图
     */
    fun scale(bitmap: Bitmap, scale: Float): Bitmap? {
        return try {
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 添加边框
     * @param bitmap 源位图
     * @param borderWidth 边框宽度
     * @param borderColor 边框颜色
     * @return 添加边框后的位图
     */
    fun addBorder(bitmap: Bitmap, borderWidth: Int, borderColor: Int = Color.WHITE): Bitmap? {
        return try {
            val newWidth = bitmap.width + borderWidth * 2
            val newHeight = bitmap.height + borderWidth * 2

            val result = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            val paint = Paint()
            paint.color = borderColor
            canvas.drawRect(0f, 0f, newWidth.toFloat(), newHeight.toFloat(), paint)

            canvas.drawBitmap(bitmap, borderWidth.toFloat(), borderWidth.toFloat(), null)

            result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 圆角裁剪
     * @param bitmap 源位图
     * @param radius 圆角半径
     * @return 圆角位图
     */
    fun roundCorners(bitmap: Bitmap, radius: Float): Bitmap? {
        return try {
            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            val paint = Paint()
            paint.isAntiAlias = true
            paint.color = Color.WHITE

            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val rectF = android.graphics.RectF(rect)
            canvas.drawRoundRect(rectF, radius, radius, paint)

            paint.xfermode = android.graphics.PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 应用颜色滤镜
     * @param bitmap 源位图
     * @param color 滤镜颜色
     * @return 应用滤镜后的位图
     */
    fun applyColorFilter(bitmap: Bitmap, color: Int): Bitmap? {
        return try {
            val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            val paint = Paint()
            paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            canvas.drawBitmap(result, 0f, 0f, paint)
            result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 保存位图到文件
     * @param bitmap 位图
     * @param filePath 文件路径
     * @param format 图片格式
     * @param quality 质量
     * @return 是否成功
     */
    fun saveToFile(
        bitmap: Bitmap,
        filePath: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): Boolean {
        return try {
            val file = File(filePath)
            FileOutputStream(file).use { out ->
                bitmap.compress(format, quality, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 释放位图资源
     * @param bitmap 要释放的位图
     */
    fun recycle(bitmap: Bitmap?) {
        bitmap?.recycle()
    }
}
