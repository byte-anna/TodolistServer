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
        try {
            val request = call.receive<CreatePostRequest>()

            val newPost = Post(
                id = UUID.randomUUID().toString(),
                userId = request.userId,
                content = request.content,
                taskId = request.taskId,
                createdAt = LocalDateTime.now().toString()
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
        try {
            val postId = call.parameters["id"] ?: return@post
            val request = call.receive<LikeRequest>()

            transaction {
                val existingLike = PostLikesTable.select {
                    (PostLikesTable.postId eq postId) and (PostLikesTable.userId eq request.userId)
                }.singleOrNull()

                if (existingLike != null) {
                    // Дизлайк
                    PostLikesTable.deleteWhere {
                        (PostLikesTable.postId eq postId) and (PostLikesTable.userId eq request.userId)
                    }
                } else {
                    // Лайк
                    PostLikesTable.insert {
                        it[PostLikesTable.postId] = postId
                        it[PostLikesTable.userId] = request.userId
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