package com.example.gateway.config;

import com.example.common.ErrorDto;
import com.example.common.ErrorResponses;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final JwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationGlobalFilter(JwtDecoder jwtDecoder, ObjectMapper objectMapper) {
        this.jwtDecoder = jwtDecoder;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (InternalPaths.isBlocked(path)) {
            return write(exchange, HttpStatus.NOT_FOUND, "Not Found", "Route is not published");
        }
        if (isPublic(path)) {
            return chain.filter(exchange);
        }
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return write(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", "JWT token is missing");
        }
        try {
            jwtDecoder.decode(header.substring("Bearer ".length()));
        } catch (JwtException exception) {
            return write(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", "JWT token is invalid");
        }
        return chain.filter(exchange);
    }

    private boolean isPublic(String path) {
        return "/api/user/register".equals(path)
                || "/api/user/auth".equals(path)
                || "/user/register".equals(path)
                || "/user/auth".equals(path)
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String error, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ErrorDto body = ErrorResponses.of(status.value(), error, message, exchange.getRequest().getPath().value());
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException exception) {
            bytes = new byte[0];
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
