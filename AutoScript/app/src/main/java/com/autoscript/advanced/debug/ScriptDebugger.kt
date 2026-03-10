package com.autoscript.advanced.debug

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 脚本调试器
 * 支持单步执行、断点调试、变量监视
 */
class ScriptDebugger {

    /**
     * 调试状态
     */
    enum class DebugState {
        IDLE, RUNNING, PAUSED, STEP_OVER, STEP_INTO, STEP_OUT, STOPPED
    }

    /**
     * 断点
     */
    data class Breakpoint(
        val id: String,
        val scriptId: String,
        val lineNumber: Int,
        val condition: String? = null,
        val hitCount: Int = 0,
        val enabled: Boolean = true
    )

    /**
     * 调试帧
     */
    data class DebugFrame(
        val scriptId: String,
        val scriptName: String,
        val lineNumber: Int,
        val functionName: String?,
        val variables: Map<String, Any?>
    )

    /**
     * 调试事件
     */
    sealed class DebugEvent {
        data class BreakpointHit(val breakpoint: Breakpoint, val frame: DebugFrame) : DebugEvent()
        data class StepComplete(val frame: DebugFrame) : DebugEvent()
        data class ExceptionThrown(val exception: Throwable, val frame: DebugFrame) : DebugEvent()
        data class ScriptEnded(val scriptId: String) : DebugEvent()
        data class VariableChanged(val name: String, val oldValue: Any?, val newValue: Any?) : DebugEvent()
    }

    /**
     * 调试配置
     */
    data class DebugConfig(
        val breakOnException: Boolean = true,
        val breakOnStart: Boolean = false,
        val maxCallStackDepth: Int = 100,
        val variableWatchInterval: Long = 100
    )

    private val state = AtomicInteger(DebugState.IDLE.ordinal)
    private val breakpoints = ConcurrentHashMap<String, MutableList<Breakpoint>>()
    private val callStack = CopyOnWriteArrayList<DebugFrame>()
    private val watchedVariables = ConcurrentHashMap<String, Any?>()
    private val eventListeners = mutableListOf<(DebugEvent) -> Unit>()

    private var currentScriptId: String? = null
    private var currentLineNumber: Int = 0
    private var pauseReason: String? = null
    private var continueCondition = CompletableDeferred<Unit>()

    private var config = DebugConfig()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 获取当前调试状态
     */
    fun getState(): DebugState {
        return DebugState.values()[state.get()]
    }

    /**
     * 设置调试配置
     */
    fun setConfig(config: DebugConfig) {
        this.config = config
    }

    /**
     * 添加断点
     * @param scriptId 脚本ID
     * @param lineNumber 行号
     * @param condition 条件表达式
     * @return 断点ID
     */
    fun addBreakpoint(scriptId: String, lineNumber: Int, condition: String? = null): String {
        val breakpointId = "bp_${System.currentTimeMillis()}_${lineNumber}"
        val breakpoint = Breakpoint(
            id = breakpointId,
            scriptId = scriptId,
            lineNumber = lineNumber,
            condition = condition
        )

        breakpoints.getOrPut(scriptId) { mutableListOf() }.add(breakpoint)
        return breakpointId
    }

    /**
     * 移除断点
     * @param breakpointId 断点ID
     */
    fun removeBreakpoint(breakpointId: String) {
        breakpoints.values.forEach { list ->
            list.removeAll { it.id == breakpointId }
        }
    }

    /**
     * 移除脚本的所有断点
     * @param scriptId 脚本ID
     */
    fun removeAllBreakpoints(scriptId: String) {
        breakpoints.remove(scriptId)
    }

    /**
     * 获取脚本的所有断点
     * @param scriptId 脚本ID
     * @return 断点列表
     */
    fun getBreakpoints(scriptId: String): List<Breakpoint> {
        return breakpoints[scriptId]?.toList() ?: emptyList()
    }

    /**
     * 启用/禁用断点
     * @param breakpointId 断点ID
     * @param enabled 是否启用
     */
    fun setBreakpointEnabled(breakpointId: String, enabled: Boolean) {
        breakpoints.values.forEach { list ->
            list.find { it.id == breakpointId }?.let { bp ->
                val index = list.indexOf(bp)
                list[index] = bp.copy(enabled = enabled)
            }
        }
    }

