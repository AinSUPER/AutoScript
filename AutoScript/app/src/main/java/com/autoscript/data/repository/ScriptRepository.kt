package com.autoscript.data.repository

import com.autoscript.data.local.dao.ScriptDao
import com.autoscript.data.local.dao.ScriptExecutionDao
import com.autoscript.data.local.entity.ScriptEntity
import com.autoscript.data.local.entity.ScriptExecutionEntity
import com.autoscript.data.local.database.ScriptDatabase
import kotlinx.coroutines.flow.Flow

class ScriptRepository(private val database: ScriptDatabase) {

    private val scriptDao: ScriptDao = database.scriptDao()
    private val executionDao: ScriptExecutionDao = database.scriptExecutionDao()

    fun getAllScripts(): Flow<List<ScriptEntity>> = scriptDao.getAllScripts()

    suspend fun getScriptById(id: Long): ScriptEntity? = scriptDao.getScriptById(id)

    suspend fun searchScripts(keyword: String): List<ScriptEntity> = scriptDao.searchScripts(keyword)

    suspend fun getScriptsByPackage(packageName: String): List<ScriptEntity> = 
        scriptDao.getScriptsByPackage(packageName)

    fun getFavoriteScripts(): Flow<List<ScriptEntity>> = scriptDao.getFavoriteScripts()

    fun getEnabledScripts(): Flow<List<ScriptEntity>> = scriptDao.getEnabledScripts()

    suspend fun getScriptsByTag(tag: String): List<ScriptEntity> = scriptDao.getScriptsByTag(tag)

    suspend fun insertScript(script: ScriptEntity): Long = scriptDao.insertScript(script)

    suspend fun updateScript(script: ScriptEntity) = scriptDao.updateScript(script)

    suspend fun deleteScript(script: ScriptEntity) = scriptDao.deleteScript(script)

    suspend fun deleteScriptById(id: Long) = scriptDao.deleteScriptById(id)

    suspend fun deleteAllScripts() = scriptDao.deleteAllScripts()

    suspend fun updateFavorite(id: Long, favorite: Boolean) = scriptDao.updateFavorite(id, favorite)

    suspend fun incrementRunCount(id: Long) = scriptDao.incrementRunCount(id)

    suspend fun getScriptCount(): Int = scriptDao.getScriptCount()

    fun getExecutionsByScript(scriptId: Long): Flow<List<ScriptExecutionEntity>> = 
        executionDao.getExecutionsByScript(scriptId)

    fun getRecentExecutions(limit: Int = 50): Flow<List<ScriptExecutionEntity>> = 
        executionDao.getRecentExecutions(limit)

    suspend fun getExecutionById(id: Long): ScriptExecutionEntity? = executionDao.getExecutionById(id)

    fun getExecutionsByStatus(status: String): Flow<List<ScriptExecutionEntity>> = 
        executionDao.getExecutionsByStatus(status)

    suspend fun insertExecution(execution: ScriptExecutionEntity): Long = 
        executionDao.insertExecution(execution)

    suspend fun updateExecution(execution: ScriptExecutionEntity) = 
        executionDao.updateExecution(execution)

    suspend fun deleteExecution(execution: ScriptExecutionEntity) = 
        executionDao.deleteExecution(execution)

    suspend fun deleteExecutionById(id: Long) = executionDao.deleteExecutionById(id)

    suspend fun deleteExecutionsByScript(scriptId: Long) = 
        executionDao.deleteExecutionsByScript(scriptId)

    suspend fun deleteOldExecutions(timestamp: Long) = executionDao.deleteOldExecutions(timestamp)

    suspend fun getExecutionCount(scriptId: Long): Int = executionDao.getExecutionCount(scriptId)

    suspend fun getSuccessCount(scriptId: Long): Int = executionDao.getSuccessCount(scriptId)

    suspend fun getFailedCount(scriptId: Long): Int = executionDao.getFailedCount(scriptId)
}
