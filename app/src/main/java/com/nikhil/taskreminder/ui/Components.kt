package com.nikhil.taskreminder.ui

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nikhil.taskreminder.ui.theme.AppColors

@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.semantics { contentDescription = label },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = AppColors.Accent,
            checkedBorderColor = AppColors.Accent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = AppColors.SwitchOff,
            uncheckedBorderColor = AppColors.SwitchOff,
        ),
    )
}
