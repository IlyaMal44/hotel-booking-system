package com.example.gateway;

import com.example.gateway.config.InternalPaths;
import com.example.gateway.config.JwtAuthenticationGlobalFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationGlobalFilterTest {

    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    private final JwtAuthenticationGlobalFilter filter =
            new JwtAuthenticationGlobalFilter(jwtDecoder, new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules());

    @Test
    void missingTokenOnProtectedPathIsUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/hotels").build());

        filter.filter(exchange, request -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void invalidTokenIsUnauthorized() {
        when(jwtDecoder.decode("bad")).thenThrow(new JwtException("bad"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/hotels").header("Authorization", "Bearer bad").build());

        filter.filter(exchange, request -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicRegisterSkipsTokenCheck() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/user/register").build());
        boolean[] called = {false};

        filter.filter(exchange, request -> {
            called[0] = true;
            return Mono.empty();
        }).block();

        assertThat(called[0]).isTrue();
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void validTokenIsForwarded() {
        Jwt jwt = new Jwt("good", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"), Map.of("sub", "user"));
        when(jwtDecoder.decode("good")).thenReturn(jwt);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/hotels").header("Authorization", "Bearer good").build());
        boolean[] called = {false};

        filter.filter(exchange, request -> {
            called[0] = true;
            return Mono.empty();
        }).block();

        assertThat(called[0]).isTrue();
    }

    @Test
    void internalRoomCommandsAreNotPublished() {
        assertThat(InternalPaths.isBlocked("/api/rooms/5/confirm-availability")).isTrue();
        assertThat(InternalPaths.isBlocked("/api/rooms/5/release")).isTrue();
        assertThat(InternalPaths.isBlocked("/api/rooms/recommend")).isFalse();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/rooms/5/release").header("Authorization", "Bearer good").build());

        filter.filter(exchange, request -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(jwtDecoder);
    }
}
