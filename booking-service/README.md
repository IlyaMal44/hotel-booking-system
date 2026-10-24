# Booking Service

Порт 8081, база `jdbc:h2:mem:bookingdb`. Здесь регистрация, JWT и сага бронирования.

Слои: `domain`, `dto`, `repository`, `enums`, `exception`, `service`, `controller`, `mapper`, `client`, `config`.

Вызов Hotel Service идёт через Eureka, с таймаутом, повторами и circuit breaker. Клиентский `requestId` делает создание брони идемпотентным.

Swagger: http://localhost:8081/swagger-ui.html

```bash
./gradlew :booking-service:bootRun
```
