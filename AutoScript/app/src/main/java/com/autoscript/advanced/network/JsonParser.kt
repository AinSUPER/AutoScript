package com.autoscript.advanced.network

import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON解析器
 * 提供JSON解析、生成、转换等功能
 */
class JsonParser {

    /**
     * 解析结果
     */
    sealed class JsonValue {
        data class JsonNull(val value: Nothing? = null) : JsonValue()
        data class JsonBoolean(val value: Boolean) : JsonValue()
        data class JsonNumber(val value: Number) : JsonValue()
        data class JsonString(val value: String) : JsonValue()
        data class JsonArray(val value: List<JsonValue>) : JsonValue()
        data class JsonObject(val value: Map<String, JsonValue>) : JsonValue()

        fun asString(): String? = if (this is JsonString) value else null
        fun asInt(): Int? = if (this is JsonNumber) value.toInt() else null
        fun asLong(): Long? = if (this is JsonNumber) value.toLong() else null
        fun asDouble(): Double? = if (this is JsonNumber) value.toDouble() else null
        fun asBoolean(): Boolean? = if (this is JsonBoolean) value else null
        fun asArray(): List<JsonValue>? = if (this is JsonArray) value else null
        fun asMap(): Map<String, JsonValue>? = if (this is JsonObject) value else null
    }

