package com.autoscript.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.autoscript.utils.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AccessibilityServiceImpl : AccessibilityService() {

    companion object {
        private const val TAG = "AccessibilityService"

        @Volatile
        private var instance: AccessibilityServiceImpl? = null

        fun getInstance(): AccessibilityServiceImpl? = instance

        fun isServiceEnabled(): Boolean = instance != null
    }

    private var gestureCallback: GestureResultCallback? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        LogUtils.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
    }

    override fun onInterrupt() {
        LogUtils.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        LogUtils.i(TAG, "Accessibility service destroyed")
    }

    fun getRootNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow
    }

    suspend fun click(x: Int, y: Int): Boolean {
        return gestureClick(x.toFloat(), y.toFloat())
    }

    suspend fun longClick(x: Int, y: Int, duration: Long = 500): Boolean {
        return gestureLongClick(x.toFloat(), y.toFloat(), duration)
    }

    suspend fun swipe(
        startX: Int, startY: Int,
        endX: Int, endY: Int,
        duration: Long = 300
    ): Boolean {
        return gestureSwipe(
            startX.toFloat(), startY.toFloat(),
            endX.toFloat(), endY.toFloat(),
            duration
        )
    }

    suspend fun pinch(
        centerX: Int, centerY: Int,
        startDistance: Float, endDistance: Float,
        duration: Long = 300
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        return suspendCancellableCoroutine { continuation ->
            val path1 = Path().apply {
                moveTo(centerX - startDistance / 2, centerY.toFloat())
                lineTo(centerX - endDistance / 2, centerY.toFloat())
            }
            val path2 = Path().apply {
                moveTo(centerX + startDistance / 2, centerY.toFloat())
                lineTo(centerX + endDistance / 2, centerY.toFloat())
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path1, 0, duration))
                .addStroke(GestureDescription.StrokeDescription(path2, 0, duration))
                .build()

            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    continuation.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    continuation.resume(false)
                }
            }

            dispatchGesture(gesture, callback, null)
        }
    }

    private suspend fun gestureClick(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        return withTimeoutOrNull(5000L) {
            suspendCancellableCoroutine { continuation ->
                val path = Path().apply {
                    moveTo(x, y)
                }

                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                    .build()

                val callback = object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        continuation.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        continuation.resume(false)
                    }
                }

                dispatchGesture(gesture, callback, null)
            }
        } ?: false
    }

    private suspend fun gestureLongClick(x: Float, y: Float, duration: Long): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        return withTimeoutOrNull(duration + 5000L) {
            suspendCancellableCoroutine { continuation ->
                val path = Path().apply {
                    moveTo(x, y)
                }

                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                    .build()

                val callback = object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        continuation.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        continuation.resume(false)
                    }
                }

                dispatchGesture(gesture, callback, null)
            }
        } ?: false
    }

    private suspend fun gestureSwipe(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        duration: Long
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        return withTimeoutOrNull(duration + 5000L) {
            suspendCancellableCoroutine { continuation ->
                val path = Path().apply {
                    moveTo(startX, startY)
                    lineTo(endX, endY)
                }

                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                    .build()

                val callback = object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        continuation.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        continuation.resume(false)
                    }
                }

                dispatchGesture(gesture, callback, null)
            }
        } ?: false
    }

    fun inputText(text: String): Boolean {
        val node = findFocusableNode() ?: return false
        
        val arguments = android.os.Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        
        val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        node.recycle()
        
        return result
    }

    private fun findFocusableNode(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        
        val nodes = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return nodes
    }

    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull()
    }

    fun findNodeById(id: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
        return nodes.firstOrNull()
    }

    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun scrollForward(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    fun scrollBackward(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    fun getNodeBounds(node: AccessibilityNodeInfo): Rect {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect
    }
}
