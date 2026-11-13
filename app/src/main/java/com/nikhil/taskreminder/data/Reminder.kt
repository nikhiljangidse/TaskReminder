package com.nikhil.taskreminder.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * One reminder.
 * - One-time reminders use [dateEpochDay] + [hour]:[minute].
 * - Repeating reminders use [repeatDays] (1 = Monday ... 7 = Sunday) and ignore the date.
 */
data class Reminder(
    val id: Int,
    val title: String,
    val note: String = "",
    val hour: Int,
    val minute: Int,
    val dateEpochDay: Long,
    val repeatDays: Set<Int> = emptySet(),
    val enabled: Boolean = true,
    val fullScreen: Boolean = true,
    val sound: Boolean = true,
    val snoozeAt: Long? = null,
    val doneAt: Long? = null,
    val lastFiredAt: Long? = null,
) {
    val isRepeating: Boolean get() = repeatDays.isNotEmpty()

    /** Next regular (non-snooze) time after [now], or null if a one-time reminder has passed. */
    fun nextRegularTrigger(now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        val time = LocalTime.of(hour, minute)
        if (!isRepeating) {
            val t = ZonedDateTime.of(LocalDate.ofEpochDay(dateEpochDay), time, now.zone)
            return if (t.isAfter(now)) t else null
        }
        for (i in 0..7) {
            val day = now.toLocalDate().plusDays(i.toLong())
            if (day.dayOfWeek.value in repeatDays) {
                val t = ZonedDateTime.of(day, time, now.zone)
                if (t.isAfter(now)) return t
            }
        }
        return null
    }

    /** When the alarm should actually ring next (snooze included). Null = nothing pending. */
    fun nextTriggerMillis(afterMillis: Long = System.currentTimeMillis()): Long? {
        if (!enabled) return null
        val snooze = snoozeAt?.takeIf { it > afterMillis }
        val now = Instant.ofEpochMilli(afterMillis).atZone(ZoneId.systemDefault())
        val regular = nextRegularTrigger(now)?.toInstant()?.toEpochMilli()
        return listOfNotNull(snooze, regular).minOrNull()
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("note", note)
        put("hour", hour)
        put("minute", minute)
        put("date", dateEpochDay)
        put("days", JSONArray(repeatDays.sorted()))
        put("enabled", enabled)
        put("fullScreen", fullScreen)
        put("sound", sound)
        snoozeAt?.let { put("snoozeAt", it) }
        doneAt?.let { put("doneAt", it) }
        lastFiredAt?.let { put("lastFiredAt", it) }
    }

    companion object {
        fun fromJson(o: JSONObject): Reminder {
            val arr = o.optJSONArray("days") ?: JSONArray()
            val days = (0 until arr.length()).map { arr.getInt(it) }.toSet()
            return Reminder(
                id = o.getInt("id"),
                title = o.optString("title", ""),
                note = o.optString("note", ""),
                hour = o.getInt("hour"),
                minute = o.getInt("minute"),
                dateEpochDay = o.getLong("date"),
                repeatDays = days,
                enabled = o.optBoolean("enabled", true),
                fullScreen = o.optBoolean("fullScreen", true),
                sound = o.optBoolean("sound", true),
                snoozeAt = if (o.has("snoozeAt")) o.getLong("snoozeAt") else null,
                doneAt = if (o.has("doneAt")) o.getLong("doneAt") else null,
                lastFiredAt = if (o.has("lastFiredAt")) o.getLong("lastFiredAt") else null,
            )
        }
    }
}
