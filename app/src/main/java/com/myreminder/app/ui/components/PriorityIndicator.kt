package com.myreminder.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myreminder.app.data.model.Priority
import com.myreminder.app.ui.theme.CompletedGray
import com.myreminder.app.ui.theme.PriorityHigh
import com.myreminder.app.ui.theme.PriorityLow
import com.myreminder.app.ui.theme.PriorityMedium

@Composable
fun PriorityIndicator(
    priority: Priority,
    completed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val color = if (completed) {
        CompletedGray
    } else {
        when (priority) {
            Priority.HIGH -> PriorityHigh
            Priority.MEDIUM -> PriorityMedium
            Priority.LOW -> PriorityLow
        }
    }

    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (completed) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
