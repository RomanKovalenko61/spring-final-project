# Makefile Usage Guide

## Доступные команды

### Компиляция и сборка

- `make clean` - Очистка всех артефактов сборки (mvn clean)
- `make compile` - Компиляция всех модулей без тестов (mvn clean compile -DskipTests)
- `make build` - Полная сборка проекта (mvn clean install -DskipTests)
- `make package` - Сборка JAR файлов (mvn package)

### Тестирование

- `make test` - Запуск всех тестов (mvn test)

**⚠️ ВАЖНО:** Для запуска интеграционных тестов необходимо, чтобы **все сервисы были запущены**:
```bash
# 1. Сначала запустите все сервисы
make start

# 2. Дождитесь полной инициализации (примерно 30-60 секунд)
make status

# 3. Теперь можно запускать тесты
make test
```

Альтернативно, можно запускать только unit-тесты без сервисов:
```bash
# Запуск тестов конкретного модуля
cd spring-final-booking
mvn test -Dtest=BookingIntegrationTest

cd spring-final-hotel
mvn test -Dtest=HotelIntegrationTest
```

### Управление сервисами

- `make start` - Запуск всех сервисов в правильном порядке (eureka → hotel → booking → gateway)
- `make stop` - Остановка всех запущенных сервисов
- `make restart` - Перезапуск всех сервисов (stop + start)
- `make status` - Проверка статуса всех сервисов (порты и PID)

### Просмотр логов

- `make logs` - Просмотр всех логов в реальном времени
- `make logs-eureka` - Просмотр логов Eureka Server
- `make logs-hotel` - Просмотр логов Hotel Service
- `make logs-booking` - Просмотр логов Booking Service
- `make logs-gateway` - Просмотр логов API Gateway

### Дополнительные команды

- `make run-jars` - Запуск собранных JAR файлов из target/
- `make dev SERVICE=<name>` - Запуск одного сервиса в режиме разработки
  - Примеры: `make dev SERVICE=eureka`, `make dev SERVICE=hotel`
- `make backup` - Создание резервной копии Makefile
- `make help` - Показать справку по всем командам

## Порты сервисов

- **Eureka Server**: 8761
- **API Gateway**: 8080
- **Hotel Service**: 8081
- **Booking Service**: 8082

## Примеры использования

### Первый запуск проекта

```bash
# 1. Скомпилировать проект
make compile

# 2. Запустить все сервисы
make start

# 3. Проверить статус
make status

# 4. (Опционально) Запустить тесты
make test
```

### Запуск тестов

```bash
# 1. Убедитесь, что все сервисы запущены
make start

# 2. Дождитесь готовности сервисов (30-60 секунд)
sleep 60

# 3. Проверьте статус
make status
# Все сервисы должны показывать RUNNING

# 4. Запустите тесты
make test
```

### Разработка

```bash
# Запустить один сервис для разработки
make dev SERVICE=booking

# Просмотреть логи конкретного сервиса
make logs-booking
```

### Перезапуск после изменений

```bash
# Остановить сервисы
make stop

# Перекомпилировать
make compile

# Запустить снова
make start
```

### Полная пересборка

```bash
# Очистить и пересобрать проект
make clean
make build

# Запустить сервисы
make start
```

## Расположение файлов

- **Логи**: `/logs/*.log`
- **PID файлы**: `/logs/*.pid`
- **JAR файлы**: `*/target/*.jar`

## Решение проблем

### Порт уже занят

```bash
# Проверить статус
make status

# Если сервис показывает RUNNING, но не отвечает
make stop
make start
```

### Сервис не запускается

```bash
# Проверить логи
make logs-<service-name>

# Или все логи сразу
make logs
```

### Очистка зависших процессов

```bash
# Остановить все через make
make stop

# Если не помогло, убить вручную
pkill -f spring-final
```

