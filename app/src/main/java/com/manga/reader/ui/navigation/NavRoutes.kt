package com.manga.reader.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Explore : Screen("explore")
    data object Search : Screen("explore") // Alias to Explore
    data object Library : Screen("library")
    data object History : Screen("history")
    data object Detail : Screen("detail/{mangaId}") {
        fun createRoute(mangaId: String) = "detail/$mangaId"
    }
    data object Reader : Screen("reader?chapterId={chapterId}&mangaId={mangaId}") {
        fun createRoute(chapterId: String, mangaId: String? = null): String {
            val encodedChapterId = java.net.URLEncoder.encode(chapterId, java.nio.charset.StandardCharsets.UTF_8.name())
            val encodedMangaId = if (!mangaId.isNullOrBlank()) {
                java.net.URLEncoder.encode(mangaId, java.nio.charset.StandardCharsets.UTF_8.name())
            } else {
                ""
            }
            return "reader?chapterId=$encodedChapterId&mangaId=$encodedMangaId"
        }
    }
}

