package com.example.couponcore.service;

import com.example.couponcore.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.stream.IntStream;

import static com.example.couponcore.util.CouponRedisUtils.getIssueRequestKey;
import static org.assertj.core.api.Assertions.assertThat;

class CouponIssueRedisServiceTest extends TestConfig {

    @Autowired
    CouponIssueRedisService couponIssueRedisService;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        Collection<String> redisKey = redisTemplate.keys("*");
        if (redisKey != null && !redisKey.isEmpty()) {
            redisTemplate.delete(redisKey);
        }
    }

    @DisplayName("쿠폰 수량 검증 - 발급 가능 수량이 존재하면 true를 반환한다")
    @Test
    void availableTotalIssueQuantity() {
        int totalIssueQuantity = 10;
        long couponId = 1;

        boolean result = couponIssueRedisService.availableTotalIssueQuantity(totalIssueQuantity, couponId);

        assertThat(result).isTrue();
    }

    @DisplayName("쿠폰 수량 검증 - 발급 가능 수량이 모두 소진되면 false를 반환한다")
    @Test
    void availableTotalIssueQuantity2() {
        int totalIssueQuantity = 10;
        long couponId = 1;

        IntStream.range(0, totalIssueQuantity).forEach(userId -> {
            redisTemplate.opsForSet().add(getIssueRequestKey(couponId), String.valueOf(userId));
        });

        boolean result = couponIssueRedisService.availableTotalIssueQuantity(totalIssueQuantity, couponId);

        assertThat(result).isFalse();
    }

    @DisplayName("쿠폰 중복 발급 검증 - 발급된 내역에 유저가 존재하지 않으면 true를 반환한다")
    @Test
    void availableUserIssueQuantity() {
        long couponId = 1L;
        long userId = 1L;

        boolean result = couponIssueRedisService.availableUserIssueQuantity(couponId, userId);

        assertThat(result).isTrue();
    }

    @DisplayName("쿠폰 중복 발급 검증 - 발급된 내역에 유저가 존재하면 false 반환한다")
    @Test
    void availableUserIssueQuantity2() {
        long couponId = 1L;
        long userId = 1L;

        redisTemplate.opsForSet().add(getIssueRequestKey(couponId), String.valueOf(userId));

        boolean result = couponIssueRedisService.availableUserIssueQuantity(couponId, userId);

        assertThat(result).isFalse();
    }
}
