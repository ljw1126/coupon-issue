package com.example.couponcore.component;

import com.example.couponcore.model.event.CouponIssueCompleteEvent;
import com.example.couponcore.service.CouponCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
public class CouponEventListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private final CouponCacheService couponCacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void issueComplete(CouponIssueCompleteEvent event) {
        printLog("issue complete. cache refresh start couponId : %s", event.couponId());
        couponCacheService.putCouponCache(event.couponId());
        couponCacheService.putCouponLocalCache(event.couponId());
        printLog("issue complete. cache refresh end couponId : %s", event.couponId());
    }

    private void printLog(String message, long couponId) {
        if (logger.isInfoEnabled()) {
            logger.info(message.formatted(couponId));
        }
    }
}
