package com.peekr.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.peekr.core.strings.LocalStrings
import com.peekr.ui.Screen
import com.peekr.ui.feed.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    feedViewModel: FeedViewModel = hiltViewModel()
) {
    val isDark   by settingsViewModel.isDarkMode.collectAsState()
    val useSystem by settingsViewModel.useSystemTheme.collectAsState()
    val s = LocalStrings.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLangDialog  by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settingsTitle) },
                actions = {
                    IconButton(onClick = { feedViewModel.syncAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = s.refreshNow)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(s.contentSection)

            SettingsItem(
                icon = Icons.Default.AccountCircle,
                title = s.connectedAccounts,
                subtitle = s.connectedAccountsDesc,
                onClick = { navController.navigate(Screen.Accounts.route) }
            )
            SettingsItem(
                icon = Icons.Default.Lock,
                title = s.apiKeys,
                subtitle = s.apiKeysDesc,
                onClick = { navController.navigate(Screen.ApiKeys.route) }
            )

            Spacer(Modifier.height(4.dp))
            SectionHeader(s.appearanceSection)

            SettingsToggleItem(
                icon = if (isDark) Icons.Default.Brightness4 else Icons.Default.Brightness7,
                title = s.themeTitle,
                subtitle = when {
                    useSystem -> s.followSystem
                    isDark    -> s.darkMode
                    else      -> s.lightMode
                },
                onClick = { showThemeDialog = true }
            )

            SettingsToggleItem(
                icon = Icons.Default.Language,
                title = s.language,
                subtitle = if (s.language == "Language") "English" else "العربية",
                onClick = { showLangDialog = true }
            )

            Spacer(Modifier.height(4.dp))
            SectionHeader(s.securitySection)

            SettingsItem(
                icon = Icons.Default.Lock,
                title = s.security,
                subtitle = s.securityDesc,
                onClick = { navController.navigate(Screen.SecuritySettings.route) }
            )
            SettingsItem(
                icon = Icons.Default.Backup,
                title = s.backup,
                subtitle = s.backupDesc,
                onClick = { navController.navigate(Screen.Backup.route) }
            )
            SettingsItem(
                icon = Icons.Default.Article,
                title = s.eventLogs,
                subtitle = s.eventLogsDesc,
                onClick = { navController.navigate(Screen.Logs.route) }
            )
        }
    }

    // ==============================
    // Theme dialog
    // ==============================
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(s.chooseTheme) },
            text = {
                Column {
                    ThemeOption(s.followSystem, useSystem) {
                        settingsViewModel.setUseSystemTheme(true)
                        showThemeDialog = false
                    }
                    ThemeOption(s.lightMode, !useSystem && !isDark) {
                        settingsViewModel.setDarkMode(false)
                        showThemeDialog = false
                    }
                    ThemeOption(s.darkMode, !useSystem && isDark) {
                        settingsViewModel.setDarkMode(true)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text(s.close) }
            }
        )
    }

    // ==============================
    // Language dialog
    // ==============================
    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = { Text("Language / اللغة") },
            text = {
                Column {
                    ThemeOption("العربية", s.language == "اللغة") {
                        settingsViewModel.setEnglish(false)
                        showLangDialog = false
                    }
                    ThemeOption("English", s.language == "Language") {
                        settingsViewModel.setEnglish(true)
                        showLangDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLangDialog = false }) { Text("OK / موافق") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsToggleItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
