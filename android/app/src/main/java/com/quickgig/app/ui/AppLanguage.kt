package com.quickgig.app.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * User Defined Feature 3 (multi-language): makes every `stringResource()`
 * call underneath resolve against the language the user picked in the app
 * ("en", "zu" or "af") rather than the phone's own language.
 *
 * Without this, res/values-zu and res/values-af only ever show up if the
 * whole phone is set to isiZulu/Afrikaans — the in-app chips would just
 * store a value that nothing reads.
 *
 * It wraps the Activity context instead of replacing it, so anything that
 * needs to find the Activity by walking up the context chain (like the
 * system back button handling in Navigation) keeps working.
 */
@Composable
fun ProvideAppLanguage(languageCode: String, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val localizedContext = remember(baseContext, languageCode) {
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(Locale.forLanguageTag(languageCode))
        LocalizedContext(baseContext, baseContext.createConfigurationContext(config).resources)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        content = content,
    )
}

private class LocalizedContext(
    base: Context,
    private val localizedResources: Resources,
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
}
