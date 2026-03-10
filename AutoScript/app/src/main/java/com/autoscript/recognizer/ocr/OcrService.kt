package com.autoscript.recognizer.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCR服务
 * 使用ML Kit实现文字识别，支持中英文
 */
class OcrService {
    
    private var chineseRecognizer: TextRecognizer? = null
    private var latinRecognizer: TextRecognizer? = null
    private var isInitialized = false
    
    /**
     * 初始化OCR识别器
     */
    fun initialize(): Boolean {
        return try {
            chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            isInitialized = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "OCR初始化失败: ${e.message}")
            false
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        chineseRecognizer?.close()
        latinRecognizer?.close()
        chineseRecognizer = null
        latinRecognizer = null
        isInitialized = false
    }
    
    /**
     * 识别文本
     */
    suspend fun recognizeText(
        bitmap: Bitmap,
        region: Rect? = null,
        language: String = "zh"
    ): String? = suspendCancellableCoroutine { continuation ->
        if (!isInitialized) {
            continuation.resumeWithException(OcrException("OCR服务未初始化"))
            return@suspendCancellableCoroutine
        }
        
        val recognizer = getRecognizer(language)
        if (recognizer == null) {
            continuation.resumeWithException(OcrException("不支持的识别器"))
            return@suspendCancellableCoroutine
        }
        
        val inputImage = if (region != null) {
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                region.left.coerceAtLeast(0),
                region.top.coerceAtLeast(0),
                region.width().coerceAtMost(bitmap.width - region.left),
                region.height().coerceAtMost(bitmap.height - region.top)
            )
            InputImage.fromBitmap(croppedBitmap, 0)
        } else {
            InputImage.fromBitmap(bitmap, 0)
        }
        
        recognizer.process(inputImage)
            .addOnSuccessListener { text ->
                continuation.resume(text.text)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(OcrException("识别失败: ${e.message}"))
            }
    }
    
    /**
     * 识别文本并返回文本块信息
     */
    suspend fun recognizeTextWithBlocks(
        bitmap: Bitmap,
        region: Rect? = null,
        language: String = "zh"
    ): List<TextBlock> = suspendCancellableCoroutine { continuation ->
        if (!isInitialized) {
            continuation.resumeWithException(OcrException("OCR服务未初始化"))
            return@suspendCancellableCoroutine
        }
        
        val recognizer = getRecognizer(language)
        if (recognizer == null) {
            continuation.resumeWithException(OcrException("不支持的识别器"))
            return@suspendCancellableCoroutine
        }
        
        val inputImage = if (region != null) {
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                region.left.coerceAtLeast(0),
                region.top.coerceAtLeast(0),
                region.width().coerceAtMost(bitmap.width - region.left),
                region.height().coerceAtMost(bitmap.height - region.top)
            )
            InputImage.fromBitmap(croppedBitmap, 0)
        } else {
            InputImage.fromBitmap(bitmap, 0)
        }
        
        recognizer.process(inputImage)
            .addOnSuccessListener { text ->
                val blocks = convertToTextBlocks(text, region)
                continuation.resume(blocks)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(OcrException("识别失败: ${e.message}"))
            }
    }
    
    /**
     * 获取指定语言的识别器
     */
    private fun getRecognizer(language: String): TextRecognizer? {
        return when (language.lowercase()) {
            "zh", "chinese", "cn" -> chineseRecognizer
            "en", "english", "latin" -> latinRecognizer
            else -> chineseRecognizer
        }
    }
    
    /**
     * 将ML Kit Text转换为TextBlock列表
     */
    private fun convertToTextBlocks(text: Text, region: Rect?): List<TextBlock> {
        val blocks = mutableListOf<TextBlock>()
        val offsetX = region?.left ?: 0
        val offsetY = region?.top ?: 0
        
        for (textBlock in text.textBlocks) {
            val boundingBox = textBlock.boundingBox
            if (boundingBox != null) {
                val adjustedRect = Rect(
                    boundingBox.left + offsetX,
                    boundingBox.top + offsetY,
                    boundingBox.right + offsetX,
                    boundingBox.bottom + offsetY
                )
                
                val lines = textBlock.lines.map { line ->
                    TextLine(
                        text = line.text,
                        boundingBox = line.boundingBox?.let {
                            Rect(
                                it.left + offsetX,
                                it.top + offsetY,
                                it.right + offsetX,
                                it.bottom + offsetY
                            )
                        } ?: Rect(),
                        confidence = line.confidence ?: 1.0f
                    )
                }
                
                blocks.add(
                    TextBlock(
                        text = textBlock.text,
                        bounds = adjustedRect,
                        lines = lines,
                        confidence = textBlock.confidence ?: 1.0f
                    )
                )
            }
        }
        
        return blocks
    }
    
    /**
     * 查找包含指定文本的文本块
     */
    suspend fun findText(
        bitmap: Bitmap,
        targetText: String,
        region: Rect? = null,
        language: String = "zh",
        caseSensitive: Boolean = false
    ): TextBlock? {
        val blocks = recognizeTextWithBlocks(bitmap, region, language)
        
        val searchText = if (caseSensitive) targetText else targetText.lowercase()
        
        return blocks.find { block ->
            val blockText = if (caseSensitive) block.text else block.text.lowercase()
            blockText.contains(searchText)
        }
    }
    
    /**
     * 查找所有包含指定文本的文本块
     */
    suspend fun findAllText(
        bitmap: Bitmap,
        targetText: String,
        region: Rect? = null,
        language: String = "zh",
        caseSensitive: Boolean = false
    ): List<TextBlock> {
        val blocks = recognizeTextWithBlocks(bitmap, region, language)
        
        val searchText = if (caseSensitive) targetText else targetText.lowercase()
        
        return blocks.filter { block ->
            val blockText = if (caseSensitive) block.text else block.text.lowercase()
            blockText.contains(searchText)
        }
    }
    
    /**
     * 使用正则表达式查找文本
     */
    suspend fun findTextByRegex(
        bitmap: Bitmap,
        pattern: String,
        region: Rect? = null,
        language: String = "zh"
    ): List<TextBlock> {
        val blocks = recognizeTextWithBlocks(bitmap, region, language)
        
        return try {
            val regex = Regex(pattern)
            blocks.filter { block ->
                regex.containsMatchIn(block.text)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 文本块数据类
     */
    data class TextBlock(
        val text: String,
        val bounds: Rect,
        val lines: List<TextLine> = emptyList(),
        val confidence: Float = 1.0f
    ) {
        val centerX: Int
            get() = bounds.centerX()
        
        val centerY: Int
            get() = bounds.centerY()
        
        val width: Int
            get() = bounds.width()
        
        val height: Int
            get() = bounds.height()
    }
    
    /**
     * 文本行数据类
     */
    data class TextLine(
        val text: String,
        val boundingBox: Rect,
        val confidence: Float = 1.0f
    )
    
    /**
     * OCR异常类
     */
    class OcrException(message: String) : Exception(message)
    
    companion object {
        private const val TAG = "OcrService"
        
        const val LANGUAGE_CHINESE = "zh"
        const val LANGUAGE_ENGLISH = "en"
    }
}
