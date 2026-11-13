package com.nikhil.taskreminder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import com.nikhil.taskreminder.alarm.AlarmScheduler
import com.nikhil.taskreminder.alarm.Notifications
import com.nikhil.taskreminder.data.Reminder
import com.nikhil.taskreminder.data.ReminderStore
import com.nikhil.taskreminder.ui.EditorScreen
import com.nikhil.taskreminder.ui.HomeScreen
import com.nikhil.taskreminder.ui.SetupDialog
import com.nikhil.taskreminder.ui.SetupItem
import com.nikhil.taskreminder.ui.dayLabel
import com.nikhil.taskreminder.ui.theme.TaskReminderTheme
import com.nikhil.taskreminder.ui.timeLabel
import java.time.Instant
import java.time.ZoneId

private sealed interface Screen {
    data object Home : Screen
    data class Edit(val id: Int?) : Screen
}

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)
    private var askedNotificationPermission = false

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { resumeTick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderStore.init(this)
        Notifications.ensureChannels(this)
        if (needsNotificationPermission()) requestNotificationPermission()

        setContent {
            TaskReminderTheme {
                val reminders by ReminderStore.items.collectAsState()
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                var showSetup by remember { mutableStateOf(false) }
                val tick = resumeTick
                val setupItems = remember(tick) { buildSetupItems() }

                when (val s = screen) {
                    Screen.Home -> HomeScreen(
                        reminders = reminders,
                        setupNeeded = setupItems.any { it.required && !it.granted },
                        onOpenSetup = { showSetup = true },
                        onAdd = { screen = Screen.Edit(null) },
                        onEdit = { screen = Screen.Edit(it.id) },
                        onToggle = { r, on -> toggleReminder(r, on) },
                    )

                    is Screen.Edit -> {
                        BackHandler { screen = Screen.Home }
                        EditorScreen(
                            existing = s.id?.let { id -> reminders.firstOrNull { it.id == id } },
                            onBack = { screen = Screen.Home },
                            onSave = {
                                saveReminder(it)
                                screen = Screen.Home
                            },
                            onDelete = {
                                deleteReminder(it)
                                screen = Screen.Home
                            },
                        )
                    }
                }

                if (showSetup) SetupDialog(items = setupItems, onDismiss = { showSetup = false })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Permissions may have changed in Settings; re-book every alarm and refresh the setup state.
        AlarmScheduler.rescheduleAll(this)
        resumeTick++
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            askedNotificationPermission = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun saveReminder(draft: Reminder) {
        val id = if (draft.id > 0) draft.id else ReminderStore.newId(this)
        val r = draft.copy(id = id, enabled = true, snoozeAt = null, doneAt = null, lastFiredAt = null)
        ReminderStore.upsert(this, r)
        Notifications.cancel(this, id)
        AlarmScheduler.schedule(this, r)

        val at = r.nextTriggerMillis()
        val msg = if (at != null) {
            val z = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
            "Reminder set for ${dayLabel(z.toLocalDate())}, ${timeLabel(z.hour, z.minute)}"
        } else {
            "Reminder saved"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        if (needsNotificationPermission()) requestNotificationPermission()
    }

    private fun toggleReminder(r: Reminder, on: Boolean) {
        val updated = r.copy(enabled = on, snoozeAt = if (on) r.snoozeAt else null)
        ReminderStore.upsert(this, updated)
        if (!on) Notifications.cancel(this, r.id)
        AlarmScheduler.schedule(this, updated)
    }

    private fun deleteReminder(r: Reminder) {
        AlarmScheduler.cancel(this, r.id)
        Notifications.cancel(this, r.id)
        ReminderStore.delete(this, r.id)
    }

    private fun buildSetupItems(): List<SetupItem> {
        val pkgUri = Uri.parse("package:$packageName")
        val items = mutableListOf<SetupItem>()

        items += SetupItem(
            title = "Notifications",
            description = "Needed to ring and show your reminders.",
            granted = NotificationManagerCompat.from(this).areNotificationsEnabled(),
            required = true,
        ) {
            if (needsNotificationPermission() && !askedNotificationPermission) {
                requestNotificationPermission()
            } else {
                openSettings(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            items += SetupItem(
                title = "Alarms & reminders",
                description = "Lets reminders go off at the exact minute.",
                granted = AlarmScheduler.canScheduleExact(this),
                required = true,
            ) { openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri)) }
        }

        if (Build.VERSION.SDK_INT >= 34) {
            items += SetupItem(
                title = "Full-screen alerts",
                description = "Shows the reminder full screen over the lock screen.",
                granted = Notifications.canUseFullScreen(this),
                required = true,
            ) { openSettings(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkgUri)) }
        }

        items += SetupItem(
            title = "Display over other apps",
            description = "Opens the full-screen reminder even while you are using the phone.",
            granted = Settings.canDrawOverlays(this),
            required = false,
        ) { openSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri)) }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        items += SetupItem(
            title = "No battery restriction",
            description = "Stops the phone from delaying or killing reminders (important on Xiaomi, Vivo, Oppo, Realme).",
            granted = pm.isIgnoringBatteryOptimizations(packageName),
            required = false,
        ) { openSettings(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri)) }

        return items
    }

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            } catch (ignored: Exception) {
                Toast.makeText(this, "Open Settings > Apps > Task Reminder", Toast.LENGTH_LONG).show()
            }
        }
    }
}
