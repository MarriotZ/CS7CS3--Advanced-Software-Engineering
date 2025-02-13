package com.tcd.asc.damn.damnapigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.profiles.active=test")
class DamnApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
