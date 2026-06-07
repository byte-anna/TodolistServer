package com.example.todolist.domain.usecase.task

import com.example.todolist.domain.repository.TaskRepository

class UpdateTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        taskId: String,
        userId: String,
        title: String? = null,
        isDone: Boolean? = null,
        priority: Int? = null,
        dueDate: String? = null
    ): Boolean {
        return taskRepository.updateTask(taskId, userId, title, isDone, priority, dueDate)
    }
}