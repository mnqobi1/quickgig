package com.quickgig.app.ui.login

import android.app.Activity
import android.util.Log
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
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

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

private const val TAG = "LoginViewModel"

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    init {
        // Keep the highlighted EN/ZU/AF chip in step with the saved app language.
        Log.d(TAG, "LoginViewModel initialized")
        authRepository.language
            .onEach { code ->
                Log.d(TAG, "Language updated: $code")
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
        // Validate that all required fields are filled before proceeding
        if (state.email.isBlank() || state.password.isBlank() || (state.mode == LoginUiState.Mode.REGISTER && state.name.isBlank())) {
            Log.w(TAG, "Submit validation failed: missing required fields")
            uiState = state.copy(error = null, errorRes = R.string.login_fill_all_fields)
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "Starting ${if (state.mode == LoginUiState.Mode.LOGIN) "login" else "registration"} request")
            uiState = uiState.copy(isLoading = true, error = null, errorRes = null)
            val result = if (state.mode == LoginUiState.Mode.LOGIN) {
                authRepository.login(state.email, state.password)
            } else {
                authRepository.register(state.name, state.email, state.password, state.language.code)
            }
            when (result) {
                is AuthResult.Success -> {
                    Log.i(TAG, "Authentication successful for user: ${result.user.id}")
                    uiState = uiState.copy(isLoading = false, signedIn = true)
                }
                is AuthResult.Error -> {
                    Log.e(TAG, "Authentication failed: ${result.message}")
                    uiState = uiState.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    /** Called with the ID token from Credential Manager / Google Identity Services.
     * The API verifies the token and issues a QuickGig JWT. */
    fun continueWithGoogle(idToken: String) {
        viewModelScope.launch {
            Log.d(TAG, "Sending Google ID token to API for verification")
            uiState = uiState.copy(isLoading = true, error = null, errorRes = null)
            when (val result = authRepository.loginWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    Log.i(TAG, "Google Sign-In successful for user: ${result.user.id}")
                    uiState = uiState.copy(isLoading = false, signedIn = true)
                }
                is AuthResult.Error -> {
                    Log.e(TAG, "Google Sign-In failed: ${result.message}")
                    uiState = uiState.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    /** Prompt the user to select a Google account via Credential Manager
     * and call continueWithGoogle with the resulting idToken. */
    fun continueWithGooglePressed(activity: Activity) {
        viewModelScope.launch {
            Log.d(TAG, "Launching Credential Manager for Google Sign-In")
            uiState = uiState.copy(isLoading = true, error = null, errorRes = null)
            try {
                // Configure Google Sign-In with server client ID for ID token verification
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("221638273539-eohsnmhd48mfqfq21atoglcqbvhr9en6.apps.googleusercontent.com")
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = CredentialManager.create(activity)
                    .getCredential(activity, request)

                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    val idToken = credential.idToken
                    Log.d(TAG, "Google ID token received, length: ${idToken.length}")
                    continueWithGoogle(idToken)
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credential::class.simpleName}")
                    uiState = uiState.copy(isLoading = false, error = "Unexpected credential type")
                }
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Google sign-in cancelled or failed", e)
                uiState = uiState.copy(isLoading = false, error = "Google sign-in cancelled or failed: ${e.message}")
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Invalid Google ID token", e)
                uiState = uiState.copy(isLoading = false, error = "Invalid Google ID token: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in failed with unexpected error", e)
                uiState = uiState.copy(isLoading = false, error = "Google sign-in failed: ${e.message}")
            }
        }
    }
}
