package com.peekr.ui.settings.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun TelegramChannelsScreen(
    navController: NavController,
    viewModel: TelegramChannelsViewModel = hiltViewModel()
) {
    val channels      by viewModel.channels.collectAsState()
    val verifications  by viewModel.verifications.collectAsState()
    val botConnected   by viewModel.botConnectedFlow.collectAsState()
    var input        by remember { mutableStateOf("") }
    val tgBlue       = Color(0xFF0088CC)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("قنوات تليجرام") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    if (channels.isNotEmpty()) {
                        IconButton(onClick = { viewModel.verifyAll() }) {
                            Icon(Icons.Default.Refresh, "تحقق من الكل", tint = tgBlue)
                        }
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ——— Status banner: bot connection ———
            item {
                val hasBotToken = botConnected
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasBotToken)
                            Color(0xFF4CAF50).copy(alpha = 0.10f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (hasBotToken) Icons.Default.Done else Icons.Default.Error,
                            null,
                            tint = if (hasBotToken) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (hasBotToken) "البوت متصل ✓"
                                else "البوت غير متصل",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (hasBotToken) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.error
                            )
                            if (!hasBotToken) {
                                Text(
                                    "أضف Bot Token من @BotFather في مفاتيح API أولاً",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!hasBotToken) {
                            TextButton(onClick = { navController.navigate("settings/apikeys") }) {
                                Text("مفاتيح API", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // ——— Add channel input ———
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("اسم القناة") },
                        placeholder = { Text("@channelname أو t.me/channel") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.Tag, null,
                                tint = tgBlue, modifier = Modifier.size(18.dp))
                        }
                    )
                    Button(
                        onClick = { viewModel.addChannel(input); input = "" },
                        enabled = input.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = tgBlue)
                    ) { Text("إضافة") }
                }
            }

            // ——— Channel list ———
            if (channels.isNotEmpty()) {
                item {
                    Text(
                        "القنوات المضافة (${channels.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = tgBlue, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(channels, key = { it.id }) { ch ->
                TelegramChannelCard(
                    channel    = ch,
                    state      = verifications[ch.id] ?: ChannelVerifyState.Idle,
                    onDelete   = { viewModel.removeChannel(ch) },
                    onVerify   = { viewModel.verifyChannel(ch.id, ch.accountName) }
                )
            }

            if (channels.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Send, null,
                            tint = tgBlue.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        Text("لم تضف قنوات بعد",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                        Text("أضف اسم القناة للبدء في متابعتها\nتأكد أن البوت مضاف لها كـ Admin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }

            if (channels.isNotEmpty()) {
                item {
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("تم ✓") }
                }
            }
        }
    }
}

@Composable
private fun TelegramChannelCard(
    channel: AccountEntity,
    state: ChannelVerifyState,
    onDelete: () -> Unit,
    onVerify: () -> Unit
) {
    val tgBlue = Color(0xFF0088CC)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Send, null, tint = tgBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    channel.accountName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp))
                }
            }
            // Verification status row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (state) {
                    is ChannelVerifyState.Checking -> {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = tgBlue)
                        Text("جاري التحقق...", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is ChannelVerifyState.OK -> {
                        Icon(Icons.Default.Done, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Text("✓ ${state.title}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    }
                    is ChannelVerifyState.Fail -> {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text("القناة غير موجودة أو البوت مش Admin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = onVerify, contentPadding = PaddingValues(0.dp)) {
                            Text("إعادة", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    is ChannelVerifyState.NoBotToken -> {
                        Icon(Icons.Default.Lock, null,
                            tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                        Text("أضف Bot Token أولاً",
                            style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFA000))
                    }
                    is ChannelVerifyState.Idle -> {
                        TextButton(onClick = onVerify, contentPadding = PaddingValues(0.dp)) {
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تحقق من القناة", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
