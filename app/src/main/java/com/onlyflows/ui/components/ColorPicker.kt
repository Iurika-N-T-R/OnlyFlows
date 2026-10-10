package com.onlyflows.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/**
 * One palette for everything the user colours. Lists, tags and the rest each carried their own
 * hardcoded strip — 12 colours here, 8 there, 6 somewhere else — so the same project and tag
 * could not be given the same colour, and "the blue one" meant three different blues.
 *
 * Six hues in three shades each. Shades matter more than extra hues: a dozen saturated colours
 * all shout equally loud, and nothing can be made to recede.
 */
val FlowPalette: List<String> = listOf(
    // Red / Orange / Amber
    "#FCA5A5", "#EF4444", "#991B1B",
    "#FDBA74", "#F97316", "#9A3412",
    "#FCD34D", "#F59E0B", "#92400E",
    // Green / Teal
    "#86EFAC", "#10B981", "#065F46",
    "#5EEAD4", "#14B8A6", "#115E59",
    // Blue / Indigo
    "#93C5FD", "#3B82F6", "#1E40AF",
    "#A5B4FC", "#6366F1", "#3730A3",
    // Violet / Pink
    "#C4B5FD", "#8B5CF6", "#5B21B6",
    "#F9A8D4", "#EC4899", "#9D174D",
    // Neutrals — the ones that let everything else stand out
    "#D1D5DB", "#6B7280", "#374151",
)

/** Parses a `#RRGGBB` string, falling back to [fallback] for anything malformed. */
fun parseHexColor(hex: String, fallback: Color = Color(0xFF3B82F6)): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex.trim())) }.getOrDefault(fallback)

fun Color.toHexString(): String =
    String.format("#%06X", 0xFFFFFF and android.graphics.Color.rgb(
        (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(),
    ))

/**
 * Palette grid, the user's saved swatches, and a custom mixer. Hosted by whatever dialog or
 * sheet needs it, so every colour choice in the app looks and behaves the same.
 *
 * [savedColors] and the two callbacks are optional: pass them and the "My colours" row appears.
 */
@Composable
fun ColorPickerPanel(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    savedColors: List<String> = emptyList(),
    onSaveColor: ((String) -> Unit)? = null,
    onRemoveSavedColor: ((String) -> Unit)? = null,
) {
    var showCustom by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ── Saved swatches ───────────────────────────────────────────────────
        if (savedColors.isNotEmpty()) {
            PickerLabel("My colours")
            SwatchGrid(
                colors     = savedColors,
                selected   = selected,
                onSelect   = onSelect,
                onLongPress = onRemoveSavedColor,
            )
        }

        // ── Palette ──────────────────────────────────────────────────────────
        PickerLabel("Palette")
        SwatchGrid(colors = FlowPalette, selected = selected, onSelect = onSelect)

        // ── Custom ───────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showCustom = !showCustom }) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text  = if (showCustom) "Hide custom colour" else "Custom colour",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.weight(1f))
            // Current choice, always visible: the grid selection ring is easy to lose
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(parseHexColor(selected))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(7.dp)),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = selected.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (showCustom) {
            CustomColorMixer(
                selected    = selected,
                onSelect    = onSelect,
                onSaveColor = onSaveColor,
            )
        }
    }
}

@Composable
private fun PickerLabel(text: String) {
    Text(
        text       = text.uppercase(),
        style      = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SwatchGrid(
    colors: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onLongPress: ((String) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.chunked(9).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { hex ->
                    Swatch(
                        hex        = hex,
                        isSelected = hex.equals(selected, ignoreCase = true),
                        onClick    = { onSelect(hex) },
                        onRemove   = onLongPress?.let { remove -> { remove(hex) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun Swatch(
    hex: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    val color = parseHexColor(hex)
    Box(contentAlignment = Alignment.Center) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                )
                .clickable(onClick = onClick),
        ) {
            if (isSelected) {
                Icon(
                    imageVector        = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint               = onColorFor(color),
                    modifier           = Modifier.size(16.dp),
                )
            }
        }
        if (onRemove != null) {
            // Saved swatches are the user's own, so they need a way back out
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onRemove),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Close,
                    contentDescription = "Remove colour",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(9.dp),
                )
            }
        }
    }
}

/** Black or white, whichever stays readable on [background]. */
private fun onColorFor(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.6f) Color.Black else Color.White
}

@Composable
private fun CustomColorMixer(
    selected: String,
    onSelect: (String) -> Unit,
    onSaveColor: ((String) -> Unit)?,
) {
    // Seeded from the current choice so the mixer opens where the user already is
    val initial = remember(selected) {
        FloatArray(3).also {
            android.graphics.Color.colorToHSV(parseHexColor(selected).let { c ->
                android.graphics.Color.rgb((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
            }, it)
        }
    }
    var hue        by remember(selected) { mutableFloatStateOf(initial[0]) }
    var saturation by remember(selected) { mutableFloatStateOf(initial[1]) }
    var value      by remember(selected) { mutableFloatStateOf(initial[2]) }
    var hexDraft   by remember(selected) { mutableStateOf(selected.uppercase()) }

    fun pushHsv() {
        val hex = String.format(
            "#%06X",
            0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)),
        )
        hexDraft = hex
        onSelect(hex)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HueSlider(hue = hue, onHueChange = { hue = it; pushHsv() })

        SliderRow(
            label = "Saturation",
            value = saturation,
            track = Brush.horizontalGradient(
                listOf(
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, value))),
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value))),
                ),
            ),
            onValueChange = { saturation = it; pushHsv() },
        )
        SliderRow(
            label = "Brightness",
            value = value,
            track = Brush.horizontalGradient(
                listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))),
            ),
            onValueChange = { value = it; pushHsv() },
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value         = hexDraft,
                onValueChange = { raw ->
                    hexDraft = raw
                    val candidate = if (raw.startsWith("#")) raw else "#$raw"
                    if (Regex("^#[0-9a-fA-F]{6}$").matches(candidate)) onSelect(candidate.uppercase())
                },
                label      = { Text("Hex") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier   = Modifier.weight(1f),
            )
            if (onSaveColor != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { onSaveColor(selected) }) {
                    Text("Save", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun HueSlider(hue: Float, onHueChange: (Float) -> Unit) {
    Column {
        Text(
            text  = "Hue",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .align(Alignment.Center),
            ) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        (0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) },
                        startX = 0f,
                        endX   = size.width,
                    ),
                    topLeft = Offset.Zero,
                )
            }
            Slider(
                value         = hue,
                onValueChange = onHueChange,
                valueRange    = 0f..360f,
                colors        = SliderDefaults.colors(
                    activeTrackColor   = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor         = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))),
                ),
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    track: Brush,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .align(Alignment.Center),
            ) {
                drawRect(brush = track, topLeft = Offset.Zero)
            }
            Slider(
                value         = value,
                onValueChange = onValueChange,
                valueRange    = 0f..1f,
                colors        = SliderDefaults.colors(
                    activeTrackColor   = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor         = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}
