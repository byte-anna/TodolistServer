package com.example.todolist.data.repository

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.data.db.TasksTable
import com.example.todolist.domain.model.Task
import com.example.todolist.domain.model.TaskCategory
import com.example.todolist.domain.repository.TaskRepository
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

class TaskRepositoryImpl : TaskRepository {

    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

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
                        category = row[TasksTable.category],
                        dueDate = row[TasksTable.dueDate]?.format(dateTimeFormatter),
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
                    priority = row[TasksTable.priority],
                    category = row[TasksTable.category],
                    dueDate = row[TasksTable.dueDate]?.format(dateTimeFormatter),
                    createdAt = row[TasksTable.createdAt].toString()
                )
            }
    }

    override suspend fun createTask(
        userId: String,
        title: String,
        priority: Int,
        category: TaskCategory,
        dueDate: String?
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
                it[this.category] = category
                it[this.dueDate] = dueDate?.let(::parseDueDate)
                it[createdAt] = now
            }
        }

        return Task(
            id = taskId,
            userId = userId,
            title = title,
            isDone = false,
            priority = priority,
            category = category,
            dueDate = dueDate?.let(::parseDueDate)?.format(dateTimeFormatter),
            createdAt = now.toString()
        )
    }

    override suspend fun updateTask(
        taskId: String,
        userId: String,
        title: String?,
        isDone: Boolean?,
        priority: Int?,
        category: TaskCategory?,
        dueDate: String?
    ): Boolean {
        return DatabaseFactory.dbQuery {
            val updatedRows = TasksTable.update(
                where = { (TasksTable.id eq taskId) and (TasksTable.userId eq userId) }
            ) { statement ->
                title?.let { statement[TasksTable.title] = it }
                isDone?.let { statement[TasksTable.isDone] = it }
                priority?.let { statement[TasksTable.priority] = it }
                category?.let { statement[TasksTable.category] = it }
                dueDate?.let { statement[TasksTable.dueDate] = parseDueDate(it) }
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

    private fun parseDueDate(value: String): LocalDateTime {
        return try {
            LocalDateTime.parse(value, dateTimeFormatter)
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException(
                "Invalid dueDate format. Expected ISO_LOCAL_DATE_TIME like 2026-06-09T14:30:00"
            )
        }
    }
}
