package com.peekr.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import com.peekr.core.logger.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class DriveState(
    val isConnected: Boolean = false,
    val accountEmail: String? = null,
    val lastBackupTime: String? = null,
    val isUploading: Boolean = false,
    val error: String? = null
)

@Singleton
class DriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val logger: AppLogger
) {
    private val _state = MutableStateFlow(DriveState())
    val state: StateFlow<DriveState> = _state

    private var driveService: Drive? = null

    companion object {
        const val FOLDER_NAME = "Peekr Backups"
        val REQUIRED_SCOPES = listOf(DriveScopes.DRIVE_FILE)
    }

    // ==============================
    // بناء Intent لتسجيل الدخول
    // ==============================
    fun getSignInIntent(): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    // ==============================
    // تفعيل الحساب بعد تسجيل الدخول
    // ==============================
    suspend fun connectAccount(account: GoogleSignInAccount) = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, REQUIRED_SCOPES
            )
            credential.selectedAccount = account.account

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Peekr")
                .build()

            _state.value = DriveState(
                isConnected = true,
                accountEmail = account.email
            )
            logger.info("جوجل درايف: تم الربط بـ ${account.email}", null)
        } catch (e: Exception) {
            logger.error("فشل ربط جوجل درايف", null, e)
            _state.value = DriveState(error = e.message)
        }
    }

    // ==============================
    // قطع الاتصال
    // ==============================
    fun disconnect() {
        driveService = null
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()
        _state.value = DriveState()
        logger.info("جوجل درايف: تم قطع الاتصال", null)
    }

    // ==============================
    // رفع نسخة احتياطية
    // ==============================
    suspend fun uploadBackup(): Result<String> = withContext(Dispatchers.IO) {
        val drive = driveService
            ?: return@withContext Result.failure(Exception("جوجل درايف مش مربوط"))

        try {
            _state.value = _state.value.copy(isUploading = true, error = null)

            // توليد النسخة في الذاكرة
            val baos = ByteArrayOutputStream()
            val dbFile = context.getDatabasePath("peekr_database")

            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("قاعدة البيانات مش موجودة"))
            }

            // إنشاء ZIP في الذاكرة
            val zipBytes = createBackupZip()

            // جيب أو أنشئ مجلد Peekr Backups
            val folderId = getOrCreateFolder(drive)

            // اسم الملف
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format(Date())
            val fileName = "peekr_backup_$timestamp.zip"

            // رفع الملف
            val metadata = DriveFile().apply {
                name = fileName
                parents = listOf(folderId)
                mimeType = "application/zip"
            }

            val content = ByteArrayContent("application/zip", zipBytes)
            val uploaded = drive.files().create(metadata, content)
                .setFields("id, name, size")
                .execute()

            val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            _state.value = _state.value.copy(
                isUploading = false,
                lastBackupTime = timeStr
            )

            logger.info("تم رفع النسخة الاحتياطية على درايف: $fileName", null)
            Result.success(fileName)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isUploading = false, error = e.message)
            logger.error("فشل رفع النسخة على درايف", null, e)
            Result.failure(e)
        }
    }

    // ==============================
    // جلب قائمة النسخ على درايف
    // ==============================
    suspend fun listBackups(): Result<List<DriveBackupItem>> = withContext(Dispatchers.IO) {
        val drive = driveService
            ?: return@withContext Result.failure(Exception("جوجل درايف مش مربوط"))

        try {
            val folderId = getOrCreateFolder(drive)
            val result = drive.files().list()
                .setQ("'$folderId' in parents and trashed=false")
                .setOrderBy("createdTime desc")
                .setFields("files(id,name,size,createdTime)")
                .execute()

            val items = result.files.map { file ->
                DriveBackupItem(
                    id = file.id,
                    name = file.name,
                    sizeBytes = file.getSize() ?: 0L,
                    createdTime = file.createdTime?.value ?: 0L
                )
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==============================
    // استعادة من درايف
    // ==============================
    suspend fun downloadAndRestore(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val drive = driveService
            ?: return@withContext Result.failure(Exception("جوجل درايف مش مربوط"))

        try {
            val baos = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(baos)

            // حفظ مؤقت واستيراد
            val tempFile = java.io.File(context.cacheDir, "restore_temp.zip")
            tempFile.writeBytes(baos.toByteArray())

            val uri = android.net.Uri.fromFile(tempFile)
            val result = backupManager.importBackup(uri)
            tempFile.delete()

            logger.info("تم استعادة النسخة الاحتياطية من درايف", null)
            result
        } catch (e: Exception) {
            logger.error("فشل استعادة النسخة من درايف", null, e)
            Result.failure(e)
        }
    }

    // ==============================
    // helpers
    // ==============================
    private fun createBackupZip(): ByteArray {
        val dbFile = context.getDatabasePath("peekr_database")
        val dbWalFile = java.io.File(dbFile.path + "-wal")
        val dbShmFile = java.io.File(dbFile.path + "-shm")
        val baos = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zip ->
            if (dbFile.exists()) {
                zip.putNextEntry(java.util.zip.ZipEntry("peekr_database"))
                dbFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            if (dbWalFile.exists()) {
                zip.putNextEntry(java.util.zip.ZipEntry("peekr_database-wal"))
                dbWalFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            if (dbShmFile.exists()) {
                zip.putNextEntry(java.util.zip.ZipEntry("peekr_database-shm"))
                dbShmFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    private fun getOrCreateFolder(drive: Drive): String {
        val existing = drive.files().list()
            .setQ("name='$FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false")
            .setFields("files(id)")
            .execute()

        if (existing.files.isNotEmpty()) return existing.files[0].id

        val folder = DriveFile().apply {
            name = FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }
        return drive.files().create(folder).setFields("id").execute().id
    }

    fun checkExistingSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(
                account, Scope(DriveScopes.DRIVE_FILE)
            )
        ) {
            val credential = GoogleAccountCredential.usingOAuth2(context, REQUIRED_SCOPES)
            credential.selectedAccount = account.account
            driveService = Drive.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
            ).setApplicationName("Peekr").build()
            _state.value = DriveState(isConnected = true, accountEmail = account.email)
        }
    }
}

data class DriveBackupItem(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdTime: Long
)
