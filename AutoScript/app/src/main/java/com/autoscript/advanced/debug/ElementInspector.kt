package com.autoscript.advanced.debug

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 元素检查器
 * 显示元素信息，支持元素定位和信息获取
 */
class ElementInspector(private val context: Context) {

    /**
     * 元素详细信息
     */
    data class ElementInfo(
        val className: String?,
        val text: String?,
        val contentDescription: String?,
        val viewIdResourceName: String?,
        val bounds: Rect,
        val boundsInScreen: Rect,
        val isClickable: Boolean,
        val isEnabled: Boolean,
        val isFocusable: Boolean,
        val isFocused: Boolean,
        val isSelected: Boolean,
        val isScrollable: Boolean,
        val isCheckable: Boolean,
        val isChecked: Boolean,
        val isEditable: Boolean,
        val isDismissable: Boolean,
        val isAccessibilityFocused: Boolean,
        val isVisibleToUser: Boolean,
        val childCount: Int,
        val depth: Int,
        val packageName: String?,
        val windowId: Int,
        val drawingOrder: Int,
        val extraRenderingInfo: String? = null
    ) {
        val id: String
            get() = viewIdResourceName ?: className?.substringAfterLast('.') ?: "unknown"

        val boundsString: String
            get() = "[${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]"

        val boundsInScreenString: String
            get() = "[${boundsInScreen.left},${boundsInScreen.top}][${boundsInScreen.right},${boundsInScreen.bottom}]"
    }

    /**
     * 检查结果
     */
    data class InspectResult(
        val success: Boolean,
        val elementInfo: ElementInfo? = null,
        val error: String? = null
    )

