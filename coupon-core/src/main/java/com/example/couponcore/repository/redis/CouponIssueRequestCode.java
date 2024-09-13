package com.example.couponcore.repository.redis;

import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.exception.ErrorCode;

public enum CouponIssueRequestCode {
    SUCCESS(1),
    DUPLICATED_COUPON_ISSUE(2),
    INVALID_COUPON_ISSUE_QUANTITY(3);

    private int code;

    CouponIssueRequestCode(int code) {
        this.code = code;
    }

    public static CouponIssueRequestCode find(String code) {
        int c = Integer.parseInt(code);
        return switch (c) {
            case 1 -> SUCCESS;
            case 2 -> DUPLICATED_COUPON_ISSUE;
            case 3 -> INVALID_COUPON_ISSUE_QUANTITY;
            default -> throw new IllegalArgumentException("존재하지 않는 코드 입니다. %s".formatted(code));
        };
    }

    public static void checkRequestResult(CouponIssueRequestCode code) {
        if (code == INVALID_COUPON_ISSUE_QUANTITY) {
            throw new CouponIssueException(ErrorCode.INVALID_COUPON_ISSUE_QUANTITY, "발급 가능한 수량을 초과합니다");
        }
        
        if (code == DUPLICATED_COUPON_ISSUE) {
            throw new CouponIssueException(ErrorCode.DUPLICATED_COUPON_ISSUE, "이미 발급된 쿠폰입니다.");
        }
    }
}
