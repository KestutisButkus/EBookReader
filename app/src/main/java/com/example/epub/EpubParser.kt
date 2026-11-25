package com.example.epub

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.net.URLDecoder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object EpubParser {

    private val SCRIPT_STYLE_REGEX = Regex("""<(script|style)[^>]*>[\s\S]*?</\1>""", RegexOption.IGNORE_CASE)
    private val HTML_TAG_REGEX = Regex("""<[^>]*>""")
    private val MULTI_WHITESPACE_REGEX = Regex("""\s+""")
    private val NUMERIC_ENTITY_REGEX = Regex("""&#(x?[0-9a-fA-F]+);""")

    /**
     * Parse an EPUB file into [EpubBook].
     */
    fun parse(file: File): EpubBook {
        ZipFile(file).use { zipFile ->
            // 1. Locate rootfile from META-INF/container.xml
            val containerEntry = zipFile.getEntry("META-INF/container.xml")
                ?: throw IllegalArgumentException("Trūksta META-INF/container.xml failo")
            val containerXml = zipFile.getInputStream(containerEntry).bufferedReader().use { it.readText() }
            val opfPath = extractOpfPath(containerXml)
                ?: throw IllegalArgumentException("Nepavyko rasti OPF failo kelio")

            // Base folder of OPF
            val opfFolder = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

            val opfEntry = findZipEntry(zipFile, opfPath)
                ?: throw IllegalArgumentException("OPF failas nerastas: $opfPath")
            val opfXml = zipFile.getInputStream(opfEntry).bufferedReader().use { it.readText() }

            // 2. Parse OPF: metadata, manifest, spine
            val opfData = parseOpf(opfXml)

            // 3. Extract cover image if present
            var coverBytes: ByteArray? = null
            opfData.coverHref?.let { href ->
                val fullCoverPath = resolvePath(opfFolder, href)
                val coverEntry = findZipEntry(zipFile, fullCoverPath) ?: findZipEntry(zipFile, href)
                coverEntry?.let { entry ->
                    coverBytes = zipFile.getInputStream(entry).use { it.readBytes() }
                }
            }

            // 4. Parse Table of Contents (EPUB2 NCX or EPUB3 Nav)
            val tocMap = mutableMapOf<String, String>()
            opfData.ncxHref?.let { ncxHref ->
                val fullNcxPath = resolvePath(opfFolder, ncxHref)
                val ncxEntry = findZipEntry(zipFile, fullNcxPath) ?: findZipEntry(zipFile, ncxHref)
                ncxEntry?.let { entry ->
                    val ncxXml = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                    parseNcx(ncxXml).forEach { (href, label) ->
                        tocMap[href] = label
                    }
                }
            }
            if (tocMap.isEmpty() && opfData.navHref != null) {
                val fullNavPath = resolvePath(opfFolder, opfData.navHref)
                val navEntry = findZipEntry(zipFile, fullNavPath) ?: findZipEntry(zipFile, opfData.navHref)
                navEntry?.let { entry ->
                    val navXml = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                    parseNavHtml(navXml).forEach { (href, label) ->
                        tocMap[href] = label
                    }
                }
            }

            // 5. Read Chapters in spine order
            val chapters = mutableListOf<EpubChapter>()
            val tocList = mutableListOf<TocItem>()

            opfData.spineItemHrefs.forEachIndexed { index, href ->
                val fullPath = resolvePath(opfFolder, href)
                val chapterEntry = findZipEntry(zipFile, fullPath) ?: findZipEntry(zipFile, href)

                if (chapterEntry != null) {
                    val htmlContent = zipFile.getInputStream(chapterEntry).bufferedReader().use { it.readText() }
                    val cleanHref = href.substringAfterLast("/")
                    val decodedHref = decodeUrlSafe(href)
                    val detectedTitle = tocMap[href]
                        ?: tocMap[decodedHref]
                        ?: tocMap[cleanHref]
                        ?: extractTitleFromHtml(htmlContent)
                        ?: "Skyrius ${index + 1}"

                    val paragraphs = parseHtmlToParagraphs(htmlContent)
                    if (paragraphs.isNotEmpty()) {
                        val rawText = paragraphs.joinToString("\n\n") { it.text }
                        val chapterIndex = chapters.size
                        val chapter = EpubChapter(
                            id = "ch_$chapterIndex",
                            index = chapterIndex,
                            title = detectedTitle,
                            href = href,
                            paragraphs = paragraphs,
                            rawText = rawText
                        )
                        chapters.add(chapter)
                        tocList.add(TocItem(title = detectedTitle, href = href, chapterIndex = chapterIndex))
                    }
                }
            }

            val finalTitle = opfData.title.ifBlank { file.nameWithoutExtension }
            val finalAuthor = opfData.author.ifBlank { "Nežinomas autorius" }

            return EpubBook(
                id = file.nameWithoutExtension.lowercase().replace("[^a-z0-9_]".toRegex(), "_"),
                title = finalTitle,
                author = finalAuthor,
                description = opfData.description,
                language = opfData.language,
                coverImageBytes = coverBytes,
                chapters = chapters,
                tableOfContents = tocList
            )
        }
    }

    private fun findZipEntry(zipFile: ZipFile, path: String): ZipEntry? {
        val clean = path.substringBefore("#")
        return zipFile.getEntry(clean)
            ?: zipFile.getEntry(decodeUrlSafe(clean))
            ?: zipFile.getEntry(clean.replace("%20", " "))
    }

    private fun decodeUrlSafe(value: String): String {
        return try {
            URLDecoder.decode(value, "UTF-8")
        } catch (_: Exception) {
            value
        }
    }

    private fun resolvePath(baseFolder: String, href: String): String {
        val cleanHref = href.substringBefore("#")
        return if (cleanHref.startsWith("/")) {
            cleanHref.substring(1)
        } else if (baseFolder.isEmpty()) {
            cleanHref
        } else {
            val parts = (baseFolder + cleanHref).split("/")
            val resolved = mutableListOf<String>()
            for (p in parts) {
                when {
                    p == ".." -> if (resolved.isNotEmpty()) resolved.removeAt(resolved.size - 1)
                    p != "." && p.isNotEmpty() -> resolved.add(p)
                }
            }
            resolved.joinToString("/")
        }
    }

    private fun extractOpfPath(containerXml: String): String? {
        val regex = """<rootfile[^>]*full-path=["']([^"']+)["']""".toRegex()
        return regex.find(containerXml)?.groupValues?.get(1)
    }

    private data class OpfResult(
        val title: String,
        val author: String,
        val description: String,
        val language: String,
        val coverHref: String?,
        val ncxHref: String?,
        val navHref: String?,
        val spineItemHrefs: List<String>
    )

    private fun parseOpf(opfXml: String): OpfResult {
        var title = ""
        var author = ""
        var description = ""
        var language = "lt"
        var coverId: String? = null
        var coverHref: String? = null
        var ncxHref: String? = null
        var navHref: String? = null

        val manifestItems = mutableMapOf<String, String>() // id to href
        val spineList = mutableListOf<String>() // list of item ids

        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(opfXml))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name.lowercase()
                    when {
                        tagName.endsWith("title") -> {
                            val t = parser.nextText().trim()
                            if (t.isNotBlank() && title.isBlank()) title = t
                        }
                        tagName.endsWith("creator") -> {
                            val a = parser.nextText().trim()
                            if (a.isNotBlank() && author.isBlank()) author = a
                        }
                        tagName.endsWith("description") -> {
                            val d = parser.nextText().trim()
                            if (d.isNotBlank() && description.isBlank()) description = d
                        }
                        tagName.endsWith("language") -> {
                            val l = parser.nextText().trim()
                            if (l.isNotBlank()) language = l
                        }
                        tagName == "meta" -> {
                            val name = parser.getAttributeValue(null, "name")
                            val content = parser.getAttributeValue(null, "content")
                            if (name == "cover" && !content.isNullOrBlank()) {
                                coverId = content
                            }
                        }
                        tagName == "item" -> {
                            val id = parser.getAttributeValue(null, "id")
                            val href = parser.getAttributeValue(null, "href")
                            val mediaType = parser.getAttributeValue(null, "media-type")
                            val properties = parser.getAttributeValue(null, "properties")

                            if (!id.isNullOrBlank() && !href.isNullOrBlank()) {
                                manifestItems[id] = href

                                if (properties?.contains("cover-image") == true) {
                                    coverId = id
                                }
                                if (properties?.contains("nav") == true) {
                                    navHref = href
                                }
                                if (mediaType == "application/x-dtbncx+xml") {
                                    ncxHref = href
                                }
                            }
                        }
                        tagName == "itemref" -> {
                            val idref = parser.getAttributeValue(null, "idref")
                            if (!idref.isNullOrBlank()) {
                                spineList.add(idref)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Pull parser exception, will fallback to regex below
        }

        if (title.isBlank()) {
            title = """<(?:dc:)?title[^>]*>([^<]+)</(?:dc:)?title>""".toRegex(RegexOption.IGNORE_CASE).find(opfXml)?.groupValues?.get(1)?.trim() ?: ""
        }
        if (author.isBlank()) {
            author = """<(?:dc:)?creator[^>]*>([^<]+)</(?:dc:)?creator>""".toRegex(RegexOption.IGNORE_CASE).find(opfXml)?.groupValues?.get(1)?.trim() ?: ""
        }
        if (description.isBlank()) {
            description = """<(?:dc:)?description[^>]*>([^<]+)</(?:dc:)?description>""".toRegex(RegexOption.IGNORE_CASE).find(opfXml)?.groupValues?.get(1)?.trim() ?: ""
        }

        // Regex fallback for manifest items if parser missed any
        if (manifestItems.isEmpty()) {
            val itemRegex1 = """<item[^>]*\bid=["']([^"']+)["'][^>]*\bhref=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
            itemRegex1.findAll(opfXml).forEach { m ->
                manifestItems[m.groupValues[1]] = m.groupValues[2]
            }
            val itemRegex2 = """<item[^>]*\bhref=["']([^"']+)["'][^>]*\bid=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
            itemRegex2.findAll(opfXml).forEach { m ->
                manifestItems[m.groupValues[2]] = m.groupValues[1]
            }
        }

        // Regex fallback for spine items
        if (spineList.isEmpty()) {
            val itemrefRegex = """<itemref[^>]*\bidref=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
            itemrefRegex.findAll(opfXml).forEach { m ->
                spineList.add(m.groupValues[1])
            }
        }

        // Resolve cover href
        if (coverId != null) {
            coverHref = manifestItems[coverId]
        }
        if (coverHref == null) {
            val potentialCover = manifestItems.entries.firstOrNull { (id, href) ->
                (id.contains("cover", ignoreCase = true) || href.contains("cover", ignoreCase = true)) &&
                        (href.endsWith(".jpg", true) || href.endsWith(".jpeg", true) || href.endsWith(".png", true) || href.endsWith(".webp", true))
            }
            coverHref = potentialCover?.value
        }

        val spineHrefs = spineList.mapNotNull { idref -> manifestItems[idref] }
        val finalSpineHrefs = if (spineHrefs.isNotEmpty()) {
            spineHrefs
        } else {
            manifestItems.values.filter { it.endsWith(".html", true) || it.endsWith(".xhtml", true) || it.endsWith(".htm", true) }
        }

        return OpfResult(
            title = stripHtml(title),
            author = stripHtml(author),
            description = stripHtml(description),
            language = language,
            coverHref = coverHref,
            ncxHref = ncxHref,
            navHref = navHref,
            spineItemHrefs = finalSpineHrefs
        )
    }

    private fun parseNcx(ncxXml: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val navPointRegex = """<navPoint[\s\S]*?<text>([\s\S]*?)</text>[\s\S]*?<content[^>]*src=["']([^"']+)["']""".toRegex()
            navPointRegex.findAll(ncxXml).forEach { match ->
                val text = stripHtml(match.groupValues[1])
                val src = match.groupValues[2].substringBefore("#")
                if (text.isNotBlank() && src.isNotBlank()) {
                    result[src] = text
                    result[decodeUrlSafe(src)] = text
                    result[src.substringAfterLast("/")] = text
                }
            }
        } catch (_: Exception) { }
        return result
    }

    private fun parseNavHtml(navXml: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val linkRegex = """<a[^>]*href=["']([^"']+)["'][^>]*>([\s\S]*?)</a>""".toRegex(RegexOption.IGNORE_CASE)
            linkRegex.findAll(navXml).forEach { match ->
                val href = match.groupValues[1].substringBefore("#")
                val text = stripHtml(match.groupValues[2])
                if (text.isNotBlank() && href.isNotBlank()) {
                    result[href] = text
                    result[decodeUrlSafe(href)] = text
                    result[href.substringAfterLast("/")] = text
                }
            }
        } catch (_: Exception) { }
        return result
    }

    private fun extractTitleFromHtml(html: String): String? {
        val h1Match = """<h[12][^>]*>([\s\S]*?)</h[12]>""".toRegex(RegexOption.IGNORE_CASE).find(html)
        if (h1Match != null) {
            val candidate = stripHtml(h1Match.groupValues[1])
            if (candidate.isNotBlank()) return candidate
        }

        val titleMatch = """<title[^>]*>([\s\S]*?)</title>""".toRegex(RegexOption.IGNORE_CASE).find(html)
        if (titleMatch != null) {
            val candidate = stripHtml(titleMatch.groupValues[1])
            if (candidate.isNotBlank()) return candidate
        }
        return null
    }

    internal fun parseHtmlToParagraphs(html: String): List<ParagraphItem> {
        val paragraphs = mutableListOf<ParagraphItem>()
        val withoutStylesAndScripts = SCRIPT_STYLE_REGEX.replace(html, "")

        val bodyContent = if (withoutStylesAndScripts.contains("<body", ignoreCase = true)) {
            val body = withoutStylesAndScripts.substringAfter("<body", "").substringAfter(">", "")
            if (body.contains("</body>", ignoreCase = true)) body.substringBefore("</body>", body) else body
        } else {
            withoutStylesAndScripts
        }

        // First match semantic block tags (headings, p, blockquote, li) so outer wrapper <div>s don't swallow inner <p>s
        val semanticBlockRegex = """<(h[1-6]|p|blockquote|li)[^>]*>([\s\S]*?)</\1>""".toRegex(RegexOption.IGNORE_CASE)
        val semanticMatches = semanticBlockRegex.findAll(bodyContent).toList()

        if (semanticMatches.isNotEmpty()) {
            for (match in semanticMatches) {
                val tag = match.groupValues[1].lowercase()
                val innerHtml = match.groupValues[2]
                val cleanText = stripHtml(innerHtml)
                if (cleanText.isNotBlank()) {
                    val isHeading = tag.startsWith("h")
                    val headingLevel = if (isHeading) tag[1].digitToIntOrNull() ?: 1 else 1
                    val isQuote = tag == "blockquote"
                    paragraphs.add(
                        ParagraphItem(
                            text = cleanText,
                            isHeading = isHeading,
                            headingLevel = headingLevel,
                            isQuote = isQuote
                        )
                    )
                }
            }
        } else {
            // Fallback to <div> blocks if no <p>/<h*> tags are used
            val divRegex = """<div[^>]*>([\s\S]*?)</div>""".toRegex(RegexOption.IGNORE_CASE)
            val divMatches = divRegex.findAll(bodyContent).toList()
            if (divMatches.isNotEmpty()) {
                for (match in divMatches) {
                    val cleanText = stripHtml(match.groupValues[1])
                    if (cleanText.isNotBlank()) {
                        paragraphs.add(ParagraphItem(text = cleanText))
                    }
                }
            }
        }

        // Final fallback: split by <br> or double newlines
        if (paragraphs.isEmpty()) {
            val withNewlines = bodyContent.replace("""<br\s*/?>""".toRegex(RegexOption.IGNORE_CASE), "\n")
            withNewlines.split("\n\n", "\n").forEach { line ->
                val cleaned = stripHtml(line)
                if (cleaned.isNotBlank()) {
                    paragraphs.add(ParagraphItem(text = cleaned))
                }
            }
        }

        return paragraphs
    }

    internal fun stripHtml(input: String): String {
        return try {
            val noTags = HTML_TAG_REGEX.replace(input, " ")
            val decodedNamed = noTags
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("&ndash;", "–")
                .replace("&mdash;", "—")
                .replace("&hellip;", "…")
                .replace("&laquo;", "«")
                .replace("&raquo;", "»")
                .replace("&lsquo;", "‘")
                .replace("&rsquo;", "’")
                .replace("&ldquo;", "“")
                .replace("&rdquo;", "”")
                .replace("&bdquo;", "„")
                .replace("\u00A0", " ")

            val decodedNumeric = NUMERIC_ENTITY_REGEX.replace(decodedNamed) { match ->
                val raw = match.groupValues[1]
                val codePoint = if (raw.startsWith("x", ignoreCase = true)) {
                    raw.substring(1).toIntOrNull(16)
                } else {
                    raw.toIntOrNull(10)
                }
                if (codePoint != null && Character.isValidCodePoint(codePoint)) {
                    String(Character.toChars(codePoint))
                } else {
                    match.value
                }
            }

            MULTI_WHITESPACE_REGEX.replace(decodedNumeric, " ").trim()
        } catch (_: Exception) {
            input.trim()
        }
    }
}
