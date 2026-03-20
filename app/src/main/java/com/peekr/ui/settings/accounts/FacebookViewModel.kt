package com.peekr.ui.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.local.dao.AccountDao
import com.peekr.data.local.entities.AccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FacebookUiState(
    val pages: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FacebookViewModel @Inject constructor(
    private val accountDao: AccountDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(FacebookUiState())
    val uiState: StateFlow<FacebookUiState> = _uiState.asStateFlow()

    init {
        loadSavedPages()
    }

    private fun loadSavedPages() {
        viewModelScope.launch {
            val account = accountDao.getAccountByPlatform("facebook")
            if (account != null && !account.extraData.isNullOrEmpty()) {
                val pages = account.extraData.split(",").filter { it.isNotEmpty() }
                _uiState.update { it.copy(pages = pages) }
            }
        }
    }

    fun addPage(pageId: String) {
        if (!_uiState.value.pages.contains(pageId)) {
            _uiState.update { it.copy(pages = it.pages + pageId, error = null) }
        }
    }

    fun removePage(pageId: String) {
        _uiState.update { it.copy(pages = it.pages - pageId) }
    }

    fun savePages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val pagesData = _uiState.value.pages.joinToString(",")
                accountDao.insertAccount(
                    AccountEntity(
                        platformId = "facebook",
                        accountName = "فيسبوك",
                        isConnected = _uiState.value.pages.isNotEmpty(),
                        connectedAt = System.currentTimeMillis(),
                        extraData = pagesData
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
