package com.peekr.ui.settings.logs

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

data class LogItem(
    val id: Long,
    val level: String,
    val platformId: String?,
    val message: String,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(navController: NavController) {
    var showClearDialog by remember { mutableStateOf(false) }

    // بيانات تجريبية
    val sampleLogs = listOf(
        LogItem(1, "INFO", null, "التطبيق اشتغل بنجاح", System.currentTimeMillis() - 3600000),
        LogItem(2, "WARNING", "youtube", "تأخر في جلب البيانات", System.currentTimeMillis() - 1800000),
        LogItem(3, "ERROR", "telegram", "فشل الاتصال بالسيرفر", System.currentTimeMillis() - 900000),
        LogItem(4, "INFO", "rss", "تم تحديث 5 feeds", System.currentTimeMillis() - 300000),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سجل الأخطاء") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { /* تصدير */ }) {
                        Icon(Icons.Default.Share, contentDescription = "تصدير")
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "مسح")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (sampleLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد سجلات", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleLogs) { log ->
                    LogCard(log)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("مسح السجل") },
            text = { Text("هتتمسح كل السجلات، مش هتقدر ترجعها.") },
            confirmButton = {
                Button(
                    onClick = { showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("مسح")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun LogCard(log: LogItem) {
    val (color, icon) = when (log.level) {
        "ERROR" -> Pair(Color(0xFFB3261E), Icons.Default.Error)
        "WARNING" -> Pair(Color(0xFFF57C00), Icons.Default.Error)
        else -> Pair(Color(0xFF4CAF50), Icons.Default.Info)
    }

    val formatter = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.level + if (log.platformId != null) " • ${log.platformId}" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = color
                    )
                    Text(
                        text = formatter.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = log.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
