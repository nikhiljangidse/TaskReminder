package com.nikhil.taskreminder

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.nikhil.taskreminder.alarm.AlarmScheduler
import com.nikhil.taskreminder.alarm.ReminderActions
import com.nikhil.taskreminder.data.ReminderStore
import com.nikhil.taskreminder.ui.AlertScreen
import com.nikhil.taskreminder.ui.theme.TaskReminderTheme

/** The full-screen reminder. Shows over the lock screen and turns the screen on. */
class AlertActivity : ComponentActivity() {
    private var reminderId by mutableIntStateOf(-1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        ReminderStore.init(this)
        reminderId = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)

        setContent {
            TaskReminderTheme {
                val items by ReminderStore.items.collectAsState()
                val reminder = items.firstOrNull { it.id == reminderId }

                // Close when the reminder is handled (here or from the notification) or deleted.
                LaunchedEffect(reminder) {
                    if (reminder == null || reminder.doneAt != null || reminder.snoozeAt != null) finish()
                }

                if (reminder != null) {
                    AlertScreen(
                        reminder = reminder,
                        onSnooze = { minutes ->
                            ReminderActions.snooze(this, reminder.id, minutes)
                            finish()
                        },
                        onDone = {
                            ReminderActions.done(this, reminder.id)
                            finish()
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reminderId = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        fun intent(context: Context, id: Int): Intent =
            Intent(context, AlertActivity::class.java)
                .putExtra(AlarmScheduler.EXTRA_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
