package com.example.couponconsumer.component;

import com.example.couponcore.repository.kafka.CouponIssueKafkaEvent;
import com.example.couponcore.service.CouponIssueProcessor;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CouponIssueKafkaListener {
    private final CouponIssueProcessor couponIssueProcessor;

    @KafkaListener(topics = "${test.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void receive(ConsumerRecord<Long, CouponIssueKafkaEvent> consumerRecord) {
        CouponIssueKafkaEvent event = consumerRecord.value();
        couponIssueProcessor.issueWithLock(event.couponId(), event.userId());
    }
}
