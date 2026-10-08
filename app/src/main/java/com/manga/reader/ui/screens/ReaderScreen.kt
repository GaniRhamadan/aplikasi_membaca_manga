package com.manga.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.manga.reader.data.MangaRepository
import com.manga.reader.model.ReaderPage
import com.manga.reader.ui.theme.PrimaryAccent
import com.manga.reader.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    chapterId: String,
    repository: MangaRepository,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    var activeChapterId by remember(chapterId) { mutableStateOf(chapterId) }
    var pages by remember { mutableStateOf<List<ReaderPage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isWebtoonMode by remember { mutableStateOf(true) } // true: Webtoon vertical scroll, false: Paged horizontal flip
    var showControls by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    // Extract manga ID and chapter title
    val mangaId = remember(activeChapterId) {
        activeChapterId.substringBefore("-ch-")
    }
    val chapterTitle = remember(activeChapterId) {
        val num = activeChapterId.substringAfter("-ch-", "")
        if (num.isNotEmpty()) "Chapter $num" else activeChapterId
    }

    LaunchedEffect(activeChapterId) {
        isLoading = true
        pages = repository.getChapterPages(activeChapterId)
        repository.recordHistory(mangaId, activeChapterId, chapterTitle)
        isLoading = false
        coroutineScope.launch {
            scrollState.scrollToItem(0)
        }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        }
                    )
                }
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryAccent)
                }
            } else if (pages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada halaman untuk chapter ini",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Reader Content
                if (isWebtoonMode) {
                    // Continuous Vertical Webtoon Scroll (Mulus tanpa jeda hitam antar halaman)
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(
                            items = pages,
                            key = { index, page -> if (page.imageUrl.isNotBlank()) page.imageUrl else index.toString() }
                        ) { index, page ->
                            AsyncImage(
                                model = page.imageUrl,
                                contentDescription = "Halaman ${index + 1}",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Horizontal Page-by-Page Flip
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = pages[pageIndex].imageUrl,
                                contentDescription = "Halaman ${pageIndex + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Indikator halaman mengambang minimalis saat membaca (hanya tampil saat kontrol disembunyikan)
            if (!showControls && !isLoading && pages.isNotEmpty()) {
                val currentPage = if (isWebtoonMode) {
                    (scrollState.firstVisibleItemIndex + 1).coerceAtMost(pages.size.coerceAtLeast(1))
                } else {
                    (pagerState.currentPage + 1).coerceAtMost(pages.size.coerceAtLeast(1))
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 18.dp)
                ) {
                    Text(
                        text = "$currentPage / ${pages.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Floating Top Bar
            if (showControls) {
                Surface(
                    color = Color.Black.copy(alpha = 0.88f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left buttons: Back & Home
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { onHomeClick() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Beranda",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Center: Title
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = chapterTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right: Mode Toggle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryAccent,
                            modifier = Modifier.clickable { isWebtoonMode = !isWebtoonMode }
                        ) {
                            Text(
                                text = if (isWebtoonMode) "Webtoon" else "Halaman",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Floating Bottom Bar
                Surface(
                    color = Color.Black.copy(alpha = 0.88f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentNum = chapterTitle.filter { it.isDigit() }.toIntOrNull() ?: 1

                        TextButton(
                            onClick = {
                                if (currentNum > 1) {
                                    activeChapterId = "$mangaId-ch-${currentNum - 1}"
                                }
                            },
                            enabled = currentNum > 1
                        ) {
                            Text(
                                text = "Sebelumnya",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentNum > 1) Color.White else Color.Gray
                            )
                        }

                        // Page indicator
                        val currentPage = if (isWebtoonMode) {
                            (scrollState.firstVisibleItemIndex + 1).coerceAtMost(pages.size.coerceAtLeast(1))
                        } else {
                            (pagerState.currentPage + 1).coerceAtMost(pages.size.coerceAtLeast(1))
                        }
                        Text(
                            text = "Hal. $currentPage / ${pages.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )

                        TextButton(
                            onClick = {
                                activeChapterId = "$mangaId-ch-${currentNum + 1}"
                            }
                        ) {
                            Text(
                                text = "Berikutnya",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
