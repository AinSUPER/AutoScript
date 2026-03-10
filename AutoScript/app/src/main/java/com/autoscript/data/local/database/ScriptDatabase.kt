package com.autoscript.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.autoscript.data.local.dao.ScriptDao
import com.autoscript.data.local.dao.ScriptExecutionDao
import com.autoscript.data.local.entity.ScriptEntity
import com.autoscript.data.local.entity.ScriptExecutionEntity

@Database(
    entities = [
        ScriptEntity::class,
        ScriptExecutionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ScriptDatabase : RoomDatabase() {

    abstract fun scriptDao(): ScriptDao
    abstract fun scriptExecutionDao(): ScriptExecutionDao

    companion object {
        private const val DATABASE_NAME = "autoscript.db"

        @Volatile
        private var INSTANCE: ScriptDatabase? = null

        fun getInstance(context: Context): ScriptDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): ScriptDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ScriptDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
