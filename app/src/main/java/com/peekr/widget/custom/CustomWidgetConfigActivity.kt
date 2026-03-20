package com.peekr.widget.custom

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.peekr.data.local.dao.PostDao
import com.peekr.ui.theme.PeekrTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CustomWidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var postDao: PostDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // لو مفيش ID صح، خروج
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            PeekrTheme {
                WidgetConfigScreen(
                    onConfirm = { sourceId, sourceName, platformId ->
                        kotlinx.coroutines.GlobalScope.launch {
                            val glanceId = GlanceAppWidgetManager(this@CustomWidgetConfigActivity)
                                .getGlanceIdBy(appWidgetId)

                            updateAppWidgetState(
                                this@CustomWidgetConfigActivity,
                                glanceId
                            ) { prefs ->
                                prefs[SOURCE_ID_KEY] = sourceId
                                prefs[SOURCE_NAME_KEY] = sourceName
                                prefs[PLATFORM_ID_KEY] = platformId
                            }

                            CustomSourceWidget().update(
                                this@CustomWidgetConfigActivity,
                                glanceId
                            )
                        }

                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(RESULT_OK, resultValue)
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    onConfirm: (sourceId: String, sourceName: String, platformId: String) -> Unit,
    onCancel: () -> Unit
) {
    val platforms = listOf(
        "all" to "كل المنصات",
        "youtube" to "يوتيوب",
        "telegram" to "تليجرام",
        "whatsapp" to "واتساب",
        "facebook" to "فيسبوك",
        "rss" to "RSS"
    )

    var selectedPlatform by remember { mutableStateOf("all") }
    var sourceId by remember { mutableStateOf("") }
    var sourceName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعداد الويدجيت") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("اختار المنصة:", style = MaterialTheme.typography.titleMedium)

            platforms.forEach { (id, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPlatform = id }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = selectedPlatform == id,
                        onClick = { selectedPlatform = id }
                    )
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Divider()

            OutlinedTextField(
                value = sourceId,
                onValueChange = { sourceId = it },
                label = { Text("ID المصدر (اختياري)") },
                placeholder = { Text("مثال: channel_id أو page_id") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = sourceName,
                onValueChange = { sourceName = it },
                label = { Text("اسم الويدجيت") },
                placeholder = { Text("مثال: قناة تقنية") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val finalSourceId = if (sourceId.isNotEmpty()) sourceId else selectedPlatform
                    val finalName = sourceName.ifEmpty {
                        platforms.find { it.first == selectedPlatform }?.second ?: "Peekr"
                    }
                    onConfirm(finalSourceId, finalName, selectedPlatform)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إضافة الويدجيت")
            }
        }
    }
}
