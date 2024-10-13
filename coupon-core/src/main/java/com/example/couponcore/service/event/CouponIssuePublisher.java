package com.example.couponcore.service.event;

public interface CouponIssuePublisher {
    String MESSAGE_KEY = "custom.messageKey";

    void publish(CouponIssueEvent event);
}
