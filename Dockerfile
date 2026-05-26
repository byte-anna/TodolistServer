FROM gradle:7.6-jdk17 AS builder
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . /home/gradle/project
RUN gradle build --no-daemon

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /home/gradle/project/build/libs/*.jar /app/server.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/server.jar"]