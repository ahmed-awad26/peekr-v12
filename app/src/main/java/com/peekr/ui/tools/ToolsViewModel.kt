package com.peekr.ui.tools

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.local.entities.ToolEntity
import com.peekr.data.repository.ToolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolsUiState(
    val tools: List<ToolEntity> = emptyList(),
    val isImporting: Boolean = false,
    val importError: String? = null,
    val importSuccess: String? = null
)

@HiltViewModel
class ToolsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val toolRepository: ToolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ToolsUiState())
    val uiState: StateFlow<ToolsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            toolRepository.getAllTools().collect { tools ->
                _uiState.update { it.copy(tools = tools) }
            }
        }
    }

    fun importToolFromUri(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importError = null, importSuccess = null) }
            try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("مش قادر يفتح الملف")

                val result = toolRepository.importTool(stream, fileName)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importSuccess = "تم إضافة الأداة \"${result.getOrNull()?.name}\" بنجاح ✅"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importError = result.exceptionOrNull()?.message ?: "فشل الاستيراد"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isImporting = false, importError = e.message ?: "خطأ غير معروف")
                }
            }
        }
    }

    fun deleteTool(tool: ToolEntity) {
        viewModelScope.launch {
            toolRepository.deleteTool(tool)
        }
    }

    fun getPopupPath(tool: ToolEntity): String? {
        return toolRepository.getPopupHtmlPath(tool)
    }

    fun clearMessages() {
        _uiState.update { it.copy(importError = null, importSuccess = null) }
    }
}
