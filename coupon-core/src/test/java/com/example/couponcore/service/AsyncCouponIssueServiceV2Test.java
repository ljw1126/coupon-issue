package com.example.couponcore.service;

import com.example.couponcore.TestConfig;
import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.exception.ErrorCode;
import com.example.couponcore.model.Coupon;
import com.example.couponcore.model.CouponType;
import com.example.couponcore.repository.mysql.CouponJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.IntStream;

import static com.example.couponcore.util.CouponRedisUtils.getIssueRequestKey;
import static com.example.couponcore.utils.MyAssertions.assertCouponIssueEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class AsyncCouponIssueServiceV2Test extends TestConfig {

    @Autowired
    AsyncCouponIssueServiceV2 asyncCouponIssueServiceV2;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @Autowired
    @Qualifier("localCacheManager")
    CacheManager cacheManager;

    @Autowired
    private OutputDestination outputDestination;

    @BeforeEach
    void setUp() {
        Collection<String> redisKey = redisTemplate.keys("*");
        if (redisKey != null && !redisKey.isEmpty()) {
            redisTemplate.delete(redisKey);
        }

        cacheManager.getCache("coupon").clear();
        outputDestination.clear();
    }

    @DisplayName("쿠폰 발급 - 쿠폰이 존재하지 않는다면 예외를 반환한다")
    @Test
    void issue() {
        long couponId = 1L;
        long userId = 1L;

        CouponIssueException couponIssueException = catchThrowableOfType(() -> {
            asyncCouponIssueServiceV2.issue(couponId, userId);
        }, CouponIssueException.class);

        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.COUPON_NOT_EXIST);
    }

    @DisplayName("쿠폰 발급 - 쿠폰 발급 기한이 아니라면 예외를 반환한다")
    @Test
    void issue2() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.plusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        CouponIssueException couponIssueException = catchThrowableOfType(() -> {
            asyncCouponIssueServiceV2.issue(coupon.getId(), 1L);
        }, CouponIssueException.class);

        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @DisplayName("쿠폰 발급 - 쿠폰 발급 가능 수량이 존재하지 않는다면 예외를 반환한다")
    @Test
    void issue3() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        IntStream.range(0, coupon.getTotalQuantity())
                .forEach(userId -> redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(userId)));

        CouponIssueException couponIssueException = catchThrowableOfType(() -> {
            asyncCouponIssueServiceV2.issue(coupon.getId(), 999L);
        }, CouponIssueException.class);

        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COUPON_ISSUE_QUANTITY);
    }

    @DisplayName("쿠폰 발급 - 이미 쿠폰 발급된 유저라면 예외를 반환한다")
    @Test
    void issue4() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        long userId = 1L;
        redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(userId));

        CouponIssueException couponIssueException = catchThrowableOfType(() -> {
            asyncCouponIssueServiceV2.issue(coupon.getId(), userId);
        }, CouponIssueException.class);

        assertThat(couponIssueException.getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @DisplayName("쿠폰 발급 - 쿠폰 발급 요청을 기록한다")
    @Test
    void issue5() {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        long userId = 1L;
        asyncCouponIssueServiceV2.issue(coupon.getId(), userId);

        Boolean isMember = redisTemplate.opsForSet().isMember(getIssueRequestKey(coupon.getId()), String.valueOf(userId));
        assertThat(isMember).isTrue();
    }

    @DisplayName("쿠폰 발급 - 쿠폰 발급 요청이 성공하면 redis set 추가와 메시지 이벤트 발송한다")
    @Test
    void issue6() throws JsonProcessingException {
        LocalDateTime now = LocalDateTime.now();
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        long userId = 1L;
        asyncCouponIssueServiceV2.issue(coupon.getId(), userId);

        Boolean isMember = redisTemplate.opsForSet().isMember(getIssueRequestKey(coupon.getId()), String.valueOf(userId));
        assertThat(isMember).isTrue();

        final Message<byte[]> received = outputDestination.receive(1000, "coupon-issue-0");
        assertCouponIssueEvent(received, coupon.getId(), userId);
    }

    @AfterEach
    void tearDown() {
        couponJpaRepository.deleteAllInBatch();
    }
}
