package com.example.couponconsumer.component;

import com.example.couponcore.configuration.RedisConfiguration;
import com.example.couponcore.repository.kafka.CouponIssueKafkaEvent;
import com.example.couponcore.repository.redis.RedisRepository;
import com.example.couponcore.service.CouponIssueProcessor;
import com.example.couponcore.service.event.CouponIssueEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Tag("embedded")
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"coupon"}, brokerProperties = {"listeners=PLAINTEXT://localhost:9094", "port=9094"})
@SpringBootTest
@TestPropertySource(properties = "spring.config.name=application-consumer")
class CouponIssueEmbeddedKafkaListenerTest {

    @MockBean
    CouponIssueProcessor couponIssueProcessor;

    @Autowired
    private KafkaTemplate<Long, CouponIssueKafkaEvent> kafkaTemplate;

    // memo. CouponCoreConfiguration과 같이 실행되다보니 의존성 문제 발생하여 MockBean 처리
    @MockBean
    RedisConfiguration redisConfiguration;

    @MockBean
    RedisRepository redisRepository;

    @MockBean
    RedissonClient redissonClient;

    @Value("${test.topic}")
    String topic;

    @BeforeEach
    void setUp() {
        kafkaTemplate.getProducerFactory().createProducer().flush();
    }

    @DisplayName("쿠폰 메시지가 있다면 쿠폰 발급한다")
    @Test
    void test() {
        long couponId = 1L;
        long userId = 1L;
        sendMessage(couponId, userId);

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(couponIssueProcessor, times(1))
                    .issueWithLock(couponId, userId);
        });
    }

    @DisplayName("쿠폰 발급 요청에 맞게 처리된다")
    @Test
    void test2() {
        long couponId = 1L;
        long userId1 = 1L;
        long userId2 = 2L;
        long userId3 = 3L;
        sendMessage(couponId, userId1);
        sendMessage(couponId, userId2);
        sendMessage(couponId, userId3);

        InOrder inOrder = Mockito.inOrder(couponIssueProcessor);

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            inOrder.verify(couponIssueProcessor, times(1))
                    .issueWithLock(couponId, userId1);
            inOrder.verify(couponIssueProcessor, times(1))
                    .issueWithLock(couponId, userId2);
            inOrder.verify(couponIssueProcessor, times(1))
                    .issueWithLock(couponId, userId3);
        });
    }

    private void sendMessage(long couponId, long userId) {
        CouponIssueEvent couponIssueEvent = new CouponIssueEvent(couponId, userId);
        CouponIssueKafkaEvent couponIssueKafkaEvent = CouponIssueKafkaEvent.from(couponIssueEvent);
        kafkaTemplate.send(topic, couponId, couponIssueKafkaEvent);
    }

    @TestConfiguration
    static class MyKafkaTemplateConfiguration {
        @Bean
        public ProducerFactory<Long, CouponIssueKafkaEvent> producerFactory() {
            Map<String, Object> configProps = new HashMap<>();
            configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");
            configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
            configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            return new DefaultKafkaProducerFactory<>(configProps);
        }

        @Bean
        public KafkaTemplate<Long, CouponIssueKafkaEvent> kafkaTemplate() {
            return new KafkaTemplate<>(producerFactory());
        }
    }
}
