diff --git a/Dockerfile b/Dockerfile
index 264b0114219655fea28933796cc0aa9ff6762483..75c73c9f5d5448c21e5690773d5ac206826ec09d 100644
--- a/Dockerfile
+++ b/Dockerfile
@@ -1,10 +1,10 @@
 FROM gradle:7.6-jdk17 AS builder
 WORKDIR /home/gradle/project
 COPY --chown=gradle:gradle . /home/gradle/project
RUN gradle installDist --no-daemon

FROM eclipse-temurin:17-jre
 WORKDIR /app
COPY --from=builder /home/gradle/project/build/install/TodoListServer /app
 EXPOSE 8080
CMD ["/app/bin/TodoListServer"]
