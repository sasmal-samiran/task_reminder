package com.myreminder.app.data.model

import androidx.compose.ui.graphics.Color

enum class Priority(val displayName: String, val color: Color) {
    HIGH("High", Color(0xFFE53935)),
    MEDIUM("Medium", Color(0xFFFDD835)),
    LOW("Low", Color(0xFF43A047))
}
