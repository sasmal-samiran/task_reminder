package com.myreminder.app.data.local

import androidx.room.*
import com.myreminder.app.data.model.TaskType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY date ASC, time ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY time ASC")
    fun getTasksByDate(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, time ASC")
    fun getTasksByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE type = :type ORDER BY date ASC, time ASC")
    fun getTasksByType(type: TaskType): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY date DESC, time DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date >= :fromDate AND completed = 0 ORDER BY date ASC, time ASC")
    fun getUpcomingTasks(fromDate: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :query || '%' OR company LIKE '%' || :query || '%') ORDER BY date ASC, time ASC")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    // Non-flow versions for use in BroadcastReceivers
    @Query("SELECT * FROM tasks WHERE date = :date AND completed = 0 ORDER BY time ASC")
    suspend fun getTasksForDate(date: LocalDate): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE date >= :fromDate AND completed = 0 AND reminderMinutes >= 0 ORDER BY date ASC, time ASC")
    suspend fun getTasksWithReminders(fromDate: LocalDate): List<TaskEntity>

    // Get dates that have tasks in a given month (for calendar dots)
    @Query("SELECT DISTINCT date FROM tasks WHERE date BETWEEN :startDate AND :endDate")
    fun getTaskDatesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<LocalDate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)
}
