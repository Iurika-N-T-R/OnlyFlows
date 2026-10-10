package com.onlyflows.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** True when the user has turned system animations off, via developer options or an
 *  accessibility setting. Every animation added to the app must branch on this — motion
 *  a user explicitly disabled is a barrier, not polish.
 *
 *  Read once per composition: the setting requires leaving the app to change, so observing
 *  it continuously would cost more than it's worth. */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Shared timings so motion added in different screens reads as one system. */
object Motion {
    /** Press feedback and other reactions that must feel instant. */
    const val QuickMs = 140

    /** The default for state changes the user is watching: values, colors, sizes. */
    const val StandardMs = 280

    /** Entrances and anything that should carry a little weight. */
    const val EmphasizedMs = 420

    /** M3 emphasized-decelerate: leaves fast, settles slowly. */
    val Decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** A spring with a visible settle, for things that appear rather than change. */
    fun <T> entranceSpring(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness    = Spring.StiffnessMediumLow,
    )
}
