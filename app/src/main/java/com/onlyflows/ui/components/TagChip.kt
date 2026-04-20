package com.onlyflows.ui.components

import androidx.compose.ui.graphics.Color

fun parseTagColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF4A90D9)
}
