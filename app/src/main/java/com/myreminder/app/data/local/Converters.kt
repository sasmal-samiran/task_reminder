package com.myreminder.app.data.local

import androidx.room.TypeConverter
import com.myreminder.app.data.model.Priority
import com.myreminder.app.data.model.TaskType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.format(timeFormatter)

    @TypeConverter
    fun toLocalTime(timeStr: String?): LocalTime? = timeStr?.let { LocalTime.parse(it, timeFormatter) }

    @TypeConverter
    fun fromTaskType(type: TaskType): String = type.name

    @TypeConverter
    fun toTaskType(name: String): TaskType = TaskType.valueOf(name)

    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(name: String): Priority = Priority.valueOf(name)
}
