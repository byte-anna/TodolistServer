package com.example.todolist.data.repository

import com.example.todolist.data.db.DatabaseFactory
import com.example.todolist.data.db.FoldersTable
import com.example.todolist.data.db.TasksTable
import com.example.todolist.domain.model.Folder
import com.example.todolist.domain.repository.FolderRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.UUID

class FolderRepositoryImpl : FolderRepository {

    override suspend fun getFolders(userId: String): List<Folder> {
        return DatabaseFactory.dbQuery {
            FoldersTable.select { FoldersTable.userId eq userId }
                .orderBy(FoldersTable.createdAt to SortOrder.ASC)
                .map { row ->
                    Folder(
                        id = row[FoldersTable.id],
                        userId = row[FoldersTable.userId],
                        name = row[FoldersTable.name],
                        color = row[FoldersTable.color],
                        createdAt = row[FoldersTable.createdAt].toString()
                    )
                }
        }
    }

    override suspend fun createFolder(userId: String, name: String, color: String): Folder {
        val folderId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        DatabaseFactory.dbQuery {
            FoldersTable.insert {
                it[id] = folderId
                it[this.userId] = userId
                it[this.name] = name
                it[this.color] = color
                it[createdAt] = now
            }
        }

        return Folder(folderId, userId, name, color, now.toString())
    }

    override suspend fun deleteFolder(folderId: String, userId: String): Boolean {
        return DatabaseFactory.dbQuery {
            // Сначала удаляем задачи в этой папке (или можно перенести в "без папки")
            TasksTable.deleteWhere {
                (TasksTable.folderId eq folderId) and (TasksTable.userId eq userId)
            }
            // Потом удаляем папку
            val deleted = FoldersTable.deleteWhere {
                (FoldersTable.id eq folderId) and (FoldersTable.userId eq userId)
            }
            deleted > 0
        }
    }
}