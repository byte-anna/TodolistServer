package com.example.todolist.data.repository

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.data.db.TasksTable
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.repository.TaskRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.UUID

class TaskRepositoryImpl : TaskRepository {

    override suspend fun getTasks(userId: String): List<Task> {
        return DatabaseFactory.dbQuery {
            TasksTable.select { TasksTable.userId eq userId }
                .orderBy(TasksTable.createdAt to SortOrder.DESC)
                .map { row ->
                    Task(
                        id = row[TasksTable.id],
                        userId = row[TasksTable.userId],
                        title = row[TasksTable.title],
                        isDone = row[TasksTable.isDone],
                        priority = row[TasksTable.priority],
                        dueDate = row[TasksTable.dueDate]?.toString(),
                        folderId = row[TasksTable.folderId],  // ✅
                        createdAt = row[TasksTable.createdAt].toString()
                    )
                }
        }
    }

    override suspend fun getTaskById(taskId: String, userId: String): Task? = DatabaseFactory.dbQuery {
        TasksTable.select { (TasksTable.id eq taskId) and (TasksTable.userId eq userId) }
            .singleOrNull()
            ?.let { row ->
                Task(
                    id = row[TasksTable.id],
                    userId = row[TasksTable.userId],
                    title = row[TasksTable.title],
                    isDone = row[TasksTable.isDone],
                    dueDate = row[TasksTable.dueDate]?.toString(),
                    priority = row[TasksTable.priority],
                    folderId = row[TasksTable.folderId],  // ✅
                    createdAt = row[TasksTable.createdAt].toString()
                )
            }
    }

    override suspend fun createTask(
        userId: String,
        title: String,
        priority: Int,
        dueDate: String?,
        folderId: String?  // ✅
    ): Task {
        val taskId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        DatabaseFactory.dbQuery {
            TasksTable.insert {
                it[id] = taskId
                it[this.userId] = userId
                it[this.title] = title
                it[this.priority] = priority
                it[this.isDone] = false
                it[this.dueDate] = dueDate?.let { LocalDateTime.parse(it) }
                it[this.folderId] = folderId  // ✅
                it[createdAt] = now
            }
        }

        return Task(taskId, userId, title, false, priority, dueDate, folderId, now.toString())
    }

    override suspend fun updateTask(
        taskId: String,
        userId: String,
        title: String?,
        isDone: Boolean?,
        priority: Int?,
        dueDate: String?,
        folderId: String?  // ✅
    ): Boolean {
        return DatabaseFactory.dbQuery {
            val updatedRows = TasksTable.update(
                where = { (TasksTable.id eq taskId) and (TasksTable.userId eq userId) }
            ) { statement ->
                title?.let { statement[TasksTable.title] = it }
                isDone?.let { statement[TasksTable.isDone] = it }
                priority?.let { statement[TasksTable.priority] = it }
                dueDate?.let { statement[TasksTable.dueDate] = LocalDateTime.parse(it) }
                folderId?.let { statement[TasksTable.folderId] = it }  // ✅
            }
            updatedRows > 0
        }
    }

    override suspend fun deleteTask(taskId: String, userId: String): Boolean {
        return DatabaseFactory.dbQuery {
            val deletedRows = TasksTable.deleteWhere {
                (TasksTable.id eq taskId) and (TasksTable.userId eq userId)
            }
            deletedRows > 0
        }
    }
}