# Этап 1: Кэширование зависимостей
FROM gradle:8.4-jdk21-jammy as cache
WORKDIR /app
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon > /dev/null

# Этап 2: Сборка приложения
FROM gradle:8.4-jdk21-jammy as builder
WORKDIR /app

# Копируем кэш зависимостей из первого этапа
COPY --from=cache /home/gradle/.gradle /home/gradle/.gradle

# Копируем исходный код (это делается после копирования кэша)
COPY src ./src
COPY build.gradle settings.gradle ./

# Собираем приложение (будет выполняться при изменении исходного кода)
RUN gradle shadowJar \
    --no-daemon \
    --parallel \
    --build-cache \
    -x test \
    -x check

# Этап 3: Финальный образ
FROM eclipse-temurin:21-jre
COPY --from=builder /app/build/libs/proxy-service-0.1-all.jar /app/app.jar
CMD ["java", "-jar", "/app/app.jar"] 