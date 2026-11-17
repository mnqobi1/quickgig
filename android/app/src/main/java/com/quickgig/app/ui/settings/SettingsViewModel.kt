package com.quickgig.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickgig.app.data.model.SupportedLanguage
import com.quickgig.app.data.repository.AuthRepository
import com.quickgig.app.data.repository.UserRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SettingsUiState(
    val language: SupportedLanguage = SupportedLanguage.ENGLISH,
    val notificationsEnabled: Boolean = true,
    val shareExactLocation: Boolean = true,
    val languageSaved: Boolean = false,
    val loggedOut: Boolean = false,
)

/** Settings screen: "language toggle, notification preferences,
 * payment/credit management, privacy, and logout" from the Planning doc. */
class SettingsViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val userId: Long,
) : ViewModel() {

    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        // Open with the chip for the language that's actually active, not always English.
        userRepository.preferredLanguage
            .onEach { code ->
                SupportedLanguage.values().firstOrNull { it.code == code }?.let {
                    uiState = uiState.copy(language = it)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onLanguageSelected(language: SupportedLanguage) {
        uiState = uiState.copy(language = language)
        viewModelScope.launch {
            // Switches the UI immediately (saved on-device first); the server copy is best-effort.
            userRepository.updateSettings(userId, preferredLanguage = language.code)
            uiState = uiState.copy(languageSaved = true)
        }
    }

    fun onNotificationsToggled(enabled: Boolean) { uiState = uiState.copy(notificationsEnabled = enabled) }
    fun onShareExactLocationToggled(enabled: Boolean) { uiState = uiState.copy(shareExactLocation = enabled) }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            uiState = uiState.copy(loggedOut = true)
        }
    }
}
