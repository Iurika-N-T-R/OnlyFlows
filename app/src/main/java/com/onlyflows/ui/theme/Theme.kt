package com.onlyflows.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Custom color set exposed via CompositionLocal ─────────────────────────────

data class FlowColors(
    val agendaCardBg: Color,
    val modalSurface: Color,
    val dateBadgeDarkBg: Color,
    val dateBadgeDarkFg: Color,
    val checklistCircle: Color,
    val flagRed: Color,
    val starGold: Color,
    val moonBlue: Color,
    val sectionHeader: Color,
    val outline: Color,
    val outlineVariant: Color,
    val tagChipBg: Color,
    val tagChipFg: Color,
    val dateBadgeOverdueBg: Color,
    val dateBadgeOverdueFg: Color,
    val dateBadgeTodayBg: Color,
    val dateBadgeTodayFg: Color,
    val dateBadgeFutureBg: Color,
    val dateBadgeFutureFg: Color,
    val isDark: Boolean,
)

val LocalFlowColors = staticCompositionLocalOf {
    FlowColors(
        agendaCardBg        = AgendaCardBgLight,
        modalSurface        = Color(0xFF1E2635),
        dateBadgeDarkBg     = DateBadgeDarkBg,
        dateBadgeDarkFg     = DateBadgeDarkFg,
        checklistCircle     = ChecklistCircleOutline,
        flagRed             = FlowRed,
        starGold            = FlowGold,
        moonBlue            = FlowMoon,
        sectionHeader       = FlowBlue,
        outline             = OutlineLight,
        outlineVariant      = OutlineVariantLight,
        tagChipBg           = TagChipBgLight,
        tagChipFg           = TagChipFgLight,
        dateBadgeOverdueBg  = DateBadgeOverdueBg,
        dateBadgeOverdueFg  = DateBadgeOverdueFg,
        dateBadgeTodayBg    = DateBadgeTodayBg,
        dateBadgeTodayFg    = DateBadgeTodayFg,
        dateBadgeFutureBg   = DateBadgeFutureBg,
        dateBadgeFutureFg   = DateBadgeFutureFg,
        isDark              = false,
    )
}

private val LightColorScheme = lightColorScheme(
    primary           = FlowBlue,
    onPrimary         = Color.White,
    primaryContainer  = FlowBlueLight,
    onPrimaryContainer= FlowBlueDark,
    secondary         = FlowGold,
    tertiary          = FlowGreen,
    background        = SurfaceLight,
    surface           = SurfaceLight,
    surfaceVariant    = SurfaceContainerLight,
    surfaceContainerLowest  = SurfaceLight,
    surfaceContainerLow     = SurfaceContainerLowLight,
    surfaceContainer        = SurfaceContainerLight,
    surfaceContainerHigh    = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    surfaceBright           = SurfaceLight,
    surfaceDim              = SurfaceDimLight,
    onBackground      = OnSurfaceLight,
    onSurface         = OnSurfaceLight,
    onSurfaceVariant  = OnSurfaceVariantLight,
    outline           = OutlineLight,
    outlineVariant    = OutlineVariantLight,
    error             = FlowRed,
)

private val DarkColorScheme = darkColorScheme(
    primary           = FlowBlue,
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFF1A3050),
    onPrimaryContainer= FlowBlueLight,
    secondary         = FlowGold,
    tertiary          = FlowGreen,
    background        = SurfaceDark,
    surface           = SurfaceCardDark,
    surfaceVariant    = SurfaceContainerDark,
    surfaceContainerLowest  = SurfaceContainerLowestDark,
    surfaceContainerLow     = SurfaceDark,
    surfaceContainer        = SurfaceContainerDark,
    surfaceContainerHigh    = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceBright           = SurfaceContainerHighestDark,
    surfaceDim              = SurfaceContainerLowestDark,
    onBackground      = OnSurfaceDark,
    onSurface         = OnSurfaceDark,
    onSurfaceVariant  = OnSurfaceVariantDark,
    outline           = OutlineDark,
    outlineVariant    = OutlineVariantDark,
    error             = FlowRed,
)

@Composable
fun OnlyFlowsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val flowColors = if (darkTheme) {
        FlowColors(
            agendaCardBg        = AgendaCardBgDark,
            modalSurface        = ModalDark,
            dateBadgeDarkBg     = DateBadgeDarkBg,
            dateBadgeDarkFg     = DateBadgeDarkFg,
            checklistCircle     = ChecklistCircleOutline,
            flagRed             = FlowRed,
            starGold            = FlowGold,
            moonBlue            = FlowMoon,
            sectionHeader       = FlowBlue,
            outline             = OutlineDark,
            outlineVariant      = OutlineVariantDark,
            tagChipBg           = TagChipBgDark,
            tagChipFg           = TagChipFgDark,
            dateBadgeOverdueBg  = DateBadgeOverdueDarkBg,
            dateBadgeOverdueFg  = DateBadgeOverdueDarkFg,
            dateBadgeTodayBg    = DateBadgeTodayDarkBg,
            dateBadgeTodayFg    = DateBadgeTodayDarkFg,
            dateBadgeFutureBg   = DateBadgeFutureDarkBg,
            dateBadgeFutureFg   = DateBadgeFutureDarkFg,
            isDark              = true,
        )
    } else {
        FlowColors(
            agendaCardBg        = AgendaCardBgLight,
            modalSurface        = ModalDark,
            dateBadgeDarkBg     = DateBadgeDarkBg,
            dateBadgeDarkFg     = DateBadgeDarkFg,
            checklistCircle     = ChecklistCircleOutline,
            flagRed             = FlowRed,
            starGold            = FlowGold,
            moonBlue            = FlowMoon,
            sectionHeader       = FlowBlue,
            outline             = OutlineLight,
            outlineVariant      = OutlineVariantLight,
            tagChipBg           = TagChipBgLight,
            tagChipFg           = TagChipFgLight,
            dateBadgeOverdueBg  = DateBadgeOverdueBg,
            dateBadgeOverdueFg  = DateBadgeOverdueFg,
            dateBadgeTodayBg    = DateBadgeTodayBg,
            dateBadgeTodayFg    = DateBadgeTodayFg,
            dateBadgeFutureBg   = DateBadgeFutureBg,
            dateBadgeFutureFg   = DateBadgeFutureFg,
            isDark              = false,
        )
    }

    CompositionLocalProvider(LocalFlowColors provides flowColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = FlowTypography,
            content     = content,
        )
    }
}

// Shortcut: MaterialTheme.flowColors
val MaterialTheme.flowColors: FlowColors
    @Composable get() = LocalFlowColors.current
