package com.example.todolist.domain.repository

import com.example.todolist.domain.model.Task

interface TaskRepository {
    suspend fun getTasks(userId: String): List<Task>
    suspend fun getTaskById(taskId: String, userId: String): Task?

    suspend fun createTask(
        userId: String,
        title: String,
        priority: Int,
        dueDate: String?
    ): Task

    suspend fun updateTask(
        taskId: String,
        userId: String,
        title: String? = null,
        isDone: Boolean? = null,
        priority: Int? = null,
        dueDate: String? = null
    ): Boolean

    suspend fun deleteTask(taskId: String, userId: String): Boolean
}