package com.manga.reader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.manga.reader.data.MangaRepository
import com.manga.reader.model.Manga
import com.manga.reader.ui.components.*
import com.manga.reader.ui.theme.DarkBackground
import com.manga.reader.ui.theme.PrimaryAccent

@Composable
fun HomeScreen(
    repository: MangaRepository,
    onMangaClick: (String) -> Unit,
    onChapterClick: (String, String?) -> Unit,
    onSeeAllPopularClick: () -> Unit
) {
    var popularList by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var latestList by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var featuredManga by remember { mutableStateOf<Manga?>(null) }
    var genres by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedGenre by remember { mutableStateOf("Semua") }
    var isLoading by remember { mutableStateOf(true) }

    val readingHistory by repository.readingHistory.collectAsState()

    LaunchedEffect(Unit) {
        popularList = repository.getPopularManga()
        latestList = repository.getLatestManga()
        featuredManga = repository.getFeaturedManga()
        genres = repository.getGenres()
        isLoading = false
    }

    val filteredPopularList = remember(popularList, selectedGenre) {
        if (selectedGenre == "Semua") {
            popularList
        } else {
            popularList.filter { m -> m.genres.any { it.equals(selectedGenre, ignoreCase = true) } }
        }
    }

    val popularChunks = remember(filteredPopularList) {
        filteredPopularList.chunked(2)
    }

    val latestChunks = remember(latestList) {
        latestList.chunked(2)
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryAccent)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Hero Spotlight Banner
            featuredManga?.let { hero ->
                item(key = "hero-spotlight") {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        HeroSpotlightCard(
                            manga = hero,
                            onReadClick = { onChapterClick("${hero.id}-ch-1", hero.id) },
                            onDetailClick = { onMangaClick(hero.id) }
                        )
                    }
                }
            }

            // 2. Genre Filter Pills
            if (genres.isNotEmpty()) {
                item(key = "genre-filter") {
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        GenrePillsRow(
                            genres = genres,
                            selectedGenre = selectedGenre,
                            onGenreSelect = { selectedGenre = it }
                        )
                    }
                }
            }

            // 3. Continue Reading (if history exists)
            if (readingHistory.isNotEmpty()) {
                val lastItem = readingHistory.first()
                item(key = "continue-reading") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        SectionHeader(
                            title = "⏱️ Lanjutkan Membaca",
                            subtitle = "Lacak progress bacaan terakhirmu"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ContinueReadingCard(
                            item = lastItem,
                            onResumeClick = { onChapterClick(lastItem.lastChapterId, lastItem.mangaId) }
                        )
                    }
                }
            }

            // 4. Manga Terpopuler Header
            item(key = "header-popular") {
                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                    SectionHeader(
                        title = "🔥 Manga Terpopuler",
                        subtitle = "Pilihan pembaca terbanyak saat ini",
                        actionText = "Lihat Semua →",
                        onActionClick = onSeeAllPopularClick
                    )
                }
            }

            // 5. Manga Terpopuler Grid (Pairs of 2)
            items(
                count = popularChunks.size,
                key = { index ->
                    val pair = popularChunks[index]
                    "pop-${pair.joinToString("-") { it.id }}"
                }
            ) { index ->
                val pair = popularChunks[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MangaCard(
                        manga = pair[0],
                        onClick = { onMangaClick(pair[0].id) },
                        modifier = Modifier.weight(1f)
                    )
                    if (pair.size > 1) {
                        MangaCard(
                            manga = pair[1],
                            onClick = { onMangaClick(pair[1].id) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 6. Update Terbaru Header
            item(key = "header-latest") {
                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                    SectionHeader(
                        title = "⚡ Update Terbaru",
                        subtitle = "Chapter baru yang siap dinikmati"
                    )
                }
            }

            // 7. Update Terbaru Grid (Pairs of 2)
            items(
                count = latestChunks.size,
                key = { index ->
                    val pair = latestChunks[index]
                    "lat-${pair.joinToString("-") { it.id }}"
                }
            ) { index ->
                val pair = latestChunks[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MangaCard(
                        manga = pair[0],
                        onClick = { onMangaClick(pair[0].id) },
                        modifier = Modifier.weight(1f)
                    )
                    if (pair.size > 1) {
                        MangaCard(
                            manga = pair[1],
                            onClick = { onMangaClick(pair[1].id) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
