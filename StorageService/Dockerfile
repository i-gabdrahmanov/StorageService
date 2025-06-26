# Этап 1: Кэширование зависимостей Gradle
FROM gradle:8.4-jdk21-jammy as cache
WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon > /dev/null

# Этап 2: Сборка приложения
FROM gradle:8.4-jdk21-jammy as builder
WORKDIR /app
COPY --from=cache /home/gradle/.gradle /home/gradle/.gradle
COPY . .
RUN gradle bootJar --no-daemon --parallel -x test

# Этап 3: Финальный образ
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Копируем собранный JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# Добавляем healthcheck для Docker
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Оптимизированные JVM параметры для работы с Ignite
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/heapdumps"

# Создаем директорию для дампов памяти
RUN mkdir /heapdumps && chmod 777 /heapdumps

# Упрощенный entrypoint для Spring Boot
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]