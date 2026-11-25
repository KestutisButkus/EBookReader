package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LineSpacing
import com.example.data.ReaderFontFamily
import com.example.data.ReaderSettings
import com.example.data.ReaderTheme
import com.example.ui.theme.ReaderColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onDismiss: () -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontFamilyChange: (ReaderFontFamily) -> Unit,
    onLineSpacingChange: (LineSpacing) -> Unit,
    onJustifyChange: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("reader_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Skaitymo Nustatymai",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. TEMA / NAKTINIS REŽIMAS
            Text(
                text = "Spalvų tema ir naktinis režimas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeCard(
                    theme = ReaderTheme.LIGHT,
                    title = "Šviesus",
                    bgColor = ReaderColors.LightBg,
                    textColor = ReaderColors.LightText,
                    isSelected = settings.theme == ReaderTheme.LIGHT,
                    onClick = { onThemeChange(ReaderTheme.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                ThemeCard(
                    theme = ReaderTheme.SEPIA,
                    title = "Sepija",
                    bgColor = ReaderColors.SepiaBg,
                    textColor = ReaderColors.SepiaText,
                    isSelected = settings.theme == ReaderTheme.SEPIA,
                    onClick = { onThemeChange(ReaderTheme.SEPIA) },
                    modifier = Modifier.weight(1f)
                )
                ThemeCard(
                    theme = ReaderTheme.NIGHT,
                    title = "Naktinis",
                    bgColor = ReaderColors.NightBg,
                    textColor = ReaderColors.NightText,
                    isSelected = settings.theme == ReaderTheme.NIGHT,
                    onClick = { onThemeChange(ReaderTheme.NIGHT) },
                    modifier = Modifier.weight(1f)
                )
                ThemeCard(
                    theme = ReaderTheme.AMOLED,
                    title = "Juodas",
                    bgColor = ReaderColors.AmoledBg,
                    textColor = ReaderColors.AmoledText,
                    isSelected = settings.theme == ReaderTheme.AMOLED,
                    onClick = { onThemeChange(ReaderTheme.AMOLED) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. ŠRIFTO DYDIS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Šrifto dydis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${settings.fontSizeSp.toInt()} sp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onFontSizeChange(settings.fontSizeSp - 2f) },
                    modifier = Modifier.testTag("decrease_font_button")
                ) {
                    Text("A-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Slider(
                    value = settings.fontSizeSp,
                    onValueChange = onFontSizeChange,
                    valueRange = 12f..36f,
                    steps = 11,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("font_size_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                IconButton(
                    onClick = { onFontSizeChange(settings.fontSizeSp + 2f) },
                    modifier = Modifier.testTag("increase_font_button")
                ) {
                    Text("A+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            // Gyva peržiūra
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = when (settings.theme) {
                    ReaderTheme.LIGHT -> ReaderColors.LightBg
                    ReaderTheme.SEPIA -> ReaderColors.SepiaBg
                    ReaderTheme.NIGHT -> ReaderColors.NightBg
                    ReaderTheme.AMOLED -> ReaderColors.AmoledBg
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Lietuvių literatūros klasika ir pasaulio šedevrai tavo delne.",
                    modifier = Modifier.padding(14.dp),
                    fontSize = settings.fontSizeSp.sp,
                    lineHeight = (settings.fontSizeSp * settings.lineSpacing.multiplier).sp,
                    fontFamily = when (settings.fontFamily) {
                        ReaderFontFamily.SERIF -> FontFamily.Serif
                        ReaderFontFamily.SANS_SERIF -> FontFamily.SansSerif
                        ReaderFontFamily.MONOSPACE -> FontFamily.Monospace
                    },
                    textAlign = if (settings.justifyText) TextAlign.Justify else TextAlign.Start,
                    color = when (settings.theme) {
                        ReaderTheme.LIGHT -> ReaderColors.LightText
                        ReaderTheme.SEPIA -> ReaderColors.SepiaText
                        ReaderTheme.NIGHT -> ReaderColors.NightText
                        ReaderTheme.AMOLED -> ReaderColors.AmoledText
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. ŠRIFTO TIPAS
            Text(
                text = "Šrifto šeima",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReaderFontFamily.entries.forEach { family ->
                    FilterChip(
                        selected = settings.fontFamily == family,
                        onClick = { onFontFamilyChange(family) },
                        label = {
                            Text(
                                text = when (family) {
                                    ReaderFontFamily.SERIF -> "Serif"
                                    ReaderFontFamily.SANS_SERIF -> "Sans-Serif"
                                    ReaderFontFamily.MONOSPACE -> "Monospace"
                                },
                                fontFamily = when (family) {
                                    ReaderFontFamily.SERIF -> FontFamily.Serif
                                    ReaderFontFamily.SANS_SERIF -> FontFamily.SansSerif
                                    ReaderFontFamily.MONOSPACE -> FontFamily.Monospace
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. EILUČIŲ TARPAI IR LYGIUOTĖ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Eilučių tarpai",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LineSpacing.entries.forEach { spacing ->
                            FilterChip(
                                selected = settings.lineSpacing == spacing,
                                onClick = { onLineSpacingChange(spacing) },
                                label = { Text(spacing.titleLt, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Lygiuotė",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        IconButton(
                            onClick = { onJustifyChange(false) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (!settings.justifyText) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FormatAlignLeft,
                                contentDescription = "Kairinė lygiuotė",
                                tint = if (!settings.justifyText) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { onJustifyChange(true) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (settings.justifyText) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatAlignJustify,
                                contentDescription = "Abipusė lygiuotė",
                                tint = if (settings.justifyText) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: ReaderTheme,
    title: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}
