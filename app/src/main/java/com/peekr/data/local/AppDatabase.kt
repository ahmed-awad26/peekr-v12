package com.peekr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.peekr.data.local.dao.*
import com.peekr.data.local.entities.*

@Database(
    entities = [
        PostEntity::class,
        ArchiveEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        ApiKeyEntity::class,
        ToolEntity::class,
        LogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun archiveDao(): ArchiveDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun toolDao(): ToolDao
    abstract fun logDao(): LogDao
}
