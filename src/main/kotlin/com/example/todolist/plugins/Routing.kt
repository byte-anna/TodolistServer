package com.example.todolist.plugins

import com.example.todolist.data.repository.UserRepository
import com.example.todolist.domain.repository.TaskRepository
import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.example.todolist.domain.repository.PostRepository
import com.example.todolist.domain.usecase.post.CreatePostUseCase
import com.example.todolist.domain.usecase.post.GetPostsUseCase
import com.example.todolist.domain.usecase.post.TogglePostLikeUseCase

fun Application.configureRouting(
    taskRepository: TaskRepository,
    userRepository: UserRepository,
    postRepository: PostRepository,

) {
    val getPostsUseCase = GetPostsUseCase(postRepository)
    val createPostUseCase = CreatePostUseCase(postRepository)
    val togglePostLikeUseCase = TogglePostLikeUseCase(postRepository)
    routing {
        // === ОТКРЫТЫЕ маршруты (не требуют авторизации) ===
        authRoutes(userRepository)

        // === ЗАЩИЩЁННЫЕ маршруты (требуют JWT токен) ===
        route("/") {
            install(JwtAuth)  // ← Применяем middleware ко всем вложенным маршрутам

            taskRoutes(taskRepository)
            postRoutes(
                getPostsUseCase,
                createPostUseCase,
                togglePostLikeUseCase
            )
        }
    }
}