    /**
     * 添加事件监听器
     * @param listener 监听器
     */
    fun addEventListener(listener: (DebugEvent) -> Unit) {
        eventListeners.add(listener)
    }

    /**
     * 移除事件监听器
     * @param listener 监听器
     */
    fun removeEventListener(listener: (DebugEvent) -> Unit) {
        eventListeners.remove(listener)
    }

    /**
     * 发送调试事件
     */
    private fun sendEvent(event: DebugEvent) {
        eventListeners.forEach { it(event) }
    }

    /**
     * 开始调试会话
     * @param scriptId 脚本ID
     */
    fun startSession(scriptId: String) {
        currentScriptId = scriptId
        currentLineNumber = 0
        callStack.clear()
        watchedVariables.clear()
        state.set(DebugState.RUNNING.ordinal)

        if (config.breakOnStart) {
            pause("启动断点")
        }
    }

    /**
     * 结束调试会话
     */
    fun endSession() {
        state.set(DebugState.STOPPED.ordinal)
        currentScriptId = null
        callStack.clear()

        if (continueCondition.isActive) {
            continueCondition.complete(Unit)
        }

        sendEvent(DebugEvent.ScriptEnded(currentScriptId ?: ""))
    }

    /**
     * 暂停执行
     * @param reason 暂停原因
     */
    suspend fun pause(reason: String = "用户暂停") {
        if (state.get() == DebugState.RUNNING.ordinal) {
            state.set(DebugState.PAUSED.ordinal)
            pauseReason = reason

            val frame = getCurrentFrame()
            if (frame != null) {
                sendEvent(DebugEvent.StepComplete(frame))
            }

            continueCondition = CompletableDeferred()
            continueCondition.await()
        }
    }

    /**
     * 同步暂停
     */
    fun pauseSync(reason: String = "用户暂停") {
        scope.launch {
            pause(reason)
        }
    }

    /**
     * 继续执行
     */
    fun continueExecution() {
        if (state.get() == DebugState.PAUSED.ordinal) {
            state.set(DebugState.RUNNING.ordinal)
            pauseReason = null

            if (continueCondition.isActive) {
                continueCondition.complete(Unit)
            }
        }
    }

    /**
     * 单步跳过
     */
    fun stepOver() {
        if (state.get() == DebugState.PAUSED.ordinal) {
            state.set(DebugState.STEP_OVER.ordinal)
            pauseReason = null

            if (continueCondition.isActive) {
                continueCondition.complete(Unit)
            }
        }
    }

    /**
     * 单步进入
     */
    fun stepInto() {
        if (state.get() == DebugState.PAUSED.ordinal) {
            state.set(DebugState.STEP_INTO.ordinal)
            pauseReason = null

            if (continueCondition.isActive) {
                continueCondition.complete(Unit)
            }
        }
    }

    /**
     * 单步跳出
     */
    fun stepOut() {
        if (state.get() == DebugState.PAUSED.ordinal) {
            state.set(DebugState.STEP_OUT.ordinal)
            pauseReason = null

            if (continueCondition.isActive) {
                continueCondition.complete(Unit)
            }
        }
    }

    /**
     * 停止调试
     */
    fun stop() {
        state.set(DebugState.STOPPED.ordinal)

        if (continueCondition.isActive) {
            continueCondition.complete(Unit)
        }
    }

    /**
     * 在执行前检查断点
     * @param scriptId 脚本ID
     * @param lineNumber 行号
     * @param variables 当前变量
     */
    suspend fun checkBreakpoint(scriptId: String, lineNumber: Int, variables: Map<String, Any?> = emptyMap()) {
        currentLineNumber = lineNumber

        val currentState = getState()
        if (currentState == DebugState.STOPPED) {
            return
        }

        if (currentState == DebugState.STEP_OVER || currentState == DebugState.STEP_INTO) {
            pause("单步执行")
            return
        }

        val scriptBreakpoints = breakpoints[scriptId] ?: return

        for (breakpoint in scriptBreakpoints) {
            if (breakpoint.enabled && breakpoint.lineNumber == lineNumber) {
                if (breakpoint.condition != null) {
                    if (!evaluateCondition(breakpoint.condition, variables)) {
                        continue
                    }
                }

                val frame = DebugFrame(
                    scriptId = scriptId,
                    scriptName = scriptId,
                    lineNumber = lineNumber,
                    functionName = null,
                    variables = variables
                )

                sendEvent(DebugEvent.BreakpointHit(breakpoint, frame))
                pause("断点: 第${lineNumber}行")
                break
            }
        }
    }

