package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an imported or bundled EPUB book.
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String,
    val description: String = "",
    val filePath: String,
    val coverImagePath: String? = null,
    val totalChapters: Int = 1,
    val currentChapterIndex: Int = 0,
    val currentScrollOffset: Int = 0,
    val readingProgressPercent: Float = 0f,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val isBundled: Boolean = false
)
