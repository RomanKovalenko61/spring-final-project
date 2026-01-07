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

