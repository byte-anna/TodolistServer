package com.example.todolist.data.repository

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.data.db.PostLikesTable
import com.example.todolist.data.db.PostsTable
import com.example.todolist.domain.model.Post
import com.example.todolist.domain.repository.PostRepository
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.UUID

class PostRepositoryImpl : PostRepository {

    override suspend fun getPosts(): List<Post> {
        return DatabaseFactory.dbQuery {
            val posts = PostsTable.selectAll()
                .orderBy(PostsTable.createdAt to SortOrder.DESC)
                .map { row ->
                    Post(
                        id = row[PostsTable.id],
                        userId = row[PostsTable.userId],
                        content = row[PostsTable.content],
                        taskId = row[PostsTable.taskId],
                        createdAt = row[PostsTable.createdAt].toString()
                    )
                }

            if (posts.isEmpty()) {
                posts
            } else {
                val postIds = posts.map { it.id }.toSet()
                val likesCountByPostId = PostLikesTable.selectAll()
                    .map { row -> row[PostLikesTable.postId] }
                    .filter { postId -> postId in postIds }
                    .groupingBy { postId -> postId }
                    .eachCount()

                posts.map { post ->
                    post.copy(likesCount = likesCountByPostId[post.id] ?: 0)
                }
            }
        }
    }

    override suspend fun createPost(userId: String, content: String, taskId: String?): Post {
        val now = LocalDateTime.now()
        val newPost = Post(
            id = UUID.randomUUID().toString(),
            userId = userId,
            content = content,
            taskId = taskId,
            createdAt = now.toString()
        )

        DatabaseFactory.dbQuery {
            PostsTable.insert {
                it[PostsTable.id] = newPost.id
                it[PostsTable.userId] = newPost.userId
                it[PostsTable.content] = newPost.content
                it[PostsTable.taskId] = newPost.taskId
            }
        }

        return newPost
    }

    override suspend fun toggleLike(postId: String, userId: String) {
        DatabaseFactory.dbQuery {
            val existingLike = PostLikesTable.select {
                (PostLikesTable.postId eq postId) and (PostLikesTable.userId eq userId)
            }.singleOrNull()

            if (existingLike != null) {
                PostLikesTable.deleteWhere {
                    (PostLikesTable.postId eq postId) and (PostLikesTable.userId eq userId)
                }
            } else {
                PostLikesTable.insert {
                    it[PostLikesTable.postId] = postId
                    it[PostLikesTable.userId] = userId
                }
            }
        }
    }

    private fun countLikes(postId: String): Int {
        return PostLikesTable.select { PostLikesTable.postId eq postId }
            .count()
            .toInt()
    }
}