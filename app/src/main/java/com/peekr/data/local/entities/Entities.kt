package com.peekr.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// ==============================
// جدول المنشورات
// ==============================
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platformId: String,        // telegram, youtube, whatsapp, facebook, rss
    val sourceId: String,          // معرف المصدر (قناة، صفحة، إلخ)
    val sourceName: String,        // اسم المصدر
    val content: String,           // محتوى المنشور
    val mediaUrl: String? = null,  // رابط الصورة أو الفيديو
    val postUrl: String? = null,   // رابط المنشور الأصلي
    val timestamp: Long,           // وقت النشر
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// ==============================
// جدول الأرشيف
// ==============================
@Entity(tableName = "archives")
data class ArchiveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,              // مرجع للبوست
    val platformId: String,
    val sourceName: String,
    val content: String,
    val mediaUrl: String? = null,
    val postUrl: String? = null,
    val categoryId: Long? = null,  // التصنيف
    val note: String? = null,      // ملاحظة شخصية
    val savedAt: Long = System.currentTimeMillis()
)

// ==============================
// جدول التصنيفات
// ==============================
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String = "#6200EE",  // لون التصنيف
    val createdAt: Long = System.currentTimeMillis()
)

// ==============================
// جدول الحسابات
// ==============================
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platformId: String,         // telegram, youtube, whatsapp, facebook, rss
    val accountName: String,        // اسم الحساب
    val isConnected: Boolean = false,
    val connectedAt: Long? = null,
    val extraData: String? = null   // بيانات إضافية (JSON)
)

// ==============================
// جدول مفاتيح API
// ==============================
@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey val platformId: String,  // telegram, youtube, facebook
    val keyName: String,                 // اسم المفتاح
    val keyValue: String,                // القيمة (مشفرة)
    val updatedAt: Long = System.currentTimeMillis()
)

// ==============================
// جدول الأدوات
// ==============================
@Entity(tableName = "tools")
data class ToolEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val folderPath: String,          // مسار ملفات الأداة
    val iconPath: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

// ==============================
// جدول الـ Logs
// ==============================
@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val level: String,               // ERROR, WARNING, INFO
    val platformId: String? = null,  // المنصة اللي حصلت فيها المشكلة
    val message: String,             // رسالة الخطأ
    val stackTrace: String? = null,  // تفاصيل الخطأ
    val timestamp: Long = System.currentTimeMillis()
)
