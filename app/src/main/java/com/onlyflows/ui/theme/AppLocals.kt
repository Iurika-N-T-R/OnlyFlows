package com.onlyflows.ui.theme

import androidx.compose.runtime.compositionLocalOf

/** True = 24-hour clock (HH:mm), false = 12-hour (hh:mm a). Default: true. */
val LocalUse24hFormat = compositionLocalOf { true }
