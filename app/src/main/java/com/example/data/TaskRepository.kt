package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val completedTasksCount: Flow<Int> = taskDao.getCompletedTasksCount()
    val totalTasksCount: Flow<Int> = taskDao.getTotalTasksCount()

    suspend fun insert(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun updateTaskStatus(id: Int, isCompleted: Boolean) {
        taskDao.updateTaskStatus(id, isCompleted)
    }

    suspend fun deleteById(id: Int) {
        taskDao.deleteTaskById(id)
    }
}
