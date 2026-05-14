package com.example.todolist.plugins

import com.example.todolist.data.db.PostLikesTable
import com.example.todolist.data.db.PostLikesTable.select
import com.example.todolist.data.db.PostsTable
import com.example.todolist.data.repository.UserRepository
import com.example.todolist.domain.model.Post
import com.example.todolist.domain.repository.FolderRepository
import com.example.todolist.domain.repository.TaskRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction  // ✅ Для транзакций
import java.time.LocalDateTime  // ✅ Для генерации времени


@kotlinx.serialization.Serializable
data class CreatePostRequest(
    val userId: String,
    val content: String,
    val taskId: String? = null
)

@Serializable
data class LikeRequest(val userId: String)

// Request/Response модели
@Serializable
data class RegisterRequest(val email: String, val password: String,val displayName: String? = null)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val userId: String, val email: String, val displayName: String? = null)

@Serializable
data class CreateTaskRequest(val title: String, val priority: Int = 1, val folderId: String? = null, val dueDate: String? = null)

@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val isDone: Boolean? = null,
    val dueDate: String? = null,
    val folderId: String? = null,
    val priority: Int? = null
)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class CreateFolderRequest(val name: String, val color: String = "#6200EE")

fun Application.configureRouting(
    taskRepository: TaskRepository,
    userRepository: UserRepository,
    folderRepository: FolderRepository
) {
    routing {
        // === AUTH: REGISTER ===
        post("/auth/register") {
            try {
                val request = call.receive<RegisterRequest>()

                if (request.email.isBlank() || request.password.length < 6) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Email и пароль обязательны. Минимум 6 символов"))
                    return@post
                }

                val existingUser = userRepository.findUserByEmail(request.email)
                if (existingUser != null) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("Пользователь с таким email уже существует"))
                    return@post
                }

                val passwordHash = userRepository.hashPassword(request.password)
                val newUser = userRepository.createUser(
                    request.email,
                    request.displayName,  // ✅ Передаём имя
                    passwordHash
                )

                call.respond(HttpStatusCode.Created, AuthResponse(newUser.id, newUser.email, newUser.displayName))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Ошибка регистрации: ${e.message}"))
            }
        }

        // === AUTH: LOGIN ===
        post("/auth/login") {
            try {
                val request = call.receive<LoginRequest>()

                val user = userRepository.findUserByEmail(request.email)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Пользователь не найден"))
                    return@post
                }

                val passwordHash = userRepository.hashPassword(request.password)
                if (user.passwordHash != passwordHash) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Неверный пароль"))
                    return@post
                }

                call.respond(HttpStatusCode.OK, AuthResponse(user.id, user.email))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Ошибка входа: ${e.message}"))
            }
        }

        // === TASKS ENDPOINTS ===
        get("/tasks") {
            val userId = call.parameters["userId"]
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId is required"))
                return@get
            }

            try {
                val tasks = taskRepository.getTasks(userId)
                call.respond(HttpStatusCode.OK, tasks)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unknown error"))
            }
        }

        post("/tasks") {
            val userId = call.parameters["userId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId is required"))
                return@post
            }

            try {
                val request = call.receive<CreateTaskRequest>()
                if (request.title.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Title cannot be empty"))
                    return@post
                }

                val newTask = taskRepository.createTask(userId, request.title.trim(), request.priority, request.folderId, request.dueDate)
                call.respond(HttpStatusCode.Created, newTask)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Failed to create task"))
            }
        }

        delete("/tasks/{id}") {
            val taskId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Task ID is required"))
                return@delete
            }
            val userId = call.parameters["userId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId is required"))
                return@delete
            }

            try {
                val deleted = taskRepository.deleteTask(taskId, userId)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Task deleted"))
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Task not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Failed to delete task"))
            }
        }

        put("/tasks/{id}") {
            val taskId = call.parameters["id"] ?: return@put
            val userId = call.parameters["userId"] ?: return@put

            try {
                val request = call.receive<UpdateTaskRequest>()
                val updated = taskRepository.updateTask(
                    taskId,
                    userId,
                    request.title,
                    request.isDone,
                    request.priority,
                    request.dueDate,  // ✅ Добавили
                    folderId = request.folderId
                )
                if (updated) call.respond(HttpStatusCode.OK)
                else call.respond(HttpStatusCode.NotFound)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        // Получить все папки пользователя
        get("/folders") {
            val userId = call.parameters["userId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId is required"))
                return@get
            }
            try {
                val folders = folderRepository.getFolders(userId)
                call.respond(HttpStatusCode.OK, folders)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Error"))
            }
        }

