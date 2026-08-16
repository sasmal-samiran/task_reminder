package com.myreminder.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.myreminder.app.data.model.Priority
import com.myreminder.app.data.model.TaskType
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val company: String,
    val type: TaskType,
    val date: LocalDate,
    val time: LocalTime?,
    val meetingLink: String?,
    val location: String?,
    val priority: Priority,
    val notes: String?,
    val reminderMinutes: Int,  // -1 = no reminder, 0 = at time, 5/15/30/60/1440
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
