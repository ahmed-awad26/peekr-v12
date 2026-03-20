package com.peekr.widget

import com.peekr.data.local.dao.PostDao
import com.peekr.data.local.entities.PostEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepository @Inject constructor(
    private val postDao: PostDao
) {
    suspend fun getLatestPosts(limit: Int = 5): List<PostEntity> {
        return postDao.getLatestPostsSync(limit)
    }

    suspend fun getLatestPostsByPlatform(platformId: String, limit: Int = 5): List<PostEntity> {
        return postDao.getLatestPostsByPlatformSync(platformId, limit)
    }

    suspend fun getLatestPostsBySource(sourceId: String, limit: Int = 5): List<PostEntity> {
        return postDao.getLatestPostsBySourceSync(sourceId, limit)
    }
}
