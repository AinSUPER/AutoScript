package com.autoscript.model.recognizer

import android.graphics.Rect

/**
 * 元素信息数据类
 * 描述识别到的UI元素的完整信息
 */
data class ElementInfo(
    val bounds: Rect = Rect(),
    val centerX: Int = bounds.centerX(),
    val centerY: Int = bounds.centerY(),
    val width: Int = bounds.width(),
    val height: Int = bounds.height(),
    
    val text: String? = null,
    val contentDescription: String? = null,
    val resourceId: String? = null,
    val className: String? = null,
    val packageName: String? = null,
    
    val isClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEditable: Boolean = false,
    val isEnabled: Boolean = true,
    val isVisible: Boolean = true,
    val isFocusable: Boolean = false,
    val isCheckable: Boolean = false,
    val isChecked: Boolean = false,
    
    val depth: Int = 0,
    val indexInParent: Int = -1,
    val childCount: Int = 0,
    
    val extraData: Map<String, Any> = emptyMap()
) {
    
    fun toRect(): Rect = bounds
    
    fun hasText(): Boolean = !text.isNullOrEmpty()
    
    fun hasResourceId(): Boolean = !resourceId.isNullOrEmpty()
    
    fun isValid(): Boolean = !bounds.isEmpty
    
    override fun toString(): String {
        return buildString {
            append("ElementInfo(")
            append("bounds=$bounds, ")
            append("text=$text, ")
            append("resourceId=$resourceId, ")
            append("className=$className")
            append(")")
        }
    }
    
    class Builder {
        private var bounds: Rect = Rect()
        private var text: String? = null
        private var contentDescription: String? = null
        private var resourceId: String? = null
        private var className: String? = null
        private var packageName: String? = null
        private var isClickable: Boolean = false
        private var isScrollable: Boolean = false
        private var isEditable: Boolean = false
        private var isEnabled: Boolean = true
        private var isVisible: Boolean = true
        private var isFocusable: Boolean = false
        private var isCheckable: Boolean = false
        private var isChecked: Boolean = false
        private var depth: Int = 0
        private var indexInParent: Int = -1
        private var childCount: Int = 0
        private var extraData: Map<String, Any> = emptyMap()
        
        fun bounds(rect: Rect) = apply { this.bounds = rect }
        fun bounds(left: Int, top: Int, right: Int, bottom: Int) = apply { 
            this.bounds = Rect(left, top, right, bottom) 
        }
        fun text(text: String?) = apply { this.text = text }
        fun contentDescription(desc: String?) = apply { this.contentDescription = desc }
        fun resourceId(id: String?) = apply { this.resourceId = id }
        fun className(name: String?) = apply { this.className = name }
        fun packageName(name: String?) = apply { this.packageName = name }
        fun clickable(clickable: Boolean) = apply { this.isClickable = clickable }
        fun scrollable(scrollable: Boolean) = apply { this.isScrollable = scrollable }
        fun editable(editable: Boolean) = apply { this.isEditable = editable }
        fun enabled(enabled: Boolean) = apply { this.isEnabled = enabled }
        fun visible(visible: Boolean) = apply { this.isVisible = visible }
        fun focusable(focusable: Boolean) = apply { this.isFocusable = focusable }
        fun checkable(checkable: Boolean) = apply { this.isCheckable = checkable }
        fun checked(checked: Boolean) = apply { this.isChecked = checked }
        fun depth(depth: Int) = apply { this.depth = depth }
        fun indexInParent(index: Int) = apply { this.indexInParent = index }
        fun childCount(count: Int) = apply { this.childCount = count }
        fun extraData(data: Map<String, Any>) = apply { this.extraData = data }
        
        fun build(): ElementInfo = ElementInfo(
            bounds = bounds,
            text = text,
            contentDescription = contentDescription,
            resourceId = resourceId,
            className = className,
            packageName = packageName,
            isClickable = isClickable,
            isScrollable = isScrollable,
            isEditable = isEditable,
            isEnabled = isEnabled,
            isVisible = isVisible,
            isFocusable = isFocusable,
            isCheckable = isCheckable,
            isChecked = isChecked,
            depth = depth,
            indexInParent = indexInParent,
            childCount = childCount,
            extraData = extraData
        )
    }
}
