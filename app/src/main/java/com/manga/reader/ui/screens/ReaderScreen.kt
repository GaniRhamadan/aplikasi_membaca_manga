package com.manga.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.manga.reader.data.MangaRepository
import com.manga.reader.model.Chapter
import com.manga.reader.model.ReaderPage
import com.manga.reader.ui.theme.DarkBackground
import com.manga.reader.ui.theme.DarkSurface
import com.manga.reader.ui.theme.PrimaryAccent
import com.manga.reader.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterId: String,
    initialMangaId: String = "",
    repository: MangaRepository,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    var activeChapterId by remember(chapterId) { mutableStateOf(chapterId) }
    var pages by remember { mutableStateOf<List<ReaderPage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isWebtoonMode by remember { mutableStateOf(true) } // true: Webtoon vertical scroll, false: Paged horizontal flip
    var showControls by remember { mutableStateOf(true) }
    var mangaChapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var showChapterPickerSheet by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    // Determine effective manga ID
    val effectiveMangaId = remember(activeChapterId, initialMangaId) {
        if (initialMangaId.isNotBlank()) {
            initialMangaId
        } else {
            repository.findMangaIdForChapter(activeChapterId)
        }
    }

    // Load full chapter list for accurate previous/next navigation
    LaunchedEffect(effectiveMangaId) {
        if (effectiveMangaId.isNotBlank()) {
            val list = repository.getChapters(effectiveMangaId)
            mangaChapters = list
        }
    }

    // Get current chapter details
    val currentChapter = remember(mangaChapters, activeChapterId) {
        val cleanActiveId = activeChapterId.trim()
        val decodedActive = try { java.net.URLDecoder.decode(cleanActiveId, "UTF-8") } catch (_: Exception) { cleanActiveId }
        mangaChapters.find {
            it.id == cleanActiveId || it.id == decodedActive ||
            (try { java.net.URLDecoder.decode(it.id, "UTF-8") } catch (_: Exception) { it.id }) == decodedActive
        } ?: repository.getChapter(activeChapterId)
    }

    val chapterTitle = remember(currentChapter, activeChapterId) {
        currentChapter?.title?.ifBlank { null }
            ?: repository.getChapterTitle(activeChapterId)
    }

    // Sort chapters in ascending reading order (Chapter 1, 2, 3...)
    val sortedChapters = remember(mangaChapters, currentChapter) {
        val targetLang = currentChapter?.language
        val langFiltered = if (targetLang != null && mangaChapters.any { it.language == targetLang }) {
            mangaChapters.filter { it.language == targetLang }
        } else {
            mangaChapters
        }

        langFiltered
            .distinctBy { it.chapterNumber.ifBlank { it.id } }
            .sortedWith(
                compareBy<Chapter> {
                    val numStr = it.chapterNumber.trim()
                    numStr.toDoubleOrNull()
                        ?: Regex("""([0-9]+(?:\.[0-9]+)?)""").find(numStr)?.groupValues?.get(1)?.toDoubleOrNull()
                        ?: Regex("""([0-9]+(?:\.[0-9]+)?)""").find(it.title)?.groupValues?.get(1)?.toDoubleOrNull()
                        ?: 0.0
                }.thenBy { it.chapterNumber }
            )
    }

    // Current index in sorted chapters
    val currentChapterIndex = remember(sortedChapters, activeChapterId, chapterTitle) {
        val cleanActiveId = activeChapterId.trim()
        val decodedActive = try { java.net.URLDecoder.decode(cleanActiveId, "UTF-8") } catch (_: Exception) { cleanActiveId }
        val idxById = sortedChapters.indexOfFirst {
            it.id == cleanActiveId || it.id == decodedActive ||
            (try { java.net.URLDecoder.decode(it.id, "UTF-8") } catch (_: Exception) { it.id }) == decodedActive
        }
        if (idxById != -1) idxById
        else {
            val num = Regex("""([0-9]+(?:\.[0-9]+)?)""").find(chapterTitle)?.groupValues?.get(1)?.toDoubleOrNull()
            if (num != null) {
                sortedChapters.indexOfFirst {
                    val cNum = it.chapterNumber.toDoubleOrNull()
                        ?: Regex("""([0-9]+(?:\.[0-9]+)?)""").find(it.chapterNumber)?.groupValues?.get(1)?.toDoubleOrNull()
                    cNum == num
                }
            } else -1
        }
    }

    val prevChapter = remember(sortedChapters, currentChapterIndex) {
        if (currentChapterIndex > 0) sortedChapters.getOrNull(currentChapterIndex - 1) else null
    }

    val nextChapter = remember(sortedChapters, currentChapterIndex) {
        if (currentChapterIndex in 0 until sortedChapters.size - 1) {
            sortedChapters.getOrNull(currentChapterIndex + 1)
        } else null
    }

    // Fallback for curated chapters if sortedChapters is still empty
    val canGoPrev = prevChapter != null || (sortedChapters.isEmpty() && activeChapterId.startsWith("curated-") && (activeChapterId.substringAfterLast("-ch-").toIntOrNull() ?: 1) > 1)
    val canGoNext = nextChapter != null || (activeChapterId.startsWith("curated-"))

    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(activeChapterId) {
        isLoading = true
        pages = emptyList()
        val fetched = repository.getChapterPages(activeChapterId)
        pages = fetched
        val resolvedManga = if (effectiveMangaId.isNotBlank()) effectiveMangaId else repository.findMangaIdForChapter(activeChapterId)
        val resolvedTitle = currentChapter?.title?.ifBlank { null } ?: repository.getChapterTitle(activeChapterId)
        if (resolvedManga.isNotBlank()) {
            repository.recordHistory(resolvedManga, activeChapterId, resolvedTitle)
        }
        isLoading = false
        scrollState.scrollToItem(0)
        if (fetched.isNotEmpty() && pagerState.pageCount > 0) {
            pagerState.scrollToPage(0)
        }
    }

    val currentPage by remember(isWebtoonMode, pages.size) {
        derivedStateOf {
            if (isWebtoonMode) {
                (scrollState.firstVisibleItemIndex + 1).coerceAtMost(pages.size.coerceAtLeast(1))
            } else {
                (pagerState.currentPage + 1).coerceAtMost(pages.size.coerceAtLeast(1))
            }
        }
    }

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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tidak ada halaman untuk chapter ini",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                pages = repository.getChapterPages(activeChapterId)
                                isLoading = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Muat Ulang", fontSize = 13.sp, color = Color.White)
                    }
                }
            } else {
                // Reader Content
                if (isWebtoonMode) {
                    // Continuous Vertical Webtoon Scroll
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(
                            items = pages,
                            key = { index, page -> if (page.imageUrl.isNotBlank()) page.imageUrl else index.toString() }
                        ) { index, page ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 420.dp)
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = page.imageUrl,
                                    contentDescription = "Halaman ${index + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
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

            // Indikator halaman mengambang minimalis saat membaca
            if (!showControls && !isLoading && pages.isNotEmpty()) {
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

                        // Center: Title & Quick Chapter Picker Trigger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (sortedChapters.isNotEmpty()) {
                                        showChapterPickerSheet = true
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = chapterTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (sortedChapters.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = "Pilih Chapter",
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
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
                        TextButton(
                            onClick = {
                                if (!isLoading) {
                                    if (prevChapter != null) {
                                        activeChapterId = prevChapter.id
                                    } else if (activeChapterId.startsWith("curated-")) {
                                        val curNum = activeChapterId.substringAfterLast("-ch-").toIntOrNull() ?: 1
                                        if (curNum > 1) {
                                            activeChapterId = "$effectiveMangaId-ch-${curNum - 1}"
                                        }
                                    }
                                }
                            },
                            enabled = canGoPrev && !isLoading
                        ) {
                            Text(
                                text = "Sebelumnya",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (canGoPrev && !isLoading) Color.White else Color.Gray
                            )
                        }

                        Text(
                            text = "Hal. $currentPage / ${pages.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )

                        TextButton(
                            onClick = {
                                if (!isLoading) {
                                    if (nextChapter != null) {
                                        activeChapterId = nextChapter.id
                                    } else if (activeChapterId.startsWith("curated-")) {
                                        val curNum = activeChapterId.substringAfterLast("-ch-").toIntOrNull() ?: 1
                                        val mId = if (effectiveMangaId.isNotBlank()) effectiveMangaId else activeChapterId.substringBefore("-ch-")
                                        activeChapterId = "$mId-ch-${curNum + 1}"
                                    }
                                }
                            },
                            enabled = canGoNext && !isLoading
                        ) {
                            Text(
                                text = "Berikutnya",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (canGoNext && !isLoading) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }

            // Quick Chapter Picker Modal Bottom Sheet
            if (showChapterPickerSheet && sortedChapters.isNotEmpty()) {
                ModalBottomSheet(
                    onDismissRequest = { showChapterPickerSheet = false },
                    containerColor = DarkBackground,
                    scrimColor = Color.Black.copy(alpha = 0.65f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "Daftar Chapter (${sortedChapters.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(sortedChapters.reversed(), key = { it.id }) { ch ->
                                val isCurrent = ch.id == activeChapterId
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) PrimaryAccent.copy(alpha = 0.2f) else DarkSurface,
                                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeChapterId = ch.id
                                            showChapterPickerSheet = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ch.title.ifBlank { "Chapter ${ch.chapterNumber}" },
                                            fontSize = 13.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) PrimaryAccent else Color.White,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isCurrent) {
                                            Text(
                                                text = "Sedang Dibaca",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = PrimaryAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
