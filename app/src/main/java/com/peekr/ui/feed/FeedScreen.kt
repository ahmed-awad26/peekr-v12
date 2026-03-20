package com.peekr.ui.feed

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.peekr.core.strings.LocalStrings
import com.peekr.data.local.entities.PostEntity
import com.peekr.ui.archive.ArchiveViewModel
import com.peekr.ui.archive.SavePostDialog
import com.peekr.ui.feed.components.PostCard
import com.peekr.ui.feed.components.formatTime

data class PlatformTab(
    val id: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    feedViewModel: FeedViewModel = hiltViewModel(),
    archiveViewModel: ArchiveViewModel = hiltViewModel()
) {
    val uiState by feedViewModel.uiState.collectAsState()
    val archiveState by archiveViewModel.uiState.collectAsState()
    val s = LocalStrings.current
    var postToSave by remember { mutableStateOf<PostEntity?>(null) }
    val listState = rememberLazyListState()

    val platformTabs = listOf(
        PlatformTab("all",      Icons.Default.GridView,  MaterialTheme.colorScheme.primary),
        PlatformTab("youtube",  Icons.Default.PlayArrow, Color(0xFFFF0000)),
        PlatformTab("telegram", Icons.Default.Send,      Color(0xFF0088CC)),
        PlatformTab("whatsapp", Icons.Default.Chat,      Color(0xFF25D366)),
        PlatformTab("facebook", Icons.Default.Facebook,  Color(0xFF1877F2)),
        PlatformTab("rss",      Icons.Default.RssFeed,   Color(0xFFFF6600)),
    )

    val platformNames = mapOf(
        "all" to s.allPlatforms, "youtube" to "يوتيوب", "telegram" to "تليجرام",
        "whatsapp" to "واتساب", "facebook" to "فيسبوك", "rss" to "RSS"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                ))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Visibility, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(s.appName, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                        if (uiState.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("${uiState.unreadCount}", style = MaterialTheme.typography.labelSmall,
                                    color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO search */ }) {
                        Icon(Icons.Default.Search, "بحث")
                    }
                    AnimatedContent(targetState = uiState.isSyncing, label = "sync") { syncing ->
                        if (syncing) {
                            Box(Modifier.padding(end = 12.dp).size(36.dp), Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            }
                        } else {
                            IconButton(onClick = { feedViewModel.syncAll() }) {
                                Icon(Icons.Default.Refresh, s.refreshNow)
                            }
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            // Platform filter pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(platformTabs) { tab ->
                    PlatformPill(
                        label = platformNames[tab.id] ?: tab.id,
                        icon = tab.icon, color = tab.color,
                        selected = uiState.selectedPlatform == tab.id,
                        count = if (tab.id != "all") uiState.posts.count { it.platformId == tab.id } else 0,
                        onClick = { feedViewModel.selectPlatform(tab.id) }
                    )
                }
            }

            // Banner
            AnimatedVisibility(uiState.error != null || uiState.syncMessage != null) {
                val msg = uiState.error ?: uiState.syncMessage
                val isError = uiState.error != null
                msg?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        ), shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Icon(if (isError) Icons.Default.Error else Icons.Default.Done, null,
                                tint = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer)
                            IconButton(onClick = { feedViewModel.clearError() }, Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp),
                                    tint = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }

            // Content
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                            Text(s.syncInProgress, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                uiState.posts.isEmpty() -> {
                    EmptyFeedView(
                        onRefresh = { feedViewModel.syncAll() },
                        onGoToSettings = { navController.navigate("settings") },
                        strings = s
                    )
                }
                // YouTube — per-channel grouped view
                uiState.selectedPlatform == "youtube" && uiState.groupedPosts.size > 1 -> {
                    YoutubeChannelFeed(
                        grouped = uiState.groupedPosts,
                        onSave = { postToSave = it },
                        onMarkRead = { feedViewModel.markAsRead(it) }
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(
                                    "${platformNames[uiState.selectedPlatform] ?: uiState.selectedPlatform} · ${uiState.posts.size} منشور",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (uiState.unreadCount > 0) {
                                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("${uiState.unreadCount} غير مقروء",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                        items(uiState.posts, key = { it.id }) { post ->
                            PostCard(post = post, onSaveClick = { postToSave = it })
                        }
                    }
                }
            }
        }
    }

    postToSave?.let { post ->
        SavePostDialog(
            post = post, categories = archiveState.categories,
            onSave = { catId, note -> archiveViewModel.savePost(post, catId, note); postToSave = null },
            onDismiss = { postToSave = null }
        )
    }
}

