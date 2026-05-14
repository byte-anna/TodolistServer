package com.example.todolist.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String,
    val userId: String,
    val content: String,
    val taskId: String? = null,
    val createdAt: String  // ✅ String, а не LocalDateTime!
)