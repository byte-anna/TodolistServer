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
    val folderId = varchar("folder_id", 36).nullable()

    // ✅ Альтернативный способ задать первичный ключ:
    override val primaryKey = PrimaryKey(id, name = "PK_tasks_id")
}

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val displayName = varchar("display_name", 100).nullable()  // ✅ Добавили
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id, name = "PK_users_id")
}

object FoldersTable : Table("folders") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36)
    val name = varchar("name", 100)
    val color = varchar("color", 7).default("#6200EE") // HEX цвет для иконки
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id, name = "PK_folders_id")
}

object PostsTable : Table("posts") {
    val id = varchar("id", 36).clientDefault { UUID.randomUUID().toString() }
    val userId = varchar("user_id", 36)
    val content = text("content")
    val taskId = varchar("task_id", 36).nullable()
    val createdAt = varchar("created_at", 50)  // ✅ ИЗМЕНИЛ: varchar вместо datetime

    override val primaryKey = PrimaryKey(id, name = "PK_posts_id")
}