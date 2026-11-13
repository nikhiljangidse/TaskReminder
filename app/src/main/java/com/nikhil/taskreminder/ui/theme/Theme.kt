package com.nikhil.taskreminder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Accent = Color(0xFFC2410C)
    val Ink = Color(0xFF17171A)
    val Muted = Color(0xFF5C5C66)
    val Ground = Color(0xFFF6F5F2)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFECEBE7)
    val ChipLine = Color(0xFFDAD9D4)
    val DoneBg = Color(0xFFEFEEEA)
    val SwitchOff = Color(0xFFC9C8CE)
    val WarnBg = Color(0xFFFFF4E5)
    val WarnLine = Color(0xFFF3C9A8)
    val OkBg = Color(0xFFD9F2E3)
    val Ok = Color(0xFF166534)
    val NextMuted = Color(0xFFB8B8C2)
    val NextText = Color(0xFFD9D9E0)
    val DarkBg = Color(0xFF121215)
    val DarkCard = Color(0xFF24242A)
    val DarkLine = Color(0xFF33333B)
    val DarkMuted = Color(0xFFA9A9B4)
}

@Composable
fun TaskReminderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.Accent,
            onPrimary = Color.White,
            background = AppColors.Ground,
            onBackground = AppColors.Ink,
            surface = AppColors.Card,
            onSurface = AppColors.Ink,
            surfaceVariant = AppColors.Ground,
            onSurfaceVariant = AppColors.Muted,
        ),
        content = content,
    )
}
