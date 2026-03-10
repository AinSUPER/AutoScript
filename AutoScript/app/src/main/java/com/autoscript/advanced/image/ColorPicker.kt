package com.autoscript.advanced.image

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect

/**
 * 图片比对工具
 * 提供图片相似度计算、差异检测、模板匹配等功能
 */
class ImageComparator {

    /**
     * 比对结果
     */
    data class CompareResult(
        val similarity: Float,
        val matchPoints: Int,
        val totalPoints: Int,
        val matchPercentage: Float
    )

    /**
     * 差异区域
     */
    data class DiffRegion(
        val rect: Rect,
        val diffPercentage: Float
    )

    /**
     * 模板匹配结果
     */
    data class TemplateMatchResult(
        val found: Boolean,
        val x: Int = 0,
        val y: Int = 0,
        val width: Int = 0,
        val height: Int = 0,
        val similarity: Float = 0f
    )

    /**
     * 比对配置
     */
    data class CompareConfig(
        val threshold: Float = 0.9f,
        val pixelTolerance: Int = 10,
        val ignoreAlpha: Boolean = false,
        val compareRegion: Rect? = null
    )

    /**
     * 计算两张图片的相似度
     * @param bitmap1 图片1
     * @param bitmap2 图片2
     * @return 相似度 (0-1)
     */
    fun calculateSimilarity(bitmap1: Bitmap, bitmap2: Bitmap): Float {
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
            return 0f
        }

        var matchCount = 0
        val totalPixels = bitmap1.width * bitmap1.height

        for (y in 0 until bitmap1.height) {
            for (x in 0 until bitmap1.width) {
                val pixel1 = bitmap1.getPixel(x, y)
                val pixel2 = bitmap2.getPixel(x, y)

                if (isColorSimilar(pixel1, pixel2, 10)) {
                    matchCount++
                }
            }
        }

