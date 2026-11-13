package com.nikhil.taskreminder.alarm

import android.content.Context
import com.nikhil.taskreminder.data.ReminderStore

/** Done / Snooze, shared by the full-screen alert and the notification buttons. */
object ReminderActions {
    fun done(context: Context, id: Int) {
        Notifications.cancel(context, id) // stops the ringing
        val r = ReminderStore.get(context, id) ?: return
        val updated = r.copy(doneAt = System.currentTimeMillis(), snoozeAt = null)
        ReminderStore.upsert(context, updated)
        AlarmScheduler.schedule(context, updated)
    }

    fun snooze(context: Context, id: Int, minutes: Int) {
        Notifications.cancel(context, id)
        val r = ReminderStore.get(context, id) ?: return
        val updated = r.copy(
            enabled = true,
            snoozeAt = System.currentTimeMillis() + minutes * 60_000L,
            doneAt = null,
        )
        ReminderStore.upsert(context, updated)
        AlarmScheduler.schedule(context, updated)
    }
}
