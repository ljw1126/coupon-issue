package com.example.couponcore.utils;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Component
public class KafkaOutputDestination {

    @Value("${spring.cloud.stream.kafka.binder.brokers}")
    private String brokers;

    public Message<byte[]> receive(int timeout, String bindingName) {
        final ConsumerRecord<String, String> consumerRecord = getOneRecord(
                bindingName,
                "coupon",
                0,
                true,
                true,
                Duration.ofMillis(timeout));

        return convertToMessage(consumerRecord);
    }

    /**
     * @see KafkaTestUtils#getOneRecord
     */
    private ConsumerRecord<String, String> getOneRecord(String group, String topic, int partition,
                                                        boolean seekToLast, boolean commit, Duration timeout) {

        Map<String, Object> consumerConfig = KafkaTestUtils.consumerProps(brokers, group, "false");
        consumerConfig.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class); //import 주의

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerConfig)) {
            TopicPartition topicPart = new TopicPartition(topic, partition);
            consumer.assign(Collections.singletonList(topicPart));
            if (seekToLast) { // 마지막 메시지
                consumer.seekToEnd(Collections.singletonList(topicPart));
                if (consumer.position(topicPart) > 0) {
                    consumer.seek(topicPart, consumer.position(topicPart) - 1); // 최신 offset - 1
                }
            }
            ConsumerRecords<String, String> consumerRecords = consumer.poll(timeout);
            ConsumerRecord<String, String> consumerRecord = consumerRecords.count() == 1 ? consumerRecords.iterator().next() : null;
            if (consumerRecord != null && commit) {
                consumer.commitSync();
            }

            return consumerRecord;
        }
    }

    private Message<byte[]> convertToMessage(ConsumerRecord<String, String> consumerRecord) {
        final byte[] payload = consumerRecord.value().getBytes(StandardCharsets.UTF_8);
        return MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.OFFSET, consumerRecord.offset())
                .setHeader(KafkaHeaders.PARTITION, consumerRecord.partition())
                .setHeader(KafkaHeaders.TOPIC, consumerRecord.topic())
                .setHeader(KafkaHeaders.TIMESTAMP, consumerRecord.timestamp())
                .setHeader(KafkaHeaders.KEY, consumerRecord.key())
                .build();
    }
}
