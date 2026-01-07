# Spring Final Project - Hotel Booking System

Микросервисная система управления отелями и бронированиями с использованием Spring Boot 3.5.x и Spring Cloud.

## 📚 Документация

### Быстрый старт
- **[QUICK_START.md](QUICK_START.md)** - Быстрый старт за 5 минут
- **[MAKEFILE_USAGE.md](MAKEFILE_USAGE.md)** - Полное руководство по Makefile
- **[API_EXAMPLES.md](API_EXAMPLES.md)** - Примеры API запросов с curl


## 🏗️ Архитектура

Система состоит из следующих компонентов:

```
┌─────────────────────────────────────────┐
│         API Gateway (8080)              │
│    Spring Cloud Gateway                 │
└───────────┬─────────────────────────────┘
            │
            ├──────────────┬───────────────┐
            ▼              ▼               ▼
    ┌───────────┐  ┌─────────────┐  ┌──────────────┐
    │  Eureka   │  │   Hotel     │  │   Booking    │
    │  (8761)   │  │  Service    │  │   Service    │
    │           │  │   (8081)    │  │    (8082)    │
    └───────────┘  └─────────────┘  └──────────────┘
         │               │                   │
         │          ┌────┴────┐         ┌────┴────┐
         │          │ H2 DB   │         │ H2 DB   │
         │          │ hoteldb │         │bookingdb│
         │          └─────────┘         └─────────┘
         │
    Service Discovery
```

- **Eureka Server** (порт 8761) - Service Discovery
- **API Gateway** (порт 8080) - Маршрутизация запросов, передача JWT
- **Hotel Service** (порт 8081) - Управление отелями и номерами, H2 in-memory
- **Booking Service** (порт 8082) - Бронирования, пользователи, JWT auth, H2 in-memory

## 🚀 Требования

- Java 17+
- Maven 3.6+
- Make (опционально, для удобства)

## Сборка и запуск

### Быстрый старт

```bash
# Показать все доступные команды
make help

# Собрать проект
make build

# Запустить все сервисы
make start

# Проверить статус сервисов
make status

# Остановить все сервисы
make stop
```

### Доступные Make команды

```bash
make build          # Собрать весь проект
make clean          # Очистить проект
make start          # Запустить все сервисы (проверяет порты)
make stop           # Остановить все сервисы
make restart        # Перезапустить все сервисы
make status         # Показать статус сервисов
make logs           # Показать логи всех сервисов
make logs-eureka    # Логи только Eureka
make logs-hotel     # Логи только Hotel Service
make logs-booking   # Логи только Booking Service
make logs-gateway   # Логи только Gateway
make test           # Запустить тесты
make package        # Создать jar файлы
make run-jars       # Запустить из jar файлов
make info           # Показать информацию о проекте
make api-test       # Быстрый API тест
```

### Запуск отдельного сервиса для разработки

```bash
# Запустить только один сервис
make dev SERVICE=hotel
make dev SERVICE=booking
make dev SERVICE=eureka
make dev SERVICE=gateway
```

### Альтернативный способ: Ручной запуск

```bash
# 1. Запустить Eureka Server
cd spring-final-eureka
mvn spring-boot:run

# 2. Запустить Hotel Service
cd spring-final-hotel
mvn spring-boot:run

# 3. Запустить Booking Service
cd spring-final-booking
mvn spring-boot:run

# 4. Запустить API Gateway
cd spring-final-gateway
mvn spring-boot:run
```


## Тестовые пользователи

После запуска Booking Service автоматически создаются тестовые пользователи:

- **Admin**: username=`admin`, password=`admin123`
- **User**: username=`user`, password=`user123`

## Тестовые данные

Hotel Service автоматически создает:
- 2 отеля (Grand Hotel, Business Hotel)
- 10 номеров различных типов

## API Endpoints

### Аутентификация (через Gateway или напрямую Booking Service)

```bash
# Регистрация
POST http://localhost:8080/api/user/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123"
}

# Аутентификация
POST http://localhost:8080/api/user/auth
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### Отели (требуется JWT токен)

```bash
# Получить список отелей
GET http://localhost:8080/api/hotels
Authorization: Bearer {token}

