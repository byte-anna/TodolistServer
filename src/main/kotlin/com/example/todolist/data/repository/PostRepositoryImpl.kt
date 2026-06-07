package com.example.todolist.data.repository

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.data.db.PostLikesTable
import com.example.todolist.data.db.PostsTable
import com.example.todolist.data.db.TasksTable
import com.example.todolist.domain.model.Post
import com.example.todolist.domain.repository.PostRepository
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

class PostRepositoryImpl : PostRepository {

    override suspend fun getPosts(): List<Post> {
        return DatabaseFactory.dbQuery {
            val posts = TransactionManager.current().exec(
                """
                SELECT p.id, p.user_id, p.content, p.task_id,
                       COALESCE(
                           t.title,
                           (
                               SELECT t2.title
                               FROM tasks t2
                               WHERE t2.user_id = p.user_id AND t2.is_done = TRUE
                               ORDER BY t2.created_at DESC
                               LIMIT 1
                           )
                       ) AS task_title,
                       CAST(p.created_at AS VARCHAR) AS created_at
                FROM posts p
                LEFT JOIN tasks t ON t.id = p.task_id
                ORDER BY p.created_at DESC
                """.trimIndent()
            ) { resultSet ->
                resultSet.toPosts()
            } ?: emptyList()

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

        val task = taskId
            ?.let { findTask(it, userId) }
            ?: findLatestCompletedTask(userId)
        val newPost = Post(
            id = UUID.randomUUID().toString(),
            userId = userId,
            content = content.withTaskTitle(task?.title),
            taskId = task?.id,
            createdAt = now.toClientIsoString()
        )

        DatabaseFactory.dbQuery {
            PostsTable.insert {
                it[PostsTable.id] = newPost.id
                it[PostsTable.userId] = newPost.userId
                it[PostsTable.content] = newPost.content
                it[PostsTable.taskId] = newPost.taskId
                it[PostsTable.createdAt] = now
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
                    it[PostLikesTable.createdAt] = LocalDateTime.now()
                }
            }
        }
    }

    private fun countLikes(postId: String): Int {
        return PostLikesTable.select { PostLikesTable.postId eq postId }
            .count()
            .toInt()
    }


    private data class TaskInfo(
        val id: String,
        val title: String
    )

    private fun findTask(taskId: String, userId: String): TaskInfo? {
        return DatabaseFactory.dbQuery {
            TasksTable.select { (TasksTable.id eq taskId) and (TasksTable.userId eq userId) }
                .singleOrNull()
                ?.let { row ->
                    TaskInfo(
                        id = row[TasksTable.id],
                        title = row[TasksTable.title]
                    )
                }
        }
    }

    private fun findLatestCompletedTask(userId: String): TaskInfo? {
        return DatabaseFactory.dbQuery {
            TasksTable.select { (TasksTable.userId eq userId) and (TasksTable.isDone eq true) }
                .orderBy(TasksTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                .limit(1)
                .singleOrNull()
                ?.let { row ->
                    TaskInfo(
                        id = row[TasksTable.id],
                        title = row[TasksTable.title]
                    )
                }
        }
    }

    private fun String.withTaskTitle(taskTitle: String?): String {
        val title = taskTitle?.takeIf { it.isNotBlank() } ?: return this
        if (contains(title)) return this

        return if (trim().equals("Выполнил задачу!", ignoreCase = true)) {
            "Выполнил задачу: $title"
        } else {
            "$this\nЗадача: $title"
        }
    }

    private fun ResultSet.toPosts(): List<Post> {
        val posts = mutableListOf<Post>()
        while (next()) {
            posts += Post(
                id = getString("id"),
                userId = getString("user_id"),
                content = getString("content").withTaskTitle(getString("task_title")),
                taskId = getString("task_id"),
                createdAt = getString("created_at").toClientIsoString()
            )
        }
        return posts
    }

    private fun String.toClientIsoString(): String {
        val normalized = trim().replace(' ', 'T')

        val dateTime = runCatching {
            LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }.getOrElse {
            runCatching {
                OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime()
            }.getOrElse {
                LocalDateTime.parse(normalized.take(19), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }
        }

        return dateTime.toClientIsoString()
    }

    private fun LocalDateTime.toClientIsoString(): String {
        return truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}