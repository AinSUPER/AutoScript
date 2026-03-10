package com.autoscript.recognizer.locator

import android.graphics.Bitmap
import android.graphics.Rect
import com.autoscript.model.recognizer.*
import com.autoscript.recognizer.image.TemplateMatcher
import com.autoscript.recognizer.image.FeatureMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图像定位器
 * 支持图片匹配、特征点匹配和相似度匹配
 */
class ImageLocator(
    private val templateMatcher: TemplateMatcher = TemplateMatcher(),
    private val featureMatcher: FeatureMatcher = FeatureMatcher()
) {
    
    /**
     * 执行图像定位
     */
    suspend fun locate(
        screenshot: Bitmap,
        config: ImageConfig,
        locatorConfig: LocatorConfig
    ): RecognizeResult = withContext(Dispatchers.IO) {
        try {
            val template = loadTemplate(config)
            if (template == null) {
                return@withContext RecognizeResult.Failure(
                    errorCode = RecognizeResult.Failure.ERROR_INVALID_INPUT,
                    errorMessage = "无法加载模板图片"
                )
            }
            
            val results = when (config.matchMethod) {
                ImageConfig.ImageMatchMethod.TEMPLATE -> {
                    performTemplateMatch(screenshot, template, config, locatorConfig)
                }
                ImageConfig.ImageMatchMethod.FEATURE_SIFT,
                ImageConfig.ImageMatchMethod.FEATURE_ORB,
                ImageConfig.ImageMatchMethod.FEATURE_AKAZE -> {
                    performFeatureMatch(screenshot, template, config, locatorConfig)
                }
                ImageConfig.ImageMatchMethod.HYBRID -> {
                    performHybridMatch(screenshot, template, config, locatorConfig)
                }
            }
            
            results
        } catch (e: Exception) {
            RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_SERVICE_UNAVAILABLE,
                errorMessage = "图像定位失败: ${e.message}"
            )
        }
    }
    
    /**
     * 加载模板图片
     */
    private fun loadTemplate(config: ImageConfig): Bitmap? {
        return when {
            config.templateData != null -> {
                android.graphics.BitmapFactory.decodeByteArray(
                    config.templateData,
                    0,
                    config.templateData.size
                )
            }
            config.templatePath != null -> {
                loadBitmapFromPath(config.templatePath)
            }
            else -> null
        }
    }
    
    /**
     * 从文件路径加载图片
     */
    private fun loadBitmapFromPath(path: String): Bitmap? {
        return try {
            android.graphics.BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 执行模板匹配
     */
    private suspend fun performTemplateMatch(
        screenshot: Bitmap,
        template: Bitmap,
        config: ImageConfig,
        locatorConfig: LocatorConfig
    ): RecognizeResult {
        val matchResults = mutableListOf<MatchResult>()
        
        val scales = generateScaleList(config.scaleRange)
        
        for (scale in scales) {
            val scaledTemplate = scaleBitmap(template, scale)
            if (scaledTemplate.width > screenshot.width || scaledTemplate.height > screenshot.height) {
                continue
            }
            
            val result = templateMatcher.match(
                screenshot,
                scaledTemplate,
                locatorConfig.confidence,
                locatorConfig.region
            )
            
            if (result.matched && result.confidence >= locatorConfig.confidence) {
                matchResults.add(result.copy(scale = scale))
                
                if (!locatorConfig.multiMatch) {
                    break
                }
            }
        }
        
        return buildResult(matchResults, locatorConfig)
    }
    
    /**
     * 执行特征点匹配
     */
    private suspend fun performFeatureMatch(
        screenshot: Bitmap,
        template: Bitmap,
        config: ImageConfig,
        locatorConfig: LocatorConfig
    ): RecognizeResult {
        val result = when (config.matchMethod) {
            ImageConfig.ImageMatchMethod.FEATURE_SIFT -> {
                featureMatcher.matchWithMethod(
                    screenshot,
                    template,
                    FeatureMatcher.FeatureMethod.SIFT,
                    locatorConfig.confidence
                )
            }
            ImageConfig.ImageMatchMethod.FEATURE_ORB -> {
                featureMatcher.matchWithMethod(
                    screenshot,
                    template,
                    FeatureMatcher.FeatureMethod.ORB,
                    locatorConfig.confidence
                )
            }
            ImageConfig.ImageMatchMethod.FEATURE_AKAZE -> {
                featureMatcher.matchWithMethod(
                    screenshot,
                    template,
                    FeatureMatcher.FeatureMethod.AKAZE,
                    locatorConfig.confidence
                )
            }
            else -> featureMatcher.match(screenshot, template, locatorConfig.confidence)
        }
        
        return if (result.matched) {
            RecognizeResult.Success(
                confidence = result.confidence,
                elementInfo = result.toElementInfo(),
                matchResults = listOf(result)
            )
        } else {
            RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
                errorMessage = "特征点匹配失败，置信度: ${result.confidence}"
            )
        }
    }
    
    /**
     * 执行混合匹配（模板+特征）
     */
    private suspend fun performHybridMatch(
        screenshot: Bitmap,
        template: Bitmap,
        config: ImageConfig,
        locatorConfig: LocatorConfig
    ): RecognizeResult {
        val templateResult = templateMatcher.match(
            screenshot,
            template,
            locatorConfig.confidence,
            locatorConfig.region
        )
        
        if (templateResult.matched && templateResult.confidence >= locatorConfig.confidence) {
            return RecognizeResult.Success(
                confidence = templateResult.confidence,
                elementInfo = templateResult.toElementInfo(),
                matchResults = listOf(templateResult.copy(matchType = MatchResult.MatchType.TEMPLATE))
            )
        }
        
        val featureResult = featureMatcher.match(
            screenshot,
            template,
            locatorConfig.confidence
        )
        
        return if (featureResult.matched && featureResult.confidence >= locatorConfig.confidence) {
            RecognizeResult.Success(
                confidence = featureResult.confidence,
                elementInfo = featureResult.toElementInfo(),
                matchResults = listOf(featureResult.copy(matchType = MatchResult.MatchType.FEATURE))
            )
        } else {
            RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
                errorMessage = "混合匹配失败"
            )
        }
    }
    
    /**
     * 生成缩放比例列表
     */
    private fun generateScaleList(range: ImageConfig.FloatRange): List<Float> {
        val scales = mutableListOf<Float>()
        var scale = range.start
        while (scale <= range.end) {
            scales.add(scale)
            scale += 0.1f
        }
        return scales
    }
    
    /**
     * 缩放图片
     */
    private fun scaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        if (scale == 1.0f) return bitmap
        
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    
    /**
     * 构建识别结果
     */
    private fun buildResult(
        matchResults: List<MatchResult>,
        locatorConfig: LocatorConfig
    ): RecognizeResult {
        if (matchResults.isEmpty()) {
            return RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
                errorMessage = "未找到匹配的图像"
            )
        }
        
        val sortedResults = matchResults.sortedByDescending { it.confidence }
        val finalResults = sortedResults.take(locatorConfig.maxResults)
        
        return if (finalResults.size == 1) {
            RecognizeResult.Success(
                confidence = finalResults.first().confidence,
                elementInfo = finalResults.first().toElementInfo(),
                matchResults = finalResults
            )
        } else {
            val elements = finalResults.map { it.toElementInfo() }
            RecognizeResult.MultiElement(
                confidence = finalResults.first().confidence,
                elements = elements
            )
        }
    }
    
    /**
     * 计算图片相似度
     */
    suspend fun calculateSimilarity(
        bitmap1: Bitmap,
        bitmap2: Bitmap
    ): Float = withContext(Dispatchers.IO) {
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
            val scaled = scaleBitmap(bitmap2, bitmap1.width.toFloat() / bitmap2.width)
            calculateHistogramSimilarity(bitmap1, scaled)
        } else {
            calculateHistogramSimilarity(bitmap1, bitmap2)
        }
    }
    
    /**
     * 计算直方图相似度
     */
    private fun calculateHistogramSimilarity(bitmap1: Bitmap, bitmap2: Bitmap): Float {
        val histogram1 = calculateHistogram(bitmap1)
        val histogram2 = calculateHistogram(bitmap2)
        
        var similarity = 0.0
        for (i in histogram1.indices) {
            similarity += Math.min(histogram1[i], histogram2[i])
        }
        
        val totalPixels = bitmap1.width * bitmap1.height
        return (similarity / totalPixels).toFloat()
    }
    
    /**
     * 计算图片直方图
     */
    private fun calculateHistogram(bitmap: Bitmap): DoubleArray {
        val histogram = DoubleArray(256)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        for (pixel in pixels) {
            val gray = (android.graphics.Color.red(pixel) * 0.299 +
                    android.graphics.Color.green(pixel) * 0.587 +
                    android.graphics.Color.blue(pixel) * 0.114).toInt()
            histogram[gray]++
        }
        
        return histogram
    }
}
