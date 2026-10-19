package com.manga.reader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manga.reader.model.Manga

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val mangaId: String,
    val title: String,
    val altTitles: String = "",
    val coverUrl: String,
    val bannerUrl: String = "",
    val author: String = "Unknown",
    val status: String = "Ongoing",
    val statusId: String = "Berjalan",
    val synopsis: String = "",
    val genres: String = "",
    val rating: Float = 0.0f,
    val releaseYear: String = "",
    val totalChapters: Int = 0,
    val source: String = "MangaDex",
    val type: String = "Manga",
    val hasIndonesian: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toManga(): Manga = Manga(
        id = mangaId,
        title = title,
        altTitles = if (altTitles.isBlank()) emptyList() else altTitles.split("||"),
        coverUrl = coverUrl,
        bannerUrl = bannerUrl,
        author = author,
        status = status,
        statusId = statusId,
        synopsis = synopsis,
        genres = if (genres.isBlank()) emptyList() else genres.split(",").map { it.trim() },
        rating = rating,
        releaseYear = releaseYear,
        totalChapters = totalChapters,
        source = source,
        type = type,
        hasIndonesian = hasIndonesian
    )

    companion object {
        fun fromManga(manga: Manga): FavoriteEntity = FavoriteEntity(
            mangaId = manga.id,
            title = manga.title,
            altTitles = manga.altTitles.joinToString("||"),
            coverUrl = manga.coverUrl,
            bannerUrl = manga.bannerUrl,
            author = manga.author,
            status = manga.status,
            statusId = manga.statusId,
            synopsis = manga.synopsis,
            genres = manga.genres.joinToString(","),
            rating = manga.rating,
            releaseYear = manga.releaseYear,
            totalChapters = manga.totalChapters,
            source = manga.source,
            type = manga.type,
            hasIndonesian = manga.hasIndonesian,
            addedAt = System.currentTimeMillis()
        )
    }
}
