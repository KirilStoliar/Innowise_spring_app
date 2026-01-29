# Build stage
FROM gradle:8.5-jdk21-alpine AS builder
WORKDIR /workspace/app

# Копируем ВЕСЬ проект
COPY .. .

# Отладочная информация - проверим что скопировалось
RUN echo "=== Checking files in /workspace/app ===" && \
    ls -la && \
    echo "=== Looking for gradlew ===" && \
    find . -name "gradlew" -type f

# Даем права на выполнение gradlew (используем полный путь)
RUN chmod +x /workspace/app/gradlew

# Проверяем что gradlew исполняемый
RUN ls -la /workspace/app/gradlew

# Собираем приложение (используем полный путь к gradlew)
RUN /workspace/app/gradlew :payment-service:bootJar --no-daemon --stacktrace

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /workspace/app/payment-service/build/libs/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]