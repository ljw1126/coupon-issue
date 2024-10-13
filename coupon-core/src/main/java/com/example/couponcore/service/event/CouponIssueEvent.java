package com.example.couponcore.service.event;

public record CouponIssueEvent(
        long couponId,
        long userId
) {
}
