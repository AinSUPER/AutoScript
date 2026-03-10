package com.autoscript.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val content: String = "",
    val scriptType: String = "custom",
    val packageName: String = "",
    val isFavorite: Boolean = false,
    val isEnabled: Boolean = true,
    val runCount: Int = 0,
    val lastRunTime: Long = 0,
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val config: Map<String, Any> = emptyMap()
)

@Entity(tableName = "script_executions")
data class ScriptExecutionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scriptId: Long,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0,
    val status: String = "running",
    val result: String = "",
    val errorMessage: String = "",
    val executionLog: String = ""
)
