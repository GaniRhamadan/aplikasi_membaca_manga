package com.manga.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.manga.reader.data.MangaRepository
import com.manga.reader.model.Chapter
import com.manga.reader.model.Manga
import com.manga.reader.ui.theme.AccentGold
import com.manga.reader.ui.theme.CrimsonPrimary
import com.manga.reader.ui.theme.DarkBackground
import com.manga.reader.ui.theme.DarkSurface
import com.manga.reader.ui.theme.DarkSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
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
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(mangaId, selectedLanguage) {
        manga = repository.getMangaById(mangaId)
        chapters = repository.getChapters(mangaId, selectedLanguage)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = manga?.title ?: "Detail Manga",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Beranda",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (isLoading || manga == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CrimsonPrimary)
            }
        } else {
            val item = manga!!
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header Info
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = item.coverUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(120.dp)
                                .height(170.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Pengarang: ${item.author}",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                            Text(
                                text = "Status: ${item.status} (${item.releaseYear})",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${item.rating} / 5.0",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Synopsis
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Sinopsis",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CrimsonPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.synopsis,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Filter Bahasa
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daftar Chapter (${chapters.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedLanguage == "id",
                                onClick = { selectedLanguage = "id" },
                                label = { Text("🇮🇩 Indo") }
                            )
                            FilterChip(
                                selected = selectedLanguage == "en",
                                onClick = { selectedLanguage = "en" },
                                label = { Text("🇬🇧 Eng") }
                            )
                        }
                    }
                }

                // Chapters list
                items(chapters) { chapter ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapterClick(chapter.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Chapter ${chapter.chapterNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = chapter.scanlationGroup,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Button(
                                onClick = { onChapterClick(chapter.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(text = "Baca", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
