package com.onlyflows.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A list or note mark: either an emoji or a drawn icon.
 *
 * Emoji alone could not carry a set that reads as one family — they come from the system font,
 * ignore the project colour and look different on every device. Icons do, but emoji are faster
 * to reach for, so both are allowed.
 *
 * Icons are stored in the existing `iconEmoji` column as `mi:<name>`, which needs no migration
 * and syncs over LAN like any other field. Anything without that prefix is treated as an emoji.
 */
private const val ICON_PREFIX = "mi:"

fun isIconToken(value: String): Boolean = value.startsWith(ICON_PREFIX)

fun iconToken(name: String): String = "$ICON_PREFIX$name"

fun iconTokenName(value: String): String = value.removePrefix(ICON_PREFIX)

/** Resolves a stored token to its vector, or null when it is an emoji or unknown. */
fun resolveProjectIcon(value: String): ImageVector? =
    if (isIconToken(value)) PROJECT_ICONS[iconTokenName(value)] else null

/**
 * Draws [value] as an icon or an emoji. Renders nothing when it is blank, so callers keep
 * deciding what their own fallback is.
 */
@Composable
fun ProjectGlyph(
    value: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 18.dp,
    emojiSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier,
) {
    if (value.isBlank()) return
    val vector = resolveProjectIcon(value)
    if (vector != null) {
        Icon(
            imageVector        = vector,
            contentDescription = null,
            tint               = tint,
            modifier           = modifier.size(size),
        )
    } else {
        Text(
            text     = value,
            style    = LocalTextStyle.current.copy(fontSize = emojiSize),
            modifier = modifier,
        )
    }
}

/** A plain-text stand-in for places that can only render a string (initials, previews). */
fun glyphAsText(value: String, fallback: String): String =
    if (value.isBlank() || isIconToken(value)) fallback else value

// ── The curated set ───────────────────────────────────────────────────────────
// Material icons are already bundled, so this costs no download and no new assets. A short
// curated list beats exposing two thousand icons: the point is to tell lists apart at a
// glance, and a set small enough to scan does that better than a set you have to search.

/** Display groups, in the order the picker shows them. */
val PROJECT_ICON_GROUPS: List<Pair<String, List<String>>> = listOf(
    "Work" to listOf(
        "Work", "Business", "Assignment", "Description", "Analytics", "Campaign",
        "Handshake", "Groups", "Schedule", "Task",
    ),
    "Study" to listOf(
        "School", "MenuBook", "AutoStories", "Science", "Calculate", "Translate",
        "Lightbulb", "Psychology", "EmojiObjects", "Draw",
    ),
    "Home" to listOf(
        "Home", "Bed", "Chair", "Kitchen", "CleaningServices", "LocalLaundryService",
        "Pets", "Yard", "Build", "Plumbing",
    ),
    "Life" to listOf(
        "FavoriteBorder", "FitnessCenter", "DirectionsRun", "SelfImprovement", "Spa",
        "Restaurant", "LocalCafe", "ShoppingCart", "Savings", "CreditCard",
    ),
    "Travel" to listOf(
        "Flight", "DirectionsCar", "Train", "Hiking", "Map", "Place",
        "BeachAccess", "Luggage", "Hotel", "Explore",
    ),
    "Media" to listOf(
        "MusicNote", "Headphones", "Movie", "PhotoCamera", "Palette", "SportsEsports",
        "Mic", "Brush", "Theaters", "Podcasts",
    ),
    "Tech" to listOf(
        "Code", "Terminal", "Computer", "PhoneIphone", "Storage", "CloudQueue",
        "BugReport", "Memory", "Settings", "Dns",
    ),
    "Marks" to listOf(
        "StarBorder", "Flag", "Bookmark", "Label", "PushPin", "Bolt",
        "CheckCircle", "Inventory2", "Folder", "Inbox",
    ),
)

