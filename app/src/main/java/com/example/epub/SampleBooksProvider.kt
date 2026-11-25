package com.example.epub

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleBooksProvider {

    data class SampleBookData(
        val filename: String,
        val title: String,
        val author: String,
        val description: String,
        val chapters: List<Pair<String, List<String>>> // Title to paragraphs
    )

    private val sampleBooks = listOf(
        SampleBookData(
            filename = "donelaitis_metai.epub",
            title = "Metai",
            author = "Kristijonas Donelaitis",
            description = "Klasikinė lietuvių literatūros poema, vaizduojanti XVIII a. Rytų Prūsijos lietuvių valstiečių būrų gyvenimą, darbus ir papročius keturiais metų laikais.",
            chapters = listOf(
                "Pavasario linksmybės" to listOf(
                    "Jau saulelė vėl atkopdama budino svietą ir žiemos šaltos trūsus pargriaudama juokės.",
                    "Šalčių pramonės su ledais sugaišti pradėjo, ir kėlės pamaži sniegas visur į nieką.",
                    "Orai vėlei atšildami linksmybę dalijo ir visus darbus vasaros pradėti ragino.",
                    "Vyturys linksmas, iš po debesų giedodamas, garbino Dievą ir pavasario džiaugsmus skelbė.",
                    "Gegutė girioj kukavo, paukšteliai maži lizdus suko, o miško žvėrys iš savo guolių džiugiai kėlės.",
                    "Būrai, išsijudinę iš ilgų žiemos miegų, žagres taisė, arklius šėrė ir į laukus arto išskubėjo.",
                    "„Ak,“ tarė Pričkus, „žiūrėkite, broliai, kaip Dievo galybė visą pasaulį iš naujo aprengia!“"
                ),
                "Vasaros darbai" to listOf(
                    "Saulelė aukštai danguje kaitrino žemę, o prakaitas nuo būrų kaktų upeliais bėgo.",
                    "Šienapjūtė prasidėjo ankstyvą rytmetį, kai rasa dar blizgėjo ant žalių pievų žolių.",
                    "Dalgiai skambėjo, dainos aidėjo per laukus, o moterys grėbliais žalią dobilą vartė.",
                    "Sunku buvo trūstis per didžiuosius karščius, bet darbštus žmogus visuomet randa džiaugsmą savo duonoje.",
                    "Rugiai subrendo geltoni kaip auksas, ir pjovėjai su pjautuvais išėjo į laukus rinkti Dievo dovanų."
                ),
                "Rudens gėrybės" to listOf(
                    "Saulelė vėl nuo mūsų tolyn keliauti pradėjo, ir dienos trumpos atnešė vėsaus vėjo gūsius.",
                    "Kluonai pilni javų, rūsiai kvepia obuoliais ir daržovėmis, o krosnyse spragsi sausos malkos.",
                    "Kaimynai susirinko į linksmą talką, dėkodami Viešpačiui už gausų derlių ir sotų stalą.",
                    "Alutis putojo ąsočiuose, ir seni žmonės pasakojo jaunimui senolių išminties pamokas."
                ),
                "Žiemos rūpesčiai" to listOf(
                    "Atėjo rūsti žiema su pūgomis ir speigais, sukaustydama ežerus ir upes storu ledu.",
                    "Būrai trobose verpė, taisė ratus ir kailinius, o vaikai džiaugėsi ledu ir sniego kalnais.",
                    "Nors lauke šaltis spaudė tvoras, šiltoje pirkioje ruseno ugnelė ir šildė viltis apie naują pavasarį."
                )
            )
        ),
        SampleBookData(
            filename = "biliunas_apsakymai.epub",
            title = "Apsakymai: Brisiaus galas ir Kliudžiau",
            author = "Jonas Biliūnas",
            description = "Giliai jautrūs, humanistine dvasia persmelkti Jono Biliūno apsakymai apie atsakomybę, užuojautą ir moralinę sąžinę.",
            chapters = listOf(
                "Brisiaus galas" to listOf(
                    "Negalėjo jau senelis Brisius nei balso atgauti, nei akių pramerkti: gulėjo jis ant šiaudų po tvarto pastoge, visas drebėdamas.",
                    "Seni buvo jo kaulai, dantys iškritę, kailis nušiuręs. O kadaise jis buvo linksmas, greitas šunelis, kurį visi mylėjo ir gerbė.",
                    "Dabar niekam jis nebebuvo reikalingas. Pavargęs, aklas, jis tik laukė, kol kas atneš plutelę duonos.",
                    "Vieną dieną prie jo priėjo šeimininkas su šautuvu. Brisius pajuto pažįstamą kvapą ir linksmai pamojo uodega.",
                    "Jis manė, kad šeimininkas kviečia jį į medžioklę, kaip senais gerais laikais...",
                    "Tyliai nuaidėjo šūvis. Brisius užmerkė akis, nesuprasdamas, už ką jį nubaudė tas, kuriam jis atidavė visą savo gyvenimą."
                ),
                "Kliudžiau" to listOf(
                    "Tai buvo viena iš laimingiausių mano vaikystės dienų. Tėvas man nupirko tikrą lanką su strėlėmis.",
                    "Aš jaučiausi tikras karys ir medžiotojas, ieškantis pavojingų žvėrių kiemo pakraščiuose.",
                    "Staiga ant tvoros pamačiau mažą, beglobę baltą katytę. Ji sėdėjo susigūžusi nuo vėjo, žiūrėdama į mane liūdnomis akimis.",
                    "Nesuvokdamas savo veiksmo žiaurumo, aš įtempiau lanko templę ir paleidau smailią strėlę.",
                    "„Kliudžiau!“ – sušukau išdidžiai. Tačiau pamatęs kenčiančio gyvūnėlio akis, aš paklaikau iš skausmo ir gėdos.",
                    "Katytė numirė, o aš verkiau visą naktį. Tas šūvis kliudė ne tik ją – jis kliudė mano paties širdį, ir tas skausmas išliko visam gyvenimui."
                ),
                "Laimės žiburys" to listOf(
                    "Ant aukšto stačio kalno viršūnės stovėjo puikus stebuklingas žiburys. Seneliai sakė: kas užlips ant to kalno ir palies žiburį, tas padarys visus žmones laimingus.",
                    "Tačiau kalnas buvo baisus: kiekvienas, kuris bandė lipti, pavirsdavo šaltu akmeniu.",
                    "Nepaisydami pavojaus, jauni ir drąsūs žmonės lipo aukštyn. Vienas po kito jie krito ir stingo akmenimis, bet kiti lipo per jų pečius.",
                    "Galiausiai pasiaukojimas nugalėjo – ranka pasiekė žiburį, kalnas sušvito, o akmenys vėl atgijo laisvais ir laimingais žmonėmis."
                )
            )
        ),
        SampleBookData(
            filename = "mazasis_princas_isvalkos.epub",
            title = "Mažasis princas (Ištraukos)",
            author = "Antoine de Saint-Exupéry",
            description = "Filosofinė pasaka apie meilę, draugystę, atsakomybę ir sugebėjimą matyti širdimi tai, kas nematoma akimis.",
            chapters = listOf(
                "Lapė ir paslaptis" to listOf(
                    "– Labas, – tarė lapė.",
                    "– Labas, – mandagiai atsakė mažasis princas, nors apsidairęs nieko nepamatė.",
                    "– Aš čia, po obelim, – pasakė balsas.",
                    "– Kas tu toks? – paklausė mažasis princas. – Tu labai gražus...",
                    "– Aš esu lapė, – atsakė ji.",
                    "– Eikš, pažaisk su manim, – pasiūlė mažasis princas. – Man taip liūdna...",
                    "– Negaliu su tavim žaisti, – tarė lapė. – Aš nesu prijaukinta.",
                    "– O ką reiškia „prijaukinti“? – pasidomėjo princas.",
                    "– Tai reiškia užmegzti ryšius, – paaiškino lapė. – Jei tu mane prisijaukinsi, mes būsime reikalingi vienas kitam. Tu man būsi vienintelis visame pasaulyje. Ir aš tau būsiu vienintelė visame pasaulyje...",
                    "– Štai mano paslaptis. Ji labai paprasta: matyti galima tik širdimi. Tai, kas svarbiausia, akimis nematoma."
                ),
                "Žvaigždžių skambesys" to listOf(
                    "– Žmonės turi savo žvaigždes, tik jos nėra vienodos, – tarė princas.",
                    "Vieniems, tiems, kurie keliauja, žvaigždės rodo kelią. Kitiems jos tėra tik mažos ugnelės danguje.",
                    "– Tu pažvelgsi naktį į dangų, ir kadangi aš gyvensiu vienoje iš jų, kadangi vienoje iš jų juoksiuosi, tau atrodys, tarsi juoktųsi visos žvaigždės. Tu turėsi žvaigždes, kurios moka juoktis!"
                )
            )
        )
    )

    fun ensureSampleBooks(context: Context): List<File> {
        val booksDir = File(context.filesDir, "books")
        if (!booksDir.exists()) booksDir.mkdirs()

        val generatedFiles = mutableListOf<File>()
        for (sample in sampleBooks) {
            val file = File(booksDir, sample.filename)
            if (!file.exists() || file.length() == 0L) {
                createEpubFile(file, sample)
            }
            if (file.exists()) {
                generatedFiles.add(file)
            }
        }
        return generatedFiles
    }

    private fun createEpubFile(file: File, sample: SampleBookData) {
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // 1. mimetype (MUST be first and uncompressed)
            val mimetypeEntry = ZipEntry("mimetype")
            mimetypeEntry.method = ZipEntry.STORED
            val mimeBytes = "application/epub+zip".toByteArray(Charsets.US_ASCII)
            mimetypeEntry.size = mimeBytes.size.toLong()
            mimetypeEntry.compressedSize = mimeBytes.size.toLong()
            val crc = CRC32()
            crc.update(mimeBytes)
            mimetypeEntry.crc = crc.value
            zos.putNextEntry(mimetypeEntry)
            zos.write(mimeBytes)
            zos.closeEntry()

            // 2. META-INF/container.xml
            val containerXml = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>""".trimIndent()
            addZipEntry(zos, "META-INF/container.xml", containerXml.toByteArray(Charsets.UTF_8))

            // Generate chapter items
            val itemEntries = StringBuilder()
            val itemRefs = StringBuilder()
            val ncxNavPoints = StringBuilder()

            sample.chapters.forEachIndexed { idx, (chTitle, paragraphs) ->
                val chFileName = "chapter_${idx + 1}.xhtml"
                val chId = "ch_${idx + 1}"
                itemEntries.append("""    <item id="$chId" href="$chFileName" media-type="application/xhtml+xml"/>""" + "\n")
                itemRefs.append("""    <itemref idref="$chId"/>""" + "\n")
                ncxNavPoints.append("""
    <navPoint id="np_$idx" playOrder="${idx + 1}">
      <navLabel><text>$chTitle</text></navLabel>
      <content src="$chFileName"/>
    </navPoint>""")

                // Write chapter xhtml
                val chapterXhtml = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" lang="lt">
<head>
  <title>$chTitle</title>
</head>
<body>
  <h1>$chTitle</h1>
  ${paragraphs.joinToString("\n  ") { "<p>$it</p>" }}
</body>
</html>""".trimIndent()
                addZipEntry(zos, "OEBPS/$chFileName", chapterXhtml.toByteArray(Charsets.UTF_8))
            }

            // 3. OEBPS/content.opf
            val opfContent = """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookID" version="2.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>${sample.title}</dc:title>
    <dc:creator>${sample.author}</dc:creator>
    <dc:language>lt</dc:language>
    <dc:description>${sample.description}</dc:description>
  </metadata>
  <manifest>
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
$itemEntries  </manifest>
  <spine toc="ncx">
$itemRefs  </spine>
</package>""".trimIndent()
            addZipEntry(zos, "OEBPS/content.opf", opfContent.toByteArray(Charsets.UTF_8))

            // 4. OEBPS/toc.ncx
            val ncxContent = """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="${sample.filename}"/>
    <meta name="dtb:depth" content="1"/>
  </head>
  <docTitle><text>${sample.title}</text></docTitle>
  <navMap>
$ncxNavPoints
  </navMap>
</ncx>""".trimIndent()
            addZipEntry(zos, "OEBPS/toc.ncx", ncxContent.toByteArray(Charsets.UTF_8))
        }
    }

    private fun addZipEntry(zos: ZipOutputStream, path: String, bytes: ByteArray) {
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }
}
