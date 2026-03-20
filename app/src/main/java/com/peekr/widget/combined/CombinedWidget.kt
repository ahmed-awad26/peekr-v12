package com.peekr.widget.combined

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
import com.peekr.widget.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CombinedWidgetEntryPoint {
    fun widgetRepository(): WidgetRepository
}

class CombinedWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CombinedWidgetEntryPoint::class.java
        )
        val posts = entryPoint.widgetRepository().getLatestPosts(limit = 5)

        provideContent {
            CombinedWidgetContent(posts = posts)
        }
    }
}

@Composable
fun CombinedWidgetContent(posts: List<com.peekr.data.local.entities.PostEntity>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
    ) {
        WidgetHeader(
            title = "Peekr",
            subtitle = "${posts.size} منشور جديد"
        )

        // فاصل
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ColorProvider(Color(0xFFEEEEEE)))
        ) {}

        if (posts.isEmpty()) {
            WidgetEmptyState()
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

class CombinedWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CombinedWidget()
}
