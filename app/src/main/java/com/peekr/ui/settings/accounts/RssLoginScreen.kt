package com.peekr.ui.settings.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.peekr.data.local.entities.AccountEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssLoginScreen(
    navController: NavController,
    viewModel: RssViewModel = hiltViewModel()
) {
    var feedUrl by remember { mutableStateOf("") }
    var editingFeed by remember { mutableStateOf<AccountEntity?>(null) }
    var editText by remember { mutableStateOf("") }
    val feeds by viewModel.feeds.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إضافة RSS") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color(0xFFFF6600).copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.RssFeed, null, tint = Color(0xFFFF6600), modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("إضافة RSS Feed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "أضف رابط الـ RSS لأي موقع تريد متابعته — التغييرات بتتحفظ تلقائياً",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = feedUrl,
                        onValueChange = { feedUrl = it },
                        label = { Text("رابط RSS") },
                        placeholder = { Text("https://example.com/feed") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            viewModel.addFeed(feedUrl)
                            feedUrl = ""
                        },
                        enabled = feedUrl.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6600))
                    ) { Text("إضافة") }
                }
            }

            if (feeds.isNotEmpty()) {
                item {
                    Text(
                        "الـ Feeds المضافة (${feeds.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(feeds, key = { it.id }) { feed ->
                if (editingFeed?.id == feed.id) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editText,
                                onValueChange = { editText = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("تعديل الرابط") }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    viewModel.updateFeed(feed, editText)
                                    editingFeed = null
                                }, modifier = Modifier.weight(1f)) { Text("حفظ") }
                                OutlinedButton(onClick = { editingFeed = null }, modifier = Modifier.weight(1f)) { Text("إلغاء") }
                            }
                        }
                    }
                } else {
                    FeedRow(
                        url = feed.accountName,
                        onEdit = { editingFeed = feed; editText = feed.accountName },
                        onDelete = { viewModel.removeFeed(feed) }
                    )
                }
            }

            if (feeds.isNotEmpty()) {
                item {
                    Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                        Text("تم ✓")
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedRow(url: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.RssFeed, null, tint = Color(0xFFFF6600), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(url, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
