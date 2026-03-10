package com.autoscript.recognizer.locator

import android.graphics.Rect
import com.autoscript.model.recognizer.*
import com.autoscript.recognizer.ocr.OcrService
import com.autoscript.service.AccessibilityServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * 文本定位器
 * 支持精确文本匹配、模糊文本匹配和OCR文字识别
 */
class TextLocator(
    private val ocrService: OcrService = OcrService()
) {
    
    /**
     * 执行文本定位
     */
    suspend fun locate(
        config: TextConfig,
        locatorConfig: LocatorConfig
    ): RecognizeResult = withContext(Dispatchers.IO) {
        try {
            if (config.useOcr) {
                locateByOcr(config, locatorConfig)
            } else {
                locateByAccessibility(config, locatorConfig)
            }
        } catch (e: Exception) {
            RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_SERVICE_UNAVAILABLE,
                errorMessage = "文本定位失败: ${e.message}"
            )
        }
    }
    
    /**
     * 通过无障碍服务定位文本
     */
    private suspend fun locateByAccessibility(
        config: TextConfig,
        locatorConfig: LocatorConfig
    ): RecognizeResult {
        val accessibilityService = AccessibilityServiceImpl.getInstance()
            ?: return RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_SERVICE_UNAVAILABLE,
                errorMessage = "无障碍服务未启用"
            )
        
        val rootInActiveWindow = accessibilityService.rootInActiveWindow
            ?: return RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
                errorMessage = "无法获取窗口根节点"
            )
        
        val matchedNodes = mutableListOf<ElementInfo>()
        findTextNodes(rootInActiveWindow, config, matchedNodes)
        
        return if (matchedNodes.isNotEmpty()) {
            val results = if (locatorConfig.multiMatch) {
                matchedNodes.take(locatorConfig.maxResults)
            } else {
                listOf(matchedNodes.first())
            }
            
            if (results.size == 1) {
                RecognizeResult.Success(
                    confidence = calculateConfidence(config, results.first().text),
                    elementInfo = results.first()
                )
            } else {
                RecognizeResult.MultiElement(elements = results)
            }
        } else {
            RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
                errorMessage = "未找到匹配的文本: ${config.text}"
            )
        }
    }
    
    /**
     * 递归查找包含目标文本的节点
     */
    private fun findTextNodes(
        node: android.view.accessibility.AccessibilityNodeInfo,
        config: TextConfig,
        results: MutableList<ElementInfo>
    ) {
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        
        val isMatched = matchText(nodeText, config) || matchText(contentDesc, config)
        
        if (isMatched) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            
            val elementInfo = ElementInfo.Builder()
                .bounds(bounds)
                .text(nodeText.ifEmpty { contentDesc })
                .contentDescription(contentDesc)
                .resourceId(node.viewIdResourceName)
                .className(node.className?.toString())
                .packageName(node.packageName?.toString())
                .clickable(node.isClickable)
                .scrollable(node.isScrollable)
                .editable(node.isEditable)
                .enabled(node.isEnabled)
                .visible(node.isVisibleToUser)
                .build()
            
            results.add(elementInfo)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findTextNodes(child, config, results)
            }
        }
    }
    
    /**
     * 文本匹配
     */
    private fun matchText(sourceText: String, config: TextConfig): Boolean {
        if (sourceText.isEmpty()) return false
        
        val targetText = if (config.caseSensitive) config.text else config.text.lowercase()
        val source = if (config.caseSensitive) sourceText else sourceText.lowercase()
        
        return when (config.matchMode) {
            TextConfig.TextMatchMode.EXACT -> source == targetText
            TextConfig.TextMatchMode.CONTAINS -> source.contains(targetText)
            TextConfig.TextMatchMode.STARTS_WITH -> source.startsWith(targetText)
            TextConfig.TextMatchMode.ENDS_WITH -> source.endsWith(targetText)
            TextConfig.TextMatchMode.REGEX -> {
                try {
                    val pattern = Pattern.compile(config.text)
                    pattern.matcher(sourceText).find()
                } catch (e: Exception) {
                    false
                }
            }
            TextConfig.TextMatchMode.FUZZY -> fuzzyMatch(source, targetText)
        }
    }
    
    /**
     * 模糊匹配（Levenshtein距离）
     */
    private fun fuzzyMatch(source: String, target: String): Boolean {
        val distance = levenshteinDistance(source, target)
        val maxLength = maxOf(source.length, target.length)
        val similarity = 1.0f - (distance.toFloat() / maxLength.toFloat())
        return similarity >= 0.7f
    }
    
    /**
     * 计算Levenshtein距离
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        
        if (m == 0) return n
        if (n == 0) return m
        
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        
        return dp[m][n]
    }
    
    /**
     * 通过OCR定位文本
     */
    private suspend fun locateByOcr(
        config: TextConfig,
        locatorConfig: LocatorConfig
    ): RecognizeResult {
        return RecognizeResult.Failure(
            errorCode = RecognizeResult.Failure.ERROR_SERVICE_UNAVAILABLE,
            errorMessage = "OCR定位需要提供截图"
        )
    }
    
    /**
     * 通过OCR在截图上定位文本
     */
    suspend fun locateByOcr(
        bitmap: android.graphics.Bitmap,
        config: TextConfig,
        locatorConfig: LocatorConfig
    ): RecognizeResult = withContext(Dispatchers.IO) {
        try {
            val textBlocks = ocrService.recognizeTextWithBlocks(
                bitmap,
                locatorConfig.region,
                config.ocrLanguage
            )
            
            val matchedBlocks = textBlocks.filter { block ->
                matchText(block.text, config)
            }
            
            if (matchedBlocks.isNotEmpty()) {
                val elements = matchedBlocks.map { block ->
                    ElementInfo.Builder()
                        .bounds(block.bounds)
                        .text(block.text)
                        .build()
                }
                
                if (elements.size == 1) {
                    RecognizeResult.Success(
                        confidence = calculateConfidence(config, elements.first().text),
                        elementInfo = elements.first()
                    )
                } else {
                    RecognizeResult.MultiElement(elements = elements)
                }
            } else {
                RecognizeResult.Failure(
                    errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
                    errorMessage = "OCR未找到匹配的文本: ${config.text}"
                )
            }
        } catch (e: Exception) {
            RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_SERVICE_UNAVAILABLE,
                errorMessage = "OCR识别失败: ${e.message}"
            )
        }
    }
    
    /**
     * 计算匹配置信度
     */
    private fun calculateConfidence(config: TextConfig, matchedText: String?): Float {
        if (matchedText.isNullOrEmpty()) return 0f
        
        val targetText = if (config.caseSensitive) config.text else config.text.lowercase()
        val source = if (config.caseSensitive) matchedText else matchedText.lowercase()
        
        return when (config.matchMode) {
            TextConfig.TextMatchMode.EXACT -> if (source == targetText) 1.0f else 0f
            TextConfig.TextMatchMode.CONTAINS -> {
                val index = source.indexOf(targetText)
                if (index >= 0) {
                    val ratio = targetText.length.toFloat() / source.length.toFloat()
                    0.7f + ratio * 0.3f
                } else 0f
            }
            TextConfig.TextMatchMode.REGEX -> 0.9f
            TextConfig.TextMatchMode.FUZZY -> {
                val distance = levenshteinDistance(source, targetText)
                val maxLength = maxOf(source.length, targetText.length)
                1.0f - (distance.toFloat() / maxLength.toFloat())
            }
            else -> 0.8f
        }
    }
}
