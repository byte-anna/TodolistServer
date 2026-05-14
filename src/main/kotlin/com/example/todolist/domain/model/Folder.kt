package com.example.todolist.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Folder(
    val id: String,
    val userId: String,
    val name: String,
    val color: String,
    val createdAt: String
)