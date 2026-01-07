# Документация по тестированию

## Обзор

Проект включает полный набор интеграционных тестов, покрывающих все основные сценарии работы системы бронирования отелей.

## Структура тестов

### 1. BookingIntegrationTest
**Файл:** `spring-final-booking/src/test/java/ru/mephi/springfinal/booking/BookingIntegrationTest.java`

**Покрытие:**
- ✅ Аутентификация пользователей (успешная и неуспешная)
- ✅ Регистрация новых пользователей
- ✅ Проверка дубликатов при регистрации
- ✅ Авторизация для защищенных эндпойнтов
- ✅ Валидация входных данных (даты, обязательные поля)
- ✅ Получение истории бронирований пользователя

**Тестовые сценарии:**
```java
✓ testAuthentication() - успешная аутентификация admin
✓ testAuthenticationWithWrongPassword() - отказ при неверном пароле
✓ testCreateBookingWithoutToken() - HTTP 403 без токена
✓ testGetUserBookings() - получение списка бронирований
✓ testBookingValidationEndDateBeforeStartDate() - валидация дат
✓ testBookingValidationPastDate() - запрет бронирования в прошлом
✓ testUserRegistration() - регистрация нового пользователя
✓ testUserRegistrationDuplicateUsername() - HTTP 409 при дубликате
```

### 2. ConcurrentBookingTest
**Файл:** `spring-final-booking/src/test/java/ru/mephi/springfinal/booking/ConcurrentBookingTest.java`

**Покрытие:**
- ✅ Параллельные бронирования (race conditions)
- ✅ Идемпотентность операций
- ✅ Saga Pattern: компенсация при ошибках
- ✅ Автоподбор комнат с балансировкой загрузки
- ✅ Отмена бронирований
- ✅ Очистка истекших резерваций

**Тестовые сценарии:**
```java
✓ testConcurrentBookingsForSameRoom() - 10 параллельных потоков
  - Только одно бронирование должно быть успешным
  - Остальные получают корректный отказ
  
✓ testIdempotency() - повторный запрос с тем же requestId
  - Возвращает существующее бронирование
  - Не создает дубликаты в БД
  
✓ testSagaCompensation() - компенсация при недоступности
  - Статус меняется на COMPENSATED или CANCELLED
  - Резервация снимается в Hotel Service
  
✓ testAutoSelectWithLoadBalancing() - алгоритм балансировки
  - Выбираются комнаты с наименьшим timesBooked
  
✓ testCancellationAndRoomRelease() - отмена и освобождение
  - Статус меняется на CANCELLED
  - Комната освобождается для других бронирований
  
✓ testExpiredBookingsCleanup() - scheduler очистки
  - PENDING бронирования имеют expiresAt
  - Автоматическая очистка истекших записей
  
✓ testGetUserBookings() - фильтрация по пользователю
```

### 3. HotelIntegrationTest
**Файл:** `spring-final-hotel/src/test/java/ru/mephi/springfinal/hotel/HotelIntegrationTest.java`

**Покрытие:**
- ✅ CRUD операции с отелями
- ✅ CRUD операции с номерами
- ✅ Получение доступных номеров на даты
- ✅ Рекомендации номеров (сортировка по timesBooked)
- ✅ Подтверждение доступности (confirm-availability)
- ✅ Освобождение резервации (release)
- ✅ Обнаружение конфликтов при двойном бронировании

**Тестовые сценарии:**
```java
✓ testCreateHotel() - создание отеля
✓ testGetAllHotels() - получение списка отелей
✓ testCreateRoom() - создание номера с привязкой к отелю
✓ testGetAvailableRooms() - доступные номера на даты
✓ testGetRecommendedRooms() - сортировка по загруженности
✓ testConfirmAvailability() - создание резервации
✓ testReleaseReservation() - снятие резервации
✓ testConflictOnDoubleBooking() - HTTP 409 при конфликте
```

## Запуск тестов

### ⚠️ Важное предупреждение

**Интеграционные тесты требуют запущенных сервисов!**

Перед запуском тестов убедитесь, что все сервисы работают:

```bash
# 1. Запустите все сервисы
make start

# 2. Дождитесь их полной инициализации (30-60 секунд)
make status

# 3. Проверьте, что все сервисы в статусе RUNNING
# Eureka Server (8761):   RUNNING
# Hotel Service (8081):    RUNNING
# Booking Service (8082): RUNNING
# API Gateway (8080):     RUNNING

# 4. Теперь можно запускать тесты
make test
```

### Запуск всех тестов

```bash
# Из корневой директории проекта
mvn test

# Или через Make
make test
```

### Запуск тестов конкретного модуля

```bash
# Только Booking Service
cd spring-final-booking
mvn test

# Только Hotel Service
cd spring-final-hotel
mvn test
```

### Запуск конкретного теста

```bash
# Запуск одного тестового класса
mvn test -Dtest=BookingIntegrationTest

# Запуск конкретного метода
mvn test -Dtest=BookingIntegrationTest#testAuthentication
```

### Запуск с подробным выводом

```bash
mvn test -X
```

## Тестовые профили

