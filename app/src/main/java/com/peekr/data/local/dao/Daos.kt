package com.peekr.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.peekr.data.local.entities.AccountEntity
import com.peekr.data.local.entities.ApiKeyEntity
import com.peekr.data.local.entities.ArchiveEntity
import com.peekr.data.local.entities.CategoryEntity
import com.peekr.data.local.entities.LogEntity
import com.peekr.data.local.entities.PostEntity
import com.peekr.data.local.entities.ToolEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT COUNT(*) > 0 FROM posts WHERE postUrl = :url")
    suspend fun existsByUrl(url: String): Boolean

    @Query("SELECT COUNT(*) FROM posts WHERE platformId = :platformId")
    suspend fun countByPlatform(platformId: String): Int

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE platformId = :platformId ORDER BY timestamp DESC")
    fun getPostsByPlatform(platformId: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("UPDATE posts SET isRead = 1 WHERE id = :postId")
    suspend fun markAsRead(postId: Long)

    @Query("DELETE FROM posts WHERE timestamp < :before")
    suspend fun deleteOldPosts(before: Long)

    @Query("SELECT COUNT(*) FROM posts WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT * FROM posts ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestPostsSync(limit: Int): List<PostEntity>

    @Query("SELECT * FROM posts WHERE platformId = :platformId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestPostsByPlatformSync(platformId: String, limit: Int): List<PostEntity>

    @Query("SELECT * FROM posts WHERE sourceId = :sourceId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestPostsBySourceSync(sourceId: String, limit: Int): List<PostEntity>
}

@Dao
interface ArchiveDao {
    @Query("SELECT * FROM archives ORDER BY savedAt DESC")
    fun getAllArchives(): Flow<List<ArchiveEntity>>

    @Query("SELECT * FROM archives WHERE categoryId = :categoryId ORDER BY savedAt DESC")
    fun getArchivesByCategory(categoryId: Long): Flow<List<ArchiveEntity>>

    @Query("SELECT COUNT(*) FROM archives")
    fun getArchiveCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchive(archive: ArchiveEntity): Long

    @Update
    suspend fun updateArchive(archive: ArchiveEntity)

    @Delete
    suspend fun deleteArchive(archive: ArchiveEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY createdAt DESC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE platformId = :platformId LIMIT 1")
    suspend fun getAccountByPlatform(platformId: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE platformId = :platformId ORDER BY connectedAt DESC")
    fun getAllAccountsByPlatform(platformId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE platformId = :platformId ORDER BY connectedAt DESC")
    suspend fun getAllAccountsByPlatformSync(platformId: String): List<AccountEntity>

    @Query("SELECT * FROM accounts ORDER BY connectedAt DESC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("DELETE FROM accounts WHERE platformId = :platformId")
    suspend fun deleteAccountByPlatform(platformId: String)
}

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys WHERE platformId = :platformId LIMIT 1")
    suspend fun getApiKeyByPlatform(platformId: String): ApiKeyEntity?

    @Query("SELECT * FROM api_keys ORDER BY updatedAt DESC")
    fun getAllApiKeys(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys ORDER BY updatedAt DESC")
    suspend fun getAllApiKeysSync(): List<ApiKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiKey(apiKey: ApiKeyEntity)

    @Query("DELETE FROM api_keys WHERE platformId = :platformId")
    suspend fun deleteApiKey(platformId: String)
}

@Dao
interface ToolDao {
    @Query("SELECT * FROM tools ORDER BY addedAt DESC")
    fun getAllTools(): Flow<List<ToolEntity>>

    @Query("SELECT * FROM tools WHERE id = :toolId LIMIT 1")
    suspend fun getToolById(toolId: Long): ToolEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: ToolEntity): Long

    @Delete
    suspend fun deleteTool(tool: ToolEntity)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity): Long

    @Query("DELETE FROM logs WHERE timestamp < :before")
    suspend fun deleteOldLogs(before: Long)

    @Query("DELETE FROM logs")
    suspend fun clearLogs()
}
