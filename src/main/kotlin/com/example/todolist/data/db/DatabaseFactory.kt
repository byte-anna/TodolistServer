package com.example.todolist.data.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    private lateinit var dataSource: HikariDataSource

    fun init() {
        dataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:h2:file:./todolist_db;DB_CLOSE_DELAY=-1"
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                maximumPoolSize = 10
            }
        )

        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(
                UsersTable,
                TasksTable,
                FoldersTable,
                PostLikesTable,
                PostsTable  // ✅ ВОТ ЭТО ДОБАВЬ!
            )
        }

        println("✅ Database initialized!")
    }

    fun <T> dbQuery(block: () -> T): T = transaction { block() }
}