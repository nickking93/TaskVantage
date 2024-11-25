package com.taskvantage.backend;

import com.taskvantage.backend.config.TestDatabaseConfig;
import com.taskvantage.backend.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestDatabaseConfig.class, TestSecurityConfig.class})
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // Test if the application context loads successfully
    }
}