// ==============================
// YouTube per-channel sections
// ==============================
@Composable
fun YoutubeChannelFeed(
    grouped: Map<String, List<PostEntity>>,
    onSave: (PostEntity) -> Unit,
    onMarkRead: (Long) -> Unit
) {
    val red = Color(0xFFFF0000)
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        grouped.forEach { (channelName, posts) ->
            // Channel header
            item(key = "header_$channelName") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(red.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = red, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(channelName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${posts.size} فيديو", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(shape = RoundedCornerShape(50), color = red.copy(alpha = 0.10f)) {
                        Text("${posts.count { !it.isRead }} جديد",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall, color = red,
                            fontWeight = FontWeight.Medium)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Horizontal scroll row of video cards
            item(key = "row_$channelName") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(posts, key = { it.id }) { post ->
                        YoutubeVideoCard(post = post, onSave = { onSave(post) })
                    }
                }
            }
        }
    }
}

@Composable
fun YoutubeVideoCard(post: PostEntity, onSave: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var saved by remember { mutableStateOf(false) }
    val red = Color(0xFFFF0000)

    Card(
        modifier = Modifier
            .width(240.dp)
            .clickable { post.postUrl?.let { try { uriHandler.openUri(it) } catch (_: Exception) {} } },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Thumbnail
            Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                if (post.mediaUrl != null) {
                    AsyncImage(
                        model = post.mediaUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(red.copy(alpha = 0.08f)),
                        Alignment.Center) {
                        Icon(Icons.Default.PlayArrow, null, tint = red, modifier = Modifier.size(36.dp))
                    }
                }
                // Unread badge
                if (!post.isRead) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                            .clip(CircleShape).background(red).padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("جديد", style = MaterialTheme.typography.labelSmall, color = Color.White,
                            fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Duration placeholder & play overlay
                Box(
                    modifier = Modifier.align(Alignment.Center)
                        .size(40.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    post.content.lines().first().take(70),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (!post.isRead) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(formatTime(post.timestamp), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Row {
                        IconButton(onClick = onSave, Modifier.size(28.dp)) {
                            Icon(if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                null, modifier = Modifier.size(15.dp),
                                tint = if (saved) red else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// ==============================
// Platform pill chip
// ==============================
@Composable
private fun PlatformPill(
    label: String, icon: ImageVector, color: Color,
    selected: Boolean, count: Int, onClick: () -> Unit
) {
    val bgColor = if (selected) color else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = bgColor,
        shadowElevation = if (selected) 4.dp else 0.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(16.dp))
            Text(label, color = contentColor, style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (count > 0 && !selected) {
                Box(Modifier.clip(CircleShape).background(color.copy(alpha = 0.2f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                    Text("$count", style = MaterialTheme.typography.labelSmall, color = color, fontSize = 10.sp)
                }
            }
        }
    }
}

// ==============================
// Empty state
// ==============================
@Composable
private fun EmptyFeedView(
    onRefresh: () -> Unit, onGoToSettings: () -> Unit,
    strings: com.peekr.core.strings.AppStrings
) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(28.dp)).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Inbox, null, modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(strings.noContentYet, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(strings.connectAccountsOrRefresh, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onGoToSettings) {
                    Icon(Icons.Default.Settings, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text(strings.settings)
                }
                Button(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text(strings.refreshNow)
                }
            }
        }
    }
}
