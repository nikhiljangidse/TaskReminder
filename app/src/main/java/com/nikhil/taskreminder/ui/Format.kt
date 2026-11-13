package com.nikhil.taskreminder.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 19:30 -> ("7:30", "PM") */
fun timeParts(hour: Int, minute: Int): Pair<String, String> {
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "$h:${minute.toString().padStart(2, '0')}" to (if (hour < 12) "AM" else "PM")
}

fun timeLabel(hour: Int, minute: Int): String {
    val (t, ap) = timeParts(hour, minute)
    return "$t $ap"
}

fun timeLabel(millis: Long): String {
    val z = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return timeLabel(z.hour, z.minute)
}

private val DAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun repeatLabel(days: Set<Int>): String = when {
    days.isEmpty() -> "Once"
    days.size == 7 -> "Every day"
    days == setOf(1, 2, 3, 4, 5) -> "Mon–Fri"
    days == setOf(6, 7) -> "Weekends"
    else -> days.sorted().joinToString(", ") { DAY_SHORT[it - 1] }
}

fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH))
}

fun countdown(fromMillis: Long, toMillis: Long): String {
    val mins = ((toMillis - fromMillis + 59_999) / 60_000).coerceAtLeast(0)
    val d = mins / (60 * 24)
    val h = (mins % (60 * 24)) / 60
    val m = mins % 60
    return when {
        d > 0 -> "in ${d}d ${h}h"
        h > 0 -> "in ${h}h ${m.toString().padStart(2, '0')}m"
        m > 0 -> "in $m min"
        else -> "now"
    }
}
