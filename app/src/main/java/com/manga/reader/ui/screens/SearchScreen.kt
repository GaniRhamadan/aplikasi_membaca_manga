package com.manga.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manga.reader.data.MangaRepository
import com.manga.reader.model.Manga
import com.manga.reader.ui.components.MangaCard
import com.manga.reader.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    repository: MangaRepository,
    onMangaClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("all") }
    var selectedGenres by remember { mutableStateOf<Set<String>>(emptySet()) }
    var availableGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        availableGenres = repository.getGenres().filter { !it.equals("Semua", ignoreCase = true) }
    }

    LaunchedEffect(searchQuery, selectedSource, selectedGenres) {
        val local = repository.searchLocalManga(
            query = searchQuery,
            filterGenres = selectedGenres,
            filterSource = selectedSource
        )
        searchResults = local

        if (searchQuery.isNotBlank()) {
            delay(200)
        }
        val needsRemote = searchQuery.isNotBlank() || selectedGenres.isNotEmpty() || selectedSource == "mangadex" || selectedSource == "komikindo"
        if (needsRemote) {
            isLoading = local.isEmpty()
            val remote = repository.searchManga(
                query = searchQuery,
                filterGenres = selectedGenres,
                filterSource = selectedSource
            )
            if (remote.isNotEmpty()) {
                searchResults = remote
            } else if (local.isEmpty()) {
                searchResults = emptyList()
            }
            isLoading = false
        }
    }

    val searchChunks by remember {
        derivedStateOf { searchResults.chunked(2) }
    }

    val row1Genres = remember(availableGenres) {
        availableGenres.filterIndexed { index, _ -> index % 2 == 0 }
    }
    val row2Genres = remember(availableGenres) {
        availableGenres.filterIndexed { index, _ -> index % 2 != 0 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search Input Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(width = 1.dp, color = BorderSubtle, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Cari judul manga, komik, atau pengarang...",
                                fontSize = 13.sp,
                                color = TextTertiary
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryAccent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Hapus",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Filter Sumber (Semua Sumber, MangaDex Live, Komikindo, Kurasi Cepat)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SourceFilterChip(
                    label = "Semua Sumber",
                    isSelected = selectedSource == "all",
                    onClick = { selectedSource = "all" }
                )
                SourceFilterChip(
                    label = "MangaDex Live",
                    isSelected = selectedSource == "mangadex",
                    onClick = { selectedSource = "mangadex" }
                )
                SourceFilterChip(
                    label = "Sub Indo (Komikindo)",
                    isSelected = selectedSource == "komikindo",
                    onClick = { selectedSource = "komikindo" }
                )
                SourceFilterChip(
                    label = "Kurasi Cepat",
                    isSelected = selectedSource == "curated",
                    onClick = { selectedSource = "curated" }
                )
            }
        }

        // Multi-Genre Filter Section
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Filter Genre",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (selectedGenres.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(PrimaryAccent.copy(alpha = 0.2f))
                                    .border(width = 1.dp, color = PrimaryAccent.copy(alpha = 0.5f), shape = RoundedCornerShape(999.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${selectedGenres.size} dipilih",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryAccent
                                )
                            }
                        }
                    }

                    if (selectedGenres.isNotEmpty()) {
                        Text(
                            text = "Reset Genre",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedGenres = emptySet() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    } else {
                        Text(
                            text = "Pilih 1 atau lebih",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }

                // 2-Row Horizontal Scrollable Genre Chips
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row1Genres.forEach { genre ->
                            val isSelected = selectedGenres.contains(genre)
                            GenreFilterChip(
                                label = genre,
                                isSelected = isSelected,
                                onClick = {
                                    selectedGenres = if (isSelected) {
                                        selectedGenres - genre
                                    } else {
                                        selectedGenres + genre
                                    }
                                }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row2Genres.forEach { genre ->
                            val isSelected = selectedGenres.contains(genre)
                            GenreFilterChip(
                                label = genre,
                                isSelected = isSelected,
                                onClick = {
                                    selectedGenres = if (isSelected) {
                                        selectedGenres - genre
                                    } else {
                                        selectedGenres + genre
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Active Genre Badges (jika ada genre yang dipilih)
        if (selectedGenres.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aktif:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    selectedGenres.forEach { genre ->
                        ActiveGenrePill(
                            label = genre,
                            onRemove = { selectedGenres = selectedGenres - genre }
                        )
                    }
                }
            }
        }

        // Status Text
        item {
            val genreSummary = if (selectedGenres.isNotEmpty()) {
                " [Genre: ${selectedGenres.joinToString(", ")}]"
            } else ""

            val statusText = if (searchQuery.isEmpty() && selectedGenres.isEmpty()) {
                "Menampilkan katalog manga pilihan (${searchResults.size} judul)"
            } else if (searchQuery.isEmpty()) {
                "Ditemukan ${searchResults.size} manga dengan genre${genreSummary}"
            } else {
                "Ditemukan ${searchResults.size} judul untuk \"$searchQuery\"${genreSummary}"
            }

            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }

        if (isLoading) {
            item(key = "search-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            }
        } else if (searchResults.isEmpty()) {
            item(key = "search-empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Tidak ada hasil yang cocok",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedGenres.isNotEmpty() && searchQuery.isNotEmpty())
                                "Tidak menemukan komik dengan judul \"$searchQuery\" dan kombinasi genre yang dipilih. Coba kurangi salah satu genre atau cari dengan kata kunci lain."
                            else if (selectedGenres.isNotEmpty())
                                "Tidak menemukan manga dengan semua genre yang dipilih (${selectedGenres.joinToString(", ")}). Coba pilih lebih sedikit genre."
                            else if (searchQuery.isNotEmpty())
                                "Tidak menemukan judul untuk \"$searchQuery\". Coba ganti filter sumber atau periksa ejaan judul."
                            else
                                "Coba cari dengan kata kunci judul komik, pengarang, atau pilih filter genre di atas.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        } else {
            items(
                count = searchChunks.size,
                key = { index ->
                    val pair = searchChunks[index]
                    "search-${pair.joinToString("-") { it.id }}"
                }
            ) { index ->
                val pair = searchChunks[index]
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun SourceFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSelected) PrimaryAccent else DarkSurface)
            .border(
                width = 1.dp,
                color = if (isSelected) PrimaryAccent else BorderSubtle,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else TextSecondary
        )
    }
}

@Composable
private fun GenreFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSelected) PrimaryAccent else DarkSurface)
            .border(
                width = 1.dp,
                color = if (isSelected) PrimaryAccent else BorderSubtle,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextSecondary
            )
        }
    }
}

@Composable
private fun ActiveGenrePill(
    label: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(PrimaryAccent.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = PrimaryAccent.copy(alpha = 0.4f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable { onRemove() }
            .padding(start = 10.dp, end = 6.dp, top = 3.dp, bottom = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryAccent
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Hapus filter",
                tint = PrimaryAccent,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
