package com.quickgig.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import com.quickgig.app.QuickGigApplication

/** Small generic factory so ViewModels can take real repositories in their
 * constructor instead of reaching for a singleton/service-locator inside
 * each ViewModel. */
class SimpleViewModelFactory(private val create: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}

@Composable
fun quickGigApp(): QuickGigApplication {
    val context = androidx.compose.ui.platform.LocalContext.current
    return context.applicationContext as QuickGigApplication
}

@Composable
inline fun <reified T : ViewModel> rememberViewModel(noinline create: () -> T): T =
    viewModel(factory = SimpleViewModelFactory(create))
