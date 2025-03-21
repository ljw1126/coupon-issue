package com.example.couponcore.util;

public class CouponRedisUtils {

    private static final String ISSUE_REQUEST_KEY_FORMAT = "issue.request.couponId=%s";
    private static final String ISSUE_REQUEST_QUEUE_KEY = "issue.request";

    private CouponRedisUtils() {
    }

    public static String getIssueRequestKey(long couponId) {
        return ISSUE_REQUEST_KEY_FORMAT.formatted(couponId);
    }

    public static String getIssueRequestQueueKey() {
        return ISSUE_REQUEST_QUEUE_KEY;
    }
}
