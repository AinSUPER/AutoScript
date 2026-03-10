package com.autoscript.model.recognizer

import android.graphics.Point
import android.graphics.Rect

/**
 * 匹配结果数据类
 * 描述图像或文本匹配的详细结果
 */
data class MatchResult(
    val matched: Boolean = false,
    val confidence: Float = 0.0f,
    val bounds: Rect = Rect(),
    val center: Point = Point(bounds.centerX(), bounds.centerY()),
    val matchedText: String? = null,
    val matchedValue: Any? = null,
    val matchType: MatchType = MatchType.UNKNOWN,
    val scale: Float = 1.0f,
    val rotation: Float = 0.0f,
    val offset: Point = Point(0, 0)
) {
    
    enum class MatchType {
        UNKNOWN,
        TEMPLATE,
        FEATURE,
        TEXT_EXACT,
        TEXT_FUZZY,
        TEXT_REGEX,
        OCR,
        COLOR,
        COORDINATE,
        VIEW_ID,
        VIEW_CLASS,
        VIEW_DESC,
        XPATH
    }
    
    fun isValid(): Boolean = matched && confidence > 0 && !bounds.isEmpty
    
    fun toElementInfo(): ElementInfo = ElementInfo.Builder()
        .bounds(bounds)
        .text(matchedText)
        .build()
    
    class Builder {
        private var matched: Boolean = false
        private var confidence: Float = 0.0f
        private var bounds: Rect = Rect()
        private var center: Point = Point()
        private var matchedText: String? = null
        private var matchedValue: Any? = null
        private var matchType: MatchType = MatchType.UNKNOWN
        private var scale: Float = 1.0f
        private var rotation: Float = 0.0f
        private var offset: Point = Point(0, 0)
        
        fun matched(matched: Boolean) = apply { this.matched = matched }
        fun confidence(confidence: Float) = apply { this.confidence = confidence }
        fun bounds(bounds: Rect) = apply { 
            this.bounds = bounds
            this.center = Point(bounds.centerX(), bounds.centerY())
        }
        fun bounds(left: Int, top: Int, right: Int, bottom: Int) = apply {
            this.bounds = Rect(left, top, right, bottom)
            this.center = Point(bounds.centerX(), bounds.centerY())
        }
        fun center(x: Int, y: Int) = apply { this.center = Point(x, y) }
        fun matchedText(text: String?) = apply { this.matchedText = text }
        fun matchedValue(value: Any?) = apply { this.matchedValue = value }
        fun matchType(type: MatchType) = apply { this.matchType = type }
        fun scale(scale: Float) = apply { this.scale = scale }
        fun rotation(rotation: Float) = apply { this.rotation = rotation }
        fun offset(x: Int, y: Int) = apply { this.offset = Point(x, y) }
        
        fun build(): MatchResult = MatchResult(
            matched = matched,
            confidence = confidence,
            bounds = bounds,
            center = center,
            matchedText = matchedText,
            matchedValue = matchedValue,
            matchType = matchType,
            scale = scale,
            rotation = rotation,
            offset = offset
        )
    }
}
