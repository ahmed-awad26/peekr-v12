package com.peekr.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.local.entities.CategoryEntity
import com.peekr.data.repository.ArchiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<CategoryEntity> = emptyList()
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            archiveRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun addCategory(name: String, color: String) {
        viewModelScope.launch {
            archiveRepository.addCategory(name, color)
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            archiveRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            archiveRepository.deleteCategory(category)
        }
    }
}