        return matchCount.toFloat() / totalPixels
    }

    /**
     * 比较两张图片
     * @param bitmap1 图片1
     * @param bitmap2 图片2
     * @param config 比对配置
     * @return 比对结果
     */
    fun compare(bitmap1: Bitmap, bitmap2: Bitmap, config: CompareConfig = CompareConfig()): CompareResult {
        val region = config.compareRegion
        val width = region?.width() ?: minOf(bitmap1.width, bitmap2.width)
        val height = region?.height() ?: minOf(bitmap1.height, bitmap2.height)

        var matchPoints = 0
        var totalPoints = 0

        val startX = region?.left ?: 0
        val startY = region?.top ?: 0

        for (y in startY until startY + height) {
            for (x in startX until startX + width) {
                if (x >= bitmap1.width || x >= bitmap2.width ||
                    y >= bitmap1.height || y >= bitmap2.height
                ) continue

                totalPoints++

                val pixel1 = bitmap1.getPixel(x, y)
                val pixel2 = bitmap2.getPixel(x, y)

                if (isColorSimilar(pixel1, pixel2, config.pixelTolerance, config.ignoreAlpha)) {
                    matchPoints++
                }
            }
        }

        val similarity = if (totalPoints > 0) matchPoints.toFloat() / totalPoints else 0f

        return CompareResult(
            similarity = similarity,
            matchPoints = matchPoints,
            totalPoints = totalPoints,
            matchPercentage = similarity * 100
        )
    }

    /**
     * 检查颜色是否相似
     * @param color1 颜色1
     * @param color2 颜色2
     * @param tolerance 容差
     * @param ignoreAlpha 是否忽略透明度
     * @return 是否相似
     */
    private fun isColorSimilar(color1: Int, color2: Int, tolerance: Int, ignoreAlpha: Boolean = false): Boolean {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        val rDiff = kotlin.math.abs(r1 - r2)
        val gDiff = kotlin.math.abs(g1 - g2)
        val bDiff = kotlin.math.abs(b1 - b2)

        if (rDiff > tolerance || gDiff > tolerance || bDiff > tolerance) {
            return false
        }

        if (!ignoreAlpha) {
            val a1 = Color.alpha(color1)
            val a2 = Color.alpha(color2)
            if (kotlin.math.abs(a1 - a2) > tolerance) {
                return false
            }
        }

        return true
    }

    /**
     * 查找差异区域
     * @param bitmap1 图片1
     * @param bitmap2 图片2
     * @param threshold 差异阈值
     * @return 差异区域列表
     */
    fun findDiffRegions(bitmap1: Bitmap, bitmap2: Bitmap, threshold: Float = 0.1f): List<DiffRegion> {
        val diffRegions = mutableListOf<DiffRegion>()
        val width = minOf(bitmap1.width, bitmap2.width)
        val height = minOf(bitmap1.height, bitmap2.height)

        val blockSize = 20
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                var diffCount = 0
                var totalCount = 0

                for (by in y until minOf(y + blockSize, height)) {
                    for (bx in x until minOf(x + blockSize, width)) {
                        totalCount++
                        if (!isColorSimilar(bitmap1.getPixel(bx, by), bitmap2.getPixel(bx, by), 30)) {
                            diffCount++
                        }
                    }
                }

                val diffPercentage = if (totalCount > 0) diffCount.toFloat() / totalCount else 0f
                if (diffPercentage > threshold) {
                    diffRegions.add(
                        DiffRegion(
                            rect = Rect(x, y, minOf(x + blockSize, width), minOf(y + blockSize, height)),
                            diffPercentage = diffPercentage
                        )
                    )
                }
                x += blockSize
            }
            y += blockSize
        }

        return mergeAdjacentRegions(diffRegions)
    }

    /**
     * 合并相邻区域
     */
    private fun mergeAdjacentRegions(regions: List<DiffRegion>): List<DiffRegion> {
        if (regions.isEmpty()) return regions

        val merged = mutableListOf<DiffRegion>()
        val used = mutableSetOf<Int>()

        for (i in regions.indices) {
            if (used.contains(i)) continue

            var currentRect = regions[i].rect
            used.add(i)

            for (j in i + 1 until regions.size) {
                if (used.contains(j)) continue

                val otherRect = regions[j].rect
                if (areAdjacent(currentRect, otherRect)) {
                    currentRect = Rect(
                        minOf(currentRect.left, otherRect.left),
                        minOf(currentRect.top, otherRect.top),
                        maxOf(currentRect.right, otherRect.right),
                        maxOf(currentRect.bottom, otherRect.bottom)
                    )
                    used.add(j)
                }
            }

            merged.add(DiffRegion(currentRect, 1f))
        }

        return merged
    }

    /**
     * 检查两个矩形是否相邻
     */
    private fun areAdjacent(rect1: Rect, rect2: Rect): Boolean {
        return rect1.left <= rect2.right + 20 &&
                rect1.right >= rect2.left - 20 &&
                rect1.top <= rect2.bottom + 20 &&
                rect1.bottom >= rect2.top - 20
    }

    /**
     * 模板匹配 - 在大图中查找小图
     * @param source 源图片
     * @param template 模板图片
     * @param threshold 相似度阈值
     * @return 匹配结果
     */
    fun findTemplate(
        source: Bitmap,
        template: Bitmap,
        threshold: Float = 0.8f
    ): TemplateMatchResult {
        if (template.width > source.width || template.height > source.height) {
            return TemplateMatchResult(false)
        }

        var bestX = 0
        var bestY = 0
        var bestSimilarity = 0f

        val stepX = maxOf(1, template.width / 10)
        val stepY = maxOf(1, template.height / 10)

        for (y in 0..(source.height - template.height) step stepY) {
            for (x in 0..(source.width - template.width) step stepX) {
                val similarity = calculateRegionSimilarity(source, template, x, y)

                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity
                    bestX = x
                    bestY = y

                    if (similarity >= threshold) {
                        return TemplateMatchResult(
                            found = true,
                            x = x,
                            y = y,
                            width = template.width,
                            height = template.height,
                            similarity = similarity
                        )
                    }
                }
            }
        }

        return TemplateMatchResult(
            found = bestSimilarity >= threshold,
            x = bestX,
            y = bestY,
            width = template.width,
            height = template.height,
            similarity = bestSimilarity
        )
    }

    /**
     * 查找所有匹配的模板位置
     * @param source 源图片
     * @param template 模板图片
     * @param threshold 相似度阈值
     * @return 所有匹配位置
     */
    fun findAllTemplates(
        source: Bitmap,
        template: Bitmap,
        threshold: Float = 0.8f
    ): List<TemplateMatchResult> {
        val results = mutableListOf<TemplateMatchResult>()

        if (template.width > source.width || template.height > source.height) {
            return results
        }

        val stepX = maxOf(1, template.width / 5)
        val stepY = maxOf(1, template.height / 5)

        for (y in 0..(source.height - template.height) step stepY) {
            for (x in 0..(source.width - template.width) step stepX) {
                val similarity = calculateRegionSimilarity(source, template, x, y)

                if (similarity >= threshold) {
                    results.add(
                        TemplateMatchResult(
                            found = true,
                            x = x,
                            y = y,
                            width = template.width,
                            height = template.height,
                            similarity = similarity
                        )
                    )
                }
            }
        }

        return removeOverlappingResults(results)
    }

    /**
     * 计算区域相似度
     */
    private fun calculateRegionSimilarity(source: Bitmap, template: Bitmap, startX: Int, startY: Int): Float {
        var matchCount = 0
        val totalPixels = template.width * template.height

        for (y in 0 until template.height) {
            for (x in 0 until template.width) {
                val sourcePixel = source.getPixel(startX + x, startY + y)
                val templatePixel = template.getPixel(x, y)

                if (isColorSimilar(sourcePixel, templatePixel, 30)) {
                    matchCount++
                }
            }
        }

        return matchCount.toFloat() / totalPixels
    }

    /**
     * 移除重叠的匹配结果
     */
    private fun removeOverlappingResults(results: List<TemplateMatchResult>): List<TemplateMatchResult> {
        if (results.isEmpty()) return results

        val sorted = results.sortedByDescending { it.similarity }
        val filtered = mutableListOf<TemplateMatchResult>()

        for (result in sorted) {
            var overlaps = false
            for (existing in filtered) {
                if (isOverlapping(result, existing)) {
                    overlaps = true
                    break
                }
            }
            if (!overlaps) {
                filtered.add(result)
            }
        }

        return filtered
    }

    /**
     * 检查两个匹配结果是否重叠
     */
    private fun isOverlapping(r1: TemplateMatchResult, r2: TemplateMatchResult): Boolean {
        return r1.x < r2.x + r2.width &&
                r1.x + r1.width > r2.x &&
                r1.y < r2.y + r2.height &&
                r1.y + r1.height > r2.y
    }

    /**
     * 生成差异图片
     * @param bitmap1 图片1
     * @param bitmap2 图片2
     * @return 差异图片 (差异部分用红色标记)
     */
    fun generateDiffBitmap(bitmap1: Bitmap, bitmap2: Bitmap): Bitmap? {
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
            return null
        }

        val diffBitmap = bitmap1.copy(Bitmap.Config.ARGB_8888, true)

        for (y in 0 until diffBitmap.height) {
            for (x in 0 until diffBitmap.width) {
                if (!isColorSimilar(bitmap1.getPixel(x, y), bitmap2.getPixel(x, y), 30)) {
                    diffBitmap.setPixel(x, y, Color.RED)
                }
            }
        }

        return diffBitmap
    }

    /**
     * 快速图片比对 (使用采样)
     * @param bitmap1 图片1
     * @param bitmap2 图片2
     * @param sampleRate 采样率 (0-1)
     * @return 相似度
     */
    fun quickCompare(bitmap1: Bitmap, bitmap2: Bitmap, sampleRate: Float = 0.1f): Float {
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
            return 0f
        }

        val step = maxOf(1, (1 / sampleRate).toInt())
        var matchCount = 0
        var totalCount = 0

        var y = 0
        while (y < bitmap1.height) {
            var x = 0
            while (x < bitmap1.width) {
                totalCount++
                if (isColorSimilar(bitmap1.getPixel(x, y), bitmap2.getPixel(x, y), 30)) {
                    matchCount++
                }
                x += step
            }
            y += step
        }

        return if (totalCount > 0) matchCount.toFloat() / totalCount else 0f
    }
}
