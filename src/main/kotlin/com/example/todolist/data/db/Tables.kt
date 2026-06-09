package com.example.todolist.data.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.*
import org.jetbrains.exposed.sql.ReferenceOption
import java.time.LocalDateTime

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val displayName = varchar("display_name", 100).nullable()
    val passwordHash = varchar("password_hash", 255)
    val salt = varchar("salt", 32)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id, name = "PK_users_id")
}

object TasksTable : Table("tasks") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 255)
    val isDone = bool("is_done").default(false)
    val priority = integer("priority").default(1)
    val createdAt = datetime("created_at")
    val dueDate = datetime("due_date").nullable()

    override val primaryKey = PrimaryKey(id, name = "PK_tasks_id")

    init {
        index("IDX_tasks_user_id_created_at", false, userId, createdAt)
    }
}

object PostsTable : Table("posts") {
    val id = varchar("id", 36).clientDefault { UUID.randomUUID().toString() }
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val content = text("content")
    val taskId = varchar("task_id", 36).references(TasksTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id, name = "PK_posts_id")
    init {
        index("IDX_posts_created_at", false, createdAt)
        index("IDX_posts_user_id", false, userId)
    }
}

object PostLikesTable : Table("post_likes") {
    val id = varchar("id", 36).clientDefault { UUID.randomUUID().toString() }
    val postId = varchar("post_id", 36).references(PostsTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = varchar("user_id", 36).references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id, name = "PK_post_likes_id")

    init {
        index("IDX_post_likes_post_id", false, postId)
        uniqueIndex("UQ_post_likes_post_id_user_id", postId, userId)
    }
}
