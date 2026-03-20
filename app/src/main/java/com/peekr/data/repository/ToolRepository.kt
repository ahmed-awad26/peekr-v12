package com.peekr.data.repository

import android.content.Context
import com.peekr.core.logger.AppLogger
import com.peekr.data.local.dao.ToolDao
import com.peekr.data.local.entities.ToolEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val toolDao: ToolDao,
    private val logger: AppLogger
) {
    private val toolsDir = File(context.filesDir, "tools")

    init {
        if (!toolsDir.exists()) toolsDir.mkdirs()
    }

    fun getAllTools(): Flow<List<ToolEntity>> = toolDao.getAllTools()

    // ==============================
    // استيراد أداة من ZIP
    // ==============================
    suspend fun importTool(zipStream: InputStream, fileName: String): Result<ToolEntity> =
        withContext(Dispatchers.IO) {
            try {
                val toolName = fileName.removeSuffix(".zip")
                val toolFolder = File(toolsDir, toolName)

                // لو موجود امسحه وابدأ من أول
                if (toolFolder.exists()) toolFolder.deleteRecursively()
                toolFolder.mkdirs()

                // فك ضغط الـ ZIP
                ZipInputStream(zipStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val file = File(toolFolder, entry.name)

                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            file.outputStream().use { out ->
                                zis.copyTo(out)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                // تأكد إن في popup.html
                val popupFile = findPopupHtml(toolFolder)
                if (popupFile == null) {
                    toolFolder.deleteRecursively()
                    return@withContext Result.failure(
                        Exception("ملف popup.html مش موجود في الـ ZIP")
                    )
                }

                // جيب الأيقونة لو موجودة
                val iconPath = findIcon(toolFolder)?.absolutePath

                // احفظ في الداتابيز
                val tool = ToolEntity(
                    name = toolName,
                    folderPath = toolFolder.absolutePath,
                    iconPath = iconPath
                )
                val id = toolDao.insertTool(tool)

                logger.info("تم إضافة أداة: $toolName", null)
                Result.success(tool.copy(id = id))
            } catch (e: Exception) {
                logger.error("فشل استيراد الأداة", null, e)
                Result.failure(e)
            }
        }

    // ==============================
    // جلب ملف popup.html
    // ==============================
    fun getPopupHtmlPath(tool: ToolEntity): String? {
        val folder = File(tool.folderPath)
        return findPopupHtml(folder)?.absolutePath
    }

    private fun findPopupHtml(folder: File): File? {
        // ابحث في المجلد الرئيسي والمجلدات الفرعية
        folder.walkTopDown().forEach { file ->
            if (file.name == "popup.html") return file
        }
        return null
    }

    private fun findIcon(folder: File): File? {
        val iconNames = listOf("icon.png", "icon128.png", "icon48.png", "logo.png")
        folder.walkTopDown().forEach { file ->
            if (file.name in iconNames) return file
        }
        return null
    }

    // ==============================
    // حذف أداة
    // ==============================
    suspend fun deleteTool(tool: ToolEntity) = withContext(Dispatchers.IO) {
        try {
            File(tool.folderPath).deleteRecursively()
            toolDao.deleteTool(tool)
            logger.info("تم حذف أداة: ${tool.name}", null)
        } catch (e: Exception) {
            logger.error("فشل حذف الأداة", null, e)
        }
    }
}
