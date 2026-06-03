package com.example.todolist.plugins

import kotlinx.serialization.Serializable

// ============ AUTH DTOs ============

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val token: String? = null
)

// ============ TASK DTOs ============

@Serializable
data class CreateTaskRequest(
    val title: String,
    val priority: Int = 1,
    val dueDate: String? = null
)

@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val isDone: Boolean? = null,
    val dueDate: String? = null,
    val priority: Int? = null
)

// ============ POST DTOs ============

@Serializable
data class CreatePostRequest(
    val userId: String,
    val content: String,
    val taskId: String? = null
)

@Serializable
data class LikeRequest(val userId: String)

// ============ COMMON ============

@Serializable
data class ErrorResponse(val error: String)