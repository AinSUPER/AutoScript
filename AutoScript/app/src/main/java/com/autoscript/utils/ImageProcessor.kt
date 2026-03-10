package com.autoscript.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.autoscript.service.AccessibilityServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File

class ImageProcessor(private val context: Context) {

    private var isOpenCVInitialized = false

    init {
        initOpenCV()
    }

    private fun initOpenCV(): Boolean {
        if (!isOpenCVInitialized) {
            isOpenCVInitialized = OpenCVLoader.initLocal()
        }
        return isOpenCVInitialized
    }

    fun isInitialized(): Boolean = isOpenCVInitialized

    suspend fun findImage(
        sourceBitmap: Bitmap,
        templateBitmap: Bitmap,
        similarity: Double = 0.9
    ): Rect? = withContext(Dispatchers.Default) {
        if (!isOpenCVInitialized) return@withContext null

        val sourceMat = Mat()
        val templateMat = Mat()
        val resultMat = Mat()

        try {
            Utils.bitmapToMat(sourceBitmap, sourceMat)
            Utils.bitmapToMat(templateBitmap, templateMat)

            Imgproc.cvtColor(sourceMat, sourceMat, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(templateMat, templateMat, Imgproc.COLOR_RGBA2GRAY)

            Imgproc.matchTemplate(sourceMat, templateMat, resultMat, Imgproc.TM_CCOEFF_NORMED)

            val result = Core.minMaxLoc(resultMat)

            if (result.maxVal >= similarity) {
                val templateWidth = templateBitmap.width
                val templateHeight = templateBitmap.height
                
                return@withContext Rect(
                    result.maxLoc.x.toInt(),
                    result.maxLoc.y.toInt(),
                    result.maxLoc.x.toInt() + templateWidth,
                    result.maxLoc.y.toInt() + templateHeight
                )
            }

            null
        } finally {
            sourceMat.release()
            templateMat.release()
            resultMat.release()
        }
    }

    suspend fun findImageInScreen(
        templatePath: String,
        similarity: Double = 0.9
    ): Rect? = withContext(Dispatchers.Default) {
        if (!isOpenCVInitialized) return@withContext null

        val templateFile = File(templatePath)
        if (!templateFile.exists()) return@withContext null

        val templateBitmap = BitmapFactory.decodeFile(templatePath) ?: return@withContext null
        
        val screenBitmap = captureScreen() ?: return@withContext null

        findImage(screenBitmap, templateBitmap, similarity)
    }

    suspend fun findAllImages(
        sourceBitmap: Bitmap,
        templateBitmap: Bitmap,
        similarity: Double = 0.9
    ): List<Rect> = withContext(Dispatchers.Default) {
        if (!isOpenCVInitialized) return@withContext emptyList()

        val results = mutableListOf<Rect>()
        val sourceMat = Mat()
        val templateMat = Mat()
        val resultMat = Mat()

        try {
            Utils.bitmapToMat(sourceBitmap, sourceMat)
            Utils.bitmapToMat(templateBitmap, templateMat)

            Imgproc.cvtColor(sourceMat, sourceMat, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(templateMat, templateMat, Imgproc.COLOR_RGBA2GRAY)

            Imgproc.matchTemplate(sourceMat, templateMat, resultMat, Imgproc.TM_CCOEFF_NORMED)

            val templateWidth = templateBitmap.width
            val templateHeight = templateBitmap.height

            for (row in 0 until resultMat.rows()) {
                for (col in 0 until resultMat.cols()) {
                    val value = resultMat.get(row, col)[0]
                    if (value >= similarity) {
                        results.add(Rect(
                            col,
                            row,
                            col + templateWidth,
                            row + templateHeight
                        ))
                    }
                }
            }

            results
        } finally {
            sourceMat.release()
            templateMat.release()
            resultMat.release()
        }
    }

    fun findColor(
        bitmap: Bitmap,
        targetColor: Int,
        tolerance: Int = 10
    ): Rect? {
        val width = bitmap.width
        val height = bitmap.height

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                if (colorMatch(pixel, targetColor, tolerance)) {
                    return Rect(x, y, x + 1, y + 1)
                }
            }
        }

        return null
    }

    fun findAllColors(
        bitmap: Bitmap,
        targetColor: Int,
        tolerance: Int = 10
    ): List<Rect> {
        val results = mutableListOf<Rect>()
        val width = bitmap.width
        val height = bitmap.height

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                if (colorMatch(pixel, targetColor, tolerance)) {
                    results.add(Rect(x, y, x + 1, y + 1))
                }
            }
        }

        return results
    }

    private fun colorMatch(color1: Int, color2: Int, tolerance: Int): Boolean {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF

        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF

        return kotlin.math.abs(r1 - r2) <= tolerance &&
               kotlin.math.abs(g1 - g2) <= tolerance &&
               kotlin.math.abs(b1 - b2) <= tolerance
    }

    private fun captureScreen(): Bitmap? {
        val service = AccessibilityServiceImpl.getInstance() ?: return null
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use takeScreenshot API for Android 11+
            // This requires additional implementation
        }
        
        return null
    }

    fun compareImages(bitmap1: Bitmap, bitmap2: Bitmap): Double {
        if (!isOpenCVInitialized) return 0.0

        val mat1 = Mat()
        val mat2 = Mat()

        try {
            Utils.bitmapToMat(bitmap1, mat1)
            Utils.bitmapToMat(bitmap2, mat2)

            Imgproc.cvtColor(mat1, mat1, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(mat2, mat2, Imgproc.COLOR_RGBA2GRAY)

            val result = Mat()
            Imgproc.matchTemplate(mat1, mat2, result, Imgproc.TM_CCOEFF_NORMED)

            return Core.minMaxLoc(result).maxVal
        } finally {
            mat1.release()
            mat2.release()
        }
    }
}
