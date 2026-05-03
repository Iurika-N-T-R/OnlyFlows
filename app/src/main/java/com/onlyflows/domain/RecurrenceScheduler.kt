package com.onlyflows.domain

import com.onlyflows.domain.model.RecurrenceBasis
import com.onlyflows.domain.model.RecurrenceRule
import com.onlyflows.domain.model.RecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Computes the next due date for a recurring task.
 *
 * @param rule       the recurrence rule
 * @param completedOn the date the task was completed (used when basis = COMPLETION_DATE)
 * @param currentDue the task's current due date (used when basis = DUE_DATE)
 * @return next due date, or null if the rule cannot produce one
 */
fun nextDueDate(
    rule: RecurrenceRule,
    completedOn: LocalDate,
    currentDue: LocalDate?,
): LocalDate? {
    val base = when (rule.basis) {
        RecurrenceBasis.COMPLETION_DATE -> completedOn
        RecurrenceBasis.DUE_DATE        -> currentDue ?: completedOn
        RecurrenceBasis.SPECIFIC_DATE   -> currentDue ?: completedOn
    }

    var next = when (rule.type) {
        RecurrenceType.HOURLY    -> base.plusDays(1) // hourly granularity not meaningful on date level
        RecurrenceType.DAILY     -> base.plusDays(rule.interval.toLong())
        RecurrenceType.WEEKDAYS  -> nextWeekday(base)
        RecurrenceType.WEEKLY    -> {
            if (rule.daysOfWeek.isEmpty()) {
                base.plusWeeks(rule.interval.toLong())
            } else {
                nextSpecificWeekday(base, rule.daysOfWeek)
            }
        }
        RecurrenceType.MONTHLY   -> base.plusMonths(rule.interval.toLong())
        RecurrenceType.YEARLY    -> base.plusYears(rule.interval.toLong())
    }

    if (rule.skipWeekends) {
        while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
            next = next.plusDays(1)
        }
    }

    // End-date check: return null if next occurrence is past the end date
    if (rule.endDate != null && next > rule.endDate) return null

    return next
}

private fun nextWeekday(from: LocalDate): LocalDate {
    var d = from.plusDays(1)
    while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) {
        d = d.plusDays(1)
    }
    return d
}

/** Returns the next occurrence of one of the specified ISO week-days (1=Mon..7=Sun) after [from]. */
private fun nextSpecificWeekday(from: LocalDate, daysOfWeek: List<Int>): LocalDate {
    if (daysOfWeek.isEmpty()) return from.plusWeeks(1)
    var d = from.plusDays(1)
    repeat(7) {
        if (d.dayOfWeek.value in daysOfWeek) return d
        d = d.plusDays(1)
    }
    return d
}
