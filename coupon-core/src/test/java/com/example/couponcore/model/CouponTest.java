package com.example.couponcore.model;

import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class CouponTest {

    @DisplayName("발급 수량이 남아 있으면 true를 반환한다")
    @Test
    void availableIssueQuantity() {
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .build();

        boolean result = coupon.availableIssueQuantity();

        assertThat(result).isTrue();
    }

    @DisplayName("발급 수량이 소진되었다면 false를 반환한다")
    @Test
    void availableIssueQuantity_fail() {
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(100)
                .build();

        boolean result = coupon.availableIssueQuantity();

        assertThat(result).isFalse();
    }

    @DisplayName("발급 수량이 설정되지 않았다면 true를 반환한다")
    @Test
    void availableIssueQuantity_fail2() {
        Coupon coupon = Coupon.builder()
                .totalQuantity(null)
                .issuedQuantity(100)
                .build();

        boolean result = coupon.availableIssueQuantity();

        assertThat(result).isTrue();
    }

    @DisplayName("발급 기간이 시작되지 않았다면 false를 반환한다")
    @Test
    void availableIssueDate() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .dateIssueStart(now.plusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();

        boolean result = coupon.availableIssueDate();

        assertThat(result).isFalse();
    }

    @DisplayName("발급 기간이 해당되면 true를 반환한다")
    @Test
    void availableIssueDate2() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();

        boolean result = coupon.availableIssueDate();

        assertThat(result).isTrue();
    }

    @DisplayName("발급 기간이 종료되면 true를 반환한다")
    @Test
    void availableIssueDate3() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.minusDays(1))
                .build();

        boolean result = coupon.availableIssueDate();

        assertThat(result).isFalse();
    }

    @DisplayName("발급 수령과 발급 기간이 유효하다면 발급에 성공한다")
    @Test
    void issue() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(1))
                .build();

        coupon.issue();

        assertThat(coupon.getIssuedQuantity()).isEqualTo(100);
    }

    @DisplayName("발급 수령을 초과하면 예외를 반환한다")
    @Test
    void issue2() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(100)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(1))
                .build();

        CouponIssueException couponIssueException = catchThrowableOfType(coupon::issue, CouponIssueException.class);
        assertThat(couponIssueException.getErrorCode()).isEqualTo(ErrorCode.INVALID_COUPON_ISSUE_QUANTITY);
    }

    @DisplayName("발급 기간이 아니면 예외를 반환한다")
    @Test
    void issue3() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .dateIssueStart(now.plusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();

        CouponIssueException couponIssueException = catchThrowableOfType(coupon::issue, CouponIssueException.class);
        assertThat(couponIssueException.getErrorCode()).isEqualTo(ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @DisplayName("발급 기간이 종료되면 true를 반환한다")
    @Test
    void isIssueComplete() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.minusDays(1))
                .build();

        boolean result = coupon.isIssueComplete();

        assertThat(result).isTrue();
    }

    @DisplayName("잔여 발급 수량이 없다면 true 반환한다")
    @Test
    void isIssueComplete2() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(100)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(1))
                .build();

        boolean result = coupon.isIssueComplete();

        assertThat(result).isTrue();
    }

    @DisplayName("잔여 발급 수량과 발급 기한이 유효하면 false를 반환한다")
    @Test
    void isIssueComplete3() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(1))
                .build();

        boolean result = coupon.isIssueComplete();

        assertThat(result).isFalse();
    }
}
