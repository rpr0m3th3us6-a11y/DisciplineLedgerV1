package com.rowanrisk.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LogicTest {
    private val today = LocalDate.of(2026, 10, 6)
    private val a = Habit("a", "A")
    private val b = Habit("b", "B")

    private fun day(offset: Long) = today.minusDays(offset).toString()

    @Test
    fun todayNotFinishedDoesNotBreakAStreak() {
        val data = LedgerData(
            habits = listOf(a, b),
            log = mapOf(day(1) to setOf("a", "b"), day(2) to setOf("a", "b")),
            bestStreak = 0,
        )
        assertEquals(2, Logic.overallStreak(data, today))
    }

    @Test
    fun completingTodayExtendsTheStreak() {
        val data = LedgerData(
            habits = listOf(a, b),
            log = mapOf(day(0) to setOf("a", "b"), day(1) to setOf("a", "b")),
            bestStreak = 0,
        )
        assertEquals(2, Logic.overallStreak(data, today))
    }

    @Test
    fun aMissedHabitBreaksTheOverallStreak() {
        val data = LedgerData(
            habits = listOf(a, b),
            log = mapOf(day(1) to setOf("a"), day(2) to setOf("a", "b"), day(3) to setOf("a", "b")),
            bestStreak = 0,
        )
        assertEquals(0, Logic.overallStreak(data, today))
    }

    @Test
    fun aGapEndsTheStreak() {
        val data = LedgerData(
            habits = listOf(a),
            log = mapOf(day(1) to setOf("a"), day(3) to setOf("a")),
            bestStreak = 0,
        )
        assertEquals(1, Logic.overallStreak(data, today))
        assertEquals(1, Logic.habitStreak(data, "a", today))
    }

    @Test
    fun habitStreakIsPerHabit() {
        val data = LedgerData(
            habits = listOf(a, b),
            log = mapOf(
                day(0) to setOf("a"),
                day(1) to setOf("a", "b"),
                day(2) to setOf("a"),
            ),
            bestStreak = 0,
        )
        assertEquals(3, Logic.habitStreak(data, "a", today))
        assertEquals(1, Logic.habitStreak(data, "b", today))
    }

    @Test
    fun noHabitsMeansNoStreakAndNothingToDo() {
        val data = LedgerData(emptyList(), emptyMap(), 0)
        assertEquals(0, Logic.overallStreak(data, today))
        assertEquals(0, Logic.doneCount(data, today))
        assertNull(Logic.nextUndone(data, today))
    }

    @Test
    fun nextUndoneSkipsWhatIsDone() {
        val data = LedgerData(listOf(a, b), mapOf(day(0) to setOf("a")), 0)
        assertEquals(b, Logic.nextUndone(data, today))
        assertTrue(Logic.isDone(data, "a", today))
        assertEquals(1, Logic.doneCount(data, today))
    }

    @Test
    fun removedHabitsDoNotCountTowardTheTotal() {
        val data = LedgerData(listOf(a), mapOf(day(0) to setOf("a", "gone")), 0)
        assertEquals(1, Logic.doneCount(data, today))
        assertEquals(1, Logic.overallStreak(data, today))
    }
}
