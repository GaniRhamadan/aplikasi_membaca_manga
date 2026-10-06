package com.manga.reader.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Detail : Screen("detail/{mangaId}") {
        fun createRoute(mangaId: String) = "detail/$mangaId"
    }
    data object Reader : Screen("reader/{chapterId}") {
        fun createRoute(chapterId: String) = "reader/$chapterId"
    }
}
