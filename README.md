# 📖 Epub Skaityklė (EPUB Book Reader for Android)

Paprasta, greita ir moderni **EPUB knygų skaityklė**, sukurta su **Kotlin** ir **Jetpack Compose** (Material Design 3), skirta patogiam skaitymui telefonuose ir planšetėse be interneto ryšio.

---

## ✨ Pagrindinės funkcijos

- **📚 Biblioteka ir EPUB importas**:
  - Grynasis Kotlin EPUB analizatorius (`EpubParser`), palaikantis **EPUB 2 (NCX)** ir **EPUB 3 (Nav)** turinius, viršelių atvaizdavimą, skyrių struktūrą ir lietuviškus rašmenis.
  - Lengvas nuosavų `.epub` knygų importavimas tiesiai iš įrenginio saugyklos (be perteklinių saugyklos leidimų, naudojant Android `OpenDocument`).
  - Integruotos lietuvių literatūros ir pasaulio klasikos knygos išbandymui iškart įdiegus (*K. Donelaitis „Metai“*, *J. Biliūnas „Apsakymai“*, *A. de Saint-Exupéry „Mažasis princas“*).
  - Paieška bibliotekoje pagal knygos pavadinimą arba autorių.

- **🌙 Naktinis režimas ir spalvų temos**:
  - **Šviesus** – švelnaus knyginio popieriaus fonas.
  - **Sepija** – šiltas pergamentinis atspalvis, mažinantis akių nuovargį.
  - **Naktinis** – tamsi skalūno tema skaitymui vakare.
  - **Juodas (AMOLED)** – giliai juodas fonas OLED ekranams, maksimaliai taupantis bateriją.
  - Greitas perjungimas vienu paspaudimu apatinėje įrankių juostoje.

- **🔤 Šrifto ir teksto pritaikymas**:
  - Šrifto dydžio reguliavimas nuo **12 sp** iki **36 sp** (slankiklis + greiti **A- / A+** mygtukai) su gyva peržiūra.
  - Šriftų šeimos: **Serif** (knyginis), **Sans-Serif** (modernus), **Monospace**.
  - Eilučių tarpų pasirinkimas (*Kompaktiškas*, *Vidutinis*, *Erdvus*) ir lygiuotė (*Abipusė* arba *Kairinė*).

- **📄 Puslapinis skaitymas ir pilno ekrano ergonomika**:
  - Automatinis teksto puslapiavimas (`EpubPaginator`) pagal ekrano dydį, šriftą ir tarpus – jokio nepatogaus vertikalaus slinkimo.
  - **Puslapių vertimas gestais**:
    - Braukimas **kairėn** arba bakstelėjimas **dešiniajame krašte** ➔ sekantis puslapis.
    - Braukimas **dešinėn** arba bakstelėjimas **kairiajame krašte** ➔ ankstesnis puslapis.
    - Bakstelėjimas **ekrano centre** ➔ parodo arba paslepia valdymo juostas.
  - **Išmanios paraštės (Safe-Area)** pritaikytos šiuolaikiniams telefonams su kameros išpjova (*Galaxy S25* ir kt.): **9%** ekrano aukščio paraštė viršuje ir **4.5%** ekrano pločio šonuose.

- **🔖 Papildomi skaitymo įrankiai**:
  - **Turinys (TOC)** – greitas šuolis į bet kurį skyrių.
  - **Žymos (Bookmarks)** – mėgstamų vietų išsaugojimas su tikslia puslapio citata ir data.
  - **Paieška tekste** – žodžių paieška visoje knygoje su rezultatų paryškinimu.
  - **Skaitymas balsu (TTS)** – integruotas teksto įgarsinimas.
  - **Automatinis pažangos išsaugojimas** – tiksli skaitymo vieta ir procentai išsaugomi lokalioje **Room** duomenų bazėje.

---

## 🛠️ Techninė architektūra

- **Kalba**: Kotlin 2.2
- **UI**: Jetpack Compose + Material Design 3 (`Edge-to-Edge`)
- **Architektūra**: MVVM (`MainViewModel` + `StateFlow` + `BookRepository`)
- **Duomenų saugojimas**: Room Database (`books`, `bookmarks`) + `SharedPreferences` skaitymo nustatymams
- **Vaizdų įkėlimas**: Coil (`coil-compose`)
- **EPUB apdorojimas**: Grynasis `java.util.zip.ZipFile` + `XmlPullParser` + individualus `EpubPaginator`

### Projekto struktūra

```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Pagrindinis aktyvumas su Edge-to-Edge
├── data/
│   ├── AppDatabase.kt               # Room duomenų bazės konfigūracija
│   ├── BookDao.kt                   # SQL užklausos knygoms ir žymoms
│   ├── BookEntity.kt                # Knygos lentelės modelis
│   ├── BookmarkEntity.kt            # Žymos lentelės modelis
│   ├── BookRepository.kt            # Duomenų šaltinis ir knygų kešavimas
│   └── ReaderPreferences.kt         # Temos, šrifto ir tarpų nustatymai
├── epub/
│   ├── EpubBook.kt                  # Knygos, skyriaus ir pastraipos modeliai
│   ├── EpubParser.kt                # EPUB2/EPUB3 failų išskleidimas ir analizė
│   ├── EpubPaginator.kt             # Dinaminis puslapiavimas pagal ekrano matmenis
│   └── SampleBooksProvider.kt       # Pradinių klasikinių EPUB knygų generatorius
└── ui/
    ├── MainViewModel.kt             # Programėlės būsenos valdymas ir TTS
    ├── components/
    │   ├── BookCard.kt              # Knygos kortelė bibliotekoje
    │   ├── BookmarksSheet.kt        # Išsaugotų žymų sąrašas
    │   ├── ReaderSettingsSheet.kt   # Temų, šrifto dydžio ir tarpų nustatymai
    │   └── TableOfContentsSheet.kt  # Knygos turinio langas
    ├── screens/
    │   ├── LibraryScreen.kt         # Bibliotekos ekranas su paieška ir importu
    │   └── ReaderScreen.kt          # Puslapinis skaitymo ekranas
    └── theme/
        ├── Color.kt                 # Spalvų paletės (Šviesus, Sepija, Naktinis, AMOLED)
        ├── Theme.kt                 # Material 3 tema
        └── Type.kt                  # Tipografija
```

---

## 🚀 Kompiliavimas ir testavimas

Reikalavimai:
- **Android Studio** (arba Gradle 9.x su JDK 17/21)
- **Min SDK**: 24 (Android 7.0)
- **Target / Compile SDK**: 36

### Paleidimo komandos

```bash
# Sukompiliuoti Debug APK
gradle :app:assembleDebug

# Paleisti vietinius testus (EPUB analizatoriaus ir puslapiavimo testai)
gradle :app:testDebugUnitTest
```

Sugeneruotas APK failas bus išsaugotas:
`app/build/outputs/apk/debug/app-debug.apk`
