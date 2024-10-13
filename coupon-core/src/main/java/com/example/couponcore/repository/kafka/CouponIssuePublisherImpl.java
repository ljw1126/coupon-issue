package com.example.couponcore.repository.kafka;

import com.example.couponcore.service.event.CouponIssueEvent;
import com.example.couponcore.service.event.CouponIssuePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class CouponIssuePublisherImpl implements CouponIssuePublisher {

    private final StreamBridge streamBridge;

    @Override
    public void publish(CouponIssueEvent event) {
        final CouponIssueKafkaEvent couponIssueKafkaEvent = CouponIssueKafkaEvent.from(event);
        final Message<CouponIssueKafkaEvent> message = MessageBuilder.withPayload(couponIssueKafkaEvent)
                .setHeader(CouponIssuePublisher.MESSAGE_KEY, event.couponId())
                .build();

        streamBridge.send("coupon-issue-0", message);
    }
}
