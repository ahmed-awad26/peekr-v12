package com.peekr.ui.settings.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.peekr.data.backup.DriveBackupItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    navController: NavController,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val driveState by viewModel.driveState.collectAsState()
    var showRestoreDialog by remember { mutableStateOf<DriveBackupItem?>(null) }

    // launcher لتصدير ملف
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    // launcher لاستيراد ملف
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    // launcher لجوجل سيجن إن
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("النسخ الاحتياطي") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==============================
            // رسائل النجاح / الخطأ
            // ==============================
            if (uiState.successMessage != null || uiState.errorMessage != null) {
                item {
                    val isSuccess = uiState.successMessage != null
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.successMessage ?: uiState.errorMessage ?: "",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                            IconButton(
                                onClick = { viewModel.clearMessages() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // ==============================
            // معلومات قاعدة البيانات
            // ==============================
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Storage, null,
                                tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("قاعدة البيانات",
                                    style = MaterialTheme.typography.titleSmall)
                                Text(uiState.dbSize,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ==============================
            // نسخ يدوي
            // ==============================
            item {
                Text(
                    "نسخ يدوي (محلي)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "احفظ نسخة احتياطية على جهازك أو استعد من نسخة موجودة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    exportLauncher.launch("peekr_backup_$ts.zip")
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isExporting
                            ) {
                                if (uiState.isExporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Upload, null,
                                        modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(4.dp))
                                Text("تصدير")
                            }

                            OutlinedButton(
                                onClick = { importLauncher.launch("application/zip") },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isImporting
                            ) {
                                if (uiState.isImporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Download, null,
                                        modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(4.dp))
                                Text("استيراد")
                            }
                        }
                    }
                }
            }

            // ==============================
            // جوجل درايف
            // ==============================
            item {
                Text(
                    "جوجل درايف",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // حالة الاتصال
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = Color(0xFF4285F4).copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CloudQueue, null,
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (driveState.isConnected) driveState.accountEmail ?: "متصل"
                                    else "غير متصل",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (driveState.lastBackupTime != null) {
                                    Text(
                                        "آخر نسخة: ${driveState.lastBackupTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (driveState.isConnected) {
                                OutlinedButton(
                                    onClick = { viewModel.disconnectDrive() },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("قطع") }
                            } else {
                                Button(
                                    onClick = {
                                        signInLauncher.launch(viewModel.getSignInIntent())
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4285F4)
                                    )
                                ) { Text("ربط") }
                            }
                        }

                        // أزرار رفع / تحميل
                        if (driveState.isConnected) {
                            Divider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.uploadToDrive() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !driveState.isUploading
                                ) {
                                    if (driveState.isUploading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.CloudUpload, null,
                                            modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text("رفع الآن")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.loadDriveBackups() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isLoadingBackups
                                ) {
                                    Icon(Icons.Default.Refresh, null,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("عرض النسخ")
                                }
                            }
                        }
                    }
                }
            }

            // ==============================
            // قائمة نسخ درايف
            // ==============================
            if (uiState.isLoadingBackups) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.driveBackups.isNotEmpty()) {
                item {
                    Text(
                        "النسخ المحفوظة على درايف (${uiState.driveBackups.size})",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                items(uiState.driveBackups) { backup ->
                    DriveBackupCard(
                        backup = backup,
                        onRestore = { showRestoreDialog = backup }
                    )
                }
            }
        }
    }

    // ديالوج تأكيد الاستعادة
    showRestoreDialog?.let { backup ->
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = { Text("استعادة النسخة؟") },
            text = {
                Text(
                    "سيتم استبدال البيانات الحالية بـ:\n${backup.name}\n\nهذا الإجراء لا يمكن التراجع عنه."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFromDrive(backup.id)
                        showRestoreDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("استعادة") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun DriveBackupCard(
    backup: DriveBackupItem,
    onRestore: () -> Unit
) {
    val dateStr = if (backup.createdTime > 0) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(backup.createdTime))
    } else "غير معروف"

    val sizeStr = when {
        backup.sizeBytes < 1024 -> "${backup.sizeBytes} B"
        backup.sizeBytes < 1024 * 1024 -> "${backup.sizeBytes / 1024} KB"
        else -> "${backup.sizeBytes / (1024 * 1024)} MB"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Backup, null,
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        dateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        sizeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(onClick = onRestore) {
                Text("استعادة")
            }
        }
    }
}
