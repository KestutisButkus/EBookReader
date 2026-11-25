package com.example.data

import android.content.Context
import android.net.Uri
import com.example.epub.EpubBook
import com.example.epub.EpubParser
import com.example.epub.SampleBooksProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BookRepository(
    private val bookDao: BookDao,
    private val context: Context
) {
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    // Thread-safe in-memory cache for parsed EpubBook to avoid re-parsing on recomposition
    private val bookCache = ConcurrentHashMap<String, EpubBook>()

    suspend fun initializeSampleBooks() = withContext(Dispatchers.IO) {
        val sampleFiles = SampleBooksProvider.ensureSampleBooks(context)
        for (file in sampleFiles) {
            try {
                val bookId = "sample_" + file.nameWithoutExtension
                val existing = bookDao.getBookById(bookId)
                if (existing == null) {
                    val parsed = EpubParser.parse(file)
                    val coverPath = saveCoverImage(bookId, parsed.coverImageBytes)
                    val entity = BookEntity(
                        id = bookId,
                        title = parsed.title,
                        author = parsed.author,
                        description = parsed.description,
                        filePath = file.absolutePath,
                        coverImagePath = coverPath,
                        totalChapters = parsed.chapters.size.coerceAtLeast(1),
                        currentChapterIndex = 0,
                        currentScrollOffset = 0,
                        readingProgressPercent = 0f,
                        lastReadTimestamp = System.currentTimeMillis(),
                        isBundled = true
                    )
                    bookDao.insertBook(entity)
                    bookCache[bookId] = parsed
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun importBookFromUri(uri: Uri): Result<BookEntity> = withContext(Dispatchers.IO) {
        val booksDir = File(context.filesDir, "books")
        if (!booksDir.exists()) booksDir.mkdirs()

        val randomId = "imported_" + UUID.randomUUID().toString().take(8)
        val destFile = File(booksDir, "$randomId.epub")

        try {
            // Copy content from URI
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalArgumentException("Nepavyko atverti pasirinkto failo"))

            val parsed = EpubParser.parse(destFile)
            if (parsed.chapters.isEmpty()) {
                destFile.delete()
                return@withContext Result.failure(IllegalArgumentException("EPUB faile nerasta skaitomų skyrių"))
            }

            val coverPath = saveCoverImage(randomId, parsed.coverImageBytes)

            val entity = BookEntity(
                id = randomId,
                title = parsed.title,
                author = parsed.author,
                description = parsed.description,
                filePath = destFile.absolutePath,
                coverImagePath = coverPath,
                totalChapters = parsed.chapters.size.coerceAtLeast(1),
                currentChapterIndex = 0,
                currentScrollOffset = 0,
                readingProgressPercent = 0f,
                lastReadTimestamp = System.currentTimeMillis(),
                isBundled = false
            )

            bookDao.insertBook(entity)
            bookCache[randomId] = parsed
            Result.success(entity)
        } catch (e: Exception) {
            if (destFile.exists()) destFile.delete()
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun loadBook(bookId: String): EpubBook? = withContext(Dispatchers.IO) {
        bookCache[bookId]?.let { return@withContext it }

        val entity = bookDao.getBookById(bookId) ?: return@withContext null
        val file = File(entity.filePath)
        if (!file.exists()) return@withContext null

        try {
            val parsed = EpubParser.parse(file)
            bookCache[bookId] = parsed
            parsed
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getBookEntity(bookId: String): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getBookById(bookId)
    }

    suspend fun updateReadingProgress(
        bookId: String,
        chapterIndex: Int,
        scrollOffset: Int,
        totalChapters: Int,
        exactProgressPercent: Float? = null
    ) = withContext(Dispatchers.IO) {
        val total = totalChapters.coerceAtLeast(1)
        val progress = (exactProgressPercent ?: ((chapterIndex.toFloat() / total.toFloat()) * 100f))
            .coerceIn(0f, 100f)
        bookDao.updateReadingProgress(
            bookId = bookId,
            chapterIndex = chapterIndex,
            scrollOffset = scrollOffset,
            progressPercent = progress
        )
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        val entity = bookDao.getBookById(bookId)
        if (entity != null) {
            try {
                val file = File(entity.filePath)
                if (file.exists()) file.delete()
                entity.coverImagePath?.let {
                    val cover = File(it)
                    if (cover.exists()) cover.delete()
                }
            } catch (_: Exception) {}
            bookDao.deleteBookmarksForBook(bookId)
            bookDao.deleteBookById(bookId)
            bookCache.remove(bookId)
        }
    }

    // Bookmarks
    fun getBookmarks(bookId: String): Flow<List<BookmarkEntity>> = bookDao.getBookmarksForBook(bookId)

    suspend fun addBookmark(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        bookDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmarkId: Long) = withContext(Dispatchers.IO) {
        bookDao.deleteBookmark(bookmarkId)
    }

    private fun saveCoverImage(bookId: String, bytes: ByteArray?): String? {
        if (bytes == null || bytes.isEmpty()) return null
        return try {
            val coversDir = File(context.filesDir, "covers")
            if (!coversDir.exists()) coversDir.mkdirs()
            val coverFile = File(coversDir, "cover_$bookId.jpg")
            FileOutputStream(coverFile).use { it.write(bytes) }
            coverFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
