package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ReaderFontFamily
import com.example.data.ReaderTheme
import com.example.epub.EpubBook
import com.example.epub.EpubPaginator
import com.example.epub.ParagraphItem
import com.example.ui.MainViewModel
import com.example.ui.components.BookmarksSheet
import com.example.ui.components.ReaderSettingsSheet
import com.example.ui.components.TableOfContentsSheet
import com.example.ui.theme.ReaderColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    book: EpubBook,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.readerSettings.collectAsStateWithLifecycle()
    val savedChapterIndex by viewModel.currentChapterIndex.collectAsStateWithLifecycle()
    val savedPageIndex by viewModel.savedPageIndex.collectAsStateWithLifecycle()
    val areControlsVisible by viewModel.areControlsVisible.collectAsStateWithLifecycle()
    val isSettingsSheetOpen by viewModel.isSettingsSheetOpen.collectAsStateWithLifecycle()
    val isTocSheetOpen by viewModel.isTocSheetOpen.collectAsStateWithLifecycle()
    val isBookmarksSheetOpen by viewModel.isBookmarksSheetOpen.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchInChapterActive.collectAsStateWithLifecycle()
    val inChapterSearchQuery by viewModel.inChapterSearchQuery.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Handle Android system back button
    BackHandler {
        if (isSearchActive) {
            viewModel.toggleSearchInChapter()
        } else {
            viewModel.closeBook()
        }
    }

    // Theme color palette
    val readerBgColor = when (settings.theme) {
        ReaderTheme.LIGHT -> ReaderColors.LightBg
        ReaderTheme.SEPIA -> ReaderColors.SepiaBg
        ReaderTheme.NIGHT -> ReaderColors.NightBg
        ReaderTheme.AMOLED -> ReaderColors.AmoledBg
    }

    val readerTextColor = when (settings.theme) {
        ReaderTheme.LIGHT -> ReaderColors.LightText
        ReaderTheme.SEPIA -> ReaderColors.SepiaText
        ReaderTheme.NIGHT -> ReaderColors.NightText
        ReaderTheme.AMOLED -> ReaderColors.AmoledText
    }

    val readerSecondaryColor = when (settings.theme) {
        ReaderTheme.LIGHT -> ReaderColors.LightSecondary
        ReaderTheme.SEPIA -> ReaderColors.SepiaSecondary
        ReaderTheme.NIGHT -> ReaderColors.NightSecondary
        ReaderTheme.AMOLED -> ReaderColors.AmoledSecondary
    }

    val controlOverlayBg = when (settings.theme) {
        ReaderTheme.LIGHT -> Color(0xFFF1ECE4).copy(alpha = 0.96f)
        ReaderTheme.SEPIA -> Color(0xFFEADBCA).copy(alpha = 0.96f)
        ReaderTheme.NIGHT -> Color(0xFF141C28).copy(alpha = 0.96f)
        ReaderTheme.AMOLED -> Color(0xFF0F0F10).copy(alpha = 0.96f)
    }

    val fontFamily = when (settings.fontFamily) {
        ReaderFontFamily.SERIF -> FontFamily.Serif
        ReaderFontFamily.SANS_SERIF -> FontFamily.SansSerif
        ReaderFontFamily.MONOSPACE -> FontFamily.Monospace
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(readerBgColor)
            .testTag("reader_screen")
    ) {
        // Safe padding tuned for Galaxy S25 punch-hole camera and gesture navigation
        val horizontalMargin = (maxWidth * 0.045f).coerceIn(16.dp, 32.dp)
        val topMargin = (maxHeight * 0.09f).coerceIn(68.dp, 100.dp)
        val bottomMargin = (maxHeight * 0.05f).coerceIn(36.dp, 60.dp)

        val availableContentWidth = maxWidth - (horizontalMargin * 2)
        val availableContentHeight = maxHeight - topMargin - bottomMargin - 48.dp

        val pages = remember(
            book,
            availableContentWidth,
            availableContentHeight,
            settings.fontSizeSp,
            settings.lineSpacing,
            settings.fontFamily
        ) {
            EpubPaginator.paginate(
                book = book,
                availableWidthDp = availableContentWidth.value,
                availableHeightDp = availableContentHeight.value,
                fontSizeSp = settings.fontSizeSp,
                lineSpacingMultiplier = settings.lineSpacing.multiplier,
                fontFamily = settings.fontFamily
            )
        }

        var currentGlobalPageIndex by remember(book.id) { mutableIntStateOf(-1) }
        var previousTotalPages by remember(book.id) { mutableIntStateOf(0) }
        var isSlideForward by remember { mutableStateOf(true) }

        // Initialize or preserve proportional reading position when pages re-paginate (e.g. font size change)
        LaunchedEffect(book.id, pages.size) {
            if (pages.isEmpty()) return@LaunchedEffect

            if (currentGlobalPageIndex == -1) {
                // First open: restore saved page if it still belongs to savedChapterIndex, else chapter start
                val restored = savedPageIndex.coerceIn(0, pages.size - 1)
                currentGlobalPageIndex = if (pages[restored].chapterIndex == savedChapterIndex) {
                    restored
                } else {
                    pages.indexOfFirst { it.chapterIndex == savedChapterIndex }.coerceAtLeast(0)
                }
            } else if (previousTotalPages > 0 && previousTotalPages != pages.size) {
                // Preserve proportional position after font size / spacing change
                val ratio = currentGlobalPageIndex.toFloat() / previousTotalPages.toFloat()
                currentGlobalPageIndex = (ratio * pages.size).toInt().coerceIn(0, pages.size - 1)
            }
            previousTotalPages = pages.size
        }

        val resolvedPageIndex = if (pages.isEmpty()) 0 else currentGlobalPageIndex.coerceIn(0, pages.size - 1)
        val currentPage = pages.getOrNull(resolvedPageIndex)

        // Notify ViewModel of reading progress changes
        LaunchedEffect(resolvedPageIndex, pages) {
            val page = pages.getOrNull(resolvedPageIndex)
            if (page != null && currentGlobalPageIndex >= 0) {
                viewModel.updatePageProgress(
                    chapterIndex = page.chapterIndex,
                    progressPercent = page.progressPercent,
                    pageIndex = resolvedPageIndex
                )
            }
        }

        // Always keep updated references inside pointerInput lambdas (fixes stale closure after re-pagination)
        val latestPageCount by rememberUpdatedState(pages.size)
        val latestPageIndex by rememberUpdatedState(resolvedPageIndex)

        fun goToNextPage() {
            if (latestPageIndex < latestPageCount - 1) {
                isSlideForward = true
                currentGlobalPageIndex = latestPageIndex + 1
            }
        }

        fun goToPreviousPage() {
            if (latestPageIndex > 0) {
                isSlideForward = false
                currentGlobalPageIndex = latestPageIndex - 1
            }
        }

        var dragAccumulatorX by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(book.id) {
                    detectDragGestures(
                        onDragEnd = {
                            val threshold = 40.dp.toPx()
                            if (dragAccumulatorX < -threshold) {
                                // Swipe left -> next page
                                goToNextPage()
                            } else if (dragAccumulatorX > threshold) {
                                // Swipe right -> previous page
                                goToPreviousPage()
                            }
                            dragAccumulatorX = 0f
                        },
                        onDragCancel = {
                            dragAccumulatorX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulatorX += dragAmount.x
                        }
                    )
                }
                .pointerInput(book.id) {
                    detectTapGestures(
                        onTap = { offset ->
                            val screenWidth = size.width
                            when {
                                offset.x > screenWidth * 0.75f -> goToNextPage()
                                offset.x < screenWidth * 0.25f -> goToPreviousPage()
                                else -> viewModel.toggleControls()
                            }
                        }
                    )
                }
        ) {
            if (currentPage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalMargin)
                        .padding(top = topMargin, bottom = bottomMargin)
                ) {
                    // Mini Top Header Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPage.chapterTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = readerSecondaryColor.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${currentPage.progressPercent.toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = readerSecondaryColor.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Animated Page Content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AnimatedContent(
                            targetState = currentPage,
                            contentKey = { it.globalPageIndex },
                            transitionSpec = {
                                if (isSlideForward) {
                                    (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                                        (slideOutHorizontally { width -> -width / 3 } + fadeOut())
                                } else {
                                    (slideInHorizontally { width -> -width / 3 } + fadeIn()) togetherWith
                                        (slideOutHorizontally { width -> width / 3 } + fadeOut())
                                }
                            },
                            label = "page_transition"
                        ) { page ->
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                page.paragraphs.forEach { paragraph ->
                                    SingleParagraphView(
                                        paragraph = paragraph,
                                        fontSizeSp = settings.fontSizeSp,
                                        lineSpacingMultiplier = settings.lineSpacing.multiplier,
                                        fontFamily = fontFamily,
                                        textColor = readerTextColor,
                                        justify = settings.justifyText,
                                        searchQuery = inChapterSearchQuery
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mini Bottom Footer Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Skyrius ${currentPage.chapterIndex + 1} iš ${book.chapters.size}",
                            fontSize = 11.sp,
                            color = readerSecondaryColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${currentPage.pageIndexInChapter + 1} / ${currentPage.totalPagesInChapter} psl.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = readerSecondaryColor.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // TOP CONTROLS BAR
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = controlOverlayBg,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { viewModel.closeBook() },
                                modifier = Modifier.testTag("reader_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Grįžti į biblioteką",
                                    tint = readerTextColor
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = readerTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentPage?.chapterTitle ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = readerSecondaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // TTS (Skaitymas balsu)
                            IconButton(
                                onClick = {
                                    val currentPageText = currentPage?.paragraphs?.joinToString(" ") { it.text }
                                    viewModel.toggleSpeech(currentPageText)
                                },
                                modifier = Modifier.testTag("reader_tts_button")
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isSpeaking) "Sustabdyti skaitymą" else "Skaityti balsu",
                                    tint = if (isSpeaking) MaterialTheme.colorScheme.primary else readerTextColor
                                )
                            }

                            // Paieška
                            IconButton(
                                onClick = { viewModel.toggleSearchInChapter() },
                                modifier = Modifier.testTag("reader_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Ieškoti tekste",
                                    tint = if (isSearchActive) MaterialTheme.colorScheme.primary else readerTextColor
                                )
                            }

                            // Pridėti žymą
                            IconButton(
                                onClick = {
                                    val snippet = currentPage?.paragraphs
                                        ?.firstOrNull { !it.isHeading }?.text
                                        ?: currentPage?.paragraphs?.firstOrNull()?.text
                                    viewModel.addCurrentBookmark(currentPageSnippet = snippet)
                                },
                                modifier = Modifier.testTag("reader_add_bookmark_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "Pridėti žymą",
                                    tint = readerTextColor
                                )
                            }

                            // Turinys
                            IconButton(
                                onClick = { viewModel.openTocSheet(true) },
                                modifier = Modifier.testTag("reader_toc_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = "Turinys",
                                    tint = readerTextColor
                                )
                            }

                            // Nustatymai
                            IconButton(
                                onClick = { viewModel.openSettingsSheet(true) },
                                modifier = Modifier.testTag("reader_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Skaitymo nustatymai",
                                    tint = readerTextColor
                                )
                            }
                        }
                    }

                    // In-chapter search bar
                    if (isSearchActive) {
                        val matchingPageIndices = remember(pages, inChapterSearchQuery) {
                            if (inChapterSearchQuery.isBlank()) {
                                emptyList()
                            } else {
                                pages.mapIndexedNotNull { idx, page ->
                                    if (page.paragraphs.any { it.text.contains(inChapterSearchQuery, ignoreCase = true) }) idx else null
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inChapterSearchQuery,
                                onValueChange = { query ->
                                    viewModel.setInChapterSearchQuery(query)
                                    if (query.isNotBlank()) {
                                        val firstMatch = pages.indexOfFirst { p ->
                                            p.paragraphs.any { it.text.contains(query, ignoreCase = true) }
                                        }
                                        if (firstMatch != -1) {
                                            isSlideForward = firstMatch > resolvedPageIndex
                                            currentGlobalPageIndex = firstMatch
                                        }
                                    }
                                },
                                placeholder = { Text("Ieškoti knygoje...", fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("in_chapter_search_input"),
                                trailingIcon = {
                                    if (inChapterSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setInChapterSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Išvalyti")
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = readerSecondaryColor.copy(alpha = 0.5f)
                                )
                            )
                            if (inChapterSearchQuery.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${matchingPageIndices.size} psl.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = readerSecondaryColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM CONTROLS BAR
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = controlOverlayBg,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Psl. ${resolvedPageIndex + 1} iš ${pages.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = readerSecondaryColor
                        )

                        val percent = currentPage?.progressPercent?.toInt() ?: 0
                        Text(
                            text = "$percent% perskaityta",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = readerSecondaryColor
                        )
                    }

                    if (pages.size > 1) {
                        Slider(
                            value = resolvedPageIndex.toFloat(),
                            onValueChange = { target ->
                                val newIdx = target.toInt().coerceIn(0, pages.size - 1)
                                isSlideForward = newIdx >= resolvedPageIndex
                                currentGlobalPageIndex = newIdx
                            },
                            valueRange = 0f..(pages.size - 1).toFloat(),
                            modifier = Modifier.testTag("reader_page_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Font Size: A- and A+
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.decreaseFontSize() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Text("A-", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = readerTextColor)
                            }
                            Text(
                                text = "${settings.fontSizeSp.toInt()} sp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = readerSecondaryColor
                            )
                            IconButton(
                                onClick = { viewModel.increaseFontSize() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Text("A+", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = readerTextColor)
                            }
                        }

                        // Quick Night Mode Toggle & Bookmarks
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val nextTheme = when (settings.theme) {
                                        ReaderTheme.LIGHT -> ReaderTheme.SEPIA
                                        ReaderTheme.SEPIA -> ReaderTheme.NIGHT
                                        ReaderTheme.NIGHT -> ReaderTheme.AMOLED
                                        ReaderTheme.AMOLED -> ReaderTheme.LIGHT
                                    }
                                    viewModel.setReaderTheme(nextTheme)
                                },
                                modifier = Modifier.testTag("quick_theme_toggle")
                            ) {
                                Icon(
                                    imageVector = when (settings.theme) {
                                        ReaderTheme.LIGHT, ReaderTheme.SEPIA -> Icons.Default.LightMode
                                        ReaderTheme.NIGHT, ReaderTheme.AMOLED -> Icons.Default.DarkMode
                                    },
                                    contentDescription = "Temos perjungimas: ${settings.theme.titleLt}",
                                    tint = when (settings.theme) {
                                        ReaderTheme.NIGHT, ReaderTheme.AMOLED -> MaterialTheme.colorScheme.primary
                                        else -> readerTextColor
                                    }
                                )
                            }

                            IconButton(
                                onClick = { viewModel.openBookmarksSheet(true) },
                                modifier = Modifier.testTag("open_bookmarks_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmarks,
                                    contentDescription = "Žymos (${bookmarks.size})",
                                    tint = readerTextColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // Snackbar overlay for bookmark feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (areControlsVisible) 130.dp else 20.dp)
        )

        if (isSettingsSheetOpen) {
            ReaderSettingsSheet(
                settings = settings,
                onDismiss = { viewModel.openSettingsSheet(false) },
                onThemeChange = { viewModel.setReaderTheme(it) },
                onFontSizeChange = { viewModel.setFontSize(it) },
                onFontFamilyChange = { viewModel.setFontFamily(it) },
                onLineSpacingChange = { viewModel.setLineSpacing(it) },
                onJustifyChange = { viewModel.setJustify(it) }
            )
        }

        if (isTocSheetOpen) {
            TableOfContentsSheet(
                bookTitle = book.title,
                chapters = book.chapters,
                currentChapterIndex = currentPage?.chapterIndex ?: 0,
                onDismiss = { viewModel.openTocSheet(false) },
                onChapterSelect = { chIndex ->
                    val targetPage = pages.indexOfFirst { it.chapterIndex == chIndex }
                    if (targetPage != -1) {
                        isSlideForward = targetPage >= resolvedPageIndex
                        currentGlobalPageIndex = targetPage
                    }
                    viewModel.openTocSheet(false)
                }
            )
        }

        if (isBookmarksSheetOpen) {
            BookmarksSheet(
                bookmarks = bookmarks,
                onDismiss = { viewModel.openBookmarksSheet(false) },
                onSelectBookmark = { chIndex ->
                    val targetPage = pages.indexOfFirst { it.chapterIndex == chIndex }
                    if (targetPage != -1) {
                        isSlideForward = targetPage >= resolvedPageIndex
                        currentGlobalPageIndex = targetPage
                    }
                    viewModel.openBookmarksSheet(false)
                },
                onDeleteBookmark = { bookmarkId ->
                    viewModel.deleteBookmark(bookmarkId)
                }
            )
        }
    }
}

@Composable
private fun SingleParagraphView(
    paragraph: ParagraphItem,
    fontSizeSp: Float,
    lineSpacingMultiplier: Float,
    fontFamily: FontFamily,
    textColor: Color,
    justify: Boolean,
    searchQuery: String
) {
    val baseFontSize = if (paragraph.isHeading) {
        when (paragraph.headingLevel) {
            1 -> (fontSizeSp * 1.35f).sp
            2 -> (fontSizeSp * 1.25f).sp
            else -> (fontSizeSp * 1.15f).sp
        }
    } else {
        fontSizeSp.sp
    }

    val lineHeight = (baseFontSize.value * lineSpacingMultiplier).sp

    val annotatedText = remember(paragraph.text, searchQuery) {
        buildAnnotatedString {
            if (searchQuery.isNotBlank() && paragraph.text.contains(searchQuery, ignoreCase = true)) {
                val pattern = Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE)
                var lastIndex = 0
                pattern.findAll(paragraph.text).forEach { match ->
                    append(paragraph.text.substring(lastIndex, match.range.first))
                    withStyle(
                        SpanStyle(
                            background = Color(0xFFFDE047),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(match.value)
                    }
                    lastIndex = match.range.last + 1
                }
                if (lastIndex < paragraph.text.length) {
                    append(paragraph.text.substring(lastIndex))
                }
            } else {
                append(paragraph.text)
            }
        }
    }

    if (paragraph.isQuote) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .background(textColor.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = annotatedText,
                fontSize = baseFontSize,
                lineHeight = lineHeight,
                fontFamily = fontFamily,
                fontStyle = FontStyle.Italic,
                color = textColor.copy(alpha = 0.9f),
                textAlign = if (justify) TextAlign.Justify else TextAlign.Start
            )
        }
    } else {
        Text(
            text = annotatedText,
            fontSize = baseFontSize,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            fontWeight = if (paragraph.isHeading) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = if (paragraph.isHeading) TextAlign.Center else if (justify) TextAlign.Justify else TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (paragraph.isHeading) 6.dp else 1.dp)
        )
    }
}
