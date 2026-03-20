package com.peekr.ui.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.local.dao.AccountDao
import com.peekr.data.local.entities.AccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RssViewModel @Inject constructor(
    private val accountDao: AccountDao
) : ViewModel() {

    // الـ RSS feeds المحفوظة في DB
    val feeds = accountDao.getAllAccountsByPlatform("rss")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFeed(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            accountDao.insertAccount(
                AccountEntity(
                    platformId = "rss",
                    accountName = url.trim(),
                    isConnected = true,
                    connectedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun removeFeed(account: AccountEntity) {
        viewModelScope.launch {
            accountDao.deleteAccountById(account.id)
        }
    }

    fun updateFeed(account: AccountEntity, newUrl: String) {
        viewModelScope.launch {
            accountDao.insertAccount(account.copy(accountName = newUrl.trim()))
        }
    }
}
