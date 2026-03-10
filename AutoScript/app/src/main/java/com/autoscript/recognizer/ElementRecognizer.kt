package com.autoscript.recognizer

import android.graphics.Bitmap
import android.graphics.Rect
import com.autoscript.model.recognizer.*
import com.autoscript.recognizer.locator.*
import com.autoscript.recognizer.image.TemplateMatcher
import com.autoscript.recognizer.image.FeatureMatcher
import com.autoscript.recognizer.ocr.OcrService
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * 元素识别器主类
 * 提供统一的元素识别接口，支持多种识别方式组合
 */
class ElementRecognizer(
    private val coordinateLocator: CoordinateLocator = CoordinateLocator(),
    private val textLocator: TextLocator = TextLocator(),
    private val imageLocator: ImageLocator = ImageLocator(),
    private val viewLocator: ViewLocator = ViewLocator(),
    private val colorLocator: ColorLocator = ColorLocator(),
    private val templateMatcher: TemplateMatcher = TemplateMatcher(),
    private val featureMatcher: FeatureMatcher = FeatureMatcher(),
    private val ocrService: OcrService = OcrService()
) : CoroutineScope {
    
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job
    
    private var isInitialized = false
    
    /**
     * 初始化识别器
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            templateMatcher.initialize()
            featureMatcher.initialize()
            ocrService.initialize()
            isInitialized = true
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        templateMatcher.release()
        featureMatcher.release()
        ocrService.release()
        job.cancel()
        isInitialized = false
    }
    
    /**
     * 通过坐标定位元素
     */
    fun locateByCoordinate(config: CoordinateConfig): RecognizeResult {
        return coordinateLocator.locate(config)
    }
    
    /**
     * 通过文本定位元素
     */
    suspend fun locateByText(
        config: TextConfig,
        locatorConfig: LocatorConfig = LocatorConfig.DEFAULT
    ): RecognizeResult = withContext(Dispatchers.IO) {
        textLocator.locate(config, locatorConfig)
    }
    
    /**
     * 通过图像定位元素
     */
    suspend fun locateByImage(
        screenshot: Bitmap,
        config: ImageConfig,
        locatorConfig: LocatorConfig = LocatorConfig.DEFAULT
    ): RecognizeResult = withContext(Dispatchers.IO) {
        imageLocator.locate(screenshot, config, locatorConfig)
    }
    
    /**
     * 通过控件属性定位元素
     */
    suspend fun locateByView(
        config: ViewConfig,
        locatorConfig: LocatorConfig = LocatorConfig.DEFAULT
    ): RecognizeResult = withContext(Dispatchers.IO) {
        viewLocator.locate(config, locatorConfig)
    }
    
    /**
     * 通过颜色定位元素
     */
    fun locateByColor(
        screenshot: Bitmap,
        config: ColorConfig
    ): RecognizeResult {
        return colorLocator.locate(screenshot, config)
    }
    
    /**
     * 模板匹配
     */
    suspend fun matchTemplate(
        source: Bitmap,
        template: Bitmap,
        config: LocatorConfig = LocatorConfig.DEFAULT
    ): MatchResult = withContext(Dispatchers.IO) {
        templateMatcher.match(source, template, config.confidence, config.region)
    }
    
    /**
     * 特征点匹配
     */
    suspend fun matchFeature(
        source: Bitmap,
        template: Bitmap,
        config: LocatorConfig = LocatorConfig.DEFAULT
    ): MatchResult = withContext(Dispatchers.IO) {
        featureMatcher.match(source, template, config.confidence)
    }
    
    /**
     * OCR文字识别
     */
    suspend fun recognizeText(
        bitmap: Bitmap,
        region: Rect? = null
    ): String? = withContext(Dispatchers.IO) {
        ocrService.recognizeText(bitmap, region)
    }
    
    /**
     * OCR文字识别并返回详细信息
     */
    suspend fun recognizeTextWithBlocks(
        bitmap: Bitmap,
        region: Rect? = null
    ): List<OcrService.TextBlock> = withContext(Dispatchers.IO) {
        ocrService.recognizeTextWithBlocks(bitmap, region)
    }
    
    /**
     * 组合识别 - 同时使用多种方式识别
     */
    suspend fun recognize(
        screenshot: Bitmap,
        configs: List<RecognizerConfig>,
        locatorConfig: LocatorConfig = LocatorConfig.DEFAULT
    ): RecognizeResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<MatchResult>()
        var bestResult: MatchResult? = null
        
        for (config in configs) {
            val result = when (config) {
                is RecognizerConfig.Coordinate -> {
                    val res = coordinateLocator.locate(config.config)
                    if (res is RecognizeResult.Success) {
                        MatchResult.Builder()
                            .matched(true)
                            .confidence(1.0f)
                            .bounds(res.elementInfo.bounds)
                            .matchType(MatchResult.MatchType.COORDINATE)
                            .build()
                    } else null
                }
                is RecognizerConfig.Text -> {
                    val res = textLocator.locate(config.config, locatorConfig)
                    if (res is RecognizeResult.Success) {
                        MatchResult.Builder()
                            .matched(true)
                            .confidence(res.confidence)
                            .bounds(res.elementInfo.bounds)
                            .matchedText(res.elementInfo.text)
                            .matchType(MatchResult.MatchType.TEXT_EXACT)
                            .build()
                    } else null
                }
                is RecognizerConfig.Image -> {
                    val res = imageLocator.locate(screenshot, config.config, locatorConfig)
                    if (res is RecognizeResult.Success) {
                        MatchResult.Builder()
                            .matched(true)
                            .confidence(res.confidence)
                            .bounds(res.elementInfo.bounds)
                            .matchType(MatchResult.MatchType.TEMPLATE)
                            .build()
                    } else null
                }
                is RecognizerConfig.View -> {
                    val res = viewLocator.locate(config.config, locatorConfig)
                    if (res is RecognizeResult.Success) {
                        MatchResult.Builder()
                            .matched(true)
                            .confidence(res.confidence)
                            .bounds(res.elementInfo.bounds)
                            .matchedText(res.elementInfo.resourceId)
                            .matchType(MatchResult.MatchType.VIEW_ID)
                            .build()
                    } else null
                }
                is RecognizerConfig.Color -> {
                    val res = colorLocator.locate(screenshot, config.config)
                    if (res is RecognizeResult.Success) {
                        MatchResult.Builder()
                            .matched(true)
                            .confidence(res.confidence)
                            .bounds(res.elementInfo.bounds)
                            .matchType(MatchResult.MatchType.COLOR)
                            .build()
                    } else null
                }
            }
            
            result?.let {
                results.add(it)
                if (bestResult == null || it.confidence > bestResult!!.confidence) {
                    bestResult = it
                }
            }
        }
        
        bestResult?.let {
            RecognizeResult.Success(
                confidence = it.confidence,
                elementInfo = it.toElementInfo(),
                matchResults = results
            )
        } ?: RecognizeResult.Failure(
            errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
            errorMessage = "未找到匹配的元素"
        )
    }
    
    /**
     * 等待元素出现
     */
    suspend fun waitForElement(
        screenshotProvider: suspend () -> Bitmap?,
        config: RecognizerConfig,
        timeout: Long = LocatorConfig.DEFAULT_WAIT_TIMEOUT,
        interval: Long = 500L
    ): RecognizeResult = withTimeoutOrNull(timeout) {
        while (true) {
            val screenshot = screenshotProvider()
            if (screenshot != null) {
                val result = when (config) {
                    is RecognizerConfig.Coordinate -> coordinateLocator.locate(config.config)
                    is RecognizerConfig.Text -> textLocator.locate(config.config, LocatorConfig.DEFAULT)
                    is RecognizerConfig.Image -> imageLocator.locate(screenshot, config.config, LocatorConfig.DEFAULT)
                    is RecognizerConfig.View -> viewLocator.locate(config.config, LocatorConfig.DEFAULT)
                    is RecognizerConfig.Color -> colorLocator.locate(screenshot, config.config)
                }
                
                if (result.success) {
                    return@withTimeoutOrNull result
                }
            }
            delay(interval)
        }
        null
    } ?: RecognizeResult.Failure(
        errorCode = RecognizeResult.Failure.ERROR_TIMEOUT,
        errorMessage = "等待元素超时"
    )
    
    /**
     * 检查元素是否存在
     */
    suspend fun exists(
        screenshot: Bitmap,
        config: RecognizerConfig
    ): Boolean {
        val result = when (config) {
            is RecognizerConfig.Coordinate -> coordinateLocator.locate(config.config)
            is RecognizerConfig.Text -> textLocator.locate(config.config, LocatorConfig.FAST)
            is RecognizerConfig.Image -> imageLocator.locate(screenshot, config.config, LocatorConfig.FAST)
            is RecognizerConfig.View -> viewLocator.locate(config.config, LocatorConfig.FAST)
            is RecognizerConfig.Color -> colorLocator.locate(screenshot, config.config)
        }
        return result.success
    }
    
    /**
     * 识别器配置密封类
     */
    sealed class RecognizerConfig {
        data class Coordinate(val config: CoordinateConfig) : RecognizerConfig()
        data class Text(val config: TextConfig) : RecognizerConfig()
        data class Image(val config: ImageConfig) : RecognizerConfig()
        data class View(val config: ViewConfig) : RecognizerConfig()
        data class Color(val config: ColorConfig) : RecognizerConfig()
    }
    
    companion object {
        private const val TAG = "ElementRecognizer"
        
        @Volatile
        private var instance: ElementRecognizer? = null
        
        fun getInstance(): ElementRecognizer {
            return instance ?: synchronized(this) {
                instance ?: ElementRecognizer().also { instance = it }
            }
        }
    }
}