    /**
     * 计算条件表达式
     */
    private fun evaluateCondition(condition: String, variables: Map<String, Any?>): Boolean {
        return try {
            val expression = condition.trim()
            when {
                expression == "true" -> true
                expression == "false" -> false
                expression.startsWith("variables[") -> {
                    val varName = expression.substringAfter("['").substringBefore("']")
                    val expectedValue = expression.substringAfter("==").trim()
                    variables[varName]?.toString() == expectedValue.trim('"')
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 进入函数调用
     * @param scriptId 脚本ID
     * @param functionName 函数名
     * @param lineNumber 行号
     * @param variables 变量
     */
    suspend fun enterFunction(scriptId: String, functionName: String, lineNumber: Int, variables: Map<String, Any?>) {
        if (callStack.size >= config.maxCallStackDepth) {
            throw StackOverflowError("调用栈深度超过限制")
        }

        val frame = DebugFrame(
            scriptId = scriptId,
            scriptName = scriptId,
            lineNumber = lineNumber,
            functionName = functionName,
            variables = variables
        )

        callStack.add(frame)

        if (getState() == DebugState.STEP_INTO) {
            pause("进入函数: $functionName")
        }
    }

    /**
     * 退出函数调用
     */
    suspend fun exitFunction() {
        if (callStack.isNotEmpty()) {
            callStack.removeAt(callStack.size - 1)
        }

        if (getState() == DebugState.STEP_OUT && callStack.isNotEmpty()) {
            pause("退出函数")
        }
    }

    /**
     * 获取当前调用帧
     */
    fun getCurrentFrame(): DebugFrame? {
        return callStack.lastOrNull()
    }

    /**
     * 获取调用栈
     */
    fun getCallStack(): List<DebugFrame> {
        return callStack.toList()
    }

    /**
     * 添加监视变量
     * @param name 变量名
     * @param value 变量值
     */
    fun watchVariable(name: String, value: Any?) {
        val oldValue = watchedVariables[name]
        watchedVariables[name] = value

        if (oldValue != value) {
            sendEvent(DebugEvent.VariableChanged(name, oldValue, value))
        }
    }

    /**
     * 移除监视变量
     * @param name 变量名
     */
    fun unwatchVariable(name: String) {
        watchedVariables.remove(name)
    }

    /**
     * 获取所有监视变量
     */
    fun getWatchedVariables(): Map<String, Any?> {
        return watchedVariables.toMap()
    }

    /**
     * 抛出调试异常
     * @param exception 异常
     */
    suspend fun throwException(exception: Throwable) {
        if (config.breakOnException) {
            val frame = getCurrentFrame()
            if (frame != null) {
                sendEvent(DebugEvent.ExceptionThrown(exception, frame))
                pause("异常: ${exception.message}")
            }
        }
    }

    /**
     * 获取调试信息
     */
    fun getDebugInfo(): Map<String, Any?> {
        return mapOf(
            "state" to getState().name,
            "scriptId" to currentScriptId,
            "lineNumber" to currentLineNumber,
            "pauseReason" to pauseReason,
            "callStackDepth" to callStack.size,
            "breakpointCount" to breakpoints.values.sumOf { it.size },
            "watchedVariables" to watchedVariables.size
        )
    }

    /**
     * 检查是否应该暂停
     */
    fun shouldPause(): Boolean {
        return state.get() == DebugState.PAUSED.ordinal
    }

    /**
     * 检查是否已停止
     */
    fun isStopped(): Boolean {
        return state.get() == DebugState.STOPPED.ordinal
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        scope.cancel()
        breakpoints.clear()
        callStack.clear()
        watchedVariables.clear()
        eventListeners.clear()
        state.set(DebugState.IDLE.ordinal)
    }
}
