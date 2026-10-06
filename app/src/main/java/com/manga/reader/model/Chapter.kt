package com.manga.reader.model

import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    val id: String,
    val mangaId: String,
    val chapterNumber: String,
    val title: String = "",
    val language: String = "id", // "id" for Indonesia, "en" for English
    val scanlationGroup: String = "",
    val releaseDate: String = "",
    val source: String = "MangaDex"
)

@Serializable
data class ReaderPage(
    val pageNumber: Int,
    val imageUrl: String,
    val width: Int = 0,
    val height: Int = 0
)
