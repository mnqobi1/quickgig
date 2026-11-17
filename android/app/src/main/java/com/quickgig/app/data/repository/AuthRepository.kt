package com.quickgig.app.data.repository

import com.quickgig.app.data.model.AuthResponse
import com.quickgig.app.data.model.User
import com.quickgig.app.data.remote.GoogleSsoRequest
import com.quickgig.app.data.remote.LoginRequest
import com.quickgig.app.data.remote.QuickGigApi
import com.quickgig.app.data.remote.RegisterRequest
import com.quickgig.app.data.remote.SessionManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val api: QuickGigApi,
    private val sessionManager: SessionManager,
) {
    suspend fun register(name: String, email: String, password: String, preferredLanguage: String): AuthResult =
        runCatching { api.register(RegisterRequest(name, email, password, preferredLanguage)) }
            .fold(onSuccess = { handleAuthResponse(it) }, onFailure = { networkError(it) })

    suspend fun login(email: String, password: String): AuthResult =
        runCatching { api.login(LoginRequest(email, password)) }
            .fold(onSuccess = { handleAuthResponse(it) }, onFailure = { networkError(it) })

    suspend fun loginWithGoogle(name: String, email: String): AuthResult =
        runCatching { api.loginWithGoogle(GoogleSsoRequest(email, name)) }
            .fold(onSuccess = { handleAuthResponse(it) }, onFailure = { networkError(it) })

    suspend fun logout() = sessionManager.clear()

    val isSignedIn get() = sessionManager.tokenFlow

    /** The app's UI language. A device-level preference: the login screen's
     * EN/ZU/AF chips set it before anyone has signed in. */
    val language get() = sessionManager.languageFlow

    suspend fun setLanguage(code: String) = sessionManager.setLanguage(code)

    private suspend fun handleAuthResponse(response: Response<AuthResponse>): AuthResult {
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            // Only the token and user id are stored here. The language is left alone
            // on purpose, so picking ZU and then signing in doesn't snap back to the
            // account's stored language.
            sessionManager.save(body.token, body.user.id)
            AuthResult.Success(body.user)
        } else {
            AuthResult.Error(readServerError(response.errorBody()?.string(), response.code()))
        }
    }

    /** The API replies to errors with {"error":"..."} — show the message, not the JSON. */
    private fun readServerError(raw: String?, httpCode: Int): String {
        if (raw.isNullOrBlank()) return "Something went wrong (HTTP $httpCode)"
        return runCatching { Json.parseToJsonElement(raw).jsonObject["error"]?.jsonPrimitive?.content }
            .getOrNull() ?: raw
    }

    private fun networkError(t: Throwable): AuthResult =
        AuthResult.Error(t.message ?: "Couldn't reach the server — check your connection")
}
