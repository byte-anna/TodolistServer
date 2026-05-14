package com.example.todolist.domain.model

import kotlinx.serialization.Serializable  // ✅ ИМПОРТ!
import java.time.LocalDateTime

@Serializable
data class Task(
    val id: String,
    val userId: String,
    val title: String,
    val isDone: Boolean,
    val priority: Int,
    val dueDate: String? = null,  // ✅ String вместо LocalDateTime
    val createdAt: String? = null, // ✅ String вместо LocalDateTime
    val folderId: String? = null,
    val isShared: Boolean = false  // ✅ Если добавляли
)
