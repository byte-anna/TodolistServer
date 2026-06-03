package com.example.todolist.plugins

import com.example.todolist.data.repository.UserRepository
import com.example.todolist.domain.repository.TaskRepository
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    taskRepository: TaskRepository,
    userRepository: UserRepository
) {
    routing {
        // Auth routes (регистрация, логин)
        authRoutes(userRepository)

        // Task routes (CRUD задач)
        taskRoutes(taskRepository)

        // Post routes (лента, лайки)
        postRoutes()
    }
}