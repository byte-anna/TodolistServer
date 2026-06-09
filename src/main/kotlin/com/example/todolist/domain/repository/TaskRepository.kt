package com.example.todolist.domain.repository

import com.example.todolist.domain.model.TaskCategory
import com.example.todolist.domain.model.Task
import java.time.LocalDate

interface TaskRepository {
    suspend fun getTasks(userId: String, dueDate: LocalDate? = null): List<Task>
    suspend fun getTaskById(taskId: String, userId: String): Task?

    suspend fun createTask(
        userId: String,
        title: String,
        priority: Int,
        category: TaskCategory,
        dueDate: String?
    ): Task

    suspend fun updateTask(
        taskId: String,
        userId: String,
        title: String? = null,
        isDone: Boolean? = null,
        priority: Int? = null,
        category: TaskCategory? = null,
        dueDate: String? = null
    ): Boolean

    suspend fun deleteTask(taskId: String, userId: String): Boolean
}
