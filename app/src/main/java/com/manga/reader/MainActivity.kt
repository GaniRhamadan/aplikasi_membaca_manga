package com.manga.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.manga.reader.data.MangaRepository
import com.manga.reader.ui.components.MangaVerseBottomBar
import com.manga.reader.ui.components.MangaVerseTopBar
import com.manga.reader.ui.navigation.Screen
import com.manga.reader.ui.screens.*
import com.manga.reader.ui.theme.DarkBackground
import com.manga.reader.ui.theme.MangaReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MangaReaderTheme {
                MangaAppNavigation()
            }
        }
    }
}

@Composable
fun MangaAppNavigation() {
    val navController = rememberNavController()
    val repository = remember { MangaRepository() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val isMainTab = currentRoute in listOf(
        Screen.Home.route,
        Screen.Explore.route,
        Screen.Library.route,
        Screen.History.route
    )

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            if (isMainTab) {
                MangaVerseTopBar(
                    onSearchClick = {
                        if (currentRoute != Screen.Explore.route) {
                            navController.navigate(Screen.Explore.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onFavoritesClick = {
                        if (currentRoute != Screen.Library.route) {
                            navController.navigate(Screen.Library.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isMainTab) {
                MangaVerseBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { targetRoute ->
                        if (currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        repository = repository,
                        onMangaClick = { mangaId ->
                            navController.navigate(Screen.Detail.createRoute(mangaId))
                        },
                        onChapterClick = { chapterId ->
                            navController.navigate(Screen.Reader.createRoute(chapterId))
                        },
                        onSeeAllPopularClick = {
                            navController.navigate(Screen.Explore.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable(Screen.Explore.route) {
                    SearchScreen(
                        repository = repository,
                        onMangaClick = { mangaId ->
                            navController.navigate(Screen.Detail.createRoute(mangaId))
                        }
                    )
                }

                composable(Screen.Library.route) {
                    LibraryScreen(
                        repository = repository,
                        onMangaClick = { mangaId ->
                            navController.navigate(Screen.Detail.createRoute(mangaId))
                        }
                    )
                }

                composable(Screen.History.route) {
                    HistoryScreen(
                        repository = repository,
                        onChapterClick = { chapterId ->
                            navController.navigate(Screen.Reader.createRoute(chapterId))
                        }
                    )
                }

                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(
                        navArgument("mangaId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val mangaId = backStackEntry.arguments?.getString("mangaId") ?: ""
                    DetailScreen(
                        mangaId = mangaId,
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onChapterClick = { chapterId ->
                            navController.navigate(Screen.Reader.createRoute(chapterId))
                        }
                    )
                }

                composable(
                    route = Screen.Reader.route,
                    arguments = listOf(
                        navArgument("chapterId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                    ReaderScreen(
                        chapterId = chapterId,
                        repository = repository,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onHomeClick = {
                            navController.popBackStack(Screen.Home.route, inclusive = false)
                        }
                    )
                }
            }
        }
    }
}
