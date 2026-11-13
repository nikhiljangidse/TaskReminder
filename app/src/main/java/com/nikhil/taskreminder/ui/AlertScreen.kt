package com.nikhil.taskreminder.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikhil.taskreminder.data.Reminder
import com.nikhil.taskreminder.ui.theme.AppColors
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AlertScreen(reminder: Reminder, onSnooze: (Int) -> Unit, onDone: () -> Unit) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = LocalDateTime.now()
        }
    }
    val (t, ap) = timeParts(now.hour, now.minute)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.DarkBg)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Clock
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = AppColors.DarkMuted, modifier = Modifier.size(16.dp))
                Text("REMINDER", color = AppColors.DarkMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.3.sp)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(t, color = Color.White, fontSize = 84.sp, fontWeight = FontWeight.SemiBold, lineHeight = 88.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    ap,
                    color = AppColors.NextText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)),
                color = AppColors.DarkMuted,
                fontSize = 16.sp,
            )
        }

        // Task
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            PulsingBell()
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    reminder.title,
                    color = Color.White,
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                if (reminder.note.isNotBlank()) {
                    Text(
                        reminder.note,
                        color = AppColors.DarkMuted,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Actions
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Snooze for",
                color = AppColors.DarkMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(5, 10, 30).forEach { mins ->
                    OutlinedButton(
                        onClick = { onSnooze(mins) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AppColors.DarkLine),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = AppColors.DarkCard,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("$mins min", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent, contentColor = Color.White),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Mark as done", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PulsingBell() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val p by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ring",
    )
    Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        listOf(p, (p + 0.5f) % 1f).forEach { v ->
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        val s = 0.85f + 0.5f * v
                        scaleX = s
                        scaleY = s
                        alpha = 1f - v
                    }
                    .border(2.dp, AppColors.Accent, CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(Color(0xFF1E1E23), CircleShape)
                .border(1.dp, Color(0xFF2E2E35), CircleShape),
        )
        Box(
            modifier = Modifier.size(96.dp).background(AppColors.Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
    }
}
