package com.quickgig.app.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickgig.app.R
import com.quickgig.app.data.model.SupportedLanguage
import com.quickgig.app.data.repository.AuthRepository
import com.quickgig.app.data.repository.AuthResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class LoginUiState(
    val mode: Mode = Mode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val language: SupportedLanguage = SupportedLanguage.ENGLISH,
    val isLoading: Boolean = false,
    /** A raw message from the server or network layer (not translatable). */
    val error: String? = null,
    /** A client-side validation message, kept as a string resource so it follows the app language. */
    val errorRes: Int? = null,
    val signedIn: Boolean = false,
) {
    enum class Mode { LOGIN, REGISTER }
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    init {
        // Keep the highlighted EN/ZU/AF chip in step with the saved app language.
        authRepository.language
            .onEach { code ->
                SupportedLanguage.values().firstOrNull { it.code == code }?.let {
                    uiState = uiState.copy(language = it)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onNameChange(value: String) { uiState = uiState.copy(name = value) }
    fun onEmailChange(value: String) { uiState = uiState.copy(email = value) }
    fun onPasswordChange(value: String) { uiState = uiState.copy(password = value) }

    /** Saving the language is what actually switches the UI — see ProvideAppLanguage. */
    fun onLanguageSelected(language: SupportedLanguage) {
        uiState = uiState.copy(language = language)
        viewModelScope.launch { authRepository.setLanguage(language.code) }
    }

    fun toggleMode() {
        uiState = uiState.copy(
            mode = if (uiState.mode == LoginUiState.Mode.LOGIN) LoginUiState.Mode.REGISTER else LoginUiState.Mode.LOGIN,
            error = null,
            errorRes = null,
        )
    }

    fun submit() {
        val state = uiState
        if (state.email.isBlank() || state.password.isBlank() || (state.mode == LoginUiState.Mode.REGISTER && state.name.isBlank())) {
            uiState = state.copy(error = null, errorRes = R.string.login_fill_all_fields)
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, errorRes = null)
            val result = if (state.mode == LoginUiState.Mode.LOGIN) {
                authRepository.login(state.email, state.password)
            } else {
                authRepository.register(state.name, state.email, state.password, state.language.code)
            }
            uiState = when (result) {
                is AuthResult.Success -> uiState.copy(isLoading = false, signedIn = true)
                is AuthResult.Error -> uiState.copy(isLoading = false, error = result.message)
            }
        }
    }

    /** Called with the (name, email) decoded from Credential Manager / Google
     * Identity Services once that's wired up with a real OAuth client ID —
     * see the SSO note in api/README.md. */
    fun continueWithGoogle(name: String, email: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, errorRes = null)
            uiState = when (val result = authRepository.loginWithGoogle(name, email)) {
                is AuthResult.Success -> uiState.copy(isLoading = false, signedIn = true)
                is AuthResult.Error -> uiState.copy(isLoading = false, error = result.message)
            }
        }
    }
}
