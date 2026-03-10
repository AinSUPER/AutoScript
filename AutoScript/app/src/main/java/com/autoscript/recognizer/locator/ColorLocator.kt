package com.autoscript.recognizer.locator

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import com.autoscript.model.recognizer.*

/**
 * 颜色定位器
 * 支持指定位置颜色识别、颜色范围匹配和多点颜色检测
 */
class ColorLocator {
    
    /**
     * 执行颜色定位
     */
    fun locate(bitmap: Bitmap, config: ColorConfig): RecognizeResult {
        return try {
            if (config.points.isEmpty()) {
                locateByColorRange(bitmap, config)
            } else {
                locateByMultiPoints(bitmap, config)
            }
        } catch (e: Exception) {
            RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_SERVICE_UNAVAILABLE,
                errorMessage = "颜色定位失败: ${e.message}"
            )
        }
    }
    
    /**
     * 通过颜色范围定位
     */
    private fun locateByColorRange(bitmap: Bitmap, config: ColorConfig): RecognizeResult {
        val matchingPoints = mutableListOf<Point>()
        
        val targetRed = Color.red(config.targetColor)
        val targetGreen = Color.green(config.targetColor)
        val targetBlue = Color.blue(config.targetColor)
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixelColor = bitmap.getPixel(x, y)
                val red = Color.red(pixelColor)
                val green = Color.green(pixelColor)
                val blue = Color.blue(pixelColor)
                
                if (isColorMatch(red, green, blue, targetRed, targetGreen, targetBlue, config.tolerance)) {
                    matchingPoints.add(Point(x, y))
                }
            }
        }
        
        return if (matchingPoints.isNotEmpty()) {
            val bounds = calculateBounds(matchingPoints)
            val center = calculateCenter(matchingPoints)
            
            val elementInfo = ElementInfo.Builder()
                .bounds(bounds)
                .build()
            
            val confidence = matchingPoints.size.toFloat() / (bitmap.width * bitmap.height)
            
            RecognizeResult.Success(
                confidence = confidence.coerceIn(0f, 1f),
                elementInfo = elementInfo
            )
        } else {
            RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
                errorMessage = "未找到匹配的颜色"
            )
        }
    }
    
    /**
     * 通过多点颜色检测定位
     */
    private fun locateByMultiPoints(bitmap: Bitmap, config: ColorConfig): RecognizeResult {
        val matchedPoints = mutableListOf<Point>()
        var allMatched = true
        
        val targetRed = Color.red(config.targetColor)
        val targetGreen = Color.green(config.targetColor)
        val targetBlue = Color.blue(config.targetColor)
        
        for (colorPoint in config.points) {
            val x = if (colorPoint.isRelative) {
                (bitmap.width * colorPoint.x / 100f).toInt()
            } else {
                colorPoint.x
            }
            
            val y = if (colorPoint.isRelative) {
                (bitmap.height * colorPoint.y / 100f).toInt()
            } else {
                colorPoint.y
            }
            
            if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) {
                allMatched = false
                continue
            }
            
            val pixelColor = bitmap.getPixel(x, y)
            val red = Color.red(pixelColor)
            val green = Color.green(pixelColor)
            val blue = Color.blue(pixelColor)
            
            if (isColorMatch(red, green, blue, targetRed, targetGreen, targetBlue, config.tolerance)) {
                matchedPoints.add(Point(x, y))
            } else {
                allMatched = false
            }
        }
        
        return if (config.matchAllPoints) {
            if (allMatched && matchedPoints.size == config.points.size) {
                val bounds = calculateBounds(matchedPoints)
                val elementInfo = ElementInfo.Builder()
                    .bounds(bounds)
                    .build()
                
                RecognizeResult.Success(
                    confidence = 1.0f,
                    elementInfo = elementInfo
                )
            } else {
                RecognizeResult.Failure(
                    errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
                    errorMessage = "多点颜色检测未全部匹配"
                )
            }
        } else {
            if (matchedPoints.isNotEmpty()) {
                val bounds = calculateBounds(matchedPoints)
                val elementInfo = ElementInfo.Builder()
                    .bounds(bounds)
                    .build()
                
                val confidence = matchedPoints.size.toFloat() / config.points.size
                RecognizeResult.Success(
                    confidence = confidence,
                    elementInfo = elementInfo
                )
            } else {
                RecognizeResult.Failure(
                    errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
                    errorMessage = "多点颜色检测无匹配点"
                )
            }
        }
    }
    
    /**
     * 检查颜色是否匹配
     */
    private fun isColorMatch(
        r1: Int, g1: Int, b1: Int,
        r2: Int, g2: Int, b2: Int,
        tolerance: Int
    ): Boolean {
        return Math.abs(r1 - r2) <= tolerance &&
                Math.abs(g1 - g2) <= tolerance &&
                Math.abs(b1 - b2) <= tolerance
    }
    
    /**
     * 计算点集的边界矩形
     */
    private fun calculateBounds(points: List<Point>): Rect {
        if (points.isEmpty()) return Rect()
        
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        
        for (point in points) {
            minX = minOf(minX, point.x)
            minY = minOf(minY, point.y)
            maxX = maxOf(maxX, point.x)
            maxY = maxOf(maxY, point.y)
        }
        
        return Rect(minX, minY, maxX, maxY)
    }
    
    /**
     * 计算点集的中心点
     */
    private fun calculateCenter(points: List<Point>): Point {
        if (points.isEmpty()) return Point(0, 0)
        
        var sumX = 0
        var sumY = 0
        
        for (point in points) {
            sumX += point.x
            sumY += point.y
        }
        
        return Point(sumX / points.size, sumY / points.size)
    }
    
    /**
     * 获取指定位置的颜色
     */
    fun getColorAt(bitmap: Bitmap, x: Int, y: Int): Int {
        if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) {
            return Color.TRANSPARENT
        }
        return bitmap.getPixel(x, y)
    }
    
    /**
     * 获取指定位置的颜色（相对坐标）
     */
    fun getColorAtRelative(bitmap: Bitmap, relativeX: Float, relativeY: Float): Int {
        val x = (bitmap.width * relativeX / 100f).toInt()
        val y = (bitmap.height * relativeY / 100f).toInt()
        return getColorAt(bitmap, x, y)
    }
    
    /**
     * 比较两个颜色是否相似
     */
    fun isColorSimilar(color1: Int, color2: Int, tolerance: Int = 10): Boolean {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        
        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)
        
        return isColorMatch(r1, g1, b1, r2, g2, b2, tolerance)
    }
    
    /**
     * 计算两个颜色之间的距离
     */
    fun colorDistance(color1: Int, color2: Int): Double {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        
        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)
        
        return Math.sqrt(
            Math.pow((r1 - r2).toDouble(), 2.0) +
            Math.pow((g1 - g2).toDouble(), 2.0) +
            Math.pow((b1 - b2).toDouble(), 2.0)
        )
    }
    
    /**
     * 查找颜色区域
     */
    fun findColorRegion(
        bitmap: Bitmap,
        targetColor: Int,
        tolerance: Int = 10,
        minAreaSize: Int = 100
    ): List<Rect> {
        val visited = Array(bitmap.height) { BooleanArray(bitmap.width) }
        val regions = mutableListOf<Rect>()
        
        val targetRed = Color.red(targetColor)
        val targetGreen = Color.green(targetColor)
        val targetBlue = Color.blue(targetColor)
        
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (visited[y][x]) continue
                
                val pixelColor = bitmap.getPixel(x, y)
                val red = Color.red(pixelColor)
                val green = Color.green(pixelColor)
                val blue = Color.blue(pixelColor)
                
                if (isColorMatch(red, green, blue, targetRed, targetGreen, targetBlue, tolerance)) {
                    val region = floodFill(bitmap, visited, x, y, targetRed, targetGreen, targetBlue, tolerance)
                    if (region.width() * region.height() >= minAreaSize) {
                        regions.add(region)
                    }
                }
            }
        }
        
        return regions
    }
    
    /**
     * 洪水填充算法查找连通区域
     */
    private fun floodFill(
        bitmap: Bitmap,
        visited: Array<BooleanArray>,
        startX: Int,
        startY: Int,
        targetRed: Int,
        targetGreen: Int,
        targetBlue: Int,
        tolerance: Int
    ): Rect {
        val queue = mutableListOf<Point>()
        queue.add(Point(startX, startY))
        
        var minX = startX
        var minY = startY
        var maxX = startX
        var maxY = startY
        
        while (queue.isNotEmpty()) {
            val point = queue.removeAt(0)
            val x = point.x
            val y = point.y
            
            if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) continue
            if (visited[y][x]) continue
            
            val pixelColor = bitmap.getPixel(x, y)
            val red = Color.red(pixelColor)
            val green = Color.green(pixelColor)
            val blue = Color.blue(pixelColor)
            
            if (!isColorMatch(red, green, blue, targetRed, targetGreen, targetBlue, tolerance)) continue
            
            visited[y][x] = true
            
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
            
            queue.add(Point(x + 1, y))
            queue.add(Point(x - 1, y))
            queue.add(Point(x, y + 1))
            queue.add(Point(x, y - 1))
        }
        
        return Rect(minX, minY, maxX, maxY)
    }
}
