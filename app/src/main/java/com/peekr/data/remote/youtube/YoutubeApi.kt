package com.peekr.data.remote.youtube

import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApi {

    // جلب معلومات القناة — كوتا: 1 وحدة
    @GET("channels")
    suspend fun getChannel(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("forHandle") forHandle: String? = null,
        @Query("forUsername") forUsername: String? = null,
        @Query("id") id: String? = null,
        @Query("key") apiKey: String
    ): ChannelResponse

    // جلب فيديوهات من Playlist (uploads) — كوتا: 1 وحدة فقط بدل 100!
    @GET("playlistItems")
    suspend fun getPlaylistVideos(
        @Query("part") part: String = "snippet",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 30,
        @Query("pageToken") pageToken: String? = null,
        @Query("key") apiKey: String
    ): PlaylistResponse

    // جلب تفاصيل فيديو
    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,statistics",
        @Query("id") videoId: String,
        @Query("key") apiKey: String
    ): VideoDetailsResponse
}

// ==============================
// Channels
// ==============================
data class ChannelResponse(val items: List<ChannelItem> = emptyList())
data class ChannelItem(
    val id: String,
    val snippet: ChannelSnippet,
    val contentDetails: ChannelContentDetails? = null
)
data class ChannelSnippet(val title: String = "", val description: String = "", val thumbnails: Thumbnails = Thumbnails())
data class ChannelContentDetails(val relatedPlaylists: RelatedPlaylists)
data class RelatedPlaylists(val uploads: String)

// ==============================
// PlaylistItems (uploads playlist — 1 quota unit per call)
// ==============================
data class PlaylistResponse(
    val items: List<PlaylistItem> = emptyList(),
    val nextPageToken: String? = null
)
data class PlaylistItem(
    val snippet: PlaylistSnippet
)
data class PlaylistSnippet(
    val title: String = "",
    val description: String = "",
    val publishedAt: String = "",
    val channelTitle: String = "",
    val resourceId: ResourceId = ResourceId(),
    val thumbnails: Thumbnails = Thumbnails()
)
data class ResourceId(
    val kind: String = "",
    val videoId: String = ""
)

// ==============================
// Search (kept for compatibility but avoid using — 100 quota units)
// ==============================
data class SearchResponse(val items: List<SearchItem> = emptyList(), val nextPageToken: String? = null)
data class SearchItem(val id: SearchItemId, val snippet: SearchSnippet)
data class SearchItemId(val videoId: String? = null)
data class SearchSnippet(
    val title: String = "",
    val description: String = "",
    val publishedAt: String = "",
    val channelId: String = "",
    val channelTitle: String = "",
    val thumbnails: Thumbnails = Thumbnails()
)

// ==============================
// Shared
// ==============================
data class Thumbnails(
    val default: ThumbnailItem? = null,
    val medium: ThumbnailItem? = null,
    val high: ThumbnailItem? = null
)
data class ThumbnailItem(val url: String)

data class VideoDetailsResponse(val items: List<VideoDetailItem> = emptyList())
data class VideoDetailItem(val id: String, val snippet: SearchSnippet, val statistics: VideoStatistics? = null)
data class VideoStatistics(val viewCount: String? = null, val likeCount: String? = null)
