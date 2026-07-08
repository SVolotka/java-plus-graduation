# Explore With Me (EWM)

Приложение для поиска и создания событий, управления заявками на участие, комментариями и пользователями.

## Архитектура

Проект состоит из **5 микросервисов** и **инфраструктурных компонентов**, взаимодействующих через API Gateway и Service Discovery.

### Инфраструктура
- **Gateway Server** – единая точка входа для внешних запросов (порт 8080). Маршрутизирует запросы к микросервисам через Eureka.
- **Discovery Server** (Eureka) – реестр сервисов для динамического обнаружения.
- **Config Server** – централизованное хранение конфигураций для всех сервисов.

### Микросервисы

| Сервис | Назначение | Ключевые API (внешние) | Внутреннее API (Feign) |
|--------|------------|------------------------|------------------------|
| **event-service** | Управление событиями, категориями, подборками | `/events/**`, `/admin/events/**`, `/admin/categories/**`, `/admin/compilations/**` | `/internal/events/{id}` (для получения события по ID) |
| **request-service** | Управление заявками на участие в событиях | `/users/{userId}/requests/**`, `/users/{userId}/events/{eventId}/requests/**` | `/internal/requests/count` (подсчёт подтверждённых заявок) |
| **user-service** | Администрирование пользователей | `/admin/users/**` | `/internal/users/batch` (получение списка пользователей по IDs), `/admin/users/{id}` (получение пользователя) |
| **comment-service** | Комментарии к событиям и реакции (лайки/дизлайки) | `/events/{eventId}/comments/**`, `/admin/comments/**`, `/comments/**` | – |
| **stats-server** | Сбор и просмотр статистики обращений | `/hit`, `/stats` | – (используется через StatsClient, не Feign) |

### Зависимости сервисов (через Feign)

- **event-service** → вызывает `user-service` (получение данных пользователя), `request-service` (подсчёт заявок), `stats-server` (отправка и получение статистики).
- **request-service** → вызывает `user-service` и `event-service` (для проверки существования и получения информации о событии).
- **comment-service** → вызывает `user-service` и `event-service` (аналогично request-service).
- **user-service** не зависит от других core-сервисов.
- **stats-server** не зависит от других сервисов.

### Внешние спецификации API
- [EWM Main Service Specification](./ewm-main-service-spec.json) – внешнее API для событий, категорий, подборок, пользователей, заявок.
- [EWM Stats Service Specification](./ewm-stats-service-spec.json) – API сервиса статистики.

### Сборка и запуск
Требования: JDK 21, Maven, PostgreSQL (опционально, для production-профиля; в тестах используется H2).

```bash
# Собрать все модули
mvn clean install -DskipTests

# Запустить инфраструктуру (по порядку):
# 1. Discovery Server
# 2. Config Server
# 3. Gateway Server
# 4. stats-server, user-service, request-service, comment-service, event-service