# Multi-stage Dockerfile для SpringProxy сервиса

# Этап 1: Кэширование зависимостей  
FROM gradle:8.4-jdk21-jammy as cache
WORKDIR /app

# Создаем временный build.gradle для кэширования зависимостей
COPY build.gradle ./
RUN echo "rootProject.name = 'springproxy'" > settings.gradle
RUN gradle dependencies --no-daemon > /dev/null

# Этап 2: Сборка приложения
FROM gradle:8.4-jdk21-jammy as builder
WORKDIR /app

# Копируем кэш зависимостей из первого этапа
COPY --from=cache /home/gradle/.gradle /home/gradle/.gradle

# Копируем файлы проекта
COPY build.gradle ./
COPY src ./src

# Создаем settings.gradle
RUN echo "rootProject.name = 'springproxy'" > settings.gradle

# Собираем приложение
RUN gradle bootJar \
    --no-daemon \
    --build-cache \
    -x test \
    -x check

# Этап 3: Финальный образ
FROM eclipse-temurin:21-jre

# Создаем пользователя для безопасности
RUN groupadd -r springproxy && useradd -r -g springproxy springproxy

# Создаем директорию приложения
WORKDIR /app
RUN chown springproxy:springproxy /app

# Копируем JAR файл
COPY --from=builder --chown=springproxy:springproxy /app/build/libs/springproxy.jar app.jar

# Переключаемся на пользователя приложения
USER springproxy

# Настройки JVM для контейнера
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Экспортируем порт
EXPOSE 8083

# Добавляем health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8083/actuator/health || exit 1

# Запускаем приложение
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]