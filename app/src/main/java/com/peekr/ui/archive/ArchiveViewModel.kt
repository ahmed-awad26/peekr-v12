package com.peekr.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.local.entities.ArchiveEntity
import com.peekr.data.local.entities.CategoryEntity
import com.peekr.data.local.entities.PostEntity
import com.peekr.data.repository.ArchiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchiveUiState(
    val archives: List<ArchiveEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val archiveCount: Int = 0,
    val lastSavedId: Long? = null
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    init {
        loadArchives()
        loadCategories()
        observeCount()
    }

    private fun loadArchives() {
        viewModelScope.launch {
            archiveRepository.getAllArchives().collect { archives ->
                _uiState.update { it.copy(archives = archives) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            archiveRepository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    private fun observeCount() {
        viewModelScope.launch {
            archiveRepository.getArchiveCount().collect { count ->
                _uiState.update { it.copy(archiveCount = count) }
            }
        }
    }

    fun savePost(post: PostEntity, categoryId: Long? = null, note: String = "") {
        viewModelScope.launch {
            val id = archiveRepository.savePost(
                post = post,
                categoryId = categoryId,
                note = note.ifEmpty { null }
            )
            _uiState.update { it.copy(lastSavedId = id) }
        }
    }

    fun filterByCategory(categoryId: Long?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        viewModelScope.launch {
            val flow = if (categoryId == null) archiveRepository.getAllArchives()
            else archiveRepository.getArchivesByCategory(categoryId)
            flow.collect { archives ->
                _uiState.update { it.copy(archives = archives) }
            }
        }
    }

    fun deleteArchive(archive: ArchiveEntity) {
        viewModelScope.launch { archiveRepository.deleteArchive(archive) }
    }

    fun updateArchiveCategory(archive: ArchiveEntity, categoryId: Long?) {
        viewModelScope.launch { archiveRepository.updateArchive(archive.copy(categoryId = categoryId)) }
    }

    fun updateArchiveNote(archive: ArchiveEntity, note: String) {
        viewModelScope.launch { archiveRepository.updateArchive(archive.copy(note = note)) }
    }
}
