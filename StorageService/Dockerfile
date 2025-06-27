# Этап 1: Кэширование зависимостей
FROM gradle:8.4-jdk21-jammy as cache
WORKDIR /app
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon > /dev/null

# Этап 2: Сборка приложения
FROM gradle:8.4-jdk21-jammy as builder
WORKDIR /app
COPY --from=cache /home/gradle/.gradle /home/gradle/.gradle
COPY src ./src
COPY build.gradle settings.gradle ./
RUN gradle build --no-daemon --parallel --build-cache -x test -x check

# Этап 3: Финальный образ
FROM eclipse-temurin:21-jre

# Устанавливаем curl для healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Основные команды запуска
ENTRYPOINT ["java", \
    "--add-opens", "java.base/java.util=ALL-UNNAMED", \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "--add-opens", "java.base/java.nio=ALL-UNNAMED", \
    "-DIGNITE_PEER_CLASS_LOADING_ENABLED=true", \
    "-DIGNITE_CLUSTER_NAME=spring-ignite-cluster", \
    "-DIGNITE_BINARY_COMPACT_FOOTER=false", \
    "-DIGNITE_NO_JDBC=true", \
    "-DIGNITE_SQL_ENABLE=false", \
    "-Dspring.datasource.driver-class-name=org.postgresql.Driver", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]