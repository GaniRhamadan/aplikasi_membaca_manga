package com.manga.reader.model

import kotlinx.serialization.Serializable

@Serializable
data class Manga(
    val id: String,
    val title: String,
    val altTitles: List<String> = emptyList(),
    val coverUrl: String,
    val bannerUrl: String = "",
    val author: String = "Unknown",
    val status: String = "Ongoing",
    val statusId: String = "Berjalan",
    val synopsis: String = "",
    val genres: List<String> = emptyList(),
    val rating: Float = 0.0f,
    val releaseYear: String = "",
    val totalChapters: Int = 0,
    val source: String = "MangaDex",
    val type: String = "Manga",
    val hasIndonesian: Boolean = true
)

@Serializable
data class HistoryItem(
    val mangaId: String,
    val mangaTitle: String,
    val coverUrl: String,
    val lastChapterId: String,
    val lastChapterTitle: String,
    val timestamp: Long = System.currentTimeMillis()
)

