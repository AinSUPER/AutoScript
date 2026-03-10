package com.autoscript.data.local.dao

import androidx.room.*
import com.autoscript.data.local.entity.ScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Query("SELECT * FROM scripts ORDER BY updateTime DESC")
    fun getAllScripts(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getScriptById(id: Long): ScriptEntity?

    @Query("SELECT * FROM scripts WHERE name LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%'")
    suspend fun searchScripts(keyword: String): List<ScriptEntity>

    @Query("SELECT * FROM scripts WHERE packageName = :packageName")
    suspend fun getScriptsByPackage(packageName: String): List<ScriptEntity>

    @Query("SELECT * FROM scripts WHERE isFavorite = 1 ORDER BY updateTime DESC")
    fun getFavoriteScripts(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE isEnabled = 1 ORDER BY updateTime DESC")
    fun getEnabledScripts(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE tags LIKE '%' || :tag || '%'")
    suspend fun getScriptsByTag(tag: String): List<ScriptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptEntity): Long

    @Update
    suspend fun updateScript(script: ScriptEntity)

    @Delete
    suspend fun deleteScript(script: ScriptEntity)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteScriptById(id: Long)

    @Query("DELETE FROM scripts")
    suspend fun deleteAllScripts()

    @Query("UPDATE scripts SET isFavorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE scripts SET runCount = runCount + 1, lastRunTime = :time WHERE id = :id")
    suspend fun incrementRunCount(id: Long, time: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM scripts")
    suspend fun getScriptCount(): Int
}
