package com.manga.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.manga.reader.data.MangaRepository
import com.manga.reader.model.Chapter
import com.manga.reader.model.Manga
import com.manga.reader.ui.theme.*

@Composable
fun DetailScreen(
    mangaId: String,
    repository: MangaRepository,
    onBackClick: () -> Unit,
    onChapterClick: (String) -> Unit
) {
    var manga by remember { mutableStateOf<Manga?>(null) }
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var selectedLanguage by remember { mutableStateOf("id") }
    var sortDesc by remember { mutableStateOf(true) }
    var chapterSearchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val favoriteIds by repository.favoriteIds.collectAsState()
    val isFavorite = favoriteIds.contains(mangaId)

    LaunchedEffect(mangaId) {
        isLoading = true
        manga = repository.getMangaById(mangaId)
        val loaded = repository.getChapters(mangaId, "id")
        chapters = loaded
        if (loaded.isNotEmpty() && loaded.none { it.language == "id" }) {
            selectedLanguage = "all"
        }
        isLoading = false
    }

    val idChaptersCount = remember(chapters) {
        chapters.count { it.language == "id" }
    }

    val filteredChapters = remember(chapters, selectedLanguage, sortDesc, chapterSearchQuery) {
        val byLang = if (selectedLanguage == "id") {
            chapters.filter { it.language == "id" }
        } else {
            chapters
        }

        val sorted = if (sortDesc) byLang else byLang.reversed()

        if (chapterSearchQuery.isBlank()) {
            sorted
        } else {
            val q = chapterSearchQuery.trim().lowercase()
            sorted.filter {
                it.chapterNumber.contains(q) || it.title.lowercase().contains(q)
            }
        }
    }

    if (isLoading || manga == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryAccent)
        }
    } else {
        val item = manga!!

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Composite Header (Backdrop + Back Button + Poster + Title + CTAs)
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Backdrop Cover Image at Top with Smooth Dark Fade
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        ) {
                            AsyncImage(
                                model = item.bannerUrl.ifEmpty { item.coverUrl },
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.35f),
                                                DarkBackground.copy(alpha = 0.75f),
                                                DarkBackground
                                            )
                                        )
                                    )
                            )
                        }

                        // Foreground Content Over Backdrop
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            // Back Navigation Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp, bottom = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = DarkSurface.copy(alpha = 0.92f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.clickable { onBackClick() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Kembali",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Kembali ke Beranda",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }

                            // Poster + Meta Row (Matching Web Design)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Cover Poster
                                Box(
                                    modifier = Modifier
                                        .width(115.dp)
                                        .height(165.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.5.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                        .background(DarkElevated)
                                ) {
                                    AsyncImage(
                                        model = item.coverUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Type Badge (Manga / Manhwa)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .background(
                                                Color.Black.copy(alpha = 0.85f),
                                                RoundedCornerShape(topEnd = 6.dp)
                                            )
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = item.type,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Meta Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary,
                                        lineHeight = 23.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "Pengarang: ${item.author.ifEmpty { "Tidak Diketahui" }}",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Badges: Tamat / Berjalan & Rating & Source
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val isCompleted = item.status.equals("Completed", ignoreCase = true) ||
                                                item.statusId.equals("Tamat", ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isCompleted) SuccessColor.copy(alpha = 0.18f) else DarkElevated,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isCompleted) SuccessColor.copy(alpha = 0.4f) else BorderSubtle,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (isCompleted) "Tamat" else "Berjalan",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCompleted) SuccessColor else AccentSecondary
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(AccentGold.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .border(1.dp, AccentGold.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = AccentGold,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "${item.rating}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentGold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // CTA Buttons Row (Baca Chapter 1 + Simpan/Favorit)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val firstCh = chapters.minByOrNull {
                                            it.chapterNumber.toDoubleOrNull()
                                                ?: Regex("""([0-9]+(?:\.[0-9]+)?)""").find(it.chapterNumber)?.groupValues?.get(1)?.toDoubleOrNull()
                                                ?: 999999.0
                                        }?.id ?: chapters.lastOrNull()?.id ?: "${item.id}-ch-1"
                                        onChapterClick(firstCh)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(44.dp)
                                 ) {
                                     Text(
                                         text = "📖 Baca Chapter 1",
                                         fontSize = 13.sp,
                                         fontWeight = FontWeight.Bold,
                                         color = Color.White
                                     )
                                 }

                                 OutlinedButton(
                                     onClick = { repository.toggleFavorite(item.id, item) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isFavorite) AccentGold else BorderSubtle
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isFavorite) AccentGold.copy(alpha = 0.15f) else DarkSurface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                        contentDescription = null,
                                        tint = if (isFavorite) AccentGold else TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFavorite) "Tersimpan" else "Favorit",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isFavorite) AccentGold else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Synopsis Card
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .border(width = 1.dp, color = BorderSubtle, shape = RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sinopsis Cerita",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryAccent.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = "🇮🇩 Bahasa Indonesia",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryAccent,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = item.synopsis,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Genre Tags
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item.genres.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .background(DarkElevated, RoundedCornerShape(999.dp))
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Language Filter Bar & Chapter Header Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Language Tab Filter (Matches Web: Indonesia & Semua)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (selectedLanguage == "id") PrimaryAccent else DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selectedLanguage == "id") PrimaryAccent else BorderSubtle
                                ),
                                modifier = Modifier.clickable { selectedLanguage = "id" }
                            ) {
                                Text(
                                    text = "🇮🇩 Indonesia ($idChaptersCount)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedLanguage == "id") Color.White else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (selectedLanguage == "all") PrimaryAccent else DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selectedLanguage == "all") PrimaryAccent else BorderSubtle
                                ),
                                modifier = Modifier.clickable { selectedLanguage = "all" }
                            ) {
                                Text(
                                    text = "🌐 Semua (${chapters.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedLanguage == "all") Color.White else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Header Row: "Daftar Chapter" and Sort Order Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daftar Chapter",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${filteredChapters.size} Chapter Tersedia (Komiku / Komikindo)",
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            }

                            // Sort Order Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.clickable { sortDesc = !sortDesc }
                            ) {
                                Text(
                                    text = if (sortDesc) "Urutan: Terbaru ⇵" else "Urutan: Awal ⇵",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Search/Jump Chapter Input for large chapter list
                        if (chapters.size > 20) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = chapterSearchQuery,
                                onValueChange = { chapterSearchQuery = it },
                                placeholder = {
                                    Text(
                                        "Cari nomor chapter (contoh: 1, 50, 144)...",
                                        fontSize = 12.sp,
                                        color = TextTertiary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryAccent,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }
                }

                // 4. Chapter Items List (Matching Web Styling with [ID] Badge and "Baca" Pill)
                items(filteredChapters, key = { it.id }) { ch ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .border(width = 1.dp, color = BorderSubtle, shape = RoundedCornerShape(10.dp))
                            .clickable { onChapterClick(ch.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = ch.title.ifEmpty { "Chapter ${ch.chapterNumber}" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    val badge = when (ch.language.lowercase()) {
                                        "id" -> "🇮🇩 ID"
                                        "en" -> "🇬🇧 EN"
                                        "ja" -> "🇯🇵 JA"
                                        "es", "es-la" -> "🇪🇸 ES"
                                        else -> ch.language.uppercase()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                PrimaryAccent.copy(alpha = 0.16f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = badge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryAccent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${ch.scanlationGroup.ifEmpty { ch.source }} • ${ch.releaseDate}",
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            }

                            // "Baca" Action Pill Button (Matching Web .btn-read-pill)
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = PrimaryAccent,
                                modifier = Modifier.clickable { onChapterClick(ch.id) }
                            ) {
                                Text(
                                    text = "Baca",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
