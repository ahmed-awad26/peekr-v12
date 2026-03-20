package com.peekr.ui

sealed class Screen(val route: String) {
    object Feed : Screen("feed")
    object Archive : Screen("archive")
    object Categories : Screen("archive/categories")
    object Tools : Screen("tools")
    object Settings : Screen("settings")
    object Accounts : Screen("settings/accounts")
    object TelegramLogin : Screen("settings/accounts/telegram")
    object TelegramChannels : Screen("settings/accounts/telegram/channels")
    object WhatsappQr : Screen("settings/accounts/whatsapp")
    object YoutubeLogin : Screen("settings/accounts/youtube")
    object FacebookLogin : Screen("settings/accounts/facebook")
    object RssLogin : Screen("settings/accounts/rss")
    object ApiKeys : Screen("settings/apikeys")
    object Logs : Screen("settings/logs")
    object Backup : Screen("settings/backup")
    object SecuritySettings : Screen("settings/security")
    object AddTool : Screen("tools/add")
    object ToolWebView : Screen("tools/webview/{toolId}") {
        fun createRoute(toolId: Long) = "tools/webview/$toolId"
    }
}
