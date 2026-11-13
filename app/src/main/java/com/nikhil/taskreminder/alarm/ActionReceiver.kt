package com.nikhil.taskreminder.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the "Done" and "Snooze" buttons on the notification. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)
        if (id < 0) return
        when (intent.action) {
            ACTION_DONE -> ReminderActions.done(context, id)
            ACTION_SNOOZE -> ReminderActions.snooze(context, id, intent.getIntExtra(EXTRA_MINUTES, 10))
        }
    }

    companion object {
        const val ACTION_DONE = "com.nikhil.taskreminder.action.DONE"
        const val ACTION_SNOOZE = "com.nikhil.taskreminder.action.SNOOZE"
        private const val EXTRA_MINUTES = "minutes"

        fun pendingIntent(context: Context, id: Int, action: String, minutes: Int): PendingIntent {
            val intent = Intent(context, ActionReceiver::class.java)
                .setAction(action)
                .putExtra(AlarmScheduler.EXTRA_ID, id)
                .putExtra(EXTRA_MINUTES, minutes)
            val requestCode = id * 10 + if (action == ACTION_DONE) 1 else 2
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
