package com.quickgig.app.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "quickgig_session")

/**
 * Holds the signed-in user's JWT and id, plus the app's UI language.
 *
 * The Planning doc's login sequence: "the app stores that token securely
 * and attaches it to every subsequent API call's header" — this is where
 * it's stored; AuthInterceptor is where it gets attached.
 *
 * The language is deliberately separate from "who is signed in": it is a
 * device-level preference (the login screen's EN/ZU/AF chips set it before
 * anyone has signed in), so signing in or out never changes it.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = longPreferencesKey("user_id")
        val LANGUAGE = stringPreferencesKey("preferred_language")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }
    val userIdFlow: Flow<Long?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }

    suspend fun save(token: String, userId: Long) {
        context.dataStore.edit {
            it[Keys.TOKEN] = token
            it[Keys.USER_ID] = userId
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    /** Sign out: forget who the user is, but keep the chosen language. */
    suspend fun clear() {
        context.dataStore.edit {
            it.remove(Keys.TOKEN)
            it.remove(Keys.USER_ID)
        }
    }

    fun currentTokenBlocking(): String? = runBlocking { tokenFlow.first() }
}
