package com.nikhil.taskreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.nikhil.taskreminder.AlertActivity
import com.nikhil.taskreminder.data.ReminderStore

/** Runs when an alarm goes off: rings the notification and opens the full-screen alert. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)
        val r = ReminderStore.get(context, id) ?: return
        if (!r.enabled) return

        val now = System.currentTimeMillis()
        val fired = r.copy(snoozeAt = null, doneAt = null, lastFiredAt = now)
        ReminderStore.upsert(context, fired)
        // Repeating reminders: book the next occurrence (skip the current minute).
        AlarmScheduler.schedule(context, fired, afterMillis = now + 60_000L)

        // Locked phone: the full-screen intent on this notification opens the alert.
        Notifications.showReminder(context, fired)

        // Phone in use: Android shows a heads-up instead, unless "Display over other apps" is allowed.
        if (fired.fullScreen && Settings.canDrawOverlays(context)) {
            try {
                context.startActivity(AlertActivity.intent(context, id))
            } catch (ignored: Exception) {
                // Falls back to the notification.
            }
        }
    }
}
