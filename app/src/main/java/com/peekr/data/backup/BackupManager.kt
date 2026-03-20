package com.peekr.data.backup

import android.content.Context
import android.net.Uri
import com.peekr.core.logger.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: AppLogger
) {
    private val dbName = "peekr_database"  // ← نفس الاسم في AppModule

    // ==============================
    // تصدير نسخة احتياطية
    // ==============================
    suspend fun exportBackup(outputUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            // وقف الـ DB مؤقتاً
            val dbFile = context.getDatabasePath(dbName)
            val dbWalFile = File(dbFile.path + "-wal")
            val dbShmFile = File(dbFile.path + "-shm")

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val backupName = "peekr_backup_$timestamp.zip"

            context.contentResolver.openOutputStream(outputUri)?.use { outStream ->
                ZipOutputStream(BufferedOutputStream(outStream)).use { zip ->

                    // اضف ملف DB
                    if (dbFile.exists()) {
                        zip.putNextEntry(ZipEntry("peekr_db"))
                        dbFile.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }

                    // اضف WAL لو موجود
                    if (dbWalFile.exists()) {
                        zip.putNextEntry(ZipEntry("peekr_db-wal"))
                        dbWalFile.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }

                    // اضف SHM لو موجود
                    if (dbShmFile.exists()) {
                        zip.putNextEntry(ZipEntry("peekr_db-shm"))
                        dbShmFile.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }

                    // اضف ملف metadata
                    val metadata = """
                        {
                            "version": 1,
                            "timestamp": "${System.currentTimeMillis()}",
                            "app": "Peekr",
                            "date": "$timestamp"
                        }
                    """.trimIndent()
                    zip.putNextEntry(ZipEntry("metadata.json"))
                    zip.write(metadata.toByteArray())
                    zip.closeEntry()
                }
            } ?: return@withContext Result.failure(Exception("مش قادر يفتح ملف الإخراج"))

            logger.info("تم تصدير نسخة احتياطية: $backupName", null)
            Result.success(backupName)
        } catch (e: Exception) {
            logger.error("فشل تصدير النسخة الاحتياطية", null, e)
            Result.failure(e)
        }
    }

    // ==============================
    // استيراد نسخة احتياطية
    // ==============================
    suspend fun importBackup(inputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(dbName)
            val dbDir = dbFile.parentFile ?: return@withContext Result.failure(
                Exception("مش قادر يلاقي مجلد قاعدة البيانات")
            )

            context.contentResolver.openInputStream(inputUri)?.use { inStream ->
                ZipInputStream(BufferedInputStream(inStream)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val outFile = File(dbDir, entry.name)
                        outFile.outputStream().use { zip.copyTo(it) }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return@withContext Result.failure(Exception("مش قادر يفتح ملف الاستيراد"))

            logger.info("تم استيراد النسخة الاحتياطية بنجاح", null)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("فشل استيراد النسخة الاحتياطية", null, e)
            Result.failure(e)
        }
    }

    // ==============================
    // حجم قاعدة البيانات
    // ==============================
    fun getDatabaseSize(): String {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return "0 KB"
        val sizeBytes = dbFile.length()
        return when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> "${sizeBytes / (1024 * 1024)} MB"
        }
    }
}
