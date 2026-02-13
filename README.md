# Innowise Trainee Spring — Microservices Platform

Микросервисное приложение на **Spring Boot 3.5.7** и **Java 21**, реализованное в рамках *trainee‑проекта*. Проект демонстрирует построение микросервисной архитектуры с API Gateway, централизованной безопасностью, асинхронным взаимодействием, CI/CD, контейнеризацией и Kubernetes, а также запуском мониторинга и тестированием сервисов.

---

## Структура проекта

```text
Innowise-trainee-spring
├── .github/../ci-cd.yml      # GitHub Actions workflow
├── api-gateway/              # API Gateway (Spring Cloud Gateway)
├── auth-service/             # Аутентификация и авторизация (JWT)
├── user-service/             # Управление пользователями
├── order-service/            # Управление заказами
├── payment-service/          # Платежи и события
├── frontend/ui/              # Angular frontend
├── k8s/                      # Kubernetes манифесты
├── docker-compose.yml        # Локальный запуск
├── build.gradle              # Root Gradle конфигурация
├── .env.example              # Пример env-переменных
└── README.md
```

---

## Взаимодействие сервисов

### API Gateway

* Единственная точка входа
* JWT‑валидация
* Маршрутизация запросов

### Auth Service

* Регистрация / логин
* Access / Refresh JWT

### User Service

* CRUD пользователей
* Роли и права доступа
* Redis cache

### Order Service

* Управление заказами
* Circuit Breaker + Retry (**Resilience4j**)
* Kafka consumer

### Payment Service

* Платежи
* MongoDB
* Kafka producer

---

## Технологический стек

### Backend

* Java 21
* Spring Boot 3.5.7
* Spring Security + JWT
* Spring Data JPA, Hibernate
* Spring Data MongoDB
* Spring Cloud Gateway
* Liquibase (PostgreSQL + MongoDB)
* Kafka
* Redis
* Resilience4j
* MapStruct
* OpenAPI / Swagger
* Micrometer + Prometheus
* Loki
* JUnit + Mockito
* TestContainers

### Frontend

* Angular
* TypeScript
* RxJS
* Tailwind

### Infrastructure & DevOps

* Docker / Docker Compose
* Kubernetes + Kustomize
* GitHub Actions (CI/CD)
* Testcontainers
* SonarCloud
* Trivy (security scan)

---

## Запуск проекта

### 1 Предварительные требования

* Java 21
* Docker & Docker Compose
* Node.js 20+
* Gradle

### 2 Конфигурация окружения

Создайте `.env` файл на основе примера:

```bash
cp .env.example .env
```

Минимально необходимо указать:

```env
POSTGRES_USER=your_postgres_user
POSTGRES_PASSWORD=your_postgres_password
JWT_SECRET=your_jwt_secret_min_32_chars
API_GATEWAY_INTERNAL_TOKEN=internal-token
```

### 3 Запуск через Docker Compose

После старта будут доступны следующие сервисы:

| Сервис          | URL                                            |
| --------------- | ---------------------------------------------- |
| API Gateway     | [http://localhost:8083](http://localhost:8083) |
| User Service    | [http://localhost:8081](http://localhost:8081) |
| Auth Service    | [http://localhost:8082](http://localhost:8082) |
| Order Service   | [http://localhost:8083](http://localhost:8083) |
| Payment Service | [http://localhost:8084](http://localhost:8084) |
| Frontend        | [http://localhost:4200](http://localhost:4200) |

---

## Swagger / OpenAPI

Swagger доступен для каждого сервиса локально, **кроме `api-gateway`**:

```text
http://localhost:{port}/swagger-ui.html
```

Примеры:

* [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) — User Service
* [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) — Auth Service

---

## Тестирование

Проект содержит **unit** и **integration** тесты. В разных сервисах используются разные профили и условия запуска.

### Unit тесты

Запускаются всегда и не требуют Testcontainers:

```bash
./gradlew test
```

### Integration тесты

#### Payment Service / Order Service

Для `payment-service` и `order-service` интеграционные тесты используют **Testcontainers** и запускаются **только при наличии system property**:

```bash
./gradlew integration-test -Duse.testcontainers=true
```

или для одного сервиса:

```bash
./gradlew :payment-service:integration-test -Duse.testcontainers=true
./gradlew :order-service:integration-test -Duse.testcontainers=true
```

---

#### User Service

В `user-service` интеграционные тесты используют отдельный профиль и **не требуют system property**:

```bash
./gradlew :user-service:test --tests "*IntegrationTest"
```

или запуск всех тестов сервиса:

```bash
./gradlew :user-service:test
```

---

### Запуск всех тестов проекта

```bash
./gradlew test integration-test -Duse.testcontainers=true
```

---

## CI/CD Pipeline

GitHub Actions пайплайн включает:

* Сборку всех сервисов
* Unit & Integration тесты
* Testcontainers
* Jacoco coverage
* SonarCloud анализ
* Docker image build
* Security scanning (**Trivy**)
* Frontend build

Пайплайн запускается при:

* `push` в `main`, `develop`
* `pull_request`

---

## Kubernetes

Манифесты находятся в папке `k8s/`. Команда запуска для всех манифестов и сервисов (кроме мониторинга, который запускается отдельно через Helm):

```bash
kubectl apply -k k8s/
```

Используется:

* Namespace `innowise`
* ConfigMaps & Secrets
* PostgreSQL + MongoDB
* Kafka + Redis
* Ingress
* OpenTelemetry instrumentation (через Helm)

---

## Безопасность

* JWT Access / Refresh токены
* Ролевая модель (**USER / ADMIN**)
* Internal token для межсервисных запросов
* Trivy security scan
* Spring Security filters

---

## Статус проекта

Проект реализован как учебный, но максимально приближенный к production:

* корректная модульность
* реальный CI/CD
* инфраструктура
* тестирование
* observability

---

## Автор

**Кирилл Столяр**
Java Backend Developer (Trainee)
