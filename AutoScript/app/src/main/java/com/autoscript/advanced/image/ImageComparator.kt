package com.autoscript.advanced.image

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect

/**
 * 颜色识别工具
 * 提供颜色提取、分析、转换等功能
 */
class ColorPicker {

    /**
     * 颜色信息
     */
    data class ColorInfo(
        val color: Int,
        val hex: String,
        val rgb: Triple<Int, Int, Int>,
        val hsv: FloatArray,
        val alpha: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ColorInfo
            if (color != other.color) return false
            return true
        }

        override fun hashCode(): Int = color

        override fun toString(): String {
            return "ColorInfo(color=$color, hex='$hex', rgb=$rgb, hsv=${hsv.contentToString()}, alpha=$alpha)"
        }
    }

    /**
     * 颜色匹配结果
     */
    data class ColorMatchResult(
        val matched: Boolean,
        val similarity: Float,
        val targetColor: ColorInfo,
        val actualColor: ColorInfo
    )

    /**
     * 从位图获取指定点的颜色
     * @param bitmap 位图
     * @param x X坐标
     * @param y Y坐标
     * @return 颜色信息
     */
    fun pickColor(bitmap: Bitmap, x: Int, y: Int): ColorInfo? {
        return try {
            if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) {
                return null
            }

            val color = bitmap.getPixel(x, y)
            analyzeColor(color)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从位图获取区域平均颜色
     * @param bitmap 位图
     * @param rect 区域
     * @return 颜色信息
     */
    fun pickAverageColor(bitmap: Bitmap, rect: Rect): ColorInfo? {
        return try {
            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var totalA = 0L
            var count = 0

            for (y in rect.top.coerceAtLeast(0) until rect.bottom.coerceAtMost(bitmap.height)) {
                for (x in rect.left.coerceAtLeast(0) until rect.right.coerceAtMost(bitmap.width)) {
                    val color = bitmap.getPixel(x, y)
                    totalR += Color.red(color)
                    totalG += Color.green(color)
                    totalB += Color.blue(color)
                    totalA += Color.alpha(color)
                    count++
                }
            }

            if (count == 0) return null

            val avgColor = Color.argb(
                (totalA / count).toInt(),
                (totalR / count).toInt(),
                (totalG / count).toInt(),
                (totalB / count).toInt()
            )

            analyzeColor(avgColor)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 分析颜色
     * @param color 颜色值
     * @return 颜色信息
     */
    fun analyzeColor(color: Int): ColorInfo {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val a = Color.alpha(color)

        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)

        val hex = String.format("#%02X%02X%02X%02X", a, r, g, b)

        return ColorInfo(
            color = color,
            hex = hex,
            rgb = Triple(r, g, b),
            hsv = hsv,
            alpha = a
        )
    }

    /**
     * 从十六进制字符串解析颜色
     * @param hex 十六进制字符串 (如 #RRGGBB 或 #AARRGGBB)
     * @return 颜色值
     */
    fun parseHexColor(hex: String): Int? {
        return try {
            var hexString = hex.trim()
            if (hexString.startsWith("#")) {
                hexString = hexString.substring(1)
            }

            when (hexString.length) {
                6 -> Color.parseColor("#$hexString")
                8 -> Color.parseColor("#$hexString")
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 颜色匹配
     * @param actualColor 实际颜色
     * @param targetColor 目标颜色
     * @param tolerance 容差 (0-255)
     * @return 匹配结果
     */
    fun matchColor(actualColor: Int, targetColor: Int, tolerance: Int = 30): ColorMatchResult {
        val actualInfo = analyzeColor(actualColor)
        val targetInfo = analyzeColor(targetColor)

        val rDiff = kotlin.math.abs(actualInfo.rgb.first - targetInfo.rgb.first)
        val gDiff = kotlin.math.abs(actualInfo.rgb.second - targetInfo.rgb.second)
        val bDiff = kotlin.math.abs(actualInfo.rgb.third - targetInfo.rgb.third)

        val maxDiff = 255f * 3
        val totalDiff = (rDiff + gDiff + bDiff).toFloat()
        val similarity = 1f - (totalDiff / maxDiff)

        val matched = rDiff <= tolerance && gDiff <= tolerance && bDiff <= tolerance

        return ColorMatchResult(
            matched = matched,
            similarity = similarity,
            targetColor = targetInfo,
            actualColor = actualInfo
        )
    }

    /**
     * 在位图中查找颜色
     * @param bitmap 位图
     * @param targetColor 目标颜色
     * @param tolerance 容差
     * @param searchArea 搜索区域 (null表示全图)
     * @return 匹配点的列表
     */
    fun findColor(
        bitmap: Bitmap,
        targetColor: Int,
        tolerance: Int = 30,
        searchArea: Rect? = null
    ): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()

        val startX = searchArea?.left ?: 0
        val startY = searchArea?.top ?: 0
        val endX = searchArea?.right ?: bitmap.width
        val endY = searchArea?.bottom ?: bitmap.height

        for (y in startY until endY) {
            for (x in startX until endX) {
                val pixel = bitmap.getPixel(x, y)
                if (matchColor(pixel, targetColor, tolerance).matched) {
                    result.add(Pair(x, y))
                }
            }
        }

        return result
    }

    /**
     * 在位图中查找第一个匹配的颜色点
     * @param bitmap 位图
     * @param targetColor 目标颜色
     * @param tolerance 容差
     * @param searchArea 搜索区域
     * @return 第一个匹配点，未找到返回null
     */
    fun findFirstColor(
        bitmap: Bitmap,
        targetColor: Int,
        tolerance: Int = 30,
        searchArea: Rect? = null
    ): Pair<Int, Int>? {
        val startX = searchArea?.left ?: 0
        val startY = searchArea?.top ?: 0
        val endX = searchArea?.right ?: bitmap.width
        val endY = searchArea?.bottom ?: bitmap.height

        for (y in startY until endY) {
            for (x in startX until endX) {
                val pixel = bitmap.getPixel(x, y)
                if (matchColor(pixel, targetColor, tolerance).matched) {
                    return Pair(x, y)
                }
            }
        }

        return null
    }

    /**
     * 检查多点颜色是否匹配
     * @param bitmap 位图
     * @param points 颜色点列表 (x, y, color)
     * @param tolerance 容差
     * @return 是否全部匹配
     */
    fun checkMultiPointColor(
        bitmap: Bitmap,
        points: List<Triple<Int, Int, Int>>,
        tolerance: Int = 30
    ): Boolean {
        for ((x, y, color) in points) {
            if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) {
                return false
            }
            val pixel = bitmap.getPixel(x, y)
            if (!matchColor(pixel, color, tolerance).matched) {
                return false
            }
        }
        return true
    }

    /**
     * 获取位图的主要颜色
     * @param bitmap 位图
     * @param colorCount 返回的颜色数量
     * @return 主要颜色列表
     */
    fun getDominantColors(bitmap: Bitmap, colorCount: Int = 5): List<ColorInfo> {
        val colorMap = mutableMapOf<Int, Int>()

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val color = bitmap.getPixel(x, y)
                colorMap[color] = (colorMap[color] ?: 0) + 1
            }
        }

        return colorMap.entries
            .sortedByDescending { it.value }
            .take(colorCount)
            .map { analyzeColor(it.key) }
    }

    /**
     * RGB转HSV
     * @param r 红色分量 (0-255)
     * @param g 绿色分量 (0-255)
     * @param b 蓝色分量 (0-255)
     * @return HSV数组 [H, S, V]
     */
    fun rgbToHsv(r: Int, g: Int, b: Int): FloatArray {
        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        return hsv
    }

    /**
     * HSV转RGB
     * @param h 色相 (0-360)
     * @param s 饱和度 (0-1)
     * @param v 明度 (0-1)
     * @return RGB三元组
     */
    fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int> {
        val color = Color.HSVToColor(floatArrayOf(h, s, v))
        return Triple(Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * 计算颜色亮度
     * @param color 颜色值
     * @return 亮度值 (0-255)
     */
    fun getLuminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f

        return (0.299f * r + 0.587f * g + 0.114f * b) * 255
    }

    /**
     * 判断颜色是否为深色
     * @param color 颜色值
     * @return 是否为深色
     */
    fun isDarkColor(color: Int): Boolean {
        return getLuminance(color) < 128
    }

    /**
     * 获取对比色
     * @param color 原颜色
     * @return 对比色
     */
    fun getContrastColor(color: Int): Int {
        return if (isDarkColor(color)) Color.WHITE else Color.BLACK
    }

    /**
     * 混合两种颜色
     * @param color1 颜色1
     * @param color2 颜色2
     * @param ratio 混合比例 (0-1, 0表示完全使用color1, 1表示完全使用color2)
     * @return 混合后的颜色
     */
    fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1 - ratio

        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        val a = (Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio).toInt()

        return Color.argb(a, r, g, b)
    }
}
