# ⚠️ ВАЖНО: Запуск тестов

## Интеграционные тесты требуют запущенных сервисов!

### Правильная последовательность:

```bash
# 1. Запустите все сервисы
make start

# 2. Дождитесь готовности (30-60 секунд)
sleep 60

# 3. Проверьте статус
make status

# Должно быть:
# ✅ Eureka Server (8761):   RUNNING
# ✅ Hotel Service (8081):    RUNNING  
# ✅ Booking Service (8082): RUNNING
# ✅ API Gateway (8080):     RUNNING

# 4. Запустите тесты
make test
```

### Если сервисы не запущены:

❌ Тесты завершатся с ошибками:
- `Connection refused`
- `503 Service Unavailable`
- `Timeout exceptions`

### Офлайн-режим (какие тесты можно запускать без поднятых сервисов)

После правок тестов некоторые сценарии поддерживают локальную (offline) работу с использованием мока `HotelServiceClient`.

- `ConcurrentBookingTest` (модуль `spring-final-booking`) использует тестовую конфигурацию-мок
  `spring-final-booking/src/test/java/ru/mephi/springfinal/booking/config/TestHotelServiceConfig.java` —
  этот тест моделирует подтверждение/освобождение номеров и может запускаться без реального Hotel Service.

- `HotelIntegrationTest` (модуль `spring-final-hotel`) выполняется через MockMvc и, как правило, не требует внешних сервисов; однако некоторые тесты могли быть настроены на использование Security — при профиле `test` Security фильтры отключены для удобства запуска.

- Большинство остальных интеграционных тестов (например, `BookingIntegrationTest`) в стандартной конфигурации ожидают наличие запущенного Hotel Service / Eureka; их можно модифицировать аналогичным образом (ввести моки) если нужно запускать полностью offline.

### Команды для запуска (offline vs full)

```bash
# Запуск конкретного теста, использующего мок (offline):
cd spring-final-booking
mvn clean test -Dtest=ConcurrentBookingTest

# Запуск конкретного теста модуля (через корень проекта):
mvn -pl spring-final-booking -am test -Dtest=ConcurrentBookingTest

# Полный запуск (требует запущенных сервисов):
make start && sleep 60 && make test

# Для гарантированной чистоты сборки перед запуском тестов:
mvn clean
```

### Альтернативные варианты:

```bash
# Тесты конкретного модуля
cd spring-final-booking
mvn test

# Конкретный тестовый класс
mvn test -Dtest=BookingIntegrationTest

# Конкретный тест
mvn test -Dtest=BookingIntegrationTest#testAuthentication
```

## Быстрая справка:

```bash
# посмотреть документацию
cat TESTING_GUIDE.md
cat MAKEFILE_USAGE.md
```

---

**См. также:**
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Подробное руководство по тестированию
- [MAKEFILE_USAGE.md](MAKEFILE_USAGE.md) - Справка по командам Makefile
- [README.md](README.md) - Основная документация проекта

