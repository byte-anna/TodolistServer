package com.example.todolist.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val passwordHash: String,
    val createdAt: String
)