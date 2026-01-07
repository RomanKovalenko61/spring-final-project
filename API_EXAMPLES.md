# API Examples for Spring Final Project

## 1. Аутентификация

### Регистрация нового пользователя
```bash
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "test123"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "testuser",
  "userId": 3
}
```

### Аутентификация существующего пользователя
```bash
curl -X POST http://localhost:8080/api/user/auth \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "userId": 1
}
```

**Сохраните токен для последующих запросов!**

---

## 2. Управление отелями

### Получить список всех отелей
```bash
curl -X GET http://localhost:8080/api/hotels \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Создать новый отель (только ADMIN)
```bash
curl -X POST http://localhost:8080/api/hotels \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Luxury Hotel",
    "address": "999 Luxury Street, Moscow"
  }'
```

---

## 3. Управление номерами

### Получить доступные номера на даты
```bash
curl -X GET "http://localhost:8080/api/rooms?startDate=2026-02-01&endDate=2026-02-05" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Получить рекомендованные номера (отсортированные по загруженности)
```bash
curl -X GET "http://localhost:8080/api/rooms/recommend?startDate=2026-02-01&endDate=2026-02-05&roomType=DOUBLE" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Создать новый номер (только ADMIN)
```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hotelId": 1,
    "roomNumber": "303",
    "type": "SUITE",
    "pricePerNight": 15000
  }'
```

Доступные типы номеров: `SINGLE`, `DOUBLE`, `SUITE`, `DELUXE`

---

## 4. Бронирования

### Создать бронирование с автоподбором комнаты
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "startDate": "2026-02-10",
    "endDate": "2026-02-15",
    "autoSelect": true,
    "roomType": "DOUBLE",
    "hotelId": 1
  }'
```

### Создать бронирование с выбором конкретной комнаты
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": 3,
    "startDate": "2026-02-20",
    "endDate": "2026-02-25",
    "autoSelect": false
  }'
```

Response (успех):
```json
{
  "id": 1,
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": 2,
  "hotelId": 1,
  "roomId": 3,
  "startDate": "2026-02-20",
  "endDate": "2026-02-25",
  "status": "CONFIRMED",
  "createdAt": "2026-01-05T10:30:00",
  "expiresAt": "2026-01-05T10:35:00",
  "compensationReason": null
}
```

Response (компенсация):
```json
{
  "id": 2,
  "requestId": "550e8400-e29b-41d4-a716-446655440001",
  "userId": 2,
  "status": "COMPENSATED",
  "compensationReason": "No available rooms"
}
```

### Получить историю бронирований текущего пользователя
```bash
curl -X GET http://localhost:8080/api/bookings \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Получить конкретное бронирование по ID
```bash
curl -X GET http://localhost:8080/api/bookings/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Отменить бронирование
```bash
curl -X DELETE http://localhost:8080/api/bookings/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 5. Административные операции

### Создать пользователя (только ADMIN)
```bash
curl -X POST http://localhost:8080/api/user \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newadmin",
    "password": "admin456",
    "roles": ["ROLE_ADMIN", "ROLE_USER"],
    "enabled": true
  }'
```

### Обновить пользователя (только ADMIN)
```bash
curl -X PATCH http://localhost:8080/api/user/3 \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": false
  }'
```

### Удалить пользователя (только ADMIN)
```bash
curl -X DELETE http://localhost:8080/api/user/3 \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

---

## Полный сценарий использования

### Шаг 1: Аутентификация
```bash
# Получить токен для пользователя
TOKEN=$(curl -s -X POST http://localhost:8080/api/user/auth \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "user123"}' \
  | jq -r '.token')

echo "Token: $TOKEN"
```

### Шаг 2: Поиск доступных номеров
```bash
curl -X GET "http://localhost:8080/api/rooms/recommend?startDate=2026-03-01&endDate=2026-03-05&roomType=DOUBLE" \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Шаг 3: Создание бронирования
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

### Шаг 4: Просмотр истории бронирований
```bash
curl -X GET http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Идемпотентность

Вы можете передать свой собственный `requestId` для обеспечения идемпотентности:

```bash
REQUEST_ID=$(uuidgen)

curl -X POST http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"requestId\": \"$REQUEST_ID\",
    \"startDate\": \"2026-03-10\",
    \"endDate\": \"2026-03-15\",
    \"autoSelect\": true,
    \"roomType\": \"SUITE\"
  }"
```

Повторный запрос с тем же `requestId` вернет то же самое бронирование без создания дубликата.

---

## Статусы бронирований

- `PENDING` - Бронирование создано, ожидает подтверждения
- `CONFIRMED` - Бронирование подтверждено, номер зарезервирован
- `CANCELLED` - Бронирование отменено пользователем
- `COMPENSATED` - Бронирование не удалось (недоступен номер, ошибка, таймаут)

---

## Типы номеров

- `SINGLE` - Одноместный
- `DOUBLE` - Двухместный
- `SUITE` - Люкс
- `DELUXE` - Делюкс

---

## Полезные команды

### Проверка здоровья сервисов через Eureka
```bash
curl http://localhost:8761/eureka/apps | xmllint --format -
```

### Просмотр логов
```bash
# Hotel Service
tail -f logs/hotel.log

# Booking Service
tail -f logs/booking.log
```

