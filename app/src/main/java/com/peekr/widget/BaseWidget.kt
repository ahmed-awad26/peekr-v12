package com.peekr.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.peekr.MainActivity
import com.peekr.data.local.entities.PostEntity
import java.text.SimpleDateFormat
import java.util.*

// ألوان المنصات
fun platformColor(platformId: String): Color = when (platformId) {
    "youtube" -> Color(0xFFFF0000)
    "telegram" -> Color(0xFF0088CC)
    "whatsapp" -> Color(0xFF25D366)
    "facebook" -> Color(0xFF1877F2)
    "rss" -> Color(0xFFFF6600)
    else -> Color(0xFF6200EE)
}

fun platformName(platformId: String): String = when (platformId) {
    "youtube" -> "يوتيوب"
    "telegram" -> "تليجرام"
    "whatsapp" -> "واتساب"
    "facebook" -> "فيسبوك"
    "rss" -> "RSS"
    else -> platformId
}

fun formatWidgetTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 3_600_000 -> "${diff / 60_000} د"
        diff < 86_400_000 -> "${diff / 3_600_000} س"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}

// ==============================
// مكون بوست واحد في الويدجيت
// ==============================
@Composable
fun WidgetPostItem(post: PostEntity) {
    val color = platformColor(post.platformId)

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // بادج المنصة
            Box(
                modifier = GlanceModifier
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = platformName(post.platformId),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(color),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = formatWidgetTime(post.timestamp),
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(Color(0xFF999999))
                )
            )
        }
        Text(
            text = post.content.take(80),
            style = TextStyle(
                fontSize = 13.sp,
                color = ColorProvider(Color(0xFF1C1B1F))
            ),
            maxLines = 2
        )
    }
}

// ==============================
// هيدر الويدجيت
// ==============================
@Composable
fun WidgetHeader(title: String, subtitle: String = "") {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color(0xFF5B4FCF))
                )
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(Color(0xFF999999))
                    )
                )
            }
        }
    }
}

// ==============================
// حالة فارغة
// ==============================
@Composable
fun WidgetEmptyState(message: String = "لا يوجد محتوى") {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(
                fontSize = 13.sp,
                color = ColorProvider(Color(0xFF999999))
            )
        )
    }
}
