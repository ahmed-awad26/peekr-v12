package com.peekr.core.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val ONBOARDING_DONE  = booleanPreferencesKey("onboarding_done")
        val IS_DARK_MODE     = booleanPreferencesKey("is_dark_mode")
        val USE_SYSTEM_THEME = booleanPreferencesKey("use_system_theme")
        val IS_ENGLISH       = booleanPreferencesKey("is_english")
    }

    // ==============================
    // Onboarding (shared مع OnboardingViewModel)
    // ==============================
    val isOnboardingDone: Flow<Boolean> = dataStore.data
        .map { it[ONBOARDING_DONE] ?: false }

    suspend fun markOnboardingDone() {
        dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    // ==============================
    // Theme
    // ==============================
    val isDarkMode: Flow<Boolean> = dataStore.data
        .map { it[IS_DARK_MODE] ?: false }

    val useSystemTheme: Flow<Boolean> = dataStore.data
        .map { it[USE_SYSTEM_THEME] ?: true }

    suspend fun setDarkMode(dark: Boolean) {
        dataStore.edit {
            it[IS_DARK_MODE] = dark
            it[USE_SYSTEM_THEME] = false
        }
    }

    suspend fun setUseSystemTheme(use: Boolean) {
        dataStore.edit { it[USE_SYSTEM_THEME] = use }
    }

    // ==============================
    // Language
    // ==============================
    val isEnglish: Flow<Boolean> = dataStore.data
        .map { it[IS_ENGLISH] ?: false }

    suspend fun setEnglish(english: Boolean) {
        dataStore.edit { it[IS_ENGLISH] = english }
    }
}
