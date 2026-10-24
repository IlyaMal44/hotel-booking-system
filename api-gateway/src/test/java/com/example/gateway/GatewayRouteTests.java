package com.example.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "server.port=0")
@ActiveProfiles("test")
class GatewayRouteTests {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    @DisplayName("В маршрутах шлюза нет отдельных путей на блокировку и release")
    void publishedRoutesDoNotIncludeRoomLockCommands() {
        List<String> routeIds = routeLocator.getRoutes().map(route -> route.getId()).collectList().block();

        assertThat(routeIds).contains("hotels", "rooms-recommend", "booking-list", "room-by-id");
        assertThat(routeIds).noneMatch(id -> id.contains("confirm") || id.contains("release"));
    }
}
