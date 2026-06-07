package com.example.todolist.domain.usecase.post

import com.example.todolist.domain.repository.PostRepository

class TogglePostLikeUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String, userId: String) {
        postRepository.toggleLike(postId, userId)
    }
}