package com.peekr.data.remote.rss

import com.peekr.core.logger.AppLogger
import com.peekr.data.local.dao.AccountDao
import com.peekr.data.local.dao.PostDao
import com.peekr.data.local.entities.PostEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.net.URL
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class RssItem(
    val title: String,
    val description: String,
    val link: String,
    val pubDate: Long,
    val imageUrl: String? = null,
    val feedTitle: String = ""
)

@Singleton
class RssClient @Inject constructor(
    private val postDao: PostDao,
    private val accountDao: AccountDao,
    private val logger: AppLogger
) {
    suspend fun syncFeeds(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val feedAccounts = accountDao.getAllAccountsByPlatformSync("rss")
            if (feedAccounts.isEmpty()) {
                return@withContext Result.success(0)
            }

            var totalNew = 0
            feedAccounts.forEach { feedAccount ->
                val url = feedAccount.accountName
                try {
                    val items = fetchFeed(url.trim())
                    items.forEach { item ->
                        postDao.insertPost(
                            PostEntity(
                                platformId = "rss",
                                sourceId = url.trim(),
                                sourceName = item.feedTitle.ifEmpty { extractDomain(url) },
                                content = item.title + "\n\n" + item.description.take(300),
                                mediaUrl = item.imageUrl,
                                postUrl = item.link,
                                timestamp = item.pubDate
                            )
                        )
                        totalNew++
                    }

                } catch (e: Exception) {
                    logger.error("فشل جلب RSS: $url", "rss", e)
                }
            }

            logger.info("RSS: تم جلب $totalNew منشور جديد", "rss")
            Result.success(totalNew)
        } catch (e: Exception) {
            logger.error("خطأ في مزامنة RSS", "rss", e)
            Result.failure(e)
        }
    }

    private fun fetchFeed(url: String): List<RssItem> {
        val items = mutableListOf<RssItem>()
        var feedTitle = ""

        val connection = URL(url).openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        val inputStream: InputStream = connection.getInputStream()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)

        var currentTitle = ""
        var currentDesc = ""
        var currentLink = ""
        var currentPubDate = ""
        var currentImage = ""
        var inItem = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tagName) {
                        "item", "entry" -> {
                            inItem = true
                            currentTitle = ""
                            currentDesc = ""
                            currentLink = ""
                            currentPubDate = ""
                            currentImage = ""
                        }
                        "enclosure" -> {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (type.startsWith("image")) {
                                currentImage = parser.getAttributeValue(null, "url") ?: ""
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (inItem) {
                        when (tagName) {
                            "title" -> currentTitle = text
                            "description", "summary", "content" -> if (currentDesc.isEmpty()) currentDesc = text
                            "link" -> currentLink = text
                            "pubDate", "published", "updated" -> currentPubDate = text
                        }
                    } else {
                        if (tagName == "title" && feedTitle.isEmpty()) feedTitle = text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName == "item" || tagName == "entry") {
                        if (currentTitle.isNotEmpty()) {
                            items.add(
                                RssItem(
                                    title = currentTitle,
                                    description = stripHtml(currentDesc),
                                    link = currentLink,
                                    pubDate = parseRssDate(currentPubDate),
                                    imageUrl = currentImage.ifEmpty { null },
                                    feedTitle = feedTitle
                                )
                            )
                        }
                        inItem = false
                    }
                }
            }
            eventType = parser.next()
        }
        inputStream.close()
        return items
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").trim()
    }

    private fun parseRssDate(dateStr: String): Long {
        if (dateStr.isEmpty()) return System.currentTimeMillis()
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        formats.forEach { format ->
            try {
                return SimpleDateFormat(format, Locale.ENGLISH).parse(dateStr)?.time
                    ?: System.currentTimeMillis()
            } catch (e: ParseException) { }
        }
        return System.currentTimeMillis()
    }

    private fun extractDomain(url: String): String {
        return try {
            URL(url).host.removePrefix("www.")
        } catch (e: Exception) {
            url
        }
    }
}
