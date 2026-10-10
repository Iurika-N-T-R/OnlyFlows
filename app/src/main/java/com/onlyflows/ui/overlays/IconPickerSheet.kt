package com.onlyflows.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onlyflows.ui.components.PROJECT_EMOJI_GROUPS
import com.onlyflows.ui.components.PROJECT_ICONS
import com.onlyflows.ui.components.PROJECT_ICON_GROUPS
import com.onlyflows.ui.components.iconToken

/**
 * Picks the mark for a list or note: a drawn icon, or an emoji.
 *
 * The field used to be a bare text box labelled "Icon (emoji)", which meant reaching for the
 * system emoji keyboard and hoping. Icons take the project's colour and look the same on every
 * device; emoji stay available because sometimes a specific one is exactly right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(
    selected: String,
    accentColor: Color,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab   by remember { mutableIntStateOf(if (selected.isNotBlank() && !selected.startsWith("mi:")) 1 else 0) }
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().imePadding().padding(bottom = 20.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                Text(
                    text       = "Icon",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f),
                )
                if (selected.isNotBlank()) {
                    TextButton(onClick = { onSelect(""); onDismiss() }) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Done") }
            }

            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Icons") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Emoji") })
            }

            if (tab == 0) {
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    placeholder   = { Text("Search icons…") },
                    leadingIcon   = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon  = if (query.isNotEmpty()) ({
                        IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear") }
                    }) else null,
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            val groups: List<Pair<String, List<String>>> = remember(tab, query) {
                if (tab == 0) {
                    val q = query.trim().lowercase()
                    PROJECT_ICON_GROUPS
                        .map { (name, icons) ->
                            name to if (q.isEmpty()) icons
                            else icons.filter { it.lowercase().contains(q) || name.lowercase().contains(q) }
                        }
                        .filter { it.second.isNotEmpty() }
                } else {
                    PROJECT_EMOJI_GROUPS
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                groups.forEach { (groupName, values) ->
                    item(key = "h_${tab}_$groupName") {
                        Text(
                            text       = groupName.uppercase(),
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier   = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
                        )
                    }
                    values.chunked(5).forEachIndexed { rowIndex, row ->
                        item(key = "r_${tab}_${groupName}_$rowIndex") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            ) {
                                row.forEach { value ->
                                    val token = if (tab == 0) iconToken(value) else value
                                    GlyphCell(
                                        token       = token,
                                        isSelected  = token == selected,
                                        accentColor = accentColor,
                                        onClick     = { onSelect(token) },
                                    )
                                }
                            }
                        }
                    }
                }
                if (groups.isEmpty()) {
                    item(key = "none") {
                        Text(
                            text     = "No icon matches \"${query.trim()}\"",
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlyphCell(
    token: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val vector = PROJECT_ICONS[token.removePrefix("mi:")].takeIf { token.startsWith("mi:") }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .then(
                if (isSelected) Modifier.border(2.dp, accentColor, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
    ) {
        if (vector != null) {
            Icon(
                imageVector        = vector,
                contentDescription = null,
                tint               = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                modifier           = Modifier.size(24.dp),
            )
        } else {
            Text(text = token, fontSize = 22.sp)
        }
    }
}
