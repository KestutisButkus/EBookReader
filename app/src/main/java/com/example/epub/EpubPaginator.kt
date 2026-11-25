package com.example.epub

import com.example.data.ReaderFontFamily
import kotlin.math.floor
import kotlin.math.max

data class ReaderPage(
    val chapterIndex: Int,
    val chapterTitle: String,
    val pageIndexInChapter: Int,
    val totalPagesInChapter: Int,
    val globalPageIndex: Int,
    val totalGlobalPages: Int,
    val paragraphs: List<ParagraphItem>,
    val progressPercent: Float
)

object EpubPaginator {

    private const val PARAGRAPH_SPACING_DP = 12f
    private const val HEADING_EXTRA_VERTICAL_DP = 14f

    /**
     * Splits a book's chapters into pages based on available content dimensions and typography settings.
     * Uses height-accurate dp estimation so pages never clip on tall or dialogue-heavy chapters.
     */
    fun paginate(
        book: EpubBook,
        availableWidthDp: Float,
        availableHeightDp: Float,
        fontSizeSp: Float,
        lineSpacingMultiplier: Float,
        fontFamily: ReaderFontFamily = ReaderFontFamily.SERIF
    ): List<ReaderPage> {
        if (book.chapters.isEmpty()) return emptyList()

        // Safe usable height with a small margin so wrapped lines never overflow the bottom edge
        val safeHeightDp = max(120f, availableHeightDp * 0.90f)
        val charWidthFactor = when (fontFamily) {
            ReaderFontFamily.SERIF -> 0.53f
            ReaderFontFamily.SANS_SERIF -> 0.51f
            ReaderFontFamily.MONOSPACE -> 0.65f
        }

        val bodyLineHeightDp = fontSizeSp * lineSpacingMultiplier
        val bodyCharWidthDp = fontSizeSp * charWidthFactor
        val charsPerBodyLine = max(14, floor(availableWidthDp / bodyCharWidthDp).toInt())
        val maxBodyLinesPerPage = max(4, floor(safeHeightDp / bodyLineHeightDp).toInt())
        val maxCharsPerPage = (charsPerBodyLine * maxBodyLinesPerPage * 0.85f).toInt().coerceAtLeast(80)

        val chapterPagesList = mutableListOf<List<List<ParagraphItem>>>()

        for (chapter in book.chapters) {
            val chPages = mutableListOf<List<ParagraphItem>>()
            var currentPageParagraphs = mutableListOf<ParagraphItem>()
            var currentHeightDp = 0f

            for (p in chapter.paragraphs) {
                val pFontSize = if (p.isHeading) {
                    when (p.headingLevel) {
                        1 -> fontSizeSp * 1.35f
                        2 -> fontSizeSp * 1.25f
                        else -> fontSizeSp * 1.15f
                    }
                } else {
                    fontSizeSp
                }

                val pLineHeightDp = pFontSize * lineSpacingMultiplier
                val pCharWidthDp = pFontSize * charWidthFactor
                val pCharsPerLine = max(12, floor(availableWidthDp / pCharWidthDp).toInt())
                val pLines = max(1, (p.text.length + pCharsPerLine - 1) / pCharsPerLine)
                val extraPadding = if (p.isHeading) HEADING_EXTRA_VERTICAL_DP else 2f
                val pHeightDp = (pLines * pLineHeightDp) + extraPadding

                // If a single body paragraph is taller than a whole page, split it cleanly into chunks
                if (pHeightDp > safeHeightDp && !p.isHeading) {
                    if (currentPageParagraphs.isNotEmpty()) {
                        chPages.add(currentPageParagraphs)
                        currentPageParagraphs = mutableListOf()
                        currentHeightDp = 0f
                    }
                    val chunks = splitTextIntoChunks(p.text, maxCharsPerPage)
                    for (chunk in chunks) {
                        chPages.add(listOf(p.copy(text = chunk)))
                    }
                    continue
                }

                val spacingBefore = if (currentPageParagraphs.isNotEmpty()) PARAGRAPH_SPACING_DP else 0f
                if (currentHeightDp + spacingBefore + pHeightDp > safeHeightDp && currentPageParagraphs.isNotEmpty()) {
                    chPages.add(currentPageParagraphs)
                    currentPageParagraphs = mutableListOf()
                    currentHeightDp = 0f
                }

                val addedSpacing = if (currentPageParagraphs.isNotEmpty()) PARAGRAPH_SPACING_DP else 0f
                currentPageParagraphs.add(p)
                currentHeightDp += addedSpacing + pHeightDp
            }

            if (currentPageParagraphs.isNotEmpty()) {
                chPages.add(currentPageParagraphs)
            }

            if (chPages.isEmpty()) {
                chPages.add(listOf(ParagraphItem(text = chapter.title, isHeading = true)))
            }

            chapterPagesList.add(chPages)
        }

        val totalGlobalPages = chapterPagesList.sumOf { it.size }
        val pages = ArrayList<ReaderPage>(totalGlobalPages)
        var globalIndex = 0

        for ((chIndex, chPages) in chapterPagesList.withIndex()) {
            val chTitle = book.chapters.getOrNull(chIndex)?.title ?: "Skyrius ${chIndex + 1}"
            val totalInCh = chPages.size

            for ((pageInCh, paragraphs) in chPages.withIndex()) {
                val progress = if (totalGlobalPages > 1) {
                    (globalIndex.toFloat() / (totalGlobalPages - 1).toFloat() * 100f).coerceIn(0f, 100f)
                } else 100f

                pages.add(
                    ReaderPage(
                        chapterIndex = chIndex,
                        chapterTitle = chTitle,
                        pageIndexInChapter = pageInCh,
                        totalPagesInChapter = totalInCh,
                        globalPageIndex = globalIndex,
                        totalGlobalPages = totalGlobalPages,
                        paragraphs = paragraphs,
                        progressPercent = progress
                    )
                )
                globalIndex++
            }
        }

        return pages
    }

    private fun splitTextIntoChunks(text: String, maxChars: Int): List<String> {
        val chunks = mutableListOf<String>()
        var remaining = text.trim()

        while (remaining.isNotEmpty()) {
            if (remaining.length <= maxChars) {
                chunks.add(remaining)
                break
            }

            var splitPos = -1
            val searchWindow = remaining.substring(0, maxChars)
            val sentenceEnders = listOf(". ", "! ", "? ", ";\n", "\n")
            for (ender in sentenceEnders) {
                val idx = searchWindow.lastIndexOf(ender)
                if (idx > splitPos && idx > maxChars / 2) {
                    splitPos = idx + ender.length
                }
            }

            if (splitPos == -1) {
                val spaceIdx = searchWindow.lastIndexOf(" ")
                splitPos = if (spaceIdx > maxChars / 3) spaceIdx + 1 else maxChars
            }

            chunks.add(remaining.substring(0, splitPos).trim())
            remaining = remaining.substring(splitPos).trim()
        }

        return chunks
    }
}
