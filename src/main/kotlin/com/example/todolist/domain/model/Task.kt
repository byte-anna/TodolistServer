package com.example.todolist.domain.model

import kotlinx.serialization.Serializable  // ✅ ИМПОРТ!

@Serializable  // ✅ АННОТАЦИЯ!
data class Task(
    val id: String,
    val userId: String,
    val title: String,
    val isDone: Boolean,
    val priority: Int,
    val dueDate: String?,
    val folderId: String?,
    val createdAt: String
)