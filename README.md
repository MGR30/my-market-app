# My Market App — Реактивная витрина интернет-магазина с OAuth2 и Spring Security

Веб-приложение «Витрина интернет-магазина», переписанное на реактивный стек (Spring WebFlux + Spring Data R2DBC).
В проект добавлена авторизация пользователей (покупателей) и защищенное OAuth2-взаимодействие между микросервисами.

## Стек технологий

| Слой | Технология |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 4.1.1 (Reactor Netty) |
| Web (Реактивный) | Spring WebFlux |
| Безопасность | Spring Security (Form Login + OAuth2 Client/Resource Server) |
| Сервер авторизации | Keycloak 24 (Client Credentials Flow) |
| Шаблонизатор | Thymeleaf (реактивный) |
| Persistence (Реактивный)| Spring Data R2DBC (PostgreSQL 16) |
| Кеш | Redis 7 (Reactive Redis) |
| Сборка | Maven (мультипроект) |
| Тесты | JUnit 5, WebTestClient, Reactor Test, Testcontainers, Spring Security Test |
| Контейнеризация | Docker, Docker Compose |

## Структура проекта (Maven Multi-module)

* `payment-api` — OpenAPI-контракт (спецификация) для сервиса платежей.
* `payment-service` — RESTful-сервис платежей. Выступает в роли **OAuth2 Resource Server** (проверяет JWT-токены Keycloak).
* `market-app` — Основное веб-приложение (витрина). Выступает в роли **OAuth2 Client** (получает токен по Client Credentials Flow для запросов в payment-service).
* `keycloak` — Конфигурация (realm-export.json) для автоматического развертывания сервера авторизации.

## Как собрать и протестировать

Для сборки требуется установленный JDK 21 и Docker (для интеграционных тестов Testcontainers).

```bash
# Сборка всего проекта и запуск тестов
mvn clean package

# Сборка без тестов (быстрее)
mvn clean package -DskipTests
Все 43 теста (34 в market-app и 9 в payment-service) должны пройти успешно.

Запуск проекта (Docker Compose)
Проект полностью контейнеризирован. Для запуска используйте:

bash
docker compose up --build -d

bash
docker compose down -v
docker compose up --build -d
После запуска доступны следующие сервисы:

Витрина магазина: http://localhost:8080/items

Сервис платежей (health): http://localhost:8081/actuator/health

Keycloak (Админка): http://localhost:8082 (Логин: admin, Пароль: admin)

Тестовые данные
При старте market-app автоматически создает пользователей в БД (users):

Логин	Пароль	Роль
user1	password1	USER
user2	password2	USER
В Keycloak автоматически импортируется realm my-market со следующими клиентами:

market-app (Client Credentials Flow): client_id=market-app, client_secret=market-app-secret.

payment-service (Bearer Only): принимает только токены с правильным iss (issuer).