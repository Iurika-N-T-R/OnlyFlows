package com.onlyflows.data.mock

import com.onlyflows.domain.model.*
import java.time.LocalDate

object MockData {

    // ── Tags ──────────────────────────────────────────────────────────────────

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

    // ── Today agenda events ───────────────────────────────────────────────────

    val todayAgendaEvents = listOf(
        AgendaEvent(title = "Marc's birthday", time = null, colorHex = "#4A90D9", isAllDay = true),
        AgendaEvent(title = "Honey birthday", time = null, colorHex = "#E05A5A", isAllDay = true),
        AgendaEvent(title = "Hit the gym with Alex", time = "7:00 AM", colorHex = "#4A90D9"),
        AgendaEvent(title = "Coffee With Sarah", time = "8:30 AM", colorHex = "#F59E0B"),
        AgendaEvent(title = "Team meeting", time = "11:00 AM", colorHex = "#10B981"),
        AgendaEvent(title = "Budget review", time = "3:30 PM", colorHex = "#8B5CF6"),
    )

    // ── Folders ───────────────────────────────────────────────────────────────

    val folders = listOf(
        Folder(id = "folder_personal", title = "Personal", sortOrder = 0),
        Folder(id = "folder_work",     title = "Work",     sortOrder = 1),
    )

    // ── Projects ──────────────────────────────────────────────────────────────

    val projects = listOf(
        Project(
            id = "proj_vacation_rome",
            title = "Vacation in Rome",
            note = "We'll go from June 14-22 and visit Jane and Paolo. Maybe do a night out in Trastevere.",
            colorHex = "#4A90D9",
            folderId = "folder_personal",
            sortOrder = 0,
            sections = listOf(
                Section("sec_things_buy", "proj_vacation_rome", "Things to buy", 0),
                Section("sec_planning", "proj_vacation_rome", "Planning", 1),
                Section("sec_things_do", "proj_vacation_rome", "Things to do", 2),
            )
        ),
        Project(
            id = "proj_party",
            title = "Throw Party for Eve",
            note = "",
            colorHex = "#EC4899",
            folderId = "folder_personal",
            sortOrder = 1,
            sections = listOf(
                Section("sec_party_food", "proj_party", "Food & Drinks", 0),
                Section("sec_party_deco", "proj_party", "Decorations", 1),
            )
        ),
        Project(
            id = "proj_marathon",
            title = "Run a Marathon",
            note = "",
            colorHex = "#F59E0B",
            folderId = "folder_personal",
            sortOrder = 2,
            sections = emptyList()
        ),
        Project(
            id = "proj_work",
            title = "Work",
            note = "",
            colorHex = "#3B82F6",
            folderId = "folder_work",
            sortOrder = 0,
            sections = listOf(
                Section("sec_prepare_pres", "proj_work", "Prepare Presentation", 0),
                Section("sec_slides_notes", "proj_work", "Slides and notes", 1),
            )
        ),
        Project(
            id = "proj_new_zealand",
            title = "New Zealand 2024",
            note = "",
            colorHex = "#10B981",
            folderId = null,
            sortOrder = 0,
            sections = listOf(
                Section("sec_nz_research", "proj_new_zealand", "Research", 0),
            )
        ),
    )

    // ── Tasks ─────────────────────────────────────────────────────────────────

