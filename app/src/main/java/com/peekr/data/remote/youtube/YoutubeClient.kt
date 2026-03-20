package com.peekr.data.remote.youtube

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
class YoutubeClient @Inject constructor(
    private val apiKeyDao: ApiKeyDao,
    private val postDao: PostDao,
    private val accountDao: AccountDao,
    private val logger: AppLogger
) {
    private val BASE_URL = "https://www.googleapis.com/youtube/v3/"

    private val api: YoutubeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeApi::class.java)
    }

    // ==============================
    // Main sync — uses playlistItems (1 quota unit per call, not 100!)
    // ==============================
    suspend fun syncChannels(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val apiKey = apiKeyDao.getApiKeyByPlatform("youtube")?.keyValue
            if (apiKey.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("YouTube API Key غير موجود — أضفه من الإعدادات"))
            }

            val channelAccounts = accountDao.getAllAccountsByPlatformSync("youtube")
            if (channelAccounts.isEmpty()) {
                return@withContext Result.success(0)
            }

            var totalNew = 0
            val errors = mutableListOf<String>()

            channelAccounts.forEach { account ->
                try {
                    val result = syncSingleChannel(account.accountName.trim(), apiKey)
                    result.onSuccess { count -> totalNew += count }
                    result.onFailure { e -> errors.add("${account.accountName}: ${e.message}") }
                } catch (e: Exception) {
                    errors.add("${account.accountName}: ${e.message}")
                    logger.error("فشل sync قناة: ${account.accountName}", "youtube", e)
                }
            }

            if (errors.isNotEmpty()) {
                logger.warning("يوتيوب: أخطاء في بعض القنوات: ${errors.joinToString()}", "youtube")
            }

            logger.info("يوتيوب: تم جلب $totalNew منشور جديد", "youtube")
            if (totalNew == 0 && errors.size == channelAccounts.size) {
                Result.failure(Exception(errors.first()))
            } else {
                Result.success(totalNew)
            }
        } catch (e: Exception) {
            logger.error("خطأ في مزامنة يوتيوب", "youtube", e)
            Result.failure(e)
        }
    }

    // ==============================
    // Sync one channel
    // ==============================
    private suspend fun syncSingleChannel(channelUrl: String, apiKey: String): Result<Int> {
        val channelInfo = resolveChannelInfo(channelUrl, apiKey)
            ?: return Result.failure(Exception("لم يتم التعرف على القناة: $channelUrl"))

        val uploadsPlaylistId = channelInfo.second
            ?: return Result.failure(Exception("لم يتم الحصول على playlist القناة"))

        val newPosts = fetchPlaylistVideos(
            playlistId = uploadsPlaylistId,
            channelTitle = channelInfo.first,
            channelId = channelUrl,
            apiKey = apiKey
        )
        return Result.success(newPosts)
    }

    // ==============================
    // Resolve channel → (title, uploadsPlaylistId)
    // Handles all URL formats
    // ==============================
    private suspend fun resolveChannelInfo(input: String, apiKey: String): Pair<String, String?>? {
        return try {
            when {
                // Direct channel ID: UCxxxxxxxxxxxxxxxx
                input.matches(Regex("UC[A-Za-z0-9_-]{22}")) -> {
                    val r = api.getChannel(id = input, apiKey = apiKey)
                    r.items.firstOrNull()?.let {
                        Pair(it.snippet.title, it.contentDetails?.relatedPlaylists?.uploads)
                    }
                }
                // youtube.com/channel/UCxxxxxxx
                input.contains("/channel/") -> {
                    val id = input.substringAfter("/channel/").substringBefore("/").substringBefore("?")
                    val r = api.getChannel(id = id, apiKey = apiKey)
                    r.items.firstOrNull()?.let {
                        Pair(it.snippet.title, it.contentDetails?.relatedPlaylists?.uploads)
                    }
                }
                // youtube.com/@handle or contains @
                input.contains("@") -> {
                    val handle = input.substringAfterLast("@").substringBefore("/").substringBefore("?").trim()
                    val r = api.getChannel(forHandle = "@$handle", apiKey = apiKey)
                    r.items.firstOrNull()?.let {
                        Pair(it.snippet.title, it.contentDetails?.relatedPlaylists?.uploads)
                    }
                }
                // youtube.com/c/channelname or youtube.com/user/username
                input.contains("/c/") || input.contains("/user/") -> {
                    val name = input
                        .substringAfterLast("/c/")
                        .substringAfterLast("/user/")
                        .substringBefore("/").substringBefore("?")
                    // Try forUsername first
                    val r = api.getChannel(forUsername = name, apiKey = apiKey)
                    if (r.items.isNotEmpty()) {
                        r.items.first().let {
                            Pair(it.snippet.title, it.contentDetails?.relatedPlaylists?.uploads)
                        }
                    } else {
                        // Fallback: try forHandle
                        val r2 = api.getChannel(forHandle = "@$name", apiKey = apiKey)
                        r2.items.firstOrNull()?.let {
                            Pair(it.snippet.title, it.contentDetails?.relatedPlaylists?.uploads)
                        }
                    }
                }
                // Plain text — try as @handle
                else -> {
                    val clean = input.trim().trimStart('@')
                    val r = api.getChannel(forHandle = "@$clean", apiKey = apiKey)
                    if (r.items.isNotEmpty()) {
                        r.items.first().let {
                            Pair(it.snippet.title, it.contentDetails?.relatedPlaylists?.uploads)
                        }
                    } else {
                        val r2 = api.getChannel(forUsername = clean, apiKey = apiKey)
                        r2.items.firstOrNull()?.let {
                            Pair(it.snippet.title, it.contentDetails?.relatedPlaylists?.uploads)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("فشل resolveChannelInfo: $input → ${e.message}", "youtube", e)
            null
        }
    }

    // ==============================
    // Fetch videos from uploads playlist — 1 quota unit per call
    // ==============================
    private suspend fun fetchPlaylistVideos(
        playlistId: String,
        channelTitle: String,
        channelId: String,
        apiKey: String
    ): Int {
        var newCount = 0
        try {
            val response = api.getPlaylistVideos(
                playlistId = playlistId,
                maxResults = 30,
                apiKey = apiKey
            )

            response.items.forEach { item ->
                val videoId = item.snippet.resourceId.videoId
                if (videoId.isBlank()) return@forEach

                val videoUrl = "https://www.youtube.com/watch?v=$videoId"

                // التحقق من عدم التكرار
                if (postDao.existsByUrl(videoUrl)) return@forEach

                val thumbnail = item.snippet.thumbnails.high?.url
                    ?: item.snippet.thumbnails.medium?.url
                    ?: item.snippet.thumbnails.default?.url

                postDao.insertPost(
                    PostEntity(
                        platformId  = "youtube",
                        sourceId    = channelId,
                        sourceName  = channelTitle.ifBlank { item.snippet.channelTitle },
                        content     = item.snippet.title + "\n\n" + item.snippet.description.take(200),
                        mediaUrl    = thumbnail,
                        postUrl     = videoUrl,
                        timestamp   = parseDate(item.snippet.publishedAt)
                    )
                )
                newCount++
            }
        } catch (e: Exception) {
            logger.error("فشل fetchPlaylistVideos: $playlistId", "youtube", e)
        }
        return newCount
    }

    private fun parseDate(dateStr: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(dateStr)?.time
                ?: System.currentTimeMillis()
        } catch (_: Exception) { System.currentTimeMillis() }
    }
}
