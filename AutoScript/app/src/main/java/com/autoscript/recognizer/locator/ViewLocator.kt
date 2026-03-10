package com.autoscript.recognizer.locator

import android.graphics.Rect
import com.autoscript.model.recognizer.*
import com.autoscript.service.AccessibilityServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 控件定位器
 * 支持控件ID、类名、描述和XPath定位
 */
class ViewLocator {
    
    /**
     * 执行控件定位
     */
    suspend fun locate(
        config: ViewConfig,
        locatorConfig: LocatorConfig
    ): RecognizeResult = withContext(Dispatchers.IO) {
        try {
            val accessibilityService = AccessibilityServiceImpl.getInstance()
            if (accessibilityService == null) {
                return@withContext RecognizeResult.Failure(
                    errorCode = RecognizeResult.Failure.ERROR_SERVICE_UNAVAILABLE,
                    errorMessage = "无障碍服务未启用"
                )
            }
            
            val rootInActiveWindow = accessibilityService.rootInActiveWindow
            if (rootInActiveWindow == null) {
                return@withContext RecognizeResult.Failure(
                    errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
                    errorMessage = "无法获取窗口根节点"
                )
            }
            
            val matchedNodes = mutableListOf<ElementInfo>()
            
            if (!config.xpath.isNullOrEmpty()) {
                locateByXPath(rootInActiveWindow, config.xpath, matchedNodes)
            } else {
                locateByAttributes(rootInActiveWindow, config, matchedNodes, 0)
            }
            
            return@withContext buildResult(matchedNodes, locatorConfig)
        } catch (e: Exception) {
            RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_SERVICE_UNAVAILABLE,
                errorMessage = "控件定位失败: ${e.message}"
            )
        }
    }
    
    /**
     * 通过属性定位控件
     */
    private fun locateByAttributes(
        node: android.view.accessibility.AccessibilityNodeInfo,
        config: ViewConfig,
        results: MutableList<ElementInfo>,
        depth: Int
    ) {
        if (matchesConfig(node, config)) {
            val elementInfo = convertToElementInfo(node, depth)
            results.add(elementInfo)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                locateByAttributes(child, config, results, depth + 1)
            }
        }
    }
    
    /**
     * 检查节点是否匹配配置
     */
    private fun matchesConfig(
        node: android.view.accessibility.AccessibilityNodeInfo,
        config: ViewConfig
    ): Boolean {
        if (config.resourceId != null) {
            if (node.viewIdResourceName != config.resourceId) {
                return false
            }
        }
        
        if (config.className != null) {
            if (node.className?.toString() != config.className) {
                return false
            }
        }
        
        if (config.text != null) {
            val nodeText = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""
            if (!nodeText.contains(config.text) && !contentDesc.contains(config.text)) {
                return false
            }
        }
        
        if (config.contentDescription != null) {
            val contentDesc = node.contentDescription?.toString() ?: ""
            if (!contentDesc.contains(config.contentDescription)) {
                return false
            }
        }
        
        if (config.packageName != null) {
            if (node.packageName?.toString() != config.packageName) {
                return false
            }
        }
        
        if (config.clickable != null && node.isClickable != config.clickable) {
            return false
        }
        
        if (config.scrollable != null && node.isScrollable != config.scrollable) {
            return false
        }
        
        if (config.editable != null && node.isEditable != config.editable) {
            return false
        }
        
        return true
    }
    
    /**
     * 通过XPath定位控件
     */
    private fun locateByXPath(
        rootNode: android.view.accessibility.AccessibilityNodeInfo,
        xpath: String,
        results: MutableList<ElementInfo>
    ) {
        val pathSegments = parseXPath(xpath)
        if (pathSegments.isEmpty()) {
            return
        }
        
        traverseXPath(rootNode, pathSegments, 0, results)
    }
    
    /**
     * 解析XPath表达式
     */
    private fun parseXPath(xpath: String): List<XPathSegment> {
        val segments = mutableListOf<XPathSegment>()
        
        val cleanPath = xpath.removePrefix("/").removePrefix("//")
        val parts = cleanPath.split("/")
        
        for (part in parts) {
            if (part.isEmpty()) continue
            
            val segment = parseXPathSegment(part)
            segments.add(segment)
        }
        
        return segments
    }
    
    /**
     * 解析XPath段
     */
    private fun parseXPathSegment(segment: String): XPathSegment {
        val className: String?
        val attributes = mutableMapOf<String, String>()
        var index: Int? = null
        
        val bracketIndex = segment.indexOf('[')
        
        if (bracketIndex == -1) {
            className = if (segment == "*" || segment.isEmpty()) null else segment
        } else {
            className = if (segment.substring(0, bracketIndex) == "*") {
                null
            } else {
                segment.substring(0, bracketIndex)
            }
            
            val bracketContent = segment.substring(bracketIndex + 1, segment.length - 1)
            
            if (bracketContent.matches(Regex("\\d+"))) {
                index = bracketContent.toInt() - 1
            } else {
                val attrPattern = Regex("""@(\w+)='([^']*)'""")
                attrPattern.findAll(bracketContent).forEach { match ->
                    attributes[match.groupValues[1]] = match.groupValues[2]
                }
            }
        }
        
        return XPathSegment(className, attributes, index)
    }
    
    /**
     * 遍历XPath
     */
    private fun traverseXPath(
        node: android.view.accessibility.AccessibilityNodeInfo,
        segments: List<XPathSegment>,
        segmentIndex: Int,
        results: MutableList<ElementInfo>
    ) {
        if (segmentIndex >= segments.size) {
            results.add(convertToElementInfo(node, segmentIndex))
            return
        }
        
        val segment = segments[segmentIndex]
        
        if (matchesXPathSegment(node, segment)) {
            if (segmentIndex == segments.size - 1) {
                results.add(convertToElementInfo(node, segmentIndex))
            } else {
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { child ->
                        traverseXPath(child, segments, segmentIndex + 1, results)
                    }
                }
            }
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                traverseXPath(child, segments, segmentIndex, results)
            }
        }
    }
    
    /**
     * 检查节点是否匹配XPath段
     */
    private fun matchesXPathSegment(
        node: android.view.accessibility.AccessibilityNodeInfo,
        segment: XPathSegment
    ): Boolean {
        if (segment.className != null) {
            val nodeClassName = node.className?.toString() ?: ""
            if (!nodeClassName.endsWith(segment.className) && nodeClassName != segment.className) {
                return false
            }
        }
        
        for ((attr, value) in segment.attributes) {
            when (attr) {
                "resource-id" -> {
                    if (node.viewIdResourceName != value) return false
                }
                "text" -> {
                    if (node.text?.toString() != value) return false
                }
                "content-desc" -> {
                    if (node.contentDescription?.toString() != value) return false
                }
            }
        }
        
        return true
    }
    
    /**
     * 将AccessibilityNodeInfo转换为ElementInfo
     */
    private fun convertToElementInfo(
        node: android.view.accessibility.AccessibilityNodeInfo,
        depth: Int
    ): ElementInfo {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        return ElementInfo.Builder()
            .bounds(bounds)
            .text(node.text?.toString())
            .contentDescription(node.contentDescription?.toString())
            .resourceId(node.viewIdResourceName)
            .className(node.className?.toString())
            .packageName(node.packageName?.toString())
            .clickable(node.isClickable)
            .scrollable(node.isScrollable)
            .editable(node.isEditable)
            .enabled(node.isEnabled)
            .visible(node.isVisibleToUser)
            .focusable(node.isFocusable)
            .checkable(node.isCheckable)
            .checked(node.isChecked)
            .depth(depth)
            .childCount(node.childCount)
            .build()
    }
    
    /**
     * 构建结果
     */
    private fun buildResult(
        matchedNodes: List<ElementInfo>,
        locatorConfig: LocatorConfig
    ): RecognizeResult {
        if (matchedNodes.isEmpty()) {
            return RecognizeResult.Failure(
                errorCode = RecognizeResult.Failure.ERROR_NOT_FOUND,
                errorMessage = "未找到匹配的控件"
            )
        }
        
        val results = if (locatorConfig.multiMatch) {
            matchedNodes.take(locatorConfig.maxResults)
        } else {
            listOf(matchedNodes.first())
        }
        
        return if (results.size == 1) {
            RecognizeResult.Success(
                confidence = 1.0f,
                elementInfo = results.first()
            )
        } else {
            RecognizeResult.MultiElement(elements = results)
        }
    }
    
    /**
     * XPath段数据类
     */
    private data class XPathSegment(
        val className: String?,
        val attributes: Map<String, String>,
        val index: Int?
    )
}