/** Name → vector. Built from the groups so the two can never drift apart. */
val PROJECT_ICONS: Map<String, ImageVector> = mapOf(
    "Work" to Icons.Outlined.Work,
    "Business" to Icons.Outlined.Business,
    "Assignment" to Icons.Outlined.Assignment,
    "Description" to Icons.Outlined.Description,
    "Analytics" to Icons.Outlined.Analytics,
    "Campaign" to Icons.Outlined.Campaign,
    "Handshake" to Icons.Outlined.Handshake,
    "Groups" to Icons.Outlined.Groups,
    "Schedule" to Icons.Outlined.Schedule,
    "Task" to Icons.Outlined.Task,

    "School" to Icons.Outlined.School,
    "MenuBook" to Icons.Outlined.MenuBook,
    "AutoStories" to Icons.Outlined.AutoStories,
    "Science" to Icons.Outlined.Science,
    "Calculate" to Icons.Outlined.Calculate,
    "Translate" to Icons.Outlined.Translate,
    "Lightbulb" to Icons.Outlined.Lightbulb,
    "Psychology" to Icons.Outlined.Psychology,
    "EmojiObjects" to Icons.Outlined.EmojiObjects,
    "Draw" to Icons.Outlined.Draw,

    "Home" to Icons.Outlined.Home,
    "Bed" to Icons.Outlined.Bed,
    "Chair" to Icons.Outlined.Chair,
    "Kitchen" to Icons.Outlined.Kitchen,
    "CleaningServices" to Icons.Outlined.CleaningServices,
    "LocalLaundryService" to Icons.Outlined.LocalLaundryService,
    "Pets" to Icons.Outlined.Pets,
    "Yard" to Icons.Outlined.Yard,
    "Build" to Icons.Outlined.Build,
    "Plumbing" to Icons.Outlined.Plumbing,

    "FavoriteBorder" to Icons.Outlined.FavoriteBorder,
    "FitnessCenter" to Icons.Outlined.FitnessCenter,
    "DirectionsRun" to Icons.Outlined.DirectionsRun,
    "SelfImprovement" to Icons.Outlined.SelfImprovement,
    "Spa" to Icons.Outlined.Spa,
    "Restaurant" to Icons.Outlined.Restaurant,
    "LocalCafe" to Icons.Outlined.LocalCafe,
    "ShoppingCart" to Icons.Outlined.ShoppingCart,
    "Savings" to Icons.Outlined.Savings,
    "CreditCard" to Icons.Outlined.CreditCard,

    "Flight" to Icons.Outlined.Flight,
    "DirectionsCar" to Icons.Outlined.DirectionsCar,
    "Train" to Icons.Outlined.Train,
    "Hiking" to Icons.Outlined.Hiking,
    "Map" to Icons.Outlined.Map,
    "Place" to Icons.Outlined.Place,
    "BeachAccess" to Icons.Outlined.BeachAccess,
    "Luggage" to Icons.Outlined.Luggage,
    "Hotel" to Icons.Outlined.Hotel,
    "Explore" to Icons.Outlined.Explore,

    "MusicNote" to Icons.Outlined.MusicNote,
    "Headphones" to Icons.Outlined.Headphones,
    "Movie" to Icons.Outlined.Movie,
    "PhotoCamera" to Icons.Outlined.PhotoCamera,
    "Palette" to Icons.Outlined.Palette,
    "SportsEsports" to Icons.Outlined.SportsEsports,
    "Mic" to Icons.Outlined.Mic,
    "Brush" to Icons.Outlined.Brush,
    "Theaters" to Icons.Outlined.Theaters,
    "Podcasts" to Icons.Outlined.Podcasts,

    "Code" to Icons.Outlined.Code,
    "Terminal" to Icons.Outlined.Terminal,
    "Computer" to Icons.Outlined.Computer,
    "PhoneIphone" to Icons.Outlined.PhoneIphone,
    "Storage" to Icons.Outlined.Storage,
    "CloudQueue" to Icons.Outlined.CloudQueue,
    "BugReport" to Icons.Outlined.BugReport,
    "Memory" to Icons.Outlined.Memory,
    "Settings" to Icons.Outlined.Settings,
    "Dns" to Icons.Outlined.Dns,

    "StarBorder" to Icons.Outlined.StarBorder,
    "Flag" to Icons.Outlined.Flag,
    "Bookmark" to Icons.Outlined.Bookmark,
    "Label" to Icons.Outlined.Label,
    "PushPin" to Icons.Outlined.PushPin,
    "Bolt" to Icons.Outlined.Bolt,
    "CheckCircle" to Icons.Outlined.CheckCircle,
    "Inventory2" to Icons.Outlined.Inventory2,
    "Folder" to Icons.Outlined.Folder,
    "Inbox" to Icons.Outlined.Inbox,
)

/** Emoji offered next to the icons, grouped the same way. */
val PROJECT_EMOJI_GROUPS: List<Pair<String, List<String>>> = listOf(
    "Work"   to listOf("💼", "📊", "📈", "🗂️", "📝", "📅", "🤝", "🏢", "⏰", "✅"),
    "Study"  to listOf("🎓", "📚", "📖", "🔬", "🧮", "🌍", "💡", "🧠", "✏️", "🖊️"),
    "Home"   to listOf("🏠", "🛏️", "🪑", "🍳", "🧹", "🧺", "🐾", "🪴", "🔧", "🚿"),
    "Life"   to listOf("❤️", "🏋️", "🏃", "🧘", "💆", "🍽️", "☕", "🛒", "💰", "💳"),
    "Travel" to listOf("✈️", "🚗", "🚆", "🥾", "🗺️", "📍", "🏖️", "🧳", "🏨", "🧭"),
    "Media"  to listOf("🎵", "🎧", "🎬", "📷", "🎨", "🎮", "🎤", "🖌️", "🍿", "🎙️"),
    "Tech"   to listOf("💻", "⌨️", "🖥️", "📱", "💾", "☁️", "🐞", "🔌", "⚙️", "🌐"),
    "Marks"  to listOf("⭐", "🚩", "🔖", "🏷️", "📌", "⚡", "🎯", "📦", "📁", "📥"),
)
