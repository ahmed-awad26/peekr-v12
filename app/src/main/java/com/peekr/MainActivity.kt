package com.peekr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.peekr.core.prefs.AppPreferences
import com.peekr.core.strings.ArabicStrings
import com.peekr.core.strings.EnglishStrings
import com.peekr.core.strings.LocalStrings
import com.peekr.security.SecurityManager
import com.peekr.ui.Screen
import com.peekr.ui.archive.ArchiveScreen
import com.peekr.ui.archive.CategoryScreen
import com.peekr.ui.feed.FeedScreen
import com.peekr.ui.onboarding.OnboardingScreen
import com.peekr.ui.onboarding.OnboardingViewModel
import com.peekr.ui.security.LockScreen
import com.peekr.ui.security.SecuritySettingsScreen
import com.peekr.ui.settings.SettingsScreen
import com.peekr.ui.settings.accounts.*
import com.peekr.ui.settings.apikeys.ApiKeysScreen
import com.peekr.ui.settings.backup.BackupScreen
import com.peekr.ui.settings.logs.LogsScreen
import com.peekr.ui.tools.ToolWebViewScreen
import com.peekr.ui.tools.ToolsScreen
import com.peekr.ui.theme.PeekrTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var securityManager: SecurityManager
    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // ← قراءة إعدادات المظهر من DataStore
            val isDark by appPreferences.isDarkMode.collectAsState(initial = false)
            val useSystem by appPreferences.useSystemTheme.collectAsState(initial = true)
            val isEnglish by appPreferences.isEnglish.collectAsState(initial = false)
            val systemDark = isSystemInDarkTheme()

            val darkTheme = when {
                useSystem -> systemDark
                else      -> isDark
            }

            val strings = if (isEnglish) EnglishStrings else ArabicStrings

            CompositionLocalProvider(LocalStrings provides strings) {
                PeekrTheme(darkTheme = darkTheme) {
                    PeekrRoot(securityManager = securityManager)
                }
            }
        }
    }

    override fun onPause() { super.onPause(); securityManager.checkAndLock() }
    override fun onResume() { super.onResume(); securityManager.checkAndLock() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeekrRoot(securityManager: SecurityManager) {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingDone by onboardingViewModel.isOnboardingDone.collectAsState()
    var appUnlocked by remember { mutableStateOf(!securityManager.isLockEnabled()) }

    if (onboardingDone == null) return

    if (!onboardingDone!!) {
        OnboardingScreen(onFinish = { })
        return
    }

    if (securityManager.isLockEnabled() && securityManager.isLocked() && !appUnlocked) {
        LockScreen(onUnlocked = { appUnlocked = true })
        return
    }

    val navController = rememberNavController()

    val s = LocalStrings.current
    val bottomNavItems = listOf(
        Triple(Screen.Feed.route,     s.home,     Icons.Default.Home),
        Triple(Screen.Archive.route,  s.archive,  Icons.Default.Bookmark),
        Triple(Screen.Tools.route,    s.tools,    Icons.Default.Extension),
        Triple(Screen.Settings.route, s.settings, Icons.Default.Settings),
    )

    val noBottomBarRoutes = listOf(
        Screen.Accounts.route, Screen.TelegramLogin.route, Screen.TelegramChannels.route, Screen.WhatsappQr.route,
        Screen.YoutubeLogin.route, Screen.FacebookLogin.route, Screen.RssLogin.route,
        Screen.ApiKeys.route, Screen.Logs.route, Screen.Backup.route,
        Screen.AddTool.route, Screen.Categories.route, Screen.SecuritySettings.route,
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute !in noBottomBarRoutes &&
            !currentRoute.orEmpty().startsWith("tools/webview")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination
                    bottomNavItems.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Feed.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Feed.route)             { FeedScreen(navController) }
            composable(Screen.Archive.route)          { ArchiveScreen(navController) }
            composable(Screen.Categories.route)       { CategoryScreen(navController) }
            composable(Screen.Tools.route)            { ToolsScreen(navController) }
            composable(Screen.Settings.route)         { SettingsScreen(navController) }
            composable(Screen.Accounts.route)         { AccountsScreen(navController) }
            composable(Screen.TelegramLogin.route)    { TelegramLoginScreen(navController) }
            composable(Screen.TelegramChannels.route)  { TelegramChannelsScreen(navController) }
            composable(Screen.WhatsappQr.route)       { WhatsappQrScreen(navController) }
            composable(Screen.YoutubeLogin.route)     { YoutubeLoginScreen(navController) }
            composable(Screen.FacebookLogin.route)    { FacebookLoginScreen(navController) }
            composable(Screen.RssLogin.route)         { RssLoginScreen(navController) }
            composable(Screen.ApiKeys.route)          { ApiKeysScreen(navController) }
            composable(Screen.Logs.route)             { LogsScreen(navController) }
            composable(Screen.Backup.route)           { BackupScreen(navController) }
            composable(Screen.SecuritySettings.route) { SecuritySettingsScreen(navController) }
            composable(
                route = "tools/webview/{toolId}",
                arguments = listOf(navArgument("toolId") { type = NavType.LongType })
            ) { backStackEntry ->
                val toolId = backStackEntry.arguments?.getLong("toolId") ?: return@composable
                ToolWebViewScreen(toolId = toolId, navController = navController)
            }
        }
    }
}
