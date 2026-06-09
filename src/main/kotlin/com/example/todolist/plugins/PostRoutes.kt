package com.example.todolist.plugins

import com.example.todolist.domain.usecase.post.CreatePostUseCase
import com.example.todolist.domain.usecase.post.GetPostsUseCase
import com.example.todolist.domain.usecase.post.TogglePostLikeUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.postRoutes(
    getPostsUseCase: GetPostsUseCase,
    createPostUseCase: CreatePostUseCase,
    togglePostLikeUseCase: TogglePostLikeUseCase
) {

    get("/posts") {
        call.requireAuthenticatedUserId() ?: return@get
        val posts = getPostsUseCase()
        call.respond(posts)
    }

    post("/posts") {
        val authenticatedUserId = call.requireAuthenticatedUserId() ?: return@post
        val request = call.receive<CreatePostRequest>()

        val newPost = createPostUseCase(
            userId = authenticatedUserId,
            content = request.content,
            taskId = request.taskId
        )

        call.respond(HttpStatusCode.Created, newPost)
    }

    post("/posts/{id}/like") {
        val authenticatedUserId = call.requireAuthenticatedUserId() ?: return@post
        val postId = call.parameters["id"] ?: run {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Post ID is required"))
            return@post
        }

        togglePostLikeUseCase(postId, authenticatedUserId)
        call.respond(HttpStatusCode.OK)
    }
}
