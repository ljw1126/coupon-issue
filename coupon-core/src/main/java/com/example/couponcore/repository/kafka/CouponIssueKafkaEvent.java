package com.example.couponcore.repository.kafka;

import com.example.couponcore.service.event.CouponIssueEvent;

public record CouponIssueKafkaEvent(
        String type,
        Long couponId,
        Long userId
) {
    private static final String EVENT_TYPE = "CouponIssue";

    public static CouponIssueKafkaEvent from(CouponIssueEvent event) {
        return new CouponIssueKafkaEvent(EVENT_TYPE, event.couponId(), event.userId());
    }
}
