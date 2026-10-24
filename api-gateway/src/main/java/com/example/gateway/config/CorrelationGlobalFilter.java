package com.example.gateway.config;

import com.example.common.Traces;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(Traces.CORRELATION_HEADER);
        String correlationId = incoming == null || incoming.isBlank() ? UUID.randomUUID().toString() : incoming;
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(Traces.CORRELATION_HEADER, correlationId))
                .build();
        exchange.getResponse().getHeaders().set(Traces.CORRELATION_HEADER, correlationId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -20;
    }
}
