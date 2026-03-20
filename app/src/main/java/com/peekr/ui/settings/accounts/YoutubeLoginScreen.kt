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
import com.peekr.core.strings.LocalStrings
import com.peekr.data.local.entities.AccountEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeLoginScreen(
    navController: NavController,
    viewModel: YoutubeViewModel = hiltViewModel()
) {
    var channelUrl by remember { mutableStateOf("") }
    var editingChannel by remember { mutableStateOf<AccountEntity?>(null) }
    var editText by remember { mutableStateOf("") }
    val channels by viewModel.channels.collectAsState()
    val validations by viewModel.validations.collectAsState()
    val s = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.youtubeLinkTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = s.back)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            // Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color(0xFFFF0000).copy(alpha = 0.12f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color(0xFFFF0000), modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(s.youtubeLinkTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        s.youtubeLinkHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Input row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = channelUrl,
                        onValueChange = { channelUrl = it },
                        label = { Text(s.channelUrlLabel) },
                        placeholder = { Text(s.channelPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, null,
                                tint = Color(0xFFFF0000), modifier = Modifier.size(20.dp))
                        }
                    )
                    Button(
                        onClick = {
                            viewModel.addChannel(channelUrl)
                            channelUrl = ""
                        },
                        enabled = channelUrl.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
                    ) { Text(s.add) }
                }
            }

            // Section header
            if (channels.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${s.addedChannels} (${channels.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        // إعادة التحقق من الكل
                        TextButton(onClick = {
                            channels.forEach { ch ->
                                viewModel.validateChannel(ch.id, ch.accountName)
                            }
                        }) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تحقق من الكل", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Channel list
            items(channels, key = { it.id }) { channel ->
                val validation = validations[channel.id] ?: ChannelValidation.Idle

                if (editingChannel?.id == channel.id) {
                    // Edit mode
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editText,
                                onValueChange = { editText = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(s.editLinkLabel) }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    viewModel.updateChannel(channel, editText)
                                    editingChannel = null
                                }, modifier = Modifier.weight(1f)) { Text(s.saveEdit) }
                                OutlinedButton(onClick = { editingChannel = null }, modifier = Modifier.weight(1f)) { Text(s.cancel) }
                            }
                        }
                    }
                } else {
                    YoutubeChannelCard(
                        channel = channel,
                        validation = validation,
                        strings = s,
                        onEdit = { editingChannel = channel; editText = channel.accountName },
                        onDelete = { viewModel.removeChannel(channel) },
                        onRecheck = { viewModel.validateChannel(channel.id, channel.accountName) }
                    )
                }
            }

            if (channels.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                        Text(s.done)
                    }
                }
            }
        }
    }
}

@Composable
private fun YoutubeChannelCard(
    channel: AccountEntity,
    validation: ChannelValidation,
    strings: com.peekr.core.strings.AppStrings,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRecheck: () -> Unit
) {
    val validationColor = when (validation) {
        is ChannelValidation.Valid -> Color(0xFF4CAF50)
        is ChannelValidation.Invalid -> MaterialTheme.colorScheme.error
        is ChannelValidation.NoApiKey -> Color(0xFFFFA000)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PlayArrow, null,
                    tint = Color(0xFFFF0000), modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    channel.accountName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }

            // Validation status bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (validation) {
                    is ChannelValidation.Checking -> {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text(strings.checking, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is ChannelValidation.Valid -> {
                        Icon(Icons.Default.Done, null,
                            tint = validationColor, modifier = Modifier.size(16.dp))
                        Text("${strings.channelFound}: ${validation.channelName}",
                            style = MaterialTheme.typography.labelSmall, color = validationColor)
                    }
                    is ChannelValidation.Invalid -> {
                        Icon(Icons.Default.Close, null,
                            tint = validationColor, modifier = Modifier.size(16.dp))
                        Text(strings.channelNotFound, style = MaterialTheme.typography.labelSmall, color = validationColor)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onRecheck, contentPadding = PaddingValues(0.dp)) {
                            Text("إعادة", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    is ChannelValidation.NoApiKey -> {
                        Icon(Icons.Default.Lock, null,
                            tint = validationColor, modifier = Modifier.size(16.dp))
                        Text(strings.addApiKeyFirst, style = MaterialTheme.typography.labelSmall, color = validationColor)
                    }
                    is ChannelValidation.Idle -> {
                        TextButton(onClick = onRecheck, contentPadding = PaddingValues(0.dp)) {
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.testConnection, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
