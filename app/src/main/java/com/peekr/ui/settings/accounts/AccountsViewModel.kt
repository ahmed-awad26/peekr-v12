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
class AccountsViewModel @Inject constructor(
    private val accountDao: AccountDao
) : ViewModel() {

    // كل الحسابات المحفوظة في DB
    val allAccounts = accountDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // فصل حساب بالكامل (يحذف كل روابطه من DB)
    fun disconnectPlatform(platformId: String) {
        viewModelScope.launch {
            accountDao.deleteAccountByPlatform(platformId)
        }
    }

    // هل المنصة دي متصلة؟
    fun isConnected(platformId: String, accounts: List<AccountEntity>): Boolean {
        return accounts.any { it.platformId == platformId && it.isConnected }
    }

    // كام رابط مضاف لمنصة معينة
    fun getCount(platformId: String, accounts: List<AccountEntity>): Int {
        return accounts.count { it.platformId == platformId }
    }
}
