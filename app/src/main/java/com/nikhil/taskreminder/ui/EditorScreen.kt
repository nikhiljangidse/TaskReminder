package com.nikhil.taskreminder.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikhil.taskreminder.data.Reminder
import com.nikhil.taskreminder.ui.theme.AppColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")
private val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

private fun maskToSet(mask: Int): Set<Int> = (1..7).filter { mask and (1 shl (it - 1)) != 0 }.toSet()
private fun setToMask(days: Set<Int>): Int = days.fold(0) { acc, d -> acc or (1 shl (d - 1)) }

private fun defaultDraft(): Reminder {
    val t = LocalDateTime.now().plusMinutes(5).withSecond(0).withNano(0)
    val rounded = t.plusMinutes(((5 - t.minute % 5) % 5).toLong())
    return Reminder(
        id = 0,
        title = "",
        hour = rounded.hour,
        minute = rounded.minute,
        dateEpochDay = rounded.toLocalDate().toEpochDay(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    existing: Reminder?,
    onBack: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
) {
    val context = LocalContext.current
    val initial = remember {
        val base = existing ?: defaultDraft()
        val today = LocalDate.now().toEpochDay()
        if (!base.isRepeating && base.dateEpochDay < today) base.copy(dateEpochDay = today) else base
    }

    var title by rememberSaveable { mutableStateOf(initial.title) }
    var note by rememberSaveable { mutableStateOf(initial.note) }
    var hour by rememberSaveable { mutableIntStateOf(initial.hour) }
    var minute by rememberSaveable { mutableIntStateOf(initial.minute) }
    var dateEpoch by rememberSaveable { mutableLongStateOf(initial.dateEpochDay) }
    var daysMask by rememberSaveable { mutableIntStateOf(setToMask(initial.repeatDays)) }
    var fullScreen by rememberSaveable { mutableStateOf(initial.fullScreen) }
    var sound by rememberSaveable { mutableStateOf(initial.sound) }

    var showTime by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val save: () -> Unit = save@{
        val task = title.trim()
        if (task.isEmpty()) {
            error = "Please type the task first"
            return@save
        }
        val days = maskToSet(daysMask)
        var date = dateEpoch
        if (days.isEmpty()) {
            val now = LocalDateTime.now()
            val at = LocalDateTime.of(LocalDate.ofEpochDay(date), LocalTime.of(hour, minute))
            if (!at.isAfter(now)) {
                if (LocalDate.ofEpochDay(date) == now.toLocalDate()) {
                    date += 1
                    Toast.makeText(context, "That time has passed today, so it's set for tomorrow", Toast.LENGTH_LONG).show()
                } else {
                    error = "That date and time is already in the past"
                    return@save
                }
            }
        }
        onSave(
            Reminder(
                id = existing?.id ?: 0,
                title = task,
                note = note.trim(),
                hour = hour,
                minute = minute,
                dateEpochDay = date,
                repeatDays = days,
                enabled = true,
                fullScreen = fullScreen,
                sound = sound,
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Ground)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.Ink)
            }
            Text(
                if (existing == null) "New reminder" else "Edit reminder",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Ink,
                modifier = Modifier.weight(1f),
            )
            if (existing != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete reminder", tint = AppColors.Muted)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- Time ----
            CardBox {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FieldLabel("Time")
                    TextButton(onClick = {
                        val t = LocalDateTime.now().plusMinutes(1)
                        hour = t.hour
                        minute = t.minute
                        dateEpoch = t.toLocalDate().toEpochDay()
                        daysMask = 0
                    }) {
                        Text("Test: in 1 min", color = AppColors.Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val h12 = if (hour % 12 == 0) 12 else hour % 12
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.Ground)
                            .clickable { showTime = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        Text(
                            String.format(Locale.ENGLISH, "%02d:%02d", h12, minute),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Ink,
                        )
                        Text("Tap to change", fontSize = 12.sp, color = AppColors.Muted)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SegButton("AM", selected = hour < 12) { if (hour >= 12) hour -= 12 }
                        SegButton("PM", selected = hour >= 12) { if (hour < 12) hour += 12 }
                    }
                }
            }

            // ---- Task + note ----
            CardBox {
                FieldLabel("Task")
                InputField(title, { title = it; error = null }, "What do you need to do?", 17.sp)
                error?.let { Text(it, color = AppColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                HorizontalDivider(color = AppColors.Line, modifier = Modifier.padding(vertical = 4.dp))
                FieldLabel("Note (optional)")
                InputField(note, { note = it }, "Add details", 16.sp)
            }

            // ---- Date (only for one-time reminders) ----
            if (daysMask == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Date")
                    val today = LocalDate.now().toEpochDay()
                    val other = dateEpoch != today && dateEpoch != today + 1
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceChip("Today", dateEpoch == today) { dateEpoch = today }
                        ChoiceChip("Tomorrow", dateEpoch == today + 1) { dateEpoch = today + 1 }
                        ChoiceChip(
                            label = if (other) dayLabel(LocalDate.ofEpochDay(dateEpoch)) else "Pick date",
                            selected = other,
                            icon = Icons.Default.DateRange,
                        ) { showDate = true }
                    }
                }
            }

            // ---- Repeat ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    FieldLabel("Repeat")
                    Text(
                        if (daysMask == 0) "Never" else repeatLabel(maskToSet(daysMask)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Ink,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (d in 1..7) {
                        val bit = 1 shl (d - 1)
                        val on = daysMask and bit != 0
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (on) AppColors.Accent else AppColors.Card)
                                .border(1.dp, if (on) AppColors.Accent else AppColors.ChipLine, CircleShape)
                                .toggleable(value = on, role = Role.Checkbox, onValueChange = { daysMask = daysMask xor bit })
                                .semantics { contentDescription = DAY_NAMES[d - 1] },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                DAY_LETTERS[d - 1],
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (on) Color.White else AppColors.Ink,
                            )
                        }
                    }
                }
                if (daysMask != 0) {
                    Text("Rings every week on the selected days.", fontSize = 13.sp, color = AppColors.Muted)
                }
            }

            // ---- Alert options ----
            CardBox {
                OptionRow(
                    title = "Full-screen alert",
                    subtitle = "Wakes the screen and shows the task, even when locked",
                    checked = fullScreen,
                ) { fullScreen = it }
                HorizontalDivider(color = AppColors.Line)
                OptionRow(
                    title = "Sound",
                    subtitle = "Rings with your alarm tone (vibrates either way)",
                    checked = sound,
                ) { sound = it }
            }
        }

        Button(
            onClick = save,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent, contentColor = Color.White),
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save reminder", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    // ---- Dialogs ----
    if (showTime) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = state.hour
                    minute = state.minute
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }

    if (showDate) {
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(dateEpoch).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dateEpoch = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete reminder?") },
            text = { Text("\"${existing.title}\" will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(existing)
                }) { Text("Delete", color = AppColors.Accent) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CardBox(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Card)
            .border(1.dp, AppColors.Line, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Muted)
}

@Composable
private fun InputField(value: String, onChange: (String) -> Unit, placeholder: String, size: TextUnit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = size, fontWeight = FontWeight.Medium, color = AppColors.Ink),
        cursorBrush = SolidColor(AppColors.Accent),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, fontSize = size, color = AppColors.Muted)
                inner()
            }
        },
    )
}

@Composable
private fun SegButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(44.dp)
            .clip(shape)
            .background(if (selected) AppColors.Accent else AppColors.Card)
            .border(1.dp, if (selected) AppColors.Accent else AppColors.ChipLine, shape)
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else AppColors.Ink,
        )
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val fg = if (selected) Color.White else AppColors.Ink
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) AppColors.Accent else AppColors.Card)
            .border(1.dp, if (selected) AppColors.Accent else AppColors.ChipLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun OptionRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
            Text(subtitle, fontSize = 13.sp, color = AppColors.Muted)
        }
        AppSwitch(checked = checked, onCheckedChange = onChange, label = title)
    }
}
