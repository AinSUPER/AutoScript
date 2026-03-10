package com.autoscript.recognizer.image

import android.graphics.Bitmap
import android.graphics.Rect
import com.autoscript.model.recognizer.MatchResult
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * 模板匹配器
 * 使用OpenCV实现模板匹配，支持多尺度和旋转匹配
 */
class TemplateMatcher {
    
    private var isInitialized = false
    
    /**
     * 初始化OpenCV
     */
    fun initialize(): Boolean {
        if (isInitialized) return true
        
        isInitialized = OpenCVLoader.initLocal()
        return isInitialized
    }
    
    /**
     * 释放资源
     */
    fun release() {
        isInitialized = false
    }
    
    /**
     * 执行模板匹配
     */
    fun match(
        source: Bitmap,
        template: Bitmap,
        confidence: Float = 0.8f,
        region: Rect? = null
    ): MatchResult {
        if (!isInitialized) {
            return MatchResult.Builder()
                .matched(false)
                .confidence(0f)
                .matchType(MatchResult.MatchType.TEMPLATE)
                .build()
        }
        
        val sourceMat = bitmapToMat(source)
        val templateMat = bitmapToMat(template)
        
        try {
            val searchRegion = region?.let {
                Rect(
                    it.left.coerceAtLeast(0),
                    it.top.coerceAtLeast(0),
                    it.right.coerceAtMost(source.width),
                    it.bottom.coerceAtMost(source.height)
                )
            }
            
            val resultMat = if (searchRegion != null) {
                val roiMat = Mat(sourceMat, Rect(searchRegion.left, searchRegion.top, searchRegion.width(), searchRegion.height()))
                performMatch(roiMat, templateMat)
            } else {
                performMatch(sourceMat, templateMat)
            }
            
            val bestMatch = findBestMatch(resultMat, template, searchRegion)
            
            return if (bestMatch.confidence >= confidence) {
                bestMatch
            } else {
                MatchResult.Builder()
                    .matched(false)
                    .confidence(bestMatch.confidence)
                    .matchType(MatchResult.MatchType.TEMPLATE)
                    .build()
            }
        } finally {
            sourceMat.release()
            templateMat.release()
        }
    }
    
