package com.example.couponconsumer.component;

import com.example.couponcore.repository.kafka.CouponIssueKafkaEvent;
import com.example.couponcore.service.CouponIssueProcessor;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CouponIssueKafkaListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CouponIssueKafkaListener.class);

    private final CouponIssueProcessor couponIssueProcessor;

    @KafkaListener(topics = "${test.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void receive(ConsumerRecord<Long, CouponIssueKafkaEvent> consumerRecord) {
        CouponIssueKafkaEvent event = consumerRecord.value();

        printLog("발급 시작 target : %s", event);
        couponIssueProcessor.issueWithLock(event.couponId(), event.userId());
        printLog("발급 종료 target : %s", event);
    }

    private void printLog(String message, CouponIssueKafkaEvent request) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{}", message.formatted(request));
        }
    }
}
