plugins {
    application
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

application {
    mainClass.set("com.example.todolist.ApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

// ✅ ВАЖНО: Блок dependencies должен быть открыт!
dependencies {
    // === Ktor Server ===
    implementation("io.ktor:ktor-server-core-jvm:2.3.9")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.9")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.9")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.9")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.9")

    // === Ktor Client ===
    implementation("io.ktor:ktor-client-core-jvm:2.3.9")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.9")

    // === Serialization ===
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // === База данных ===
    implementation("com.h2database:h2:2.2.224")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // === Exposed ORM ===
    implementation("org.jetbrains.exposed:exposed-core:0.48.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.48.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.48.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.48.0")

    // === Logging ===
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // === Tests ===
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.8.0")
}  // ✅ Не забудь закрыть блок!