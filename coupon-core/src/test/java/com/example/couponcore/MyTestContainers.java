package com.example.couponcore;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MyTestContainers {

    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3")
            .withDatabaseName("coupon")
            .withUsername("tester")
            .withPassword("1234")
            .withUrlParam("serverTimezone", "UTC")
            .withUrlParam("useSSL", "false")
            .withUrlParam("allowPublicKeyRetrieval", "true");

    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    static {
        mySQLContainer.start();
        redisContainer.start();
    }

    @DynamicPropertySource
    private static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.jpa.show-sql", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }
}
