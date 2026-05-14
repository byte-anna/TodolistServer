package com.example.todolist.domain.repository

import com.example.todolist.domain.model.Folder

interface FolderRepository {
    suspend fun getFolders(userId: String): List<Folder>
    suspend fun createFolder(userId: String, name: String, color: String): Folder
    suspend fun deleteFolder(folderId: String, userId: String): Boolean
}