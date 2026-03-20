package com.peekr.ui.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.peekr.data.local.entities.CategoryEntity
import com.peekr.data.local.entities.PostEntity

@Composable
fun SavePostDialog(
    post: PostEntity,
    categories: List<CategoryEntity>,
    onSave: (categoryId: Long?, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Bookmark, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("حفظ في الأرشيف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // معاينة المحتوى
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = post.content.take(100) + if (post.content.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // اختيار التصنيف
                if (categories.isNotEmpty()) {
                    Text("تصنيف (اختياري):", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null },
                                label = { Text("بدون") }
                            )
                        }
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                                leadingIcon = if (selectedCategoryId == cat.id) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // ملاحظة
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظة (اختياري)") },
                    placeholder = { Text("لماذا حفظت هذا؟") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedCategoryId, note) }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
