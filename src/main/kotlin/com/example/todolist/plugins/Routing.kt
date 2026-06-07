package com.example.todolist.plugins

import com.example.todolist.data.repository.UserRepository
import com.example.todolist.domain.repository.TaskRepository
import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.example.todolist.domain.repository.PostRepository
import com.example.todolist.domain.usecase.post.CreatePostUseCase
import com.example.todolist.domain.usecase.post.GetPostsUseCase
import com.example.todolist.domain.usecase.post.TogglePostLikeUseCase
import com.example.todolist.domain.usecase.task.CreateTaskUseCase
import com.example.todolist.domain.usecase.task.DeleteTaskUseCase
import com.example.todolist.domain.usecase.task.GetTasksUseCase
import com.example.todolist.domain.usecase.task.UpdateTaskUseCase

fun Application.configureRouting(
    taskRepository: TaskRepository,
    userRepository: UserRepository,
    postRepository: PostRepository,

) {
    val getTasksUseCase = GetTasksUseCase(taskRepository)
    val createTaskUseCase = CreateTaskUseCase(taskRepository)
    val updateTaskUseCase = UpdateTaskUseCase(taskRepository)
    val deleteTaskUseCase = DeleteTaskUseCase(taskRepository)

    val getPostsUseCase = GetPostsUseCase(postRepository)
    val createPostUseCase = CreatePostUseCase(postRepository)
    val togglePostLikeUseCase = TogglePostLikeUseCase(postRepository)
    routing {
        // === ОТКРЫТЫЕ маршруты (не требуют авторизации) ===
        authRoutes(userRepository)

        // === ЗАЩИЩЁННЫЕ маршруты (требуют JWT токен) ===
        route("/") {
            install(JwtAuth)  // ← Применяем middleware ко всем вложенным маршрутам

            taskRoutes(
                getTasksUseCase,
                createTaskUseCase,
                updateTaskUseCase,
                deleteTaskUseCase
            )
            postRoutes(
                getPostsUseCase,
                createPostUseCase,
                togglePostLikeUseCase
            )
        }
    }
}