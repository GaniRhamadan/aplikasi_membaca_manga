package com.manga.reader.model

import kotlinx.serialization.Serializable

@Serializable
data class Manga(
    val id: String,
    val title: String,
    val altTitles: List<String> = emptyList(),
    val coverUrl: String,
    val author: String = "Unknown",
    val status: String = "Ongoing",
    val synopsis: String = "",
    val genres: List<String> = emptyList(),
    val rating: Float = 0.0f,
    val releaseYear: String = "",
    val totalChapters: Int = 0,
    val source: String = "MangaDex", // MangaDex, Komikindo, MangaSee
    val hasIndonesian: Boolean = true
)
