package com.peekr.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.peekr.data.local.entities.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    navController: NavController,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة التصنيفات") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة تصنيف")
            }
        }
    ) { paddingValues ->
        if (uiState.categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Category,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "لا توجد تصنيفات بعد",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "اضغط + لإضافة تصنيف جديد",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.categories, key = { it.id }) { category ->
                    CategoryCard(
                        category = category,
                        onEdit = { editingCategory = category },
                        onDelete = { viewModel.deleteCategory(category) }
                    )
                }
            }
        }
    }

    // ديالوج إضافة تصنيف
    if (showAddDialog) {
        CategoryDialog(
            title = "إضافة تصنيف",
            onConfirm = { name, color ->
                viewModel.addCategory(name, color)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // ديالوج تعديل تصنيف
    editingCategory?.let { cat ->
        CategoryDialog(
            title = "تعديل التصنيف",
            initialName = cat.name,
            initialColor = cat.color,
            onConfirm = { name, color ->
                viewModel.updateCategory(cat.copy(name = name, color = color))
                editingCategory = null
            },
            onDismiss = { editingCategory = null }
        )
    }
}

@Composable
fun CategoryCard(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = try { Color(android.graphics.Color.parseColor(category.color)) }
                    catch (e: Exception) { MaterialTheme.colorScheme.primary },
                    modifier = Modifier.size(24.dp)
                ) { }
                Text(category.name, style = MaterialTheme.typography.titleMedium)
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف التصنيف") },
            text = { Text("هيتحذف التصنيف \"${category.name}\" والعناصر المرتبطة بيه هتفضل بدون تصنيف.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun CategoryDialog(
    title: String,
    initialName: String = "",
    initialColor: String = "#6200EE",
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    val colors = listOf(
        "#6200EE", "#03DAC6", "#FF6200", "#E91E63",
        "#2196F3", "#4CAF50", "#FF9800", "#9C27B0",
        "#F44336", "#00BCD4", "#8BC34A", "#FF5722"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم التصنيف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اختار لون:", style = MaterialTheme.typography.labelMedium)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colors) { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = try { Color(android.graphics.Color.parseColor(color)) }
                                catch (e: Exception) { MaterialTheme.colorScheme.primary },
                                modifier = Modifier.size(if (isSelected) 32.dp else 28.dp)
                            ) { }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotEmpty()) onConfirm(name, selectedColor) },
                enabled = name.isNotEmpty()
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
