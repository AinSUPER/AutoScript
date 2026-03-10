package com.autoscript.model.recognizer

import android.graphics.Rect

/**
 * 定位器配置数据类
 * 用于配置各种定位器的参数
 */
data class LocatorConfig(
    val timeout: Long = DEFAULT_TIMEOUT,
    val retryCount: Int = DEFAULT_RETRY_COUNT,
    val retryInterval: Long = DEFAULT_RETRY_INTERVAL,
    val confidence: Float = DEFAULT_CONFIDENCE,
    val multiMatch: Boolean = false,
    val maxResults: Int = 1,
    val region: Rect? = null,
    val waitForElement: Boolean = false,
    val waitTimeout: Long = DEFAULT_WAIT_TIMEOUT
) {
    companion object {
        const val DEFAULT_TIMEOUT = 10000L
        const val DEFAULT_RETRY_COUNT = 3
        const val DEFAULT_RETRY_INTERVAL = 500L
        const val DEFAULT_CONFIDENCE = 0.8f
        const val DEFAULT_WAIT_TIMEOUT = 5000L
        
        val DEFAULT = LocatorConfig()
        
        val FAST = LocatorConfig(
            timeout = 5000L,
            retryCount = 1,
            retryInterval = 200L,
            confidence = 0.7f
        )
        
        val PRECISE = LocatorConfig(
            timeout = 15000L,
            retryCount = 5,
            retryInterval = 500L,
            confidence = 0.95f
        )
        
        val MULTI_MATCH = LocatorConfig(
            multiMatch = true,
            maxResults = 10
        )
    }
    
    class Builder {
        private var timeout: Long = DEFAULT_TIMEOUT
        private var retryCount: Int = DEFAULT_RETRY_COUNT
        private var retryInterval: Long = DEFAULT_RETRY_INTERVAL
        private var confidence: Float = DEFAULT_CONFIDENCE
        private var multiMatch: Boolean = false
        private var maxResults: Int = 1
        private var region: Rect? = null
        private var waitForElement: Boolean = false
        private var waitTimeout: Long = DEFAULT_WAIT_TIMEOUT
        
        fun timeout(timeout: Long) = apply { this.timeout = timeout }
        fun retryCount(count: Int) = apply { this.retryCount = count }
        fun retryInterval(interval: Long) = apply { this.retryInterval = interval }
        fun confidence(confidence: Float) = apply { this.confidence = confidence }
        fun multiMatch(multiMatch: Boolean) = apply { this.multiMatch = multiMatch }
        fun maxResults(max: Int) = apply { this.maxResults = max }
        fun region(region: Rect?) = apply { this.region = region }
        fun waitForElement(wait: Boolean) = apply { this.waitForElement = wait }
        fun waitTimeout(timeout: Long) = apply { this.waitTimeout = timeout }
        
        fun build(): LocatorConfig = LocatorConfig(
            timeout = timeout,
            retryCount = retryCount,
            retryInterval = retryInterval,
            confidence = confidence,
            multiMatch = multiMatch,
            maxResults = maxResults,
            region = region,
            waitForElement = waitForElement,
            waitTimeout = waitTimeout
        )
    }
}

/**
 * 坐标定位配置
 */
data class CoordinateConfig(
    val x: Int,
    val y: Int,
    val isRelative: Boolean = false,
    val offsetX: Int = 0,
    val offsetY: Int = 0
)

/**
 * 文本定位配置
 */
data class TextConfig(
    val text: String,
    val matchMode: TextMatchMode = TextMatchMode.EXACT,
    val caseSensitive: Boolean = false,
    val useOcr: Boolean = false,
    val ocrLanguage: String = "zh"
) {
    enum class TextMatchMode {
        EXACT,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        REGEX,
        FUZZY
    }
}

/**
 * 图像定位配置
 */
data class ImageConfig(
    val templatePath: String? = null,
    val templateData: ByteArray? = null,
    val matchMethod: ImageMatchMethod = ImageMatchMethod.TEMPLATE,
    val scaleRange: FloatRange = FloatRange(0.8f, 1.2f),
    val scaleStep: Float = 0.1f,
    val rotationRange: FloatRange = FloatRange(0f, 0f),
    val rotationStep: Float = 15f
) {
    enum class ImageMatchMethod {
        TEMPLATE,
        FEATURE_SIFT,
        FEATURE_ORB,
        FEATURE_AKAZE,
        HYBRID
    }
    
    data class FloatRange(val start: Float, val end: Float)
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ImageConfig
        if (templatePath != other.templatePath) return false
        if (templateData != null) {
            if (other.templateData == null) return false
            if (!templateData.contentEquals(other.templateData)) return false
        } else if (other.templateData != null) return false
        if (matchMethod != other.matchMethod) return false
        return true
    }
    
    override fun hashCode(): Int {
        var result = templatePath?.hashCode() ?: 0
        result = 31 * result + (templateData?.contentHashCode() ?: 0)
        result = 31 * result + matchMethod.hashCode()
        return result
    }
}

/**
 * 控件定位配置
 */
data class ViewConfig(
    val resourceId: String? = null,
    val className: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
    val xpath: String? = null,
    val packageName: String? = null,
    val clickable: Boolean? = null,
    val scrollable: Boolean? = null,
    val editable: Boolean? = null
)

/**
 * 颜色定位配置
 */
data class ColorConfig(
    val targetColor: Int,
    val tolerance: Int = 10,
    val points: List<ColorPoint> = emptyList(),
    val matchAllPoints: Boolean = true
) {
    data class ColorPoint(
        val x: Int,
        val y: Int,
        val isRelative: Boolean = false
    )
}
