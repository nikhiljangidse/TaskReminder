package com.nikhil.taskreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nikhil.taskreminder.MainActivity
import com.nikhil.taskreminder.data.Reminder
import com.nikhil.taskreminder.data.ReminderStore

/** Talks to AlarmManager: one exact alarm per reminder, keyed by the reminder id. */
object AlarmScheduler {
    const val ACTION_FIRE = "com.nikhil.taskreminder.action.FIRE"
    const val EXTRA_ID = "reminder_id"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    }

    /** Schedules (or cancels) the next ring for [reminder]. [afterMillis] = only times after this count. */
    fun schedule(context: Context, reminder: Reminder, afterMillis: Long = System.currentTimeMillis()) {
        val am = (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        val fire = firePendingIntent(context, reminder.id)
        val at = reminder.nextTriggerMillis(afterMillis)
        if (at == null) {
            am.cancel(fire)
            return
        }
        try {
            if (canScheduleExact(context)) {
                // setAlarmClock = most reliable; also shows the alarm icon in the status bar.
                val show = PendingIntent.getActivity(
                    context,
                    reminder.id,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), fire)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, fire)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, fire)
        }
    }

    fun cancel(context: Context, id: Int) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(firePendingIntent(context, id))
    }

    fun rescheduleAll(context: Context) {
        ReminderStore.all(context).forEach { schedule(context, it) }
    }

    private fun firePendingIntent(context: Context, id: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
