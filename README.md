# E-Scooter Sharing Platform

Початковий фундамент платформи шерингу електросамокатів: REST CRUD для
`Scooter`, PostgreSQL із Liquibase-міграціями, статичний UI та дві моделі
автентифікації — OIDC для браузера і JWT Bearer для API.

## Вимоги та запуск

- JDK 21
- Maven 3.9+
- Docker Desktop або сумісний Docker Engine

Переконайтеся, що Docker працює, та запустіть із кореня проєкту:

```bash
mvn spring-boot:run
```

Spring Boot Docker Compose інтеграція автоматично запускає сервіси з
`compose.yaml`: PostgreSQL та Keycloak. Під час першого запуску завантажуються
Docker-образи, тому старт може тривати довше. Додаток доступний на
http://localhost:8081, Keycloak — на http://localhost:8080.

Схема `scooters` і три тестові записи створюються Liquibase changelog-ом
автоматично; Hibernate працює з `ddl-auto: validate` і не генерує схему.
PostgreSQL зберігає дані у Docker volume `postgres-data`. Щоб вручну запустити
контейнери окремо, можна виконати `docker compose up -d`, а потім
`mvn spring-boot:run`. Щоб видалити локальну тестову БД і створити її з нуля:
`docker compose down -v`, після чого знову запустіть застосунок.

Параметри БД за замовчуванням: `scooter_sharing` / `scooter_app` /
`scooter_dev`. Їх можна перевизначити змінними середовища `DB_HOST`, `DB_PORT`,
`DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`. У початковому шаблоні datasource не
був налаштований; чинні налаштування Keycloak та OIDC збережено.

## Keycloak і UI

Realm `course-realm` імпортується з `keycloak/course-realm.json`.

| Користувач | Пароль | Realm-ролі |
|---|---|---|
| `alice` | `alice123` | `USER`, `ADMIN` |
| `bob` | `bob123` | `USER` |

OIDC-клієнт `backend-app` залишається public client із увімкненим Authorization
Code Flow та Direct Access Grants. У браузері відкрийте
http://localhost:8081/ui/index.html; неавтентифікованого користувача
перенаправить на сторінку входу Keycloak. Після входу сторінка дозволяє
переглядати, створювати, редагувати й видаляти самокати. UI отримує access token
із захищеного `/ui/token` і передає його API як Bearer-токен.

## REST API

Усі маршрути нижче очікують `Authorization: Bearer <token>`:

| Метод | Шлях | Опис |
|---|---|---|
| `GET` | `/api/scooters` | список |
| `GET` | `/api/scooters/{id}` | один самокат |
| `POST` | `/api/scooters` | створити |
| `PUT` | `/api/scooters/{id}` | оновити |
| `DELETE` | `/api/scooters/{id}` | видалити |

DTO має поля `id`, `serialNumber`, `model`, `batteryLevel` (0–100),
`pricePerMinute` (додатне число) і `status` (`AVAILABLE`, `IN_USE`,
`MAINTENANCE`). Створення повертає `201 Created`, видалення — `204 No Content`.
Доступ до CRUD мають ролі `USER` та `ADMIN`.

Для перевірки використовуйте `requests.http` (наприклад, вбудований HTTP Client
IntelliJ IDEA). Файл отримує токени обох користувачів, виконує CRUD, перевіряє
`401` без токена, `403` для USER на ADMIN-маршруті та `403` для USER на методі
з `@PreAuthorize`. Прямі API-запити використовують JWT і не потребують
браузерної сесії.

## Два рівні контролю ролей

- У `SecurityConfig` маршрут `/api/admin/**` вимагає `ADMIN`; наприклад,
  `GET /api/admin/reports` повертає `403` для `bob`.
- `AdminController.moreReports()` захищений `@PreAuthorize("hasRole('ADMIN')")`;
  `GET /api/reports/more` для `bob` також повертає `403`. Методна безпека
  увімкнена через `@EnableMethodSecurity`.
- `/api/**` є окремим stateless Resource Server ланцюжком із JWT-валідатором
  Keycloak. Другий ланцюжок зберігає сесійний OIDC-вхід для `/ui/**`.

## Критерії прийнятності

1. CRUD `Scooter` реалізовано через Entity, DTO, MapStruct, Repository,
   Service і REST Controller.
2. Liquibase changelog створює схему та початкові записи на чистій БД.
3. Статичний UI підтримує всі CRUD-сценарії.
4. UI використовує OIDC-вхід Keycloak і активну сесію.
5. API приймає JWT Bearer stateless, незалежно від UI-сесії.
6. ADMIN-маршрут у `SecurityFilterChain` відхиляє роль USER зі статусом `403`.
7. `@PreAuthorize` незалежно від URL-правил відхиляє роль USER зі статусом `403`.
8. `requests.http` містить токени, CRUD і перевірки `401`/`403`.
