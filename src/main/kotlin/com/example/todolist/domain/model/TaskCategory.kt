package com.example.todolist.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TaskCategory {
    NONE,
    STUDY,
    WORK,
    HOME,
    PERSONAL
}