// Создать папку
        post("/folders") {
            val userId = call.parameters["userId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId is required"))
                return@post
            }
            try {
                val request = call.receive<CreateFolderRequest>()
                if (request.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Name cannot be empty"))
                    return@post
                }
                val newFolder = folderRepository.createFolder(userId, request.name.trim(), request.color)
                call.respond(HttpStatusCode.Created, newFolder)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Error"))
            }
        }

// Удалить папку
        delete("/folders/{id}") {
            val folderId = call.parameters["id"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Folder ID is required"))
                return@delete
            }
            val userId = call.parameters["userId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("userId is required"))
                return@delete
            }
            try {
                val deleted = folderRepository.deleteFolder(folderId, userId)
                if (deleted) call.respond(HttpStatusCode.OK)
                else call.respond(HttpStatusCode.NotFound)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        route("/posts") {
            // ✅ GET: Получить ленту постов
            get {
                try {
                    val posts = transaction {
                        PostsTable.selectAll()
                            .orderBy(PostsTable.createdAt to SortOrder.DESC)
                            .map { row ->
                                val postId = row[PostsTable.id]

                                // ✅ Считаем лайки для этого поста
                                val likesCount = PostLikesTable.select {
                                    PostLikesTable.postId eq postId
                                }.count().toInt()

                                Post(
                                    id = row[PostsTable.id],
                                    userId = row[PostsTable.userId],
                                    content = row[PostsTable.content],
                                    taskId = row[PostsTable.taskId],
                                    createdAt = row[PostsTable.createdAt],
                                    likesCount = likesCount  // ✅ Передаём посчитанное значение!
                                )
                            }
                    }
                    call.respond(posts)
                } catch (e: Exception) {
                    println("❌ Error fetching posts: ${e.message}")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Ошибка: ${e.message}")
                }
            }

            // ✅ POST: Создать новый пост
            post {
                try {
                    // ✅ Принимаем CreatePostRequest (то, что шлёт клиент)
                    val request = call.receive<CreatePostRequest>()

                    // ✅ Генерируем недостающие поля на сервере
                    val newPost = Post(
                        id = java.util.UUID.randomUUID().toString(),  // ✅ Генерируем id
                        userId = request.userId,
                        content = request.content,
                        taskId = request.taskId,
                        createdAt = java.time.LocalDateTime.now().toString()  // ✅ Генерируем время
                    )

                    transaction {
                        PostsTable.insert {
                            it[id] = newPost.id
                            it[userId] = newPost.userId
                            it[content] = newPost.content
                            it[taskId] = newPost.taskId
                            it[createdAt] = newPost.createdAt
                        }
                    }

                    // ✅ Возвращаем созданный пост
                    call.respond(HttpStatusCode.Created, newPost)
                } catch (e: Exception) {
                    println("❌ Ошибка создания поста: ${e.message}")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest, "Ошибка: ${e.message}")
                }
            }

            // ✅ Эндпоинт: Поставить/Убрать лайк
            post("/{id}/like") {
                try {
                    val postId = call.parameters["id"] ?: return@post
                    val request = call.receive<LikeRequest>()

                    transaction {
                        val existingLike = PostLikesTable.select {
                            (PostLikesTable.postId eq postId) and (PostLikesTable.userId eq request.userId)
                        }.singleOrNull()

                        if (existingLike != null) {
                            // Если лайк уже есть -> Удаляем (дизлайк)
                            PostLikesTable.deleteWhere {
                                (PostLikesTable.postId eq postId) and (PostLikesTable.userId eq request.userId)
                            }
                        } else {
                            // Если лайка нет -> Добавляем
                            PostLikesTable.insert {
                                it[PostLikesTable.postId] = postId
                                it[PostLikesTable.userId] = request.userId
                                it[PostLikesTable.createdAt] = java.time.LocalDateTime.now().toString()
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    println("❌ Error liking post: ${e.message}")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, "Error liking post")
                }
            }
        }
    }

}