package com.autoscript.model.recognizer

/**
 * 识别结果基类
 * 所有识别器返回结果的统一封装
 */
sealed class RecognizeResult {
    
    abstract val success: Boolean
    abstract val confidence: Float
    abstract val timestamp: Long
    
    data class Success(
        override val success: Boolean = true,
        override val confidence: Float = 1.0f,
        override val timestamp: Long = System.currentTimeMillis(),
        val elementInfo: ElementInfo,
        val matchResults: List<MatchResult> = emptyList()
    ) : RecognizeResult()
    
    data class Failure(
        override val success: Boolean = false,
        override val confidence: Float = 0.0f,
        override val timestamp: Long = System.currentTimeMillis(),
        val errorCode: Int,
        val errorMessage: String
    ) : RecognizeResult() {
        companion object {
            const val ERROR_NOT_FOUND = 1001
            const val ERROR_LOW_CONFIDENCE = 1002
            const val ERROR_TIMEOUT = 1003
            const val ERROR_INVALID_INPUT = 1004
            const val ERROR_SERVICE_UNAVAILABLE = 1005
            const val ERROR_PERMISSION_DENIED = 1006
        }
    }
    
    data class MultiElement(
        override val success: Boolean = true,
        override val confidence: Float = 1.0f,
        override val timestamp: Long = System.currentTimeMillis(),
        val elements: List<ElementInfo>,
        val totalCount: Int = elements.size
    ) : RecognizeResult()
}
