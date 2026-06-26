package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedTasksCount: StateFlow<Int> = repository.completedTasksCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalTasksCount: StateFlow<Int> = repository.totalTasksCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun addTask(title: String, description: String, priority: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insert(Task(title = title, description = description, priority = priority))
        }
    }

    fun updateTaskStatus(id: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateTaskStatus(id, isCompleted)
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
