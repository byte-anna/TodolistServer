package com.example.todolist.domain.usecase.task

import com.example.todolist.domain.model.Task
import com.example.todolist.domain.repository.TaskRepository
import java.time.LocalDate

class GetTasksUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(userId: String, dueDate: LocalDate? = null): List<Task> {
        return taskRepository.getTasks(userId, dueDate)
    }
}
