package com.autoscript.advanced.system

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build

/**
 * 剪贴板操作管理
 * 提供复制、粘贴、获取剪贴板内容等功能
 */
class ClipboardManager(private val context: Context) {

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    /**
     * 剪贴板数据类型
     */
    enum class ClipType {
        TEXT, HTML, URI, INTENT, UNKNOWN
    }

    /**
     * 剪贴板数据
     */
    data class ClipData(
        val type: ClipType,
        val text: String? = null,
        val htmlText: String? = null,
        val label: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * 复制文本到剪贴板
     * @param text 文本内容
     * @param label 标签
     * @return 是否成功
     */
    fun copyText(text: String, label: String = "text"): Boolean {
        return try {
            val clip = ClipData.newPlainText(label, text)
            clipboardManager.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 复制HTML文本到剪贴板
     * @param htmlText HTML文本
     * @param plainText 纯文本
     * @param label 标签
     * @return 是否成功
     */
    fun copyHtml(htmlText: String, plainText: String, label: String = "html"): Boolean {
        return try {
            val clip = ClipData.newHtmlText(label, plainText, htmlText)
            clipboardManager.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取剪贴板文本内容
     * @return 文本内容
     */
    fun getText(): String? {
        return try {
            if (!clipboardManager.hasPrimaryClip()) {
                return null
            }

            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).text?.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取剪贴板HTML内容
     * @return HTML内容
     */
    fun getHtmlText(): String? {
        return try {
            if (!clipboardManager.hasPrimaryClip()) {
                return null
            }

            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    clip.getItemAt(0).htmlText
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取剪贴板数据
     * @return 剪贴板数据
     */
    fun getClipData(): ClipData? {
        return try {
            if (!clipboardManager.hasPrimaryClip()) {
                return null
            }

            val clip = clipboardManager.primaryClip ?: return null

            val type = when (clip.description?.getMimeType(0)) {
                "text/plain" -> ClipType.TEXT
                "text/html" -> ClipType.HTML
                "text/uri-list" -> ClipType.URI
                "application/x-android-clip-intent" -> ClipType.INTENT
                else -> ClipType.UNKNOWN
            }

            if (clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                ClipData(
                    type = type,
                    text = item.text?.toString(),
                    htmlText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) item.htmlText else null,
                    label = clip.description?.label?.toString(),
                    timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        clipboardManager.primaryClipDescription?.timestamp ?: System.currentTimeMillis()
                    } else {
                        System.currentTimeMillis()
                    }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查剪贴板是否有内容
     * @return 是否有内容
     */
    fun hasClip(): Boolean {
        return clipboardManager.hasPrimaryClip()
    }

    /**
     * 检查剪贴板是否有文本内容
     * @return 是否有文本
     */
    fun hasText(): Boolean {
        return clipboardManager.hasPrimaryClip() &&
                clipboardManager.primaryClipDescription?.hasMimeType("text/plain") == true
    }

    /**
     * 清空剪贴板
     * @return 是否成功
     */
    fun clear(): Boolean {
        return try {
            val clip = ClipData.newPlainText("", "")
            clipboardManager.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 添加剪贴板变化监听器
     * @param listener 监听器
     */
    fun addOnPrimaryClipChangedListener(listener: () -> Unit) {
        clipboardManager.addPrimaryClipChangedListener(listener)
    }

    /**
     * 移除剪贴板变化监听器
     * @param listener 监听器
     */
    fun removeOnPrimaryClipChangedListener(listener: () -> Unit) {
        clipboardManager.removePrimaryClipChangedListener(listener)
    }

    /**
     * 检查剪贴板内容是否包含指定文本
     * @param text 要检查的文本
     * @param ignoreCase 是否忽略大小写
     * @return 是否包含
     */
    fun containsText(text: String, ignoreCase: Boolean = false): Boolean {
        val clipText = getText() ?: return false
        return clipText.contains(text, ignoreCase)
    }

    /**
     * 检查剪贴板内容是否匹配正则表达式
     * @param pattern 正则表达式
     * @return 是否匹配
     */
    fun matches(pattern: String): Boolean {
        val clipText = getText() ?: return false
        return Regex(pattern).containsMatchIn(clipText)
    }

    /**
     * 从剪贴板提取匹配的文本
     * @param pattern 正则表达式
     * @return 匹配的文本列表
     */
    fun extractMatches(pattern: String): List<String> {
        val clipText = getText() ?: return emptyList()
        return Regex(pattern).findAll(clipText).map { it.value }.toList()
    }

    /**
     * 追加文本到剪贴板
     * @param text 要追加的文本
     * @param separator 分隔符
     * @return 是否成功
     */
    fun appendText(text: String, separator: String = "\n"): Boolean {
        val currentText = getText() ?: ""
        return copyText(currentText + separator + text)
    }

    /**
     * 预添加文本到剪贴板
     * @param text 要预添加的文本
     * @param separator 分隔符
     * @return 是否成功
     */
    fun prependText(text: String, separator: String = "\n"): Boolean {
        val currentText = getText() ?: ""
        return copyText(text + separator + currentText)
    }

    /**
     * 获取剪贴板文本长度
     * @return 文本长度
     */
    fun getTextLength(): Int {
        return getText()?.length ?: 0
    }

    /**
     * 检查剪贴板是否为空
     * @return 是否为空
     */
    fun isEmpty(): Boolean {
        return !hasClip() || getText().isNullOrEmpty()
    }

    /**
     * 获取剪贴板MIME类型
     * @return MIME类型列表
     */
    fun getMimeTypes(): List<String> {
        return try {
            if (!clipboardManager.hasPrimaryClip()) {
                return emptyList()
            }

            val description = clipboardManager.primaryClipDescription ?: return emptyList()
            (0 until description.mimeTypeCount).map { description.getMimeType(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 检查剪贴板是否包含指定MIME类型
     * @param mimeType MIME类型
     * @return 是否包含
     */
    fun hasMimeType(mimeType: String): Boolean {
        return try {
            clipboardManager.primaryClipDescription?.hasMimeType(mimeType) == true
        } catch (e: Exception) {
            false
        }
    }
}
