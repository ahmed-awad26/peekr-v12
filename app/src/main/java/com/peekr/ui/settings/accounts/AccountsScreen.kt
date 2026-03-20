package com.peekr.ui.settings.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

data class PlatformInfo(
    val id: String,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    navController: NavController,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val accounts by viewModel.allAccounts.collectAsState()
    var confirmDisconnect by remember { mutableStateOf<PlatformInfo?>(null) }

    val platforms = listOf(
        PlatformInfo("telegram", "تليجرام", Icons.Default.Send, Color(0xFF0088CC), "رسائل شخصية + قنوات + مجموعات"),
        PlatformInfo("youtube", "يوتيوب", Icons.Default.PlayArrow, Color(0xFFFF0000), "متابعة قنوات وفيديوهات"),
        PlatformInfo("whatsapp", "واتساب", Icons.Default.Chat, Color(0xFF25D366), "رسائل شخصية عبر QR"),
        PlatformInfo("facebook", "فيسبوك", Icons.Default.Facebook, Color(0xFF1877F2), "صفحات عامة فقط"),
        PlatformInfo("rss", "RSS", Icons.Default.RssFeed, Color(0xFFFF6600), "أي موقع يدعم RSS"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ربط الحسابات") },
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
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "اربط حساباتك للبدء في متابعة المحتوى",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(platforms.size) { index ->
                val platform = platforms[index]
                val isConnected = viewModel.isConnected(platform.id, accounts)
                val count = viewModel.getCount(platform.id, accounts)

                PlatformCard(
                    platform = platform,
                    isConnected = isConnected,
                    connectedCount = count,
                    onConnectClick = {
                        when (platform.id) {
                            "telegram" -> navController.navigate("settings/accounts/telegram")
                            "youtube"  -> navController.navigate("settings/accounts/youtube")
                            "whatsapp" -> navController.navigate("settings/accounts/whatsapp")
                            "facebook" -> navController.navigate("settings/accounts/facebook")
                            "rss"      -> navController.navigate("settings/accounts/rss")
                        }
                    },
                    onManageClick = {
                        when (platform.id) {
                            "telegram" -> navController.navigate("settings/accounts/telegram/channels")
                            "youtube"  -> navController.navigate("settings/accounts/youtube")
                            "rss"      -> navController.navigate("settings/accounts/rss")
                            else -> navController.navigate("settings/accounts/${platform.id}")
                        }
                    },
                    onDisconnectClick = { confirmDisconnect = platform }
                )
            }
        }
    }

    confirmDisconnect?.let { platform ->
        AlertDialog(
            onDismissRequest = { confirmDisconnect = null },
            title = { Text("قطع ${platform.name}؟") },
            text = { Text("سيتم حذف جميع الروابط المضافة لـ ${platform.name}. هل أنت متأكد؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disconnectPlatform(platform.id)
                        confirmDisconnect = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف الكل") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun PlatformCard(
    platform: PlatformInfo,
    isConnected: Boolean,
    connectedCount: Int = 0,
    onConnectClick: () -> Unit,
    onManageClick: () -> Unit = {},
    onDisconnectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = platform.color.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = platform.icon,
                        contentDescription = null,
                        tint = platform.color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = platform.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = platform.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isConnected) {
                    val label = if (connectedCount > 1) "✓ $connectedCount روابط مضافة" else "✓ متصل"
                    Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                if (isConnected) {
                    Button(
                        onClick = onManageClick,
                        colors = ButtonDefaults.buttonColors(containerColor = platform.color),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text("إدارة", style = MaterialTheme.typography.labelMedium) }
                    OutlinedButton(
                        onClick = onDisconnectClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text("حذف", style = MaterialTheme.typography.labelMedium) }
                } else {
                    Button(onClick = onConnectClick, colors = ButtonDefaults.buttonColors(containerColor = platform.color)) {
                        Text("ربط")
                    }
                }
            }
        }
    }
}
