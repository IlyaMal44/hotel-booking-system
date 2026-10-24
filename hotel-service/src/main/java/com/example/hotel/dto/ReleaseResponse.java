package com.example.hotel.dto;

public record ReleaseResponse(
        /** Номер, по которому пришел release */
        Long roomId,
        /** requestId исходной блокировки */
        String requestId,
        /** true, если блокировку сняли сейчас, false если ее уже не было */
        boolean released
) {
}
