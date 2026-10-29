package com.onlyflows.domain.smartadd

import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.ProjectType
import com.onlyflows.domain.model.RecurrenceType
import com.onlyflows.domain.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NaturalLanguageTest {
    // Thursday 29 October 2026, 10:00
    private val now = LocalDateTime.of(2026, 10, 29, 10, 0)
    private val tomorrow = LocalDate.of(2026, 10, 30)

    private fun ready(text: String, projects: List<Project> = emptyList()): Batch {
        val r = NaturalLanguage.interpret(text, projects, now)
        assertTrue("expected Ready for \"$text\" but got $r", r is Interpretation.Ready)
        return (r as Interpretation.Ready).batch
    }

    private fun task(text: String) = ready(text).tasks.single()

    @Test fun callTomorrowAtNine() {
        val t = task("Call John tomorrow at 9 AM")
        assertEquals("Call John", t.title)
        assertEquals(tomorrow, t.dueDate)
        assertEquals(LocalTime.of(9, 0), t.dueTime)
    }

    @Test fun everyMondayIsWeekly() {
        val t = task("Buy groceries every Monday")
        assertEquals("Buy groceries", t.title)
        assertEquals(RecurrenceType.WEEKLY, t.recurrence?.type)
        assertEquals(listOf(1), t.recurrence?.daysOfWeek)
        assertEquals(LocalDate.of(2026, 11, 2), t.dueDate)
    }

    @Test fun createProject() {
        val c = ready("Create a project for my website").containers.single()
        assertEquals("Website", c.title)
        assertEquals(ProjectType.PROJECT, c.type)
    }

    @Test fun addToNewChecklist() {
        val c = ready("Add milk, eggs, bread to my shopping checklist").containers.single()
        assertEquals("Shopping", c.title)
        assertEquals(ProjectType.LIST, c.type)
        assertEquals(listOf("Milk", "Eggs", "Bread"), c.tasks.map { it.title })
    }

    @Test fun addToExistingList() {
        val c = ready("Add milk to my shopping list", listOf(Project("p1", "Shopping list", type = ProjectType.LIST))).containers.single()
        assertEquals("Shopping list", c.title)
    }

    @Test fun reminderNextFriday() {
        val t = task("Remind me to pay rent next Friday")
        assertEquals("Pay rent", t.title)
        assertEquals(tomorrow, t.dueDate)
        assertEquals(LocalDateTime.of(tomorrow, LocalTime.of(9, 0)), t.reminderAt)
    }

    @Test fun reminderWithoutWhenAsks() {
        assertTrue(NaturalLanguage.interpret("Remind me to call mom", emptyList(), now) is Interpretation.Ask)
    }

    @Test fun frenchTomorrowAt9() {
        val t = task("Appeler Jean demain à 9h")
        assertEquals("Appeler Jean", t.title)
        assertEquals(tomorrow, t.dueDate)
        assertEquals(LocalTime.of(9, 0), t.dueTime)
    }

    @Test fun frenchReminder() {
        val t = task("Rappelle-moi de payer le loyer vendredi prochain")
        assertEquals("Payer le loyer", t.title)
        assertEquals(tomorrow, t.dueDate)
        assertEquals(LocalDateTime.of(tomorrow, LocalTime.of(9, 0)), t.reminderAt)
    }

    @Test fun frenchAddToList() {
        val c = ready("Ajoute lait, oeufs et pain à ma liste de courses").containers.single()
        assertEquals("Courses", c.title)
        assertEquals(ProjectType.LIST, c.type)
        assertEquals(listOf("Lait", "Oeufs", "Pain"), c.tasks.map { it.title })
    }

    @Test fun tagsPriorityAndTimeToday() {
        val t = task("Meeting with Anna 3pm #work !!")
        assertEquals("Meeting with Anna", t.title)
        assertEquals(now.toLocalDate(), t.dueDate)
        assertEquals(LocalTime.of(15, 0), t.dueTime)
        assertEquals(listOf("work"), t.tagNames)
        assertEquals(TaskPriority.MEDIUM, t.priority)
    }

    @Test fun plainTaskHasNoDate() {
        val t = task("Clean the garage")
        assertEquals("Clean the garage", t.title)
        assertNull(t.dueDate)
    }

    @Test fun twoMatchingListsAsk() {
        val lists = listOf(Project("a", "Courses maison", type = ProjectType.LIST), Project("b", "Courses bureau", type = ProjectType.LIST))
        val r = NaturalLanguage.interpret("Ajoute du pain à ma liste de courses", lists, now)
        assertTrue(r is Interpretation.Ask)
        assertEquals(3, (r as Interpretation.Ask).options.size)
    }
}
