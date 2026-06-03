package com.nimmaguru.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nimmaguru.app.core.ui.theme.NimmaGuruTheme
import com.nimmaguru.app.feature.appreciation.presentation.PostAppreciationScreen
import com.nimmaguru.app.feature.auth.presentation.BasicOnboardingScreen
import com.nimmaguru.app.feature.auth.presentation.OtpVerifyScreen
import com.nimmaguru.app.feature.auth.presentation.PhoneEntryScreen
import com.nimmaguru.app.feature.calendar.presentation.CalendarScreen
import com.nimmaguru.app.feature.calendar.presentation.CreateSessionScreen
import com.nimmaguru.app.feature.calendar.presentation.SessionDetailScreen
import com.nimmaguru.app.feature.discover.presentation.DiscoverScreen
import com.nimmaguru.app.feature.home.presentation.HomeScreen
import com.nimmaguru.app.feature.onboarding.presentation.GuruOnboardingScreen
import com.nimmaguru.app.feature.profile.presentation.GuruProfileScreen
import com.nimmaguru.app.feature.profile.presentation.MyProfileScreen
import com.nimmaguru.app.navigation.BasicOnboardingRoute
import com.nimmaguru.app.navigation.CalendarRoute
import com.nimmaguru.app.navigation.CreateSessionRoute
import com.nimmaguru.app.navigation.DiscoverRoute
import com.nimmaguru.app.navigation.GuruDetailRoute
import com.nimmaguru.app.navigation.GuruOnboardingRoute
import com.nimmaguru.app.navigation.HomeRoute
import com.nimmaguru.app.navigation.OtpVerifyRoute
import com.nimmaguru.app.navigation.PhoneEntryRoute
import com.nimmaguru.app.navigation.PostAppreciationRoute
import com.nimmaguru.app.navigation.ProfileRoute
import com.nimmaguru.app.navigation.SessionDetailRoute

/**
 * Bottom navigation tab definition.
 * R-COMP-03: All icons have contentDescription via stringResource.
 */
data class BottomNavTab(
    val route: Any,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescResId: Int,
)

val bottomNavTabs = listOf(
    BottomNavTab(
        route = HomeRoute,
        labelResId = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        contentDescResId = R.string.cd_bottom_nav_home,
    ),
    BottomNavTab(
        route = DiscoverRoute,
        labelResId = R.string.nav_find_guru,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        contentDescResId = R.string.cd_bottom_nav_find,
    ),
    BottomNavTab(
        route = CalendarRoute,
        labelResId = R.string.nav_calendar,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        contentDescResId = R.string.cd_bottom_nav_calendar,
    ),
    BottomNavTab(
        route = ProfileRoute,
        labelResId = R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        contentDescResId = R.string.cd_bottom_nav_profile,
    ),
)

/**
 * Main navigation host for the app.
 * ADR-01: Single Activity + Compose Navigation.
 * P-COMP-06: Uses launchSingleTop + popUpTo to prevent back stack duplication.
 */
@Composable
fun NimmaGuruNavHost(
    modifier: Modifier = Modifier,
    startDestination: Any = HomeRoute,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        bottomNavTabs.any { tab -> dest.hasRoute(tab.route::class) }
    } == true

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { dest ->
                            dest.hasRoute(tab.route::class)
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.contentDescResId),
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.labelResId),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onNavigateToGuruDetail = { guruId ->
                        navController.navigate(GuruDetailRoute(guruId))
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(GuruOnboardingRoute)
                    },
                    onNavigateToSession = { sessionId, guruId ->
                        navController.navigate(SessionDetailRoute(sessionId, guruId))
                    },
                )
            }
            composable<DiscoverRoute> {
                DiscoverScreen(
                    onNavigateToGuruDetail = { guruId ->
                        navController.navigate(GuruDetailRoute(guruId))
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(GuruOnboardingRoute)
                    },
                )
            }
            composable<CalendarRoute> {
                CalendarScreen(
                    onNavigateToSession = { sessionId, guruId ->
                        navController.navigate(SessionDetailRoute(sessionId, guruId))
                    },
                    onNavigateToCreateSession = {
                        navController.navigate(CreateSessionRoute)
                    },
                )
            }
            composable<ProfileRoute> {
                MyProfileScreen(
                    onNavigateToOnboarding = {
                        navController.navigate(GuruOnboardingRoute)
                    },
                    onNavigateToGuruDetail = { guruId ->
                        navController.navigate(GuruDetailRoute(guruId))
                    },
                    onSignedOut = {
                        navController.navigate(PhoneEntryRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable<GuruDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<GuruDetailRoute>()
                GuruProfileScreen(
                    guruId = route.guruId,
                    onNavigateToSession = { sessionId, guruId ->
                        navController.navigate(SessionDetailRoute(sessionId, guruId))
                    },
                    onNavigateToAppreciation = { guruId ->
                        navController.navigate(PostAppreciationRoute(guruId))
                    },
                )
            }
            composable<SessionDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<SessionDetailRoute>()
                SessionDetailScreen(
                    sessionId = route.sessionId,
                    guruId = route.guruId,
                    onBack = { navController.popBackStack() },
                    onNavigateToGuru = { guruId ->
                        navController.navigate(GuruDetailRoute(guruId))
                    },
                )
            }
            composable<PostAppreciationRoute> {
                PostAppreciationScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable<PhoneEntryRoute> {
                PhoneEntryScreen(
                    onNavigateToOtp = { phone ->
                        navController.navigate(OtpVerifyRoute(phoneNumber = phone)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(HomeRoute) {
                            popUpTo(PhoneEntryRoute) { inclusive = true }
                        }
                    },
                    onNavigateToBasicOnboarding = {
                        navController.navigate(BasicOnboardingRoute) {
                            popUpTo(PhoneEntryRoute) { inclusive = true }
                        }
                    },
                )
            }
            composable<OtpVerifyRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<OtpVerifyRoute>()
                OtpVerifyScreen(
                    phoneNumber = route.phoneNumber,
                    onNavigateToHome = {
                        navController.navigate(HomeRoute) {
                            popUpTo(PhoneEntryRoute) { inclusive = true }
                        }
                    },
                    onNavigateToBasicOnboarding = {
                        navController.navigate(BasicOnboardingRoute) {
                            popUpTo(PhoneEntryRoute) { inclusive = true }
                        }
                    },
                )
            }
            composable<BasicOnboardingRoute> {
                BasicOnboardingScreen(
                    onNavigateToHome = {
                        navController.navigate(HomeRoute) {
                            popUpTo(BasicOnboardingRoute) { inclusive = true }
                        }
                    }
                )
            }
            composable<GuruOnboardingRoute> {
                GuruOnboardingScreen(
                    onNavigateToHome = {
                        navController.navigate(HomeRoute) {
                            popUpTo(navController.graph.findStartDestination().id)
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable<CreateSessionRoute> {
                CreateSessionScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_PlaceholderScreen() {
    NimmaGuruTheme {
        PlaceholderScreen(title = "Home")
    }
}
