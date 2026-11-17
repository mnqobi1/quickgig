package com.quickgig.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.quickgig.app.R
import com.quickgig.app.ui.nav.Routes

data class BottomTab(val route: String, val labelRes: Int, val icon: ImageVector)

val bottomTabs = listOf(
    BottomTab(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    BottomTab(Routes.MY_GIGS, R.string.nav_my_gigs, Icons.Filled.List),
    BottomTab(Routes.POST_GIG, R.string.nav_post, Icons.Filled.Add),
    BottomTab(Routes.PROFILE, R.string.nav_profile, Icons.Filled.Person),
)

@Composable
fun QuickGigBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        bottomTabs.forEach { tab ->
            val label = stringResource(tab.labelRes)
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}
