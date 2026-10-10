package com.onlyflows.ui.components

import com.onlyflows.domain.model.Tag

/** A tag folder and its contents; [name] is null for the ungrouped bucket. */
data class TagFolder(val name: String?, val tags: List<Tag>)

/**
 * Splits tags into folders: named folders in alphabetical order, ungrouped tags last.
 * Inside a folder the repository's manual order (sortOrder, then name) is preserved.
 */
fun List<Tag>.byFolder(): List<TagFolder> {
    val grouped = groupBy { it.groupName?.takeIf { n -> n.isNotBlank() } }
    val named = grouped.keys.filterNotNull()
        .sortedBy { it.lowercase() }
        .map { TagFolder(it, grouped.getValue(it)) }
    val loose = grouped[null].orEmpty()
    return if (loose.isEmpty()) named else named + TagFolder(null, loose)
}

/** Every folder name currently in use, alphabetically — used to offer existing folders. */
fun List<Tag>.folderNames(): List<String> =
    mapNotNull { it.groupName?.takeIf { n -> n.isNotBlank() } }
        .distinct()
        .sortedBy { it.lowercase() }

/**
 * Tags matching [query], which is matched against the tag name and its folder name so typing
 * a folder narrows the list to it. A blank query matches everything.
 */
fun List<Tag>.matching(query: String): List<Tag> {
    val q = query.trim().removePrefix("#").lowercase()
    if (q.isEmpty()) return this
    return filter { tag ->
        tag.name.lowercase().contains(q) || tag.groupName?.lowercase()?.contains(q) == true
    }
}

/**
 * Ranks [query] matches for inline `#` completion: prefix matches first, then substring,
 * each alphabetically. Returns at most [limit] tags.
 */
fun List<Tag>.suggestFor(query: String, limit: Int = 6): List<Tag> {
    val q = query.trim().removePrefix("#").lowercase()
    if (q.isEmpty()) return take(limit)
    val prefix = mutableListOf<Tag>()
    val inner  = mutableListOf<Tag>()
    forEach { tag ->
        val name = tag.name.lowercase()
        when {
            name.startsWith(q) -> prefix += tag
            name.contains(q)   -> inner += tag
        }
    }
    return (prefix.sortedBy { it.name.lowercase() } + inner.sortedBy { it.name.lowercase() }).take(limit)
}
