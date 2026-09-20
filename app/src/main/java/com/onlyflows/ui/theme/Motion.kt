package com.onlyflows.ui.theme

import android.provider.Settings
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
