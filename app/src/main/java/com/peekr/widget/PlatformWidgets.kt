package com.peekr.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlatformWidgetEntryPoint {
    fun widgetRepository(): WidgetRepository
}

// ==============================
// Telegram Widget
// ==============================
class TelegramWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(
            context.applicationContext, PlatformWidgetEntryPoint::class.java
        ).widgetRepository()
        val posts = repo.getLatestPostsByPlatform("telegram", 5)
        provideContent { PlatformWidgetContent("telegram", posts) }
    }
}

class TelegramWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TelegramWidget()
}

// ==============================
// YouTube Widget
// ==============================
class YoutubeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(
            context.applicationContext, PlatformWidgetEntryPoint::class.java
        ).widgetRepository()
        val posts = repo.getLatestPostsByPlatform("youtube", 5)
        provideContent { PlatformWidgetContent("youtube", posts) }
    }
}

class YoutubeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = YoutubeWidget()
}

// ==============================
// WhatsApp Widget
// ==============================
class WhatsappWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(
            context.applicationContext, PlatformWidgetEntryPoint::class.java
        ).widgetRepository()
        val posts = repo.getLatestPostsByPlatform("whatsapp", 5)
        provideContent { PlatformWidgetContent("whatsapp", posts) }
    }
}

class WhatsappWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WhatsappWidget()
}

// ==============================
// Facebook Widget
// ==============================
class FacebookWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(
            context.applicationContext, PlatformWidgetEntryPoint::class.java
        ).widgetRepository()
        val posts = repo.getLatestPostsByPlatform("facebook", 5)
        provideContent { PlatformWidgetContent("facebook", posts) }
    }
}

class FacebookWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FacebookWidget()
}

// ==============================
// RSS Widget
// ==============================
class RssWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(
            context.applicationContext, PlatformWidgetEntryPoint::class.java
        ).widgetRepository()
        val posts = repo.getLatestPostsByPlatform("rss", 5)
        provideContent { PlatformWidgetContent("rss", posts) }
    }
}

class RssWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RssWidget()
}

// ==============================
// محتوى مشترك للمنصات
// ==============================
@Composable
fun PlatformWidgetContent(
    platformId: String,
    posts: List<com.peekr.data.local.entities.PostEntity>
) {
    val color = platformColor(platformId)
    val name = platformName(platformId)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
    ) {
        // هيدر بلون المنصة
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(color.copy(alpha = 0.1f)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(color)
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "${posts.size} جديد",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = ColorProvider(color)
                )
            )
        }

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(2.dp)
                .background(ColorProvider(color))
        ) {}

        if (posts.isEmpty()) {
            WidgetEmptyState("لا يوجد محتوى من $name")
        } else {
            posts.forEach { post ->
                WidgetPostItem(post = post)
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ColorProvider(Color(0xFFF5F5F5)))
                ) {}
            }
        }
    }
}
