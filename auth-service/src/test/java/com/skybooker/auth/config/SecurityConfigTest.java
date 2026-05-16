package com.skybooker.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SecurityConfigTest {

    @Autowired(required = false)
    private SecurityFilterChain securityFilterChain;

    @Test
    void contextLoads() {
        // This will trigger the securityFilterChain bean creation
        // and cover the lines in SecurityConfig.java
        assertNotNull(securityFilterChain, "SecurityFilterChain should be created");
    }
}
