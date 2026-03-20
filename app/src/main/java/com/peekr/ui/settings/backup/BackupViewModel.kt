package com.peekr.ui.settings.backup

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.peekr.data.backup.BackupManager
import com.peekr.data.backup.DriveBackupItem
import com.peekr.data.backup.DriveBackupManager
import com.peekr.data.backup.DriveState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val dbSize: String = "0 KB",
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val driveBackups: List<DriveBackupItem> = emptyList(),
    val isLoadingBackups: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val driveBackupManager: DriveBackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    val driveState: StateFlow<DriveState> = driveBackupManager.state

    init {
        _uiState.update { it.copy(dbSize = backupManager.getDatabaseSize()) }
        driveBackupManager.checkExistingSignIn()
    }

    // ==============================
    // تصدير محلي
    // ==============================
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            val result = backupManager.exportBackup(uri)
            _uiState.update {
                it.copy(
                    isExporting = false,
                    successMessage = if (result.isSuccess) "✅ تم تصدير النسخة الاحتياطية بنجاح"
                    else null,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    // ==============================
    // استيراد محلي
    // ==============================
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null) }
            val result = backupManager.importBackup(uri)
            _uiState.update {
                it.copy(
                    isImporting = false,
                    successMessage = if (result.isSuccess) "✅ تم استيراد النسخة الاحتياطية. أعد تشغيل التطبيق."
                    else null,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    // ==============================
    // جوجل درايف
    // ==============================
    fun getSignInIntent(): Intent = driveBackupManager.getSignInIntent()

    fun handleSignInResult(result: ActivityResult) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                driveBackupManager.connectAccount(account)
            } catch (e: ApiException) {
                _uiState.update { it.copy(errorMessage = "فشل تسجيل الدخول: ${e.message}") }
            }
        }
    }

    fun disconnectDrive() {
        driveBackupManager.disconnect()
    }

    fun uploadToDrive() {
        viewModelScope.launch {
            val result = driveBackupManager.uploadBackup()
            _uiState.update {
                it.copy(
                    successMessage = if (result.isSuccess) "✅ تم الرفع على درايف: ${result.getOrNull()}"
                    else null,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
            if (result.isSuccess) loadDriveBackups()
        }
    }

    fun loadDriveBackups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBackups = true) }
            val result = driveBackupManager.listBackups()
            _uiState.update {
                it.copy(
                    isLoadingBackups = false,
                    driveBackups = result.getOrNull() ?: emptyList()
                )
            }
        }
    }

    fun restoreFromDrive(fileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            val result = driveBackupManager.downloadAndRestore(fileId)
            _uiState.update {
                it.copy(
                    isImporting = false,
                    successMessage = if (result.isSuccess) "✅ تم الاستعادة. أعد تشغيل التطبيق." else null,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
