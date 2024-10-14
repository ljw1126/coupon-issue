package com.example.couponcore.service.event;

import org.springframework.kafka.support.KafkaHeaders;

public interface CouponIssuePublisher {
    String MESSAGE_KEY = KafkaHeaders.KEY;

    void publish(CouponIssueEvent event);
}
