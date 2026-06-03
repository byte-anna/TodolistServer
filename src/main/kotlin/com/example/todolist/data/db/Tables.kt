package com.example.todolist.data.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*

object TasksTable : Table("tasks") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val title = varchar("title", 255)
    val isDone = bool("is_done").default(false)
    val priority = integer("priority").default(1)
    val createdAt = datetime("created_at")
    val dueDate = datetime("due_date").nullable()
    override val primaryKey = PrimaryKey(id, name = "PK_tasks_id")
}

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val displayName = varchar("display_name", 100).nullable()
    val passwordHash = varchar("password_hash", 64)
    val salt = varchar("salt", 32)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id, name = "PK_users_id")
}

object PostsTable : Table("posts") {
    val id = varchar("id", 36).clientDefault { UUID.randomUUID().toString() }
    val userId = varchar("user_id", 36)
    val content = text("content")
    val taskId = varchar("task_id", 36).nullable()
    val createdAt = varchar("created_at", 50)

    override val primaryKey = PrimaryKey(id, name = "PK_posts_id")
}

object PostLikesTable : Table("post_likes") {
    val id = varchar("id", 36).clientDefault { UUID.randomUUID().toString() }
    val postId = varchar("post_id", 36)
    val userId = varchar("user_id", 36)
    val createdAt = varchar("created_at", 50)

    override val primaryKey = PrimaryKey(id, name = "PK_post_likes_id")
}