# Создать отель (ADMIN)
POST http://localhost:8080/api/hotels
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "New Hotel",
  "address": "789 New Street, Moscow"
}
```

### Номера

```bash
# Получить доступные номера
GET http://localhost:8080/api/rooms?startDate=2026-02-01&endDate=2026-02-05
Authorization: Bearer {token}

# Получить рекомендованные номера (отсортированные по загруженности)
GET http://localhost:8080/api/rooms/recommend?startDate=2026-02-01&endDate=2026-02-05
Authorization: Bearer {token}

# Создать номер (ADMIN)
POST http://localhost:8080/api/rooms
Authorization: Bearer {token}
Content-Type: application/json

{
  "hotelId": 1,
  "roomNumber": "303",
  "type": "SUITE",
  "pricePerNight": 15000
}
```

### Бронирования

```bash
# Создать бронирование с автоподбором комнаты
POST http://localhost:8080/api/bookings
Authorization: Bearer {token}
Content-Type: application/json

{
  "startDate": "2026-02-01",
  "endDate": "2026-02-05",
  "autoSelect": true,
  "roomType": "DOUBLE"
}

# Создать бронирование с выбором конкретной комнаты
POST http://localhost:8080/api/bookings
Authorization: Bearer {token}
Content-Type: application/json

{
  "roomId": 1,
  "startDate": "2026-02-01",
  "endDate": "2026-02-05",
  "autoSelect": false
}

# Получить историю бронирований
GET http://localhost:8080/api/bookings
Authorization: Bearer {token}

# Получить бронирование по ID
GET http://localhost:8080/api/bookings/{id}
Authorization: Bearer {token}

# Отменить бронирование
DELETE http://localhost:8080/api/bookings/{id}
Authorization: Bearer {token}
```

## Swagger UI

- Eureka: http://localhost:8761
- Hotel Service: http://localhost:8081/swagger-ui.html
- Booking Service: http://localhost:8082/swagger-ui.html
- API Gateway: http://localhost:8080/swagger-ui.html

## H2 Console

- Hotel Service: http://localhost:8081/h2-console (JDBC URL: `jdbc:h2:mem:hoteldb`)
- Booking Service: http://localhost:8082/h2-console (JDBC URL: `jdbc:h2:mem:bookingdb`)

## Особенности реализации

### Распределенные транзакции (Saga Pattern)

При создании бронирования используется паттерн Saga:

1. **PENDING** - Создается бронирование в статусе PENDING
2. **Confirmation** - Booking Service вызывает Hotel Service для подтверждения доступности
3. **CONFIRMED** - При успехе статус меняется на CONFIRMED
4. **COMPENSATED** - При ошибке выполняется компенсация (отмена резервации)

### Идемпотентность

Все операции создания бронирования идемпотентны благодаря использованию `requestId`:
- Повторный запрос с тем же `requestId` вернет существующее бронирование
- Предотвращает создание дубликатов при сетевых сбоях

### Retry и Timeout

- Вызовы между сервисами имеют настроенные timeout (5 секунд)
- Автоматические retry с exponential backoff (3 попытки)
- При исчерпании попыток выполняется компенсация

### Алгоритм балансировки загрузки

Hotel Service отслеживает количество бронирований каждого номера (`timesBooked`).
При автоподборе номера выбираются те, что имеют наименьшее количество бронирований.

### Cleanup Scheduler

- Hotel Service: очистка истекших резерваций каждую минуту
- Booking Service: очистка истекших PENDING бронирований каждую минуту

## Технологический стек

- Spring Boot 3.5.9
- Spring Cloud 2024.0.2
- Spring Data JPA
- Spring Security + JWT
- H2 Database (in-memory)
- Lombok
- MapStruct
- SpringDoc OpenAPI
- Netflix Eureka
- Spring Cloud Gateway
- WebFlux (для WebClient и Gateway)

## Логирование

Все сервисы поддерживают correlation ID через MDC:
- Заголовок `X-Correlation-Id` прослеживается через все сервисы
- Логи включают correlation ID для трассировки запросов

