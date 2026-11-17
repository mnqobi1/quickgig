package com.quickgig.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.quickgig.app.ui.ProvideAppLanguage
import com.quickgig.app.ui.nav.QuickGigNavHost
import com.quickgig.app.ui.theme.QuickGigTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as QuickGigApplication

        setContent {
            // null until the saved language has been read (a few milliseconds), so a
            // user who chose isiZulu never sees a flash of English on launch.
            val language by app.sessionManager.languageFlow.collectAsState(initial = null)

            language?.let { code ->
                ProvideAppLanguage(code) {
                    QuickGigTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            QuickGigNavHost()
                        }
                    }
                }
            }
        }
    }
}
