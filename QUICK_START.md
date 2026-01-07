# Quick Start Guide

## Быстрый старт (за 5 минут)

### 1. Предварительные требования

- Java 17+
- Maven 3.6+
- Свободные порты: 8080, 8081, 8082, 8761

### 2. Запуск

```bash
# Клонируйте проект (если еще не сделали)
cd /home/astek/IdeaProjects/spring-final-project

# Соберите проект
make build

# Запустите все сервисы
make start
```

Подождите ~50 секунд, пока все сервисы запустятся.

### 3. Проверка

```bash
# Проверьте статус
make status
```

Все сервисы должны быть в статусе **RUNNING**.

### 4. Первый запрос

```bash
# Получите JWT токен
curl -X POST http://localhost:8080/api/user/auth \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.token'
```

Сохраните полученный токен (например: `export TOKEN="ваш_токен"`).

### 5. Просмотр отелей

```bash
curl -X GET http://localhost:8080/api/hotels \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 6. Создание бронирования

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "startDate": "2026-03-01",
    "endDate": "2026-03-05",
    "autoSelect": true,
    "roomType": "DOUBLE"
  }' | jq
```

### 7. Просмотр бронирований

```bash
curl -X GET http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Веб-интерфейсы

После запуска откройте в браузере:

- **Eureka Dashboard**: http://localhost:8761
- **Swagger UI (Hotel)**: http://localhost:8081/swagger-ui.html
- **Swagger UI (Booking)**: http://localhost:8082/swagger-ui.html
- **H2 Console (Hotel)**: http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:mem:hoteldb`
  - Username: `sa`
  - Password: (пусто)
- **H2 Console (Booking)**: http://localhost:8082/h2-console
  - JDBC URL: `jdbc:h2:mem:bookingdb`
  - Username: `sa`
  - Password: (пусто)

---

## Тестовые данные

### Пользователи

| Username | Password   | Role       |
|----------|------------|------------|
| admin    | admin123   | ROLE_ADMIN |
| user     | user123    | ROLE_USER  |

### Отели

- **Grand Hotel** - 123 Main Street, Moscow (5 номеров)
- **Business Hotel** - 456 Business Ave, Moscow (5 номеров)

### Типы номеров

- SINGLE (одноместный)
- DOUBLE (двухместный)
- SUITE (люкс)
- DELUXE (делюкс)

---

## Полезные команды

```bash
# Остановить все сервисы
make stop

# Перезапустить
make restart

# Просмотр логов
make logs-hotel    # Hotel Service
make logs-booking  # Booking Service
make logs          # Все сервисы

# Очистка
make clean

# Помощь
make help
```

---

## Типичные сценарии

### Создать бронирование с конкретным номером

```bash
# 1. Получите список доступных номеров
curl -X GET "http://localhost:8080/api/rooms?startDate=2026-04-01&endDate=2026-04-05" \
  -H "Authorization: Bearer $TOKEN" | jq

# 2. Создайте бронирование с roomId
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 1,
    "startDate": "2026-04-01",
    "endDate": "2026-04-05",
    "autoSelect": false
  }' | jq
```

### Получить рекомендованные номера

```bash
# Номера отсортированы по загруженности (наименее загруженные первыми)
curl -X GET "http://localhost:8080/api/rooms/recommend?startDate=2026-05-01&endDate=2026-05-10&roomType=SUITE" \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Отменить бронирование

```bash
# Получите ID бронирования из списка
curl -X GET http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" | jq

# Отмените бронирование
curl -X DELETE http://localhost:8080/api/bookings/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Troubleshooting

### Порты заняты

```bash
# Проверьте, какие порты заняты
lsof -i :8761
lsof -i :8080
lsof -i :8081
lsof -i :8082

# Остановите старые процессы
make stop
```

### Сервисы не запускаются

```bash
# Проверьте логи
make logs-eureka
make logs-hotel
make logs-booking
make logs-gateway

# Попробуйте пересобрать
make clean
make build
make start
```

### JWT токен истек

JWT токены действительны 1 час. Просто получите новый:

```bash
curl -X POST http://localhost:8080/api/user/auth \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.token'
```

---

## Следующие шаги

1. Изучите API: см. [API_EXAMPLES.md](API_EXAMPLES.md)
2. Изучите архитектуру: см. [README.md](README.md)