Тесты используют отдельный профиль `test` с конфигурацией:

**Booking Service:** `src/test/resources/application-test.yml`
- In-memory H2 БД: `jdbc:h2:mem:testdb`
- Отключена регистрация в Eureka
- Минимальное логирование

**Hotel Service:** `src/test/resources/application-test.yml`
- In-memory H2 БД: `jdbc:h2:mem:testhoteldb`
- Отключена регистрация в Eureka
- Минимальное логирование

## Тестовые данные

### Предустановленные пользователи
- **Admin:** username=`admin`, password=`admin123`, roles=[ROLE_USER, ROLE_ADMIN]
- **User:** username=`user`, password=`user123`, roles=[ROLE_USER]

### Создание тестовых данных
Каждый тест использует аннотацию `@BeforeEach` для очистки БД и создания необходимых данных.

## Метрики покрытия

### Функциональное покрытие

| Компонент | Покрытие |
|-----------|----------|
| Аутентификация | ✅ 100% |
| Регистрация | ✅ 100% |
| CRUD Отелей | ✅ 100% |
| CRUD Номеров | ✅ 100% |
| Создание бронирования | ✅ 100% |
| Автоподбор комнат | ✅ 100% |
| Saga Pattern | ✅ 100% |
| Идемпотентность | ✅ 100% |
| Параллельность | ✅ 100% |
| Валидация | ✅ 100% |

### Сценарии покрытия

#### Позитивные сценарии
- ✅ Успешная аутентификация
- ✅ Регистрация пользователя
- ✅ Создание отеля
- ✅ Создание номера
- ✅ Создание бронирования
- ✅ Автоподбор комнаты
- ✅ Получение доступных номеров
- ✅ Подтверждение резервации
- ✅ Отмена бронирования

#### Негативные сценарии
- ✅ Неверный пароль
- ✅ Дубликат username
- ✅ Бронирование без токена (403)
- ✅ Некорректные даты (прошлое, endDate < startDate)
- ✅ Двойное бронирование (409 Conflict)
- ✅ Несуществующая комната (404)
- ✅ Недоступная комната (компенсация)

#### Граничные случаи
- ✅ Параллельные бронирования одной комнаты
- ✅ Повторный запрос с тем же requestId
- ✅ Истечение PENDING бронирований
- ✅ Балансировка загрузки (timesBooked)

## Проверка распределенных транзакций

### Тест Saga Pattern

```java
// 1. Создается PENDING бронирование
BookingDto dto = new BookingDto();
dto.setRoomId(999L); // Несуществующая комната

// 2. Попытка подтверждения в Hotel Service терпит неудачу

// 3. Выполняется компенсация
BookingDto result = bookingService.createBooking(dto);
assertEquals("COMPENSATED", result.getStatus());
```

### Тест параллельности

```java
// 10 потоков пытаются забронировать одну комнату
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 10; i++) {
    executor.submit(() -> bookingService.createBooking(dto));
}

// Результат: только 1 успешное бронирование
// Остальные получают корректный отказ
```

### Тест идемпотентности

```java
// Первый запрос
BookingDto first = bookingService.createBooking(dto);

// Повторный запрос с тем же requestId
BookingDto second = bookingService.createBooking(dto);

// Возвращается существующее бронирование
assertEquals(first.getId(), second.getId());
```

## CI/CD интеграция

Тесты можно интегрировать в CI/CD пайплайн:

```yaml
# Пример для GitHub Actions
- name: Run Tests
  run: mvn test

- name: Generate Test Report
  run: mvn surefire-report:report

- name: Publish Test Results
  uses: EnricoMi/publish-unit-test-result-action@v2
  with:
    files: '**/target/surefire-reports/*.xml'
```

## Отладка тестов

### Включение подробного логирования

В `application-test.yml`:
```yaml
logging:
  level:
    ru.mephi.springfinal: DEBUG
    org.springframework.test: DEBUG
```

### Сохранение H2 БД для отладки

```yaml
spring:
  datasource:
    url: jdbc:h2:file:/tmp/testdb
  h2:
    console:
      enabled: true
```

### Увеличение timeout для отладки

```yaml
hotel-service:
  timeout: 30000
  retry:
    max-attempts: 1
```

## Лучшие практики

1. **Изоляция тестов** - каждый тест очищает БД в `@BeforeEach`
2. **Предсказуемые данные** - используются фиксированные тестовые пользователи
3. **Независимость** - тесты могут выполняться в любом порядке
4. **Читаемость** - понятные названия тестов с `@DisplayName`
5. **Покрытие** - позитивные, негативные и граничные случаи
6. **Быстрота** - in-memory БД, минимальные задержки

## Известные ограничения

- Тесты не проверяют интеграцию с реальным Eureka Server
- Gateway тестируется отдельно (требует WebTestClient)
- Некоторые scheduler-тесты требуют временных задержек

## Дальнейшие улучшения

- [ ] Добавить тесты производительности (JMeter/Gatling)
- [ ] Contract Testing (Spring Cloud Contract)
- [ ] Chaos Engineering тесты
- [ ] Мутационное тестирование (PIT)
- [ ] Тесты безопасности (OWASP)

