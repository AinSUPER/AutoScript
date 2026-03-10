package com.autoscript.recognizer.image

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import com.autoscript.model.recognizer.MatchResult
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.features2d.AKAZE
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.features2d.SIFT
import org.opencv.imgproc.Imgproc

/**
 * 特征点匹配器
 * 使用OpenCV实现SIFT、ORB等特征点检测和匹配
 */
class FeatureMatcher {
    
    private var isInitialized = false
    
    private var siftDetector: SIFT? = null
    private var orbDetector: ORB? = null
    private var akazeDetector: AKAZE? = null
    private var matcher: DescriptorMatcher? = null
    
    /**
     * 初始化OpenCV和特征检测器
     */
    fun initialize(): Boolean {
        if (isInitialized) return true
        
        val opencvLoaded = OpenCVLoader.initLocal()
        if (!opencvLoaded) return false
        
        try {
            siftDetector = SIFT.create()
            orbDetector = ORB.create()
            akazeDetector = AKAZE.create()
            matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
            
            isInitialized = true
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        siftDetector = null
        orbDetector = null
        akazeDetector = null
        matcher = null
        isInitialized = false
    }
    
    /**
     * 特征检测方法枚举
     */
    enum class FeatureMethod {
        SIFT,
        ORB,
        AKAZE
    }
    
    /**
     * 执行特征点匹配
     */
    fun match(
        source: Bitmap,
        template: Bitmap,
        confidence: Float = 0.7f
    ): MatchResult {
        return matchWithMethod(source, template, FeatureMethod.ORB, confidence)
    }
    
    /**
     * 使用指定方法执行特征点匹配
     */
    fun matchWithMethod(
        source: Bitmap,
        template: Bitmap,
        method: FeatureMethod,
        confidence: Float = 0.7f
    ): MatchResult {
        if (!isInitialized) {
            return MatchResult.Builder()
                .matched(false)
                .confidence(0f)
                .matchType(MatchResult.MatchType.FEATURE)
                .build()
        }
        
        val sourceMat = bitmapToMat(source)
        val templateMat = bitmapToMat(template)
        
        try {
            val sourceGray = Mat()
            val templateGray = Mat()
            
            Imgproc.cvtColor(sourceMat, sourceGray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(templateMat, templateGray, Imgproc.COLOR_RGBA2GRAY)
            
            val (sourceKeypoints, sourceDescriptors) = detectAndCompute(sourceGray, method)
            val (templateKeypoints, templateDescriptors) = detectAndCompute(templateGray, method)
            
            if (sourceDescriptors.empty() || templateDescriptors.empty()) {
                return MatchResult.Builder()
                    .matched(false)
                    .confidence(0f)
                    .matchType(MatchResult.MatchType.FEATURE)
                    .build()
            }
            
            val matches = matchDescriptors(sourceDescriptors, templateDescriptors, method)
            
            val result = findObjectLocation(
                sourceKeypoints,
                templateKeypoints,
                matches,
                template.width,
                template.height,
                confidence
            )
            
            sourceGray.release()
            templateGray.release()
            
            return result
        } finally {
            sourceMat.release()
            templateMat.release()
        }
    }
    
    /**
     * 检测特征点并计算描述符
     */
    private fun detectAndCompute(
        image: Mat,
        method: FeatureMethod
    ): Pair<MatOfKeyPoint, Mat> {
        val keypoints = MatOfKeyPoint()
        val descriptors = Mat()
        
        when (method) {
            FeatureMethod.SIFT -> siftDetector?.detectAndCompute(image, Mat(), keypoints, descriptors)
            FeatureMethod.ORB -> orbDetector?.detectAndCompute(image, Mat(), keypoints, descriptors)
            FeatureMethod.AKAZE -> akazeDetector?.detectAndCompute(image, Mat(), keypoints, descriptors)
        }
        
        return Pair(keypoints, descriptors)
    }
    
    /**
     * 匹配描述符
     */
    private fun matchDescriptors(
        sourceDescriptors: Mat,
        templateDescriptors: Mat,
        method: FeatureMethod
    ): MatOfDMatch {
        val matches = MatOfDMatch()
        
        when (method) {
            FeatureMethod.SIFT -> {
                val bfMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE)
                bfMatcher.match(templateDescriptors, sourceDescriptors, matches)
            }
            FeatureMethod.ORB,
            FeatureMethod.AKAZE -> {
                matcher?.match(templateDescriptors, sourceDescriptors, matches)
            }
        }
        
        return matches
    }
    
    /**
     * 查找目标位置
     */
    private fun findObjectLocation(
        sourceKeypoints: MatOfKeyPoint,
        templateKeypoints: MatOfKeyPoint,
        matches: MatOfDMatch,
        templateWidth: Int,
        templateHeight: Int,
        confidence: Float
    ): MatchResult {
        val matchesList = matches.toList()
        
        if (matchesList.isEmpty()) {
            return MatchResult.Builder()
                .matched(false)
                .confidence(0f)
                .matchType(MatchResult.MatchType.FEATURE)
                .build()
        }
        
        val minDist = matchesList.minOf { it.distance }
        val goodMatches = matchesList.filter { it.distance <= Math.max(2 * minDist, 30.0) }
        
        if (goodMatches.size < 4) {
            return MatchResult.Builder()
                .matched(false)
                .confidence(goodMatches.size.toFloat() / matchesList.size)
                .matchType(MatchResult.MatchType.FEATURE)
                .build()
        }
        
        val sourcePointsList = mutableListOf<Point>()
        val templatePointsList = mutableListOf<Point>()
        
        val sourceKpList = sourceKeypoints.toList()
        val templateKpList = templateKeypoints.toList()
        
        for (match in goodMatches) {
            sourcePointsList.add(sourceKpList[match.trainIdx].pt)
            templatePointsList.add(templateKpList[match.queryIdx].pt)
        }
        
        val sourcePoints = MatOfPoint2f()
        sourcePoints.fromList(sourcePointsList)
        
        val templatePoints = MatOfPoint2f()
        templatePoints.fromList(templatePointsList)
        
        return try {
            val homography = Calib3d.findHomography(templatePoints, sourcePoints, Calib3d.RANSAC, 5.0)
            
            if (homography.empty()) {
                MatchResult.Builder()
                    .matched(false)
                    .confidence(goodMatches.size.toFloat() / matchesList.size)
                    .matchType(MatchResult.MatchType.FEATURE)
                    .build()
            } else {
                val objCorners = MatOfPoint2f(
                    Point(0.0, 0.0),
                    Point(templateWidth.toDouble(), 0.0),
                    Point(templateWidth.toDouble(), templateHeight.toDouble()),
                    Point(0.0, templateHeight.toDouble())
                )
                
                val sceneCorners = MatOfPoint2f()
                Core.perspectiveTransform(objCorners, sceneCorners, homography)
                
                val corners = sceneCorners.toList()
                val minX = corners.minOf { it.x }.toInt()
                val minY = corners.minOf { it.y }.toInt()
                val maxX = corners.maxOf { it.x }.toInt()
                val maxY = corners.maxOf { it.y }.toInt()
                
                val matchConfidence = goodMatches.size.toFloat() / matchesList.size
                
                MatchResult.Builder()
                    .matched(matchConfidence >= confidence)
                    .confidence(matchConfidence)
                    .bounds(Rect(minX, minY, maxX, maxY))
                    .matchType(MatchResult.MatchType.FEATURE)
                    .build()
            }
        } catch (e: Exception) {
            MatchResult.Builder()
                .matched(false)
                .confidence(goodMatches.size.toFloat() / matchesList.size)
                .matchType(MatchResult.MatchType.FEATURE)
                .build()
        }
    }
    
    /**
     * 使用KNN匹配
     */
    fun matchKNN(
        source: Bitmap,
        template: Bitmap,
        method: FeatureMethod = FeatureMethod.ORB,
        confidence: Float = 0.7f,
        ratioThreshold: Float = 0.75f
    ): MatchResult {
        if (!isInitialized) {
            return MatchResult.Builder()
                .matched(false)
                .confidence(0f)
                .matchType(MatchResult.MatchType.FEATURE)
                .build()
        }
        
        val sourceMat = bitmapToMat(source)
        val templateMat = bitmapToMat(template)
        
        try {
            val sourceGray = Mat()
            val templateGray = Mat()
            
            Imgproc.cvtColor(sourceMat, sourceGray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(templateMat, templateGray, Imgproc.COLOR_RGBA2GRAY)
            
            val (sourceKeypoints, sourceDescriptors) = detectAndCompute(sourceGray, method)
            val (templateKeypoints, templateDescriptors) = detectAndCompute(templateGray, method)
            
            if (sourceDescriptors.empty() || templateDescriptors.empty()) {
                return MatchResult.Builder()
                    .matched(false)
                    .confidence(0f)
                    .matchType(MatchResult.MatchType.FEATURE)
                    .build()
            }
            
            val knnMatches = mutableListOf<MatOfDMatch>()
            val bfMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
            bfMatcher.knnMatch(templateDescriptors, sourceDescriptors, knnMatches, 2)
            
            val goodMatches = mutableListOf<DMatch>()
            for (match in knnMatches) {
                val matchList = match.toList()
                if (matchList.size >= 2) {
                    if (matchList[0].distance < ratioThreshold * matchList[1].distance) {
                        goodMatches.add(matchList[0])
                    }
                }
            }
            
            if (goodMatches.size < 4) {
                return MatchResult.Builder()
                    .matched(false)
                    .confidence(goodMatches.size.toFloat() / knnMatches.size)
                    .matchType(MatchResult.MatchType.FEATURE)
                    .build()
            }
            
            val sourcePointsList = mutableListOf<Point>()
            val templatePointsList = mutableListOf<Point>()
            
            val sourceKpList = sourceKeypoints.toList()
            val templateKpList = templateKeypoints.toList()
            
            for (match in goodMatches) {
                sourcePointsList.add(sourceKpList[match.trainIdx].pt)
                templatePointsList.add(templateKpList[match.queryIdx].pt)
            }
            
            val sourcePoints = MatOfPoint2f()
            sourcePoints.fromList(sourcePointsList)
            
            val templatePoints = MatOfPoint2f()
            templatePoints.fromList(templatePointsList)
            
            val homography = Calib3d.findHomography(templatePoints, sourcePoints, Calib3d.RANSAC, 5.0)
            
            if (homography.empty()) {
                return MatchResult.Builder()
                    .matched(false)
                    .confidence(goodMatches.size.toFloat() / knnMatches.size)
                    .matchType(MatchResult.MatchType.FEATURE)
                    .build()
            }
            
            val objCorners = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(template.width.toDouble(), 0.0),
                Point(template.width.toDouble(), template.height.toDouble()),
                Point(0.0, template.height.toDouble())
            )
            
            val sceneCorners = MatOfPoint2f()
            Core.perspectiveTransform(objCorners, sceneCorners, homography)
            
            val corners = sceneCorners.toList()
            val minX = corners.minOf { it.x }.toInt()
            val minY = corners.minOf { it.y }.toInt()
            val maxX = corners.maxOf { it.x }.toInt()
            val maxY = corners.maxOf { it.y }.toInt()
            
            val matchConfidence = goodMatches.size.toFloat() / knnMatches.size
            
            return MatchResult.Builder()
                .matched(matchConfidence >= confidence)
                .confidence(matchConfidence)
                .bounds(Rect(minX, minY, maxX, maxY))
                .matchType(MatchResult.MatchType.FEATURE)
                .build()
        } finally {
            sourceMat.release()
            templateMat.release()
        }
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
     * 获取特征点数量
     */
    fun getKeypointCount(bitmap: Bitmap, method: FeatureMethod = FeatureMethod.ORB): Int {
        if (!isInitialized) return 0
        
        val mat = bitmapToMat(bitmap)
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        
        val (keypoints, _) = detectAndCompute(gray, method)
        
        gray.release()
        mat.release()
        
        return keypoints.rows()
    }
}
