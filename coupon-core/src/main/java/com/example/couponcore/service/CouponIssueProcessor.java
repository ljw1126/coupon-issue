package com.example.couponcore.service;

import com.example.couponcore.model.Coupon;
import com.example.couponcore.service.event.CouponIssueCompleteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponIssueProcessor {

    private final CouponService couponService;
    private final CouponIssueService couponIssueService;
    private final ApplicationEventPublisher applicationEventPublisher;
    
    @Transactional
    public void issue(long couponId, long userId) {
        Coupon coupon = couponService.findCoupon(couponId);
        coupon.issue();
        couponIssueService.saveCouponIssue(couponId, userId);
    }

    @Transactional
    public void issueWithLock(long couponId, long userId) {
        Coupon coupon = couponService.findCouponWithLock(couponId);
        coupon.issue();
        couponIssueService.saveCouponIssue(couponId, userId);
        publishCouponEvent(coupon);
    }

    private void publishCouponEvent(Coupon coupon) {
        if (coupon.isIssueComplete()) {
            applicationEventPublisher.publishEvent(new CouponIssueCompleteEvent(coupon.getId()));
        }
    }
}
