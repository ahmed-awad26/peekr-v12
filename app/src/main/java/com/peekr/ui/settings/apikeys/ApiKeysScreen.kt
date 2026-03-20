package com.peekr.ui.settings.apikeys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.peekr.core.strings.LocalStrings

data class ApiKeyField(
    val platformId: String,
    val platformName: String,
    val keyName: String,
    val placeholder: String,
    val helpUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen(
    navController: NavController,
    viewModel: ApiKeysViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val s = LocalStrings.current
    val keyVisibility = remember { mutableStateMapOf<String, Boolean>() }

    val apiFields = listOf(
        ApiKeyField("telegram_bot",  "تليجرام", "Bot Token",       "123456:AAxxxxxxxx",  "من @BotFather في تليجرام"),
        ApiKeyField("telegram_id",   "تليجرام", "API ID (اختياري)","12345678",           "my.telegram.org — للحساب الشخصي"),
        ApiKeyField("telegram_hash", "تليجرام", "API Hash (اختياري)","abcdef1234...",    "my.telegram.org — للحساب الشخصي"),
        ApiKeyField("youtube",       "يوتيوب",  "YouTube API Key", "AIzaSy...",          "console.cloud.google.com"),
        ApiKeyField("facebook",      "فيسبوك",  "Access Token",    "EAABsbCS...",        "developers.facebook.com"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.apiKeysTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==============================
            // شريط حالة المنصات في الأعلى
            // ==============================
            item {
                Text(
                    s.platformStatus,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.platformStatuses.forEach { status ->
                        PlatformStatusChip(
                            status = status,
                            onTest = { viewModel.testConnection(status.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // معلومة
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info, null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            s.apiKeysInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ==============================
            // حقول الإدخال مجمعة حسب المنصة
            // ==============================
            val grouped = apiFields.groupBy { it.platformName }
            grouped.forEach { (platformName, fields) ->
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        platformName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(fields.size) { idx ->
                    val field = fields[idx]
                    val isVisible = keyVisibility[field.platformId] ?: false
                    val currentValue = uiState.keyValues[field.platformId] ?: ""

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { viewModel.updateKeyValue(field.platformId, it) },
                            label = { Text(field.keyName) },
                            placeholder = { Text(field.placeholder) },
                            supportingText = {
                                Text(
                                    "${s.fromWebsite}: ${field.helpUrl}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            visualTransformation = if (isVisible) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row {
                                    // عين - إخفاء/إظهار
                                    IconButton(onClick = {
                                        keyVisibility[field.platformId] = !isVisible
                                    }) {
                                        Icon(
                                            if (isVisible) Icons.Default.VisibilityOff
                                            else Icons.Default.Visibility,
                                            null, modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    // حذف
                                    if (currentValue.isNotEmpty()) {
                                        IconButton(onClick = {
                                            viewModel.updateKeyValue(field.platformId, "")
                                        }) {
                                            Icon(
                                                Icons.Default.Clear, null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            // لون الـ border حسب وجود قيمة
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (currentValue.isNotEmpty())
                                    Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (currentValue.isNotEmpty())
                                    Color(0xFF4CAF50).copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            // ==============================
            // زرار الحفظ
            // ==============================
            item {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { viewModel.saveAllKeys() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Done, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (uiState.isSaving) s.testing else s.saveKeys)
                }

                if (uiState.savedSuccess) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Done, null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                s.savedSuccess,
                                color = Color(0xFF4CAF50),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                uiState.error?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            err,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PlatformStatusChip(
    status: PlatformStatusItem,
    onTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color(status.color)
    val bgColor = when {
        status.isTesting           -> MaterialTheme.colorScheme.surfaceVariant
        status.isConnected == true -> Color(0xFF4CAF50).copy(alpha = 0.12f)
        status.isConnected == false -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        status.hasKey     -> color.copy(alpha = 0.12f)
        else                       -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        onClick = onTest,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                status.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            when {
                status.isTesting -> CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = color
                )
                status.isConnected == true -> Icon(
                    Icons.Default.Done, null,
                    tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp)
                )
                status.isConnected == false -> Icon(
                    Icons.Default.Close, null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)
                )
                status.hasKey -> Icon(
                    Icons.Default.Lock, null,
                    tint = color, modifier = Modifier.size(14.dp)
                )
                else -> Icon(
                    Icons.Default.Error, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = when {
                    status.isTesting           -> "..."
                    status.isConnected == true -> "OK"
                    status.isConnected == false -> "Fail"
                    status.hasKey     -> "Key ✓"
                    else                       -> "Missing"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    status.isConnected == true -> Color(0xFF4CAF50)
                    status.isConnected == false -> MaterialTheme.colorScheme.error
                    status.hasKey -> color
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
