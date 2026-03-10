package com.autoscript.data.local.dao

import androidx.room.*
import com.autoscript.data.local.entity.ScriptExecutionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptExecutionDao {

    @Query("SELECT * FROM script_executions WHERE scriptId = :scriptId ORDER BY startTime DESC")
    fun getExecutionsByScript(scriptId: Long): Flow<List<ScriptExecutionEntity>>

    @Query("SELECT * FROM script_executions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentExecutions(limit: Int = 50): Flow<List<ScriptExecutionEntity>>

    @Query("SELECT * FROM script_executions WHERE id = :id")
    suspend fun getExecutionById(id: Long): ScriptExecutionEntity?

    @Query("SELECT * FROM script_executions WHERE status = :status ORDER BY startTime DESC")
    fun getExecutionsByStatus(status: String): Flow<List<ScriptExecutionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecution(execution: ScriptExecutionEntity): Long

    @Update
    suspend fun updateExecution(execution: ScriptExecutionEntity)

    @Delete
    suspend fun deleteExecution(execution: ScriptExecutionEntity)

    @Query("DELETE FROM script_executions WHERE id = :id")
    suspend fun deleteExecutionById(id: Long)

    @Query("DELETE FROM script_executions WHERE scriptId = :scriptId")
    suspend fun deleteExecutionsByScript(scriptId: Long)

    @Query("DELETE FROM script_executions WHERE startTime < :timestamp")
    suspend fun deleteOldExecutions(timestamp: Long)

    @Query("SELECT COUNT(*) FROM script_executions WHERE scriptId = :scriptId")
    suspend fun getExecutionCount(scriptId: Long): Int

    @Query("SELECT COUNT(*) FROM script_executions WHERE status = 'success' AND scriptId = :scriptId")
    suspend fun getSuccessCount(scriptId: Long): Int

    @Query("SELECT COUNT(*) FROM script_executions WHERE status = 'failed' AND scriptId = :scriptId")
    suspend fun getFailedCount(scriptId: Long): Int
}
