package com.example.todolist.plugins

import com.example.todolist.data.db.PostLikesTable
import com.example.todolist.data.db.PostsTable
import com.example.todolist.domain.model.Post
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

fun Route.postRoutes() {

    get("/posts") {
        call.requireAuthenticatedUserId() ?: return@get

        try {
            val posts = transaction {
                PostsTable.selectAll()
                    .orderBy(PostsTable.createdAt to SortOrder.DESC)
                    .map { row ->
                        val postId = row[PostsTable.id]

                        val likesCount = PostLikesTable.select {
                            PostLikesTable.postId eq postId
                        }.count().toInt()

                        Post(
                            id = postId,
                            userId = row[PostsTable.userId],
                            content = row[PostsTable.content],
                            taskId = row[PostsTable.taskId],
                            createdAt = row[PostsTable.createdAt],
                            likesCount = likesCount
                        )
                    }
            }
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

            val newPost = Post(
                id = UUID.randomUUID().toString(),
                userId = authenticatedUserId,
                content = request.content,
                taskId = request.taskId,
                createdAt = LocalDateTime.now().toString()
            )

            transaction {
                PostsTable.insert {
                    it[PostsTable.id] = newPost.id
                    it[PostsTable.userId] = newPost.userId
                    it[PostsTable.content] = newPost.content
                    it[PostsTable.taskId] = newPost.taskId
                    it[PostsTable.createdAt] = newPost.createdAt
                }
            }

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

            transaction {
                val existingLike = PostLikesTable.select {
                    (PostLikesTable.postId eq postId) and (PostLikesTable.userId eq authenticatedUserId)
                }.singleOrNull()

                if (existingLike != null) {
                    // Дизлайк
                    PostLikesTable.deleteWhere {
                        (PostLikesTable.postId eq postId) and (PostLikesTable.userId eq authenticatedUserId)
                    }
                } else {
                    // Лайк
                    PostLikesTable.insert {
                        it[PostLikesTable.postId] = postId
                        it[PostLikesTable.userId] = authenticatedUserId
                        it[PostLikesTable.createdAt] = LocalDateTime.now().toString()
                    }
                }
            }
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