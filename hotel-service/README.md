# Hotel Management Service

Порт 8082, база `jdbc:h2:mem:hoteldb`. Слои: `domain`, `dto`, `repository`, `enums`, `exception`, `service`, `controller`, плюс `mapper` и `config`.

Внутренние операции блокировки номера не торчат из Gateway. Статистика загруженности — `GET /api/rooms/statistics`.

Swagger: http://localhost:8082/swagger-ui.html

```bash
./gradlew :hotel-service:bootRun
```
