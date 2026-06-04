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
        // === ОТКРЫТЫЕ маршруты (не требуют авторизации) ===
        authRoutes(userRepository)

        // === ЗАЩИЩЁННЫЕ маршруты (требуют JWT токен) ===
        route("/") {
            install(JwtAuth)  // ← Применяем middleware ко всем вложенным маршрутам

            taskRoutes(taskRepository)
            postRoutes()
        }
    }
}