    /**
     * 解析JSON字符串
     * @param jsonString JSON字符串
     * @return 解析结果
     */
    fun parse(jsonString: String): JsonValue? {
        return try {
            val trimmed = jsonString.trim()
            when {
                trimmed == "null" -> JsonValue.JsonNull()
                trimmed == "true" -> JsonValue.JsonBoolean(true)
                trimmed == "false" -> JsonValue.JsonBoolean(false)
                trimmed.startsWith("{") -> parseObject(JSONObject(trimmed))
                trimmed.startsWith("[") -> parseArray(JSONArray(trimmed))
                trimmed.startsWith("\"") -> JsonValue.JsonString(
                    JSONObject("{\"v\":$trimmed}").getString("v")
                )
                trimmed.contains(".") -> JsonValue.JsonNumber(trimmed.toDouble())
                else -> JsonValue.JsonNumber(trimmed.toLong())
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析JSON对象
     */
    private fun parseObject(jsonObject: JSONObject): JsonValue.JsonObject {
        val map = mutableMapOf<String, JsonValue>()
        val keys = jsonObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = parseValue(jsonObject.get(key))
        }

        return JsonValue.JsonObject(map)
    }

    /**
     * 解析JSON数组
     */
    private fun parseArray(jsonArray: JSONArray): JsonValue.JsonArray {
        val list = mutableListOf<JsonValue>()

        for (i in 0 until jsonArray.length()) {
            list.add(parseValue(jsonArray.get(i)))
        }

        return JsonValue.JsonArray(list)
    }

    /**
     * 解析JSON值
     */
    private fun parseValue(value: Any): JsonValue {
        return when (value) {
            null -> JsonValue.JsonNull()
            is Boolean -> JsonValue.JsonBoolean(value)
            is Int -> JsonValue.JsonNumber(value)
            is Long -> JsonValue.JsonNumber(value)
            is Double -> JsonValue.JsonNumber(value)
            is Float -> JsonValue.JsonNumber(value)
            is String -> JsonValue.JsonString(value)
            is JSONObject -> parseObject(value)
            is JSONArray -> parseArray(value)
            else -> JsonValue.JsonString(value.toString())
        }
    }

    /**
     * 从JSON字符串获取对象
     * @param jsonString JSON字符串
     * @return Map对象
     */
    fun parseToMap(jsonString: String): Map<String, Any?>? {
        return try {
            val jsonObject = JSONObject(jsonString)
            jsonObjectToMap(jsonObject)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从JSON字符串获取列表
     * @param jsonString JSON字符串
     * @return 列表
     */
    fun parseToList(jsonString: String): List<Any?>? {
        return try {
            val jsonArray = JSONArray(jsonString)
            jsonArrayToList(jsonArray)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * JSONObject转Map
     */
    private fun jsonObjectToMap(jsonObject: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = jsonObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = when (val value = jsonObject.get(key)) {
                null -> null
                is JSONObject -> jsonObjectToMap(value)
                is JSONArray -> jsonArrayToList(value)
                JSONObject.NULL -> null
                else -> value
            }
        }

        return map
    }

    /**
     * JSONArray转List
     */
    private fun jsonArrayToList(jsonArray: JSONArray): List<Any?> {
        val list = mutableListOf<Any?>()

        for (i in 0 until jsonArray.length()) {
            list.add(
                when (val value = jsonArray.get(i)) {
                    null -> null
                    is JSONObject -> jsonObjectToMap(value)
                    is JSONArray -> jsonArrayToList(value)
                    JSONObject.NULL -> null
                    else -> value
                }
            )
        }

        return list
    }

    /**
     * 从JSON字符串获取指定字段的值
     * @param jsonString JSON字符串
     * @param path 字段路径 (如 "user.name" 或 "items[0].id")
     * @return 字段值
     */
    fun getValue(jsonString: String, path: String): Any? {
        return try {
            var current: Any? = JSONObject(jsonString)
            val parts = parsePath(path)

            for (part in parts) {
                current = when {
                    part.isArrayAccess -> {
                        val index = part.index!!
                        when (current) {
                            is JSONArray -> if (index < current.length()) current.get(index) else null
                            is List<*> -> current.getOrNull(index)
                            else -> null
                        }
                    }
                    else -> {
                        when (current) {
                            is JSONObject -> if (current.has(part.name)) current.get(part.name) else null
                            is Map<*, *> -> current[part.name]
                            else -> null
                        }
                    }
                }

                if (current == null) break
            }

            current
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 路径部分
     */
    private data class PathPart(
        val name: String,
        val isArrayAccess: Boolean = false,
        val index: Int? = null
    )

    /**
     * 解析路径
     */
    private fun parsePath(path: String): List<PathPart> {
        val parts = mutableListOf<PathPart>()
        val regex = Regex("""(\w+)(?:\[(\d+)\])?""")
        val segments = path.split(".")

        for (segment in segments) {
            val matches = regex.findAll(segment)
            for (match in matches) {
                val name = match.groupValues[1]
                val indexStr = match.groupValues[2]

                if (indexStr.isNotEmpty()) {
                    parts.add(PathPart(name, true, indexStr.toInt()))
                } else {
                    parts.add(PathPart(name))
                }
            }
        }

        return parts
    }

    /**
     * 将Map转换为JSON字符串
     * @param map Map对象
     * @return JSON字符串
     */
    fun toJson(map: Map<String, Any?>): String {
        return mapToJson(map).toString()
    }

    /**
     * Map转JSONObject
     */
    private fun mapToJson(map: Map<String, Any?>): JSONObject {
        val jsonObject = JSONObject()

        for ((key, value) in map) {
            jsonObject.put(key, when (value) {
                null -> JSONObject.NULL
                is Map<*, *> -> mapToJson(value as Map<String, Any?>)
                is List<*> -> listToJson(value)
                else -> value
            })
        }

        return jsonObject
    }

    /**
     * List转JSONArray
     */
    private fun listToJson(list: List<*>): JSONArray {
        val jsonArray = JSONArray()

        for (item in list) {
            jsonArray.put(when (item) {
                null -> JSONObject.NULL
                is Map<*, *> -> mapToJson(item as Map<String, Any?>)
                is List<*> -> listToJson(item)
                else -> item
            })
        }

        return jsonArray
    }

    /**
     * 将List转换为JSON字符串
     * @param list 列表
     * @return JSON字符串
     */
    fun toJson(list: List<Any?>): String {
        return listToJson(list).toString()
    }

    /**
     * 格式化JSON字符串
     * @param jsonString JSON字符串
     * @param indent 缩进空格数
     * @return 格式化后的字符串
     */
    fun format(jsonString: String, indent: Int = 2): String? {
        return try {
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("{")) {
                JSONObject(trimmed).toString(indent)
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed).toString(indent)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 压缩JSON字符串
     * @param jsonString JSON字符串
     * @return 压缩后的字符串
     */
    fun compress(jsonString: String): String? {
        return try {
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("{")) {
                JSONObject(trimmed).toString()
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed).toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查是否为有效JSON
     * @param jsonString JSON字符串
     * @return 是否有效
     */
    fun isValid(jsonString: String): Boolean {
        return try {
            val trimmed = jsonString.trim()
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed)
                trimmed.startsWith("[") -> JSONArray(trimmed)
                else -> return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 合并两个JSON对象
     * @param json1 第一个JSON
     * @param json2 第二个JSON
     * @return 合并后的JSON
     */
    fun merge(json1: String, json2: String): String? {
        return try {
            val obj1 = JSONObject(json1)
            val obj2 = JSONObject(json2)

            val keys = obj2.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                obj1.put(key, obj2.get(key))
            }

            obj1.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 从JSON对象中移除字段
     * @param jsonString JSON字符串
     * @param keys 要移除的字段名
     * @return 移除后的JSON字符串
     */
    fun removeFields(jsonString: String, vararg keys: String): String? {
        return try {
            val jsonObject = JSONObject(jsonString)

            for (key in keys) {
                jsonObject.remove(key)
            }

            jsonObject.toString()
        } catch (e: Exception) {
            null
        }
    }
}
