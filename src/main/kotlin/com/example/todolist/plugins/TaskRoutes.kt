package com.example.todolist.plugins

import com.example.todolist.domain.usecase.task.CreateTaskUseCase
import com.example.todolist.domain.usecase.task.DeleteTaskUseCase
import com.example.todolist.domain.usecase.task.GetTasksUseCase
import com.example.todolist.domain.usecase.task.UpdateTaskUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

fun Route.taskRoutes(
    getTasksUseCase: GetTasksUseCase,
    createTaskUseCase: CreateTaskUseCase,
    updateTaskUseCase: UpdateTaskUseCase,
    deleteTaskUseCase: DeleteTaskUseCase
) {

    get("/tasks") {
        val userId = call.requireAuthenticatedUserId() ?: return@get
        val tasks = getTasksUseCase(userId)
        call.respond(HttpStatusCode.OK, tasks)
    }

    post("/tasks") {
        val userId = call.requireAuthenticatedUserId() ?: return@post
        val request = call.receive<CreateTaskRequest>()

        if (request.title.isBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Title cannot be empty")
            )
            return@post
        }

        if (request.priority < 1) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Priority must be greater than 0")
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
    }

    delete("/tasks/{id}") {
        val taskId = call.parameters["id"] ?: run {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Task ID is required"))
            return@delete
        }
        val userId = call.requireAuthenticatedUserId() ?: return@delete

        val deleted = deleteTaskUseCase(taskId, userId)
        if (deleted) {
            call.respond(HttpStatusCode.OK, mapOf("message" to "Task deleted"))
        } else {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("Task not found"))
        }
    }

    put("/tasks/{id}") {
        val taskId = call.parameters["id"] ?: run {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Task ID is required"))
            return@put
        }
        val userId = call.requireAuthenticatedUserId() ?: return@put
        val request = call.receive<UpdateTaskRequest>()

        if (request.priority != null && request.priority < 1) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Priority must be greater than 0")
            )
            return@put
        }

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
    }
}
