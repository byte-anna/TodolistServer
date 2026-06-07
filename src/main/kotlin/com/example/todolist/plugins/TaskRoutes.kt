package com.example.todolist.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.todolist.domain.usecase.task.CreateTaskUseCase
import com.example.todolist.domain.usecase.task.DeleteTaskUseCase
import com.example.todolist.domain.usecase.task.GetTasksUseCase
import com.example.todolist.domain.usecase.task.UpdateTaskUseCase

fun Route.taskRoutes(
    getTasksUseCase: GetTasksUseCase,
    createTaskUseCase: CreateTaskUseCase,
    updateTaskUseCase: UpdateTaskUseCase,
    deleteTaskUseCase: DeleteTaskUseCase
) {

    get("/tasks") {
        val userId = call.requireAuthenticatedUserId() ?: return@get

        try {
            val tasks = getTasksUseCase(userId)
            call.respond(HttpStatusCode.OK, tasks)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(e.message ?: "Unknown error")
            )
        }
    }

    post("/tasks") {
        val userId = call.requireAuthenticatedUserId() ?: return@post

        try {
            val request = call.receive<CreateTaskRequest>()
            if (request.title.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Title cannot be empty")
                )
                return@post
            }

            val newTask = createTaskUseCase(
                userId,
                request.title.trim(),
                request.priority,
                request.dueDate
            )
            call.respond(HttpStatusCode.Created, newTask)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(e.message ?: "Failed to create task")
            )
        }
    }

    delete("/tasks/{id}") {
        val taskId = call.parameters["id"] ?: run {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Task ID is required"))
            return@delete
        }
        val userId = call.requireAuthenticatedUserId() ?: return@delete

        try {
            val deleted = deleteTaskUseCase(taskId, userId)
            if (deleted) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Task deleted"))
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Task not found"))
            }
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(e.message ?: "Failed to delete task")
            )
        }
    }

    put("/tasks/{id}") {
        val taskId = call.parameters["id"] ?: run {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Task ID is required"))
            return@put
        }
        val userId = call.requireAuthenticatedUserId() ?: return@put

        try {
            val request = call.receive<UpdateTaskRequest>()
            val updated = updateTaskUseCase(
                taskId,
                userId,
                request.title,
                request.isDone,
                request.priority,
                request.dueDate
            )
            if (updated) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.NotFound)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}