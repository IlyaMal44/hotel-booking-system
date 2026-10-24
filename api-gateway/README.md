# API Gateway

Порт 8080. Маршруты берутся из Eureka (`lb://booking-service`, `lb://hotel-service`).

Публичные пути: `POST /api/user/register`, `POST /api/user/auth`. Остальное требует JWT. Подпись проверяется здесь и ещё раз в сервисе.

`/api/rooms/{id}/confirm-availability` и `/api/rooms/{id}/release` наружу не маршрутизируются.

```bash
./gradlew :api-gateway:bootRun
```
