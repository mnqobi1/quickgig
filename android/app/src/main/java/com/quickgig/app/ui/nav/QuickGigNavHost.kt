package com.quickgig.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.app.Activity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.quickgig.app.ui.chat.ChatScreen
import com.quickgig.app.ui.chat.ChatViewModel
import com.quickgig.app.ui.gigdetail.GigDetailScreen
import com.quickgig.app.ui.gigdetail.GigDetailViewModel
import com.quickgig.app.ui.home.HomeFeedScreen
import com.quickgig.app.ui.home.HomeViewModel
import com.quickgig.app.ui.login.LoginScreen
import com.quickgig.app.ui.login.LoginViewModel
import com.quickgig.app.ui.mygigs.MyGigsScreen
import com.quickgig.app.ui.mygigs.MyGigsViewModel
import com.quickgig.app.ui.notifications.NotificationsScreen
import com.quickgig.app.ui.notifications.NotificationsViewModel
import com.quickgig.app.ui.postgig.PostGigScreen
import com.quickgig.app.ui.postgig.PostGigViewModel
import com.quickgig.app.ui.profile.ProfileScreen
import com.quickgig.app.ui.profile.ProfileViewModel
import com.quickgig.app.ui.quickGigApp
import com.quickgig.app.ui.rememberViewModel
import com.quickgig.app.ui.settings.SettingsScreen
import com.quickgig.app.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.first

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Wires every screen from the Planning doc's UI Design section together:
 * Login -> Home (default landing) -> Post Gig / My Gigs / Profile via the
 * bottom nav, Settings reached from Profile's top-right icon.
 *
 * Who is signed in comes from SessionManager (set at login), and the start
 * screen is Home if a session was saved last time, Login otherwise.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QuickGigNavHost(activity: Activity, navController: NavHostController = rememberNavController()) {
    val app = quickGigApp()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.HOME

    // Decided once, from the saved token. (Not re-evaluated afterwards: changing a
    // NavHost's start destination mid-flight would reset the back stack.)
    val startDestination by produceState<String?>(initialValue = null) {
        val token = app.sessionManager.tokenFlow.first()
        value = if (token.isNullOrBlank()) Routes.LOGIN else Routes.HOME
    }
    val currentUserId by app.sessionManager.userIdFlow.collectAsState(initial = null)

    val start = startDestination
    if (start == null) {
        LoadingBox()
        return
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LOGIN) {
            val vm = rememberViewModel { LoginViewModel(app.authRepository) }
            LoginScreen(vm, activity = activity, onSignedIn = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            val vm = rememberViewModel { HomeViewModel(app.gigRepository) }
            HomeFeedScreen(
                viewModel = vm,
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                onOpenGig = { gigId -> navController.navigate("${
                    Routes.GIG_DETAIL
                }/$gigId") },
            )
        }
        composable(Routes.POST_GIG) {
            val vm = rememberViewModel { PostGigViewModel(app.gigRepository) }
            PostGigScreen(vm, onDone = { navController.navigate(Routes.MY_GIGS) { launchSingleTop = true } })
        }
        composable(Routes.MY_GIGS) {
            val userId = currentUserId
            if (userId == null) {
                LoadingBox()
                return@composable
            }
            val vm = rememberViewModel { MyGigsViewModel(app.gigRepository, userId) }
            MyGigsScreen(
                viewModel = vm,
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                onOpenGig = { gigId -> navController.navigate("${
                    Routes.GIG_DETAIL
                }/$gigId") },
            )
        }
        composable(Routes.PROFILE) {
            val userId = currentUserId
            if (userId == null) {
                LoadingBox()
                return@composable
            }
            val vm = rememberViewModel { ProfileViewModel(app.userRepository, userId) }
            ProfileScreen(
                viewModel = vm,
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            val userId = currentUserId
            if (userId == null) {
                LoadingBox()
                return@composable
            }
            val vm = rememberViewModel { SettingsViewModel(app.userRepository, app.authRepository, userId) }
            SettingsScreen(
                vm,
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) { popUpTo(0) }
                },
            )
        }
        composable(Routes.GIG_DETAIL) {
            val gigId = navController.currentBackStackEntry!!.arguments?.getLong("gigId") ?: return@composable LoadingBox()
            val vm = rememberViewModel {
                GigDetailViewModel(app.gigRepository, app.userRepository, gigId, currentUserId ?: 0)
            }
            GigDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onNavigateChat = { gigId -> navController.navigate("${Routes.CHAT.replace("{gigId}", gigId.toString())}") },
            )
        }
        composable(Routes.CHAT) {
            val gigId = navController.currentBackStackEntry!!.arguments?.getLong("gigId") ?: return@composable LoadingBox()
            val vm = rememberViewModel {
                ChatViewModel(app.messageRepository, gigId, currentUserId ?: 0)
            }
            ChatScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.NOTIFICATIONS) {
            val userId = currentUserId ?: return@composable LoadingBox()
            val vm = rememberViewModel { NotificationsViewModel(app.userRepository, userId) }
            NotificationsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenGig = { gigId -> navController.navigate("${Routes.GIG_DETAIL.replace("{gigId}", gigId.toString())}") },
            )
        }
    }
}
