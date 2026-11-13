package com.nikhil.taskreminder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikhil.taskreminder.ui.theme.AppColors

data class SetupItem(
    val title: String,
    val description: String,
    val granted: Boolean,
    val required: Boolean,
    val open: () -> Unit,
)

@Composable
fun SetupDialog(items: List<SetupItem>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Reminder setup") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = if (item.granted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = if (item.granted) "Allowed" else "Not allowed",
                            tint = if (item.granted) AppColors.Ok else AppColors.Accent,
                            modifier = Modifier.size(22.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title + if (item.required) "" else " (recommended)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Ink,
                            )
                            Text(item.description, fontSize = 13.sp, color = AppColors.Muted)
                        }
                        if (!item.granted) {
                            TextButton(onClick = item.open) {
                                Text("Allow", color = AppColors.Accent, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
    )
}
