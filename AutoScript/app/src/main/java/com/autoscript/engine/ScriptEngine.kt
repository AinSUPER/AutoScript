package com.autoscript.engine

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.autoscript.data.repository.ScriptRepository
import com.autoscript.service.AccessibilityServiceManager
import com.autoscript.utils.LogUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ScriptEngine(
    private val context: Context,
    private val repository: ScriptRepository,
    private val scope: CoroutineScope
) {
    companion object {
        const val TAG = "ScriptEngine"
    }

    private val _runningState = MutableStateFlow<RunningState>(RunningState.Idle)
    val runningState: StateFlow<RunningState> = _runningState

    private var currentJob: Job? = null
    private var currentScriptId: Long = -1

    private lateinit var accessibilityManager: AccessibilityServiceManager

    fun setAccessibilityManager(manager: AccessibilityServiceManager) {
        this.accessibilityManager = manager
    }

    fun executeScript(scriptId: Long) {
        if (_runningState.value is RunningState.Running) {
            LogUtils.w(TAG, "Script is already running")
            return
        }

        currentScriptId = scriptId
        currentJob = scope.launch {
            try {
                _runningState.value = RunningState.Running(scriptId)
                
                val script = repository.getScriptById(scriptId)
                if (script == null) {
                    _runningState.value = RunningState.Error("Script not found")
                    return@launch
                }

                val parser = ScriptParser()
                val actions = parser.parse(script.content)
                
                for ((index, action) in actions.withIndex()) {
                    if (!isActive) {
                        _runningState.value = RunningState.Stopped
                        break
                    }
                    
                    executeAction(action)
                    repository.incrementRunCount(scriptId)
                }

                if (isActive) {
                    _runningState.value = RunningState.Completed(scriptId)
                }
            } catch (e: CancellationException) {
                _runningState.value = RunningState.Stopped
            } catch (e: Exception) {
                LogUtils.e(TAG, "Script execution error", e)
                _runningState.value = RunningState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun executeAction(action: ScriptAction) {
        when (action) {
            is ScriptAction.Click -> {
                accessibilityManager.click(action.x, action.y)
            }
            is ScriptAction.LongClick -> {
                accessibilityManager.longClick(action.x, action.y)
            }
            is ScriptAction.Swipe -> {
                accessibilityManager.swipe(
                    action.startX, action.startY,
                    action.endX, action.endY,
                    action.duration
                )
            }
            is ScriptAction.Input -> {
                accessibilityManager.inputText(action.text)
            }
            is ScriptAction.Wait -> {
                delay(action.millis)
            }
            is ScriptAction.FindImage -> {
                val found = findImage(action.imagePath, action.similarity)
                if (!found && action.throwIfNotFound) {
                    throw ScriptException("Image not found: ${action.imagePath}")
                }
            }
            is ScriptAction.FindText -> {
                val found = findText(action.text)
                if (!found && action.throwIfNotFound) {
                    throw ScriptException("Text not found: ${action.text}")
                }
            }
            is ScriptAction.Condition -> {
                if (evaluateCondition(action.condition)) {
                    for (subAction in action.trueActions) {
                        executeAction(subAction)
                    }
                } else {
                    for (subAction in action.falseActions) {
                        executeAction(subAction)
                    }
                }
            }
            is ScriptAction.Loop -> {
                repeat(action.count) {
                    for (subAction in action.actions) {
                        if (!isActive) break
                        executeAction(subAction)
                    }
                }
            }
        }
    }

    private suspend fun findImage(imagePath: String, similarity: Float): Boolean {
        return withContext(Dispatchers.IO) {
            true
        }
    }

    private suspend fun findText(text: String): Boolean {
        return withContext(Dispatchers.Default) {
            val rootNode = accessibilityManager.getRootNode() ?: return@withContext false
            findTextInNode(rootNode, text)
        }
    }

    private fun findTextInNode(node: AccessibilityNodeInfo, text: String): Boolean {
        if (node.text?.contains(text) == true) {
            return true
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findTextInNode(child, text)) {
                child.recycle()
                return true
            }
            child.recycle()
        }
        
        return false
    }

    private fun evaluateCondition(condition: Condition): Boolean {
        return true
    }

    fun stopScript() {
        currentJob?.cancel()
        currentJob = null
        _runningState.value = RunningState.Stopped
    }

    fun pauseScript() {
        _runningState.value = RunningState.Paused(currentScriptId)
    }

    fun resumeScript() {
        if (_runningState.value is RunningState.Paused) {
            _runningState.value = RunningState.Running(currentScriptId)
        }
    }

    sealed class RunningState {
        object Idle : RunningState()
        data class Running(val scriptId: Long) : RunningState()
        data class Paused(val scriptId: Long) : RunningState()
        object Stopped : RunningState()
        data class Completed(val scriptId: Long) : RunningState()
        data class Error(val message: String) : RunningState()
    }
}

class ScriptException(message: String) : Exception(message)
