package com.example.couponconsumer.configuration;

import com.example.couponcore.repository.kafka.CouponIssueKafkaEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfiguration {
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String BOOTSTRAP_SERVERS;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String AUTO_OFFSET_RESET; // earliest: 토픽의 가장 처음 부터 읽음

    @Value("${spring.kafka.consumer.enable-auto-commit}")
    private boolean ENABLE_AUTO_COMMIT;

    @Value("${spring.kafka.consumer.key-deserializer}")
    private String KEY_DESERIALIZER;

    @Value("${spring.kafka.consumer.value-deserializer}")
    private String VALUE_DESERIALIZER;

    @Value("${spring.kafka.consumer.group-id}")
    private String GROUP_ID; //필수

    @Value("${spring.kafka.properties.spring.json.trusted.packages}")
    private String TRUSTED_PACKAGES;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, CouponIssueKafkaEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Long, CouponIssueKafkaEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<Long, CouponIssueKafkaEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, ENABLE_AUTO_COMMIT);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KEY_DESERIALIZER);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, VALUE_DESERIALIZER);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, TRUSTED_PACKAGES);

        return new DefaultKafkaConsumerFactory<>(props,
                new LongDeserializer(),
                new JsonDeserializer<>(CouponIssueKafkaEvent.class));
    }
}
