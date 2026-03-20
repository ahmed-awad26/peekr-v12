package com.peekr.data.remote.facebook

import com.peekr.core.logger.AppLogger
import com.peekr.data.local.dao.AccountDao
import com.peekr.data.local.dao.ApiKeyDao
import com.peekr.data.local.dao.PostDao
import com.peekr.data.local.entities.PostEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacebookClient @Inject constructor(
    private val apiKeyDao: ApiKeyDao,
    private val accountDao: AccountDao,
    private val postDao: PostDao,
    private val logger: AppLogger
) {
    private val BASE_URL = "https://graph.facebook.com/v18.0/"

    private val api: FacebookApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FacebookApi::class.java)
    }

    suspend fun syncPages(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val accessToken = apiKeyDao.getApiKeyByPlatform("facebook")?.keyValue
            if (accessToken.isNullOrEmpty()) {
                logger.warning("Facebook Access Token غير موجود", "facebook")
                return@withContext Result.failure(Exception("أضف Facebook Access Token في الإعدادات"))
            }

            val account = accountDao.getAccountByPlatform("facebook")
            if (account == null || !account.isConnected) {
                return@withContext Result.success(0)
            }

            val pageIds = account.extraData?.split(",") ?: emptyList()
            var totalNew = 0

            pageIds.forEach { pageId ->
                try {
                    val count = fetchPagePosts(pageId.trim(), accessToken)
                    totalNew += count
                } catch (e: Exception) {
                    logger.error("فشل جلب بوستات الصفحة: $pageId", "facebook", e)
                }
            }

            logger.info("فيسبوك: تم جلب $totalNew منشور جديد", "facebook")
            Result.success(totalNew)
        } catch (e: Exception) {
            logger.error("خطأ في مزامنة فيسبوك", "facebook", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchPagePosts(pageId: String, accessToken: String): Int {
        // جلب معلومات الصفحة الأول
        val pageInfo = try {
            api.getPageInfo(pageId = pageId, accessToken = accessToken)
        } catch (e: Exception) {
            logger.warning("مش قادر يجيب معلومات الصفحة: $pageId", "facebook")
            null
        }

        val pageName = pageInfo?.name ?: pageId

        // جلب البوستات
        val response = api.getPagePosts(pageId = pageId, accessToken = accessToken)
        var count = 0

        response.data.forEach { post ->
            val content = post.message ?: post.story ?: return@forEach
            val timestamp = parseDate(post.created_time)

            postDao.insertPost(
                PostEntity(
                    platformId = "facebook",
                    sourceId = pageId,
                    sourceName = pageName,
                    content = content,
                    mediaUrl = post.full_picture,
                    postUrl = post.permalink_url,
                    timestamp = timestamp
                )
            )
            count++
        }
        return count
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            // فيسبوك بيرجع: 2024-01-15T10:30:00+0000
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
