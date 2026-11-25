package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReaderTheme(val titleLt: String) {
    LIGHT("Šviesus"),
    SEPIA("Sepija"),
    NIGHT("Naktinis"),
    AMOLED("Juodas")
}

enum class ReaderFontFamily(val titleLt: String) {
    SERIF("Serif (Knyginis)"),
    SANS_SERIF("Sans-Serif (Modernus)"),
    MONOSPACE("Monospace (Griežtas)")
}

enum class LineSpacing(val titleLt: String, val multiplier: Float) {
    COMPACT("Kompaktiškas", 1.35f),
    NORMAL("Vidutinis", 1.65f),
    RELAXED("Erdvus", 1.95f)
}

data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.SEPIA,
    val fontSizeSp: Float = 18f,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.SERIF,
    val lineSpacing: LineSpacing = LineSpacing.NORMAL,
    val justifyText: Boolean = true
)

class ReaderPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ReaderSettings> = _settings.asStateFlow()

    private fun loadSettings(): ReaderSettings {
        val themeName = prefs.getString("reader_theme", ReaderTheme.SEPIA.name) ?: ReaderTheme.SEPIA.name
        val theme = try { ReaderTheme.valueOf(themeName) } catch (_: Exception) { ReaderTheme.SEPIA }

        val fontName = prefs.getString("reader_font", ReaderFontFamily.SERIF.name) ?: ReaderFontFamily.SERIF.name
        val font = try { ReaderFontFamily.valueOf(fontName) } catch (_: Exception) { ReaderFontFamily.SERIF }

        val lineSpacingName = prefs.getString("reader_line_spacing", LineSpacing.NORMAL.name) ?: LineSpacing.NORMAL.name
        val lineSpacing = try { LineSpacing.valueOf(lineSpacingName) } catch (_: Exception) { LineSpacing.NORMAL }

        val fontSize = prefs.getFloat("reader_font_size", 18f)
        val justify = prefs.getBoolean("reader_justify", true)

        return ReaderSettings(
            theme = theme,
            fontSizeSp = fontSize,
            fontFamily = font,
            lineSpacing = lineSpacing,
            justifyText = justify
        )
    }

    fun updateTheme(theme: ReaderTheme) {
        prefs.edit().putString("reader_theme", theme.name).apply()
        _settings.value = _settings.value.copy(theme = theme)
    }

    fun updateFontSize(sizeSp: Float) {
        val clamped = sizeSp.coerceIn(12f, 36f)
        prefs.edit().putFloat("reader_font_size", clamped).apply()
        _settings.value = _settings.value.copy(fontSizeSp = clamped)
    }

    fun updateFontFamily(fontFamily: ReaderFontFamily) {
        prefs.edit().putString("reader_font", fontFamily.name).apply()
        _settings.value = _settings.value.copy(fontFamily = fontFamily)
    }

    fun updateLineSpacing(spacing: LineSpacing) {
        prefs.edit().putString("reader_line_spacing", spacing.name).apply()
        _settings.value = _settings.value.copy(lineSpacing = spacing)
    }

    fun updateJustifyText(justify: Boolean) {
        prefs.edit().putBoolean("reader_justify", justify).apply()
        _settings.value = _settings.value.copy(justifyText = justify)
    }
}
