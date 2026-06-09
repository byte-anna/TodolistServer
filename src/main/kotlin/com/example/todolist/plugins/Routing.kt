package com.example.todolist.plugins

import com.example.todolist.domain.repository.AuthRepository
import com.example.todolist.domain.repository.PostRepository
import com.example.todolist.domain.repository.TaskRepository
import com.example.todolist.domain.usecase.post.CreatePostUseCase
import com.example.todolist.domain.usecase.post.GetPostsUseCase
import com.example.todolist.domain.usecase.post.TogglePostLikeUseCase
import com.example.todolist.domain.usecase.task.CreateTaskUseCase
import com.example.todolist.domain.usecase.task.DeleteTaskUseCase
import com.example.todolist.domain.usecase.task.GetTasksUseCase
import com.example.todolist.domain.usecase.task.UpdateTaskUseCase
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(
    taskRepository: TaskRepository,
    authRepository: AuthRepository,
    postRepository: PostRepository
) {
    val getTasksUseCase = GetTasksUseCase(taskRepository)
    val createTaskUseCase = CreateTaskUseCase(taskRepository)
    val updateTaskUseCase = UpdateTaskUseCase(taskRepository)
    val deleteTaskUseCase = DeleteTaskUseCase(taskRepository)

    val getPostsUseCase = GetPostsUseCase(postRepository)
    val createPostUseCase = CreatePostUseCase(postRepository)
    val togglePostLikeUseCase = TogglePostLikeUseCase(postRepository)

    routing {
        authRoutes(authRepository)

        route("/") {
            install(JwtAuth)

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
