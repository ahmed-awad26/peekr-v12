package com.peekr.data.remote.facebook

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FacebookApi {

    // جلب بوستات صفحة عامة
    @GET("{pageId}/posts")
    suspend fun getPagePosts(
        @Path("pageId") pageId: String,
        @Query("fields") fields: String = "id,message,story,full_picture,permalink_url,created_time",
        @Query("limit") limit: Int = 20,
        @Query("access_token") accessToken: String
    ): FacebookPostsResponse

    // جلب معلومات صفحة
    @GET("{pageId}")
    suspend fun getPageInfo(
        @Path("pageId") pageId: String,
        @Query("fields") fields: String = "id,name,picture,fan_count",
        @Query("access_token") accessToken: String
    ): FacebookPageInfo
}

// ==============================
// Data Classes
// ==============================

data class FacebookPostsResponse(
    val data: List<FacebookPost> = emptyList(),
    val paging: FacebookPaging? = null
)

data class FacebookPost(
    val id: String,
    val message: String? = null,
    val story: String? = null,
    val full_picture: String? = null,
    val permalink_url: String? = null,
    val created_time: String
)

data class FacebookPaging(
    val cursors: FacebookCursors? = null,
    val next: String? = null
)

data class FacebookCursors(
    val before: String? = null,
    val after: String? = null
)

data class FacebookPageInfo(
    val id: String,
    val name: String,
    val picture: FacebookPicture? = null,
    val fan_count: Int? = null
)

data class FacebookPicture(
    val data: FacebookPictureData? = null
)

data class FacebookPictureData(
    val url: String? = null
)
