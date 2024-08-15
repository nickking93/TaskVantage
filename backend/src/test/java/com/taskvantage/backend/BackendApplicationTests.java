package com.taskvantage.backend;

import com.taskvantage.backend.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {BackendApplication.class, TestSecurityConfig.class})
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
