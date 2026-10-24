package com.example.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "server.port=0")
@ActiveProfiles("test")
class EurekaServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
