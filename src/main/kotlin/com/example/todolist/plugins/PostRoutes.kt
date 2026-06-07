package com.example.todolist.plugins

import com.example.todolist.data.db.PostLikesTable
import com.example.todolist.data.db.PostsTable
import com.example.todolist.domain.model.Post
import com.example.todolist.domain.repository.PostRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import com.example.todolist.domain.usecase.post.CreatePostUseCase
import com.example.todolist.domain.usecase.post.GetPostsUseCase
import com.example.todolist.domain.usecase.post.TogglePostLikeUseCase


fun Route.postRoutes(
    getPostsUseCase: GetPostsUseCase,
    createPostUseCase: CreatePostUseCase,
    togglePostLikeUseCase: TogglePostLikeUseCase
) {

    get("/posts") {
        call.requireAuthenticatedUserId() ?: return@get

        try {
            val posts = getPostsUseCase()
            call.respond(posts)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Ошибка: ${e.message}")
            )
        }
    }

    post("/posts") {
        val authenticatedUserId = call.requireAuthenticatedUserId() ?: return@post

        try {
            val request = call.receive<CreatePostRequest>()
            val now = LocalDateTime.now()

            val newPost = createPostUseCase(
                userId = authenticatedUserId,
                content = request.content,
                taskId = request.taskId
            )

            call.respond(HttpStatusCode.Created, newPost)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Ошибка: ${e.message}")
            )
        }
    }

    post("/posts/{id}/like") {
        val authenticatedUserId = call.requireAuthenticatedUserId() ?: return@post

        try {
            val postId = call.parameters["id"] ?: return@post

            togglePostLikeUseCase(postId, authenticatedUserId)
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Error liking post: ${e.message}")
            )
        }
    }
}