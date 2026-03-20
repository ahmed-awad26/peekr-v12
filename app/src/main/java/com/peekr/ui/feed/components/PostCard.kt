package com.peekr.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.peekr.data.local.entities.PostEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PostCard(
    post: PostEntity,
    onSaveClick: (PostEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    var isSaved by remember { mutableStateOf(false) }

    val platformColor = when (post.platformId) {
        "youtube"  -> Color(0xFFFF0000)
        "telegram" -> Color(0xFF0088CC)
        "whatsapp" -> Color(0xFF25D366)
        "facebook" -> Color(0xFF1877F2)
        "rss"      -> Color(0xFFFF6600)
        else       -> MaterialTheme.colorScheme.primary
    }

    val platformIcon = when (post.platformId) {
        "youtube"  -> Icons.Default.PlayArrow
        "telegram" -> Icons.Default.Send
        "whatsapp" -> Icons.Default.Chat
        "facebook" -> Icons.Default.Facebook
        "rss"      -> Icons.Default.RssFeed
        else       -> Icons.Default.Star
    }

    val platformName = when (post.platformId) {
        "youtube"  -> "يوتيوب"
        "telegram" -> "تليجرام"
        "whatsapp" -> "واتساب"
        "facebook" -> "فيسبوك"
        "rss"      -> "RSS"
        else       -> post.platformId
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                post.postUrl?.let { url ->
                    try { uriHandler.openUri(url) } catch (_: Exception) { }
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!post.isRead)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ← شريط اللون في الأعلى (رفيع)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(platformColor.copy(alpha = if (!post.isRead) 1f else 0.3f))
            )

            // Media image
            post.mediaUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp)) {

                // Header row: platform badge + source name + time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Platform icon circle
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(platformColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                platformIcon, null,
                                tint = platformColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = post.sourceName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Unread dot
                        if (!post.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(platformColor)
                            )
                        }
                    }
                    Text(
                        text = formatTime(post.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Content
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (!post.isRead) FontWeight.Medium else FontWeight.Normal,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(6.dp))

                // Footer actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Platform label (small)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = platformColor.copy(alpha = 0.10f)
                    ) {
                        Text(
                            platformName,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = platformColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row {
                        // Open link button
                        post.postUrl?.let {
                            IconButton(
                                onClick = {
                                    try { uriHandler.openUri(it) } catch (_: Exception) { }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Launch, null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Save / bookmark button
                        IconButton(
                            onClick = { isSaved = !isSaved; onSaveClick(post) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isSaved) Icons.Default.Bookmark
                                else Icons.Default.BookmarkBorder,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSaved) platformColor
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000       -> "الآن"
        diff < 3_600_000    -> "${diff / 60_000} د"
        diff < 86_400_000   -> "${diff / 3_600_000} س"
        diff < 604_800_000  -> "${diff / 86_400_000} ي"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}
