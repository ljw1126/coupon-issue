package com.example.couponconsumer.component;

import com.example.couponconsumer.TestConfig;
import com.example.couponcore.repository.redis.RedisRepository;
import com.example.couponcore.service.CouponIssueProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;

import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Disabled
@Import(CouponIssueListener.class)
class CouponIssueListenerTest extends TestConfig {

    @Autowired
    CouponIssueListener couponIssueListener;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    RedisRepository redisRepository;

    @MockBean
    CouponIssueProcessor couponIssueProcessor;

    @BeforeEach
    void setUp() {
        Collection<String> redisKeys = redisTemplate.keys("*");
        if (redisKeys != null && !redisKeys.isEmpty()) {
            redisTemplate.delete(redisKeys);
        }
    }

    @DisplayName("쿠폰 발급 큐에 처리 대상이 없다면 발급을 하지 않는다")
    @Test
    void issue() throws JsonProcessingException {
        couponIssueListener.issue();

        verify(couponIssueProcessor, never())
                .issueWithLock(anyLong(), anyLong());
    }

    @DisplayName("쿠폰 발급 큐에 처리 대상이 있다면 발급한다")
    @Test
    void issue2() throws JsonProcessingException {
        long couponId = 1L;
        long userId = 1L;
        int totalQuantity = Integer.MAX_VALUE;

        redisRepository.issueRequest(couponId, userId, totalQuantity); // SADD, RPUSH로 쿠폰 요청 데이터 넣음

        couponIssueListener.issue();

        verify(couponIssueProcessor, times(1))
                .issueWithLock(anyLong(), anyLong());
    }

    @DisplayName("쿠폰 발급 요청 순서에 맞게 처리된다")
    @Test
    void issue3() throws JsonProcessingException {
        long couponId = 1L;
        long userId = 1L;
        long userId2 = 2L;
        long userId3 = 3L;
        int totalQuantity = Integer.MAX_VALUE;

        redisRepository.issueRequest(couponId, userId, totalQuantity);
        redisRepository.issueRequest(couponId, userId2, totalQuantity);
        redisRepository.issueRequest(couponId, userId3, totalQuantity);

        couponIssueListener.issue();

        InOrder inOrder = Mockito.inOrder(couponIssueProcessor);
        inOrder.verify(couponIssueProcessor, times(1)).issueWithLock(couponId, userId);
        inOrder.verify(couponIssueProcessor, times(1)).issueWithLock(couponId, userId2);
        inOrder.verify(couponIssueProcessor, times(1)).issueWithLock(couponId, userId3);
    }
}