    /**
     * 多尺度模板匹配
     */
    fun matchMultiScale(
        source: Bitmap,
        template: Bitmap,
        confidence: Float = 0.8f,
        scales: FloatArray = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f),
        region: Rect? = null
    ): MatchResult {
        if (!isInitialized) {
            return MatchResult.Builder()
                .matched(false)
                .confidence(0f)
                .matchType(MatchResult.MatchType.TEMPLATE)
                .build()
        }
        
        var bestResult: MatchResult? = null
        
        for (scale in scales) {
            val scaledTemplate = scaleBitmap(template, scale)
            val result = match(source, scaledTemplate, confidence, region)
            
            if (result.matched) {
                if (bestResult == null || result.confidence > bestResult.confidence) {
                    bestResult = result.copy(scale = scale)
                }
            }
            
            if (scaledTemplate != template) {
                scaledTemplate.recycle()
            }
        }
        
        return bestResult ?: MatchResult.Builder()
            .matched(false)
            .confidence(0f)
            .matchType(MatchResult.MatchType.TEMPLATE)
            .build()
    }
    
    /**
     * 旋转模板匹配
     */
    fun matchWithRotation(
        source: Bitmap,
        template: Bitmap,
        confidence: Float = 0.8f,
        angles: FloatArray = floatArrayOf(0f, 90f, 180f, 270f),
        region: Rect? = null
    ): MatchResult {
        if (!isInitialized) {
            return MatchResult.Builder()
                .matched(false)
                .confidence(0f)
                .matchType(MatchResult.MatchType.TEMPLATE)
                .build()
        }
        
        var bestResult: MatchResult? = null
        
        for (angle in angles) {
            val rotatedTemplate = rotateBitmap(template, angle)
            val result = match(source, rotatedTemplate, confidence, region)
            
            if (result.matched) {
                if (bestResult == null || result.confidence > bestResult.confidence) {
                    bestResult = result.copy(rotation = angle)
                }
            }
            
            if (rotatedTemplate != template) {
                rotatedTemplate.recycle()
            }
        }
        
        return bestResult ?: MatchResult.Builder()
            .matched(false)
            .confidence(0f)
            .matchType(MatchResult.MatchType.TEMPLATE)
            .build()
    }
    
    /**
     * 查找所有匹配
     */
    fun matchAll(
        source: Bitmap,
        template: Bitmap,
        confidence: Float = 0.8f,
        maxResults: Int = 10,
        region: Rect? = null
    ): List<MatchResult> {
        if (!isInitialized) {
            return emptyList()
        }
        
        val sourceMat = bitmapToMat(source)
        val templateMat = bitmapToMat(template)
        
        try {
            val resultMat = performMatch(sourceMat, templateMat)
            val matches = findAllMatches(resultMat, template, confidence, maxResults)
            return matches
        } finally {
            sourceMat.release()
            templateMat.release()
        }
    }
    
    /**
     * 执行OpenCV模板匹配
     */
    private fun performMatch(source: Mat, template: Mat): Mat {
        val result = Mat()
        Imgproc.matchTemplate(source, template, result, Imgproc.TM_CCOEFF_NORMED)
        return result
    }
    
    /**
     * 查找最佳匹配位置
     */
    private fun findBestMatch(resultMat: Mat, template: Bitmap, region: Rect?): MatchResult {
        val minMaxResult = Core.minMaxLoc(resultMat)
        
        val maxVal = minMaxResult.maxVal.toFloat()
        val maxLoc = minMaxResult.maxLoc
        
        val offsetX = region?.left ?: 0
        val offsetY = region?.top ?: 0
        
        val left = (maxLoc.x + offsetX).toInt()
        val top = (maxLoc.y + offsetY).toInt()
        val right = left + template.width
        val bottom = top + template.height
        
        return MatchResult.Builder()
            .matched(maxVal >= 0.5f)
            .confidence(maxVal)
            .bounds(Rect(left, top, right, bottom))
            .matchType(MatchResult.MatchType.TEMPLATE)
            .build()
    }
    
    /**
     * 查找所有匹配位置
     */
    private fun findAllMatches(
        resultMat: Mat,
        template: Bitmap,
        confidence: Float,
        maxResults: Int
    ): List<MatchResult> {
        val results = mutableListOf<MatchResult>()
        val threshold = confidence.toDouble()
        
        val locations = mutableListOf<Point>()
        val mask = Mat.zeros(resultMat.size(), CvType.CV_8U)
        
        while (results.size < maxResults) {
            val minMaxResult = Core.minMaxLoc(resultMat, mask)
            
            if (minMaxResult.maxVal < threshold) break
            
            val maxLoc = minMaxResult.maxLoc
            locations.add(maxLoc)
            
            val left = maxLoc.x.toInt()
            val top = maxLoc.y.toInt()
            val right = left + template.width
            val bottom = top + template.height
            
            val matchResult = MatchResult.Builder()
                .matched(true)
                .confidence(minMaxResult.maxVal.toFloat())
                .bounds(Rect(left, top, right, bottom))
                .matchType(MatchResult.MatchType.TEMPLATE)
                .build()
            
            results.add(matchResult)
            
            Imgproc.rectangle(
                mask,
                Point(maxLoc.x - template.width / 2, maxLoc.y - template.height / 2),
                Point(maxLoc.x + template.width / 2, maxLoc.y + template.height / 2),
                Scalar(255.0),
                -1
            )
        }
        
        mask.release()
        return results
    }
    
    /**
     * Bitmap转Mat
     */
    private fun bitmapToMat(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        return mat
    }
    
    /**
     * Mat转Bitmap
     */
    private fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
    
    /**
     * 缩放Bitmap
     */
    private fun scaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        if (scale == 1.0f) return bitmap
        
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    
    /**
     * 旋转Bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
        if (angle == 0f) return bitmap
        
        val matrix = android.graphics.Matrix()
        matrix.postRotate(angle)
        
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
    
    /**
     * 计算图片相似度
     */
    fun calculateSimilarity(bitmap1: Bitmap, bitmap2: Bitmap): Float {
        if (!isInitialized) return 0f
        
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
            return 0f
        }
        
        val mat1 = bitmapToMat(bitmap1)
        val mat2 = bitmapToMat(bitmap2)
        
        try {
            val hist1 = calculateHistogram(mat1)
            val hist2 = calculateHistogram(mat2)
            
            return Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_CORREL).toFloat()
        } finally {
            mat1.release()
            mat2.release()
        }
    }
    
    /**
     * 计算直方图
     */
    private fun calculateHistogram(mat: Mat): Mat {
        val hsvMat = Mat()
        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_RGB2HSV)
        
        val hist = Mat()
        val channels = intArrayOf(0)
        val histSize = MatOfInt(50)
        val ranges = MatOfFloat(0f, 180f)
        
        Imgproc.calcHist(listOf(hsvMat), MatOfInt(*channels), Mat(), hist, histSize, ranges)
        
        hsvMat.release()
        return hist
    }
}
