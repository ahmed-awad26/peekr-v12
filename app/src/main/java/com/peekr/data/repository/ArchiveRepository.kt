package com.peekr.data.repository

import com.peekr.data.local.dao.ArchiveDao
import com.peekr.data.local.dao.CategoryDao
import com.peekr.data.local.entities.ArchiveEntity
import com.peekr.data.local.entities.CategoryEntity
import com.peekr.data.local.entities.PostEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArchiveRepository @Inject constructor(
    private val archiveDao: ArchiveDao,
    private val categoryDao: CategoryDao
) {
    // ==============================
    // الأرشيف
    // ==============================
    fun getAllArchives(): Flow<List<ArchiveEntity>> = archiveDao.getAllArchives()

    fun getArchivesByCategory(categoryId: Long): Flow<List<ArchiveEntity>> =
        archiveDao.getArchivesByCategory(categoryId)

    fun getArchiveCount(): Flow<Int> = archiveDao.getArchiveCount()

    suspend fun savePost(post: PostEntity, categoryId: Long? = null, note: String? = null): Long {
        return archiveDao.insertArchive(
            ArchiveEntity(
                postId = post.id,
                platformId = post.platformId,
                sourceName = post.sourceName,
                content = post.content,
                mediaUrl = post.mediaUrl,
                postUrl = post.postUrl,
                categoryId = categoryId,
                note = note
            )
        )
    }

    suspend fun updateArchive(archive: ArchiveEntity) = archiveDao.updateArchive(archive)

    suspend fun deleteArchive(archive: ArchiveEntity) = archiveDao.deleteArchive(archive)

    // ==============================
    // التصنيفات
    // ==============================
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun addCategory(name: String, color: String = "#6200EE"): Long {
        return categoryDao.insertCategory(CategoryEntity(name = name, color = color))
    }

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)
}
