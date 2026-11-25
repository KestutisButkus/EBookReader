package com.example.epub

data class ParagraphItem(
    val text: String,
    val isHeading: Boolean = false,
    val headingLevel: Int = 1,
    val isQuote: Boolean = false
)

data class EpubChapter(
    val id: String,
    val index: Int,
    val title: String,
    val href: String,
    val paragraphs: List<ParagraphItem>,
    val rawText: String
)

data class TocItem(
    val title: String,
    val href: String,
    val chapterIndex: Int
)

data class EpubBook(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val language: String,
    val coverImageBytes: ByteArray? = null,
    val chapters: List<EpubChapter>,
    val tableOfContents: List<TocItem>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EpubBook
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
