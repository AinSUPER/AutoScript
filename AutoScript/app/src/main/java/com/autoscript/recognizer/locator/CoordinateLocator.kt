package com.autoscript.recognizer.locator

import android.graphics.Rect
import com.autoscript.model.recognizer.*

/**
 * 坐标定位器
 * 支持绝对坐标、相对坐标和偏移坐标定位
 */
class CoordinateLocator {
    
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    
    /**
     * 设置屏幕尺寸
     */
    fun setScreenSize(width: Int, height: Int) {
        this.screenWidth = width
        this.screenHeight = height
    }
    
    /**
     * 执行坐标定位
     */
    fun locate(config: CoordinateConfig): RecognizeResult {
        return try {
            val (finalX, finalY) = calculateCoordinates(config)
            
            val elementInfo = ElementInfo.Builder()
                .bounds(
                    Rect(
                        finalX - DEFAULT_ELEMENT_SIZE / 2,
                        finalY - DEFAULT_ELEMENT_SIZE / 2,
                        finalX + DEFAULT_ELEMENT_SIZE / 2,
                        finalY + DEFAULT_ELEMENT_SIZE / 2
                    )
                )
                .build()
            
            RecognizeResult.Success(
                confidence = 1.0f,
                elementInfo = elementInfo
            )
        } catch (e: Exception) {
            RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_INVALID_INPUT,
                errorMessage = "坐标计算失败: ${e.message}"
            )
        }
    }
    
    /**
     * 计算最终坐标
     */
    private fun calculateCoordinates(config: CoordinateConfig): Pair<Int, Int> {
        val (baseX, baseY) = if (config.isRelative) {
            val x = (screenWidth * config.x / 100f).toInt()
            val y = (screenHeight * config.y / 100f).toInt()
            Pair(x, y)
        } else {
            Pair(config.x, config.y)
        }
        
        val finalX = baseX + config.offsetX
        val finalY = baseY + config.offsetY
        
        return Pair(finalX, finalY)
    }
    
    /**
     * 批量坐标定位
     */
    fun locateMultiple(configs: List<CoordinateConfig>): RecognizeResult {
        val elements = configs.mapNotNull { config ->
            try {
                val (finalX, finalY) = calculateCoordinates(config)
                ElementInfo.Builder()
                    .bounds(
                        Rect(
                            finalX - DEFAULT_ELEMENT_SIZE / 2,
                            finalY - DEFAULT_ELEMENT_SIZE / 2,
                            finalX + DEFAULT_ELEMENT_SIZE / 2,
                            finalY + DEFAULT_ELEMENT_SIZE / 2
                        )
                    )
                    .build()
            } catch (e: Exception) {
                null
            }
        }
        
        return if (elements.isNotEmpty()) {
            RecognizeResult.MultiElement(elements = elements)
        } else {
            RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_INVALID_INPUT,
                errorMessage = "所有坐标计算失败"
            )
        }
    }
    
    /**
     * 验证坐标是否在屏幕范围内
     */
    fun isValidCoordinate(x: Int, y: Int): Boolean {
        return x in 0 until screenWidth && y in 0 until screenHeight
    }
    
    /**
     * 将绝对坐标转换为相对坐标（百分比）
     */
    fun toRelativeCoordinate(x: Int, y: Int): Pair<Float, Float> {
        if (screenWidth == 0 || screenHeight == 0) {
            return Pair(0f, 0f)
        }
        val relativeX = (x.toFloat() / screenWidth) * 100f
        val relativeY = (y.toFloat() / screenHeight) * 100f
        return Pair(relativeX, relativeY)
    }
    
    /**
     * 将相对坐标转换为绝对坐标
     */
    fun toAbsoluteCoordinate(relativeX: Float, relativeY: Float): Pair<Int, Int> {
        val absoluteX = (screenWidth * relativeX / 100f).toInt()
        val absoluteY = (screenHeight * relativeY / 100f).toInt()
        return Pair(absoluteX, absoluteY)
    }
    
    /**
     * 计算两点之间的距离
     */
    fun calculateDistance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
        return Math.sqrt(Math.pow((x2 - x1).toDouble(), 2.0) + Math.pow((y2 - y1).toDouble(), 2.0))
    }
    
    /**
     * 计算两点之间的角度
     */
    fun calculateAngle(x1: Int, y1: Int, x2: Int, y2: Int): Double {
        return Math.toDegrees(Math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()))
    }
    
    companion object {
        private const val DEFAULT_ELEMENT_SIZE = 10
    }
}
