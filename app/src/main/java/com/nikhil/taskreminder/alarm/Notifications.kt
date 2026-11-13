package com.nikhil.taskreminder.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nikhil.taskreminder.AlertActivity
import com.nikhil.taskreminder.R
import com.nikhil.taskreminder.data.Reminder
import com.nikhil.taskreminder.ui.timeLabel

object Notifications {
    private const val CH_SOUND = "reminder_alarm_v1"
    private const val CH_SILENT = "reminder_silent_v1"
    private val VIBRATION = longArrayOf(0, 700, 500, 700, 500, 700)

    fun ensureChannels(context: Context) {
        val nm = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val loud = NotificationChannel(CH_SOUND, "Reminders (ring)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Rings and opens full screen at the reminder time"
            setSound(tone, alarmAttrs)
            enableVibration(true)
            vibrationPattern = VIBRATION
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val silent = NotificationChannel(CH_SILENT, "Reminders (vibrate only)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Vibrates and opens full screen, without sound"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = VIBRATION
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannels(listOf(loud, silent))
    }

    fun canUseFullScreen(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        return (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent()
    }

    @SuppressLint("MissingPermission")
    fun showReminder(context: Context, r: Reminder) {
        ensureChannels(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val alertPi = PendingIntent.getActivity(
            context,
            r.id,
            AlertActivity.intent(context, r.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, if (r.sound) CH_SOUND else CH_SILENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.title)
            .setContentText(r.note.ifBlank { "Reminder · ${timeLabel(r.hour, r.minute)}" })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(alertPi)
            .addAction(0, "Snooze 10 min", ActionReceiver.pendingIntent(context, r.id, ActionReceiver.ACTION_SNOOZE, 10))
            .addAction(0, "Done", ActionReceiver.pendingIntent(context, r.id, ActionReceiver.ACTION_DONE, 0))
        if (r.fullScreen) builder.setFullScreenIntent(alertPi, true)

        val notification = builder.build()
        // Keep ringing until the user taps Done / Snooze (like an alarm clock).
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        NotificationManagerCompat.from(context).notify(r.id, notification)
    }

    fun cancel(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }
}
