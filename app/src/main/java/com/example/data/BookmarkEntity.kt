package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing user bookmarks / favorite quotes.
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val snippet: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
