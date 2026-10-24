# Схема данных

Обе базы — H2 in-memory, схема создаётся Hibernate (`ddl-auto: create-drop`) из JPA-сущностей. Ниже поля сверх минимального набора из задания и зачем они нужны.

## bookingdb

### users

| Колонка | Зачем |
| --- | --- |
| id | ключ |
| username | уникальный логин |
| password | BCrypt |
| role | `USER` или `ADMIN` |
| created_at | когда учётная запись появилась |

### bookings

| Колонка | Зачем |
| --- | --- |
| id | ключ |
| user_id | ссылка на users |
| room_id | номер в другой базе, без внешнего ключа между сервисами |
| start_date, end_date | интервал проживания, конец не включается |
| status | `PENDING`, `CONFIRMED`, `CANCELLED` |
| created_at | из задания |
| updated_at | момент последней смены статуса |
| request_id | уникальный ключ идемпотентности |

Индекс `idx_booking_room_dates` по `(room_id, start_date, end_date, status)` ускоряет проверку пересечений. Индекс `idx_booking_user` — историю пользователя.

### room_guards

Одна строка на `room_id`. Нужна, чтобы взять пессимистичную блокировку перед проверкой пересечения. Это не календарь, а замок от гонки.

## hoteldb

### hotels

| Колонка | Зачем |
| --- | --- |
| id, name, address | из задания |
| description, stars | фильтр и карточка отеля |

### rooms

| Колонка | Зачем |
| --- | --- |
| id, hotel_id, number, available, times_booked | из задания |
| capacity, floor | дополнительные сведения о номере |
| created_at | когда номер завели |

Уникальность `(hotel_id, number)`. Индекс `idx_room_times_booked` по `(times_booked, id)` соответствует сортировке рекомендации.

`available` — номер в эксплуатации. Даты сюда не записываются.

### room_holds

Временная, а после подтверждения — удерживаемая блокировка слота.

| Колонка | Зачем |
| --- | --- |
| id | ключ |
| room_id | номер |
| booking_id | бронь из другого сервиса |
| request_id | уникальный ключ идемпотентности |
| start_date, end_date | интервал блокировки |
| status | `ACTIVE` или `RELEASED` |
| created_at, released_at | когда поставили и сняли |

Индекс `idx_hold_room_dates` по `(room_id, status, start_date, end_date)` используется при проверке пересечения.
