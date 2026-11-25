package com.example.ui

import android.app.Application
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BookEntity
import com.example.data.BookRepository
import com.example.data.BookmarkEntity
import com.example.data.LineSpacing
import com.example.data.ReaderFontFamily
import com.example.data.ReaderPreferences
import com.example.data.ReaderSettings
import com.example.data.ReaderTheme
import com.example.epub.EpubBook
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = BookRepository(database.bookDao(), application)
    private val preferences = ReaderPreferences(application)

    // Reading settings (night mode, font size, etc.)
    val readerSettings: StateFlow<ReaderSettings> = preferences.settings

    // All books in library
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val booksList: StateFlow<List<BookEntity>> = combine(repository.allBooks, _searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently opened book in reader
    private val _activeBook = MutableStateFlow<EpubBook?>(null)
    val activeBook: StateFlow<EpubBook?> = _activeBook.asStateFlow()

    private val _activeBookEntity = MutableStateFlow<BookEntity?>(null)
    val activeBookEntity: StateFlow<BookEntity?> = _activeBookEntity.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _savedPageIndex = MutableStateFlow(0)
    val savedPageIndex: StateFlow<Int> = _savedPageIndex.asStateFlow()

    private var currentExactProgressPercent: Float = 0f

    // Controls overlay visibility in Reader
    private val _areControlsVisible = MutableStateFlow(true)
    val areControlsVisible: StateFlow<Boolean> = _areControlsVisible.asStateFlow()

    // Sheet dialog states
    private val _isSettingsSheetOpen = MutableStateFlow(false)
    val isSettingsSheetOpen: StateFlow<Boolean> = _isSettingsSheetOpen.asStateFlow()

    private val _isTocSheetOpen = MutableStateFlow(false)
    val isTocSheetOpen: StateFlow<Boolean> = _isTocSheetOpen.asStateFlow()

    private val _isBookmarksSheetOpen = MutableStateFlow(false)
    val isBookmarksSheetOpen: StateFlow<Boolean> = _isBookmarksSheetOpen.asStateFlow()

    // In-chapter search
    private val _inChapterSearchQuery = MutableStateFlow("")
    val inChapterSearchQuery: StateFlow<String> = _inChapterSearchQuery.asStateFlow()

    private val _isSearchInChapterActive = MutableStateFlow(false)
    val isSearchInChapterActive: StateFlow<Boolean> = _isSearchInChapterActive.asStateFlow()

    // Bookmarks reactively scoped to the currently active book entity (prevents collector leaks)
    val bookmarks: StateFlow<List<BookmarkEntity>> = _activeBookEntity
        .flatMapLatest { entity ->
            if (entity != null) repository.getBookmarks(entity.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Status message / snackbar
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Loading indicator
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Text to Speech
    private var textToSpeech: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            repository.initializeSampleBooks()
            _isLoading.value = false
        }

        try {
            textToSpeech = TextToSpeech(application) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.forLanguageTag("lt")
                }
            }
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }
                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        } catch (_: Exception) {}
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openBook(bookId: String, preferredChapterIndex: Int? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            stopSpeech()
            val entity = repository.getBookEntity(bookId)
            val parsedBook = repository.loadBook(bookId)

            if (parsedBook != null && entity != null) {
                val targetChapter = preferredChapterIndex ?: entity.currentChapterIndex
                val maxCh = (parsedBook.chapters.size - 1).coerceAtLeast(0)
                _currentChapterIndex.value = targetChapter.coerceIn(0, maxCh)
                _savedPageIndex.value = if (preferredChapterIndex == null) entity.currentScrollOffset.coerceAtLeast(0) else 0
                currentExactProgressPercent = if (preferredChapterIndex == null) entity.readingProgressPercent else 0f
                _areControlsVisible.value = true
                _inChapterSearchQuery.value = ""
                _isSearchInChapterActive.value = false
                _activeBookEntity.value = entity
                _activeBook.value = parsedBook
            } else {
                _statusMessage.value = "Nepavyko atidaryti knygos"
            }
            _isLoading.value = false
        }
    }

    fun closeBook() {
        stopSpeech()
        saveCurrentProgress()
        _activeBook.value = null
        _activeBookEntity.value = null
        _isSettingsSheetOpen.value = false
        _isTocSheetOpen.value = false
        _isBookmarksSheetOpen.value = false
        _isSearchInChapterActive.value = false
    }

    fun toggleControls() {
        _areControlsVisible.value = !_areControlsVisible.value
    }

    fun updatePageProgress(chapterIndex: Int, progressPercent: Float, pageIndex: Int) {
        _currentChapterIndex.value = chapterIndex
        _savedPageIndex.value = pageIndex
        currentExactProgressPercent = progressPercent
        val book = _activeBook.value ?: return
        val entity = _activeBookEntity.value ?: return
        viewModelScope.launch {
            repository.updateReadingProgress(
                bookId = entity.id,
                chapterIndex = chapterIndex,
                scrollOffset = pageIndex,
                totalChapters = book.chapters.size,
                exactProgressPercent = progressPercent
            )
        }
    }

    private fun saveCurrentProgress() {
        val book = _activeBook.value ?: return
        val entity = _activeBookEntity.value ?: return
        val chIndex = _currentChapterIndex.value
        val pageIdx = _savedPageIndex.value
        val progress = currentExactProgressPercent
        viewModelScope.launch {
            repository.updateReadingProgress(
                bookId = entity.id,
                chapterIndex = chIndex,
                scrollOffset = pageIdx,
                totalChapters = book.chapters.size,
                exactProgressPercent = progress
            )
        }
    }

    // Settings actions
    fun setReaderTheme(theme: ReaderTheme) {
        preferences.updateTheme(theme)
    }

    fun setFontSize(sizeSp: Float) {
        preferences.updateFontSize(sizeSp)
    }

    fun increaseFontSize() {
        preferences.updateFontSize(readerSettings.value.fontSizeSp + 2f)
    }

    fun decreaseFontSize() {
        preferences.updateFontSize(readerSettings.value.fontSizeSp - 2f)
    }

    fun setFontFamily(family: ReaderFontFamily) {
        preferences.updateFontFamily(family)
    }

    fun setLineSpacing(spacing: LineSpacing) {
        preferences.updateLineSpacing(spacing)
    }

    fun setJustify(justify: Boolean) {
        preferences.updateJustifyText(justify)
    }

    // Sheets
    fun openSettingsSheet(open: Boolean) {
        _isSettingsSheetOpen.value = open
    }

    fun openTocSheet(open: Boolean) {
        _isTocSheetOpen.value = open
    }

    fun openBookmarksSheet(open: Boolean) {
        _isBookmarksSheetOpen.value = open
    }

    fun toggleSearchInChapter() {
        _isSearchInChapterActive.value = !_isSearchInChapterActive.value
        if (!_isSearchInChapterActive.value) {
            _inChapterSearchQuery.value = ""
        }
    }

    fun setInChapterSearchQuery(query: String) {
        _inChapterSearchQuery.value = query
    }

    // Bookmarks
    fun addCurrentBookmark(currentPageSnippet: String? = null, note: String = "") {
        val book = _activeBook.value ?: return
        val entity = _activeBookEntity.value ?: return
        val chIndex = _currentChapterIndex.value
        val chapter = book.chapters.getOrNull(chIndex) ?: return

        val snippet = currentPageSnippet?.takeIf { it.isNotBlank() }?.take(140)
            ?: chapter.paragraphs.firstOrNull { !it.isHeading }?.text?.take(140)
            ?: chapter.title

        viewModelScope.launch {
            val bookmark = BookmarkEntity(
                bookId = entity.id,
                chapterIndex = chIndex,
                chapterTitle = chapter.title,
                snippet = snippet,
                note = note
            )
            repository.addBookmark(bookmark)
            _statusMessage.value = "Žyma išsaugota"
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
            _statusMessage.value = "Žyma pašalinta"
        }
    }

    // Import EPUB file
    fun importEpub(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.importBookFromUri(uri)
            if (result.isSuccess) {
                val book = result.getOrNull()
                _statusMessage.value = "Knyga „${book?.title ?: ""}“ sėkmingai pridėta!"
                book?.let { openBook(it.id) }
            } else {
                _statusMessage.value = "Klaida importuojant: ${result.exceptionOrNull()?.localizedMessage ?: "Nežinoma klaida"}"
            }
            _isLoading.value = false
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            if (_activeBookEntity.value?.id == bookId) {
                closeBook()
            }
            repository.deleteBook(bookId)
            _statusMessage.value = "Knyga pašalinta iš bibliotekos"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Speech (TTS)
    fun toggleSpeech(textOverride: String? = null) {
        if (_isSpeaking.value) {
            stopSpeech()
        } else {
            val book = _activeBook.value ?: return
            val chapter = book.chapters.getOrNull(_currentChapterIndex.value) ?: return
            val textToRead = textOverride?.takeIf { it.isNotBlank() }
                ?: chapter.rawText.ifBlank { chapter.paragraphs.joinToString(" ") { it.text } }
            if (textToRead.isNotBlank()) {
                textToSpeech?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "chapter_${_currentChapterIndex.value}")
                _isSpeaking.value = true
            }
        }
    }

    private fun stopSpeech() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopSpeech()
        textToSpeech?.shutdown()
    }
}
