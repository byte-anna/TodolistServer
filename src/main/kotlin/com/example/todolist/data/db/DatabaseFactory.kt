package com.example.todolist.data.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    private lateinit var dataSource: HikariDataSource

    fun init() {
        connect(
            jdbcUrl = requiredEnv("DATABASE_URL").toJdbcPostgresUrl(),
            driverClassName = "org.postgresql.Driver",
            username = requiredEnv("DATABASE_USER"),
            password = requiredEnv("DATABASE_PASSWORD")
        )
    }

    fun initForTests() {
        connect(
            jdbcUrl = "jdbc:h2:mem:todolist_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driverClassName = "org.h2.Driver",
            username = "sa",
            password = ""
        )
    }

    private fun connect(
        jdbcUrl: String,
        driverClassName: String,
        username: String,
        password: String
    ) {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }

        dataSource = HikariDataSource(
            HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                this.driverClassName = driverClassName
                this.username = username
                this.password = password
                maximumPoolSize = 10
            }
        )

        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(
                UsersTable,
                TasksTable,
                PostsTable,
                PostLikesTable,
            )
        }
    }

    fun <T> dbQuery(block: () -> T): T = transaction { block() }
    private fun requiredEnv(name: String): String {
        return System.getenv(name)
            ?: error("Environment variable $name is required for PostgreSQL connection")
    }

    private fun String.toJdbcPostgresUrl(): String {
        return when {
            startsWith("jdbc:postgresql://") -> this
            startsWith("postgresql://") -> replaceFirst("postgresql://", "jdbc:postgresql://")
            startsWith("postgres://") -> replaceFirst("postgres://", "jdbc:postgresql://")
            else -> this
        }
    }
}