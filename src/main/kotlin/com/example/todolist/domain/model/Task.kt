package com.example.todolist.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: String,
    val userId: String,
    val title: String,
    val isDone: Boolean,
    val priority: Int,
    val dueDate: String? = null,
    val createdAt: String? = null,
    val isShared: Boolean = false
)
