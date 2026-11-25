package com.example

import com.example.data.LineSpacing
import com.example.data.ReaderFontFamily
import com.example.data.ReaderTheme
import com.example.epub.EpubPaginator
import com.example.epub.EpubParser
import com.example.epub.SampleBooksProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EpubParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testGenerateAndParseEpub() {
        val sample = SampleBooksProvider.SampleBookData(
            filename = "test_book.epub",
            title = "Testinė Knyga",
            author = "Lietuvių Autorius",
            description = "Testinis aprašymas",
            chapters = listOf(
                "Pirmas skyrius" to listOf(
                    "Pirma pastraipa su įdomiu turiniu.",
                    "Antra pastraipa tęsia pasakojimą."
                ),
                "Antras skyrius" to listOf(
                    "Trečia pastraipa antrame skyriuje."
                )
            )
        )

        val epubFile = tempFolder.newFile("test_book.epub")
        val method = SampleBooksProvider.javaClass.getDeclaredMethod(
            "createEpubFile",
            File::class.java,
            SampleBooksProvider.SampleBookData::class.java
        )
        method.isAccessible = true
        method.invoke(SampleBooksProvider, epubFile, sample)

        assertTrue("EPUB failas turi egzistuoti ir turėti turinį", epubFile.exists() && epubFile.length() > 0)

        val parsedBook = EpubParser.parse(epubFile)
        assertNotNull(parsedBook)
        assertEquals("Testinė Knyga", parsedBook.title)
        assertEquals("Lietuvių Autorius", parsedBook.author)
        assertEquals(2, parsedBook.chapters.size)
        assertEquals(3, parsedBook.chapters[0].paragraphs.size)
        assertTrue(parsedBook.chapters[0].paragraphs[0].isHeading)
        assertEquals("Pirmas skyrius", parsedBook.chapters[0].paragraphs[0].text)
        assertEquals("Pirma pastraipa su įdomiu turiniu.", parsedBook.chapters[0].paragraphs[1].text)
        assertEquals("Antras skyrius", parsedBook.chapters[1].title)
    }

    @Test
    fun testHtmlNestedDivsAndEntities() {
        val html = """
            <html>
            <head><style>p { color: red; }</style></head>
            <body>
                <div class="wrapper">
                    <h1>Skyriaus &bdquo;Pavadinimas&ldquo;</h1>
                    <p>Pirma pastraipa su &#8211; brūkšniu.</p>
                    <p>Antra pastraipa &amp; simboliai.</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        val items = EpubParser.parseHtmlToParagraphs(html)
        assertEquals(3, items.size)
        assertTrue(items[0].isHeading)
        assertEquals("Skyriaus „Pavadinimas“", items[0].text)
        assertFalse(items[1].text.contains("color: red"))
        assertEquals("Pirma pastraipa su – brūkšniu.", items[1].text)
        assertEquals("Antra pastraipa & simboliai.", items[2].text)
    }

    @Test
    fun testReaderThemesAndTypography() {
        assertEquals("Šviesus", ReaderTheme.LIGHT.titleLt)
        assertEquals("Sepija", ReaderTheme.SEPIA.titleLt)
        assertEquals("Naktinis", ReaderTheme.NIGHT.titleLt)
        assertEquals("Juodas", ReaderTheme.AMOLED.titleLt)

        assertTrue(LineSpacing.COMPACT.multiplier < LineSpacing.NORMAL.multiplier)
        assertTrue(LineSpacing.NORMAL.multiplier < LineSpacing.RELAXED.multiplier)

        assertEquals("Serif (Knyginis)", ReaderFontFamily.SERIF.titleLt)
    }

    @Test
    fun testPaginatorSplitsIntoPages() {
        val sample = SampleBooksProvider.SampleBookData(
            filename = "test_paginator.epub",
            title = "Testas",
            author = "Autorius",
            description = "Aprasymas",
            chapters = listOf(
                "Skyrius 1" to listOf(
                    "Pirma pastraipa teksto.",
                    "Antra pastraipa teksto su daugiau žodžių.",
                    "Trečia pastraipa teksto."
                )
            )
        )
        val epubFile = tempFolder.newFile("test_paginator.epub")
        val method = SampleBooksProvider.javaClass.getDeclaredMethod(
            "createEpubFile",
            File::class.java,
            SampleBooksProvider.SampleBookData::class.java
        )
        method.isAccessible = true
        method.invoke(SampleBooksProvider, epubFile, sample)

        val book = EpubParser.parse(epubFile)
        val pages = EpubPaginator.paginate(
            book = book,
            availableWidthDp = 340f,
            availableHeightDp = 600f,
            fontSizeSp = 18f,
            lineSpacingMultiplier = 1.6f,
            fontFamily = ReaderFontFamily.SERIF
        )
        assertTrue("Puslapių turi būti bent 1", pages.isNotEmpty())
        assertEquals("Skyrius 1", pages[0].chapterTitle)
        assertTrue("Pastraipų puslapyje turi būti", pages[0].paragraphs.isNotEmpty())
    }
}
