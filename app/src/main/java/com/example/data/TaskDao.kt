package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, timestamp DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, isCompleted: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    fun getCompletedTasksCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks")
    fun getTotalTasksCount(): Flow<Int>
}
