package com.rowanrisk.ledger

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

data class Habit(val id: String, val name: String)

/**
 * Same shape as the web app: habits, a log of date -> habit ids done that day, and the best streak.
 * That keeps the JSON export from the web app importable here, and the other way round.
 */
data class LedgerData(
    val habits: List<Habit>,
    val log: Map<String, Set<String>>,
    val bestStreak: Int,
)

object Logic {
    fun idsDone(data: LedgerData, day: LocalDate): Set<String> = data.log[day.toString()].orEmpty()

    fun isDone(data: LedgerData, habitId: String, day: LocalDate = LocalDate.now()): Boolean =
        habitId in idsDone(data, day)

    fun doneCount(data: LedgerData, day: LocalDate = LocalDate.now()): Int {
        val ids = idsDone(data, day)
        return data.habits.count { it.id in ids }
    }

    fun nextUndone(data: LedgerData, day: LocalDate = LocalDate.now()): Habit? {
        val ids = idsDone(data, day)
        return data.habits.firstOrNull { it.id !in ids }
    }

    /** Consecutive days a habit was done. A not-yet-done today does not break the streak. */
    fun habitStreak(data: LedgerData, habitId: String, today: LocalDate = LocalDate.now()): Int {
        var streak = 0
        var day = today
        while (true) {
            if (habitId in idsDone(data, day)) {
                streak++
            } else if (day != today) {
                break
            }
            day = day.minusDays(1)
        }
        return streak
    }

    /** Consecutive days where every habit was done. Today only counts once it is complete. */
    fun overallStreak(data: LedgerData, today: LocalDate = LocalDate.now()): Int {
        if (data.habits.isEmpty()) return 0
        var streak = 0
        var day = today
        while (true) {
            val ids = idsDone(data, day)
            val complete = data.habits.all { it.id in ids }
            if (complete) {
                streak++
            } else if (day != today) {
                break
            }
            day = day.minusDays(1)
        }
        return streak
    }
}

object Store {
    private const val PREFS = "ledger"
    private const val KEY = "state"
    private val lock = Any()

    private val defaultHabitNames = listOf(
        "Dry-fire / range reps",
        "PT block",
        "Deep work block (RRS / client work)",
        "No mindless scrolling before noon",
    )

    fun load(context: Context): LedgerData = synchronized(lock) { loadLocked(context) }

    fun toggle(context: Context, habitId: String, day: LocalDate = LocalDate.now()): LedgerData =
        synchronized(lock) {
            val data = loadLocked(context)
            if (data.habits.none { it.id == habitId }) return@synchronized data
            val key = day.toString()
            val ids = data.log[key].orEmpty()
            val newIds = if (habitId in ids) ids - habitId else ids + habitId
            val newLog = if (newIds.isEmpty()) data.log - key else data.log + (key to newIds)
            commitLocked(context, data.copy(log = newLog))
        }

    fun addHabit(context: Context, name: String): LedgerData = synchronized(lock) {
        val data = loadLocked(context)
        val clean = name.trim().take(60)
        if (clean.isEmpty()) return@synchronized data
        val habit = Habit("h${System.currentTimeMillis()}", clean)
        commitLocked(context, data.copy(habits = data.habits + habit))
    }

    fun removeHabit(context: Context, habitId: String): LedgerData = synchronized(lock) {
        val data = loadLocked(context)
        commitLocked(context, data.copy(habits = data.habits.filter { it.id != habitId }))
    }

    fun exportJson(data: LedgerData): String {
        val root = JSONObject()
            .put("app", "discipline-ledger")
            .put("version", 1)
            .put("exportedAt", Instant.now().toString())
            .put("data", toJson(data))
        return root.toString(2)
    }

    /** Accepts a web-app export or a bare data object. Throws if it is not valid, changing nothing. */
    fun importJson(context: Context, text: String): LedgerData = synchronized(lock) {
        val root = JSONObject(text)
        val dataObj = if (root.optString("app") == "discipline-ledger") root.optJSONObject("data") else root
        val parsed = dataObj?.let { fromJson(it) }
            ?: throw IllegalArgumentException("not a Discipline Ledger export")
        commitLocked(context, parsed)
    }

    // ---- internals (call only while holding [lock]) ----

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun loadLocked(context: Context): LedgerData {
        val raw = prefs(context).getString(KEY, null)
        if (raw != null) {
            val parsed = runCatching { fromJson(JSONObject(raw)) }.getOrNull()
            if (parsed != null) return parsed
        }
        // First run: create the starter habits once and keep them, so their ids stay stable.
        val stamp = System.currentTimeMillis()
        val fresh = LedgerData(
            habits = defaultHabitNames.mapIndexed { i, n -> Habit("h${stamp}_$i", n) },
            log = emptyMap(),
            bestStreak = 0,
        )
        saveLocked(context, fresh)
        return fresh
    }

    private fun commitLocked(context: Context, data: LedgerData): LedgerData {
        val streak = Logic.overallStreak(data)
        val updated = if (streak > data.bestStreak) data.copy(bestStreak = streak) else data
        saveLocked(context, updated)
        return updated
    }

    private fun saveLocked(context: Context, data: LedgerData) {
        prefs(context).edit().putString(KEY, toJson(data).toString()).apply()
    }

    private fun toJson(data: LedgerData): JSONObject {
        val habits = JSONArray()
        data.habits.forEach { habits.put(JSONObject().put("id", it.id).put("name", it.name)) }
        val log = JSONObject()
        for (day in data.log.keys.sorted()) {
            val ids = JSONObject()
            data.log.getValue(day).forEach { ids.put(it, true) }
            log.put(day, ids)
        }
        return JSONObject()
            .put("habits", habits)
            .put("log", log)
            .put("bestStreak", data.bestStreak)
    }

    private fun fromJson(o: JSONObject): LedgerData? {
        val habitsArr = o.optJSONArray("habits") ?: return null
        val logObj = o.optJSONObject("log") ?: return null

        val habits = ArrayList<Habit>()
        for (i in 0 until habitsArr.length()) {
            val h = habitsArr.optJSONObject(i) ?: return null
            val id = h.optString("id", "")
            if (id.isEmpty()) return null
            habits.add(Habit(id, h.optString("name", "")))
        }

        val log = HashMap<String, Set<String>>()
        val days = logObj.keys()
        while (days.hasNext()) {
            val day = days.next()
            val dayObj = logObj.optJSONObject(day) ?: continue
            val ids = HashSet<String>()
            val keys = dayObj.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                if (dayObj.optBoolean(id, false)) ids.add(id)
            }
            if (ids.isNotEmpty()) log[day] = ids
        }

        return LedgerData(habits, log, o.optInt("bestStreak", 0).coerceAtLeast(0))
    }
}
