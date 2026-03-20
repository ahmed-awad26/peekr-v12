package com.peekr.data.repository

import com.peekr.data.local.dao.PostDao
import com.peekr.data.local.entities.PostEntity
import com.peekr.data.remote.facebook.FacebookClient
import com.peekr.data.remote.rss.RssClient
import com.peekr.data.remote.telegram.TelegramClient
import com.peekr.data.remote.whatsapp.WhatsappBridge
import com.peekr.data.remote.youtube.YoutubeClient
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedRepository @Inject constructor(
    private val postDao: PostDao,
    private val youtubeClient: YoutubeClient,
    private val rssClient: RssClient,
    private val telegramClient: TelegramClient,
    private val whatsappBridge: WhatsappBridge,
    private val facebookClient: FacebookClient
) {
    fun getAllPosts(): Flow<List<PostEntity>> = postDao.getAllPosts()

    fun getPostsByPlatform(platformId: String): Flow<List<PostEntity>> =
        postDao.getPostsByPlatform(platformId)

    fun getUnreadCount(): Flow<Int> = postDao.getUnreadCount()

    suspend fun syncAll(): Map<String, Result<Int>> {
        val results = mutableMapOf<String, Result<Int>>()
        results["youtube"] = youtubeClient.syncChannels()
        results["rss"] = rssClient.syncFeeds()
        results["facebook"] = facebookClient.syncPages()
        if (telegramClient.isAuthorized()) {
            results["telegram"] = telegramClient.syncChats()
        }
        return results
    }

    suspend fun syncPlatform(platformId: String): Result<Int> {
        return when (platformId) {
            "youtube" -> youtubeClient.syncChannels()
            "rss" -> rssClient.syncFeeds()
            "telegram" -> telegramClient.syncChats()
            "facebook" -> facebookClient.syncPages()
            else -> Result.success(0)
        }
    }

    suspend fun markAsRead(postId: Long) = postDao.markAsRead(postId)
}
