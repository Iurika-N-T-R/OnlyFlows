package com.onlyflows.domain.smartadd

import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectType
import com.onlyflows.domain.model.RecurrenceBasis
import com.onlyflows.domain.model.RecurrenceRule
import com.onlyflows.domain.model.RecurrenceType
import com.onlyflows.domain.model.TaskPriority
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Everyday sentences, in English or French, turned into what to create:
 * "Call John tomorrow at 9 AM", "Buy groceries every Monday", "Create a project for my website",
 * "Add milk, eggs, bread to my shopping checklist", "Rappelle-moi de payer le loyer vendredi prochain".
 *
 * Rule-based on purpose: it runs offline, instantly, and the same text always means the same
 * thing. Dates are local (the device's time zone, through [LocalDateTime.now]).
 * ponytail: regexes, not a grammar — add a pattern when a common phrasing slips through.
 */
object NaturalLanguage {

    private val I = RegexOption.IGNORE_CASE
    // Android's ICU regex already treats accented letters as word characters for \b
    private fun re(p: String) = Regex(p, I)

    // ── vocabulary ────────────────────────────────────────────────────────────

    // Full names only: "mon" (French "my") or "sat" must not read as days.
    private const val DAY_EN = "(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday)"
    private const val DAY_FR = "(?:lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)"
    private const val MONTH = "(?:january|february|march|april|may|june|july|august|september|october|november|december|" +
        "jan|feb|mar|apr|jun|jul|aug|sept|sep|oct|nov|dec|" +
        "janvier|février|fevrier|mars|avril|mai|juin|juillet|août|aout|septembre|octobre|novembre|décembre|decembre)"
    private const val NUM = "(?:\\d+|a|an|one|two|three|four|five|six|seven|eight|nine|ten|un|une|deux|trois|quatre|cinq|six|sept|huit|neuf|dix)"

    private fun day(word: String): DayOfWeek {
        val w = word.lowercase(Locale.ROOT)
        return when {
            w.startsWith("mon") || w.startsWith("lun") -> DayOfWeek.MONDAY
            w.startsWith("tue") || w.startsWith("mar") -> DayOfWeek.TUESDAY
            w.startsWith("wed") || w.startsWith("mer") -> DayOfWeek.WEDNESDAY
            w.startsWith("thu") || w.startsWith("jeu") -> DayOfWeek.THURSDAY
            w.startsWith("fri") || w.startsWith("ven") -> DayOfWeek.FRIDAY
            w.startsWith("sat") || w.startsWith("sam") -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }
    }

    private fun month(word: String): Int {
        val w = word.lowercase(Locale.ROOT)
        return when {
            w.startsWith("jan") -> 1; w.startsWith("feb") || w.startsWith("fév") || w.startsWith("fev") -> 2
            w.startsWith("mar") -> 3; w.startsWith("apr") || w.startsWith("avr") -> 4
            w == "may" || w == "mai" -> 5; w == "june" || w == "jun" || w == "juin" -> 6
            w.startsWith("jul") || w == "juillet" -> 7; w.startsWith("aug") || w.startsWith("ao") -> 8
            w.startsWith("sep") -> 9; w.startsWith("oct") -> 10; w.startsWith("nov") -> 11
            else -> 12
        }
    }

    private fun number(word: String): Int = word.toIntOrNull() ?: when (word.lowercase(Locale.ROOT)) {
        "a", "an", "one", "un", "une" -> 1; "two", "deux" -> 2; "three", "trois" -> 3; "four", "quatre" -> 4
        "five", "cinq" -> 5; "six" -> 6; "seven", "sept" -> 7; "eight", "huit" -> 8; "nine", "neuf" -> 9; else -> 10
    }

    /** The text still to read; each recognised piece is cut out of it. */
    private class Work(var s: String) {
        fun take(r: Regex): MatchResult? {
            val m = r.find(s) ?: return null
            s = s.substring(0, m.range.first) + " " + s.substring(m.range.last + 1)
            return m
        }
    }

    // ── one task ──────────────────────────────────────────────────────────────

    private val reminderPrefix = re("^(?:please\\s+)?(?:remind\\s+me\\s+(?:to|about|that)?|reminder\\s*:?|don'?t\\s+let\\s+me\\s+forget\\s+(?:to)?|rappelle[- ]moi\\s+(?:de\\s+|d'|d’|que\\s+)?|rappel\\s*:?|n'oublie\\s+pas\\s+(?:de\\s+|d')?)\\s*")
    private val taskPrefix = re("^(?:please\\s+)?(?:(?:add|create|new)\\s+(?:a\\s+)?(?:new\\s+)?(?:task|todo|to-do)\\s*(?:to|:)?|(?:task|todo|to-do)\\s*:|(?:ajoute|crée|créer|cree|nouvelle)\\s+(?:une\\s+)?(?:nouvelle\\s+)?tâche\\s*(?:de|pour|:)?)\\s*")

    /** Reads one task: title, date, time, recurrence, priority, #tags and "remind me". */
    fun task(text: String, now: LocalDateTime = LocalDateTime.now()): TaskDraft = parseTask(text, now).first

    /** The task plus whether a reminder was asked for without saying when. */
    private fun parseTask(text: String, now: LocalDateTime): Pair<TaskDraft, Boolean> {
        val today = now.toLocalDate()
        val w = Work(" " + text.trim().removeSuffix(".") + " ")
        w.s = w.s.trim()
        var remind = false
        reminderPrefix.find(w.s)?.let { if (it.value.isNotBlank()) { remind = true; w.s = w.s.substring(it.range.last + 1) } }
        taskPrefix.find(w.s)?.let { if (it.value.isNotBlank()) w.s = w.s.substring(it.range.last + 1) }
        w.s = " ${w.s} "

        val tags = Regex("(?<=\\s)#([\\p{L}\\p{N}_-]+)").findAll(w.s).map { it.groupValues[1] }.toList()
        w.s = w.s.replace(Regex("(?<=\\s)#[\\p{L}\\p{N}_-]+"), " ")

        var priority = TaskPriority.NONE
        when {
            w.take(re("(?<=\\s)(?:!!!|!1)(?=\\s)|\\b(?:urgent(?:e|ly)?|asap|high\\s+priority|top\\s+priority|priorité\\s+haute|haute\\s+priorité|prioritaire)\\b")) != null -> priority = TaskPriority.HIGH
            w.take(re("(?<=\\s)(?:!!|!2)(?=\\s)|\\b(?:medium\\s+priority|priorité\\s+moyenne)\\b")) != null -> priority = TaskPriority.MEDIUM
            w.take(re("(?<=\\s)(?:!|!3)(?=\\s)|\\b(?:low\\s+priority|priorité\\s+basse|basse\\s+priorité)\\b")) != null -> priority = TaskPriority.LOW
        }

        val (recurrence, firstDue) = recurrence(w, today)
        // relative times first ("in 2 hours" also fixes the date)
        val rel = w.take(re("\\b(?:in|dans)\\s+($NUM)\\s+(hours?|hrs?|minutes?|mins?|heures?)\\b"))?.let { m ->
            val n = number(m.groupValues[1]).toLong()
            (if (m.groupValues[2].lowercase(Locale.ROOT).startsWith("m")) now.plusMinutes(n) else now.plusHours(n)).withSecond(0).withNano(0)
        }
        val dated = if (rel == null) date(w, today) else null
        var date: LocalDate? = rel?.toLocalDate() ?: dated?.first
        var time: LocalTime? = rel?.toLocalTime() ?: time(w)
        val evening = dated?.second == true

        if (time != null && date == null && recurrence == null) date = if (time > now.toLocalTime()) today else today.plusDays(1)
        if (date == null) date = firstDue

        var title = cleanup(w.s)
        if (title.isEmpty()) title = text.trim()
        val reminderAt = if (!remind) null else when {
            date != null && time != null -> LocalDateTime.of(date, time)
            date != null && evening -> LocalDateTime.of(date, LocalTime.of(19, 0))
            date != null -> LocalDateTime.of(date, LocalTime.of(9, 0))
            else -> null
        }
        val draft = TaskDraft(
            title = title.replaceFirstChar { it.titlecase(Locale.getDefault()) },
            dueDate = date, dueTime = time, priority = priority, recurrence = recurrence,
            reminderAt = reminderAt, tagNames = tags,
        )
        return draft to (remind && reminderAt == null)
    }

    private fun cleanup(s: String): String {
        var t = s.replace(Regex("\\s+"), " ").trim()
        val dangling = re("(?:^|\\s)(?:at|on|by|for|in|the|of|à|a|le|la|les|pour|vers|de|du|d'|et|and)$")
        val leading = re("^(?:to|at|on|by|à|de|d'|le|la|pour)\\s+")
        repeat(3) {
            t = t.replace(dangling, "").trim().trim(',', ';', ':', '-', '.').trim()
            t = t.replace(leading, "").trim()
        }
        return t
    }

    // ── recurrence ────────────────────────────────────────────────────────────

    private fun recurrence(w: Work, today: LocalDate): Pair<RecurrenceRule?, LocalDate?> {
        fun rule(type: RecurrenceType, interval: Int = 1, days: List<Int> = emptyList()) =
            RecurrenceRule(type = type, interval = interval, daysOfWeek = days, basis = RecurrenceBasis.DUE_DATE)

        w.take(re("\\b(?:every\\s+weekdays?|each\\s+weekday|on\\s+weekdays|en\\s+semaine|(?:tous\\s+)?les\\s+jours\\s+ouvrés|chaque\\s+jour\\s+ouvré)\\b"))?.let {
            val first = generateSequence(today) { it.plusDays(1) }.first { it.dayOfWeek.value <= 5 }
            return rule(RecurrenceType.WEEKDAYS) to first
        }
        val listEn = "${DAY_EN}s?(?:\\s*(?:,|and|&)\\s*${DAY_EN}s?)*"
        val listFr = "${DAY_FR}s?(?:\\s*(?:,|et)\\s*(?:les\\s+)?${DAY_FR}s?)*"
        val dayList = w.take(re("\\b(?:every|each)\\s+($listEn)\\b"))?.groupValues?.get(1)
            ?: w.take(re("\\bon\\s+((?:${DAY_EN}s)(?:\\s*(?:,|and|&)\\s*${DAY_EN}s)*)\\b"))?.groupValues?.get(1)
            ?: w.take(re("\\b(?:tous\\s+les|chaque)\\s+($listFr)\\b"))?.groupValues?.get(1)
        if (dayList != null) {
            val days = Regex("$DAY_EN|$DAY_FR", I).findAll(dayList).map { day(it.value) }.distinct().sortedBy { it.value }.toList()
            val first = generateSequence(today) { it.plusDays(1) }.first { it.dayOfWeek in days }
            return rule(RecurrenceType.WEEKLY, days = days.map { it.value }) to first
        }
        fun typeOf(word: String) = word.lowercase(Locale.ROOT).let {
            when {
                it.startsWith("d") || it.startsWith("j") || it.startsWith("quot") -> RecurrenceType.DAILY
                it.startsWith("w") || it.startsWith("sem") || it.startsWith("heb") -> RecurrenceType.WEEKLY
                it.startsWith("mo") || it.startsWith("mens") -> RecurrenceType.MONTHLY
                else -> RecurrenceType.YEARLY
            }
        }
        w.take(re("\\b(?:every|each)\\s+(other\\s+|\\d+\\s+)?(day|week|month|year)s?\\b"))?.let { m ->
            val n = m.groupValues[1].trim().let { if (it.equals("other", true)) 2 else it.toIntOrNull() ?: 1 }
            return rule(typeOf(m.groupValues[2]), n) to today
        }
        w.take(re("\\b(?:tous\\s+les|toutes\\s+les|chaque)\\s+(\\d+\\s+)?(jours?|semaines?|mois|ans|années?)\\b"))?.let { m ->
            val n = m.groupValues[1].trim().toIntOrNull() ?: 1
            return rule(typeOf(m.groupValues[2]), n) to today
        }
        w.take(re("\\b(daily|every\\s*day|weekly|monthly|yearly|annually|quotidiennement|hebdomadaire(?:ment)?|mensuel(?:le)?(?:ment)?|annuel(?:le)?(?:ment)?)\\b"))?.let { m ->
            val word = m.groupValues[1].lowercase(Locale.ROOT)
            val type = when {
                word.startsWith("dai") || word.startsWith("every") || word.startsWith("quot") -> RecurrenceType.DAILY
                word.startsWith("week") || word.startsWith("heb") -> RecurrenceType.WEEKLY
                word.startsWith("mon") || word.startsWith("mens") -> RecurrenceType.MONTHLY
                else -> RecurrenceType.YEARLY
            }
            return rule(type) to today
        }
        return null to null
    }

    // ── dates and times ───────────────────────────────────────────────────────

    /** The date, and whether it was "tonight". */
    private fun date(w: Work, today: LocalDate): Pair<LocalDate, Boolean>? {
        w.take(re("\\b(?:the\\s+)?day\\s+after\\s+tomorrow\\b|\\baprès[- ]demain\\b|\\bapres[- ]demain\\b"))?.let { return today.plusDays(2) to false }
        w.take(re("\\btomorrow\\b|\\bdemain\\b"))?.let { return today.plusDays(1) to false }
        w.take(re("\\b(?:tonight|this\\s+evening|ce\\s+soir)\\b"))?.let { return today to true }
        w.take(re("\\b(?:today|aujourd'hui|aujourd’hui)\\b"))?.let { return today to false }
        w.take(re("\\b(?:in|dans)\\s+($NUM)\\s+(days?|weeks?|months?|years?|jours?|semaines?|mois|ans)\\b"))?.let { m ->
            val n = number(m.groupValues[1]).toLong()
            val u = m.groupValues[2].lowercase(Locale.ROOT)
            return when {
                u.startsWith("d") || u.startsWith("j") -> today.plusDays(n)
                u.startsWith("w") || u.startsWith("s") -> today.plusWeeks(n)
                u.startsWith("mo") || u == "mois" -> today.plusMonths(n)
                else -> today.plusYears(n)
            } to false
        }
        w.take(re("\\bnext\\s+week\\b|\\bla\\s+semaine\\s+prochaine\\b"))?.let { return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)) to false }
        w.take(re("\\bnext\\s+month\\b|\\ble\\s+mois\\s+prochain\\b"))?.let { return today.plusMonths(1) to false }
        w.take(re("\\b(?:(next|this|on|by|coming)\\s+)?($DAY_EN)\\b"))?.let { m ->
            val d = day(m.groupValues[2])
            return (if (m.groupValues[1].equals("this", true)) today.with(TemporalAdjusters.nextOrSame(d)) else today.with(TemporalAdjusters.next(d))) to false
        }
        w.take(re("\\b(?:(?:ce|le)\\s+)?($DAY_FR)(?:\\s+(prochain))?\\b"))?.let { m ->
            return today.with(TemporalAdjusters.next(day(m.groupValues[1]))) to false
        }
        w.take(Regex("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b"))?.let { m ->
            return runCatching { LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) }.getOrNull()?.let { it to false }
        }
        fun dated(d: Int, mo: Int, y: String): LocalDate? = runCatching {
            val year = y.toIntOrNull()?.let { if (it < 100) 2000 + it else it }
            val date = LocalDate.of(year ?: today.year, mo, d)
            if (year == null && date < today) date.plusYears(1) else date
        }.getOrNull()
        w.take(re("\\b(?:on\\s+|le\\s+|the\\s+)?(\\d{1,2})(?:st|nd|rd|th|er)?\\s+(?:of\\s+)?($MONTH)\\.?(?:\\s+(\\d{4}))?\\b"))?.let { m ->
            return dated(m.groupValues[1].toInt(), month(m.groupValues[2]), m.groupValues[3])?.let { it to false }
        }
        w.take(re("\\b(?:on\\s+)?($MONTH)\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b(?:,?\\s+(\\d{4}))?"))?.let { m ->
            return dated(m.groupValues[2].toInt(), month(m.groupValues[1]), m.groupValues[3])?.let { it to false }
        }
        w.take(Regex("(?<![\\d/])(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?(?![\\d/])"))?.let { m ->
            return dated(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3])?.let { it to false }
        }
        return null
    }

    private fun time(w: Work): LocalTime? {
        w.take(re("\\b(?:at\\s+|à\\s+|a\\s+|vers\\s+)?(noon|midday|midi)\\b"))?.let { return LocalTime.NOON }
        w.take(re("\\b(?:at\\s+|à\\s+|vers\\s+)?(midnight|minuit)\\b"))?.let { return LocalTime.MIDNIGHT }
        w.take(re("\\b(?:at\\s+|à\\s+|vers\\s+|by\\s+)?(\\d{1,2})(?:[:h.](\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)(?=\\s|$|[,.;])"))?.let { m ->
            var h = m.groupValues[1].toInt() % 12
            if (m.groupValues[3].lowercase(Locale.ROOT).startsWith("p")) h += 12
            return runCatching { LocalTime.of(h, m.groupValues[2].toIntOrNull() ?: 0) }.getOrNull()
        }
        w.take(re("\\b(?:at\\s+|à\\s+|a\\s+|vers\\s+)?(\\d{1,2})h(\\d{2})?\\b"))?.let { m ->
            return runCatching { LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toIntOrNull() ?: 0) }.getOrNull()
        }
        w.take(re("\\b(?:at\\s+|à\\s+|vers\\s+)?(\\d{1,2}):(\\d{2})\\b"))?.let { m ->
            return runCatching { LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt()) }.getOrNull()
        }
        // "at 5" with no am/pm: read as working hours, 1–7 is the afternoon
        w.take(re("\\b(?:at|à|vers)\\s+(\\d{1,2})\\b(?!\\s*(?:%|€|\\$|kg|km))"))?.let { m ->
            val h = m.groupValues[1].toInt()
            if (h > 23) return null
            return LocalTime.of(if (h in 1..7) h + 12 else h, 0)
        }
        w.take(re("\\b(?:in\\s+the\\s+|this\\s+|le\\s+|ce\\s+)?(morning|matin)\\b"))?.let { return LocalTime.of(9, 0) }
        w.take(re("\\b(?:in\\s+the\\s+|this\\s+|l'|cet\\s+)?(afternoon|après-midi|apres-midi)\\b"))?.let { return LocalTime.of(14, 0) }
        return null
    }

    // ── whole sentences ───────────────────────────────────────────────────────

    private val createRe = re("^(?:please\\s+)?(?:create|make|start|add|set\\s+up|new)\\s+(?:a\\s+|an\\s+|the\\s+)?(?:new\\s+)?(project|checklist|list|notes?\\s+list|notebook)\\b(?:\\s+(?:for|called|named|titled|about))?\\s*:?\\s*(.*)$")
    private val createNamedFirstRe = re("^(?:please\\s+)?(?:create|make|start|set\\s+up|new)\\s+(?:a\\s+|an\\s+)?(?:new\\s+)?(.+?)\\s+(project|checklist|list|notes)(?:\\s+(?:with|containing)\\s+(.+))?$")
    private val createFrRe = re("^(?:crée|créer|cree|creer|crées|fais|faire|ajoute|nouveau|nouvelle)\\s+(?:un\\s+|une\\s+)?(?:nouveau\\s+|nouvelle\\s+)?(projet|checklist|liste\\s+de\\s+notes|carnet(?:\\s+de\\s+notes)?|liste)\\b(?:\\s+(?:pour|nommée?|appelée?|intitulée?))?\\s*:?\\s*(.*)$")
    private val addToRe = re("^(?:please\\s+)?(?:add|put)\\s+(.+?)\\s+(?:to|in|into|on)\\s+(?:my|the|our)\\s+(.+)$")
    private val addToFrRe = re("^(?:ajoute|ajouter|rajoute|mets|mettre|met)\\s+(.+?)\\s+(?:à|a|au|aux|dans|sur)\\s+(?:ma|mon|mes|la|le|les|l'|l’)\\s*(.+)$")
    private val noteRe = re("^(?:take\\s+a\\s+note|make\\s+a\\s+note|write\\s+down|jot\\s+down|note\\s+down|note|noter|prends?\\s+(?:une\\s+)?note|note\\s+que)\\b\\s*(?:that|que|:)?\\s*(.+)$")
    private val destinationRe = re("\\s+(?:to|in|into|for|on)\\s+(?:my|the)\\s+(.+?)\\s+(project|list|checklist)\\s*$")
    private val destinationFrRe = re("\\s+(?:dans|pour|sur|à)\\s+(?:mon|ma|le|la)\\s+(projet|liste)\\s+(?:de\\s+|des\\s+|du\\s+|d')?(.+?)\\s*$")

    private fun kindOf(word: String): ProjectType? = word.lowercase(Locale.ROOT).let {
        when {
            it.contains("note") || it.contains("carnet") -> ProjectType.NOTE
            it.contains("list") || it.contains("liste") || it.contains("checklist") -> ProjectType.LIST
            it.contains("proj") -> ProjectType.PROJECT
            else -> null
        }
    }

    private fun name(raw: String): String = raw.trim()
        .replace(re("^(?:de|des|du|d'|d’|pour|for|my|the|a|an|mon|ma|mes|le|la|les|l'|l’)\\s*"), "")
        .replace(re("^(?:de|des|du|d'|d’)\\s+"), "")
        .trim().trim('.', ',', ':', '"', '\'')
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

    private fun items(raw: String): List<String> = raw.split(re("\\s*,\\s*|\\s+and\\s+|\\s+et\\s+|\\s*&\\s*|\\s*;\\s*"))
        .map { it.trim().trim('.') }.filter { it.isNotEmpty() }

    private fun norm(s: String) = s.lowercase(Locale.getDefault())
        .replace(re("\\b(?:check)?list[e]?s?\\b|\\bprojec?ts?\\b|\\bprojets?\\b|\\bnotes?\\b|\\bcarnet\\b|\\bde\\b|\\bdes\\b|\\bmy\\b|\\bthe\\b"), " ")
        .replace(Regex("\\s+"), " ").trim()

    private sealed interface Target {
        data class Found(val p: Project) : Target
        data class New(val title: String) : Target
        data class Choose(val list: List<Project>) : Target
    }

    /** Finds the project or list a sentence talks about, by name, among [projects]. */
    private fun resolve(name: String, kind: ProjectType?, projects: List<Project>): Target {
        val n = norm(name)
        val pool = projects.filter { !it.isArchived && (kind == null || it.type == kind || (kind != ProjectType.NOTE && it.type != ProjectType.NOTE)) }
        fun score(p: Project): Int {
            val t = norm(p.title)
            return when {
                n.isEmpty() || t.isEmpty() -> 0
                t == n -> 4
                t.contains(n) || n.contains(t) -> 2
                t.split(" ").any { it.length >= 4 && n.split(" ").contains(it) } -> 1
                else -> 0
            } + if (kind != null && p.type == kind && t.isNotEmpty() && (t == n || t.contains(n) || n.contains(t))) 1 else 0
        }
        val scored = pool.map { it to score(it) }.filter { it.second > 0 }
        val best = scored.maxOfOrNull { it.second } ?: return Target.New(name)
        val top = scored.filter { it.second == best }.map { it.first }
        return if (top.size == 1) Target.Found(top[0]) else Target.Choose(top)
    }

    /** Interprets a whole sentence against the user's existing [projects]. */
    fun interpret(text: String, projects: List<Project>, now: LocalDateTime = LocalDateTime.now()): Interpretation {
        val t = text.trim().replace(Regex("\\s+"), " ").removeSuffix(".")
        if (t.isEmpty()) return Interpretation.Invalid("Nothing to add")

        // "Create a project for my website", "Make a shopping list", "Crée une liste de courses"
        val create = createRe.find(t)?.let { Triple(it.groupValues[1], it.groupValues[2], null as String?) }
            ?: createFrRe.find(t)?.let { Triple(it.groupValues[1], it.groupValues[2], null) }
            ?: createNamedFirstRe.find(t)?.let { Triple(it.groupValues[2], it.groupValues[1], it.groupValues[3].ifBlank { null }) }
        if (create != null) {
            val type = kindOf(create.first) ?: ProjectType.PROJECT
            var rest = create.second
            var itemText = create.third
            Regex("\\s+(?:with|containing|avec|contenant)\\s+(.+)$", I).find(rest)?.let { itemText = it.groupValues[1]; rest = rest.substring(0, it.range.first) }
            val title = name(rest)
            if (title.isBlank()) return Interpretation.Invalid("Give it a name, e.g. \"Create a project for my website\"")
            val entries = itemText?.let(::items).orEmpty()
            return Interpretation.Ready(Batch(listOf(container(title, type, entries, now))))
        }

        // "Add milk, eggs, bread to my shopping checklist", "Ajoute du pain à ma liste de courses"
        val add = addToRe.find(t) ?: addToFrRe.find(t)
        if (add != null) {
            val target = add.groupValues[2]
            val kind = kindOf(target)
            val listName = name(norm(target).ifEmpty { if (kind == ProjectType.NOTE) "Notes" else "" }.let { n -> if (n.isEmpty()) target else n })
            val resolved = resolve(listName, kind, projects)
            if (kind != null || resolved is Target.Found || resolved is Target.Choose) {
                val entries = items(add.groupValues[1])
                val type = kind ?: ProjectType.LIST
                return when (resolved) {
                    is Target.Found -> Interpretation.Ready(Batch(listOf(container(resolved.p.title, resolved.p.type, entries, now))))
                    is Target.New -> Interpretation.Ready(Batch(listOf(container(resolved.title, type, entries, now))))
                    is Target.Choose -> Interpretation.Ask(
                        "Which one do you mean by \"$listName\"?",
                        resolved.list.map { p -> p.title to Batch(listOf(container(p.title, p.type, entries, now))) } +
                            ("New ${typeWord(type)} \"$listName\"" to Batch(listOf(container(listName, type, entries, now)))),
                    )
                }
            }
        }

        // "Note: the wifi code is 1234", "Note que la réunion est décalée"
        noteRe.find(t)?.let { m ->
            val body = m.groupValues[1].trim().replaceFirstChar { it.titlecase(Locale.getDefault()) }
            val noteLists = projects.filter { !it.isArchived && it.type == ProjectType.NOTE }
            val note = NoteDraft(title = body)
            return when (noteLists.size) {
                0 -> Interpretation.Ready(Batch(listOf(ContainerDraft("Notes", ProjectType.NOTE, notes = listOf(note)))))
                1 -> Interpretation.Ready(Batch(listOf(ContainerDraft(noteLists[0].title, ProjectType.NOTE, notes = listOf(note)))))
                else -> Interpretation.Ask("Which notes list?", noteLists.map { p -> p.title to Batch(listOf(ContainerDraft(p.title, ProjectType.NOTE, notes = listOf(note)))) })
            }
        }

        // A task, maybe for a named project: "Write the intro for my Website project"
        var body = t
        var dest: Pair<String, ProjectType>? = null
        (destinationRe.find(body))?.let { m -> dest = name(m.groupValues[1]) to (kindOf(m.groupValues[2]) ?: ProjectType.PROJECT); body = body.substring(0, m.range.first) }
            ?: destinationFrRe.find(body)?.let { m -> dest = name(m.groupValues[2]) to (kindOf(m.groupValues[1]) ?: ProjectType.PROJECT); body = body.substring(0, m.range.first) }
        val (draft, needsWhen) = parseTask(body, now)

        fun place(d: TaskDraft): List<Pair<String, Batch>> {
            val (dn, dk) = dest ?: return listOf("" to Batch(tasks = listOf(d)))
            return when (val r = resolve(dn, dk, projects)) {
                is Target.Found -> listOf(r.p.title to Batch(listOf(ContainerDraft(r.p.title, r.p.type, tasks = listOf(d)))))
                is Target.Choose -> r.list.map { p -> p.title to Batch(listOf(ContainerDraft(p.title, p.type, tasks = listOf(d)))) }
                is Target.New -> listOf(
                    "New ${typeWord(dk)} \"$dn\"" to Batch(listOf(ContainerDraft(dn, dk, tasks = listOf(d)))),
                    "Inbox" to Batch(tasks = listOf(d)),
                )
            }
        }

        if (needsWhen) {
            val today = now.toLocalDate()
            val inHour = now.plusHours(1).withSecond(0).withNano(0)
            val choices = listOf(
                "In 1 hour" to inHour,
                "This evening 19:00" to LocalDateTime.of(today, LocalTime.of(19, 0)).let { if (it > now) it else it.plusDays(1) },
                "Tomorrow 09:00" to LocalDateTime.of(today.plusDays(1), LocalTime.of(9, 0)),
            )
            val placed = place(draft).first().second
            return Interpretation.Ask("When should I remind you?", choices.map { (label, at) ->
                label to placed.map { it.copy(dueDate = it.dueDate ?: at.toLocalDate(), dueTime = it.dueTime ?: at.toLocalTime(), reminderAt = at) }
            })
        }
        val options = place(draft)
        return if (options.size == 1) Interpretation.Ready(options[0].second)
        else Interpretation.Ask(if (dest != null && options.any { it.first == "Inbox" }) "No ${typeWord(dest!!.second)} called \"${dest!!.first}\" yet" else "Which one?", options)
    }

    private fun typeWord(t: ProjectType) = when (t) { ProjectType.LIST -> "list"; ProjectType.NOTE -> "notes list"; ProjectType.PROJECT -> "project" }

    private fun container(title: String, type: ProjectType, entries: List<String>, now: LocalDateTime) =
        if (type == ProjectType.NOTE) ContainerDraft(title, type, notes = entries.map { NoteDraft(it.replaceFirstChar { c -> c.titlecase(Locale.getDefault()) }) })
        else ContainerDraft(title, type, tasks = entries.map { task(it, now) })

    private fun Batch.map(f: (TaskDraft) -> TaskDraft) = Batch(containers.map { c -> c.copy(tasks = c.tasks.map(f)) }, tasks.map(f))
}
