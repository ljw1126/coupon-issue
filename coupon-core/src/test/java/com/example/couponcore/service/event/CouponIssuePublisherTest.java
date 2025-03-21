package com.example.couponcore.service.event;

import com.example.couponcore.CouponCoreConfiguration;
import com.example.couponcore.utils.KafkaOutputDestination;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static com.example.couponcore.utils.MyAssertions.assertCouponIssueEvent;

@Tag("integration")
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.config.name=application-core"})
@SpringBootTest(classes = CouponCoreConfiguration.class)
class CouponIssuePublisherTest {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))
            .withKraft()
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withEnv("KAFKA_CREATE_TOPICS", "coupon");

    @DynamicPropertySource
    private static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.kafka.binder.brokers", kafkaContainer::getBootstrapServers);
    }

    @MockBean
    private RedissonClient redissonClient; // redis connection fail when @ComponentScan

    @Autowired
    CouponIssuePublisher sut; // system under test: 테스트 대상 시스템

    @Autowired
    KafkaOutputDestination outputDestination;

    @DisplayName("CouponIssueEvent 객체를 publish 하면 메시지가 발행된다")
    @Test
    void couponIssueEvent() throws JsonProcessingException {
        long couponId = 1L;
        long userId = 1L;
        CouponIssueEvent event = new CouponIssueEvent(couponId, userId);

        sut.publish(event);

        final Message<byte[]> result = outputDestination.receive(1000, "coupon-issue-0");
        assertCouponIssueEvent(result, couponId, userId);
    }
}
