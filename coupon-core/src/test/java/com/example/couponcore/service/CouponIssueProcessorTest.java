package com.example.couponcore.service;

import com.example.couponcore.TestConfig;
import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.exception.ErrorCode;
import com.example.couponcore.model.Coupon;
import com.example.couponcore.model.CouponIssue;
import com.example.couponcore.model.CouponType;
import com.example.couponcore.repository.mysql.CouponIssueJpaRepository;
import com.example.couponcore.repository.mysql.CouponIssueRepository;
import com.example.couponcore.repository.mysql.CouponJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowableOfType;

class CouponIssueProcessorTest extends TestConfig {

    @Autowired
    private CouponIssueProcessor couponIssueProcessor;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @Autowired
    CouponIssueJpaRepository couponIssueJpaRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    @MockBean
    ApplicationEventPublisher applicationEventPublisher;

    @BeforeEach
    void setUp() {
        couponJpaRepository.deleteAllInBatch();
        couponIssueJpaRepository.deleteAllInBatch();
    }

    @DisplayName("발급 수량, 기한, 중복 발급 문제가 없다면 쿠폰을 발급한다")
    @Test
    void issueWithLock() {
        long userId = 1;
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(1))
                .build();
        couponJpaRepository.save(coupon);

        couponIssueProcessor.issueWithLock(coupon.getId(), userId);

        Coupon result = couponJpaRepository.findById(coupon.getId()).get();
        assertThat(result.getIssuedQuantity()).isEqualTo(1);

        CouponIssue firstCouponIssue = couponIssueRepository.findFirstCouponIssue(coupon.getId(), userId);
        assertThat(firstCouponIssue).isNotNull();
    }

    @DisplayName("발급 수량에 문제가 있으면 예외를 반환한다")
    @Test
    void issueWithLock2() {
        long userId = 1;
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(100)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(1))
                .build();
        couponJpaRepository.save(coupon);

        CouponIssueException couponIssueException = catchThrowableOfType(() -> couponIssueProcessor.issueWithLock(coupon.getId(), userId),
                CouponIssueException.class);
        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COUPON_ISSUE_QUANTITY);
    }

    @DisplayName("발급 기한에 문제가 있으면 예외를 반환한다")
    @Test
    void issueWithLock3() {
        long userId = 1;
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(now.plusDays(2))
                .dateIssueEnd(now.plusDays(3))
                .build();
        couponJpaRepository.save(coupon);

        CouponIssueException couponIssueException = catchThrowableOfType(() -> couponIssueProcessor.issueWithLock(coupon.getId(), userId),
                CouponIssueException.class);
        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @DisplayName("중복 발급 검증에 문제가 있다면 예외를 반환한다")
    @Test
    void issueWithLock4() {
        long userId = 1;
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(3))
                .build();
        couponJpaRepository.save(coupon);

        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(coupon.getId())
                .userId(userId)
                .build();
        couponIssueJpaRepository.save(couponIssue);

        CouponIssueException couponIssueException = catchThrowableOfType(() -> couponIssueProcessor.issueWithLock(coupon.getId(), userId),
                CouponIssueException.class);
        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @DisplayName("쿠폰이 없는 경우 예외를 반환한다")
    @Test
    void issueWithLock5() {
        long couponId = 1L;
        long userId = 1L;

        CouponIssueException couponIssueException = catchThrowableOfType(() -> couponIssueProcessor.issueWithLock(couponId, userId),
                CouponIssueException.class);
        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.COUPON_NOT_EXIST);
    }

}
