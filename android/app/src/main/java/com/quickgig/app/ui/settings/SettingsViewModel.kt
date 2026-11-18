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
    val name: String = "",
    val language: SupportedLanguage = SupportedLanguage.ENGLISH,
    val phone: String? = null,
    val campusArea: String? = null,
    val photoUrl: String? = null,
    val notificationsEnabled: Boolean = true,
    val shareExactLocation: Boolean = true,
    val languageSaved: Boolean = false,
    val settingsSaved: Boolean = false,
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

        // Load the current profile so the form pre-fills with what the server knows.
        viewModelScope.launch {
            val result = userRepository.getProfile(userId)
            if (result.isSuccess) {
                val p = result.getOrNull()!!
                uiState = uiState.copy(
                    name = p.name,
                    phone = p.phone,
                    campusArea = p.campusArea,
                    photoUrl = p.photoUrl,
                    notificationsEnabled = p.notificationsEnabled,
                )
            }
        }
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

    fun onSaveSettings(
        name: String?,
        phone: String?,
        campusArea: String?,
        photoUrl: String?,
    ) {
        viewModelScope.launch {
            val result = userRepository.updateSettings(
                userId = userId,
                name = name,
                phone = phone,
                preferredLanguage = uiState.language.code,
                campusArea = campusArea,
                photoUrl = photoUrl,
                notificationsEnabled = uiState.notificationsEnabled,
            )
            if (result.isSuccess) {
                uiState = uiState.copy(settingsSaved = true)
                uiState = uiState.copy(
                    name = name ?: "",
                    phone = phone,
                    campusArea = campusArea,
                    photoUrl = photoUrl,
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            uiState = uiState.copy(loggedOut = true)
        }
    }
}
