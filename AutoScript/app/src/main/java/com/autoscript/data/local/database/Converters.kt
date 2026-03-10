package com.autoscript.data.local.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return gson.toJson(value ?: emptyList<Int>())
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String {
        return gson.toJson(value ?: emptyList<Long>())
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long> {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromMap(value: Map<String, Any>?): String {
        return gson.toJson(value ?: emptyMap<String, Any>())
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, Any> {
        if (value.isNullOrEmpty()) return emptyMap()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(value, type)
    }
}
