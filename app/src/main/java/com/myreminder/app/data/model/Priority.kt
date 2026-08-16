package com.myreminder.app.data.model

import androidx.compose.ui.graphics.Color

enum class Priority(
    val displayName: String,
    val color: Color,
    val hexColorInt: Long,
    val badgeColorInt: Long
) {
    HIGH(
        displayName = "High",
        color = Color(0xFFC62828), // Strong red
        hexColorInt = 0xFFC62828,
        badgeColorInt = 0xFFEF5350
    ),
    MEDIUM(
        displayName = "Medium",
        color = Color(0xFFD97706), // Warm amber / orange
        hexColorInt = 0xFFD97706,
        badgeColorInt = 0xFFF59E0B
    ),
    LOW(
        displayName = "Low",
        color = Color(0xFF1E40AF), // Deep royal blue
        hexColorInt = 0xFF1E40AF,
        badgeColorInt = 0xFF3B82F6
    )
}