    val tasks = listOf(

        // Today tasks
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

        // This Evening tasks
        Task(
            id = "task_grocery_evening",
            title = "Grocery shopping",
            note = "Dinner with Gina",
            projectId = null,
            scheduledGroup = TaskGroup.THIS_EVENING,
            sortOrder = 0,
        ),
        Task(
            id = "task_respond_client",
            title = "Respond to client",
            note = "Call plumber",
            projectId = "proj_work",
            scheduledGroup = TaskGroup.THIS_EVENING,
            isStarred = true,
            sortOrder = 1,
        ),
        Task(
            id = "task_dj_gig",
            title = "Confirm DJ gig",
            note = "Update website",
            projectId = null,
            scheduledGroup = TaskGroup.THIS_EVENING,
            sortOrder = 2,
        ),
        Task(
            id = "task_presentation",
            title = "Grocery shopping",
            note = "Prepare presentation",
            projectId = "proj_work",
            scheduledGroup = TaskGroup.THIS_EVENING,
            hasNote = true,
            hasChecklist = true,
            sortOrder = 3,
        ),

        // Vacation in Rome tasks
        Task(
            id = "task_extra_battery",
            title = "Extra camera battery",
            note = "",
            projectId = "proj_vacation_rome",
            sectionId = "sec_things_buy",
            hasNote = true,
            tagIds = tagIds("Errand"),
            sortOrder = 0,
        ),
        Task(
            id = "task_power_adapter",
            title = "Power adapter",
            note = "",
            projectId = "proj_vacation_rome",
            sectionId = "sec_things_buy",
            hasNote = true,
            tagIds = tagIds("Errand"),
            sortOrder = 1,
        ),
        Task(
            id = "task_add_trip_dates",
            title = "Add trip dates to calendar",
            note = "",
            projectId = "proj_vacation_rome",
            sectionId = "sec_planning",
            sortOrder = 0,
        ),
        Task(
            id = "task_read_metro",
            title = "Read about the metro",
            note = "",
            projectId = "proj_vacation_rome",
            sectionId = "sec_planning",
            hasNote = true,
            sortOrder = 1,
        ),
        Task(
            id = "task_borrow_guide",
            title = "Borrow Sarah's travel guide",
            note = "",
            projectId = "proj_vacation_rome",
            sectionId = "sec_planning",
            isStarred = true,
            sortOrder = 2,
        ),

        // Book hotels task (with checklist)
        Task(
            id = "task_book_hotels",
            title = "Book hotels",
            note = "Make sure they are central, have Wifi, and are close to a subway station.",
            projectId = "proj_vacation_rome",
            sectionId = "sec_planning",
            scheduledGroup = TaskGroup.TODAY,
            hasChecklist = true,
            checklistItems = listOf(
                ChecklistItem("ci_london", "task_book_hotels", "London from June 3", sortOrder = 0),
                ChecklistItem("ci_paris", "task_book_hotels", "Paris from June 10", sortOrder = 1),
                ChecklistItem("ci_berlin", "task_book_hotels", "Berlin from June 17", sortOrder = 2),
            ),
            sortOrder = 3,
        ),

        // Design search task
        Task(
            id = "task_design_search",
            title = "Design search",
            note = "Should display screens and apps",
            projectId = "proj_work",
            sectionId = "sec_slides_notes",
            tagIds = tagIds("Important"),
            scheduledGroup = TaskGroup.TODAY,
            dueDate = LocalDate.now().plusDays(5),
            hasChecklist = true,
            checklistItems = listOf(
                ChecklistItem("ci_screens", "task_design_search", "Screens results", sortOrder = 0),
                ChecklistItem("ci_apps", "task_design_search", "Apps results", sortOrder = 1),
            ),
            sortOrder = 0,
        ),

        // Make screenshots task
        Task(
            id = "task_make_screenshots",
            title = "Make screenshots",
            note = "",
            projectId = "proj_work",
            sectionId = "sec_slides_notes",
            hasNote = true,
            sortOrder = 1,
        ),
        Task(
            id = "task_upload_screenshots",
            title = "Upload screenshots",
            note = "",
            projectId = "proj_work",
            sectionId = "sec_slides_notes",
            sortOrder = 2,
        ),

        // New Zealand tasks
        Task(
            id = "task_nz_look_up",
            title = "Look up Southern Lights",
            note = "",
            projectId = "proj_new_zealand",
            sectionId = "sec_nz_research",
            isStarred = true,
            hasChecklist = true,
            sortOrder = 0,
        ),
        Task(
            id = "task_nz_flights_dunedin",
            title = "Research flights to Dunedin",
            note = "",
            projectId = "proj_new_zealand",
            sectionId = "sec_nz_research",
            dueDate = LocalDate.now().withDayOfMonth(12).withMonth(11),
            sortOrder = 1,
        ),
        Task(
            id = "task_nz_airlines",
            title = "Research airlines to New Zealand",
            note = "",
            projectId = "proj_new_zealand",
            sectionId = "sec_nz_research",
            dueDate = LocalDate.now().withDayOfMonth(12).withMonth(11),
            sortOrder = 2,
        ),
    )

    // ── Upcoming (grouped by date) ────────────────────────────────────────────

    data class UpcomingDay(
        val date: LocalDate,
        val label: String,
        val agendaEvents: List<AgendaEvent>,
        val tasks: List<Task>,
    )

    val upcomingDays: List<UpcomingDay> = run {
        val today = LocalDate.now()
        listOf(
            UpcomingDay(
                date = today.plusDays(1),
                label = "Tomorrow",
                agendaEvents = listOf(
                    AgendaEvent("Interview with Lydia", "10:00 AM", "#4A90D9"),
                    AgendaEvent("Benefits presentation", "1:00 PM", "#F59E0B"),
                ),
                tasks = listOf(
                    Task("up_task1", "Prepare interview questions", projectId = "proj_work"),
                    Task("up_task2", "Make reservation for dinner", projectId = "proj_party"),
                )
            ),
            UpcomingDay(
                date = today.plusDays(2),
                label = "Thursday",
                agendaEvents = listOf(
                    AgendaEvent("Work from home", null, "#10B981", isAllDay = true),
                    AgendaEvent("Monthly conference call", "1:00 PM", "#8B5CF6"),
                ),
                tasks = listOf(
                    Task("up_task3", "Buy movie tickets for Friday"),
                    Task("up_task4", "Get copy of signed contract", projectId = "proj_work"),
                    Task("up_task5", "Call Mom and Dad", tagIds = tagIds("Family")),
                    Task("up_task6", "Order a cake", projectId = "proj_party"),
                )
            ),
            UpcomingDay(
                date = today.plusDays(3),
                label = "Friday",
                agendaEvents = listOf(
                    AgendaEvent("Electrician comes", "5:00 PM", "#E05A5A"),
                    AgendaEvent("Volunteer at the library", "6:30 PM", "#10B981"),
                ),
                tasks = listOf(
                    Task("up_task7", "Buy party decorations", projectId = "proj_party"),
                    Task("up_task8", "Read article about nutrition", projectId = "proj_marathon"),
                    Task("up_task9", "Buy party decorations", projectId = "proj_party"),
                )
            ),
        )
    }
}
