package com.onlyflows.data.smartadd

import com.onlyflows.domain.model.ProjectType
import com.onlyflows.domain.model.TaskPriority
import com.onlyflows.domain.smartadd.Batch
import com.onlyflows.domain.smartadd.ContainerDraft
import com.onlyflows.domain.smartadd.NaturalLanguage
import com.onlyflows.domain.smartadd.NoteDraft
import com.onlyflows.domain.smartadd.TaskDraft
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/**
 * Reads JSON, CSV / TSV and plain text or Markdown into a [Batch].
 * The kind of each item (task, project, checklist, notes list) comes from an explicit "type"
 * when there is one, else from its shape: children make a container, strings under a
 * "shopping"/"checklist" heading make a checklist, paragraphs under "Notes" make notes.
 */
object FileImporter {

    fun parse(text: String, fileName: String = ""): Batch {
        val body = text.removePrefix("﻿").trim()
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext == "json" || body.startsWith("{") || body.startsWith("[")) {
            runCatching { return json(body) }
        }
        if (ext == "csv" || ext == "tsv" || looksLikeCsv(body)) return csv(body)
        return plain(body)
    }

    // ── shared helpers ────────────────────────────────────────────────────────

    /** "Échéance" → "echeance": keys and columns match without case, accents, spaces or _ -. */
    private fun key(s: String) = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("[\\s_-]+"), "")

    /** French names accepted for each English field name (compared through [key]). */
    private val FRENCH = mapOf(
        "title" to listOf("titre", "intitule", "libelle"), "name" to listOf("nom"), "task" to listOf("tache"),
        "text" to listOf("texte"), "content" to listOf("contenu"), "body" to listOf("corps"), "summary" to listOf("resume"),
        "description" to listOf("descriptif", "remarque", "remarques", "commentaire"), "details" to listOf("detail"),
        "due" to listOf("echeance", "pourle"), "deadline" to listOf("datelimite", "delai"), "start" to listOf("debut", "datedebut"),
        "time" to listOf("heure"), "duetime" to listOf("heureecheance"),
        "priority" to listOf("priorite", "importance"),
        "completed" to listOf("termine", "terminee", "fini", "finie", "coche", "faite"), "done" to listOf("fait"), "status" to listOf("statut", "etat"),
        "checklist" to listOf("souslist", "soustaches", "soustache"), "subtasks" to listOf("soustaches"), "steps" to listOf("etapes"),
        "tags" to listOf("etiquettes", "etiquette", "mots-cles", "motscles"), "labels" to listOf("libelles"),
        "section" to listOf("rubrique"), "heading" to listOf("titre-section"), "group" to listOf("groupe"),
        "recurrence" to listOf("repetition", "frequence"), "repeat" to listOf("repeter"), "every" to listOf("chaque"),
        "reminder" to listOf("rappel"), "remindat" to listOf("rappelle"),
        "tasks" to listOf("taches"), "items" to listOf("elements", "articles"), "entries" to listOf("entrees"), "children" to listOf("enfants"),
        "projects" to listOf("projets"), "lists" to listOf("listes"), "notebooks" to listOf("carnets"), "sections" to listOf("rubriques"),
        "project" to listOf("projet"), "list" to listOf("liste"), "parent" to listOf("dossier"), "folder" to listOf("dossier"),
        "type" to listOf("genre"), "kind" to listOf("sorte"),
    )

    /** The English name, then its French equivalents, all through [key]. */
    private fun names(k: String) = listOf(key(k)) + FRENCH[key(k)].orEmpty().map(::key)

    private fun kindOf(word: String): ProjectType? = word.lowercase(Locale.getDefault()).let {
        when {
            it.contains("note") || it.contains("journal") || it.contains("idea") || it.contains("idée") || it.contains("carnet") -> ProjectType.NOTE
            it.contains("checklist") || it.contains("list") || it.contains("shopping") || it.contains("grocer") || it.contains("courses") -> ProjectType.LIST
            it.contains("project") || it.contains("projet") -> ProjectType.PROJECT
            else -> null
        }
    }

    private fun priority(v: String?): TaskPriority = when (v?.let(::key)) {
        null, "", "none", "aucune", "0", "4", "p4" -> TaskPriority.NONE
        "high", "haute", "elevee", "eleve", "forte", "urgent", "urgente", "1", "p1", "!!!", "h" -> TaskPriority.HIGH
        "medium", "moyenne", "normal", "normale", "2", "p2", "!!", "m" -> TaskPriority.MEDIUM
        "low", "basse", "faible", "3", "p3", "!", "l" -> TaskPriority.LOW
        else -> TaskPriority.NONE
    }

    private fun truthy(v: String?) = v?.let(::key) in setOf(
        "true", "yes", "y", "1", "x", "done", "completed", "complete", "checked",
        "vrai", "oui", "o", "fait", "faite", "termine", "terminee", "fini", "finie", "coche", "cochee", "acheve",
    )

    /** "2026-11-03", "2026-11-03T09:30", "03/11/2026" or words ("next friday"). */
    private fun dateTime(v: String?): Pair<LocalDate?, LocalTime?> {
        val s = v?.trim().orEmpty()
        if (s.isEmpty()) return null to null
        runCatching { val dt = LocalDateTime.parse(s.removeSuffix("Z").take(19)); return dt.toLocalDate() to dt.toLocalTime() }
        runCatching { return LocalDate.parse(s.take(10)) to null }
        val t = NaturalLanguage.task("x $s")
        return t.dueDate to t.dueTime
    }

    private fun time(v: String?): LocalTime? = v?.trim()?.takeIf { it.isNotEmpty() }?.let { s ->
        runCatching { LocalTime.parse(if (s.length == 4) "0$s" else s.take(5)) }.getOrNull() ?: NaturalLanguage.task("x at $s").dueTime
    }

    private fun splitList(v: String?): List<String> = v?.split(Regex("\\s*[;|\\n]\\s*|\\s*,\\s*"))?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    /** Collects containers by title, keeping the order they first appear in. */
    private class Containers {
        val map = LinkedHashMap<String, ContainerDraft>()
        fun get(title: String, type: ProjectType?): ContainerDraft {
            val k = title.trim().lowercase(Locale.getDefault())
            val c = map[k] ?: ContainerDraft(title.trim(), type ?: kindOf(title) ?: ProjectType.PROJECT)
            return (if (type != null && map[k] != null && c.tasks.isEmpty() && c.notes.isEmpty()) c.copy(type = type) else c).also { map[k] = it }
        }
        fun put(c: ContainerDraft) { map[c.title.trim().lowercase(Locale.getDefault())] = c }
        fun list() = map.values.toList()
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    /** The object's own key for [k] (any case, accents, or French name), or null. */
    private fun JSONObject.find(k: String): String? {
        val wanted = names(k)
        return keys().asSequence().firstOrNull { key(it) in wanted }
    }

    private fun JSONObject.str(vararg keys: String): String? = keys.firstNotNullOfOrNull { k ->
        find(k)?.takeIf { !isNull(it) }?.let { opt(it) }?.takeIf { it !is JSONArray && it !is JSONObject }?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun JSONObject.arr(vararg keys: String): JSONArray? = keys.firstNotNullOfOrNull { k -> find(k)?.let { optJSONArray(it) } }

    private fun JSONArray.items() = (0 until length()).map { opt(it) }

    private fun strings(a: JSONArray?): List<String> = a?.items()?.mapNotNull { v ->
        when (v) { is JSONObject -> v.str("title", "name", "text", "content", "item"); null -> null; else -> v.toString() }
    }?.filter { it.isNotBlank() }.orEmpty()

    private fun tagsOf(o: JSONObject): List<String> = o.arr("tags", "labels")?.let(::strings) ?: splitList(o.str("tags", "labels"))

    private fun jsonTask(v: Any?, section: String? = null): TaskDraft? {
        if (v !is JSONObject) return v?.toString()?.takeIf { it.isNotBlank() }?.let { NaturalLanguage.task(it).copy(section = section) }
        val title = v.str("title", "name", "task", "text", "content", "summary") ?: return null
        var (date, t) = dateTime(v.str("due", "dueDate", "due_date", "date", "deadline", "start", "scheduled"))
        time(v.str("time", "dueTime", "due_time"))?.let { t = it }
        val recurrence = v.str("recurrence", "repeat", "every", "rrule")?.let { NaturalLanguage.task("x ${if (it.startsWith("every", true)) it else "every $it"}").recurrence }
        val reminder = v.str("reminder", "remindAt", "remind_at", "alarm")?.let { r -> dateTime(r).let { (d, tm) -> d?.let { LocalDateTime.of(it, tm ?: LocalTime.of(9, 0)) } } }
        return TaskDraft(
            title = title,
            note = v.str("description", "notes", "note", "details", "body", "desc").orEmpty(),
            dueDate = date, dueTime = t,
            priority = priority(v.str("priority", "prio", "importance")),
            recurrence = recurrence,
            completed = truthy(v.str("completed", "done", "isCompleted", "checked", "status")),
            checklist = strings(v.arr("checklist", "subtasks", "checkItems", "items", "steps")),
            reminderAt = reminder,
            tagNames = tagsOf(v),
            section = v.str("section", "heading", "group") ?: section,
        )
    }

    private fun jsonNote(v: Any?): NoteDraft? = when (v) {
        is JSONObject -> {
            val title = v.str("title", "name").orEmpty()
            val content = v.str("content", "text", "body", "note").orEmpty()
            if (title.isBlank() && content.isBlank()) null else NoteDraft(title.ifBlank { content.lineSequence().first().take(60) }, if (title.isBlank()) content.substringAfter('\n', "") else content)
        }
        null -> null
        else -> v.toString().takeIf { it.isNotBlank() }?.let { NoteDraft(it) }
    }

    private fun isContainer(o: JSONObject): Boolean {
        val type = o.str("type", "kind").orEmpty().lowercase(Locale.ROOT)
        if (type.isNotEmpty()) return key(type) !in setOf("task", "todo", "note", "tache")
        return o.arr("tasks", "todos", "children", "entries", "sections") != null || o.arr("notes") != null ||
            (o.arr("items") != null && o.str("due", "dueDate", "date", "deadline") == null && o.arr("tasks") == null && o.find("name") != null)
    }

    private fun jsonContainer(o: JSONObject, hint: ProjectType?): ContainerDraft? {
        val title = o.str("title", "name", "list", "project") ?: return null
        val typeWord = o.str("type", "kind").orEmpty()
        val notesArr = o.arr("notes")
        val children = o.arr("tasks", "todos", "items", "children", "entries")
        val type = kindOf(typeWord) ?: hint ?: when {
            notesArr != null && children == null -> ProjectType.NOTE
            children != null && children.items().all { it !is JSONObject } -> kindOf(title) ?: ProjectType.LIST
            else -> kindOf(title) ?: ProjectType.PROJECT
        }
        val (due, _) = dateTime(o.str("due", "dueDate", "deadline"))
        if (type == ProjectType.NOTE) {
            val notes = (notesArr ?: children)?.items()?.mapNotNull(::jsonNote).orEmpty()
            return ContainerDraft(title, type, o.str("description", "note").orEmpty(), due, notes = notes)
        }
        val tasks = children?.items()?.mapNotNull { jsonTask(it) }.orEmpty().toMutableList()
        o.arr("sections")?.items()?.forEach { s ->
            if (s is JSONObject) {
                val name = s.str("title", "name")
                s.arr("tasks", "items", "todos")?.items()?.mapNotNull { jsonTask(it, name) }?.let { tasks += it }
            }
        }
        return ContainerDraft(title, type, o.str("description", "note", "notes").orEmpty(), due, tasks = tasks,
            notes = if (notesArr != null && children != null) notesArr.items().mapNotNull(::jsonNote) else emptyList())
    }

    private fun json(body: String): Batch {
        val root = JSONTokener(body).nextValue()
        val containers = Containers()
        val tasks = ArrayList<TaskDraft>()
        fun item(v: Any?, hint: ProjectType?) {
            if (v is JSONObject && (hint != null || isContainer(v))) {
                if (key(v.str("type", "kind").orEmpty()) == "note") {
                    jsonNote(v)?.let { n -> val c = containers.get(v.str("list", "project") ?: "Notes", ProjectType.NOTE); containers.put(c.copy(notes = c.notes + n)) }
                    return
                }
                jsonContainer(v, hint)?.let { d ->
                    val c = containers.get(d.title, d.type)
                    containers.put(c.copy(note = c.note.ifBlank { d.note }, dueDate = c.dueDate ?: d.dueDate, tasks = c.tasks + d.tasks, notes = c.notes + d.notes))
                }
                return
            }
            val t = jsonTask(v) ?: return
            val parent = (v as? JSONObject)?.str("project", "list", "parent", "folder")
            if (parent != null) { val c = containers.get(parent, null); containers.put(c.copy(tasks = c.tasks + t)) } else tasks += t
        }
        when (root) {
            is JSONArray -> root.items().forEach { item(it, null) }
            is JSONObject -> {
                val known = listOf("projects" to ProjectType.PROJECT, "lists" to ProjectType.LIST, "checklists" to ProjectType.LIST, "notebooks" to ProjectType.NOTE, "noteLists" to ProjectType.NOTE)
                var any = false
                known.forEach { (k, type) -> root.arr(k)?.let { any = true; it.items().forEach { v -> item(v, type) } } }
                root.arr("tasks", "todos", "items")?.let { any = true; it.items().forEach { v -> item(v, null) } }
                root.arr("notes")?.let { a ->
                    any = true
                    val c = containers.get("Notes", ProjectType.NOTE); containers.put(c.copy(notes = c.notes + a.items().mapNotNull(::jsonNote)))
                }
                if (!any) item(root, null)
            }
        }
        return Batch(containers.list(), tasks)
    }

    // ── CSV ───────────────────────────────────────────────────────────────────

    private val titleCols = listOf("title", "name", "task", "content", "item", "text").flatMap(::names).toSet()

    private fun delimiter(line: String) = listOf(',', ';', '\t').maxBy { d -> line.count { it == d } }

    private fun looksLikeCsv(body: String): Boolean {
        val first = body.lineSequence().firstOrNull() ?: return false
        val d = delimiter(first)
        return first.count { it == d } >= 1 && rows(body, d).firstOrNull()?.any { key(it) in titleCols } == true
    }

    /** RFC 4180 rows: quoted fields may hold the delimiter, quotes ("") and new lines. */
    private fun rows(body: String, d: Char): List<List<String>> {
        val out = ArrayList<List<String>>(); var row = ArrayList<String>(); val f = StringBuilder(); var q = false; var i = 0
        while (i < body.length) {
            val ch = body[i]
            when {
                q && ch == '"' && body.getOrNull(i + 1) == '"' -> { f.append('"'); i++ }
                ch == '"' -> q = !q
                !q && ch == d -> { row += f.toString(); f.clear() }
                !q && (ch == '\n' || ch == '\r') -> {
                    if (ch == '\r' && body.getOrNull(i + 1) == '\n') i++
                    row += f.toString(); f.clear(); if (row.any { it.isNotBlank() }) out += row; row = ArrayList()
                }
                else -> f.append(ch)
            }
            i++
        }
        row += f.toString(); if (row.any { it.isNotBlank() }) out += row
        return out
    }

    private fun csv(body: String): Batch {
        val d = if (body.lineSequence().first().contains('\t')) '\t' else delimiter(body.lineSequence().first())
        val all = rows(body, d)
        if (all.isEmpty()) return Batch()
        val header = all[0].map(::key)
        val hasHeader = header.any { it in titleCols }
        fun col(vararg english: String) = english.flatMap(::names).firstNotNullOfOrNull { n -> header.indexOf(n).takeIf { it >= 0 } }
        val cTitle = if (hasHeader) col("title", "name", "task", "content", "item", "text") else 0
        val cType = col("type", "kind")
        val cProject = col("project", "list", "parent", "container", "folder")
        val cSection = col("section", "heading", "group")
        val cNote = col("description", "notes", "note", "details", "desc", "body")
        val cDue = col("due", "duedate", "date", "deadline")
        val cTime = col("time", "duetime")
        val cPrio = col("priority", "prio")
        val cDone = col("completed", "done", "status", "checked", "iscompleted")
        val cTags = col("tags", "labels")
        val cCheck = col("checklist", "subtasks", "items", "steps")
        val cRepeat = col("recurrence", "repeat", "every")
        val cReminder = col("reminder", "remindat")

        val containers = Containers()
        val tasks = ArrayList<TaskDraft>()
        (if (hasHeader) all.drop(1) else all).forEach { r ->
            fun v(i: Int?) = i?.let { r.getOrNull(it)?.trim() }?.takeIf { it.isNotEmpty() }
            val title = v(cTitle) ?: return@forEach
            val type = v(cType)?.let(::key).orEmpty().let { if (it == "tache") "task" else it }
            val project = v(cProject)
            if (type.isNotEmpty() && type != "task" && type != "todo" && type != "note") {
                val c = containers.get(title, kindOf(type) ?: ProjectType.PROJECT)
                containers.put(c.copy(note = c.note.ifBlank { v(cNote).orEmpty() }, dueDate = c.dueDate ?: dateTime(v(cDue)).first))
                return@forEach
            }
            if (type == "note") {
                val c = containers.get(project ?: "Notes", ProjectType.NOTE)
                containers.put(c.copy(notes = c.notes + NoteDraft(title, v(cNote).orEmpty())))
                return@forEach
            }
            val (date, t0) = if (cDue != null || cTime != null) dateTime(v(cDue)) else null to null
            val t = if (cDue == null && cTime == null && !hasHeader) NaturalLanguage.task(title) else TaskDraft(
                title = title, note = v(cNote).orEmpty(), dueDate = date, dueTime = time(v(cTime)) ?: t0,
                priority = priority(v(cPrio)), completed = truthy(v(cDone)),
                checklist = splitList(v(cCheck)), tagNames = splitList(v(cTags)), section = v(cSection),
                recurrence = v(cRepeat)?.let { NaturalLanguage.task("x ${if (it.startsWith("every", true)) it else "every $it"}").recurrence },
                reminderAt = v(cReminder)?.let { dateTime(it).let { (d2, tm) -> d2?.let { LocalDateTime.of(it, tm ?: LocalTime.of(9, 0)) } } },
            )
            if (project != null) {
                val c = containers.get(project, null)
                if (c.type == ProjectType.NOTE) containers.put(c.copy(notes = c.notes + NoteDraft(t.title, t.note)))
                else containers.put(c.copy(tasks = c.tasks + t))
            } else tasks += t
        }
        return Batch(containers.list(), tasks)
    }

    // ── plain text / Markdown ─────────────────────────────────────────────────

    private val heading = Regex("^(#{1,6})\\s+(.+?)\\s*#*$")
    private val colonHeading = Regex("^([^-*+•\\[\\d][^:]{0,60}):\\s*$")
    private val bullet = Regex("^(\\s*)(?:[-*+•]|\\d+[.)])\\s+(?:\\[([ xX])]\\s+)?(.+)$")
    private val box = Regex("^(\\s*)\\[([ xX])]\\s+(.+)$")

    private fun plain(body: String): Batch {
        val containers = Containers()
        val loose = ArrayList<TaskDraft>()
        var current: String? = null
        var section: String? = null
        var level = 0
        val paragraph = ArrayList<String>()

        fun container() = current?.let { containers.get(it, null) }
        fun addTask(t: TaskDraft) {
            val c = container()
            if (c == null) loose += t else containers.put(c.copy(tasks = c.tasks + t.copy(section = section)))
        }
        fun addChecklistItem(s: String): Boolean {
            val c = container()
            if (c != null && c.type != ProjectType.NOTE && c.tasks.isNotEmpty()) {
                val last = c.tasks.last(); containers.put(c.copy(tasks = c.tasks.dropLast(1) + last.copy(checklist = last.checklist + s))); return true
            }
            if (c == null && loose.isNotEmpty()) { val last = loose.removeAt(loose.size - 1); loose += last.copy(checklist = last.checklist + s); return true }
            return false
        }
        fun flushNote() {
            val c = container() ?: return paragraph.clear()
            if (paragraph.isNotEmpty() && c.type == ProjectType.NOTE)
                containers.put(c.copy(notes = c.notes + NoteDraft(paragraph.first(), paragraph.drop(1).joinToString("\n"))))
            paragraph.clear()
        }

        body.lines().forEach { raw ->
            val line = raw.trimEnd()
            if (line.isBlank()) { flushNote(); return@forEach }
            val h = heading.find(line.trim())
            val ch = if (h == null && bullet.find(line) == null) colonHeading.find(line.trim()) else null
            if (h != null || ch != null) {
                flushNote()
                val lvl = h?.groupValues?.get(1)?.length ?: 1
                val name = (h?.groupValues?.get(2) ?: ch!!.groupValues[1]).trim()
                if (current != null && lvl > level && container()?.type != ProjectType.NOTE) section = name
                else { current = name; level = lvl; section = null; containers.get(name, null) }
                return@forEach
            }
            val b = bullet.find(line) ?: box.find(line)
            if (b != null) {
                flushNote()
                val indented = b.groupValues[1].replace("\t", "    ").length >= 2
                val done = b.groupValues[2].equals("x", true)
                val text = b.groupValues[3].trim()
                if (container()?.type == ProjectType.NOTE) { val c = container()!!; containers.put(c.copy(notes = c.notes + NoteDraft(text))); return@forEach }
                if (indented && addChecklistItem(text)) return@forEach
                addTask(NaturalLanguage.task(text).copy(completed = done))
                return@forEach
            }
            if (container()?.type == ProjectType.NOTE) paragraph += line.trim()
            else addTask(NaturalLanguage.task(line.trim()))
        }
        flushNote()
        return Batch(containers.list(), loose)
    }
}
