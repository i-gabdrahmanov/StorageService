# Этап сборки (используем корректный образ Gradle)
FROM gradle:8.4-jdk21-jammy AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Этап запуска (используем корректный образ Temurin JRE)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]