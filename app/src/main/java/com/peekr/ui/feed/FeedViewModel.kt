package com.peekr.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.local.entities.PostEntity
import com.peekr.data.repository.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val posts: List<PostEntity> = emptyList(),
    val groupedPosts: Map<String, List<PostEntity>> = emptyMap(), // channelName → posts
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val selectedPlatform: String = "all",
    val error: String? = null,
    val syncMessage: String? = null,
    val unreadCount: Int = 0
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // Platform filter as a StateFlow — drives reactive post loading
    private val _selectedPlatform = MutableStateFlow("all")

    init {
        // React to platform changes automatically — no race conditions
        viewModelScope.launch {
            _selectedPlatform.flatMapLatest { platform ->
                _uiState.update { it.copy(isLoading = true, selectedPlatform = platform) }
                if (platform == "all") feedRepository.getAllPosts()
                else feedRepository.getPostsByPlatform(platform)
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }.collect { posts ->
                val grouped = if (posts.isNotEmpty())
                    posts.groupBy { it.sourceName }.toSortedMap()
                else emptyMap()
                _uiState.update { it.copy(posts = posts, groupedPosts = grouped, isLoading = false) }
            }
        }

        viewModelScope.launch {
            feedRepository.getUnreadCount().collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    fun selectPlatform(platformId: String) {
        _selectedPlatform.value = platformId
    }

    fun syncAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null, syncMessage = null) }
            try {
                val results = feedRepository.syncAll()

                // بناء رسالة النتائج
                val parts = mutableListOf<String>()
                var totalNew = 0
                var hasError = false

                results.forEach { (platform, result) ->
                    val label = when (platform) {
                        "youtube"  -> "يوتيوب"
                        "rss"      -> "RSS"
                        "telegram" -> "تليجرام"
                        "facebook" -> "فيسبوك"
                        else       -> platform
                    }
                    result.onSuccess { count ->
                        if (count > 0) {
                            parts.add("$label: $count جديد")
                            totalNew += count
                        }
                    }
                    result.onFailure { e ->
                        hasError = true
                        parts.add("$label: ${e.message?.take(60)}")
                    }
                }

                val msg = when {
                    parts.isEmpty() && !hasError -> "لا يوجد محتوى جديد"
                    parts.isEmpty() && hasError -> "فشل التحديث — راجع الإعدادات"
                    else -> parts.joinToString(" · ")
                }

                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncMessage = msg,
                        error = if (hasError && totalNew == 0) msg else null
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        error = e.message ?: "خطأ غير معروف"
                    )
                }
            }
        }
    }

    fun markAsRead(postId: Long) {
        viewModelScope.launch { feedRepository.markAsRead(postId) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, syncMessage = null) }
    }
}
