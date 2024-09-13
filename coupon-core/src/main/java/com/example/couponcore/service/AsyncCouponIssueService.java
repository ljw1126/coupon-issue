package com.example.couponcore.service;

import com.example.couponcore.repository.redis.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncCouponIssueService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private final RedisRepository redisRepository;

    /**
     * 1. 유저의 요청을 sorted set 적재
     * 2. 유저의 요청의 순서를 조회
     * 3. 조회 결과를 선착순 조건과 비교
     * 4. 쿠폰 발급 queue에 적재
     */
    public void issue(long couponId, long userId) {
        String key = "issue.request.sorted_set.couponId=%s".formatted(couponId);
        redisRepository.zAdd(key, String.valueOf(userId), System.currentTimeMillis());
    }
}
