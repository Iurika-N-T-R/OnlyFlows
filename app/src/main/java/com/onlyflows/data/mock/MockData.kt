package com.onlyflows.data.mock

import com.onlyflows.domain.model.*
import java.time.LocalDate

// Sample data for @Preview composables only — never shown in the running app.
object MockData {

    val tags = listOf(
        Tag(id = "tag_errand", name = "Errand", colorHex = "#6B7280"),
        Tag(id = "tag_home", name = "Home", colorHex = "#10B981"),
        Tag(id = "tag_office", name = "Office", colorHex = "#3B82F6"),
        Tag(id = "tag_important", name = "Important", colorHex = "#10B981"),
        Tag(id = "tag_pending", name = "Pending", colorHex = "#F59E0B"),
        Tag(id = "tag_computer", name = "Computer", colorHex = "#8B5CF6"),
        Tag(id = "tag_sarah", name = "Sarah", colorHex = "#EC4899"),
        Tag(id = "tag_work", name = "Work", colorHex = "#3B82F6"),
        Tag(id = "tag_family", name = "Family", colorHex = "#10B981"),
    )

    private fun tagIds(vararg names: String) =
        tags.filter { it.name in names }.map { it.id }

    val todayAgendaEvents = listOf(
        AgendaEvent(title = "Marc's birthday", time = null, colorHex = "#4A90D9", isAllDay = true),
        AgendaEvent(title = "Honey birthday", time = null, colorHex = "#E05A5A", isAllDay = true),
        AgendaEvent(title = "Hit the gym with Alex", time = "7:00 AM", colorHex = "#4A90D9"),
        AgendaEvent(title = "Coffee With Sarah", time = "8:30 AM", colorHex = "#F59E0B"),
        AgendaEvent(title = "Team meeting", time = "11:00 AM", colorHex = "#10B981"),
        AgendaEvent(title = "Budget review", time = "3:30 PM", colorHex = "#8B5CF6"),
    )

    val tasks = listOf(
        Task(
            id = "task_travel_guide",
            title = "Borrow Sarah's travel guide",
            note = "",
            projectId = "proj_vacation_rome",
            scheduledGroup = TaskGroup.TODAY,
            isStarred = false,
            tagIds = tagIds("Sarah"),
            dueDate = LocalDate.now(),
            sortOrder = 0,
        ),
        Task(
            id = "task_expense_report",
            title = "Finish expense report",
            note = "",
            projectId = "proj_work",
            scheduledGroup = TaskGroup.TODAY,
            tagIds = tagIds("Work"),
            sortOrder = 1,
        ),
        Task(
            id = "task_catering",
            title = "Organize catering",
            note = "",
            projectId = "proj_party",
            scheduledGroup = TaskGroup.TODAY,
            dueDate = LocalDate.now().plusDays(5),
            sortOrder = 2,
        ),
        Task(
            id = "task_grocery_today",
            title = "Grocery shopping",
            note = "",
            projectId = null,
            scheduledGroup = TaskGroup.TODAY,
            tagIds = tagIds("Family"),
            hasChecklist = true,
            sortOrder = 3,
        ),
        Task(
            id = "task_water_plants",
            title = "Water the plants",
            note = "",
            projectId = null,
            scheduledGroup = TaskGroup.TODAY,
            tagIds = tagIds("Family"),
            sortOrder = 4,
        ),
        Task(
            id = "task_grocery_evening",
            title = "Grocery shopping",
            note = "Dinner with Gina",
            projectId = null,
            scheduledGroup = TaskGroup.THIS_EVENING,
            sortOrder = 0,
        ),
    )
}
