package com.peekr.ui.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.peekr.data.local.entities.ArchiveEntity
import com.peekr.data.local.entities.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    navController: NavController,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("الأرشيف")
                        if (uiState.archiveCount > 0) {
                            Text(
                                "${uiState.archiveCount} عنصر محفوظ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("archive/categories") }) {
                        Icon(Icons.Default.Category, contentDescription = "التصنيفات")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // فلتر التصنيفات
            if (uiState.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategoryId == null,
                            onClick = { viewModel.filterByCategory(null) },
                            label = { Text("الكل") }
                        )
                    }
                    items(uiState.categories) { category ->
                        FilterChip(
                            selected = uiState.selectedCategoryId == category.id,
                            onClick = { viewModel.filterByCategory(category.id) },
                            label = { Text(category.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier.size(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = try { Color(android.graphics.Color.parseColor(category.color)) }
                                        catch (e: Exception) { MaterialTheme.colorScheme.primary }
                                    ) { }
                                }
                            }
                        )
                    }
                }
            }

            // المحتوى
            if (uiState.archives.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "الأرشيف فارغ",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "احفظ أي محتوى من الفيد للرجوع إليه لاحقاً",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.archives, key = { it.id }) { archive ->
                        ArchiveCard(
                            archive = archive,
                            categories = uiState.categories,
                            onDelete = { viewModel.deleteArchive(archive) },
                            onCategoryChange = { categoryId ->
                                viewModel.updateArchiveCategory(archive, categoryId)
                            },
                            onNoteChange = { note ->
                                viewModel.updateArchiveNote(archive, note)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveCard(
    archive: ArchiveEntity,
    categories: List<CategoryEntity>,
    onDelete: () -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onNoteChange: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var showMenu by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(archive.note ?: "") }

    val platformColor = when (archive.platformId) {
        "youtube" -> Color(0xFFFF0000)
        "telegram" -> Color(0xFF0088CC)
        "whatsapp" -> Color(0xFF25D366)
        "facebook" -> Color(0xFF1877F2)
        "rss" -> Color(0xFFFF6600)
        else -> MaterialTheme.colorScheme.primary
    }

    val category = categories.find { it.id == archive.categoryId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // هيدر
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = platformColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            archive.platformId,
                            style = MaterialTheme.typography.labelSmall,
                            color = platformColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        archive.sourceName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تغيير التصنيف") },
                            leadingIcon = { Icon(Icons.Default.Category, null) },
                            onClick = {
                                showMenu = false
                                showCategoryDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("إضافة ملاحظة") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = {
                                showMenu = false
                                showNoteDialog = true
                            }
                        )
                        archive.postUrl?.let {
                            DropdownMenuItem(
                                text = { Text("فتح الرابط") },
                                leadingIcon = { Icon(Icons.Default.Launch, null) },
                                onClick = {
                                    showMenu = false
                                    try { uriHandler.openUri(it) } catch (e: Exception) { }
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // المحتوى
            Text(
                archive.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // ملاحظة
            archive.note?.let { note ->
                if (note.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Note,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // التصنيف
            category?.let {
                Spacer(Modifier.height(6.dp))
                AssistChip(
                    onClick = { showCategoryDialog = true },
                    label = { Text(it.name, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Box(modifier = Modifier.size(8.dp)) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = try { Color(android.graphics.Color.parseColor(it.color)) }
                                catch (e: Exception) { MaterialTheme.colorScheme.primary },
                                modifier = Modifier.fillMaxSize()
                            ) { }
                        }
                    }
                )
            }
        }
    }

    // ديالوج الملاحظة
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("إضافة ملاحظة") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("الملاحظة") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(onClick = {
                    onNoteChange(noteText)
                    showNoteDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // ديالوج التصنيف
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("اختار تصنيف") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // بدون تصنيف
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCategoryChange(null)
                                showCategoryDialog = false
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(20.dp))
                        Text("بدون تصنيف")
                    }
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCategoryChange(cat.id)
                                    showCategoryDialog = false
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = try { Color(android.graphics.Color.parseColor(cat.color)) }
                                catch (e: Exception) { MaterialTheme.colorScheme.primary },
                                modifier = Modifier.size(16.dp)
                            ) { }
                            Text(cat.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
