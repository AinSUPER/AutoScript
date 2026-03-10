package com.autoscript.engine

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

class ScriptParser {

    private val gson = Gson()

    fun parse(content: String): List<ScriptAction> {
        val actions = mutableListOf<ScriptAction>()
        
        try {
            val jsonElements = gson.fromJson(content, Array<JsonObject>::class.java)
            
            for (element in jsonElements) {
                val action = parseAction(element)
                if (action != null) {
                    actions.add(action)
                }
            }
        } catch (e: Exception) {
            actions.addAll(parseSimpleFormat(content))
        }
        
        return actions
    }

    private fun parseAction(json: JsonObject): ScriptAction? {
        val type = json.get("type")?.asString ?: return null
        
        return when (type) {
            "click" -> ScriptAction.Click(
                x = json.get("x")?.asInt ?: 0,
                y = json.get("y")?.asInt ?: 0
            )
            "longClick" -> ScriptAction.LongClick(
                x = json.get("x")?.asInt ?: 0,
                y = json.get("y")?.asInt ?: 0,
                duration = json.get("duration")?.asLong ?: 500L
            )
            "swipe" -> ScriptAction.Swipe(
                startX = json.get("startX")?.asInt ?: 0,
                startY = json.get("startY")?.asInt ?: 0,
                endX = json.get("endX")?.asInt ?: 0,
                endY = json.get("endY")?.asInt ?: 0,
                duration = json.get("duration")?.asLong ?: 300L
            )
            "input" -> ScriptAction.Input(
                text = json.get("text")?.asString ?: ""
            )
            "wait" -> ScriptAction.Wait(
                millis = json.get("millis")?.asLong ?: 1000L
            )
            "findImage" -> ScriptAction.FindImage(
                imagePath = json.get("imagePath")?.asString ?: "",
                similarity = json.get("similarity")?.asFloat ?: 0.9f,
                throwIfNotFound = json.get("throwIfNotFound")?.asBoolean ?: false
            )
            "findText" -> ScriptAction.FindText(
                text = json.get("text")?.asString ?: "",
                throwIfNotFound = json.get("throwIfNotFound")?.asBoolean ?: false
            )
            "condition" -> {
                val conditionJson = json.getAsJsonObject("condition")
                val condition = parseCondition(conditionJson)
                val trueActions = json.getAsJsonArray("trueActions")?.mapNotNull { 
                    parseAction(it.asJsonObject) 
                } ?: emptyList()
                val falseActions = json.getAsJsonArray("falseActions")?.mapNotNull { 
                    parseAction(it.asJsonObject) 
                } ?: emptyList()
                ScriptAction.Condition(condition, trueActions, falseActions)
            }
            "loop" -> {
                val count = json.get("count")?.asInt ?: 1
                val loopActions = json.getAsJsonArray("actions")?.mapNotNull { 
                    parseAction(it.asJsonObject) 
                } ?: emptyList()
                ScriptAction.Loop(count, loopActions)
            }
            else -> null
        }
    }

    private fun parseCondition(json: JsonObject?): Condition {
        if (json == null) return Condition.AlwaysTrue
        
        val type = json.get("type")?.asString ?: return Condition.AlwaysTrue
        
        return when (type) {
            "imageExists" -> Condition.ImageExists(
                imagePath = json.get("imagePath")?.asString ?: "",
                similarity = json.get("similarity")?.asFloat ?: 0.9f
            )
            "textExists" -> Condition.TextExists(
                text = json.get("text")?.asString ?: ""
            )
            "packageInForeground" -> Condition.PackageInForeground(
                packageName = json.get("packageName")?.asString ?: ""
            )
            "and" -> {
                val conditions = json.getAsJsonArray("conditions")?.mapNotNull {
                    parseCondition(it.asJsonObject)
                } ?: emptyList()
                Condition.And(conditions)
            }
            "or" -> {
                val conditions = json.getAsJsonArray("conditions")?.mapNotNull {
                    parseCondition(it.asJsonObject)
                } ?: emptyList()
                Condition.Or(conditions)
            }
            "not" -> Condition.Not(parseCondition(json.getAsJsonObject("condition")))
            else -> Condition.AlwaysTrue
        }
    }

    private fun parseSimpleFormat(content: String): List<ScriptAction> {
        val actions = mutableListOf<ScriptAction>()
        val lines = content.lines()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//")) continue
            
            val parts = trimmed.split("\\s+".toRegex())
            when (parts[0]) {
                "click" -> {
                    if (parts.size >= 3) {
                        actions.add(ScriptAction.Click(parts[1].toInt(), parts[2].toInt()))
                    }
                }
                "swipe" -> {
                    if (parts.size >= 5) {
                        actions.add(ScriptAction.Swipe(
                            parts[1].toInt(), parts[2].toInt(),
                            parts[3].toInt(), parts[4].toInt()
                        ))
                    }
                }
                "wait" -> {
                    if (parts.size >= 2) {
                        actions.add(ScriptAction.Wait(parts[1].toLong()))
                    }
                }
                "input" -> {
                    if (parts.size >= 2) {
                        actions.add(ScriptAction.Input(parts.drop(1).joinToString(" ")))
                    }
                }
            }
        }
        
        return actions
    }
}

sealed class ScriptAction {
    data class Click(val x: Int, val y: Int) : ScriptAction()
    data class LongClick(val x: Int, val y: Int, val duration: Long = 500L) : ScriptAction()
    data class Swipe(
        val startX: Int, val startY: Int,
        val endX: Int, val endY: Int,
        val duration: Long = 300L
    ) : ScriptAction()
    data class Input(val text: String) : ScriptAction()
    data class Wait(val millis: Long) : ScriptAction()
    data class FindImage(
        val imagePath: String,
        val similarity: Float = 0.9f,
        val throwIfNotFound: Boolean = false
    ) : ScriptAction()
    data class FindText(
        val text: String,
        val throwIfNotFound: Boolean = false
    ) : ScriptAction()
    data class Condition(
        val condition: com.autoscript.engine.Condition,
        val trueActions: List<ScriptAction>,
        val falseActions: List<ScriptAction> = emptyList()
    ) : ScriptAction()
    data class Loop(
        val count: Int,
        val actions: List<ScriptAction>
    ) : ScriptAction()
}

sealed class Condition {
    object AlwaysTrue : Condition()
    data class ImageExists(val imagePath: String, val similarity: Float = 0.9f) : Condition()
    data class TextExists(val text: String) : Condition()
    data class PackageInForeground(val packageName: String) : Condition()
    data class And(val conditions: List<Condition>) : Condition()
    data class Or(val conditions: List<Condition>) : Condition()
    data class Not(val condition: Condition) : Condition()
}
