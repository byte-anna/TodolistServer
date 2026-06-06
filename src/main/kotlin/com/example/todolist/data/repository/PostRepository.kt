package com.example.todolist.domain.repository

import com.example.todolist.domain.model.Post

interface PostRepository {
    suspend fun getPosts(): List<Post>
    suspend fun createPost(userId: String, content: String, taskId: String?): Post
    suspend fun toggleLike(postId: String, userId: String)
}