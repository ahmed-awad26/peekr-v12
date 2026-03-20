package com.peekr.widget.custom

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.peekr.widget.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// مفاتيح الـ preferences
val SOURCE_ID_KEY = stringPreferencesKey("source_id")
val SOURCE_NAME_KEY = stringPreferencesKey("source_name")
val PLATFORM_ID_KEY = stringPreferencesKey("platform_id")

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CustomWidgetEntryPoint {
    fun widgetRepository(): WidgetRepository
}

class CustomSourceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> =
        PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val sourceId = prefs[SOURCE_ID_KEY] ?: ""
        val sourceName = prefs[SOURCE_NAME_KEY] ?: "مصدر مخصص"
        val platformId = prefs[PLATFORM_ID_KEY] ?: ""

        val repo = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CustomWidgetEntryPoint::class.java
        ).widgetRepository()

        val posts = if (sourceId.isNotEmpty()) {
            repo.getLatestPostsBySource(sourceId, 5)
        } else {
            emptyList()
        }

        provideContent {
            CustomWidgetContent(
                sourceName = sourceName,
                platformId = platformId,
                posts = posts,
                isConfigured = sourceId.isNotEmpty()
            )
        }
    }
}

@Composable
fun CustomWidgetContent(
    sourceName: String,
    platformId: String,
    posts: List<com.peekr.data.local.entities.PostEntity>,
    isConfigured: Boolean
) {
    val color = if (platformId.isNotEmpty()) platformColor(platformId)
    else Color(0xFF6200EE)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
    ) {
        // هيدر
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(color.copy(alpha = 0.1f)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = sourceName,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(color)
                    )
                )
                if (platformId.isNotEmpty()) {
                    Text(
                        text = platformName(platformId),
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(color.copy(alpha = 0.7f))
                        )
                    )
                }
            }
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

        if (!isConfigured) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "افتح Peekr لإعداد الويدجيت",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(Color(0xFF999999))
                    )
                )
            }
        } else if (posts.isEmpty()) {
            WidgetEmptyState("لا يوجد محتوى من $sourceName")
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

class CustomSourceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CustomSourceWidget()
}
