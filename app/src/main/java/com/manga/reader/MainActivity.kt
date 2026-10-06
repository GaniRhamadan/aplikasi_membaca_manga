package com.manga.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.manga.reader.data.MangaRepository
import com.manga.reader.ui.navigation.Screen
import com.manga.reader.ui.screens.DetailScreen
import com.manga.reader.ui.screens.HomeScreen
import com.manga.reader.ui.screens.ReaderScreen
import com.manga.reader.ui.screens.SearchScreen
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
                onSearchClick = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                repository = repository,
                onBackClick = {
                    navController.popBackStack()
                },
                onMangaClick = { mangaId ->
                    navController.navigate(Screen.Detail.createRoute(mangaId))
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
                }
            )
        }
    }
}
