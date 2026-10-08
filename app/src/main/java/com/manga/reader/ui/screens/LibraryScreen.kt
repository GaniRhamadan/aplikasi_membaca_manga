package com.manga.reader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manga.reader.data.MangaRepository
import com.manga.reader.model.Manga
import com.manga.reader.ui.components.MangaCard
import com.manga.reader.ui.theme.DarkBackground
import com.manga.reader.ui.theme.TextPrimary
import com.manga.reader.ui.theme.TextSecondary
import com.manga.reader.ui.theme.TextTertiary

@Composable
fun LibraryScreen(
    repository: MangaRepository,
    onMangaClick: (String) -> Unit
) {
    val favoriteIds by repository.favoriteIds.collectAsState()
    var favoriteMangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }

    LaunchedEffect(favoriteIds) {
        favoriteMangaList = repository.getFavoriteManga()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "⭐ Manga Favorit",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Daftar komik yang kamu simpan untuk dibaca kembali",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }
        }

        if (favoriteMangaList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Belum Ada Favorit",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Simpan komik favoritmu dengan menekan tombol bintang pada halaman detail manga.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        } else {
            val chunks = favoriteMangaList.chunked(2)
            items(chunks.size) { index ->
                val pair = chunks[index]
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
