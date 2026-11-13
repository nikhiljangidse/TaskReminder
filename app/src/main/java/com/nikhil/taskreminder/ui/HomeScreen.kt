package com.nikhil.taskreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikhil.taskreminder.data.Reminder
import com.nikhil.taskreminder.ui.theme.AppColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class Upcoming(val reminder: Reminder, val at: Long)

@Composable
fun HomeScreen(
    reminders: List<Reminder>,
    setupNeeded: Boolean,
    onOpenSetup: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Reminder) -> Unit,
    onToggle: (Reminder, Boolean) -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = System.currentTimeMillis()
        }
    }

    val zone = ZoneId.systemDefault()
    val upcoming = remember(reminders, now) {
        val nowZoned = Instant.ofEpochMilli(now).atZone(zone)
        reminders.mapNotNull { r ->
            val snooze = r.snoozeAt?.takeIf { r.enabled && it > now }
            val regular = r.nextRegularTrigger(nowZoned)?.toInstant()?.toEpochMilli()
            val at = listOfNotNull(snooze, regular).minOrNull()
            if (at == null) null else Upcoming(r, at)
        }.sortedBy { it.at }
    }
    val upcomingIds = upcoming.map { it.reminder.id }.toSet()
    val earlier = reminders.filter { it.id !in upcomingIds }.sortedByDescending { it.lastFiredAt ?: 0L }
    val next = upcoming.firstOrNull { it.reminder.enabled }
    val groups = upcoming.groupBy { dayLabel(Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate()) }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Ground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = bottomInset + 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") { Header(onOpenSetup) }
            if (setupNeeded) item(key = "setup") { SetupCard(onOpenSetup) }
            if (next != null) item(key = "next") { NextCard(next, now) }
            if (reminders.isEmpty()) item(key = "empty") { EmptyState() }

            groups.forEach { (label, rows) ->
                item(key = "h_$label") {
                    SectionHeader(label, if (rows.size == 1) "1 reminder" else "${rows.size} reminders")
                }
                items(rows, key = { it.reminder.id }) { u ->
                    ReminderRow(
                        r = u.reminder,
                        now = now,
                        onClick = { onEdit(u.reminder) },
                        onToggle = { on -> onToggle(u.reminder, on) },
                    )
                }
            }
            if (earlier.isNotEmpty()) {
                item(key = "h_earlier") { SectionHeader("Earlier", "") }
                items(earlier, key = { it.id }) { r -> PastRow(r, onClick = { onEdit(r) }) }
            }
        }

        ExtendedFloatingActionButton(
            text = { Text("New reminder", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            onClick = onAdd,
            shape = RoundedCornerShape(18.dp),
            containerColor = AppColors.Accent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun Header(onOpenSetup: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.Muted,
            )
            Text("Reminders", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
        }
        IconButton(
            onClick = onOpenSetup,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AppColors.Card)
                .border(1.dp, Color(0xFFE4E3DF), CircleShape),
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Setup and permissions", tint = AppColors.Ink)
        }
    }
}

@Composable
private fun SetupCard(onOpenSetup: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.WarnBg)
            .border(1.dp, AppColors.WarnLine, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenSetup)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = AppColors.Accent)
        Column(modifier = Modifier.weight(1f)) {
            Text("Setup needed", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
            Text(
                "Allow a few permissions so reminders can ring and open full screen.",
                fontSize = 13.sp,
                color = AppColors.Muted,
            )
        }
        Text("Fix", color = AppColors.Accent, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NextCard(next: Upcoming, now: Long) {
    val z = Instant.ofEpochMilli(next.at).atZone(ZoneId.systemDefault())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Ink)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "NEXT REMINDER",
                color = AppColors.NextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = AppColors.NextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            next.reminder.title,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${dayLabel(z.toLocalDate())} · ${timeLabel(z.hour, z.minute)}",
                color = AppColors.NextText,
                fontSize = 15.sp,
            )
            Text(
                countdown(now, next.at),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AppColors.Accent)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            tint = AppColors.Muted,
            modifier = Modifier.size(40.dp),
        )
        Text("No reminders yet", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
        Text(
            "Tap New reminder to set a time and a task.",
            fontSize = 14.sp,
            color = AppColors.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
        if (trailing.isNotEmpty()) Text(trailing, fontSize = 13.sp, color = AppColors.Muted)
    }
}

@Composable
private fun ReminderRow(r: Reminder, now: Long, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val (t, ap) = timeParts(r.hour, r.minute)
    val snoozed = r.snoozeAt?.takeIf { r.enabled && it > now }
    val mode = if (r.fullScreen) "Full-screen" else "Notification"
    val meta = if (snoozed != null) "Snoozed till ${timeLabel(snoozed)}" else "${repeatLabel(r.repeatDays)} · $mode"
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Card)
            .border(1.dp, AppColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(64.dp)) {
            Text(t, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = if (r.enabled) AppColors.Ink else AppColors.Muted)
            Text(ap, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Muted)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                r.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(meta, fontSize = 13.sp, color = if (snoozed != null) AppColors.Accent else AppColors.Muted)
        }
        AppSwitch(checked = r.enabled, onCheckedChange = onToggle, label = "Reminder ${r.title}")
    }
}

@Composable
private fun PastRow(r: Reminder, onClick: () -> Unit) {
    val (t, ap) = timeParts(r.hour, r.minute)
    val done = r.doneAt
    val meta = when {
        done != null -> "Done at ${timeLabel(done)}"
        r.lastFiredAt != null -> "Missed · tap to set again"
        else -> "Didn't ring · tap to set again"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.DoneBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(64.dp)) {
            Text(t, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Muted)
            Text(ap, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Muted)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                r.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Muted,
                textDecoration = if (done != null) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(meta, fontSize = 13.sp, color = AppColors.Muted)
        }
        if (done != null) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(AppColors.OkBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = AppColors.Ok, modifier = Modifier.size(16.dp))
            }
        }
    }
}