    /**
     * 从AccessibilityNodeInfo提取元素信息
     * @param nodeInfo 节点信息
     * @return 元素信息
     */
    fun extractElementInfo(nodeInfo: AccessibilityNodeInfo?): ElementInfo? {
        if (nodeInfo == null) return null

        return try {
            val bounds = Rect()
            val boundsInScreen = Rect()
            nodeInfo.getBoundsInParent(bounds)
            nodeInfo.getBoundsInScreen(boundsInScreen)

            ElementInfo(
                className = nodeInfo.className?.toString(),
                text = nodeInfo.text?.toString(),
                contentDescription = nodeInfo.contentDescription?.toString(),
                viewIdResourceName = nodeInfo.viewIdResourceName,
                bounds = bounds,
                boundsInScreen = boundsInScreen,
                isClickable = nodeInfo.isClickable,
                isEnabled = nodeInfo.isEnabled,
                isFocusable = nodeInfo.isFocusable,
                isFocused = nodeInfo.isFocused,
                isSelected = nodeInfo.isSelected,
                isScrollable = nodeInfo.isScrollable,
                isCheckable = nodeInfo.isCheckable,
                isChecked = nodeInfo.isChecked,
                isEditable = nodeInfo.isEditable,
                isDismissable = nodeInfo.isDismissable,
                isAccessibilityFocused = nodeInfo.isAccessibilityFocused,
                isVisibleToUser = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    nodeInfo.isVisibleToUser
                } else {
                    true
                },
                childCount = nodeInfo.childCount,
                depth = calculateDepth(nodeInfo),
                packageName = nodeInfo.packageName?.toString(),
                windowId = nodeInfo.windowId,
                drawingOrder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    nodeInfo.drawingOrder
                } else {
                    0
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 计算节点深度
     */
    private fun calculateDepth(nodeInfo: AccessibilityNodeInfo): Int {
        var depth = 0
        var parent = nodeInfo.parent
        while (parent != null) {
            depth++
            parent = parent.parent
        }
        return depth
    }

    /**
     * 查找指定坐标的元素
     * @param rootInfo 根节点
     * @param x X坐标
     * @param y Y坐标
     * @return 元素信息
     */
    fun findElementAtPosition(rootInfo: AccessibilityNodeInfo?, x: Int, y: Int): ElementInfo? {
        if (rootInfo == null) return null

        val node = findNodeAtPosition(rootInfo, x, y)
        return extractElementInfo(node)
    }

    /**
     * 递归查找指定位置的节点
     */
    private fun findNodeAtPosition(nodeInfo: AccessibilityNodeInfo, x: Int, y: Int): AccessibilityNodeInfo? {
        val bounds = Rect()
        nodeInfo.getBoundsInScreen(bounds)

        if (!bounds.contains(x, y)) {
            return null
        }

        for (i in 0 until nodeInfo.childCount) {
            val child = nodeInfo.getChild(i)
            if (child != null) {
                val result = findNodeAtPosition(child, x, y)
                if (result != null) {
                    return result
                }
            }
        }

        return nodeInfo
    }

    /**
     * 按文本查找元素
     * @param rootInfo 根节点
     * @param text 文本内容
     * @param exact 是否精确匹配
     * @return 元素信息列表
     */
    fun findElementsByText(
        rootInfo: AccessibilityNodeInfo?,
        text: String,
        exact: Boolean = false
    ): List<ElementInfo> {
        if (rootInfo == null) return emptyList()

        val results = mutableListOf<ElementInfo>()
        findNodesByText(rootInfo, text, exact, results)
        return results
    }

    /**
     * 递归查找包含文本的节点
     */
    private fun findNodesByText(
        nodeInfo: AccessibilityNodeInfo,
        text: String,
        exact: Boolean,
        results: MutableList<ElementInfo>
    ) {
        val nodeText = nodeInfo.text?.toString() ?: ""
        val contentDesc = nodeInfo.contentDescription?.toString() ?: ""

        val matched = if (exact) {
            nodeText == text || contentDesc == text
        } else {
            nodeText.contains(text, ignoreCase = true) || contentDesc.contains(text, ignoreCase = true)
        }

        if (matched) {
            extractElementInfo(nodeInfo)?.let { results.add(it) }
        }

        for (i in 0 until nodeInfo.childCount) {
            nodeInfo.getChild(i)?.let { child ->
                findNodesByText(child, text, exact, results)
            }
        }
    }

    /**
     * 按ID查找元素
     * @param rootInfo 根节点
     * @param id 资源ID
     * @return 元素信息列表
     */
    fun findElementsById(rootInfo: AccessibilityNodeInfo?, id: String): List<ElementInfo> {
        if (rootInfo == null) return emptyList()

        val results = mutableListOf<ElementInfo>()
        findNodesById(rootInfo, id, results)
        return results
    }

    /**
     * 递归查找指定ID的节点
     */
    private fun findNodesById(
        nodeInfo: AccessibilityNodeInfo,
        id: String,
        results: MutableList<ElementInfo>
    ) {
        if (nodeInfo.viewIdResourceName == id) {
            extractElementInfo(nodeInfo)?.let { results.add(it) }
        }

        for (i in 0 until nodeInfo.childCount) {
            nodeInfo.getChild(i)?.let { child ->
                findNodesById(child, id, results)
            }
        }
    }

    /**
     * 按类名查找元素
     * @param rootInfo 根节点
     * @param className 类名
     * @return 元素信息列表
     */
    fun findElementsByClassName(rootInfo: AccessibilityNodeInfo?, className: String): List<ElementInfo> {
        if (rootInfo == null) return emptyList()

        val results = mutableListOf<ElementInfo>()
        findNodesByClassName(rootInfo, className, results)
        return results
    }

    /**
     * 递归查找指定类名的节点
     */
    private fun findNodesByClassName(
        nodeInfo: AccessibilityNodeInfo,
        className: String,
        results: MutableList<ElementInfo>
    ) {
        if (nodeInfo.className?.toString() == className) {
            extractElementInfo(nodeInfo)?.let { results.add(it) }
        }

        for (i in 0 until nodeInfo.childCount) {
            nodeInfo.getChild(i)?.let { child ->
                findNodesByClassName(child, className, results)
            }
        }
    }

    /**
     * 查找可点击的元素
     * @param rootInfo 根节点
     * @return 可点击元素列表
     */
    fun findClickableElements(rootInfo: AccessibilityNodeInfo?): List<ElementInfo> {
        if (rootInfo == null) return emptyList()

        val results = mutableListOf<ElementInfo>()
        findClickableNodes(rootInfo, results)
        return results
    }

    /**
     * 递归查找可点击节点
     */
    private fun findClickableNodes(nodeInfo: AccessibilityNodeInfo, results: MutableList<ElementInfo>) {
        if (nodeInfo.isClickable) {
            extractElementInfo(nodeInfo)?.let { results.add(it) }
        }

        for (i in 0 until nodeInfo.childCount) {
            nodeInfo.getChild(i)?.let { child ->
                findClickableNodes(child, results)
            }
        }
    }

    /**
     * 获取元素层级结构
     * @param rootInfo 根节点
     * @param maxDepth 最大深度
     * @return 层级结构字符串
     */
    fun getHierarchy(rootInfo: AccessibilityNodeInfo?, maxDepth: Int = 10): String {
        if (rootInfo == null) return "Empty"

        val builder = StringBuilder()
        buildHierarchy(rootInfo, 0, maxDepth, builder)
        return builder.toString()
    }

    /**
     * 构建层级结构
     */
    private fun buildHierarchy(
        nodeInfo: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int,
        builder: StringBuilder
    ) {
        if (depth > maxDepth) return

        val indent = "  ".repeat(depth)
        val info = extractElementInfo(nodeInfo)

        builder.append("$indent${info?.className ?: "Unknown"}")
        info?.let {
            builder.append(" [${it.boundsInScreenString}]")
            if (!it.text.isNullOrEmpty()) {
                builder.append(" text=\"${it.text}\"")
            }
            if (!it.viewIdResourceName.isNullOrEmpty()) {
                builder.append(" id=\"${it.viewIdResourceName}\"")
            }
            if (it.isClickable) {
                builder.append(" clickable")
            }
        }
        builder.append("\n")

        for (i in 0 until nodeInfo.childCount) {
            nodeInfo.getChild(i)?.let { child ->
                buildHierarchy(child, depth + 1, maxDepth, builder)
            }
        }
    }

    /**
     * 获取元素数量
     * @param rootInfo 根节点
     * @return 元素数量
     */
    fun countElements(rootInfo: AccessibilityNodeInfo?): Int {
        if (rootInfo == null) return 0
        return countNodes(rootInfo)
    }

    /**
     * 递归计数节点
     */
    private fun countNodes(nodeInfo: AccessibilityNodeInfo): Int {
        var count = 1
        for (i in 0 until nodeInfo.childCount) {
            nodeInfo.getChild(i)?.let { child ->
                count += countNodes(child)
            }
        }
        return count
    }

    /**
     * 生成元素XPath
     * @param nodeInfo 节点信息
     * @return XPath字符串
     */
    fun generateXPath(nodeInfo: AccessibilityNodeInfo?): String? {
        if (nodeInfo == null) return null

        val path = mutableListOf<String>()
        var current: AccessibilityNodeInfo? = nodeInfo

        while (current != null) {
            val parent = current.parent
            if (parent != null) {
                val className = current.className?.toString()?.substringAfterLast('.') ?: "*"
                var index = 0
                for (i in 0 until parent.childCount) {
                    val sibling = parent.getChild(i)
                    if (sibling?.className?.toString()?.substringAfterLast('.') == className) {
                        if (sibling == current) {
                            path.add(0, "$className[$index]")
                            break
                        }
                        index++
                    }
                }
            } else {
                path.add(0, current.className?.toString()?.substringAfterLast('.') ?: "root")
            }
            current = parent
        }

        return "//" + path.joinToString("/")
    }

    /**
     * 生成元素选择器
     * @param nodeInfo 节点信息
     * @return 选择器字符串
     */
    fun generateSelector(nodeInfo: AccessibilityNodeInfo?): String {
        if (nodeInfo == null) return ""

        val info = extractElementInfo(nodeInfo) ?: return ""

        return buildString {
            append("className: ${info.className}")

            if (!info.viewIdResourceName.isNullOrEmpty()) {
                append(", id: ${info.viewIdResourceName}")
            }

            if (!info.text.isNullOrEmpty()) {
                append(", text: \"${info.text}\"")
            }

            append(", bounds: ${info.boundsInScreenString}")

            if (info.isClickable) append(", clickable")
            if (info.isScrollable) append(", scrollable")
            if (info.isEditable) append(", editable")
        }
    }
}
