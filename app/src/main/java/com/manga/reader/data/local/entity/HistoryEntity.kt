package com.manga.reader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manga.reader.model.HistoryItem

@Entity(tableName = "reading_history")
data class HistoryEntity(
    @PrimaryKey
    val mangaId: String,
    val mangaTitle: String,
    val coverUrl: String,
    val lastChapterId: String,
    val lastChapterTitle: String,
    val pageIndex: Int = 0,
    val lastReadAt: Long = System.currentTimeMillis()
) {
    fun toHistoryItem(): HistoryItem = HistoryItem(
        mangaId = mangaId,
        mangaTitle = mangaTitle,
        coverUrl = coverUrl,
        lastChapterId = lastChapterId,
        lastChapterTitle = lastChapterTitle
    )

    companion object {
        fun fromHistoryItem(item: HistoryItem, pageIndex: Int = 0): HistoryEntity = HistoryEntity(
            mangaId = item.mangaId,
            mangaTitle = item.mangaTitle,
            coverUrl = item.coverUrl,
            lastChapterId = item.lastChapterId,
            lastChapterTitle = item.lastChapterTitle,
            pageIndex = pageIndex,
            lastReadAt = System.currentTimeMillis()
        )
    }
}
