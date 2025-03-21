package com.example.couponcore.service;

import com.example.couponcore.TestConfig;
import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.exception.ErrorCode;
import com.example.couponcore.model.CouponIssue;
import com.example.couponcore.repository.mysql.CouponIssueJpaRepository;
import com.example.couponcore.repository.mysql.CouponIssueRepository;
import com.example.couponcore.repository.mysql.CouponJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@Tag("integration")
class CouponIssueServiceTest extends TestConfig {

    @Autowired
    CouponIssueService couponIssueService;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @Autowired
    CouponIssueJpaRepository couponIssueJpaRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    @BeforeEach
    void setUp() {
        couponJpaRepository.deleteAllInBatch();
        couponIssueJpaRepository.deleteAllInBatch();
    }

    @DisplayName("쿠폰 발급 내역이 존재하면 예외를 반환한다")
    @Test
    void saveCouponIssue() {
        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(1L)
                .userId(1L)
                .build();
        couponIssueJpaRepository.save(couponIssue);

        CouponIssueException couponIssueException = catchThrowableOfType(() -> couponIssueService.saveCouponIssue(1L, 1L), CouponIssueException.class);
        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @DisplayName("쿠폰 발급 내역이 존재하지 않는다면 쿠폰을 발급한다")
    @Test
    void saveCouponIssue2() {
        long couponId = 1L;
        long userId = 1L;

        CouponIssue result = couponIssueService.saveCouponIssue(couponId, userId);

        boolean isPresent = couponIssueJpaRepository.findById(result.getId()).isPresent();
        assertThat(isPresent).isTrue();
    }
}
