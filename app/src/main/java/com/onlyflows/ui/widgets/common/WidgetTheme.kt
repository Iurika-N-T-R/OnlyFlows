package com.onlyflows.ui.widgets.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.glance.ImageProvider

object WidgetColors {
    val DarkBg        = Color(0xFF1C1C1E)
    val DarkSurface   = Color(0xFF2C2C2E)
    val TextPrimary   = Color.White
    val TextSecondary = Color(0xFF8E8E93)
    val AccentBlue    = Color(0xFF0A84FF)
    val OverdueRed    = Color(0xFFFF3B30)
    val DotDark       = Color(0xFF3A3A3C)
}

fun circleImageProvider(context: Context, color: Color, sizeDp: Float): ImageProvider {
    val density = context.resources.displayMetrics.density
    val sizePx  = (sizeDp * density).toInt().coerceAtLeast(1)
    val bitmap  = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas  = Canvas(bitmap)
    val paint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red   * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue  * 255).toInt(),
        )
    }
    val r = sizePx / 2f
    canvas.drawCircle(r, r, r, paint)
    return ImageProvider(bitmap)
}
