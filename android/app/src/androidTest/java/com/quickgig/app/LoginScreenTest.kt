package com.quickgig.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickgig.app.data.repository.AuthRepository
import com.quickgig.app.ui.login.LoginScreen
import com.quickgig.app.ui.login.LoginViewModel
import com.quickgig.app.ui.theme.QuickGigTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI test for the Login screen (Espresso/Compose test runner —
 * needs a device or emulator, per the Part 1 plan's "Espresso covers key
 * flows such as login"). Exercises validation only; it doesn't hit the
 * real network, so it runs the same with or without the API running.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun submittingWithEmptyFields_showsValidationError() {
        val fakeAuthRepository = AuthRepository(
            api = FakeQuickGigApi(),
            sessionManager = com.quickgig.app.data.remote.SessionManager(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            ),
        )
        val viewModel = LoginViewModel(fakeAuthRepository)

        composeTestRule.setContent {
            QuickGigTheme { LoginScreen(viewModel = viewModel, onSignedIn = {}) }
        }

        composeTestRule.onNodeWithText("Log in").performClick()
        composeTestRule.onNodeWithText("Please fill in all fields").assertExists()
    }

    @Test
    fun typingIntoEmailField_updatesTheFieldValue() {
        val fakeAuthRepository = AuthRepository(
            api = FakeQuickGigApi(),
            sessionManager = com.quickgig.app.data.remote.SessionManager(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            ),
        )
        val viewModel = LoginViewModel(fakeAuthRepository)

        composeTestRule.setContent {
            QuickGigTheme { LoginScreen(viewModel = viewModel, onSignedIn = {}) }
        }

        composeTestRule.onNodeWithText("Email").performTextInput("student@example.com")
        composeTestRule.onNodeWithText("student@example.com").assertExists()
    }
}
